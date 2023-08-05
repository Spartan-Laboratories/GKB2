package com.spartanlabs.bottools.dataprocessing

import org.w3c.dom.Node
import org.w3c.dom.NodeList

interface XMLReader {
    val root: Node
    fun write()
}
private fun Node.stepOver() = if(nextSibling == null) null else nextSibling.nextSibling
private fun Node.stepDown() = if(firstChild == null) null else firstChild.nextSibling
fun Node.children() :List<Node>{
    val list = ArrayList<Node>()
    for(i in 0 until childNodes.length)
        if(childNodes.item(i).nodeType == Node.ELEMENT_NODE)
            list.add(childNodes.item(i))
    /*
    var childNode = firstChild//stepDown()
    while(childNode!=null){
        list.add(childNode)
        childNode = childNode.nextSibling//stepOver()
        if(childNode!=null && childNode.nodeType == Node.TEXT_NODE)
            childNode = childNode.nextSibling
    }

     */
    return list
}

infix fun Node.has(childName : String) : Boolean {
    for (i in 0 until childNodes.length) if (childNodes.item(i).nodeName == childName) return true
    return false
}
infix fun Node.getChild(name :String) : Node? {
    require(this has name)
    for(node in children())
        if(node.nodeName.lowercase() == name.lowercase())
            return node
    return null
}
infix fun Node.then(nextFunction : (Node) -> String)    = nextFunction.invoke(this)
infix fun Node.then(nextFunction: (Node) -> NodeList) = nextFunction.invoke(this)
fun Node.getValue(): String = firstChild.nodeValue
infix fun Node.setValue(newValue : String) {
    textContent = newValue
}
infix fun Node.containing(content : String) = this setValue content
operator fun Node.get(childName : String) = this getChild childName
operator fun Node.div(childName: String) = (this getChild childName)!!

operator fun Node.minus(childWithValue: String) = (this getChild childWithValue)!!.getValue()
//operator fun Node.set(childNode:String, newValue: String) = this[childNode] setValue newValue