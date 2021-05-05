import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.converters.coalescedString
import com.kotlindiscord.kord.extensions.commands.converters.optionalMember
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.ensureWebhook
import dev.kord.common.Color
import dev.kord.core.behavior.GuildApplicationCommandBehavior
import dev.kord.core.behavior.WebhookBehavior
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.execute
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.sorted
import kotlinx.coroutines.flow.toList

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
    }

}