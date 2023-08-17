package com.spartanlabs.bottools.commands

import com.spartanlabs.bottools.main.Parser
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.springframework.context.support.ClassPathXmlApplicationContext

class CommandFactory {
    companion object{
        var trigger:String = "/"
        private lateinit var context:Event
        private val name:String
            get() = when{
                context is GenericCommandInteractionEvent
                        -> (context as GenericCommandInteractionEvent).name
                context.isMessageCommand()
                        -> (Parser parse (context as MessageReceivedEvent)).commandName
                else    -> ""
            }
        @JvmStatic
        infix fun getCommand(event: Event) = with(ClassPathXmlApplicationContext("commands.xml")){
            context = event
            getBean(name) as Command set event
        }
        private infix fun String.`starts with`(trigger:String) = startsWith(trigger)
        private fun Event.isMessageCommand():Boolean = this is MessageReceivedEvent && (context as MessageReceivedEvent).message.contentRaw `starts with` trigger
    }
}