package com.spartanlabs.bottools.plugins.poker

import com.spartanlabs.bottools.botactions.*
import com.spartanlabs.bottools.commands.*
import com.spartanlabs.bottools.main.*
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageEditData

class PokerCommand() : Command("poker") {
    override val brief = "Allows you to play texas hold em' poker"
    override val details = "challenge other people in the server to a game of poker"
    private val createTable = object: SubCommand("createtable", this@PokerCommand) {
        override val brief = "creates a new table for people to play at"
        override val details = "creates a new table for people to play at"
        lateinit var table: Table
        val buttonListener = object: BotListener(){
            init{
                responder newButtonInteractionAction ::onButtonClick
                Bot.jda.addEventListener(this)
            }
        }
        override fun invoke(args: Array<String>) {
            targetMember!!.user.name.let { creatorName ->
                val channel = guild createCategory "Poker Tables" createChannel "$creatorName's poker table"
                table = Table(channel,Player(creatorName))
                tables[channel] = table
                handles[table] = HashMap()
            }
            // Create the buttons that will ask the user what they want to do next
            reply!!.addActionRow(
                Button.primary("$poker:$name:sit", "Sit down at your table"),
                Button.primary("$poker:$name:join", "sit at table and join in on the next game")
            ).complete()
        }
        /**
         * This is the function that is called when a button is clicked
         * @param event the event that is triggered when a button is clicked
         */
        private fun onButtonClick(event: ButtonInteractionEvent) {
            when (event.button.id) {
                "$poker:$name:sit" -> {
                   Player(event.user.name) attemptSitAt table
                }

                "$poker:$name:join" -> Bot say "The table was created but you did not join it" to channel
                else -> throw Exception("Unknown button id")
            }
            // Since these buttons are only used once, we can remove the listener
            Bot.jda.removeEventListener(buttonListener)
        }
    }
    private val sitAtTable = MethodCommand("sithere", "sit at a table")         { targetMember!! attemptSitAt   channel }
    private val playGame = MethodCommand("playgame", "join in on the next game"){
        targetMember!! attemptPlay    channel
    }
    private val playerActionListener = object: BotListener(){
        init{
            responder newButtonInteractionAction ::performPlayerAction
            Bot.jda.addEventListener(this)
        }
    }
    val Channel.isTable:Boolean
        get() = this in tables.keys
    init {
        makeInteractive()
        this + "create" and "table" becomes "createtable"
        this + "sit"    and "here"  becomes "sithere"
        this + "play"   and "game"  becomes "playgame"
    }
    override fun invoke(args: Array<String>) {}
    private infix fun Player.attemptSitAt(table:Table) = with(table.seat(this)) { isSuccess.also { success ->
        if (success){
            if(channel != table.channel)channel > "You are now sitting at the table. Head over to ${table.channel.asMention} to view the table"
            table.channel > "$name has sat down at the table"
            table.perspectives[this@attemptSitAt]!!.let{playerPerspective ->
                playerPerspective > "This is where you will see and interact with the table"
                playerPerspective > "Please use either `/poker play game` to join in on the next game or `/poker see options` for other options"
            }
        }
        else channel > this.toString()
    }}
    private infix fun Member.attemptSitAt(channel: MessageChannel) =
        if(!channel.isTable) reply!! > "This channel is not associated with a poker table. Please use this command in a poker table channel" +
                " or use `/poker createtable` to create a new table"
        else Player(user.name) attemptSitAt tables[channel]!!
    private infix fun Player.attemptPlay(table:Table) = table addPlayer this
    private infix fun Member.attemptPlay(channel:MessageChannel) {
        if(!channel.isTable) reply!! > "This channel is not associated with a poker table. Please use this command in a poker table channel" +
                " or use `/poker createtable` to create a new table"
        else {
            val player = Player(user.name)
            val table = tables[channel]!!
            player attemptPlay table
            handles[table]!![player] = scEvent!!.deferReply().complete()
            say("Waiting for the next game to start")
        }
    }
    companion object{
        private const val poker = "plugin:poker"
        private const val play  = "play"
        private val tables = HashMap<TextChannel,Table>()
        private val handles = HashMap<Table, HashMap<Player, InteractionHook>>()
        internal fun promptForAction(table:Table, player:Player, rotationState:RotationState){
            val handle = handles[table]!![player]!!
            when(rotationState){
                RotationState.LITTLEBLIND ->{
                    MessageCreateData.fromContent("")
                    handle.editOriginal(MessageEditData.fromContent("It is your turn to place the little blind")
                        .apply{components.add(ActionRow.of(
                        Button.success("$poker:$play:littleblind", "Place little blind"),
                        Button.danger("$poker:$play:fold", "Fold")))})
                }
                else ->{}
                /*
                RotationState.BIGBLIND ->{
                    handle.addActionRow(
                        Button.success("$poker:$play:bigblind", "Place big blind"),
                        Button.danger("$poker:$play:fold", "Fold"))
                    handle.setContent("It is your turn to place the big blind").complete()
                }
                RotationState.CHECK ->{
                    handle.addActionRow(
                        Button.success("$poker:$play:check", "Check"),
                        Button.primary("$poker:$play:bet", "Bet"),
                        Button.secondary("$poker:$play:raise", "Raise"),
                        Button.danger("$poker:$play:fold", "Fold"))
                    handle.setContent("It is your turn").complete()
                }
                RotationState.CALL ->{
                    handle.addActionRow(
                        Button.success("$poker:$play:call", "Call"),
                        Button.secondary("$poker:$play:raise", "Raise"),
                        Button.danger("$poker:$play:fold", "Fold"))
                    handle.setContent("It is your turn").complete()
                }

                 */
            }
        }
        private fun performPlayerAction(event:ButtonInteractionEvent){
            // If the button is not a play action button, then it is not what we are listening for
            if(!event.button.id!!.startsWith("$poker:$play:")) return
            val channel = event.channel as MessageChannel
            val table = tables[channel] ?: return // If the channel is not a table, then exit
            val player = table[event.member!!]
            when(event.button.id){
                "$poker:$play:littleblind" -> {
                    table.acceptPlayerAction(player, PlayerAction.LITTLEBLIND)
                    event.reply("You have placed the little blind").complete()
                }
                "$poker:$play:bigblind" -> {
                    table.acceptPlayerAction(player, PlayerAction.BIGBLIND)
                    event.reply("You have placed the big blind").complete()
                }
                "$poker:$play:check" -> {
                    table.acceptPlayerAction(player, PlayerAction.CHECK)
                    event.reply("You have checked").complete()
                }
                "$poker:$play:bet" -> {
                    table.acceptPlayerAction(player, PlayerAction.BET)
                    event.reply("You have bet").complete()
                }
                "$poker:$play:raise" -> {
                    table.acceptPlayerAction(player, PlayerAction.RAISE)
                    event.reply("You have raised").complete()
                }
                "$poker:$play:call" -> {
                    table.acceptPlayerAction(player, PlayerAction.CALL)
                    event.reply("You have called").complete()
                }
                "$poker:$play:fold" -> {
                    table.acceptPlayerAction(player, PlayerAction.FOLD)
                    event.reply("You have folded").complete()
                }
            }
        }

        internal fun promptGameStart(table:Table, playingNext: Set<Player>) {
            handles[table]!!.filter { it.key in playingNext }.forEach { (player, handle) ->
                handle.editOriginal(MessageEditData.fromContent("The next game will be starting soon)")).complete()
            }
        }
    }
}