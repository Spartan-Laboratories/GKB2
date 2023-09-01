package com.spartanlabs.bottools.botactions

import com.spartanlabs.bottools.main.Bot
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL

/**
 * The bottom layer wrapper responsible for the simplest bot interactions with discord.
 * Meant to be used only within the project. Although this class' methods are public,
 * typically specific commands should extend the Command
 * class and use its methods instead.
 *
 * @see [bottools.commands.Command]
 *
 *
 * @author Spartak
 */

/** The logger that will track BotAction activity
 * Most of the logged messages should be considered 'trace'
 */
private val log = LoggerFactory.getLogger("BotAction")
infix fun JDA.update(commands: List<CommandData>) = updateCommands().addCommands(commands).complete()
operator fun MessageChannel.compareTo(message: String) =    0.also{ Bot say message     in this}
operator fun MessageChannel.compareTo(image: URL) =         0.also{ Bot show image.path in this}
operator fun MessageChannel.compareTo(file: File) =         0.also{ Bot send file       in this}


/**
 * Sends the given message in the given MessageChannel as a
 * Text-to-Speech message causing it to be read out loud to
 * the online members of the guild.
 * @param channel in which you want to send a message
 * @param message that you want to be sent
 * @return the message that was sent
 */
fun tts(channel: MessageChannel, message: String): Message {
    return channel.sendMessage(message).setTTS(true).complete()
}
fun addReactionToMessage(emoji: Emoji, message: Message) = message.addReaction(emoji).complete()
infix fun Bot.Companion.say(message:String) = MessageCreateData.fromContent(message)
infix fun MessageCreateData.to(channel: MessageChannel) = channel.sendMessage(this).complete()
operator fun MessageChannel.contains(message:MessageCreateData) = true.also{ message to this}
infix fun Bot.Companion.show(url:String) = MessageCreateData.fromContent(url)
infix fun Bot.Companion.send(file: File) = MessageCreateData.fromFiles(FileUpload.fromData(file))
infix fun Bot.Companion.send(message:Message) = MessageCreateData.fromMessage(message)