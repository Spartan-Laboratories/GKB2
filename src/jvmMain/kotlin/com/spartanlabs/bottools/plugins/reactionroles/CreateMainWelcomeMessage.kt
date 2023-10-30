package com.spartanlabs.bottools.plugins.reactionroles

import com.spartanlabs.bottools.commands.Command
import com.spartanlabs.bottools.dataprocessing.D
import net.dv8tion.jda.api.entities.Guild

class CreateMainWelcomeMessage : Command("createmainwelcomemessage") {
    override var brief = "Creates a welcome message with a reaction for every current reaction role."
    override var details = "If more reaction roles are added after the welcome message has already been created\n" +
            "the message will recognise the new reaction role and add the appropriate new reaction to itself."

    init {
        makeInteractive()
    }

    override fun invoke(args: Array<String>) = ReactionRoleActions.createWelcomeMessage(guild, channel)

}
val Guild.welcomeMessages get() = (D/this/"welcomeMessages").children