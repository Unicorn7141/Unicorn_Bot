import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.converters.coalescedString
import com.kotlindiscord.kord.extensions.commands.converters.optionalMember
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.pagination.Paginator
import com.kotlindiscord.kord.extensions.pagination.pages.Page
import com.kotlindiscord.kord.extensions.pagination.pages.Pages
import dev.kord.common.Color
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Member
import dev.kord.core.sorted
import dev.kord.rest.request.KtorRequestException
import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class FunCommands(bot: ExtensibleBot): Extension(bot) {
    override val name = "Fun Commands"

    /* Arguments Section */
    class SayArgs : Arguments() {
        val quote by coalescedString("quote", "Whatever it is you want me to say")
    }

    class GoogleArgs : Arguments() {
        val member by optionalMember("member", "Who would you like to google for?")
        val query by coalescedString("query", "A query to google")
    }

    class BallArgs : Arguments() {
        val question by coalescedString("question", "A `yes` or `no` question you want me to answer")
    }

    class LyricsArgs : Arguments() {
        val song by coalescedString("song - author", "The song you'd like getting lyrics for")
    }

    class SongArgs : Arguments() {
        val member by optionalMember("member", "A member to get data about")
    }

    /* Commands Section */
    override suspend fun setup() {
        // say/echo command
        command(::SayArgs) {
            // General definitions
            name = "say"
            aliases = arrayOf("echo")
            description = "Repeats after you"

            // Actual command stuff
            action {
                message.delete()
                if (!message.mentionedUsers.toList().isNullOrEmpty()) {
                    channel.createMessage(arguments.quote)
                } else {
                    channel.createMessage("${message.author?.mention} tried using a mention")
                }
            }
        }
        // google command
        command(::GoogleArgs) {
            name = "lmgtfy"
            aliases = arrayOf("google", "g")
            description = "Let people know how to google stuff, by giving them an example"

            action {
                val roles = when (arguments.member) {
                    null -> event.member?.roles?.sorted()?.toList()
                    else -> arguments.member?.roles?.sorted()?.toList()
                }
                val _color = roles?.reversed()?.firstOrNull { it.color.rgb != 0 }?.color ?: Color(7506394)
                val google = "https://letmegooglethat.com/?q=${arguments.query.replace(" ", "+")}"
                val author = if (arguments.member == null) message.author else arguments.member
                channel.createEmbed {
                    if (message.mentionedUsers.toList().isNullOrEmpty()) {
                        description = "${author?.mention}, Have you tried googling **[${arguments.query}]($google)**?"
                        color = _color
                    } else {
                        description = "Don't try mentioning people"
                        color = Color(java.awt.Color.red.rgb)
                    }
                }
            }
        }
        // random cat
        command {
            name = "cat"
            aliases = arrayOf("kitty")
            description = "Get a random cat"

            action {
                val roles = event.member?.roles?.sorted()?.toList()
                val _color = roles?.reversed()?.firstOrNull { it.color.rgb != 0 }?.color ?: Color(7506394)
                message.reply {
                    allowedMentions()
                    embed {
                        title = "Look at this cutie"
                        image = callCatAPI(client)
                        color = _color
                    }
                }
            }
        }
        // random dog
        command {
            name = "dog"
            aliases = arrayOf("doggo")
            description = "Get a random doggo"

            action {
                val roles = event.member?.roles?.sorted()?.toList()
                val _color = roles?.reversed()?.firstOrNull { it.color.rgb != 0 }?.color ?: Color(7506394)
                message.reply {
                    allowedMentions()
                    embed {
                        title = "This doggo so cuteeeeee"
                        image = callDogAPI(client)
                        color = _color
                    }
                }
            }
        }
        // random fox
        command {
            name = "fox"
            aliases = arrayOf("foxy")
            description = "Get a random fox"

            action {
                val roles = event.member?.roles?.sorted()?.toList()
                val _color = roles?.reversed()?.firstOrNull { it.color.rgb != 0 }?.color ?: Color(7506394)
                message.reply {
                    allowedMentions()
                    embed {
                        title = "Look at this \uD83E\uDD7A"
                        image = callFoxAPI(client)
                        color = _color
                    }
                }
            }
        }
        // 8ball
        command(::BallArgs) {
            name = "8ball"
            aliases = arrayOf("8")
            description = "Predicting an answer to your questions"

            action {
                val action = this
                message.reply {
                    val answer = listOf(
                        " As I see it, yes.", " Ask again later.",
                        " Better not tell you now.", " Cannot predict now.",
                        " Concentrate and ask again.", " Don’t count on it.",
                        " It is certain.", " It is decidedly so.",
                        " Most likely.", " My reply is no.",
                        " My sources say no.", " Outlook not so good.",
                        " Outlook good.", " Reply hazy, try again.",
                        " Signs point to yes.", " Very doubtful.",
                        " Without a doubt.", " Yes.",
                        " Yes – definitely.", " You may rely on it."
                    ).shuffled()[0]
                    allowedMentions()
                    content = answer
                }
            }
        }
        // lyrics
        command(::LyricsArgs) {
            name = "lyrics"
            description = "Get lyrics for songs"

            action {
                val roles = event.member?.roles?.sorted()?.toList()
                val _color = roles?.reversed()?.firstOrNull { it.color.rgb != 0 }?.color ?: Color(7506394)
                val song = callLyricsAPI(client, arguments.song)
                val pages = Pages()
                val songLyrics = song["lyrics"]?.split(" ")?.chunked(100)?.map { it.joinToString(" ") }
                val lyrics = mutableListOf("")
                try {
                    if (songLyrics != null) {
                        for (word in songLyrics) {
                            if ((lyrics.last() + word).length <= 2000) {
                                lyrics[lyrics.lastIndex] += " $word\n\n"
                            } else {
                                lyrics.add("\n\n$word")
                            }
                        }
                        for (page in lyrics.joinToString("\n\n|").split("|")) {
                            pages.addPage(
                                Page(
                                    description = page,
                                    title = song["title"],
                                    author = song["author"],
                                    color = _color
                                )
                            )
                        }
                        Paginator(bot, pages, message.channel, keepEmbed = true).send()
                    } else {
                        message.reply {
                            allowedMentions()
                            content = "Song could not be found"
                        }
                    }
                } catch (e: Exception) {
                    when (e) {
                        is ServerResponseException -> {
                            message.reply {
                                allowedMentions()
                                content = "Song could not be found"
                            }
                        }
                        is KtorRequestException -> {
                            println(lyrics.map { it.length }.joinToString(" | "))
                            message.reply {
                                allowedMentions()
                                content = "A formatting error occurred...."
                            }
                        }
                    }
                }
            }
        }
        // get lyrics
        command() {
            name = "getlyrics"
            aliases = arrayOf("glyrics")
            description = "Get lyrics for the song you're currently playing"

            action {
                val playingSong = getSong(message.getAuthorAsMember()!!)
                if (playingSong != null) {
                    val roles = event.member?.roles?.sorted()?.toList()
                    val _color = roles?.reversed()?.firstOrNull { it.color.rgb != 0 }?.color ?: Color(7506394)
                    val song = callLyricsAPI(client, playingSong)
                    val pages = Pages()
                    val songLyrics = song["lyrics"]?.split(" ")?.chunked(100)?.map { it.joinToString(" ") }
                    val lyrics = mutableListOf("")
                    try {
                        if (songLyrics != null) {
                            for (word in songLyrics) {
                                if ((lyrics.last() + word).length <= 2000) {
                                    lyrics[lyrics.lastIndex] += " $word\n\n"
                                } else {
                                    lyrics.add("\n\n$word")
                                }
                            }
                            for (page in lyrics.joinToString("\n\n|").split("|")) {
                                pages.addPage(
                                    Page(
                                        description = page,
                                        title = song["title"],
                                        author = song["author"],
                                        color = _color
                                    )
                                )
                            }
                            Paginator(bot, pages, message.channel, keepEmbed = true).send()
                        } else {
                            message.reply {
                                allowedMentions()
                                content = "Song could not be found"
                            }
                        }
                    } catch (e: Exception) {
                        when (e) {
                            is ServerResponseException -> {
                                message.reply {
                                    allowedMentions()
                                    content = "Song could not be found"
                                }
                            }
                            is KtorRequestException -> {
                                println(lyrics.map { it.length }.joinToString(" | "))
                                message.reply {
                                    allowedMentions()
                                    content = "A formatting error occurred...."
                                }
                            }
                        }
                    }
                } else {
                    message.reply {
                        allowedMentions()
                        content = "You're not playing any music \uD83D\uDE15"
                    }
                }
            }
        }
    }
}

