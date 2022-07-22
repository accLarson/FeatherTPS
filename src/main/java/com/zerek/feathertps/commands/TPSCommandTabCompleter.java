package com.zerek.feathertps.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TPSCommandTabCompleter implements TabCompleter {
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> options = new ArrayList<>();
        options.add("all");
//        switch (args.length) {
//            case 0:
//                return options;
//            case 1:
//                List<String> match = new ArrayList<String>();
//                for (String topic : options)
//                    if (topic.toLowerCase().startsWith(args[0].toLowerCase())) match.add(topic);
//                return match;
//            default:
//                options.clear();
//                return options;
//        }
        return options;
    }
}
