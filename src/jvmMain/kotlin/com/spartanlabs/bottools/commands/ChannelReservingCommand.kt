package com.spartanlabs.bottools.commands

import com.spartanlabs.bottools.main.SingleChannelListener
import net.dv8tion.jda.api.entities.Guild

abstract class ChannelReservingCommand protected constructor(commandName: String) : Command(commandName) {
    override val detailStatement =
        """"This command is able to reserve a Text Channel for itself. (Legacy only).
            When a command reserves a channel its subcommands can be used directly, 
            however, no other top level commands may be used in that channel.
            To reserve a channel use /$commandName reserve
        """.trimIndent()
    private val reserved = HashMap<Guild, SingleChannelListener>()
    init {
        SCReserve()
        SCUnreserve()
    }

    abstract override fun invoke(args: Array<String>)
    protected fun preExecute() {}
    protected inner class SCReserve : SubCommand("reserve", this@ChannelReservingCommand) {
        override val brief = "Reserves this text channel to be used exclusively by this command."
        override val details = """
                After reserving, subcommands can be called directly.
                Example: `/play` instead of`/music play`
                """.trimIndent()
        init {
        }

        public override fun invoke(args: Array<String>){
            if (reserved.containsKey(guild))
                say("Unable to reserve this channel, a reservation for this command already exists in this server")
            else {
                reserved[guild] = SingleChannelListener(channel, this@ChannelReservingCommand)
                say("This channel is now reserved for the purposes of the command: $name\nYou can now use its sub-commands directly")
            }
        }
    }

    protected inner class SCUnreserve : SubCommand("unreserve", this@ChannelReservingCommand) {
        override val brief = "Removes the reservation for this channel"
        override val details = ""
        public override fun invoke(args: Array<String>){
            reserved[guild]!!.destroy()
            reserved.remove(guild)
            say("channel is no longer reserved")
        }
    }
}