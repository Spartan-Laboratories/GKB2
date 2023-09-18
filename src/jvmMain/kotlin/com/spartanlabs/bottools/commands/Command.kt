package com.spartanlabs.bottools.commands

import com.spartanlabs.bottools.botactions.*
import com.spartanlabs.bottools.botactions.createChannel as catCC
import com.spartanlabs.bottools.botactions.createChannel as guildCC
import com.spartanlabs.bottools.botactions.createChannelCategory as newCategory
import com.spartanlabs.bottools.botactions.createThread as channelCT
import com.spartanlabs.bottools.dataprocessing.D
import com.spartanlabs.bottools.main.Bot
import com.spartanlabs.bottools.main.Parser
import com.spartanlabs.bottools.main.Parser.CommandContainer
import com.spartanlabs.bottools.manager.MyLogger
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.*
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import net.dv8tion.jda.api.utils.FileUpload
import java.io.File
import java.net.URL
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.collections.set

abstract class Command protected constructor(val name: String) {
    /*--------------I AM--------------------------------*/
    @set:JvmName("setActive")
    var active = true
    lateinit var newBrief: String
    protected abstract val brief : String
    protected abstract val details: String
    protected open val detailStatement = ""
    private val basicDescription
        get() = "$name: $brief"
    val helpMessage: String
        get() = """$basicDescription
            |$details
            |""".trimMargin()
    /*^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^*/

    /*---------EVENT INFORMATION------------------------*/
    var event: Event? = null
        internal set(value) {
            when (value) {
                is MessageReceivedEvent -> set(value)
                is GenericCommandInteractionEvent -> pullInteractionData(value)
            }
            field = value
        }
    open var messageEvent: MessageReceivedEvent? = null
    open lateinit var guild: Guild
    open lateinit var channel: MessageChannel
    open lateinit var message: Message
    open lateinit var user: User
    open lateinit var member: Member
    open lateinit var tagged : List<Member>
    open val targetMember: Member?
        get() {
            return scEvent?.getOptionsByType(OptionType.USER)?.let{
                if(it.size > 0) it[0].asMember
                else            null
            } ?: member
        }
    /*^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^*/
    protected var jda = Bot.jda
    protected val guilds
        get() = jda.guilds
    protected var terminalArg = 0
    protected var eb = EmbedBuilder()
    protected var args = Array<String>(0){""}
        private set
    open var reply: ReplyCallbackAction? = null

    val subCommands by lazy{ HashMap<String, SubCommand>()}
    protected var subCommandRequired = false
    var isInteractible = false
        private set
    private var noSubCommandDescription: String? = null
    private val scRequiredErrMsg = "This command has to be followed by a sub-command."
    private val invalidSCErrMsg = " is not a valid sub-command."

    protected lateinit var slashCommandData: SlashCommandData
    protected var deferredReply: ReplyCallbackAction? = null
    var scEvent: SlashCommandInteractionEvent? = null
    val get     by lazy { this + "get" }
    val set     by lazy { this + "set" }
    val enable  by lazy { this + "enable" }
    val disable by lazy { this + "disable" }
    val test    by lazy { this + "test" }
    val admin   by lazy { this + "admin" }

    /**
     * The only constructor in the class. It is required that this constructor is called from the constructors of subclasses.
     * Otherwise various help message and error report bugs will occur.
     */
    init {
        resetEmbedBuilder()
        generateSlashCommandData()
    }

    /**
     * Takes in a [MessageReceivedEvent] and sets it as this command's event for the execution of the following commands.
     * Needs to be called prior to calling [.execute]
     * Returns the [Command] that it is a part of to be able to conveniently call [.execute] right away
     * @param event - the event of the message that triggered this command
     * @return The [Command] whose event is being set
     */
    private fun set(event: MessageReceivedEvent): Command = this.also{
        messageEvent = event
        guild = event.guild
        member = event.member!!
        user = event.author
        channel = event.channel.asTextChannel()
        message = event.message
        tagged = message.mentions.members
    }
    internal infix fun set(event:Event):Command = this.apply{
        this.event = event
    }
    open operator fun invoke(){
        when(event){
            is MessageReceivedEvent             -> this(Parser parse event as MessageReceivedEvent)
            is GenericCommandInteractionEvent   -> with(event as GenericCommandInteractionEvent){
                reply = deferReply()
                this@Command(this)
            }
        }
    }
    operator fun invoke(interaction: GenericCommandInteractionEvent) = when(interaction){
        is MessageContextInteractionEvent, is UserContextInteractionEvent   ->
            this(Parser parse "/$name")
        is SlashCommandInteractionEvent                                     -> {
            log.info("Reaction to slash command interaction by ${interaction.member}")
            scEvent = interaction
            this(Parser parse interaction)
        }
        else                                                                -> null
    }

    /**
     * Makes the bot send the automatically generated help message in the text channel that the command was received in.
     */
    protected fun help() = this say helpMessage

