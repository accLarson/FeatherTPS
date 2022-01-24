package com.zerek.feathertps.listeners;

import com.zerek.feathertps.FeatherTPS;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;

import java.util.Collection;

public class EntityTargetListener implements Listener {

    private final FeatherTPS plugin;
    public EntityTargetListener(FeatherTPS plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityTrack(EntityTargetEvent event) {
        if (event.getReason().toString().equals("TEMPT")) plugin.getTempted().add((LivingEntity) event.getEntity());
    }
}
