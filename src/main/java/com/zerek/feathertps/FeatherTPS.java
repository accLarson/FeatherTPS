package com.zerek.feathertps;

import com.zerek.feathertps.commands.PingCommand;
import com.zerek.feathertps.commands.PingCommandTabCompleter;
import com.zerek.feathertps.commands.TPSCommand;
import com.zerek.feathertps.commands.TPSCommandTabCompleter;
import com.zerek.feathertps.listeners.EntityTargetListener;
import com.zerek.feathertps.tasks.CheckTPSTask;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class FeatherTPS extends JavaPlugin {

    private final Collection<LivingEntity> tempted = new ArrayList<>();
    Map<String,Object> killDenseConfig = new HashMap<>();
    Map<String,Object> tpsKickConfig = new HashMap<>();
    @Override
    public void onEnable() {
        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(new EntityTargetListener(this),this);

        ConfigurationSection configurationSection = getConfig().getConfigurationSection("kill-dense");
        configurationSection.getKeys(false).forEach(key -> killDenseConfig.put(key, configurationSection.get(key)));

        ConfigurationSection configurationSection2 = getConfig().getConfigurationSection("tps-kick");
        configurationSection2.getKeys(false).forEach(key -> tpsKickConfig.put(key,configurationSection2.get(key)));

        getServer().getScheduler().scheduleSyncRepeatingTask(this, new CheckTPSTask(this, tpsKickConfig, killDenseConfig), 0L, 200L);

        this.getCommand("tps").setExecutor(new TPSCommand(this));
        this.getCommand("tps").setTabCompleter(new TPSCommandTabCompleter());
        this.getCommand("ping").setExecutor(new PingCommand(this));
        this.getCommand("ping").setTabCompleter(new PingCommandTabCompleter());
    }
    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
    public Collection<LivingEntity> getTempted() {
        return tempted;
    }
}