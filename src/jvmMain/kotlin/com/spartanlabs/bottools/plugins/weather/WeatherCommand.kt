package com.spartanlabs.bottools.plugins.weather

import com.spartanlabs.bottools.commands.Command
import com.spartanlabs.bottools.commands.MethodCommand
import com.spartanlabs.bottools.commands.Option

class WeatherCommand : Command("weather"){
    override val brief = "Get the weather for a location"
    override val details = "Use this command to get weather information for either a zipcode or a city"
    private val city = MethodCommand(this, "getcity", "Get the weather for a city"){
        getOption("city")!!.asString.latlong.let{
            if(it.isFailure)            reply > "Could not find the target city"
            else it.getOrNull()!!.let{  reply > "lat: ${it.first}, long: ${it.second}" }
        }
    } + Option("string", "city", "The city to get the weather for", true)
    private val zip = MethodCommand(this, "getzip", "Get the weather for a zipcode"){
        getOption("zip")!!.asString.latlong.let {
            if (it.isFailure) reply > "Could not find the target zip code"
            else it.getOrNull()!!.let { reply > "lat: ${it.first}, long: ${it.second}" }
        }
    } + Option("string", "zip", "The zipcode to get the weather for", true)
    init{
        get and "city" becomes "getcity"
        get and "zip" becomes "getzip"
        makeInteractive()
    }
    override fun invoke(args:Array<String>){

    }
}