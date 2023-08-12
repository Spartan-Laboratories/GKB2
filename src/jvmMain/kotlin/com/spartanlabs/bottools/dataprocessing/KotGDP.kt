package com.spartanlabs.bottools.dataprocessing

import com.spartanlabs.bottools.main.*
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.guild.update.GuildUpdateNameEvent
import org.slf4j.LoggerFactory
import org.w3c.dom.Node
import java.io.File
import java.io.FileNotFoundException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.pathString

val D = KotGDP()
val B = KotGDP() openFile "BotData.xml"
class KotGDP(private val reader : BaseXMLReader = BaseXMLReader()) : XMLReader by reader {
    private val log = LoggerFactory.getLogger(KotGDP::class.java)
    private val customPersonNodes = HashMap<String, String>()
    private val customGuildNodes = HashMap<String,String>()

    init{
        Bot.responder.apply{
            newGuildUpdateNameAction(                   ::`change server name`)
            newGuildJoinAction          {with(it.guild, ::createGuildDatabase)}
            newGuildMemberJoinAction    {with(it){with(guild){
                updateServerDatabase()
                getRoleById(D/guild-"defaultrole").let{role->
                    addRoleToMember(member, role!!).complete()
                }
            } } }
        }
    }

    infix fun openFile(fileName: String) = DataAccessPoint(reader setDocument fileName)
    operator fun div(guild: Guild) = this inServer guild
    infix fun inServer(guild:Guild) = GuildDataAccessPoint(guild)
    private infix fun `change server name` (event: GuildUpdateNameEvent) = renameFolder(event.oldName, event.newName)
    private fun renameFolder(oldPath:String, newPath:String) = File(oldPath).renameTo(File(newPath))

    fun createGuildDatabase(guild: Guild) {
        val guildPath = "guildData/${guild.name}"
        File(guildPath).mkdirs()
        val guildData = listOf(
            "<data>",
            "<defaultRole>online</defaultRole>",
            "<welcomeMessage>hello</welcomeMessage>",
            "</data>",
        )
        val guildDataPath = Paths.get("$guildPath/GuildData.xml")
        val people = Arrays.asList("<People>\n", "</People>")
        val peoplePath = Paths.get("$guildPath/MemberData.xml")
        Files.write(guildDataPath, guildData, Charset.forName("UTF-8"))
        Files.write(peoplePath, people, Charset.forName("UTF-8"))
        updateServerDatabase(guild)
        reader setDocument guildDataPath.pathString
        reader at root
        customGuildNodes.forEach {
            if(root has it.key)return@forEach
            reader create it.key containing it.value
        }
        write()
    }

    internal fun updateServerDatabase() : Unit = Bot.jda!!.guildCache.forEach(::updateServerDatabase)
    private infix fun updateServerDatabase(guild: Guild) {
        for (member in guild.loadMembers().get()) try{
            log.debug("Found guild member named: ${member.user.name}")
            member.user.name.xmlAcceptable().let{member ->
                if (member !in filedMembers(guild))
                    createMemberEntry(member)
            }
        }catch(e:FileNotFoundException){
            log.error("Unable to update database for guild: $guild")
        }
    }
    private fun filedMembers(guild: Guild):List<String>{
        reader setDocument "guildData/${guild.name}/MemberData.xml"
        return root.children().map {
            try {
                it - "tName"
            } catch (e: IllegalArgumentException) {
                log.error("filedMembers() failed to access the name of a member of the guild: $guild.name")
                "no name"
            }
        }
    }
    private fun createMemberEntry(username:String){
        log.debug("This member is not yet in the database. Creating new data node...")
        val personNode =
        reader at reader.root createCategory "Person"
            reader create "tName"       containing username
            reader create "keyid"
            reader create "nPoints"     containing "0.0"
            reader create "requestedby" containing "none"
            reader create "money"       containing "1000"
            reader create "Games"

            reader createCategory "time"
                reader create "timezone"    containing "none"
                reader create "dston"       containing "false"
                reader create "dststart"    containing "none"
                reader create "dstend"      containing "none"
        reader at personNode
        customPersonNodes.forEach { reader create it.key containing it.value }
        write()
    }
    private fun String.xmlAcceptable() = toList().filter(Character::isLetterOrDigit).joinToString("")

    inner class GuildDataAccessPoint internal constructor(private val guild: Guild)
        :DataAccessPoint(reader setDocument "guildData/${guild.name}/GuildData.xml"){
        operator fun div(member: Member) = this forMember member
        infix fun forMember(member: Member):DataAccessPoint = DataAccessPoint(getNodeOf(guild,member)!!)
    }
    inner class TerminalAccessPoint internal constructor(val nodeName:String)
    operator fun String.unaryMinus()    = TerminalAccessPoint(this)
    open inner class DataAccessPoint internal constructor(private val node:Node){
        var value:String
            get() = reader getValue node
            set(value) = Unit.also { reader.setValue(node, value) }
        operator fun div(groupName: String)     = this assertChild groupName
        operator fun minus(nodeName: String)    = reader getValue (this/nodeName).node
        infix fun getValue(nodeName:String)     = node-nodeName
        operator fun plus(newValue: String)     = this setTo newValue
        infix fun setTo(newValue:String)        = Unit.also{ reader.setValue(node, newValue)}
        fun readAll() = node.children()
        infix fun create(childNodeName:String)  = DataAccessPoint(reader at node create childNodeName)
        infix fun containing(text:String) = reader.setValue(node, text)
        infix fun assertChild(childNodeName: String)    =   if(node has childNodeName)  this getGroup   childNodeName
                                                            else                        this create     childNodeName
        infix fun remove(childNodeName:String)  = reader.removeTag(node,childNodeName)
        infix fun remove(node:Node)             = reader.removePossibleTag(this.node, node)
        private infix fun getGroup(groupName:String)    = DataAccessPoint(node/groupName)
        operator fun div(terminalPoint:TerminalAccessPoint) = this-terminalPoint.nodeName
    }
    private fun getNodeOf(guild:Guild, member: Member): Node? {
        reader setDocument "guildData/${guild.name}/MemberData.xml"
        for (personNode in root.children())
            if (personNode - "tName" == member.user.name.xmlAcceptable())
                return personNode
        return null
    }
    fun addTagFile(localFilePath:String) {
        reader setDocument localFilePath
        (root/"person").children().forEach{customPersonNodes.put(it.nodeName, it.getValue())}
        customGuildNodes.putAll((root/"data").children().map { it.nodeName to it.getValue() }.toMap())
    }
    internal fun addTag(fileName:String, node: String, value:String = "")   = performActionInFile(fileName, node, ::addTag, value)
    internal fun removeTag(fileName: String, node:String) :Unit             = performActionInFile(fileName, node, ::removeTag)
    private fun performActionInFile(fileName:String,  node:String, action : (parent:Node, child:String, value:String) -> Unit, value:String=""){
        Bot.jda?.guilds!!.forEach {
            reader setDocument "guildData/${it.name}/$fileName"
            when(fileName.lowercase()){
                "guilddata.xml" -> action.invoke(root, node, value)
                "memberdata.xml" -> root.children().forEach{personNode -> action(personNode, node, value)}
                else -> throw IllegalArgumentException("Invalid file name")
            }
            write()
        }
    }
    private fun addTag(parent: Node, child: String, value: String) = reader at parent create child containing value
    private fun removeTag(parent:Node, child:String, value:String) = reader.removeTag(parent, child)
}
