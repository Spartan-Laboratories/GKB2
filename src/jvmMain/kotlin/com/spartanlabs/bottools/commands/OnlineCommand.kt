package com.spartanlabs.bottools.commands

import com.spartanlabs.bottools.botactions.online.getConnectionCode
import com.spartanlabs.bottools.botactions.online.readCookies
import com.spartanlabs.bottools.dataprocessing.B
import com.spartanlabs.bottools.dataprocessing.SourceKeyParser
import com.spartanlabs.bottools.dataprocessing.minus
import com.spartanlabs.bottools.main.evaluateList
import it.skrape.fetcher.Cookie

abstract class OnlineCommand
/**
 * Use this if you plan on having a primary address that you want to be
 * opened with the open() function.
 * @param primaryAddress the primary URL that will be accessed by this command
 */
protected constructor(name: String, protected var primaryAddress: String = "") : Command(name) {
    protected val valueMap = HashMap<String, String>()
    protected val keyParser = SourceKeyParser()
    protected lateinit var data : String
    private val cookieList by lazy { B/"Cookies"/name}
    private val cookieMap
        get() = cookieList.readAll().map { it-"name" to it-"value" }.toMap()
    init{
        prepAddress()
    }
    protected fun prepAddress(){
        if (primaryAddress.isNotBlank()) {
            deleteOldCookies()
            acquireCookies()
            logCookieAcquisition()
            stateCookies()
            testConnection()
        }
    }
    private fun deleteOldCookies(){
        for (node in cookieList.readAll())
            cookieList.remove(node)
        cookieList + ""
    }
    private fun acquireCookies() = readCookies(primaryAddress).forEachIndexed(::writeCookie)
    private fun writeCookie(index:Int, cookie: Cookie) =
        (cookieList/"cookie$index").let {
            it/"name" + cookie.name
            it/"value"+ cookie.value
        }
    private fun logCookieAcquisition(){}
    private fun stateCookies() = cookieList.readAll().map { (it - "name") to (it - "value") }.map{it.toString()}.forEach(log::info)
    private fun testConnection() =  try{ connect();     log.info("The command $name successfully validated connection")}
                                    catch(e:Exception){ log.info("The command $name could not successfully validate connection.")}
    protected fun cutToAfter(searchTerm: String) {
        data = cutToAfter(data, searchTerm)
    }

    protected fun cutToAfter(data: String?, searchTerm: String) = data!!.substring(data.indexOf(searchTerm) + searchTerm.length)
    protected open fun getValueFromKey(key: String?): String {
        val keySet = keyParser.getKeys(key!!)
        for (searchTerm in keySet.initial) cutToAfter(searchTerm)
        if (!validate(keySet.validation)) for (alternate in keySet.alternative) {
            cutToAfter(alternate)
            if (validate(keySet.validation)) break
        }
        return data.substring(0, data.indexOf(keySet.terminal))
    }

    private fun validate(correctEntries: ArrayList<String>): Boolean =
        if (correctEntries.size == 0) true
        else evaluateList(correctEntries){
            when(it){
                "digit" -> Character.isDigit(data[0]) || data[0] == '-'
                "alphabetic" -> Character.isAlphabetic(data[0].code)
                else -> data.startsWith(it)
            }
        }

    /**
     * Opens a new connection with the primary address of this command.
     * @return whether the connection was successfully established.
     */
    protected open fun connect() = connect(primaryAddress)
    protected infix fun connect(address: String) = com.spartanlabs.bottools.botactions.online.open(address, cookieMap)
    protected fun connectViaSkrape() = this connectViaSkrape primaryAddress
    protected infix fun connectViaSkrape(address: String) = com.spartanlabs.bottools.botactions.online.openViaSkrape(address)
    protected infix fun testConnection(URL: String) = getConnectionCode(URL)
    protected fun open(address:String, executeWithURLData:()->Unit){
        data = connect(address)
        executeWithURLData()
    }
    protected operator fun get(URL: String? = primaryAddress) = com.spartanlabs.bottools.botactions.online.get(URL!!)
    protected open fun mapValueByKey(vararg keyName:String) = keyName.forEach{ valueMap[it] = getValueFromKey(it)}
}