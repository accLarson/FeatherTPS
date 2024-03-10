package com.zerek.feathertps.commands;

import com.zerek.feathertps.FeatherTPS;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PingCommand implements CommandExecutor {

    private final FeatherTPS plugin;
    String pingMessage;


    public PingCommand(FeatherTPS plugin) {
        this.plugin = plugin;
        this.pingMessage = plugin.getConfig().getString("ping-message");

    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (strings.length == 0 && commandSender instanceof Player) {
            String ping = String.valueOf(((Player) commandSender).getPing());
            commandSender.sendMessage(MiniMessage.miniMessage().deserialize(pingMessage,
                    Placeholder.unparsed("ping", ping),
                    Placeholder.unparsed("player",commandSender.getName())));
            return true;
        }
        else if (strings.length == 1) {
            if (commandSender instanceof Player || commandSender instanceof ConsoleCommandSender) {

                Player player = Bukkit.getPlayer(strings[0]);
                if (player == null) {
                    commandSender.sendMessage(ChatColor.of("#E4453A") + "Player not online.");
                    return true;
                }

                String ping = String.valueOf(player.getPing());

                commandSender.sendMessage(MiniMessage.miniMessage().deserialize(pingMessage,
                        Placeholder.unparsed("ping", ping),
                        Placeholder.unparsed("player",player.getName())));
            }
            return true;
        }
        else {
            commandSender.sendMessage(ChatColor.of("#E4453A") + "Invalid Command.");
            return true;

        }
    }
}
