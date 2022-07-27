package com.zerek.feathertps.tasks;

import com.zerek.feathertps.FeatherTPS;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
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
            Collection<LivingEntity> killableMobs = plugin.getServer().getWorlds().stream().flatMap(world -> world.getLivingEntities().stream()).collect(Collectors.toList());
            killableMobs.removeIf(e -> e.getNearbyEntities((Double) killC.get("xz-range"), (Double) killC.get("y-range"), (Double) killC.get("xz-range")).size() < (Integer) killC.get("dense-count"));
            killableMobs.removeIf(e -> !killableEntities.contains(e.getType().toString()) || (e.customName() != null) || e.isLeashed());
            killableMobs.removeIf(e -> e.customName() != null);
            killableMobs.removeIf(LivingEntity::isLeashed);
            killableMobs.removeIf(e ->(e.getEquipment().getHelmet().getType() != Material.AIR));
            killableMobs.removeIf(e ->(e.getEquipment().getChestplate().getType() != Material.AIR));
            killableMobs.removeIf(e ->(e.getEquipment().getLeggings().getType() != Material.AIR));
            killableMobs.removeIf(e ->(e.getEquipment().getBoots().getType() != Material.AIR));
            killableMobs.removeIf(e ->(e.getEquipment().getItemInMainHand().getType() != Material.AIR));
            killableMobs.removeIf(e ->(e.getEquipment().getItemInOffHand().getType() != Material.AIR));
            killableMobs.removeAll(plugin.getTempted());
            plugin.getTempted().clear();

            // Kill dense mobs on a random change.
            int denseMobsTotal = killableMobs.size();
            int killedMobsCount = 0;
            for (LivingEntity k : killableMobs) {
                if (rand.nextInt(100) < (Integer) killC.get("chance")) {
                    killedMobsCount++;
                    k.remove();
                }
            }

            // Create list of filtered armor stands that can be removed.
            Collection<ArmorStand> removableStands = plugin.getServer().getWorlds().stream().flatMap(world -> world.getEntitiesByClass(ArmorStand.class).stream()).collect(Collectors.toList());
            removableStands.removeIf(s -> s.getNearbyEntities((Double) killC.get("xz-range"), (Double) killC.get("y-range"), (Double) killC.get("xz-range")).size() < (Integer) killC.get("dense-count"));
            removableStands.removeIf(s ->(s.getEquipment().getHelmet().getType() != Material.AIR));
            removableStands.removeIf(s ->(s.getEquipment().getChestplate().getType() != Material.AIR));
            removableStands.removeIf(s ->(s.getEquipment().getLeggings().getType() != Material.AIR));
            removableStands.removeIf(s ->(s.getEquipment().getBoots().getType() != Material.AIR));
            int denseStandTotal = removableStands.size();
            int removedStandCount = 0;

            // Remove dense armor stands on a random change.
            for (ArmorStand s : removableStands) {
                if (rand.nextInt(100) < (Integer) killC.get("chance")) {
                    removedStandCount++;
                    s.remove();
                }
            }

            //broadcast kills/removals to ops and logger
            if (denseMobsTotal > 0 || denseStandTotal > 0){

                double roundedTPS = Math.round(plugin.getServer().getTPS()[0] * 100)/100.0;

                plugin.getLogger().warning("TPS: " + roundedTPS + " \nDense Mobs Killed: " + killedMobsCount + "/" + denseMobsTotal + " \nDense Armor Stands Removed: " + removedStandCount + "/" + denseStandTotal);

                for (OfflinePlayer op : plugin.getServer().getOperators()) {
                    if (op.isOnline()) op.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize((String) killC.get("message"),
                            Placeholder.unparsed("roundedtps", String.valueOf(roundedTPS)),
                            Placeholder.unparsed("killedmobscount", String.valueOf(killedMobsCount)),
                            Placeholder.unparsed("densemobstotal", String.valueOf(denseMobsTotal)),
                            Placeholder.unparsed("removedstandcount", String.valueOf(removedStandCount)),
                            Placeholder.unparsed("densestandtotal", String.valueOf(denseStandTotal))));
                }
            }
        }
    }
}
