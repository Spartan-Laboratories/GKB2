package com.spartanlabs.bottools.commands

import com.spartanlabs.bottools.botactions.contains
import com.spartanlabs.bottools.botactions.say
import com.spartanlabs.bottools.main.Bot
import net.dv8tion.jda.api.interactions.commands.OptionType

class HelpCommand : Command("help") {
    override val brief = "See what this bot can do"
    override val details = "Gives a detailed description of this Bot's capabilities"
    init {
        info("starting help command initialization")
        makeInteractive()
        slashCommandData.addOption(OptionType.STRING, "commandname", "the name of the command that you need help with", false)
        //+ Option(name = "command name", description = "the name of the command which you need help with", required = true)
    }

    override fun invoke(args: Array<String>){
        reply!!>"Hello, I am ${jda.selfUser.name}"
        if (args.isEmpty()) executeGenericVersion()
        else executeCommandNameVersion(args)
    }

    private fun executeGenericVersion() {
        eb = eb.setAuthor("Hello, I am TrumpBot")
            .setTitle("Here is what I can do:")
        createFields()
        sendEmbed()
    }

    private fun executeCommandNameVersion(commandAlias: Array<String>) {
        if (!Bot.commands.keys.contains(commandAlias[0])) {
            say("No such command exists")
            executeGenericVersion()
            return
        }
        Bot.commands[getOption("commandname")!!.asString.split(" ")[0]]?.apply{
            this.channel = this@HelpCommand.channel
            help(getSecondaryArgs(commandAlias))
        } ?: Unit.also { Bot say "no such command was found" in channel }
    }
    private fun createFields() = Bot.commands.keys.forEach { keyName ->
        eb = eb.addField("`/$keyName`", Bot.commands[keyName]!!.helpMessage, false)
    }
}