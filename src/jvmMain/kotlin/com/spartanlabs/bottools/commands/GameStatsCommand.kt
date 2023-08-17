package com.spartanlabs.bottools.commands

import com.spartanlabs.bottools.dataprocessing.D
import com.spartanlabs.bottools.dataprocessing.KotGDP
import com.spartanlabs.bottools.services.ServiceCommand
import com.spartanlabs.bottools.services.UserGameService
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel

abstract class GameStatsCommand
     protected constructor(protected var gameName: String, primaryAddress:String="")
    : OnlineCommand(gameName, primaryAddress) {
    override var brief = "A command provides information related to the game $gameName"
    override var details = ""
    override val detailStatement = "This command is oriented around providing various subcommands related to a specific game."
    protected var tagSymbol = "#"
    protected val userOption            = Option("user", "person", "the server member whose game you want to see", false);
    protected val targetMemberOption    = Option("user", "person", "the person whose game id you want to set", false)
    protected val show = this + "show"
    protected val id = this + "id"
    protected val last = this + "last"
    protected val showStats: SubCommand =
        MethodCommand(::showStats, "showstats", "Show the target player's stats", this) + userOption
    protected val lastGame: SubCommand =
        MethodCommand(::manualLastGame, "lastgame", "Show the stats from the last game that was played",this) + userOption
    init {
        //  Command setup
        subCommandRequired = true
        makeInteractive()
        // Organization
        last and "game" becomes "lastgame"
        PropertyCommand(::matchesChannelId, this, "channel")
        TargetablePropertyCommand(::user_ID, this)
        //id.addCommand("set", "setid").addCommand("get", "getid").addCommand("show", "getid")
        //show.addCommand("id", "getid").addCommand("stats", "showstats")
    }
    protected inner class SCSetID() : SubCommand("setid", this@GameStatsCommand) {
        override val brief = "sets the game id of the mentioned user"
        override val details =
            """"For example:
            `${this@GameStatsCommand.name} setid *in-game id* @forthisperson` to set the ID for the mentioned user. Or"
            `${this@GameStatsCommand.name} setid *in-game id*` to set the ID for yourself""".trimIndent()
        init {
            val id = Option("string", "id", "the $gameName id", true)
            val tagline = Option("string", "tagline", "the tag number associated with this id", false)
            this + id + tagline + targetMemberOption
        }

        override fun invoke(args: Array<String>){
            var tagline = getOption("tagline")?.asString
            if (tagline != null && Character.isDigit(tagline[0]))  //if the first character of the tagline is numeric
                tagline = tagSymbol + tagline   //then add tag symbol to the beginning of it
            val fullTag = args[0] + (tagline ?: "")
            user_ID = fullTag
            `reply with`("The $gameName ID of ${targetMember!!.effectiveName} has been set to $fullTag")
        }
    }

    inner class SCGetID() : SubCommand("getid", this@GameStatsCommand) {
        override val brief: String = "Shows the recorded game ID for the mentioned user or for yourself if no user is mentioned"
        override val details: String = ""
        init {
            this + targetMemberOption
        }

        override operator fun invoke(args: Array<String>){
            if(user_ID.isNullOrBlank())  sendNoIDMessage()
            else                        `reply with`("The in-game ID of " + (targetMember!!.user.name) + " is " + user_ID)
        }
    }
    /**
     * Returns the in-game username of the guild member that is the target of this command
     * @return In-game username of the target member
     */
    internal var user_ID: String?
        get()       = D/guild/targetMember!!/"Games"/gameName-"id"
        set(value)  = D/guild/targetMember!!/"Games"/gameName/"id" + value!!
    protected var lastMatchID: String?
        get()       = D/guild/targetMember!!/"Games"/gameName-"latestmatchid"
        set(newID)  = D/guild/targetMember!!/"Games"/gameName/"latestmatchid"+newID!!
    internal var matchesChannelId: String?
        get()       = D/guild/"Games"/gameName-"matcheschannel"
        set(value)  = D/guild/"Games"/gameName/"matcheschannel"+value!!
    private class DataCommand(dataPropertyAccessPoint:KotGDP.DataAccessPoint, dataPropertyName:String, parent:GameStatsCommand)
    :PropertyCommand(GameStatsCommand.PropertyHolder(dataPropertyAccessPoint, dataPropertyName)::property, parent){}
    private fun getDataCommand(valueName:String) = DataCommand(dataPropertyAccessPoint, valueName, this)
    val dataPropertyAccessPoint:KotGDP.DataAccessPoint
        get() = D/guild/"Games"/gameName
    class PropertyHolder(private val dataPoint:KotGDP.DataAccessPoint, private val endPoint:String){
        var property:String?
            get()       = dataPoint-endPoint
            set(value)  = dataPoint/endPoint+value!!
    }

    protected fun sendNoIDMessage() = `reply with`("This person's ${gameName} ID has not been set. Use:```/$gameName setid *in-game id* @forthisperson``` to set someone's ID")
    var ss : (Array<String>) -> Unit = {}
    protected open fun showStats(args: Array<String>){}
    private fun manualLastGame(args: Array<String>){
        require(user_ID != null){
            `reply with`("Could not get the last game that this user has played because their id has not been set.")
            return
        }
        lastGame(args = arrayOf(), auto = false)
    }
    protected open fun lastGame(args: Array<String>, auto: Boolean){}
    private fun userGameServiceTrigger(args:Array<String>):Unit{
        guild   = jda.getGuildById(args[0])!!
        member  = guild.getMemberById(args[1])!!
        channel = guild.getTextChannelById(matchesChannelId?:"0") ?: guild.defaultChannel as MessageChannel
        lastGame(arrayOf(args[2]),true)
    }
    protected open infix fun postPatchNotes(value: Array<String>){}
    protected fun createPatchNotesService() = ServiceCommand.createService("game services/$gameName/patch notes", ::postPatchNotes, 10)
    protected fun createUserGamesService() = UserGameService.createService(gameName, ::userGameServiceTrigger, 60)
    override fun getValueFromKey(key: String?): String = super.getValueFromKey("$gameName:$key")
}

