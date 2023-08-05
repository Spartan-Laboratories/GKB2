package com.spartanlabs.bottools.services

import com.spartanlabs.bottools.dataprocessing.*

class ServicePacketReader(packetName: String) : BaseXMLReader() {
    init {
        setDocument(packetName)
    }

    val uRL: String
        get() = root-"url"
    val skipLineData: List<String>
        get() = (root/"skiplines").children().map { it.getValue() }
    val keyName:String
        get() = root-"keyname"
    var oldValue:String
        get() = root-"oldvalue"
        set(data) = setValue(root/"oldvalue", data).let{Unit}
    override fun setDocument(pathName: String) = super.setDocument("services/$pathName.xml")

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val test = ServicePacketReader("game services/Dota 2.xml")
            println(test.uRL)
            test.skipLineData.forEach(::println)
        }
    }
}