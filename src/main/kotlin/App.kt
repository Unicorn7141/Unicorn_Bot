import com.kotlindiscord.kord.extensions.ExtensibleBot

val TOKEN = System.getenv("TOKEN") ?: error("Couldn't find a system argument called \'TOKEN\'")

suspend fun main() {
    val bot = ExtensibleBot(TOKEN) {
        // Message commands handler
        messageCommands {
            defaultPrefix = "u!"
            invokeOnMention = true // allow mentioning the bot function as a prefix
        }

        extensions {
            help = false

            // adding the extensions containing the commands
            add(::HelperExtension) // custom help command
            add(::FunCommands) // fun commands
        }
    }

    bot.start()
}