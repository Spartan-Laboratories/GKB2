package com.spartanlabs.bot.commands

import com.spartanlabs.bottools.botactions.contains
import com.spartanlabs.bottools.botactions.online.connector
import com.spartanlabs.bottools.botactions.say
import com.spartanlabs.bottools.commands.GameStatsCommand
import com.spartanlabs.bottools.main.Bot
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import com.spartanlabs.generaltools.to

class DotaCommand: GameStatsCommand("dota2","https://www.dotabuff.com") {
    private val patchNoteURL = "https://steamdb.info/api/PatchnotesRSS/?appid=570"
    init {
        //createPatchNotesService()
        //createdUserGamesService()
    }
    override fun invoke(args: Array<String>) {}
    protected fun openDB(urlSuffix: String, actions:()->Unit) = open("$primaryAddress/$urlSuffix", actions)
    override fun lastGame(args: Array<String>, auto: Boolean) = showLastGameStats(auto)
    override fun showStats(args: Array<String>) {
        println("show stats test proc")
    }


    override fun postPatchNotes(value: Array<String>){
        debug("post patch notes test proc")
        data = connectViaSkrape(patchNoteURL)
        mapValueByKey("dota2patchnotes")
        com.spartanlabs.bottools.botactions.tts(
            jda.getGuildsByName("me", true)[0].defaultChannel as MessageChannel,
            valueMap["dota2patchnotes"]!!
        )
    }

    private fun showLastGameStats(auto: Boolean = false) {
        openDB("players/$user_ID") {
            mapValueByKey("dbicon")
        }
        openDB("players/$user_ID/matches") {
            mapValueByKey("steam name");
            mapValueByKey("hero name")
            "hero image".let { valueMap.put(it, getValueFromKey(it))}
            mapValueByKey("match outcome", "kills", "deaths", "assists")
        }
        openDB("players/$user_ID/matches"){
            mapValueByKey("match id");
        }
        val matchID = valueMap["match id"]
        openDB("matches/$matchID") {
            mapValueByKey("duration", "radiant score", "dire score")
            cutToAfter(valueMap["steam name"]!!)
            mapValueByKey("player lane", "player role", "player lane outcome")
            mapValueByKey("net worth", "lasthit", "deny", "gpm", "xpm", "damage", "healing")
            if (valueMap["healing"]!!.startsWith("<"))
                valueMap["healing"] = ""
            mapValueByKey("building damage")
        }
        getImageLine()

        // Talents
        var talentName: String
        var isActive: Boolean
        valueMap["talents description"] = ""
        openDB("matches/$matchID") {
            //Navigate to the talents line
            cutToAfter("talents cell-divider")
            cutToAfter(valueMap["hero name"]!!)
            for (i in 0..7) {
                cutToAfter("talent-cell")
                isActive = data.startsWith(" active")
                talentName = getValueFromKey("talent")
                if (isActive) talentName = "**$talentName**"
                talentName += if (i % 2 == 0) " vs " else "\n"
                valueMap["talents description"] += talentName
            }
        }

        // State who played the game
        if (auto) Bot say "${member.asMention} played a DOTA2 game" in channel

        // Build the embed based on the values in the valuemap
        valueMap.let {eb
            .setColor(if(it["match outcome"]!!.lowercase() == "won") Color.GREEN else Color.RED)
            .setAuthor(it["steam name"], "$primaryAddress/players/$user_ID", it["dbicon"])
            .setTitle(it["hero name"], "https://www.dotabuff.com/matches/$matchID")
            .setThumbnail("http://dotabuff.com${it["hero image"]}")
            .setTitle(it["match outcome"]!!.uppercase(),"https://www.dotabuff.com/matches/$matchID")
            .setDescription("Radiant  ${it["radiant score"]} : ${it["dire score"]}  Dire\nDuration: ${it["duration"]}")
            .addField("Lane", "${it["player lane"]}\n${it["player role"]}\n${it["player lane outcome"]}", true)
            .addField("K/D/A", "${it["kills"]}\n${it["deaths"]}\n${it["assists"]}", true)
            .addField("Farm", "LH/DN: \t\t${it["lasthit"]}/${it["deny"]}\nGPM/XPM:   ${it["gpm"]}/${it["xpm"]}\nNet Worth:   ${it["net worth"]}", true)
            .addField("Damage", "Hero Damage:     ${it["damage"]}\nHealing:      ${it["healing"]}\nTower Damage:  ${it["building damage"]}", true) //.addField("Talents", talentDesc, true)
            .setImage("attachment://itemline.png")
            .setFooter(// Make sure that the footer states whether this is an automatic post or not, and if not then who requested it
                if (auto) "Automated message" else "Requested by " + member.effectiveName,
                if (auto) jda.selfUser.avatarUrl else member.user.avatarUrl
            )
        }
        sendEmbed(channel, "res/itemline.png")
        eb.setAuthor("Talents").setDescription(valueMap["talents description"])
        sendEmbed(channel)

    }
    private fun getImageLine() {
        val itemline: BufferedImage
        try {
            val numImages = getImages()
            val imgSize = 48
            itemline = BufferedImage(numImages * imgSize, imgSize, BufferedImage.TYPE_INT_ARGB)
            val g = itemline.graphics

            // Draw images
            var itemImage: BufferedImage
            for (i in 1..numImages) {
                itemImage = ImageIO.read(File("res/img$i.png"))
                g.drawImage(
                    itemImage,
                    imgSize * (i - 1),
                    0,
                    imgSize * i,
                    imgSize,
                    0,
                    0,
                    itemImage.width,
                    itemImage.height,
                    null
                )
            }
            itemline to "res/itemline"
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun getImages(): Int {
        for (i in 1..6) {
            connector download ("https://www.dotabuff.com${getValueFromKey("item image")}") to "res/img$i"
            cutToAfter("</a></div></div>")
            if (!data.startsWith("<div class=\"match-item-with-time")) return i
        }
        return 6
    }
}