package com.zerek.feathertps.tasks;

import com.zerek.feathertps.FeatherTPS;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Async task that performs heavy entity filtering calculations off the main thread
 */
public class EntityKillTask implements Runnable {

    private final FeatherTPS plugin;
    private final Map<String, Object> killC;
    private final List<String> killableEntities;
    private final Random rand = new Random();

    public EntityKillTask(FeatherTPS plugin, Map<String, Object> killC) {
        this.plugin = plugin;
        this.killC = killC;
        this.killableEntities = (List<String>) killC.get("killable-entities");
    }

    @Override
    public void run() {
        // Step 1: Capture entity data on main thread (must be sync)
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            List<EntityData> entityDataList = captureEntityData();

            // Step 2: Do heavy filtering async
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                Set<UUID> entitiesToRemove = filterEntitiesAsync(entityDataList);

                // Step 3: Remove entities on main thread (must be sync)
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    removeEntitiesSync(entitiesToRemove);
                });
            });
        });
    }

    /**
     * Captures entity data from all worlds while on the main thread
     * This must be done sync because we're accessing Bukkit API
     */
    private List<EntityData> captureEntityData() {
        List<EntityData> entityDataList = new ArrayList<>();

        plugin.getServer().getWorlds().forEach(world -> {
            world.getEntities().forEach(entity -> {
                // Only capture data for killable entity types
                if (!killableEntities.contains(entity.getType().toString())) {
                    return;
                }

                // Skip entities with custom names
                if (entity.customName() != null) {
                    return;
                }

                // Skip if not a living entity
                if (!(entity instanceof LivingEntity)) {
                    return;
                }

                LivingEntity livingEntity = (LivingEntity) entity;

                // Skip leashed entities
                if (livingEntity.isLeashed()) {
                    return;
                }

                // Skip entities with equipment
                if (hasAnyEquipment(livingEntity)) {
                    return;
                }

                // Skip tempted entities
                if (plugin.getTempted().contains(livingEntity)) {
                    return;
                }

                // Capture nearby entity count (this is the expensive operation)
                int nearbyCount = (int) world.getEntities().stream()
                        .filter(e -> killableEntities.contains(e.getType().toString()))
                        .filter(e -> e.getLocation().distanceSquared(entity.getLocation()) <= Math.pow((Double) killC.get("range"), 2))
                        .count();

                // Only add if it meets density requirement
                if (nearbyCount >= (Integer) killC.get("dense-count")) {
                    entityDataList.add(new EntityData(
                            entity.getUniqueId(),
                            entity.getLocation().clone(),
                            nearbyCount
                    ));
                }
            });
        });

        return entityDataList;
    }

    /**
     * Filters entities asynchronously based on chance
     * This can be done async because we're just working with captured data
     */
    private Set<UUID> filterEntitiesAsync(List<EntityData> entityDataList) {
        Set<UUID> entitiesToRemove = ConcurrentHashMap.newKeySet();
        int chance = (Integer) killC.get("chance");

        for (EntityData data : entityDataList) {
            if (rand.nextInt(100) < chance) {
                entitiesToRemove.add(data.uuid);
            }
        }

        return entitiesToRemove;
    }

    /**
     * Removes entities by UUID on the main thread
     * This must be done sync because we're calling Bukkit API
     */
    private void removeEntitiesSync(Set<UUID> entitiesToRemove) {
        int removedCount = 0;
        int totalDense = entitiesToRemove.size();

        // Clear tempted entities now that we've processed them
        plugin.getTempted().clear();

        for (UUID uuid : entitiesToRemove) {
            Entity entity = plugin.getServer().getEntity(uuid);
            if (entity != null && entity.isValid()) {
                entity.remove();
                removedCount++;
            }
        }

        // Log and notify ops if any entities were removed
        if (totalDense > 0) {
            double tps = plugin.getServer().getTPS()[0];
            String formattedTPS = formatTPS(tps);

            plugin.getLogger().warning("TPS: " + formattedTPS + " \nDense Mobs Killed: " + removedCount + "/" + totalDense);

            // Notify ops (this is already on main thread, so it's safe)
            String message = (String) killC.get("message");
            for (org.bukkit.OfflinePlayer op : plugin.getServer().getOperators()) {
                if (op.isOnline() && op.getPlayer() != null) {
                    op.getPlayer().sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
                            message,
                            Placeholder.unparsed("roundedtps", formattedTPS),
                            Placeholder.unparsed("killedmobscount", String.valueOf(removedCount)),
                            Placeholder.unparsed("densemobstotal", String.valueOf(totalDense))
                    ));
                }
            }
        }
    }

    /**
     * Checks if an entity has any equipment (armor or items in hands)
     */
    private boolean hasAnyEquipment(LivingEntity entity) {
        EntityEquipment equipment = entity.getEquipment();
        if (equipment == null) return false;

        // Check armor slots
        if (equipment.getHelmet() != null && equipment.getHelmet().getType() != Material.AIR) return true;
        if (equipment.getChestplate() != null && equipment.getChestplate().getType() != Material.AIR) return true;
        if (equipment.getLeggings() != null && equipment.getLeggings().getType() != Material.AIR) return true;
        if (equipment.getBoots() != null && equipment.getBoots().getType() != Material.AIR) return true;

        // Check hand slots
        return equipment.getItemInMainHand().getType() != Material.AIR ||
                equipment.getItemInOffHand().getType() != Material.AIR;
    }

    /**
     * Formats TPS value to 2 decimal places and caps at 20.00
     */
    private String formatTPS(double tps) {
        if (tps > 20.00) return "20.00";
        else return String.format("%.2f", tps);
    }

    /**
     * Simple data class to hold entity information captured from the main thread
     */
    private static class EntityData {
        final UUID uuid;
        final Location location;
        final int nearbyCount;

        EntityData(UUID uuid, Location location, int nearbyCount) {
            this.uuid = uuid;
            this.location = location;
            this.nearbyCount = nearbyCount;
        }
    }
}
