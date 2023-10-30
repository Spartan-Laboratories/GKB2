package com.spartanlabs.bottools.dataprocessing

import com.spartanlabs.bottools.main.Bot
import com.spartanlabs.bottools.main.newGuildJoinAction
import com.spartanlabs.bottools.main.newGuildMemberJoinAction
import com.spartanlabs.bottools.main.newGuildUpdateNameAction
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
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.io.path.pathString

val D:Database = KotGDP()
val B:DatabaseAccessPoint = KotGDP() openFile "BotData.xml"
class KotGDP(private val reader : BaseXMLReader = BaseXMLReader()) : XMLReader by reader, Database {
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
    override fun onNewServer(guild: Guild) = createGuildDatabase(guild)
    override fun onNewMember(server: Guild, member: Member) :Result<Unit> = try{
        createMemberEntry(server, member.user.name)
        Result.success(Unit)
    }catch (e:Exception){
        log.error("Unable to create database entry for member: ${member.user.name}")
        Result.failure(e)
    }
    override fun onLaunchMemberCheck():Result<Unit> = try {
        updateServerDatabase()
        Result.success(Unit)
    }catch (e:Exception){
        log.error("An error occured while trying to conduct an on launch member check")
        Result.failure(e)
    }

    infix fun openFile(fileName: String):DatabaseAccessPoint = XmlDataAccessPoint(reader setDocument fileName)
    override operator fun div(guild: Guild):MemberAccessPoint = this inServer guild
    infix fun inServer(guild:Guild):MemberAccessPoint = XmlMemberAccessPoint(guild)
    private infix fun `change server name` (event: GuildUpdateNameEvent) = renameFolder(event.oldName, event.newName)
    private fun renameFolder(oldPath:String, newPath:String) = File(oldPath).renameTo(File(newPath))

    fun createGuildDatabase(guild: Guild): Result<Unit> {
        try {
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
                if (root has it.key) return@forEach
                reader create it.key containing it.value
            }
            write()
        } catch (e: Exception) {
            log.error("Unable to create database for guild: $guild")
            return Result.failure(e)
        }
        return Result.success(Unit)
    }

    internal fun updateServerDatabase() : Unit = Bot.jda.guildCache.forEach(::updateServerDatabase)
    private infix fun updateServerDatabase(guild: Guild) {
        for (member in guild.loadMembers().get()) try{
            log.debug("Found guild member named: ${member.user.name}")
            member.user.name.xmlAcceptable().let{member ->
                if (member !in filedMembers(guild))
                    createMemberEntry(guild,member)
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
    private fun createMemberEntry(server:Guild, username:String){
        log.debug("$username is not yet in the database. Creating new data node...")
        reader setDocument "guildData/${server.name}/MemberData.xml"
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

    private inner class XmlMemberAccessPoint internal constructor(private val guild: Guild)
        :XmlDataAccessPoint(reader setDocument "guildData/${guild.name}/GuildData.xml"), MemberAccessPoint{
        override operator fun div(member: Member) = this forMember member
        infix fun forMember(member: Member):XmlDataAccessPoint = XmlDataAccessPoint(getNodeOf(guild,member)!!)
    }
    inner class TerminalAccessPoint internal constructor(val nodeName:String)
    operator fun String.unaryMinus()    = TerminalAccessPoint(this)
    private open inner class XmlDataAccessPoint internal constructor(protected val node:Node):DatabaseAccessPoint{
        override val name = node.nodeName
        var value:String
            get() = reader getValue node
            set(value) = Unit.also { reader.setValue(node, value) }
        override operator fun div(groupName: String) = this assertChild groupName
        override operator fun minus(nodeName: String)    = reader getValue (this/nodeName).node
        infix fun getValue(nodeName:String)     = node-nodeName
        operator fun plus(newValue: String)     = this setTo newValue
        infix fun setTo(newValue:String)        = Unit.also{ reader.setValue(node, newValue)}

        override infix fun create(childNodeName:String)  = XmlDataAccessPoint(reader at node create childNodeName)
        override infix fun containing(text:String) = Unit.also { reader.setValue(node, text) }
        infix fun assertChild(childNodeName: String) =
            if (node has childNodeName) this getGroup childNodeName
            else                        this create childNodeName
        override infix fun remove(childNode:String)      = reader.removeTag(node, childNode)
        infix fun remove(xmlDataAccessPoint: XmlDataAccessPoint) = remove(xmlDataAccessPoint.node.nodeName)
        private infix fun getGroup(groupName:String)    = XmlDataAccessPoint(node/groupName)
        operator fun div(terminalPoint:TerminalAccessPoint) = this-terminalPoint.nodeName
        override operator fun set(nodeName:String, value:String) = this/nodeName + value
        override infix fun to(value:String) = this setTo value
        override val children = node.children().map(Node::getNodeName)
        override fun clear() = children.forEach(::remove)
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
        Bot.jda.guilds.forEach {
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
