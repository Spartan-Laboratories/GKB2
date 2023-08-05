package com.spartanlabs.bottools.main

import net.dv8tion.jda.api.events.Event

infix fun Responder.reactTo(event : Event) = actOn(event)
infix fun Responder.newGuildJoinAction(onEventAction: EventAction.GuildJoinAction) = addOnGuildJoinAction(onEventAction)
infix fun Responder.newGuildUpdateNameAction(onEventAction : EventAction.GuildUpdateNameAction) = addOnGuildUpdateNameAction(onEventAction)
infix fun Responder.newGuildMemberJoinAction(onEventAction : EventAction.GuildMemberJoinAction) = addOnGuildMemberJoinAction(onEventAction)
infix fun Responder.newUserUpdateOnlineStatusAction(onEventAction : EventAction.UserUpdateOnlineStatusAction) = addOnUserUpdateOnlineStatusAction(onEventAction)
infix fun Responder.newMessageReactionAddAction(onEventAction : EventAction.MessageReactionAddAction) = addOnMessageReactionAddAction(onEventAction)
infix fun Responder.newMessageReceivedAction(onEventAction : EventAction.MessageReceivedAction) = addOnMessageReceivedAction(onEventAction)
infix fun Responder.newMessageDeleteAction(onEventAction : EventAction.MessageDeleteAction) = addOnMessageDeleteAction(onEventAction)
infix fun Responder.newSlashCommandInteractionAction(onEventAction : EventAction.SlashCommandInteractionAction) = addOnSlashCommandInteractionAction(onEventAction)
infix fun Responder.newUserContextInteractionAction(onEventAction : EventAction.UserContextInteractionAction) = addOnUserContextInteractionAction(onEventAction)
infix fun Responder.newMessageContextInteractionAction(onEventAction : EventAction.MessageContextInteractionAction) = addOnMessageContextInteractionAction(onEventAction)