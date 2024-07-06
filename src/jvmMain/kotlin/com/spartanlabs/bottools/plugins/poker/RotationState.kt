package com.spartanlabs.bottools.plugins.poker

internal enum class RotationState {
    LITTLEBLIND{
        override var promptMessage = "It is your turn to place the little blind"
    },
    BIGBLIND{
        override var promptMessage = "It is your turn to place the big blind"
    },
    CHECK{
        override var promptMessage = "It is your turn"
    },
    CALL{
        override var promptMessage = "It is your turn"
    };
    open lateinit var promptMessage:String
}
internal enum class PlayerAction {
    FOLD, CHECK, CALL, RAISE, BET, LITTLEBLIND, BIGBLIND;
}