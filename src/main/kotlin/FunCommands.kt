import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.converters.coalescedString
import com.kotlindiscord.kord.extensions.commands.converters.optionalMember
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.Color
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.reply
import dev.kord.core.sorted
import io.ktor.client.*
import io.ktor.client.engine.java.*
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
    class SayArgs: Arguments() {
        val quote by coalescedString("quote", "Whatever it is you want me to say")
    }
    class GoogleArgs: Arguments() {
        val member by optionalMember("member", "Who would you like to google for?")
        val query by coalescedString("query", "A query to google")
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
                if (!message.mentionsEveryone) {
                    channel.createMessage(arguments.quote)
                } else {
                    channel.createMessage("${message.author?.mention} tried mentioning everyone")
                }
            }
        }
        // google command
        command(::GoogleArgs) {
            name = "google"
            aliases = arrayOf("lmgtfy", "g")
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
                    description = "${author?.mention}, Have you tried googling **[${arguments.query}]($google)**?"
                    color = _color
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
    val response = client.get<JsonElement>("https://dog.ceo/api/breeds/image/random")
    return response.jsonObject["message"]?.jsonPrimitive?.content ?: "https://apple.co/2RveQ5W"
}