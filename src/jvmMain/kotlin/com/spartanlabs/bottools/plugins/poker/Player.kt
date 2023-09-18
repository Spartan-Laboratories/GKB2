package com.spartanlabs.bottools.plugins.poker

internal data class Player(
    val name:String,
    var chips:Int = 1000,
    var isPlaying:Boolean = false)