    fun help(subcommand: Array<String>) {
        if (subcommand.size == 0) help() else getSubCommand(subcommand[0])!!.help(getSecondaryArgs(subcommand))
    }
    infix fun say(message: String) = Bot say message in channel
    protected infix fun show(url: String) = Bot show url in channel
    protected infix fun show(file: File) = Bot send file in channel
    protected infix fun File.sendIn(channel: MessageChannel) = Bot send this in channel
    /**
     * Outputs the given message in the text channel of the event set by [pullInteractionData(GuildMessageReceivedEvent)].
     * <br></br>Works the same way as [.say] but with a delay in seconds set by secondsDelay.
     * @param secondsDelay - the delay in seconds after which the message is to be written.
     * @param message - the message that is to be written in the text channel.
     */
    protected fun say(secondsDelay: Int, message: String?) = channel.sendMessage(message!!).completeAfter(secondsDelay.toLong(), TimeUnit.SECONDS)

    protected fun treatAsSubCommand(commandData: CommandContainer): Boolean =
        if(commandData.args.isEmpty()) subCommandRequired
            .also { if (subCommandRequired) showSCErrorMessage("${commandData.commandName} must be followed by a valid subcommand") }
        else isValidSubCommand(args[0])

    private val subcommandNames: String
        get() = subCommands.keys.toString().let { it.substring(1,it.length-1) }

    private fun showSCErrorMessage(errorMessage: String) {
        say(errorMessage)
        say("""
            The possible sub-commands are:
            ${subcommandNames}
            """.trimIndent()
        )
    }

    private fun resetEmbedBuilder() {
        eb = EmbedBuilder()
    }

    protected fun getSecondaryArgs(args: Array<String>): Array<String> {
        require(args.size != 0) { "Passed in arguments must have a size of >=1" }
        if (args.size == 1) return arrayOf<String>()
        return args.sliceArray(1 until args.size)
    }


    fun addSubCommand(command: SubCommand) {
        subCommands[command.name] = command
    }

    protected fun removeSubCommand(commandName: String) = subCommands.remove(commandName)

    private fun isValidSubCommand(name: String): Boolean = (name in subCommands.keys).also{
        if(it == false && subCommandRequired) showSCErrorMessage(name + invalidSCErrMsg)
    }

    fun getSubCommand(name: String): SubCommand? {
        if (!isValidSubCommand(name)) say("No sub-command \"$name\" exists")
        return subCommands[name]
    }

    /**
     * Sets the description of this command's action in the case that it is used without a sub-command.
     * It is recommended that this method is used by all commands that do not require a sub-command,
     * otherwise their description that is shown by /help [command name] will be blank.
     * Note that by default sub-commands are not required, to change that call [.isSubCommandRequired]
     * @param description - a description of what this command does when used without a sub-command
     */
    protected fun setNoSubCommandDescription(description: String?) {
        noSubCommandDescription = description
    }

    protected fun validateSubCommand(args: Array<String>): Boolean = isValidSubCommand(args[0])

    protected fun quickEmbed(title: String?, description: String) = eb.apply {
        setAuthor(title)
        setDescription(with(description){if(length>emdl)substring(0,emdl);this})
    }

    protected fun sendEmbed(channel: MessageChannel = this.channel, filePath: String = "") {
        val message = channel.sendMessageEmbeds(finalEmbed)
        if(filePath != "") message.addFiles(FileUpload.fromData(File(filePath), "itemline.png"))
        message.complete()
        resetEmbedBuilder()
    }

    protected fun sendEmbed(channelList: Collection<MessageChannel>) = channelList.forEach{ sendEmbed(it) }.also{ resetEmbedBuilder() }
    open operator fun invoke(commandText: CommandContainer){
        args = commandText.args
        if (treatAsSubCommand(commandText)) {
            commandText.commandName = args[0]
            commandText.args = getSecondaryArgs(args)
            subCommands[args[0]]!!(commandText)
        }
        else this(args)
    }
    abstract operator fun invoke(args : Array<String>)
    fun say(message: Message) = Bot send message in channel

    protected fun makeInteractive() = slashCommandData.apply { description = brief }.also { isInteractible = true }

    open val commandData: CommandData
        get() = slashCommandData
    private fun pullInteractionData(event: GenericCommandInteractionEvent): Command {
        guild = event.guild!!
        member = event.member!!
        channel = event.messageChannel
        user = when(event){
            is UserContextInteractionEvent  -> event.target
            else                            -> event.user
        }
        if (event is MessageContextInteractionEvent)
            message = event.target
        return this
    }

    protected open val description: String?
        get() {
            return helpMessage.let{
                if(it.length > 100)
                    it.substring(0, it.indexOf("\n"))
                else it
            }
        }

    protected open fun addOption(option: Option): Command  = this.also{
        slashCommandData.addOptions(option)
    }
    open operator fun plus(option: Option) = addOption(option)

    open infix fun `reply with`(message: String) = reply?.setContent(message)?.complete() ?: say(message)

