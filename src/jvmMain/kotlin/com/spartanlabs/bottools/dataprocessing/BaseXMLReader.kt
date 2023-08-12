package com.spartanlabs.bottools.dataprocessing

import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.SAXException
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.*
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * A wrapper around an existing XML reading library
 * @author Spartak
 */
open class BaseXMLReader : XMLReader {
    private val log = LoggerFactory.getLogger(BaseXMLReader::class.java)
    private val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    private val tr = TransformerFactory.newInstance().newTransformer()
    private var doc: Document? = null
    private lateinit var docName: String
    private lateinit var currentNode : Node

    /**
     * Creates the XML reader
     */
    init {
        //debug = true;
        try {
            tr.apply{
                setOutputProperty(OutputKeys.INDENT, "yes")
                setOutputProperty(OutputKeys.METHOD, "xml")
                setOutputProperty(OutputKeys.ENCODING, "UTF-8")
                setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
            }
        } catch (e: ParserConfigurationException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: TransformerConfigurationException) {
            e.printStackTrace()
        } catch (e: TransformerFactoryConfigurationError) {
            e.printStackTrace()
        }
    }

    /**
     * Sets the document that is to be read and written to.
     *
     * @param pathName the name of the document including the file path from the root folder (project folder)
     */
    internal open infix fun setDocument(pathName: String) : Node {
        docName = pathName
        try {
            //db = dbf.newDocumentBuilder()
            doc = db.parse(pathName)
        } catch (e: FileNotFoundException) {
            log.error(e.message)
            e.printStackTrace()
            throw FileNotFoundException("Could not find specified file")
        } catch (e: SAXException) {
            log.error("XML reader was unable to open file: $pathName")
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: ParserConfigurationException) {
            e.printStackTrace()
        }
        return root
    }

    override val root: Node
        get() = doc!!.documentElement

    /**
     * Gets the sub-node of this node
     * @param node - the parent node
     * @return the child node
     */
    internal infix fun stepDown(node: Node) = if(node.firstChild == null) null else node.firstChild.nextSibling

    /**
     * Gets the next element of the same hierarchy level
     * @param node - the current node
     * @return the next node
     */
    internal infix fun stepOver(node: Node) = if(node.nextSibling == null) null else node.nextSibling.nextSibling
    internal fun stepOver(node: Node, times: Int) : Node = if (times == 0) node else stepOver(stepOver(node)!!, times - 1)

    /**
     * Get the node of the hierarchically higher level that contains this node
     * @param node - the current node
     * @return the parent node
     */
    internal infix fun stepOut(node: Node): Node = node.parentNode

    internal infix fun getValue(node: Node): String = try {
        node.firstChild.nodeValue
    } catch (e: NullPointerException) {
        if(node != null)    log.error("Could not read value of the node ${node.nodeName}")
        else log.error("Could not read the value of the given node.")
        ""
    }

    /**
     * The path string of the document that the reader is currently parsing
     * @return - The name of the document that is currently in use (the one that was set by [.setDocument]
     */
    private fun currentDocument(): String = this.docName

    /**
     * Writes a new element to the file
     * @param root - the element that will contain the new element
     * @param tagName - the name of the tag that is being created
     * @param content - the text value inside the tag that is being created
     * @return - The [Element] that was just created by this method
     */
    @JvmOverloads
    internal fun newChild(root: Node = this.root, tagName: String, content: String = ""): Element {
        val addition = doc!!.createElement(tagName.replace(" ", ""))
        addition.appendChild(doc!!.createTextNode(content))
        root.appendChild(addition)
        val oldRoot = root
        root.parentNode.replaceChild(root, oldRoot)
        write()
        return addition
    }
    infix fun at(node : Node) : BaseXMLReader{
        currentNode = node
        return this
    }
    infix fun create(node : String) : Node = newChild(currentNode, node )
    infix fun createCategory(node :String) = (this create node).also { currentNode = it }
    @JvmOverloads
    private fun removeTag(root: Node = this.root, child: Node?) {
        if (child != null)
            root.removeChild(child)
        write()
    }

    @JvmOverloads
    fun removePossibleTag(root: Node? = this.root, child: Node?) {
        if (root == null || child == null) return
        removeTag(root, child)
    }

    @JvmOverloads
    fun removeTag(root: Node = this.root, child: String) =
        removeTag(root, getChild(root, child))

    @JvmOverloads
    fun removeTagByText(root: Node = this.root, textValue: String) {
        for (node in root.children()) if (getValue(node) == textValue) removeTag(root, node)
    }

    fun replaceValue(newValue: String, vararg nodes: Node) {
        nodes[0].appendChild(doc!!.createTextNode(newValue))
        for (i in 1 until nodes.size)
            nodes[i].appendChild(nodes[i - 1])
    }

    fun setValue(node: Node, value: String?): Node {
        node.textContent = value
        write()
        return node
    }

    fun getChild(parent: Node, name: String): Node? {
        for(node in parent.children())
            if (node.nodeName == name)
                return node;
        return null
    }

    override fun write() {
        try {
            tr!!.transform(DOMSource(doc), StreamResult(FileOutputStream(currentDocument())))
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: TransformerException) {
            e.printStackTrace()
        }
    }

    fun nodeHasChild(parentNode: Node, child: String): Boolean = parentNode has child
    fun writeItemsList(node:Node, categoryName:String, items:List<String>){
        newChild(node, categoryName).apply {
            items.forEach {
                newChild(this,"item",it)
            }
        }
    }
}

