package com.spartanlabs.bottools.plugins

import com.spartanlabs.bottools.commands.Command
import com.spartanlabs.bottools.dataprocessing.D
import com.spartanlabs.bottools.main.Bot
import com.spartanlabs.bottools.main.newMessageDeleteAction
import com.spartanlabs.bottools.main.newMessageReactionAddAction
import com.spartanlabs.bottools.main.newMessageReceivedAction
import com.spartanlabs.bottools.manager.MyLogger
import com.spartanlabs.bottools.plugins.math.MathCommand
import com.spartanlabs.bottools.plugins.poker.PokerCommand
import com.spartanlabs.bottools.plugins.reactionroles.AddReactionRole
import com.spartanlabs.bottools.plugins.reactionroles.CreateMainWelcomeMessage
import com.spartanlabs.bottools.plugins.reactionroles.ReactionRoleActions
import com.spartanlabs.bottools.plugins.weather.WeatherCommand
import com.spartanlabs.generaltools.evaluateList
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class Plugin(val fn:Bot.Companion.()->Collection<Command>){
    val log = MyLogger(Plugin::class.java)
    operator fun invoke():Collection<Command> = fn.invoke(Bot).also{
        //log.info("Plugin ${this@Plugin.javaClass} has been invoked")
        //log.info(it.toString())
    }
}


object Plugins {
    operator fun get(pluginName:String):Plugin? = when(pluginName){
        "reactionroles" -> `REACTION ROLES`
        "math"          -> Math
        "poker"         -> Poker
        "spamcontrol"   -> SpamControl
        "weather"       -> Weather
        else            -> null
    }
    val `REACTION ROLES` = Plugin {
        //TODO D.addTagFile("src/jvmMain/resources/ReactionRoles.xml")
        responder newMessageReactionAddAction  ReactionRoleActions::giveReactionRole
        responder newMessageDeleteAction       ReactionRoleActions::removeDeletedWelcomeMessage
        return@Plugin listOf(AddReactionRole(),CreateMainWelcomeMessage())
    }
    val Math = Plugin{
        return@Plugin listOf(MathCommand())
    }
    val Poker = Plugin{
        return@Plugin listOf(PokerCommand())
    }
    val SpamControl = Plugin{
        responder newMessageReceivedAction ::isSpam
        return@Plugin listOf()
    }
    val Weather = Plugin{
        return@Plugin listOf(WeatherCommand())
    }

    private fun isSpam(event: MessageReceivedEvent): Boolean {
        val channel: MessageChannel = event.channel
        val history = channel.history
        val spamThreshold = 10
        history.retrievePast(spamThreshold).complete()
        val retrievedMessages = history.retrievedHistory
        if (retrievedMessages.size > spamThreshold) return false
        return evaluateList(retrievedMessages){it.contentRaw == event.message.contentRaw}
    }
}