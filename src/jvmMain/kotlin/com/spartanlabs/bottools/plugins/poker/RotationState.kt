package com.spartanlabs.bottools.plugins.poker

internal enum class RotationState {
    LITTLEBLIND, BIGBLIND, CHECK, CALL;
}
internal enum class PlayerAction {
    FOLD, CHECK, CALL, RAISE, BET, LITTLEBLIND, BIGBLIND;
}