package com.spartanlabs.bottools.commands

import kotlin.reflect.KMutableProperty

open class PropertyCommand(private val property:KMutableProperty<String?>, parent:Command, valueType:String="string", getter:String="get", setter:String="set")
    :SubCommand(property.name, parent) {
    protected val propertyName= property.name.lowercase().replace("_","")
    private val getterName  = "get$propertyName"
    private val setterName  = "set$propertyName"
    final override val brief = "View or change $propertyName"
    final override val details = "Allows you to see what is the current value of $propertyName or to change it."
    private val setterOption = Option(valueType, "value", "what you want to set $propertyName to", true)
    protected open val getCommand:SubCommand = MethodCommand(::reply, getterName, "shows2 the current value of $propertyName", parent)
    protected open val setCommand:SubCommand = MethodCommand(::write, setterName, "sets $propertyName to the given value", parent) + setterOption
    private var value:String?
        get() =     property.getter.call()
        set(value) =property.setter.call(value)
    init {
        if(this::class == PropertyCommand::class)
            add()
    }
    protected fun add(){
        `add getter`(parent.get)
        `add setter`(parent.set)
    }
    override fun invoke(args: Array<String>) {}
    infix fun `using getter`(organizationCommand: OrganizationCommand) = this.apply{
        parent.get.subCommands
    }
    fun `with getters`(vararg getters:String) = getters.forEach(::`add getter`)
    private infix fun `add getter`(customGetter:String) = this `add getter` parent + customGetter
    private infix fun `add getter`(organizationCommand: OrganizationCommand) = organizationCommand and propertyName becomes getterName
    private infix fun `add setter`(organizationCommand: OrganizationCommand) = organizationCommand and propertyName becomes setterName

    private fun reply(message:Array<String>){
        reply > value!!.ifBlank{"$propertyName has not yet been set"}
    }
    private fun write(value:Array<String>){
        reply > "$propertyName has been set to: ${value[0]}"
        this.value = value[0]
    }
}
class TargetablePropertyCommand(property:KMutableProperty<String?>, parent:Command):PropertyCommand(property, parent){
    override val getCommand = super.getCommand + Option("user", "person", "the server member whose $propertyName you want to see", false)
    override val setCommand = super.setCommand + Option("user", "person", "the server member for whom you would like to change the $propertyName value", false)
    init { add() }
}