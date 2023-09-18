package com.spartanlabs.bottools.main

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.spartanlabs.bottools.botactions.update
import com.spartanlabs.bottools.commands.Command
import com.spartanlabs.bottools.commands.CommandFactory
import com.spartanlabs.bottools.commands.CommandFactory.Companion.commandData
import com.spartanlabs.bottools.commands.CommandFactory.Companion.commands
import com.spartanlabs.bottools.dataprocessing.B
import com.spartanlabs.bottools.dataprocessing.D
import com.spartanlabs.bottools.manager.MyLogger
import com.spartanlabs.bottools.manager.viewModel
import com.spartanlabs.generaltools.read
import com.spartanlabs.generaltools.time
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.ComponentScan
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

private val formatter = DateTimeFormatter.ofPattern("hh:mm:ss", Locale.getDefault())
private var log = MyLogger(Bot::class.java)
@ComponentScan("com.spartanlabs")
abstract class Bot(){
    var eventsText = ""
    var centralProcess: CentralProcess?
    val formattedUptime = mutableStateOf("00:000")
    private val commandList        = updateState("Creating Commands") { CommandFactory.commands.invoke() }
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
        log.time("server database update", D::updateServerDatabase)
        state = "Finalizing"
        jda update with(arrayListOf<Command>()){
            println(commandList.toString())
            println(pluginCommands.toString())
            addAll(commandList)
            addAll(pluginCommands)
            commandData
        }
        //commands.forEach { Bot.commands.put(it.name,it) }
        running = true
        createDefaultResponses()
        centralProcess = CentralProcess.apply{
            start(::applyDailyUpdate)
        }
        state = "Running!"
        //tokenState.value = true
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

    object CentralProcess: Runnable {
        var lastDailyUpdate: String
            get()       = B-"lastdailyupdate"
            set(value)  = B/"lastdailyupdate" + value
        val currentDate: String
            get() = Instant.now().atOffset(ZoneOffset.ofHours(-6))
                .let{"${it.dayOfMonth}/${it.monthValue}"}

        private val log = LoggerFactory.getLogger(CentralProcess::class.java)
        private const val second = 1000L
        private const val minute = 60 * second
        private const val hour = 60 * minute
        private const val day = 24 * hour
        private lateinit var externalUpdate: (date:String) -> Unit
        fun start(externalUpdate:(date:String)->Unit) = Thread(this).also {this.externalUpdate=externalUpdate }.start()

        override fun run() = doDaily {
            log.info("The last daily update was on $lastDailyUpdate")
            log.info("The current date is: $currentDate")
            externalUpdate.invoke(currentDate)
            lastDailyUpdate = currentDate
        }
        private fun doDaily(dailyUpdate:()->Unit){
            do{
                try{
                    dailyUpdate.invoke()
                    Thread.sleep(day)
                }catch (e:InterruptedException){
                    log.error("The central update process has terminated")
                    e.printStackTrace()
                }
            }while (true)
        }
    }
    protected abstract fun applyDailyUpdate(currentDate: String?)

    companion object {
        internal var state by viewModel::generalState
        internal fun start(){
            listener;responder;jda;notifyComplete
        }
        private fun <T> updateState(actionName:String, action:()->T):T{
            state = actionName
            log.info(state)
            return action()
        }
        private var running = false
        private val keys = read("keys.txt")
        private val listener by lazy{
            updateState("Creating Listener!",::BotListener)
        }
        val responder by lazy{listener.responder}
        private val intents = EnumSet.allOf(GatewayIntent::class.java)
        val jda by lazy{
            updateState("Creating JDA!",::createJDA)
        }
        var commands = HashMap<String, Command>()
            protected set
        val commandActiveStatus = HashMap<String, Boolean>()
        private val notifyComplete by lazy{
            updateState("Part 1 initialization complete"){}
        }
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
        private infix fun handleCommand(event: MessageReceivedEvent) {
            if (Parser `starts with trigger` event.message.contentRaw)
                CommandFactory.getCommand(event)
        }

        private infix fun handleCommand(event: SlashCommandInteractionEvent) = CommandFactory.getCommand(event)()

        private infix fun handleCommand(event: UserContextInteractionEvent) = CommandFactory.getCommand(event)()

        private fun handleCommand(event: MessageContextInteractionEvent) = CommandFactory.getCommand(event)()
    }
}