package com.zerek.feathertps.listeners;

import com.zerek.feathertps.FeatherTPS;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Map;
import java.util.Random;

/**
 * Listener that reduces zombified piglin spawns from nether portals when TPS is low
 */
public class CreatureSpawnListener implements Listener {

    private final FeatherTPS plugin;
    private final Map<String, Object> config;
    private final Random random = new Random();

    public CreatureSpawnListener(FeatherTPS plugin, Map<String, Object> config) {
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Quick boolean check - very cheap operation
        if (!plugin.shouldReducePortalSpawns()) {
            return;
        }

        // Check if it's a zombified piglin
        if (event.getEntityType() != EntityType.ZOMBIFIED_PIGLIN) {
            return;
        }

        // Check if it spawned from a nether portal
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NETHER_PORTAL) {
            return;
        }

        // Get the configured spawn rate percentage (default 5%)
        int spawnRate = (int) config.get("spawn-rate");

        // Generate random number 0-99, if it's >= spawn rate, cancel the spawn
        // This means if spawn-rate is 5, only values 0-4 will allow spawn (5% chance)
        if (random.nextInt(100) >= spawnRate) {
            event.setCancelled(true);
        }
    }
}
