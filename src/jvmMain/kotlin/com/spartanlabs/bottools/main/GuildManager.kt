package com.spartanlabs.bottools.main

import com.spartanlabs.bottools.dataprocessing.D
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.internal.utils.tuple.ImmutablePair
import java.awt.Color

class GuildManager {
    private lateinit var currentGuild : Guild
    private val defaultRole:Role
        get() = currentGuild.getRoleById(D/currentGuild-"defaultRole")?:currentGuild.publicRole
    private var rankRequirements: HashMap<Int, ImmutablePair<String, Double>>? = null

    /**
     * Load a new guild. Must be called prior to the use of other Guild Manager methods.
     * @param guild
     */
    fun loadGuild(guild: Guild) {
        currentGuild = guild
        //rankRequirements = Bot.data.getRankReqs(guild)
    }

    /**
     * Makes the Guild Manager add points to the passed in user as a reward for writing a message. [.loadGuild] must be
     * called prior to the user of this method, otherwise the guild used will be either the previously used guild or null.
     *
     * @param user - the user that has written a mesage
     * @param message - the message that was written. Must be the raw message (including any possible command triggers)
     */
    fun managePoints(member : Member, message: String?, hasImageAttachment: Boolean) {
        val previousPoints = D/currentGuild/member-"nPoints"
        val awardedPoints: Double = if (Parser `starts with trigger` message!! ) 0.5 else 1.8
        val newPoints = (previousPoints as Double) + awardedPoints
        D/currentGuild/member/"nPoints"+newPoints.toString()
        // TODO Rankup system
        //checkRankUp(currentGuild.getMember(user), previousPoints, currentPoints);
    }

    private fun checkRankUp(member: Member, previousPoints: Double, currentPoints: Double) {
        var breachedPoints = 0.0
        for (i in rankRequirements!!.keys) {
            val requirement = rankRequirements!![i]!!.right
            if (currentPoints >= requirement && requirement > breachedPoints && !member.roles.contains(
                    currentGuild.getRolesByName(
                        rankRequirements!![i]!!.left, false
                    )[0]
                )
            ) {
                currentGuild.addRoleToMember(
                    member,
                    currentGuild.getRolesByName(rankRequirements!![i]!!.left, false)[0]
                ).complete()
                currentGuild.removeRoleFromMember(
                    member,
                    currentGuild.getRolesByName(rankRequirements!![i - 1]!!.left, false)[0]
                ).complete()
                currentGuild.getTextChannelsByName(
                    "bot-commands",
                    false
                )[0].sendMessage("Congratulations you have been promoted").complete()
                breachedPoints = requirement
            }
        }
    }

    fun sendDogeMessage(channel: MessageChannel) {
        var builder = EmbedBuilder()
        builder = builder.setTitle("Doge").setDescription("this is a doge")
            .setThumbnail("http://clipartbarn.com/wp-content/uploads/2016/10/Thumbs-up-thumb-clip-art-at-vector.jpg")
            .setColor(Color(0xff0000))
            .setImage("https://lh6.ggpht.com/Gg2BA4RXi96iE6Zi_hJdloQAZxO6lC6Drpdr7ouKAdCbEcE_Px-1o4r8bg8ku_xzyF4y=h900")
        channel.sendMessageEmbeds(builder.build()).complete()
    }
}