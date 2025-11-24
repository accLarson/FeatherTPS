package com.zerek.feathertps.tasks;

import com.zerek.feathertps.FeatherTPS;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.inventory.EntityEquipment;

import java.util.*;
import java.util.stream.Collectors;

public class CheckTPSTask implements Runnable{

    private final MiniMessage mm = MiniMessage.miniMessage();
    private final FeatherTPS plugin;
    private final Map<String,Object> kickC;
    private final Map<String,Object> killC;
    private final Random rand = new Random();
    private final List<String> killableEntities;
    public CheckTPSTask(FeatherTPS plugin, Map<String, Object> kickC, Map<String, Object> killC) {
        this.plugin = plugin;
        this.kickC = kickC;
        this.killC = killC;
        this.killableEntities = (List<String>) killC.get("killable-entities");
    }

    @Override
    public void run() {
        double tps = plugin.getServer().getTPS()[0];

        // tps-kick
        if (tps <= (double) kickC.get("tps")) {
            Runnable kick = () -> {
                if (tps > 18.0) plugin.getServer().broadcast(mm.deserialize((String) kickC.get("cancel-message")));
                else plugin.getServer().getOnlinePlayers().forEach(player -> player.kick(mm.deserialize((String) kickC.get("kick-message")), PlayerKickEvent.Cause.KICK_COMMAND));
            };
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, kick, 260L);
            plugin.getServer().broadcast(mm.deserialize((String) kickC.get("warn-message")));
        }

        // Create list of filtered living entities that can be killed.
        if (tps <= (double) killC.get("tps")){
            Collection<Entity> killableMobs = plugin.getServer().getWorlds().stream()
                    .flatMap(world -> world.getEntities().stream())
                    .collect(Collectors.toList());
            
            // Filter by density
            killableMobs.removeIf(e -> e.getNearbyEntities((Double) killC.get("xz-range"), (Double) killC.get("y-range"), (Double) killC.get("xz-range")).size() < (Integer) killC.get("dense-count"));
            
            // Filter by entity type and custom name
            killableMobs.removeIf(e -> !killableEntities.contains(e.getType().toString()));
            killableMobs.removeIf(e -> e.customName() != null);
            
            // Filter living entities by leash and equipment
            killableMobs.removeIf(e -> e instanceof LivingEntity && ((LivingEntity) e).isLeashed());
            killableMobs.removeIf(e -> e instanceof LivingEntity && this.hasAnyEquipment((LivingEntity) e));

            // Remove tempted living entities
            killableMobs.removeAll(plugin.getTempted());
            plugin.getTempted().clear();

            // Kill dense mobs on a random change.
            int denseMobsTotal = killableMobs.size();
            int killedMobsCount = 0;
            for (Entity k : killableMobs) {
                if (rand.nextInt(100) < (Integer) killC.get("chance")) {
                    killedMobsCount++;
                    k.remove();
                }
            }

            //broadcast kills/removals to ops and logger
            if (denseMobsTotal > 0){

                plugin.getLogger().warning("TPS: " + tps + " \nDense Mobs Killed: " + killedMobsCount + "/" + denseMobsTotal);

                for (OfflinePlayer op : plugin.getServer().getOperators()) {
                    if (op.isOnline()) op.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize((String) killC.get("message"),
                            Placeholder.unparsed("roundedtps", String.valueOf(tps)),
                            Placeholder.unparsed("killedmobscount", String.valueOf(killedMobsCount)),
                            Placeholder.unparsed("densemobstotal", String.valueOf(denseMobsTotal))));
                }
            }
        }
    }

    /**
     * Checks if an entity has any equipment (armor or items in hands)
     * @param entity The entity to check
     * @return true if the entity has any equipment, false otherwise
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
        return equipment.getItemInMainHand().getType() != Material.AIR || equipment.getItemInOffHand().getType() != Material.AIR;
    }
}
