package com.spartanlabs.bottools.commands

import com.spartanlabs.bottools.dataprocessing.D
import org.springframework.stereotype.Component
import com.spartanlabs.bottools.dataprocessing.KotGDP.DataAccessPoint as DAP

@Component
class admin:Command("admin") {
    override val brief = "administrative commands"
    override val details =  "a set of commands created for use by server administrators"
    override fun invoke(args: Array<String>) {}
    val guildData:DAP
        get() = D/guild
    init {
        DataAccessCommand(::guildData, this, "guild-data", getter="access", setter="modify")
    }
}