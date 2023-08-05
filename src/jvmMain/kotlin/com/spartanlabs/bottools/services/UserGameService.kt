package com.spartanlabs.bottools.services

import com.spartanlabs.bottools.dataprocessing.D

class UserGameService internal constructor(var gameName: String) :
    UserBasedService("game services/$gameName/UserGames") {
    override val userId: String
        get() = D/guild/member/"Games"/gameName-"id"

    companion object {
        fun createService(gameName: String, onChange: TriggerAction, interval: Int) =
            createService(UserGameService(gameName), onChange, interval)
    }
}