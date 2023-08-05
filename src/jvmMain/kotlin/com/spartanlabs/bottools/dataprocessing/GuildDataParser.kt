package com.spartanlabs.bottools.dataprocessing

/*
@Suppress("unused")
class GuildDataParser(private val reader: BaseXMLReader = BaseXMLReader()) : XMLReader by reader {
    val log = LoggerFactory.getLogger(GuildDataParser::class.java)

    private lateinit var guildPath : String
    private val playlistDecoder = PlaylistDecoder()
    private lateinit var member : Member
    operator fun div(guild: Guild) = this inServer guild
    operator fun div(person : Member) = this forMember person
    operator fun div(nodeName:String) = reader.root/nodeName
    infix fun inServer(guild : Guild) : GuildDataParser {
        this setPathTo guild
        reader setDocument guildPath + "GuildData.xml"
        return this
    }
    infix fun forMember(member: Member) : GuildDataParser{
        reader setDocument guildPath + "MemberData.xml"
        this.member = member
        return this
    }
    infix fun read(dataValue : String) : String = when(dataValue.lowercase().replace(" ", "")){
        "promotionpoints"   ->  this getPromotionPoints member
        "dsttrackingstate"  ->  this isDSTTrackingOn    member
        "timezone"          ->  this getTimeZone        member
        else -> {
            val idNode = this goTo member getChild "IDs"
            if(idNode has dataValue)
                this getValue idNode
            else ""
        }
    }


    private infix fun getPromotionPoints(member : Member): String = this goTo member getChild "nPoints" then ::getValue

    private infix fun getValue(node :Node) = node.getValue()

    @Synchronized
    fun updateServerDatabase() {
        // DO NOT DELETE    use if the infinite whitespace in xml files bug occurs again
        //for(Guild g: Botmain.jda.getGuilds())Trimmer.trim("C:\\Users\\spart\\OneDrive\\Documents\\Programming\\workspace\\TrumpBotTest\\guildData\\" + g.getName() + "\\MemberData.xml");
        Bot.jda!!.guilds.forEach(::updateServerDatabase)
        //Botmain.out("Finished updating server member database")
    }

    private fun updateServerDatabase(guild: Guild) {
        val filedMembers = attemptRetrieveNames(guild)
        val actualMembers = guild.loadMembers().get()
        for (member in actualMembers) {
            log.debug("Found guild member named: ${member.user.name}")
            if(member.user.name !in filedMembers)
                createMemberEntry()
        }
    }
    private fun createMemberEntry(){
        log.debug("This member is not yet in the database. Creating new data node...")
        reader at reader.root createCategory "Person"
            reader create "tName"       containing member.user.name
            reader create "keyid"
            reader create "nPoints"     containing "0.0"
            reader create "match"       containing "none"
            reader create "requestedby" containing "none"
            reader create "money"       containing "1000"
            reader create "IDs"

            reader createCategory "time"
                reader create "timezone"    containing "none"
                reader create "dston"       containing "false"
                reader create "dststart"    containing "none"
                reader create "dstend"      containing "none"
    }

    fun setDSTon(guild: Guild, member: Member, on: Boolean) {
        setPathTo(guild)
        reader.setDocument(guildPath + "MemberData.xml")
        val person = getPersonNode(member.user.name)
        val dston: Node = reader.getChild(reader.getChild(person, "time"), "dston")
        reader.setValue(dston, if (on) "on" else "off")
    }

    private infix fun isDSTTrackingOn(member: Member): String = this goTo member getChild "time" getChild "dston" then ::getValue

    private infix fun attemptRetrieveNames(guild: Guild): ArrayList<String> {
        setPathTo(guild)
        reader setDocument (guildPath + "MemberData.xml")
        return try {
            retriveNames()
        } catch (e: Exception) {
            println("Could not retrieve names for the guild " + guild.name)
            e.printStackTrace()
            ArrayList(0)
        }
    }

    fun getRankReqs(guild: Guild): HashMap<Int, ImmutablePair<String, Double>> {
        setPathTo(guild)
        reader.setDocument(guildPath + "autopromotionranks.xml")
        val reqs = HashMap<Int, ImmutablePair<String, Double>>()
        var rank: Node = reader.stepDown(reader.root) ?: return reqs
        var rankName: String
        var pointReq: Double
        var nameNode: Node
        var i = 0
        reqs[i++] = ImmutablePair(getDefaultRole(guild).name, 0.0)
        do {
            nameNode = reader.stepDown(rank)
            rankName = reader.getValue(nameNode)
            pointReq = reader.getValue(reader.stepOver(nameNode)).toDouble()
            log.debug("Putting: $rankName, $pointReq")
            reqs[i++] = ImmutablePair(rankName, pointReq)
        } while (reader.stepOver(rank).also { rank = it } != null)
        return reqs
    }

    fun addPoints(member: Member, amount: Double): Double {
        log.debug("Starting addPoints() with: " + member.user.name)
        val person = getPersonNode(member.user.name)
        log.debug("Person: " + person.nodeName)
        val pointsNode = getPointsNode(person)
        val currentAmount: Double = reader.getValue(pointsNode).toDouble()
        log.debug("Points: $currentAmount")
        reader.setValue(pointsNode, (amount + currentAmount).toString())
        val finalValue: Double = reader.getValue(pointsNode).toDouble()
        log.debug("Final value: $finalValue")
        return finalValue
    }

    fun setPoints(user: User, amount: Double) {
        val person = getPersonNode(user.name)
        val pointsNode = getPointsNode(person)
        val currentAmount: Double = reader.getValue(pointsNode).toDouble()
        reader.setValue(pointsNode, amount.toString())
    }

    fun getDefaultRole(guild: Guild): Role {
        setPathTo(guild)
        reader.setDocument(guildPath + "GuildData.xml")
        val roleList = guild.getRolesByName(reader.getValue(reader.stepDown(reader.root)), true)
        return if (roleList.size > 0) roleList[0] else guild.publicRole
    }

    fun getWelcomeMessage(guild: Guild): String {
        setPathTo(guild)
        reader.setDocument(guildPath + "GuildData.xml")
        val root: Node = reader.root
        val defaultRole: Node = reader.stepDown(root)
        val welcomeMessage: Node = reader.stepOver(defaultRole)
        return reader.getValue(welcomeMessage)
    }
    fun setWelcomeMessage(guild: Guild, message: String) {
        setPathTo(guild)
        reader.setDocument(guildPath + "GuildData.xml")
        var node: Node = reader.stepDown(reader.root)
        node = reader.stepOver(node, 2)
        reader.setValue(node, message)
    }
    private fun retriveNames(): List<String> =
        with(root.children()){
            List(size){index -> get(index).nodeName}
        }
    private infix fun goTo(member : Member) : Node = this getPersonNode member.user.name
    private infix fun getPersonNode(username: String): Node {
        var username = username
        username = username.lowercase(Locale.getDefault())
        log.debug(31.toChar().toString() + "Starting getPersonNode function with: " + username)
        reader.setDocument(guildPath + "MemberData.xml")
        var person = firstPerson
        log.debug(32.toChar().toString() + "First person is: " + reader.getValue(reader.stepDown(person)))
        while (person != null) {
            val nameNode: Node = reader.stepDown(person)
            val name: String = reader.getValue(nameNode).toLowerCase()
            log.debug("Found person: $name")
            if (name.lowercase(Locale.getDefault()) == username) break
            person = reader.stepOver(person)
        }
        log.debug("Ending getPersonNode function with: $username")
        return person
    }

    private val firstPerson: Node
        private get() = reader.stepDown(reader.root)

    private infix fun getPointsNode(personNode: Node): Node {
        log.debug("Starting getPointsNode function with: $personNode")
        val nameTag: Node = reader.stepDown(personNode)
        log.debug("getPointsNode() Teh name of the person is: " + reader.getValue(nameTag))
        val pointsNode: Node = reader.stepOver(nameTag)
        log.debug("getPointsNode() returning $pointsNode")
        return pointsNode
    }

    private infix fun setPathTo(guild: Guild) : GuildDataParser{
        guildPath = "guildData/" + guild.name + "/"
        return this
    }

    fun setPlaylist(guild: Guild, playlistName: String?) {
        playlistDecoder.setPlayList(guild, playlistName)
    }

    fun nextSong(): String? = playlistDecoder.nextSong

    fun makePlaylist(guild: Guild, playlistName: String) = playlistDecoder.makePlaylist(guild, playlistName)

    fun addSong(guild: Guild, name: String) = playlistDecoder.addSong(guild, name)

    internal inner class PlaylistDecoder {
        private var playlist: String? = null
        private var onSong = 0
        fun setPlayList(guild: Guild, playlist: String?) {
            this.playlist = playlist
            onSong = 0
            setDoc(guild)
        }

        val nextSong: String?
            get() {
                var node = findPlaylist(playlist)
                node = reader.stepDown(node)
                node = reader.stepOver(node, onSong++)
                return if (node == null) null else reader.getValue(node)
            }

        fun makePlaylist(guild: Guild, playlist: String) {
            setDoc(guild)
            reader.newChild(reader.root, "Playlist", playlist)
        }

        fun addSong(guild: Guild, name: String) {
            setDoc(guild)
            reader.newChild(findPlaylist(playlist), "song", name)
        }

        fun listPlaylists(guild: Guild): ArrayList<String> {
            val playlists = ArrayList<String>()
            setDoc(guild)
            var playlist: Node = reader.stepDown(reader.root)
            while (playlist != null) {
                playlists.add(reader.getValue(playlist))
                playlist = reader.stepOver(playlist)
            }
            return playlists
        }

        private fun findPlaylist(name: String?): Node {
            var name = name
            var node: Node
            name = name!!.lowercase(Locale.getDefault())
            node = reader.stepDown(reader.root)
            while (node != null && !reader.getValue(node).toLowerCase().startsWith(name)) {
                log.debug("Found playlist: " + reader.getValue(node))
                node = reader.stepOver(node)
            }
            return node
        }

        private fun setDoc(guild: Guild) {
            setPathTo(guild)
            reader.setDocument(guildPath + "Playlists.xml")
        }

        fun listSongs(guild: Guild, playlistName: String?): ArrayList<String> {
            setDoc(guild)
            val songNames = ArrayList<String>()
            val playlist = findPlaylist(playlistName)
            var song: Node = reader.stepDown(playlist)
            while (song != null) {
                songNames.add(reader.getValue(song))
                song = reader.stepOver(song)
            }
            return songNames
        }
    }

    fun listPlaylists(guild: Guild): ArrayList<String> {
        return playlistDecoder.listPlaylists(guild)
    }

    fun listSongs(guild: Guild, string: String?): ArrayList<String> {
        return playlistDecoder.listSongs(guild, string)
    }

    infix fun getTimeZone(member : Member): String = this goTo member getChild "time" getChild "timezone" then ::getValue

    fun setTimeZone(guild: Guild, member: Member, timezone: Int) {
        setPathTo(guild)
        reader.setDocument(guildPath + "MemberData.xml")
        val personNode = getPersonNode(member.user.name)
        val timeNode: Node = reader.getChild(personNode, "time")
        val timezoneNode: Node = reader.getChild(timeNode, "timezone")
        reader.setValue(timezoneNode, timezone.toString())
    }

    fun setDST(guild: Guild, member: Member, start: Boolean, rawInput: String) {
        setPathTo(guild)
        reader.setDocument(guildPath + "MemberData.xml")
        val personNode = getPersonNode(member.user.name)
        val timeNode: Node = reader.getChild(personNode, "time")
        val dstNode: Node = reader.getChild(timeNode, if (start) "dststart" else "dstend")
        reader.setValue(dstNode, rawInput)
    }

    fun getDST(guild: Guild, member: Member, start: Boolean): String {
        setPathTo(guild)
        reader.setDocument(guildPath + "MemberData.xml")
        val personNode = getPersonNode(member.user.name)
        val timeNode: Node = reader.getChild(personNode, "time")
        val dstNode: Node = reader.getChild(timeNode, if (start) "dststart" else "dstend")
        return reader.getValue(dstNode)
    }

    fun addDuplicatedChannel(own: Guild, source: Guild, channel: TextChannel) {
        setPathTo(own)
        reader.setDocument(guildPath + "DuplicatedChannels.xml")
        val root: Node = reader.root
        val id = source.id.replace("\n".toRegex(), "")
        lateinit var sourceGuildNode: Node
        var foundExisting = false
        for (child in reader.getChildren(reader.root)) if (reader.getValue(child).replace("\n", "").equals(id)) {
            foundExisting = true
            sourceGuildNode = child
            break
        }
        if (!foundExisting) sourceGuildNode = reader.newChild(root, "Server", id)
        reader.newChild(sourceGuildNode, "Channel", channel.id)
    }

    val duplicates: HashMap<Guild, HashMap<Guild, ArrayList<TextChannel>>>
        get() {
            val duplicates = HashMap<Guild, HashMap<Guild, ArrayList<TextChannel>>>()
            for (g in Bot.jda!!.guilds) {
                duplicates[g] = HashMap()
                setPathTo(g)
                reader.setDocument(guildPath + "DuplicatedChannels.xml")
                for (n in reader.getChildren(reader.root)) {
                    val sourceGuild: Guild = Bot.jda!!.getGuildById(reader.getValue(n).replace("\n", "").trim())!!
                    duplicates[g]!![sourceGuild] = ArrayList()
                    for (channelNode in reader.getChildren(n))
                        sourceGuild.getTextChannelById(reader.getValue(channelNode))
                            ?.let { duplicates[g]!![sourceGuild]!!.add(it) }
                }
            }
            return duplicates
        }

    fun addTag(fileName: String, name: String, value: String) {
        for (guild in Bot.jda!!.guilds) {
            setPathTo(guild)
            reader.setDocument("$guildPath$fileName.xml")
            lateinit var parentNode: Node
            parentNode = when (fileName.lowercase(

            )) {
                "memberdata" -> reader.stepDown(reader.root)
                "guilddata" -> reader.root
                else -> throw IllegalArgumentException("invalid file name")
            }
            do {
                reader.newChild(parentNode, name, value)
            } while (reader.stepOver(parentNode).also { parentNode = it } != null)
        }
    }

    fun removeTag(fileName: String, tag: String) {
        for (guild in Bot.jda!!.guilds) {
            setPathTo(guild)
            reader.setDocument("$guildPath$fileName.xml")
            var parentNode: Node = when (fileName.lowercase()) {
                "memberdata" -> reader.stepDown(reader.root)
                "guilddata" -> reader.root
                else -> throw IllegalArgumentException("invalid file name")
            }
            do reader.removeTag(parentNode, tag)
            while (reader.stepOver(parentNode).also { parentNode = it } != null)
        }
    }

    fun getLastMatchID(guild: Guild, member: Member): String {
        setPathTo(guild)
        reader.setDocument(guildPath + "MemberData.xml")
        val person = getPersonNode(member.user.name)
        val id: Node = reader.getChild(person, "match")
        return reader.getValue(id)
    }

    fun setMatch(guild: Guild, member: Member, newID: String) {
        setPathTo(guild)
        reader.setDocument(guildPath + "MemberData.xml")
        val person = getPersonNode(member.user.name)
        val id: Node = reader.getChild(person, "match")
        reader.setValue(id, newID)
    }

    fun getD2MatchChannel(guild: Guild): String {
        setPathTo(guild)
        reader.setDocument(guildPath + "GuildData.xml")
        val channelIDNode: Node = reader.getChild(reader.root, "d2matchinfochannel")
        return reader.getValue(channelIDNode)
    }

    fun setD2MatchChannel(guild: Guild, channelName: String) {
        setPathTo(guild)
        reader.setDocument(guildPath + "GuildData.xml")
        val channelIDNode: Node = reader.getChild(reader.root, "d2matchinfochannel")
        System.out.println("Before: " + reader.getValue(channelIDNode))
        reader.setValue(channelIDNode, channelName)
        System.out.println("After: " + reader.getValue(channelIDNode))
    }

    fun addRequest(guild: Guild, requested: Member, requester: Member) {
        setPathTo(guild)
        reader.setDocument(guildPath + "MemberData.xml")
        val requestedName = requested.user.name
        val requesterName = requester.user.name
        val requestedPersonNode = getPersonNode(requestedName)
        val requestedBy: Node = reader.getChild(requestedPersonNode, "requestedby")
        if (reader.getValue(requestedBy).equals("none") || reader.getValue(requestedBy).equals("")) reader.setValue(
            requestedBy,
            "$requesterName "
        ) else reader.setValue(requestedBy, reader.getValue(requestedBy) + "$requesterName ")
    }

    fun removeRequest(guild: Guild, requested: Member, requester: Member) {
        setPathTo(guild)
        reader.setDocument(guildPath + "MemberData.xml")
        val requestedName = requested.user.name
        val requesterName = requester.user.name
        val personNode = getPersonNode(requestedName)
        val requestedby: Node = reader.getChild(personNode, "requestedby")
        val requests: String = reader.getValue(requestedby)
        requests.replaceFirst("$requesterName ".toRegex(), "")
    }

    fun getRequesters(guild: Guild, requested: User): String {
        setPathTo(guild)
        reader.setDocument(guildPath + "MemberData.xml")
        val personNode = getPersonNode(requested.name)
        return reader.getValue(reader.getChild(personNode, "requestedby"))
    }

    fun getContents(guild: Guild, person: String): Array<String> {
        setPathTo(guild)
        reader.setDocument(guildPath + "MemberData.xml")
        val personNode = getPersonNode(person)
        val contents = personNode.childNodes
        return Array(contents.length, contents::getNodeNameAt)
    }

    fun removeSongFromPlaylist(guild: Guild, song: String, playlist: String) {
        setPathTo(guild)
        reader.setDocument(guildPath + "Playlists.xml")
        val root: Node = reader.root
        lateinit var playlistNode: Node
        for (node in reader.getChildren(root)) if (reader.getValue(node) == playlist) playlistNode = node
        reader.removeTagByText(playlistNode, song)
    }

    var recordedPatch: String
        get() {
            reader.setDocument("BotData.xml")
            val root: Node = reader.root
            val patchNode: Node = reader.getChild(root, "dotapatch")
            return reader.getValue(patchNode)
        }
        set(patch) {
            reader.setDocument("BotData.xml")
            val root: Node = reader.root
            val patchNode: Node = reader.getChild(root, "dotapatch")
            reader.setValue(patchNode, patch)
        }

    fun getDotaPatchNotesChannel(guild: Guild): String {
        setPathTo(guild)
        reader.setDocument(guildPath + "GuildData.xml")
        val root: Node = reader.root
        val channel: Node = reader.getChild(root, "dotapatchnoteschannel")
        return reader.getValue(channel)
    }

    fun setDotaPatchNotesChannel(guild: Guild, channelName: String) {
        setPathTo(guild)
        reader.setDocument(guildPath + "GuildData.xml")
        val root: Node = reader.root
        val channel: Node = reader.getChild(root, "dotapatchnoteschannel")
        reader.setValue(channel, channelName)
    }

    val catCounter: String
        get() {
            reader.setDocument("Botdata.xml")
            val counterNode: Node = reader.getChild(reader.root, "catcounter")
            val counterText: String = reader.getValue(counterNode)
            val counter = counterText.toInt() + 1
            reader.setValue(counterNode, counter.toString())
            return counterText
        }

    fun setGreetActive(guild: Guild, valueOf: String) {
        setPathTo(guild)
        reader.setDocument(guildPath + "GuildData.xml")
        var greetingActive: Node
    }

    fun setGameID(guild: Guild, member: Member, gameName: String, memberID: String) {
        setPathTo(guild)
        reader.setDocument(guildPath + "MemberData.xml")
        val personNode = getPersonNode(member.user.name)
        val idNode = reader.getChild(personNode, "IDs")
        if (idNode has gameName) idNode getChild gameName setValue memberID
        else reader.newChild(idNode, gameName, memberID)
    }

    infix fun getID(gameName: String): String? {
        reader setDocument guildPath + "MemberData.xml"
        return this goTo member getChild "IDs" getChild gameName then ::getValue
    }

    fun setMoney(guild: Guild, member: Member, value: Int) {
        setPathTo(guild)
        reader.setDocument(guildPath + "MemberData.xml")
        val personNode = getPersonNode(member.user.name)
        reader.setValue(reader.getChild(personNode, "money"), value.toString())
    }

    fun getMoney(guild: Guild, member: Member): Int {
        setPathTo(guild)
        reader.setDocument(guildPath + "MemberData.xml")
        val personNode = getPersonNode(member.user.name)
        if (!reader.nodeHasChild(personNode, "money")) reader.newChild(personNode, "money", "1000")
        return reader.getValue(reader.getChild(personNode, "money")).toInt()
    }

    fun setWelcomeMessageID(guild: Guild, channel: TextChannel, message: Message) {
        setDocument(guild, "GuildData.xml")
        val parent: Node = reader.getChild(reader.root, "welcomeMessages")
        val id: Node = reader.newChild(parent, "welcomeMessage")
        reader.newChild(id, "channel", channel.id)
        reader.newChild(id, "message", message.id)
    }

    fun getWelcomeMessageIDs(guild: Guild): List<Pair<String, String>> {
        setDocument(guild, "GuildData.xml")
        val welcomeMessageIDs = ArrayList<Pair<String, String>>()
        reader.getChildren(reader.getChild(reader.root, "welcomeMessages")).forEach { parent ->
            val channel: Node = reader.getChild(parent, "channel")
            val message: Node = reader.getChild(parent, "message")
            welcomeMessageIDs.add(
                Pair.of(
                    reader.getValue(channel),
                    reader.getValue(message)
                )
            )
        }
        return welcomeMessageIDs
    }

    fun addReactionRole(guild: Guild, emote: Emoji, role: Role) {
        setDocument(guild, "GuildData.xml")
        val rrsNode: Node = reader.getChild(reader.root, "reactionRoles")
        val rrNode: Node = reader.newChild(rrsNode, "reactionRole")
        reader.newChild(rrNode, "emote", emote.name)
        reader.newChild(rrNode, "role", role.id)
    }

    fun getGameEmoteIDs(guild: Guild): List<String> {
        setDocument(guild, "GuildData.xml")
        val rrsNode: Node = reader.getChild(reader.root, "reactionRoles")
        val emoteIDs = ArrayList<String>()
        reader.getChildren(rrsNode).forEach { combination ->
            emoteIDs.add(
                reader.getValue(
                    reader.getChild(
                        combination,
                        "emote"
                    )
                )
            )
        }
        return emoteIDs
    }

    fun getCorrespondingRoleID(emote: Emoji): String? {
        val rrsNode: Node = reader.getChild(reader.root, "reactionRoles")
        for (n in reader.getChildren(rrsNode)) if (reader.getValue(reader.getChild(n, "emote"))
                .equals(emote.name)
        ) return reader.getValue(reader.getChild(n, "role"))
        return null
    }

    protected fun setDocument(guild: Guild, docName: String) {
        setPathTo(guild)
        reader.setDocument(guildPath + docName)
    }

    fun createGuildDatabase(guild: Guild) {
        setPathTo(guild)
        File(guildPath).mkdirs()
        val guildData = listOf(
            "<data>",
            "<defaultRole>online</defaultRole>",
            "<welcomeMessage>hello</welcomeMessage>",
            "<d2matchinfochannel>none</d2matchinfochannel>",
            "<dotapatchnoteschannel>none</dotapatchnoteschannel>",
            "<welcomeMessages></welcomeMessages>",
            "<reactionRoles></reactionRoles>",
            "</data>",
        )
        val guildDataPath = Paths.get(guildPath + "GuildData.xml")
        val people = Arrays.asList("<People>\n", "</People>")
        val peoplePath = Paths.get(guildPath + "MemberData.xml")
        val ranks = Arrays.asList("<Enlisted>\n", "</Enlisted>")
        val ranksPath = Paths.get(guildPath + "AutoPromotionRanks.xml")
        val duplicatedChannels = Arrays.asList("<Duplicates>\n", "</Duplicates>")
        val duplicatesPath = Paths.get(guildPath + "DuplicatedChannels.xml")
        try {
            Files.write(guildDataPath, guildData, Charset.forName("UTF-8"))
            Files.write(peoplePath, people, Charset.forName("UTF-8"))
            Files.write(ranksPath, ranks, Charset.forName("UTF-8"))
            Files.write(duplicatesPath, duplicatedChannels, Charset.forName("UTF-8"))
        } catch (e: IOException) {
            e.printStackTrace()
        }
        updateServerDatabase(guild)
    }

    fun modBotDataValue(name: String, value: String) {
        reader.setDocument("BotData.xml")
        modCurrentDocValue(name, value)
    }

    fun getBotDataValue(name: String): String {
        reader.setDocument("BotData.xml")
        return getCurrentDocValue(name)
    }

    fun modGuildDataValue(guild: Guild, name: String, value: String) {
        setDocument(guild, "GuildData.xml")
        modCurrentDocValue(name, value)
    }

    fun getGuildDataValue(guild: Guild, name: String): String {
        setDocument(guild, "GuildData.xml")
        return getCurrentDocValue(name)
    }

    private fun modCurrentDocValue(name: String, value: String) {
        val node: Node = reader.getChild(reader.root, name)
        reader.setValue(node, value)
    }

    private fun getCurrentDocValue(name: String): String {
        val node: Node = reader.getChild(reader.root, name)
        return reader.getValue(node)
    }

    fun removeWelcomeMessage(guild: Guild, messageId: String) {
        setDocument(guild, "GuildData.xml")
        val welcomeMessages: Node = reader.getChild(reader.root, "welcomeMessages")
        lateinit var deletedMessage: Node
        for (welcomeMessage in reader.getChildren(welcomeMessages)) {
            val messageNode: Node = reader.getChild(welcomeMessage, "message")
            val messageID: String = reader.getValue(messageNode)
            if (messageID == messageId) {
                deletedMessage = welcomeMessage
                break
            }
        }
        reader.removePossibleTag(welcomeMessages, deletedMessage)
    }
}

 */