package com.spartanlabs.bottools.services

import com.spartanlabs.bottools.commands.OnlineCommand
import org.slf4j.LoggerFactory

abstract class CheckValueService protected constructor(private val packetName: String)
    : OnlineCommand(packetName.replace("/","").replace(" ",""),ServicePacketReader(packetName).uRL), Runnable {
    override val brief = "service"
    override val details = ""
    private var interval = 0
    protected lateinit var onChange: TriggerAction
    protected var packet: ServicePacketReader
    private val log = LoggerFactory.getLogger(this::class.java)
    protected open var oldValue:String
        get() = packet.oldValue
        set(value) {
            packet.oldValue = value
        }
    init {
        packet = ServicePacketReader(packetName)
        println("Created service: $packetName")
    }

    private infix fun at(interval: Int) = this.also {
        this.interval = interval
    }

    protected open fun triggerResponce(newValue: Array<String>) = onChange.trigger(newValue)


    private fun sleep(seconds: Int = interval) {
        try {
            Thread.sleep((seconds * 1000).toLong())
        } catch (e: InterruptedException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }

    override operator fun invoke(args: Array<String>) {}
    protected abstract operator fun invoke()

    override fun run() {
        initialSleep()
        while (true) {
            log.info("The service $name is starting a check")
            packet = ServicePacketReader(packetName)
            this()
            sleep()
        }
    }

    private fun initialSleep() = sleep(5)

    private infix fun respondWith(onChange: TriggerAction) = this.also{
        it.onChange = onChange
    }

    protected open fun checkValues(oldValue:String) =
        if (data != oldValue) {
            log.debug("$name found a new value: $data")
            this.oldValue = data
            triggerResponce(arrayOf(data))
        } else log.debug("$name detected no changes")

    protected fun navigate() {
        data = getValueFromKey(packet.keyName)
    }
    companion object {
        @JvmStatic
        protected fun createService(service: CheckValueService, onChange:TriggerAction, interval: Int):Unit =
            Thread(service respondWith onChange at interval).start()
    }
}
open fun interface TriggerAction{
fun trigger(a:Array<String>)
}