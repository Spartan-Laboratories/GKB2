package com.spartanlabs.bot
import com.spartanlabs.bottools.main.Bot
import com.spartanlabs.bottools.manager.start

fun main() {
    val bot = lazy {
        KotBot()
    }
    start(bot)
}

class KotBot : Bot() {
    override fun applyDailyUpdate(currentDate: String?) {}
}