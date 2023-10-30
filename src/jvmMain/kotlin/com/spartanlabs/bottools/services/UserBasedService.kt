package com.spartanlabs.bottools.services

import com.spartanlabs.bottools.dataprocessing.D
import com.spartanlabs.bottools.dataprocessing.DatabaseAccessPoint as DAP
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import org.apache.commons.lang3.tuple.ImmutablePair
import org.slf4j.LoggerFactory

open class UserBasedService internal constructor(serviceName: String) : CheckValueService(serviceName) {
    override var oldValue:String
        get() = finalAccessPoint()?.let{ it.left-it.right} ?: ""
        set(value) = finalAccessPoint()!!.let{ it.left/it.right to value}
    private fun finalAccessPoint():ImmutablePair<DAP, String>?{
        var accessPoint = D/guild/member
        packet.oldValue.split("/").let{
            it.forEachIndexed{index, name ->
                if(index < it.size - 1)
                    accessPoint = accessPoint/name
                else return ImmutablePair.of(accessPoint, name)
            }
        }
        return null
    }
    override operator fun invoke() = jda.guilds.forEach(::checkGuild)

    protected open fun checkGuild(guild: Guild) {
        this.guild = guild
        log.info("Checking guild: {}", guild.name)
        guild.members.forEach{
            member = it
            userId.let {
                if(it == null || it == "" || it =="none")
                    return@forEach
            }
            checkMember(it)
        }
    }

    protected fun checkMember(member: Member) {
        log.info("Checking member: {}", member.user.name)
        data = connect(packet.uRL.replace("userid", userId!!))
        navigate()
        checkValues(oldValue)
    }

    override fun triggerResponce(newValue: Array<String>) {
        onChange.trigger(arrayOf(guild.id, member.id,data))
    }
    protected open val userId: String?
        get() = D/guild/member - "keyid"

    companion object {
        private val log = LoggerFactory.getLogger(UserBasedService::class.java)
        fun createService(serviceName: String, onChange: TriggerAction, interval: Int):Unit =
            CheckValueService.createService(UserBasedService(serviceName), onChange, interval)
    }
}
