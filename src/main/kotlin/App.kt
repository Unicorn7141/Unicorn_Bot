import com.kotlindiscord.kord.extensions.ExtensibleBot
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent

val TOKEN = System.getenv("TOKEN") ?: error("Couldn't find a system argument called \'TOKEN\'")

@OptIn(PrivilegedIntent::class)
suspend fun main() {

    val bot = ExtensibleBot(TOKEN) {
        messageCommands {
            defaultPrefix = "u!"
            invokeOnMention = true
        }

        intents {
            + Intents.all
        }

        members {
            fillPresences = true
            all()
        }

        extensions {
            help = false // disable default help
            add(::HelperExtension) // help command
            add(::FunCommands) // fun commands
        }
    }

    bot.start()
}