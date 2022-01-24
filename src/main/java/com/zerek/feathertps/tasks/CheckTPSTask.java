package com.zerek.feathertps.tasks;

import com.zerek.feathertps.FeatherTPS;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerKickEvent;

import java.util.*;
import java.util.stream.Collectors;

public class CheckTPSTask implements Runnable{

    private final MiniMessage mm = MiniMessage.get();
    private final FeatherTPS plugin;
    private final Map<String,Object> kickC;
    private final Map<String,Object> killC;
    private final Random rand = new Random();
    private List<String> killableEntities = new ArrayList<>();
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
                if (tps > 18.0) plugin.getServer().broadcast(mm.parse((String) kickC.get("cancel-message")));
                else plugin.getServer().getOnlinePlayers().forEach(player -> player.kick(mm.parse((String) kickC.get("kick-message")), PlayerKickEvent.Cause.KICK_COMMAND));
            };
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, kick, 260L);
            plugin.getServer().broadcast(mm.parse((String) kickC.get("warn-message")));
        }

        // kill-dense-mobs
        if (tps <= (double) killC.get("tps")){
            System.out.println(tps + " < " + killC.get("tps"));
            Collection<LivingEntity> killable = plugin.getServer().getWorlds().stream().flatMap(world -> world.getLivingEntities().stream()).collect(Collectors.toList());
            killable.removeIf(e -> e.getNearbyEntities((Double) killC.get("xz-range"), (Double) killC.get("y-range"), (Double) killC.get("xz-range")).size() < (Integer) killC.get("dense-count"));
            killable.removeIf(e -> !killableEntities.contains(e.getType().toString()) || (e.getCustomName() != null) || e.isLeashed());
            killable.removeIf(e -> e.getCustomName() != null);
            killable.removeIf(LivingEntity::isLeashed);
            killable.removeIf(e ->(e.getEquipment().getHelmet().getType() != Material.AIR));
            killable.removeIf(e ->(e.getEquipment().getChestplate().getType() != Material.AIR));
            killable.removeIf(e ->(e.getEquipment().getLeggings().getType() != Material.AIR));
            killable.removeIf(e ->(e.getEquipment().getBoots().getType() != Material.AIR));
            killable.removeIf(e ->(e.getEquipment().getItemInMainHand().getType() != Material.AIR));
            killable.removeIf(e ->(e.getEquipment().getItemInOffHand().getType() != Material.AIR));
            killable.removeAll(plugin.getTempted());
            plugin.getTempted().clear();
            int killableTotal = killable.size();
            int killCount = 0;
            for (LivingEntity k : killable) {
                if (rand.nextInt(100) < (Integer) killC.get("chance")) {
                    killCount++;
                    k.remove();
                }
            }
            if (killableTotal > 0){
                double roundedTPS = Math.round(plugin.getServer().getTPS()[0] * 100)/100.0;
                plugin.getLogger().warning("TPS: " + roundedTPS + " | Dense Mobs Killed: " + killCount + "/" + killableTotal);
                for (OfflinePlayer op : plugin.getServer().getOperators()) {
                    if (op.isOnline()) op.getPlayer().sendMessage(MiniMessage.get().parse((String) killC.get("message"), "roundedTPS", String.valueOf(roundedTPS), "killCount", String.valueOf(killCount), "killableTotal", String.valueOf(killableTotal)));
                }
            }
        }
    }
}
