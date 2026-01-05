package com.zerek.feathertps.listeners;

import com.zerek.feathertps.FeatherTPS;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;

import java.util.Collection;

public class EntityTargetListener implements Listener {

    private final FeatherTPS plugin;
    public EntityTargetListener(FeatherTPS plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityTrack(EntityTargetEvent event) {
        // Only process if the entity is actually a LivingEntity
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        
        LivingEntity entity = (LivingEntity) event.getEntity();
        
        if (event.getReason().toString().equals("TEMPT")) {
            plugin.getTempted().add(entity);
        } else {
            // Entity changed target to something other than tempt, remove it
            plugin.getTempted().remove(entity);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Remove dead entities from tempted set immediately
        plugin.getTempted().remove(event.getEntity());
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        // Remove unloaded entities from tempted set
        for (org.bukkit.entity.Entity entity : event.getEntities()) {
            if (entity instanceof LivingEntity) {
                plugin.getTempted().remove((LivingEntity) entity);
            }
        }
    }
}
