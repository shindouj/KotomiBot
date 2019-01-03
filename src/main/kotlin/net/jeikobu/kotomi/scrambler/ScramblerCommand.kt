package net.jeikobu.kotomi.scrambler

import net.jeikobu.jbase.command.AbstractCommand
import net.jeikobu.jbase.command.Command
import net.jeikobu.jbase.command.CommandData
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IRole
import sx.blah.discord.handle.obj.Permissions
import java.util.*

@Command(name = "scrambleRoles", argsLength = 1, permissions = [Permissions.ADMINISTRATOR])
class ScramblerCommand(data: CommandData?) : AbstractCommand(data) {
    override fun run(message: IMessage?) {
        when (args[0]) {
            "enable" -> setEnabled(true)
            "disable" -> setEnabled(false)
            "addRole" -> addRoles()
            "setMode" -> setMode()
            "setInterval" -> setInterval()
        }
    }

    fun setEnabled(enabled: Boolean) {
        val operationName = if (enabled) getLocalized("enabled") else getLocalized("disabled")

        if (enabled && !getInterval().isPresent) {
            destinationChannel.sendMessage(getLocalized("intervalFirst"))
            return
        } else if (getEnabled() == enabled) {
            destinationChannel.sendMessage(getLocalized("alreadySwitched", operationName))
        } else {
            guildConfig.setValue(ScramblerKeys.SCRAMBLER_ENABLED.configKey, enabled.toString())
            destinationChannel.sendMessage(getLocalized("enableSwitchSuccessful", operationName))
        }
    }

    private fun getEnabled(): Boolean {
        return guildConfig.getValue(ScramblerKeys.SCRAMBLER_ENABLED.configKey, "false", Boolean::class.java).get()
    }

    private fun getInterval(): Optional<ScramblerInterval> {
        val intervalOptional = guildConfig.getValue(ScramblerKeys.SCRAMBLER_INTERVAL.configKey, String::class.java)
        return if (intervalOptional.isPresent) {
            Optional.of(ScramblerInterval.fromString(intervalOptional.get()))
        } else {
            Optional.empty()
        }
    }

    private fun setInterval() {
        try {
            val intervalType = ScramblerIntervalType.fromName(args[1])
            val data: String = if (args.size > 2) args[2] else ""

            guildConfig.setValue(ScramblerKeys.SCRAMBLER_INTERVAL.configKey, ScramblerInterval(intervalType, data).toString())
            destinationChannel.sendMessage(getLocalized("intervalSuccess", args[1]))
        } catch (e: IllegalArgumentException) {
            destinationChannel.sendMessage(getLocalized("wrongIntervalType", args[1]))
        }
    }

    private fun addRoles() {
        var omittedRoles = ""
        var rolesList: List<IRole> = args.subList(1, args.size).mapNotNull { x ->
            try {
                destinationGuild.getRoleByID(x.toLong())
            } catch (e: Exception) {
                try {
                    destinationGuild.getRolesByName(x).first()
                } catch (e: Exception) {
                    omittedRoles += "$x, "
                    null
                }
            }
        }

        if (rolesList.isEmpty()) {
            destinationChannel.sendMessage(getLocalized("noRolesFound"))
            return
        } else if (omittedRoles.isNotEmpty()) {
            destinationChannel.sendMessage(getLocalized("omittedRoles", omittedRoles.substringBeforeLast(", ")))
        }

        rolesList += getRoles()
        setRoles(rolesList)
    }

    private fun getRoles(): List<IRole> {
        val roleList = guildConfig.getValue(ScramblerKeys.SCRAMBLER_ROLES.configKey, String::class.java)

        return if (roleList.isPresent) {
            roleList.get().split(", ").mapNotNull { x ->
                try {
                    destinationGuild.getRoleByID(x.toLong())
                } catch (e: Exception) {
                    destinationChannel.sendMessage(getLocalized("roleDeleted", x))
                    null
                }
            }
        } else {
            emptyList()
        }
    }

    private fun setRoles(rolesList: List<IRole>) {
        val roleStringList: String = rolesList.joinToString { x -> "$x.stringID, " }
        guildConfig.setValue(ScramblerKeys.SCRAMBLER_ROLES.configKey, roleStringList)
    }

    private fun setMode() {
        try {
            if (args.size > 1) {
                val mode = ScramblerMode.fromName(args[1].toLowerCase())
                guildConfig.setValue(ScramblerKeys.SCRAMBLER_MODE.configKey, mode.modeName)
                destinationChannel.sendMessage(getLocalized("modeSetSuccessfully", mode.modeName))
            } else {
                destinationChannel.sendMessage(getLocalized("noSuchMode", "empty"))
            }
        } catch (e: IllegalArgumentException) {
            destinationChannel.sendMessage(getLocalized("noSuchMode", args[1]))
        }
    }

    override fun usageMessage(): String {
        return getLocalized("usage", configManager.getCommandPrefix(destinationGuild))
    }
}