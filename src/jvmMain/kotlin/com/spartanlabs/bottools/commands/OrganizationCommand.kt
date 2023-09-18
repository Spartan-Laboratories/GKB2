package com.spartanlabs.bottools.commands

import com.spartanlabs.bottools.main.Parser.CommandContainer


class OrganizationCommand(name: String, parent:Command) : SubCommand(name, parent) {
    override var brief = "This should not be visible"
    override var details = brief
    private val orgCommands = HashMap<String, String>()
    data class orgData(val alias: String, val trueName: String)
    init {
        subCommandRequired = true
    }

    override fun invoke(commandData: CommandContainer){
        commandData.args[0] = orgCommands[commandData.args[0]]!!
        parent(commandData)
    }

    override fun invoke(args: Array<String>) {}

    fun addCommand(suffix: String, command: String = suffix): OrganizationCommand {
        orgCommands[suffix] = command
        if(command !in parent.subCommands.keys)
            throw IllegalArgumentException("Organization attempted to add subcommand: $command, which is not a valid subcommand of it's parent: ${parent.name}")
        parent.subCommands[command]!!.addOrganization(this, suffix)
        return this
    }
    fun MethodCommand(extension:Pair<String,String>, brief:String, onExecute: (Array<String>) -> Unit) =
        parent.MethodCommand(extension.second,brief,onExecute)
        .also{this and extension.first becomes extension.second}
    infix fun and(suffix: String) = OrganizationCommandConnectionPoint(this,suffix)
    //override infix operator fun plus(suffix: String) = this and suffix
    operator fun plus(command: orgData) = addCommand(command.alias, command.trueName)
    class OrganizationCommandConnectionPoint(val parent:OrganizationCommand, val suffix: String){
        infix fun `leads to`(commandName:String) = parent.addCommand(suffix, commandName)
        infix fun becomes(commandName: String) = this `leads to` commandName
    }
}