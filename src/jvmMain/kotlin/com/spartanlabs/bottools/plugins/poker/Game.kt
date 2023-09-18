package com.spartanlabs.bottools.plugins.poker

import org.apache.commons.collections4.list.SetUniqueList
import java.util.*

internal class Game(private val players:Set<Player>) {
    val orderedPlayers = SetUniqueList.setUniqueList(players.toList())
    fun acceptPlayerAction(player: Player, action: PlayerAction, amount: Int):Player {
        println("Player ${player.name} has performed action $action with amount $amount")
        val playerIndex = orderedPlayers.indexOf(player)
        val nextPlayer =
            if(playerIndex == orderedPlayers.size - 1)
                orderedPlayers[0]
            else orderedPlayers[playerIndex + 1]
        println("The next player to be prompted is ${orderedPlayers[0].name}")
        return nextPlayer
    }
}