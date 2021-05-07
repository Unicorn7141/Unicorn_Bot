import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.checks.or
import com.kotlindiscord.kord.extensions.commands.converters.coalescedString
import com.kotlindiscord.kord.extensions.commands.converters.member
import com.kotlindiscord.kord.extensions.commands.converters.optionalCoalescedString
import com.kotlindiscord.kord.extensions.commands.converters.user
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.dm
import dev.kord.common.Color
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.ban
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.reply
import dev.kord.rest.Image
import dev.kord.rest.request.KtorRequestException
import kotlinx.coroutines.flow.toList

class ModerationCommands(bot: ExtensibleBot): Extension(bot) {
    override val name = "Moderation Commands"

    /* Arguments */
    class KickArgs : Arguments() {
        val member by member("member", "The member you wanna kick")
        val reason by optionalCoalescedString("reason", "The reason to the kick")
    }

    class BanArgs : Arguments() {
        val member by member("member", "The member you wanna ban")
        val reason by coalescedString("reason", "The reason to the ban")
    }

    class UnbanArgs : Arguments() {
        val member by user("member", "The member to unban")
        val reason by optionalCoalescedString("reason", "The reason to the unbanning")
    }

    override suspend fun setup() {
        // kick
        command(::KickArgs) {
            name = "kick"
            description = "Kick a member from the server"

            requirePermissions(Permission.KickMembers)
            check(or(hasPermission(Permission.KickMembers), hasPermission(Permission.ManageGuild)))
            action {
                val reason = arguments.reason ?: "No reason was supplied"
                val kicked = arguments.member
                val kicker = message.getAuthorAsMember()
                val guild = message.getGuild()

                kicked.dm {
                    embed {
                        title = "You were kicked"
                        thumbnail { url = guild.getIconUrl(Image.Format.PNG) ?: "" }
                        field("From", false) { guild.name }
                        field("By", true) { kicker?.mention ?: "" }
                        field("Reason", true) { reason }
                        color = Color(java.awt.Color.red.rgb)
                    }
                }.also {
                    guild.kick(kicked.id, reason)
                    message.channel.createEmbed {
                        title = "Kicked Successfully"
                        description = "${kicker?.mention} kicked ${kicked.mention}"
                        thumbnail {
                            url = guild.getIconUrl(Image.Format.GIF) ?: guild.getIconUrl(Image.Format.PNG) ?: ""
                        }
                        field("Reason", false) { reason }
                        color = Color(java.awt.Color.red.rgb)
                        footer {
                            icon = kicked.avatar.url
                            text = "In memory of ${kicked.nickname ?: kicked.username}"
                        }
                    }
                }
            }
        }
        // ban
        command(::BanArgs) {
            name = "ban"
            description = "Ban a member from the server"

            requirePermissions(Permission.BanMembers)
            check(or(hasPermission(Permission.BanMembers), hasPermission(Permission.ManageGuild)))
            action {
                val reason = arguments.reason
                val banned = arguments.member
                val banner = message.getAuthorAsMember()
                val guild = message.getGuild()

                banned.dm {
                    embed {
                        title = "You were banned"
                        thumbnail { url = guild.getIconUrl(Image.Format.PNG) ?: "" }
                        field("From", false) { guild.name }
                        field("By", true) { banner?.mention ?: "" }
                        field("Reason", true) { reason }
                    }
                }.also {
                    guild.ban(banned.id) { this.reason = reason }

                    message.channel.createEmbed {
                        title = "Banned Successfully"
                        description = "${banner?.mention} banned ${banned.mention}"
                        thumbnail {
                            url = guild.getIconUrl(Image.Format.PNG) ?: ""
                        }
                        field("Reason", false) { reason }
                        color = Color(java.awt.Color.red.rgb)
                        footer {
                            icon = banned.avatar.url
                            text = "R.I.P ${banned.nickname ?: banned.username}"
                        }
                    }
                }
            }
        }

        command(::UnbanArgs) {
            name = "unban"
            description = "Unban a member"

            requirePermissions(Permission.BanMembers)
            check(hasPermission(Permission.BanMembers))
            action {
                val bans = guild?.bans?.toList()
                val member = arguments.member
                val author = message.author
                val reason = arguments.reason ?: "Because they're nice"
                if (bans != null) {
                    if (bans.any { it.user.id == member.id }) {
                        guild?.unban(member.id)
                        message.channel.createEmbed {
                            title = "Unbanned Successfully"
                            description = "${author?.mention} has unbanned ${member.mention}"
                            field("Reason", false) { reason }
                            thumbnail { url = guild?.getIconUrl(Image.Format.PNG) ?: "" }
                            author {
                                icon = author?.avatar?.url
                                name = "${author?.asMember(guild?.id!!)?.displayName} is a hero"
                            }
                            footer {
                                icon = member.avatar.url
                                text = "${member.username} is free!"
                            }
                            color = Color(57, 255, 20)
                        }
                    }
                } else {
                    message.reply {
                        allowedMentions()
                        content = "${member.mention} is not banned from this server"
                    }
                }
            }
        }
    }
}