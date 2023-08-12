package com.spartanlabs.bottools.botactions.online

import com.spartanlabs.bottools.botactions.*
import com.spartanlabs.bottools.main.Bot
import com.spartanlabs.webtools.Connector
import com.spartanlabs.webtools.to
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import org.json.JSONObject
import org.json.JSONTokener
import org.jsoup.Jsoup
import java.io.*

/**
 * An extension of the BotAction class adding functionality that has
 * to do with accessing online content. Wrapper class for Connector.
 * @see Connector
 *
 * @author Spartak
 */
private val connector = Connector()

/**
 * Sends an image found online at the designated URL to the passed in Text channel
 * @param channel the Text channel that the image is to be sent to
 * @param imageAddress the URL of the image that is to be sent
 */
fun sendImageInChannel(imageAddress: String, channel: MessageChannel) =
    com.spartanlabs.bottools.botactions.sendFileInChannel(channel, connector download imageAddress to "res/temp.png")
data class SaveImageAction(val savedImage:String){
    infix fun to(fileName: String) = connector download savedImage to fileName
}
infix fun Bot.Companion.save(imageURL:String) = SaveImageAction(imageURL)

/**
 * Returns the next line of the source HTML code from the URL you are currently
 * connected to
 * @return the next line of HTML source code
 * @throws IOException
 */
@get:Throws(IOException::class)
val nextLine: String
    get() = connector.next()

/**
 * Establishes a connection with the specified URL. Return whether the
 * established connection was successful or not.
 * @param URL the URL address that you want to connect to.
 * @return **true** is the connection was established successfully <br></br>
 * **false** if it was not
 */
fun connect(URL: String?): Boolean = connector.open(URL!!)

fun open(URL: String, cookieList:Map<String, String>):String =
    Jsoup.connect(URL).cookies(cookieList).execute().body()
fun getConnectionCode(URL: String, cookieList:Map<String, String> = emptyMap()) =
    Jsoup.connect(URL).cookies(cookieList).execute().statusCode()
fun skrape(URL:String) = skrape(HttpFetcher){
    request{url = URL }
    response{this}
}
fun readCookies(URL:String) = skrape(URL).cookies
fun openViaSkrape(URL:String) = skrape(URL).responseBody
/**
 * Return the results of a get request sent to the specified URL
 * @param URL to send a get request to
 * @return what is returned by the get request
 */
fun get(URL: String): JSONObject = JSONObject(JSONTokener(StringReader(connector.get(URL))))
/**
 * Closes the connection to the currently opened URL.
 * Must be done before opening another one.
 */
fun closeConnection() = connector.close()

/**
 * Allows for easier browsing of html data by skipping the line
 * that starts with the given search term.
 * @param lineStartSearchTerm the beggining of the line that you are searching for
 * @return    The line that was found
 * @throws IOException if the line was not found
 */
@Throws(IOException::class)
fun skipLinesTo(lineStartSearchTerm: String?): String {
    var data: String
    do {
        data = nextLine
    } while (!(data.startsWith(lineStartSearchTerm!!) || data.trim { it <= ' ' }.startsWith(lineStartSearchTerm)))
    return data
}
