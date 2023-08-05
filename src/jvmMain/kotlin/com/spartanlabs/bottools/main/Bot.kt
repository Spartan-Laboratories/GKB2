package com.spartanlabs.bottools.main

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.spartanlabs.bottools.commands.Command
import com.spartanlabs.bottools.commands.HelpCommand
import com.spartanlabs.bottools.dataprocessing.B
import com.spartanlabs.bottools.dataprocessing.D
import com.spartanlabs.bottools.main.Parser.CommandContainer
import com.spartanlabs.bottools.botactions.contains
import com.spartanlabs.bottools.botactions.say
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.function.Predicate

private val formatter = DateTimeFormatter.ofPattern("hh:mm:ss", Locale.getDefault())
private var log = LoggerFactory.getLogger(Bot::class.java)
abstract class Bot(var tokenState: MutableState<Boolean>){
    var centralProcess: CentralProcess?
    val formattedUptime = mutableStateOf("00:000")
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
        /* In case I want to see logger status messages
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
	    StatusPrinter.print(lc);
	    */
        log.info("Begun bot initialization")
        running = true
        //Thread(UptimeThread(formattedUptime)).start()
        log.info("Uptime thread created")

        // My systems
        console = Console()
        console.start()
        log.info("Console created")

        // Bot setup
        initializeBotExistence()
        addInteractionResponses()
        Parser addTrigger "/"

        // My Systems again
        D.updateServerDatabase() //Must be after bot and console initialization
        log.info("The server database has been updated")

        //Commands
        createCommand(HelpCommand())
        log.info("Help command has been created")
        listCommands()
        log.info("All commands have been created")
        createAllSlashCommands()

        centralProcess = CentralProcess.apply{
            start(::applyDailyUpdate)
        }

        tokenState.value = true
    }

    private fun initializeBotExistence() {
        log.info("Starting JDA creation")
        try {
            BufferedReader(FileReader(File("key.txt"))).use { keyReader ->
                for (i in keys.indices) keys[i] = keyReader.readLine()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            log.error("An unknown error has occurred while parsing the bot's key file.")
        }
        log.info("Successfully read bot key.\nStarting jda building.")
        try {
            jda = JDABuilder.createDefault(keys[0], EnumSet.allOf(GatewayIntent::class.java))
                .addEventListeners(BotListener().also { listener = it })
                //.enableCache(CacheFlag.)
                .enableCache(CacheFlag.VOICE_STATE)
                .enableCache(CacheFlag.EMOJI)
                .enableCache(CacheFlag.ONLINE_STATUS)
                .enableCache(CacheFlag.FORUM_TAGS)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .build().awaitReady()
            //jda.getPresence().setGame(Game.of(Game.GameType.DEFAULT, "/help for info"));
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: Exception) {
            log.error("An unknown error has occurred with the bot builder.")
            e.printStackTrace()
        }
        log.info("Completed JDA creation")
    }
    private fun addInteractionResponses(){
        responder newMessageReceivedAction              ::handleCommand
        responder newSlashCommandInteractionAction      ::handleCommand
        responder newMessageContextInteractionAction    ::handleCommand
        responder newUserContextInteractionAction       ::handleCommand
        log.info("The default interaction responses have been created")
    }

    protected abstract fun listCommands()
    object CentralProcess: Runnable {
        var lastDailyUpdate: String
            get() = B-"lastdailyupdate"
            set(value) = B/"lastdailyupdate" + value
        val currentDate: String
            get() = Instant.now().atOffset(ZoneOffset.ofHours(-6))
                .let{"${it.dayOfMonth}/${it.monthValue}"}

        private val log = LoggerFactory.getLogger(CentralProcess::class.java)
        private const val second = 1000L
        private const val minute = 60 * second
        private const val hour = 60 * minute
        private const val day = 24 * hour
        private lateinit var externalUpdate: (date:String) -> Unit
        private lateinit var secondlyUpdate: () -> Unit
        fun start(externalUpdate:(date:String)->Unit) = Thread(this).also {this.externalUpdate=externalUpdate }.start()
        fun startEverySecondUpdate(action:()->Unit) = Thread(this).also {this.secondlyUpdate = action }.start()

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
        private fun doEverySecond(everySecondUpdate:()->Unit){
            do{
                try {
                    everySecondUpdate.invoke()
                    Thread.sleep(second)
                }catch (e:InterruptedException){
                    log.error("An error occured in the doEverySecond() method of the CentralProcess")
                    e.printStackTrace()
                }
            }while (true)
        }
    }
    protected abstract fun applyDailyUpdate(currentDate: String?)

    companion object {
        var jda: JDA? = null
        var commands = HashMap<String, Command>()
            protected set
        private val interactions = ArrayList<CommandData>()
        val responder by lazy { listener.responder }

        //var commandNameList = ArrayList<String>()
        var keys = arrayOfNulls<String>(3)
        private lateinit var console: Console

        private lateinit var listener: BotListener
        private var running = false
        val commandActiveStatus = HashMap<String, Boolean>()
        private fun createAllSlashCommands() = jda!!.updateCommands().addCommands(interactions).complete()

        private fun handleCommand(commandText: CommandContainer, event: MessageReceivedEvent) {
            val commandName = commandText.commandName
            val channel = event.channel
            if (commands.containsKey(commandName)) {
                try {
                    commands[commandName]!!.setEvent(event).invoke(commandText)
                } catch (ipe: InsufficientPermissionException) {
                    ipe.printStackTrace()
                    Bot say "Insufficient permissions to perform this command" in channel
                } catch (e: Exception) {
                    e.printStackTrace()
                    Bot say "An error occured while trying to execute this command" in channel
                }
            } else {
                Bot say "This command does not exist" in channel
            }
        }

        fun stop() {
            jda!!.shutdown()
            console.interrupt()
            running = false
        }

        private infix fun handleCommand(event: MessageReceivedEvent) {
            if (Parser `starts with trigger` event.message.contentRaw)
                handleCommand(Parser parse event, event)
        }
        private infix fun handleCommand(event: SlashCommandInteractionEvent) = commands[event.name]!!(event)

        private infix fun handleCommand(event: UserContextInteractionEvent) = commands[event.name]!!(event)

        private fun handleCommand(event: MessageContextInteractionEvent) {
            val name = event.name.lowercase(Locale.getDefault()).replace(" ".toRegex(), "")
            commands[name]!!(event)
        }

        infix fun createCommand(command: Command) {
            commands[command.name] = command
            commandActiveStatus[command.name] = true
            if (command.isInteractible) interactions.add(command.commandData)
        }

        fun getCommand(name: String): Command = commands[name]!!

    }
}
fun <T>evaluateList(list:Iterable<T>, validator: Predicate<T>):Boolean{
    for(t in list)if(validator.test(t))return true
    return false
}