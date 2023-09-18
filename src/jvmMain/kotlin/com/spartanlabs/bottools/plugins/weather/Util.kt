package com.spartanlabs.bottools.plugins.weather

import java.io.BufferedReader
import java.io.FileReader

object Converter{
    val reader: BufferedReader
        get() = BufferedReader(FileReader("res/zip codes.txt"))
    infix fun convert(location:String) =
        if(location[0].let(isAlphabet)) convertCity(location)
        else                            convertZIP(location)
    val isAlphabet = {c:Char -> c in 'a'..'z' || c in 'A'..'Z'}
    private infix fun convertZIP(zipCode:String) = with(zipCode){
        reader  .readLines()
                .filter { it.startsWith(this) }[0]
                .split("\t")
                .let{it[13] to it[14]}
    }
    private infix fun convertCity(city:String) = with(city.lowercase()){
        reader  .readLines()
                .filter { it.lowercase().contains(this) }[0]
                .split("\t")
                .let{it[13] to it[14]}
    }
}
val String.latlong:Result<Pair<String,String>>
    get() = try{
        Result.success(Converter convert this)
    }catch(e:Exception){
        Result.failure(e)
    }