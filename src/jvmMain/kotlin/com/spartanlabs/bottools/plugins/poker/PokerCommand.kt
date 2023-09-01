package com.spartanlabs.bottools.plugins.poker

import com.spartanlabs.bottools.commands.Command
import com.spartanlabs.bottools.commands.MethodCommand
import com.spartanlabs.bottools.commands.Option
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel

class PokerCommand() : Command("poker") {
    override val brief = "allows you to play texas hold em' poker"
    override val details = "challenge other people in the server to a game of poker"
    override fun invoke(args: Array<String>) {
        TODO("Not yet implemented")
    }

    init {
        MethodCommand(this,"createtable", "creates a new table for people to play at"){
            (channel as TextChannel).parentCategory!!.createTextChannel(getOption("table name")?.asString?:"table1")
        } + Option("string", "tablename", "name the table you are creating",false)
    }
}