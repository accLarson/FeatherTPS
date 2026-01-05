package com.zerek.feathertps.tasks;

import com.zerek.feathertps.FeatherTPS;
import io.papermc.paper.ServerBuildInfo;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.inventory.EntityEquipment;

import java.util.*;

/**
 * Main TPS checking task that runs on the main thread
 * Handles TPS-based player kicking and triggers entity filtering synchronously
 */
public class CheckTPSTask implements Runnable{

    private final MiniMessage mm = MiniMessage.miniMessage();
    private final FeatherTPS plugin;
    private final Map<String,Object> kickC;
    private final Map<String,Object> killC;
    private final Map<String,Object> portalC;
    private final Random rand = new Random();
    private boolean kickScheduled = false;

    public CheckTPSTask(FeatherTPS plugin, Map<String, Object> kickC, Map<String, Object> killC, Map<String, Object> portalC) {
        this.plugin = plugin;
        this.kickC = kickC;
        this.killC = killC;
        this.portalC = portalC;
    }

    private String formatTPS(double tps) {
        if (tps > 20.00) return "20.00";
        else return String.format("%.2f", tps);
    }

    @Override
    public void run() {
        double tps = plugin.getServer().getTPS()[0];

        plugin.setReducePortalSpawns(tps < (double) portalC.get("tps-threshold"));

        handleKickSystem(tps);

        cleanupTemptedEntities();

        if (tps <= (double) killC.get("tps")) killDenseEntities();

    }

    /**
     * Handles the TPS-based kick system
     * Schedules kicks when TPS drops, cancels if TPS recovers, executes if TPS stays low
     * 
     * @param tps Current server TPS
     */
    private void handleKickSystem(double tps) {
        if (kickScheduled) {
            if (tps >= (double) kickC.get("cancel-threshold")) {
                plugin.getServer().broadcast(mm.deserialize((String) kickC.get("cancel-message")));
                kickScheduled = false;
            } else if (tps <= (double) kickC.get("tps")) {
                String transferHost = (String) kickC.get("transfer-host");
                int transferPort = (int) kickC.get("transfer-port");
                
                plugin.getServer().getOnlinePlayers().forEach(player -> {
                    try {
                        player.transfer(transferHost, transferPort);
                    } catch (Exception e) {
                        // Fallback to kick if the transfer fails
                        player.kick(mm.deserialize((String) kickC.get("kick-message")), PlayerKickEvent.Cause.KICK_COMMAND);
                    }
                });
                kickScheduled = false;
            }
        } else if (tps <= (double) kickC.get("tps")) {
            kickScheduled = true;
            plugin.getServer().broadcast(mm.deserialize((String) kickC.get("warn-message")));
        }
    }

    /**
     * Removes invalid entities from the tempted set
     * Called every cycle to prevent memory leaks
     */
    private void cleanupTemptedEntities() {
        Set<LivingEntity> tempted = plugin.getTempted();
        tempted.removeIf(entity -> 
            entity == null || 
            !entity.isValid() || 
            entity.isDead()
        );
    }

    private void killDenseEntities() {
        Set<String> killableEntities = new HashSet<>((List<String>) killC.get("killable-entities"));
        Set<UUID> toRemove = new HashSet<>();
        int eligibleCount = 0;

        for (World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!killableEntities.contains(entity.getType().toString())) continue;
                if (entity.customName() != null) continue;
                if (!(entity instanceof LivingEntity)) continue;

                LivingEntity livingEntity = (LivingEntity) entity;
                if (livingEntity.isLeashed()) continue;
                if (plugin.getTempted().contains(livingEntity)) continue;

                EntityEquipment eq = livingEntity.getEquipment();
                if (eq != null && (
                    (eq.getHelmet() != null && eq.getHelmet().getType() != Material.AIR) ||
                    (eq.getChestplate() != null && eq.getChestplate().getType() != Material.AIR) ||
                    (eq.getLeggings() != null && eq.getLeggings().getType() != Material.AIR) ||
                    (eq.getBoots() != null && eq.getBoots().getType() != Material.AIR) ||
                    eq.getItemInMainHand().getType() != Material.AIR || eq.getItemInOffHand().getType() != Material.AIR)) continue;

                double range = (double) killC.get("range");
                int nearby = (int) entity.getLocation().getNearbyEntities(range, range, range).stream()
                        .filter(e -> killableEntities.contains(e.getType().toString()))
                        .count() + 1;

                if (nearby >= (int) killC.get("dense-count")) {
                    eligibleCount++;
                    if (rand.nextInt(100) < (int) killC.get("chance")) {
                        toRemove.add(entity.getUniqueId());
                    }
                }
            }
        }

        int removedCount = 0;
        for (UUID uuid : toRemove) {
            Entity entity = plugin.getServer().getEntity(uuid);
            if (entity != null && entity.isValid()) {
                entity.remove();
                removedCount++;
            }
        }

        if (removedCount > 0) {
            String tpsStr = formatTPS(plugin.getServer().getTPS()[0]);
            plugin.getLogger().warning("TPS: " + tpsStr + " \nDense Mobs Killed: " + removedCount + "/" + eligibleCount);
            
            for (OfflinePlayer op : plugin.getServer().getOperators()) {
                if (op.isOnline() && op.getPlayer() != null)
                    op.getPlayer().sendMessage(mm.deserialize(
                            (String) killC.get("message"),
                            Placeholder.unparsed("roundedtps", tpsStr),
                            Placeholder.unparsed("killedmobscount", String.valueOf(removedCount)),
                            Placeholder.unparsed("densemobstotal", String.valueOf(eligibleCount))
                    ));
            }
        }
    }
}
