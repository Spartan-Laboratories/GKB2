package com.spartanlabs.bottools.commands

import kotlin.reflect.KMutableProperty

open class PropertyCommand(private val property:KMutableProperty<String?>, parent:Command, valueType:String="string"):SubCommand(property.name, parent) {
    protected val propertyName= property.name.lowercase().replace("_","")
    private val getterName  = "get$propertyName"
    private val setterName  = "set$propertyName"
    final override val brief = "View of change $propertyName"
    final override val details = "Allows you to see what is the current value of $propertyName or to change it."
    private val setterOption = Option(valueType, "value", "what you want to set $propertyName to", true)
    protected open val getCommand:SubCommand = MethodCommand(::reply, getterName, "shows the current value of $propertyName", parent)
    protected open val setCommand:SubCommand = MethodCommand(::write, setterName, "sets $propertyName to the given value", parent) + setterOption
    private var value:String?
        get() =     property.getter.call()
        set(value) =property.setter.call(value)
    init {
        parent.get and propertyName becomes getterName
        parent.set and propertyName becomes setterName
    }
    override fun invoke(args: Array<String>) {}
    fun `with getters`(vararg getters:String){
        getters.forEach{
            (parent)
        }
    }

    private fun reply(message:Array<String>){
        `reply with`(value?:"")
    }
    private fun write(value:Array<String>){
        `reply with`("$propertyName has been set to: ${value[0]}")
        this.value = value[0]
    }
}
class TargetablePropertyCommand(property:KMutableProperty<String?>, parent:Command):PropertyCommand(property, parent){
    override val getCommand = super.getCommand + Option("user", "person", "the server member whose $propertyName you want to see", false)
    override val setCommand = super.setCommand + Option("user", "person", "the server member for whom you would like to change the $propertyName value", false)
}