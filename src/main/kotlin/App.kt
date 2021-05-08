import com.kotlindiscord.kord.extensions.ExtensibleBot
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

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

    transaction {

    }

    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(Server)

        runBlocking {
            for (guild in bot.kord.guilds.toList()) {
                val gID = guild.id.asString
                try {
                    addGuild(gID)
                } catch (e: Exception) {
                    println("[!!] Error: $e")
                }
                serverCache[gID] = getPrefix(gID)
                println("${guild.name} | ${gID} | ${serverCache[gID]}")
            }
        }
    }

    bot.start()
}

// creating caches
val serverCache = mutableMapOf<String, String>()

// database + tables
val database = Database.connect("jdbc:sqlite:file:data.db", "org.sqlite.JDBC")
object Server: IntIdTable("Servers") {
    val guildId = varchar("gID", 20).uniqueIndex()
    var prefix = varchar("prefix", 10).default(PREFIX)
}


// cache related functions
// getters
fun getPrefix(guildId: String): String {
    return Server.slice(Server.prefix).select { Server.guildId eq guildId }.withDistinct().map {
        it[Server.prefix]
    }[0]
}

// updaters
fun updatePrefix(guildId: String, prefix: String) {
    transaction {
        Server.update({ Server.guildId eq guildId }) {
            it[Server.prefix] = prefix
        }
        serverCache[guildId] = prefix
    }
}

// setters
fun addGuild(ID: String) {
    transaction {
        val id = Server.insertAndGetId {
            it[guildId] = ID
            it[prefix] = PREFIX
        }
        serverCache[ID] = PREFIX
        commit()
    }
}