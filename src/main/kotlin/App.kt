import com.kotlindiscord.kord.extensions.ExtensibleBot
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.select

val TOKEN = System.getenv("TOKEN") ?: error("Couldn't find a system argument called \'TOKEN\'")
const val PREFIX = "u!"

@OptIn(PrivilegedIntent::class)
suspend fun main() {

    val bot = ExtensibleBot(TOKEN) {
        messageCommands {
            defaultPrefix = PREFIX
            invokeOnMention = true
            prefix { defaultPrefix ->
                serverCache[guildId?.asString] ?: defaultPrefix
            }
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
            add(::ModerationCommands) // moderation commands
        }
    }

    bot.start()
}

// creating caches
val serverCache = mutableMapOf<String, String>()

// database
val database = Database.connect("jdbc:sqlite:file:data.db", "org.sqlite.JDBC")
val db = database.connector
object Server: IntIdTable("Servers") {
    val guildId = varchar("gID", 20).uniqueIndex()
    var prefix = varchar("prefix", 10).default(PREFIX)
}


// cache related functions
