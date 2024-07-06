package com.spartanlabs.bottools.botactions

import com.spartanlabs.bottools.main.Bot
import com.spartanlabs.bottools.manager.MyLogger
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageCreateData
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
private val log = MyLogger(Bot::class.java)

/**
 * Takes a list of CommandData generated by Command classes
 * and sends a non-blocking rest message to discord api
 * letting it know which commands and their details need to populate
 */
infix fun JDA.update(commands: List<CommandData>) = updateCommands().addCommands(commands).queue()
operator fun MessageChannel.compareTo(message: String) =    0.also{ Bot say message     in this}
operator fun MessageChannel.compareTo(message: MessageCreateData) =   0.also{ sendMessage(message).complete()}
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
infix fun Bot.Companion.say(message:String) = MessageCreateData.fromContent(message.ifBlank { "_" })
infix fun MessageCreateData.to(channel: MessageChannel) = channel.sendMessage(this).complete()
operator fun MessageChannel.contains(message:MessageCreateData) = true.also{ message to this}
infix fun Bot.Companion.show(url:String) = MessageCreateData.fromContent(url)
infix fun Bot.Companion.send(file: File) = MessageCreateData.fromFiles(FileUpload.fromData(file))
infix fun Bot.Companion.send(message:Message) = MessageCreateData.fromMessage(message)

infix fun Guild.createChannel(name:String) = createTextChannel(name).complete()
infix fun Category.createChannel(name:String) = createTextChannel(name).complete()
infix fun Guild.createChannelCategory(categoryName:String):Result<Category> = categories.filter { it.name == categoryName }.let{
    if(it.isEmpty())    Result.success(createCategory(categoryName).complete())
    else                Result.failure(Exception("Category already exists"))
}
infix fun TextChannel.createThread(threadName:String) = createThreadChannel(threadName).complete()
infix fun TextChannel.delete(message:Message) = deleteMessageById(message.id).queue()
infix fun MessageChannelUnion.delete(message:Message) = deleteMessageById(message.id).queue()
val Message.delete get() = delete().queue()