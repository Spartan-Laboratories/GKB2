package com.spartanlabs.bottools.plugins

import com.spartanlabs.bottools.commands.Command
import com.spartanlabs.bottools.dataprocessing.D
import com.spartanlabs.bottools.main.Bot
import com.spartanlabs.bottools.main.newMessageDeleteAction
import com.spartanlabs.bottools.main.newMessageReactionAddAction
import com.spartanlabs.bottools.main.newMessageReceivedAction
import com.spartanlabs.bottools.plugins.math.MathCommand
import com.spartanlabs.bottools.plugins.poker.PokerCommand
import com.spartanlabs.bottools.plugins.reactionroles.AddReactionRole
import com.spartanlabs.bottools.plugins.reactionroles.CreateMainWelcomeMessage
import com.spartanlabs.bottools.plugins.reactionroles.ReactionRoleActions
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
private fun Bot.Companion.createSingleCommandPlugin(command:Command):Bot.Companion.()->Unit = {
    Bot createCommand command
}
class Plugin(val fn:Bot.Companion.()->Unit = { }){
    constructor(command: Command):this(){
        Bot.Companion createCommand command
    }
    operator fun invoke() = fn?.invoke(Bot)

}


object Plugins {
    val `REACTION ROLES` = Plugin {
        D.addTagFile("src/jvmMain/resources/ReactionRoles.xml")
        createCommand(AddReactionRole())
        createCommand(CreateMainWelcomeMessage())
        responder newMessageReactionAddAction  ReactionRoleActions::giveReactionRole
        responder newMessageDeleteAction       ReactionRoleActions::removeDeletedWelcomeMessage
    }
    val Math = Plugin{
        Bot createCommand MathCommand()
    }
    val Poker = Plugin{
        Bot createCommand PokerCommand()
    }
    val SpamControl = Plugin{
        responder newMessageReceivedAction ::isSpam
    }

    private fun isSpam(event: MessageReceivedEvent): Boolean {
        val channel: MessageChannel = event.channel
        val history = channel.history
        val spamThreshold = 10
        history.retrievePast(spamThreshold).complete()
        val retrievedMessages = history.retrievedHistory
        if (retrievedMessages.size > spamThreshold) return false
        for (i in 1 until spamThreshold) if (retrievedMessages[i].contentRaw != event.message.contentRaw) return false
        return true
    }
}