package com.spartanlabs.bottools.manager

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.spartanlabs.bottools.main.Bot

class ViewModel(bot:Lazy<Bot>) {
    val bot by bot
    var _generalState = "not started"
    var generalState by mutableStateOf("not started")
    //var generalStateStage1 by mutableStateOf(Bot.state)
    init{

    }
}