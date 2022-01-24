package com.zerek.feathertps;

import com.zerek.feathertps.listeners.EntityTargetListener;
import com.zerek.feathertps.tasks.CheckTPSTask;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class FeatherTPS extends JavaPlugin {

    private final Collection<LivingEntity> tempted = new ArrayList<LivingEntity>();
    Map<String,Object> killDenseMobConfig = new HashMap<>();
    Map<String,Object> tpsKickConfig = new HashMap<>();
    @Override
    public void onEnable() {
        saveDefaultConfig();

        getServer().getPluginManager().registerEvents(new EntityTargetListener(this),this);

        ConfigurationSection configurationSection = getConfig().getConfigurationSection("kill-dense-mobs");
        configurationSection.getKeys(false).forEach(key -> killDenseMobConfig.put(key, configurationSection.get(key)));

        ConfigurationSection configurationSection2 = getConfig().getConfigurationSection("tps-kick");
        configurationSection2.getKeys(false).forEach(key -> tpsKickConfig.put(key,configurationSection2.get(key)));

        getServer().getScheduler().scheduleSyncRepeatingTask(this, new CheckTPSTask(this, tpsKickConfig, killDenseMobConfig), 0L, 200L);
    }
    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
    public Collection<LivingEntity> getTempted() {
        return tempted;
    }
}