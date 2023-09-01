package com.spartanlabs.bottools.manager

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.spartanlabs.bottools.main.Bot

class ViewModel(bot:Lazy<Bot>) {
    val bot by bot
    var generalState by mutableStateOf("Not started")
    init{

    }
}