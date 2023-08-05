package com.spartanlabs.bottools.commands

import com.spartanlabs.bottools.commands.OrganizationCommand.orgData
import kotlin.reflect.KMutableProperty

open class PropertyCommand(private val property:KMutableProperty<String?>, parent:Command, valueType:String="string"):SubCommand(property.name, parent) {

    protected val propertyName = property.name.lowercase()
    final override val brief = "View of change $propertyName"
    final override val details = "Allows you to see what is the current value of $propertyName or to change it."

    protected open val getCommand:SubCommand = MethodCommand(::reply, "get$propertyName", "shows the current value of $propertyName", parent)
    protected open val setCommand:SubCommand = MethodCommand(::write, "set$propertyName", "sets $propertyName to the given value", parent) +
            Option(valueType, "value", "what you want to set $propertyName to", true)
    private var value:String?
        get() = property.getter.call()
        set(value) = property.setter.call(value)
    override fun invoke(args: Array<String>) {}
    init {
        if(this::class == PropertyCommand::class)
            add()
    }
    protected fun add(){
        parent.get + orgData(propertyName, "get$propertyName")
        parent.set + orgData(propertyName, "set$propertyName")
    }
    private fun reply(message:Array<String>){
        reply(value?:"")
    }
    private fun write(value:Array<String>){
        reply("$propertyName has been set to: ${value[0]}")
        this.value = value[0]
    }
}
class TargetablePropertyCommand(property:KMutableProperty<String?>, parent:Command):PropertyCommand(property, parent){
       override val getCommand = super.getCommand + Option("user", "person", "the server member whose $propertyName you want to see", false)
       override val setCommand = super.setCommand + Option("user", "person", "the server member for whom you would like to change the $propertyName value", false)
        init{add()}
}