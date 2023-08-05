package com.spartanlabs.bottools.plugins.reactionroles

import com.spartanlabs.bottools.commands.Command
import com.spartanlabs.bottools.commands.Option
import com.spartanlabs.bottools.dataprocessing.D
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.emoji.Emoji
import org.slf4j.LoggerFactory

class AddReactionRole : Command("addreactionrole") {
    override val brief = "Use this command to tie a server emote to a role"
    override val details = ""
    private val log = LoggerFactory.getLogger(this::class.java)
    init {
        makeInteractive()
        this + Option("string", "emote", "the reaction emote that you want tied to a role", true) +
        Option("role", "role", "the role that you want to be given on reaction", true)
    }

    public override fun invoke(args: Array<String>){
        val emoteID = getOption("emote")!!.asString.split(":")[2].split(">")[0]
        val emote = guild.getEmojiById(emoteID)
        require(emote != null)
        log.trace("The emote has been recognized as ${emote.name}")
        val role = getOption("role")!!.asRole
        log.trace("The role has been recognized as ${role.name}")
        makeReactionRole(guild, emoteID, role.id)
        val creationMessage = "A reaction role has been created for the guild ${guild.banner}." +
                " Emote ${emote.asMention} is now tied to the role: ${role.asMention}"
        log.info(creationMessage)
        reply(creationMessage)
        ReactionRoleActions.updateMessages(guild, emote)
    }
    private fun invalidEmoteMessage(): Emoji? = null.also{
        say("Emote was not found. First option must be an emote that is found in this server.")
    }

    private fun getRole(roleString: String): Role {
        val roleID = args[1]
        val role = guild.getRoleById(roleID)
        return role ?: invalidRoleMessage()!!
    }

    private fun invalidRoleMessage(): Role? = null.also{
        say("Role was not found. Second option must be a role that is found in this server")
    }

    private fun makeReactionRole(guild: Guild, emoteID: String, roleID: String) {
        with(D/guild/"reactionRoles" create "reactionRole") {
            this create "emote" containing emoteID
            this create "role"  containing roleID
        }
    }

}