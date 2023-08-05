
package com.spartanlabs.bottools.services

import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import java.net.URL

class RssService protected constructor(serviceName: String?) : CheckValueService(serviceName!!) {
    val feed = packet.uRL.let { address ->
        primaryAddress = address
        prepAddress()
        SyndFeedInput().build(XmlReader(URL(address)))
    }

    override operator fun invoke() {
        println("Testing rss feed service")
        data = feed!!.entries[0].uri
        checkValues(packet.oldValue)
    }

    companion object {
        fun createService(serviceName: String, onChange: TriggerAction, interval: Int) =
            createService(RssService(serviceName), onChange, interval)
    }
}