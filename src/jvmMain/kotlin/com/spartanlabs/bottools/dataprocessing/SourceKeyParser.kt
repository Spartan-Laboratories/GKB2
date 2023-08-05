package com.spartanlabs.bottools.dataprocessing

import org.w3c.dom.Node

class SourceKeyParser(parseKeyDocument:String = "WebParseKeys") {
    private val reader: BaseXMLReader = BaseXMLReader()

    init {
        reader setDocument "$parseKeyDocument.xml"
    }

    fun getKeys(searchedName: String): KeySet {
        val keySet = KeySet()
        for(keyNode in reader.root.children()) {
             if(keyNode-"name" != searchedName) continue //Find the matching name node
             if(keyNode has "delegate") return getKeys(keyNode-"delegate")
             keyNode.children().forEach{
                when(it.nodeName.lowercase()){
                 "initial" -> keySet.addInitial(it.getValue())
                 "alternate" -> keySet.addAlternative(it.getValue())
                 "validation" -> keySet.addValidation(it.getValue())
                 "terminal" -> keySet.terminal = it.getValue()
                }
             }
             break
        }
        return keySet
    }
    public infix fun setDocument(name:String) = reader setDocument "$name.xml"

    private fun compare(nameNode: Node, searchedName: String) = reader.getValue(nameNode) == searchedName

    inner class KeySet internal constructor() {
        @JvmField
        var initial = ArrayList<String>()
        @JvmField
        var validation = ArrayList<String>()
        @JvmField
        var alternative = ArrayList<String>()
        @JvmField
        var terminal = ""
        internal infix fun addInitial(initial: String) =
            this.initial.add(initial)

        internal infix fun addValidation(validation: String) =
            this.validation.add(validation)

        internal infix fun addAlternative(alternative: String) =
            this.alternative.add(alternative)

    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val finder = SourceKeyParser()
            val keys = finder.getKeys("duration")
            println("Initial")
            for (s in keys.initial) println(s)
            println("Alternative")
            for (s in keys.alternative) println(s)
            System.out.printf("Terminal: %s\n", keys.terminal)
        }
    }
}