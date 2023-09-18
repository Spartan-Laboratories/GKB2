package com.spartanlabs.bottools.main

import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.update.GuildUpdateNameEvent
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.events.user.update.UserUpdateOnlineStatusEvent
@FunctionalInterface
fun interface EventAction<EventType : Event> {
    fun perform(e : EventType)
    fun interface GuildJoinAction : EventAction<GuildJoinEvent>
    fun interface UserUpdateOnlineStatusAction : EventAction<UserUpdateOnlineStatusEvent>
    fun interface MessageReceivedAction : EventAction<MessageReceivedEvent>
    fun interface MessageReactionAddAction : EventAction<MessageReactionAddEvent>
    fun interface MessageDeleteAction : EventAction<MessageDeleteEvent>
    fun interface GuildUpdateNameAction : EventAction<GuildUpdateNameEvent>
    fun interface GuildMemberJoinAction : EventAction<GuildMemberJoinEvent>
    fun interface SlashCommandInteractionAction : EventAction<SlashCommandInteractionEvent>
    fun interface UserContextInteractionAction : EventAction<UserContextInteractionEvent>
    fun interface MessageContextInteractionAction : EventAction<MessageContextInteractionEvent>
    fun interface ButtonInteractionAction : EventAction<ButtonInteractionEvent>
}