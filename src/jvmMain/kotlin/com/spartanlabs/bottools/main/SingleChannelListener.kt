package com.spartanlabs.bottools.main

import com.spartanlabs.bottools.commands.Command
import com.spartanlabs.bottools.main.Parser.CommandContainer
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class SingleChannelListener(protected var channel: MessageChannel, protected var command: Command) : ListenerAdapter() {
    protected var commandInfo: CommandContainer? = null
    private val primaryListener = Bot.jda.registeredListeners[0] as BotListener

    init {
        primaryListener.ignoreChannel(channel)
        Bot.jda!!.addEventListener(this)
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.channel.id != channel.id) return
        val message = event.message.contentRaw
        if (Parser.`starts with trigger`(message)) {

            // Arguments vary depending on whether the main command name was used or not
            // If it was used then the standard way that argument are retrieved works
            val commandInfo = Parser.parse(message)
            // Otherwise start reading them from the first word
            commandInfo.args =
                if (commandInfo.commandName == command.name) commandInfo.args else commandInfo.beheaded.split(" ".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            command.set(event).invoke(commandInfo)
        }
    }

    fun destroy() {
        primaryListener listenTo channel
        Bot.jda!!.removeEventListener(this)
    }
}