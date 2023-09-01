package com.spartanlabs.bottools.commands

import com.spartanlabs.bottools.main.Parser
import com.spartanlabs.bottools.plugins.Plugin
import com.spartanlabs.bottools.plugins.Plugins
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import org.slf4j.LoggerFactory
import org.springframework.context.support.ClassPathXmlApplicationContext

class CommandFactory {
    companion object{
        private val log = LoggerFactory.getLogger(CommandFactory::class.java)
        private val applicationContext = ClassPathXmlApplicationContext("internals.xml","plugins.xml","commands.xml")
        @JvmStatic val commands = applicationContext.getBeansOfType(Command::class.java).values.filter{it.active }
        /*
        @JvmStatic val plugins  = (applicationContext.getBean("plugins") as List<String>).map { Plugins[it] }.filterNotNull()
        @JvmStatic val List<Plugin>.commands
            get() = arrayListOf<Command>().apply { plugins.forEach { this.addAll(it()) } }

         */
        var trigger:String = "/"
        private lateinit var event:Event
        private val name:String
            get() = when{
                event is GenericCommandInteractionEvent
                        -> (event as GenericCommandInteractionEvent).name
                event.isMessageCommand()
                        -> (Parser parse (event as MessageReceivedEvent)).commandName
                else    -> ""
            }
        @JvmStatic internal infix fun getCommand(event: Event):Command{
            log.info("Starting command creation")
            this.event = event
            log.info("The event is of type: $event")
            val command = applicationContext.getBean(name) as Command
            log.info("The command name was detected as $name")
            log.info("The command that was created is: $command")
            return command set event
        }
        internal val List<Command>.commandData:List<CommandData>
            get() = map{it.commandData}
        private infix fun String.`starts with`(trigger:String) = startsWith(trigger)
        private fun Event.isMessageCommand():Boolean = this is MessageReceivedEvent && (event as MessageReceivedEvent).message.contentRaw `starts with` trigger
    }
}
internal fun Event.toString():String{
    return javaClass.toString()
}