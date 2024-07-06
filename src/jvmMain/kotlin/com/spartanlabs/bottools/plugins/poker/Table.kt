package com.spartanlabs.bottools.plugins.poker

import com.spartanlabs.bottools.botactions.createThread
import com.spartanlabs.bottools.botactions.say
import com.spartanlabs.bottools.main.Bot
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel

internal class Table(internal val channel: TextChannel, var admin:Player, internal var numSeats:Int = 6) {
    private var players = mutableSetOf<Player>()
    val perspectives = hashMapOf<Player, ThreadChannel>()
    private val playingNext = mutableSetOf<Player>()
    var game:Game? = null
    var state = State.WAITING_FOR_PLAYERS
    val hasOpenSeats:Boolean
        get() = players.size < numSeats
    infix fun seat(player:Player):Result<MutableSet<Player>> =
        if      (player in players) Result.failure(PlayerAlreadySittingException())
        else if (!hasOpenSeats)     Result.failure(FullTableException())
        else                        Result.success(players.apply {
            /* Add the player to the list of people who are sitting down at the table
             * This does not imply that they are playing in the next game
             */
            add(player)
            Bot say "${player.name} has sat down at the table" to channel
            /* The player's perspective is a thread that is created in the channel*/
            val perspective = channel createThread "${player.name}'s perspective"
            perspectives[player] = perspective
        })
    internal infix fun addPlayer(player:Player) {
        playingNext.add(player)
        if(playingNext.size >= 2) {
            state = State.IN_PROGRESS
            PokerCommand.promptGameStart(this, playingNext)
            PokerCommand.promptForAction(this, playingNext.first(), RotationState.LITTLEBLIND)
            game = Game(playingNext)
            playingNext.clear()
        }
    }
    internal fun acceptPlayerAction(player:Player, action:PlayerAction, amount:Int = 0){
        val nextPlayer = game!!.acceptPlayerAction(player, action, amount)
        val nextPrompt = when(action){
            PlayerAction.LITTLEBLIND                                -> RotationState.BIGBLIND
            PlayerAction.BIGBLIND                                   -> RotationState.CHECK
            PlayerAction.CHECK                                      -> RotationState.CHECK
            PlayerAction.BET,PlayerAction.CALL,PlayerAction.RAISE   -> RotationState.CALL
            PlayerAction.FOLD                                       -> RotationState.CALL //TODO Here I should check if there is only one player left
        }
        PokerCommand.promptForAction(this,player, nextPrompt)
    }
    internal operator infix fun get(member: Member) = players.first{it.name == member.user.name}
    infix fun unseat(player:Player) = players.apply { remove(player) }
    internal class FullTableException:Exception("There are not available seats at this table")
    internal class PlayerAlreadySittingException:Exception("This player is already sitting at this table")
    enum class State{
        WAITING_FOR_PLAYERS,WAITING_FOR_NEXT_GAME,IN_PROGRESS
    }
}
