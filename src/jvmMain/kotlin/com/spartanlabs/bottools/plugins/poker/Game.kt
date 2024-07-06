package com.spartanlabs.bottools.plugins.poker

import com.spartanlabs.bottools.manager.MyLogger
import org.apache.commons.collections4.list.SetUniqueList

val log = MyLogger(Game::class.java)
internal class Game(players:Set<Player>) {
    private val orderedPlayers = SetUniqueList.setUniqueList(players.toList())
    private var pot = 0
    private val potValue get() = "the pot value is now $pot"
    fun acceptPlayerAction(player: Player, action: PlayerAction, amount: Int):PlayerPromptData {
        log info "Player ${player.name} has performed action $action with amount $amount"
        when(action){
            PlayerAction.LITTLEBLIND -> {
                player giveToPot amount
                log info "Player ${player.name} has placed a little blind of $amount"
                log info potValue
            }
            PlayerAction.BIGBLIND -> {
                player giveToPot amount
                log info "Player ${player.name} has placed a big blind of $amount"
                log info potValue
            }
            PlayerAction.CHECK -> {
                log info "Player ${player.name} has checked"
            }
            PlayerAction.CALL -> {
                player giveToPot amount
                log info "Player ${player.name} has called with $amount"
                log info potValue
            }
            PlayerAction.RAISE -> {
                player giveToPot amount
                log info "Player ${player.name} has raised to $amount"
                log info potValue
            }
            PlayerAction.BET -> {
                player giveToPot amount
                log info "Player ${player.name} has bet $amount"
                log info potValue
            }
            PlayerAction.FOLD -> {
                orderedPlayers.remove(player)
                log info "Player ${player.name} has folded"
                if(orderedPlayers.size == 1){
                    val winner = orderedPlayers[0]
                    log info "Player ${winner.name} has won the game"
                    pot awardTo winner
                }
            }
        }
        // Deciding the next player to be prompted
        // Should be done after the action is processed
        // because the action may change the order of the players
        val playerIndex = orderedPlayers.indexOf(player)
        val nextPlayer =
            if(playerIndex == orderedPlayers.size - 1)
                orderedPlayers[0]
            else orderedPlayers[playerIndex + 1]
        log info "The next player to be prompted is ${orderedPlayers[0].name}"
        return PlayerPromptData(nextPlayer, RotationState.CHECK)
    }
    private infix fun Player.giveToPot(amount:Int){
        chips -= amount
        this@Game.pot += amount
    }
    private infix fun Int.awardTo(player:Player) {
        player.chips += this
    }
}