package com.zerek.feathertps.commands;

import com.zerek.feathertps.FeatherTPS;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class TPSCommand implements CommandExecutor {

    private final FeatherTPS plugin;
    private final String tpsMessage;

    public TPSCommand(FeatherTPS plugin) {
        this.plugin = plugin;
        this.tpsMessage = plugin.getConfig().getString("tps-message");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player || sender instanceof ConsoleCommandSender){
            sender.sendMessage(MiniMessage.miniMessage().deserialize(tpsMessage, Placeholder.unparsed("tps", Arrays.toString(plugin.getServer().getTPS()))));
        }
        return true;
    }
}
