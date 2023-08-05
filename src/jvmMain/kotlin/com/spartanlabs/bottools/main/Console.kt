package com.spartanlabs.bottools.main

import com.spartanlabs.bottools.dataprocessing.D
import com.spartanlabs.bottools.main.Parser.CommandContainer
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Member
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*

class Console : Thread() {
    private val log = LoggerFactory.getLogger(Console::class.java)
    private lateinit var commandInfo: CommandContainer
    private var args = ArrayList<String>()
    private lateinit var commandName : String
    private val scanner = Scanner(System.`in`)
    private lateinit var channel : String
    private lateinit var guild : String
    private lateinit var user : Member
    private var running = false
    override fun run() {
        running = true
        log.info("The console is active")
        while (running) try {
            scan()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        log.info("Console thread has terminated")
    }

    @Throws(IOException::class, ImproperConsoleCommandUseException::class, InterruptedException::class)
    private fun scan() {
        sleep(100)
        if (scanner.hasNext()) {
            val command = scanner.nextLine()
            parse(command)
        }
    }

    @Throws(IOException::class, ImproperConsoleCommandUseException::class)
    private fun parse(command: String): Boolean {
        commandInfo = Parser.parse(command)
        args.addAll(commandInfo.args)
        name = commandInfo.commandName
        return when (name) {
            "set" -> {
                if (commandInfo!!.args == null || args.size == 0) log.info("Invalid command arguments")
                else if (args[0] == "channel")
                    if (args.size < 2 || args[1] == null || args[1] == "") log.info("No channel type specified")
                    else if (commandInfo!!.args[1] == "text")
                        if (commandInfo!!.args.size < 3 || commandInfo!!.args[2] == null || commandInfo!!.args[2] == "") log.info("No text channel specified")
                        else channel = commandInfo!!.args[2]
                    else if (commandInfo!!.args[1] == "voice") Bot.jda!!.getVoiceChannelsByName(commandInfo!!.args[2]!!, true)[0]
                    else log.info("Improper channel type")
                else if (commandInfo!!.args[0] == "points" || commandInfo!!.args[0] == "promotionpoints")
                    if (commandInfo!!.args.size < 2) log.info("Invalid arguments")
                true
            }

            "showchat" -> {
                if (args.size != 1) throw ImproperConsoleCommandUseException(
                    "showchat",
                    "Incorrect number of arguments. Required: 1. Provided: " + args.size
                )
                val channel = user!!.user.openPrivateChannel().complete()
                val history = channel.history
                val size = args[0]!!.toInt()
                val messages = history.retrievePast(size).complete()
                var i = messages.size
                while (i > 0) {
                    log.info(messages[i - 1].contentRaw)
                    i--
                }
                true
            }

            "update" -> {
                D.updateServerDatabase()
                true
            }

            "listservers" -> {
                for (g in Bot.jda!!.guilds) log.info(g.name)
                true
            }

            "test" -> {
                Bot.jda!!.presence.setStatus(OnlineStatus.IDLE)
                //TODO Botmain.jda.getPresence().setGame(Game.playing("with himself"));;
                Bot.jda!!.presence.setStatus(OnlineStatus.ONLINE)
                true
            }

            "addtag" -> {
                if (args.size == 3) D.addTag(
                    args[0],
                    args[1],
                    args[2]
                ) else if (args.size == 2) D.addTag(
                    args[0], args[1], ""
                )
                true
            }

            "removetag" -> {
                D.removeTag(args[0], args[1])
                true
            }

            "print" -> {
                log.info(args[0])
                true
            }

            "checkpersoncontents" -> {
                val guild = Bot.jda!!.getGuildsByName(this.guild, true)[0]
                val person = guild.getMembersByEffectiveName(args[0],true).get(0)
                (D/guild/person).readAll().forEach { out("${it.nodeName}: ${it.nodeValue}") }
                true
            }

            "sayemoji" -> {
                val emoji =  //guild.getEmotesByName(args[0], true).get(0).getAsMention();
                    Bot.jda!!.getEmojisByName("dota", true)[0].asMention
                //TODO guild.getDefaultChannel().sendMessage(emoji).complete();
                true
            }

            else -> commandNotFoundMessage()
        }
    }

    internal inner class ImproperConsoleCommandUseException(var commandName: String, var issue: String) :
        Exception() {
        override fun printStackTrace() {
            log.info("The command " + commandName + "was used incorrectly")
            System.err.println(issue)
            super.printStackTrace()
        }
    }

    fun out(text: String?) {
        println(text)
    }

    private fun commandNotFoundMessage(): Boolean {
        log.info("The entered data was not a generic console command")
        return false
    }
}