val client = HttpClient(Java) {
    install(JsonFeature) {
        serializer = KotlinxSerializer()
    }
}


suspend fun callCatAPI(client: HttpClient): String {
    val response = client.get<JsonElement>("https://api.thecatapi.com/v1/images/search")
    return response.jsonArray[0].jsonObject["url"]?.jsonPrimitive?.content ?: "https://apple.co/2RveQ5W"
}

suspend fun callFoxAPI(client: HttpClient): String {
    val response = client.get<JsonElement>("https://randomfox.ca/floof")
    return response.jsonObject["image"]?.jsonPrimitive?.content ?: "https://apple.co/2RveQ5W"
}

suspend fun callDogAPI(client: HttpClient): String {
    return try {
        val response = client.get<JsonElement>("https://dog.ceo/api/breeds/image/random")
        response.jsonObject["message"]?.jsonPrimitive?.content ?: "https://apple.co/2RveQ5W"
    } catch (e: ClientRequestException) {
        "https://apple.co/2RveQ5W"
    }
}

suspend fun callLyricsAPI(client: HttpClient, song: String): Map<String, String> {
    return try {
        val response = client.get<JsonElement>("https://some-random-api.ml/lyrics?title=$song")
        val title = response.jsonObject["title"]?.jsonPrimitive?.content ?: "Unavailable"
        val author = response.jsonObject["author"]?.jsonPrimitive?.content ?: "Unavailable"
        val lyrics = response.jsonObject["lyrics"]?.jsonPrimitive?.content ?: "Unavailable"
        mapOf("title" to title, "author" to author, "lyrics" to lyrics)
    } catch (e: ClientRequestException) {
        mapOf("title" to "Unavailable", "author" to "Unavailable", "lyrics" to "Unavailable")

    }
}

fun buildPages(words: Collection<String>): ArrayList<String> {
    val pages = arrayListOf<String>()
    var builder = StringBuilder()
    for (word in words) {
        if (builder.length + word.length >= 2000) {
            pages.add(builder.toString())
            builder = StringBuilder()
        }
        builder.append(" $word")
    }
    return pages
}

suspend fun getSong(member: Member): String? {
    return if (member.getPresenceOrNull()?.data?.activities?.map { it.party.value?.id?.value }.toString() == "[spotify:${member.id.asString}") {
        member.getPresenceOrNull()?.data?.activities?.joinToString("\n") { "${it.details.value} ${it.state.value}" }
    } else {
        null
    }
}