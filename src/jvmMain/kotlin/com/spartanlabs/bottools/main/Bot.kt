package com.spartanlabs.bottools.main

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.spartanlabs.bottools.botactions.update
import com.spartanlabs.bottools.commands.*
import com.spartanlabs.bottools.commands.CommandFactory.Companion.commandData
import com.spartanlabs.bottools.commands.CommandFactory.Companion.commands
import com.spartanlabs.bottools.dataprocessing.D
import com.spartanlabs.bottools.manager.MyLogger
import com.spartanlabs.bottools.manager.viewModel
import com.spartanlabs.generaltools.read
import com.spartanlabs.generaltools.time
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.interaction.command.*
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.springframework.context.annotation.ComponentScan
import java.time.format.DateTimeFormatter
import java.util.*

private val formatter = DateTimeFormatter.ofPattern("hh:mm:ss", Locale.getDefault())
private var log = MyLogger(Bot::class.java)
abstract class Bot {
    var eventsText = ""
    val formattedUptime = mutableStateOf("00:000")
    private val commandList by lazy{
        updateState("Creating Commands") { CommandFactory.commands }
    }
    private val pluginCommands  = CommandFactory.plugins.commands
    class UptimeThread(private var formattedUptime:MutableState<String>): Runnable {
        private val startTime = System.currentTimeMillis()
        private val uptime: Long
            get() = System.currentTimeMillis() - startTime
        val minutes
            get() = uptime / 1000 / 60
        val seconds
            get() = uptime / 1000 % 60
        override fun run(){
            while (true) {
                formattedUptime.value = "$minutes:$seconds"
                Thread.sleep(1000)
            }
        }
    }
    init {
        state = "Updating Server Database"
        log.time("server database update", D::onLaunchMemberCheck)
        state = "Finalizing"
        jda update with(arrayListOf<Command>()){
            addAll(commandList)
            addAll(pluginCommands)
            commandData
        }
        //commands.forEach { Bot.commands.put(it.name,it) }
        running = true
        createDefaultResponses()
        state = "Running!"
    }
    private fun createDefaultResponses(){
        responder newMessageReceivedAction              ::handleCommand
        responder newSlashCommandInteractionAction      ::handleCommand
        responder newMessageContextInteractionAction    ::handleCommand
        responder newUserContextInteractionAction       ::handleCommand
        responder newSlashCommandInteractionAction      ::appendEventInfo
    }
    private fun appendEventInfo(event:SlashCommandInteractionEvent){
        eventsText += "${event.fullCommandName} was just detected\n"
    }
    protected abstract fun applyDailyUpdate(currentDate: String?)

    companion object {
        internal var state by viewModel::generalState
        internal fun <T> updateState(actionName:String, action:()->T):T{
            state = actionName
            log.info(state)
            return action()
        }
        private var running = false
        private val keys = read("keys.txt")
        private val listener = updateState("Creating Listener!",::BotListener)
        val responder by listener::responder
        private val intents = EnumSet.allOf(GatewayIntent::class.java)
        val jda = updateState("Creating JDA!",::createJDA)
        var commands = HashMap<String, Command>(); protected set
        val commandActiveStatus = HashMap<String, Boolean>()
        private fun createJDA(): JDA {
            lateinit var jda: JDA
            log.time("JDA creation") {
                jda = JDABuilder.createDefault(keys[0], intents)
                    .addEventListeners(listener)
                    .enableCache(CacheFlag.VOICE_STATE)
                    .enableCache(CacheFlag.EMOJI)
                    .enableCache(CacheFlag.ONLINE_STATUS)
                    .enableCache(CacheFlag.FORUM_TAGS)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .build().awaitReady()
            }
            return jda
        }
        private val notifyComplete = updateState("Part 1 initialization complete"){}
        private infix fun handleCommand(event: MessageReceivedEvent) {
            if (Parser `starts with trigger` event.message.contentRaw)
                CommandFactory.getCommand(event)
        }
        private infix fun handleCommand(event: SlashCommandInteractionEvent) = CommandFactory.getCommand(event)()
        private infix fun handleCommand(event: UserContextInteractionEvent) = CommandFactory.getCommand(event)()
        private fun handleCommand(event: MessageContextInteractionEvent) = CommandFactory.getCommand(event)()
    }
}