package com.spartanlabs.bottools.commands

import org.slf4j.LoggerFactory
import kotlin.reflect.KProperty
import kotlin.reflect.KVisibility
import com.spartanlabs.bottools.dataprocessing.KotGDP.DataAccessPoint as DAP

class DataAccessCommand(private val property: KProperty<DAP>, val parent:Command, val customName:String = property.name, valueType:String="string", getter:String="get", setter:String="set")
{
    val log = LoggerFactory.getLogger(DataAccessCommand::class.java)!!
    init {
        require(property.visibility == KVisibility.PUBLIC) {
            ("\nUnable to form a DataAccessCommand due to visibility of the given property: ${property.name}\n" +
            "visibility is: ${property.visibility}, required: ${KVisibility.PUBLIC}").also { log.error(it) }
        }
    }
    private var path = ""
    private val parsedPath: List<String>
        get() = path.split("/")
    var data:String?
        get(){
            var dap = property.getter.call()
            for (i in parsedPath.indices){
                if (i == parsedPath.size - 1)
                    break
                dap /= parsedPath[i]
            }
            return dap - parsedPath[parsedPath.size - 1]
        }
        set(value) {
            var dap = property.getter.call()
            for (p in parsedPath)
                dap /= p
            dap + value!!
        }
    @Suppress("Unused")
    val trueCommand = object:PropertyCommand(::data, parent, customName, valueType, getter, setter){
        override val getCommand = super.getCommand  + Option("string", "path","what do you want to get", true)
        override val setCommand = super.setCommand  + Option("string", "path", "what do you want to set", true) //+
                                                      //Option("string", "new-value", "what do you want to set it to?", true)
        init { add() }
        override fun replyWithValue(message:Array<String>){
            path = getOption("path")!!.asString
            super.replyWithValue(message)
        }
        override fun write(value:Array<String>){
            path = getOption("path")!!.asString
            super.write(value)
        }
    }
}