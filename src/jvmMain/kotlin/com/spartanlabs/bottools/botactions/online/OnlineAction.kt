package com.spartanlabs.bottools.botactions.online

import com.spartanlabs.bottools.botactions.*
import com.spartanlabs.webtools.Connector
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import org.json.JSONObject
import org.json.JSONTokener
import org.jsoup.Jsoup
import java.io.*

val connector = Connector()
/**
 * Returns the next line of the source HTML code from the URL you are currently
 * connected to
 * @return the next line of HTML source code
 * @throws IOException
 */
@get:Throws(IOException::class)
val nextLine: String
    get() = connector.next()

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
