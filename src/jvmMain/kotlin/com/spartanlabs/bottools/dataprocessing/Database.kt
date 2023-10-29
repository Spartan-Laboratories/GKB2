package com.spartanlabs.bottools.dataprocessing

import net.dv8tion.jda.api.entities.*

interface Database {
    operator fun div(server: Guild):MemberAccessPoint
    fun onNewServer(server: Guild):Result<Unit>
    fun onNewMember(server:Guild, member: Member):Result<Unit>
    fun onLaunchMemberCheck():Result<Unit>
}
interface MemberAccessPoint:DatabaseAccessPoint{
    operator fun div(member: Member): DatabaseAccessPoint
}
interface DatabaseAccessPoint{
    val name:String
    operator fun div(category: String):DatabaseAccessPoint
    infix fun remove(category: String):Result<Unit>
    operator fun minus(valName:String):String
    operator fun set(nodeName:String, value:String)
    infix fun to(value:String)
    infix fun create(nodeName:String):DatabaseAccessPoint
    infix fun containing(value:String)
    val children:List<String>
    val values get() = children.map { this - it }

    fun clear()
}