    protected infix fun replyWith(message: String) = `reply with`(message)
    /*
    protected inner class SaveImageAction internal constructor(val url: String){
        infix fun to(fileName: String) = com.spartanlabs.generictools.saveImage(url, fileName)
    }*/


    protected open fun generateSlashCommandData() {
        slashCommandData = Commands.slash(
            name.lowercase(),
            description!!
        )
    }

    protected open infix fun addToSlashCommandData(subcommand: SubcommandData)          = slashCommandData.addSubcommands(subcommand)
    open infix fun addToSlashCommandData(subcommandGroup: SubcommandGroupData)          = slashCommandData.addSubcommandGroups(subcommandGroup)
    protected open infix fun addToSlashCommandData(optionData: OptionData)              = slashCommandData.addOptions(optionData)
    infix operator fun plus(name: String):OrganizationCommand =
        if(name !in subCommands.keys)
            OrganizationCommand(name, this)
        else
            subCommands[name] as OrganizationCommand
    protected fun resetChannel(): MessageChannel = channel.apply {
        channel = messageEvent?.channel ?: scEvent!!.messageChannel
    }
    protected fun resetMember(): Member = member.apply {
        member = (messageEvent?.member ?: scEvent!!.member)!!
    }
    protected fun resetGuild(): Guild = guild.apply {
        guild = (messageEvent?.guild ?: scEvent!!.guild)!!
    }
    protected fun resetMessage(): Message = message.also{
        message = messageEvent!!.message
    }

    protected infix fun tts(message: String): Message = com.spartanlabs.bottools.botactions.tts(channel, message)
    protected fun replyWithEmbed() = reply!!.addEmbeds(finalEmbed).complete()
    protected val mentions
        get() = message.mentions.members

    private val finalEmbed: MessageEmbed
        get() {
            if(eb.build().footer.let{it == null || it.text == ""})
                eb.setFooter(Bot.jda!!.selfUser.name)
            return eb.setTimestamp(Instant.now()).build()
        }

    open fun getOption(optionName: String): OptionMapping? = scEvent!!.getOption(optionName)

    companion object {
        @JvmStatic protected val log = MyLogger(Command::class.java)
        @JvmStatic protected val error  :(String)->Unit = log::error
        @JvmStatic protected val warn   :(String)->Unit = log::warn
        @JvmStatic protected val info   :(String)->Unit = log::info
        @JvmStatic protected val debug  :(String)->Unit = log::debug
        @JvmStatic protected val trace  :(String)->Unit = log::trace
        private const val embedMaximumDescriptionLength = 2048
        private const val emdl = embedMaximumDescriptionLength
        /**
         *
         * @param args the String[] that is to be converted to a String
         * @param startIndex the element to start with (first element is 1)
         * @param endIndex the element to end with (last element is same as .lenth)
         * @return A String that contains the selected element group with a space between elements
         */
        @JvmOverloads
        fun concatArgs(args: Array<String>, startIndex: Int = 1, endIndex: Int = args.size): String {
            var concatenation = ""
            for (i in startIndex..endIndex) concatenation += args[i - 1] + " "
            concatenation = concatenation.substring(0, concatenation.length - 1)
            return concatenation
        }

        fun removeLeadingElement(args: Array<String?>): Array<String?> {
            val newArgs = arrayOfNulls<String>(args.size - 1)
            for (i in 1..args.size) newArgs[i - 1] = args[i]
            return newArgs
        }
    }
    protected operator fun ReplyCallbackAction?.compareTo(message: String):Int{reply
        (if(this!=null)this@Command::`reply with`else this@Command::say)(message);
        return 0
    }
    protected operator fun MessageChannel.compareTo(message: String)        = 0.also{Bot say message in this}
    protected operator fun MessageChannel.compareTo(embed:MessageEmbed)     = 0.also{sendMessageEmbeds(embed).complete()}
    protected operator fun MessageChannel.compareTo(file:File)              = 0.also{Bot send file in this}
    protected operator fun MessageChannel.compareTo(url: URL)                = 0.also{Bot show url.path in this}
    protected operator fun Guild.div(node:String)   = D/this/node
    protected operator fun Member.div(node:String)  = D/guild/this/node
    protected operator fun Guild.div(member:Member) = D/this/member
    protected infix fun Guild.createChannel(name:String) = guildCC(name)
    protected infix fun Guild.createCategory(name:String) = newCategory(name).getOrDefault(guild.categories.first { it.name == name })
    protected infix fun Category.createChannel(name:String) = catCC(name)
    protected fun my(optionName:String) = getOption(optionName)!!.asString
    protected operator fun div(optionName:String) = my(optionName)
    fun MethodCommand(name:String, brief:String, onExecute: (Array<String>)->Unit) = MethodCommand(this, name, brief, onExecute)
    protected fun Option(name:String, description:String, required:Boolean = true, type:String = "string",) = Option(type,name, description, required)
}
