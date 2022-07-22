package com.zerek.feathertps.commands;

import com.zerek.feathertps.FeatherTPS;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class TPSCommand implements CommandExecutor {

    private final FeatherTPS plugin;
    private final String tpsMessage,tpsAllMessage;

    public TPSCommand(FeatherTPS plugin) {
        this.plugin = plugin;
        this.tpsMessage = plugin.getConfig().getString("tps-message");
        this.tpsAllMessage = plugin.getConfig().getString("tps-all-message");
    }

    private String getTPS(int index){
        double tps = plugin.getServer().getTPS()[index];
        if (tps > 20.00) return "20.00";
        else return String.format("%.2f", tps);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player || sender instanceof ConsoleCommandSender){
            String TPSString;
            switch (args.length) {
                case 0:
                    TPSString = getTPS(0);
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(tpsMessage, Placeholder.unparsed("tps", TPSString)));
                    break;
                case 1:
                    if (args[0].equalsIgnoreCase("all")) {
                        TPSString = getTPS(0) + " - " + getTPS(1) + " - " + getTPS(2);
                        sender.sendMessage(MiniMessage.miniMessage().deserialize(tpsAllMessage, Placeholder.unparsed("tps", TPSString)));
                    } else sender.sendMessage(ChatColor.of("#E4453A") + "Invalid Command");
                    break;
                default:
                    sender.sendMessage(ChatColor.of("#E4453A") + "Invalid Command");
                    break;
            }
        }
        return true;
    }
}
