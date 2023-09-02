package com.spartanlabs.bottools.services

import it.skrape.fetcher.BrowserFetcher
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape

class ServiceCommand(serviceName: String) : CheckValueService(serviceName) {
    override val brief = "service"
    override val details = ""
    override operator fun invoke() {
        data = skrape(BrowserFetcher){
            request{
                url = primaryAddress
            }
            response { responseBody }
        }
        navigate()
        checkValues(packet.oldValue)
    }

    companion object {
        private val createdServices = ArrayList<String>()
        fun createService(serviceName: String, onChange: TriggerAction, interval: Int){
            if(serviceName !in createdServices) {
                createService(ServiceCommand(serviceName), onChange, interval)
                createdServices.add(serviceName)
            }
        }
    }
}