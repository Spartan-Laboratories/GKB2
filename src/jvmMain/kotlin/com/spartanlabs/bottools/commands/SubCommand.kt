package com.spartanlabs.bottools.commands

import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData

abstract class SubCommand(name: String, protected val parent: Command) : Command(name) {
    init{ parent.addSubCommand(this)}
    override var guild          by parent::guild
    override val targetMember   by parent::targetMember
    override var channel        by parent::channel
    override var reply          by parent::reply
    val nestLevel
        get() = getNestLevel(parent)
    private var subcommandData      : SubcommandData?       = null
    private var subcommandGroupData : SubcommandGroupData?  = null
    private val options = ArrayList<Option>()

    init{
        generateSlashSubcommandData()
    }
    protected fun getParent(steps: Int) : Command =
        (steps - 1).let {
        if(it > 0) parent
        else getParent(it)
    }

    protected val parentCommand: Command
        get() = if (SubCommand::class.java.isAssignableFrom(parent.javaClass)) (parent as SubCommand).parentCommand else parent

    protected infix fun getNestLevel(command: Command): Int = if (!SubCommand::class.java.isAssignableFrom(command::class.java)) 1 else getNestLevel((command as SubCommand).parent) + 1

    override fun generateSlashCommandData() {}
    fun addOrganization(org: OrganizationCommand, suffix: String) {
        generateSlashSubcommandData(org, suffix)
        subcommandData!!.addOptions(options)
    }

    internal fun generateSlashSubcommandData(parent: Command = this.parent, name: String = this.name) {
        val name = name.lowercase()

        when(getNestLevel(parent)){
            1 -> parent.addToSlashCommandData(SubcommandGroupData(name, description!!).also{subcommandGroupData = it})
            2 -> SubcommandData(name, description!!).let {
                subcommandData = it
                (parent as SubCommand).addToSCGData(it)
            }
            else -> (parent as SubCommand).addToSlashCommandData(getOptionData(name))
        }
    }

    fun addToSCGData(subcommandData: SubcommandData)  = subcommandGroupData!!.addSubcommands(subcommandData)
    fun addToSCData(optionData: OptionData)          = subcommandData!!.addOptions(optionData)

    private fun getOptionData(name: String) = OptionData(OptionType.STRING, name, description!!)
    override infix fun getOption(optionName: String): OptionMapping? = parent.getOption(optionName)

    override fun addOption(option: Option) = this.also{
        when (val nestLevel = getNestLevel(parent)) {
            1 -> options.add(option)
            2 -> this + option
            else -> getParent(nestLevel - 2) + option
        }
    }

    override infix fun `reply with`(message: String) = parentCommand `reply with` message
    override operator fun plus(option:Option) = super.plus(option) as SubCommand
}