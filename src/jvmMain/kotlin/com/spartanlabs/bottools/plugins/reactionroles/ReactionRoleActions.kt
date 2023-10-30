package com.spartanlabs.bottools.plugins.reactionroles

import com.spartanlabs.bottools.botactions.addReactionToMessage
import com.spartanlabs.bottools.botactions.say
import com.spartanlabs.bottools.botactions.to
import com.spartanlabs.bottools.dataprocessing.D
import com.spartanlabs.bottools.main.Bot
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import org.slf4j.LoggerFactory

object ReactionRoleActions {
    private val log = LoggerFactory.getLogger(this::class.java)
    fun onReactionAdd(event: MessageReactionAddEvent) {
        log.trace("${this::class} detected a reaction add event")
        if(event.member?.user == Bot.jda.selfUser){
            log.trace("The reaction add event was caused by the bot itself. Ignoring.")
            return
        }
        if(event.messageId !in event.guild.welcomeMessages){
            log.trace("The added reaction was not on a welcome message. Ignoring.")
            return
        }
        giveReactionRole(event)
    }
    fun giveReactionRole(event: MessageReactionAddEvent) {
        log.trace("The reaction add event was performed by ${event.member?.user?.name} in server ${event.guild.name}")
        log.trace("The reaction added was ${event.emoji}")
        val role = getCorrespondingRole(event.guild, event.emoji)
        val roleMessage =   if(role != null)    "The role ${role.name} will be given to ${event.member?.user?.name}"
                            else                "No role was found for the reaction ${event.emoji}"
        log.trace(roleMessage)
        event.guild.addRoleToMember(event.member!!.user, role!!).complete()
    }

    fun removeDeletedWelcomeMessage(event: MessageDeleteEvent) = D/event.guild/"welcomeMessages" remove event.messageId

    fun createWelcomeMessage(guild: Guild, channel: MessageChannel) {
        (Bot say D/guild-"welcomeMessage" to channel).let {message ->
            D/message.guild/"welcomeMessages"/message.id to message.id
            addGameEmotes(message)
        }
    }


    private fun addGameEmotes(welcomeMessage: Message){
        getEmoteList(welcomeMessage.guild).forEach{
            addReactionToMessage(it, welcomeMessage)
        }
    }
    private fun getEmoteList(guild: Guild):List<Emoji> = (D/guild/"reactionRoles").children.map(guild::getEmojiById).filterNotNull()
    private fun getCorrespondingRole(guild: Guild, emote: Emoji) =
        emote.asReactionCode.split(":")[1].let{
        guild.getRoleById(D/guild/"reactionRoles"-it)
    }
    private fun getGuildWelcomeMessages(guild: Guild) :List<Message>{
        val messageNode = D/guild/"welcomeMessages"
        val messageIds = messageNode.children
        return List<Message>(messageIds.size){index->
            guild.getTextChannelById(messageNode-messageIds[index])!!.retrieveMessageById(messageIds[index]).complete()
        }
    }
    fun updateMessages(guild: Guild, emote: Emoji) = with(getGuildWelcomeMessages(guild)) {
        if (isNotEmpty())forEach{
            addReactionToMessage(emote, it)
        }
    }
}