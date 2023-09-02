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
        PropertyCommand(::matchesChannelId, this, "channel").`with getters`("view")
        PropertyCommand(::patchnotesChannelID, this,"channel") `using getter` show
        TargetablePropertyCommand(::user_ID, this)
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
    internal var patchnotesChannelID:String?
        get()       = D/guild/"Games"/gameName-"patchnoteschannel"
        set(value)  = D/guild/"Games"/gameName/"patchnoteschannel"+value!!
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
    protected fun sendNoIDMessage() = reply!!> "This person's ${gameName} ID has not been set. Use:```/$gameName setid *in-game id* @forthisperson``` to set someone's ID"
    protected open fun showStats(args: Array<String>){}
    private fun manualLastGame(args: Array<String>){
        require(user_ID != null){
            reply!!>"Could not get the last game that this user has played because their id has not been set."
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

