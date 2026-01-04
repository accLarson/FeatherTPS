package com.zerek.feathertps.tasks;

import com.zerek.feathertps.FeatherTPS;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.player.PlayerKickEvent;

import java.util.Map;

/**
 * Main TPS checking task that runs on the main thread
 * Handles TPS-based player kicking and triggers async entity filtering
 */
public class CheckTPSTask implements Runnable{

    private final MiniMessage mm = MiniMessage.miniMessage();
    private final FeatherTPS plugin;
    private final Map<String,Object> kickC;
    private final Map<String,Object> killC;
    private final Map<String,Object> portalC;

    public CheckTPSTask(FeatherTPS plugin, Map<String, Object> kickC, Map<String, Object> killC, Map<String, Object> portalC) {
        this.plugin = plugin;
        this.kickC = kickC;
        this.killC = killC;
        this.portalC = portalC;
    }

    /**
     * Formats TPS value to 2 decimal places and caps at 20.00
     * @param tps The raw TPS value
     * @return Formatted TPS string
     */
    private String formatTPS(double tps) {
        if (tps > 20.00) return "20.00";
        else return String.format("%.2f", tps);
    }

    @Override
    public void run() {
        double tps = plugin.getServer().getTPS()[0];
        String formattedTPS = formatTPS(tps);

        // Update portal spawn reduction flag based on TPS
        plugin.setReducePortalSpawns(tps < (double) portalC.get("tps-threshold"));

        // tps-kick
        if (tps <= (double) kickC.get("tps")) {
            Runnable kick = () -> {
                if (tps > (double) kickC.get("cancel-threshold")) plugin.getServer().broadcast(mm.deserialize((String) kickC.get("cancel-message")));
                else plugin.getServer().getOnlinePlayers().forEach(player -> player.kick(mm.deserialize((String) kickC.get("kick-message")), PlayerKickEvent.Cause.KICK_COMMAND));
            };
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, kick, 260L);
            plugin.getServer().broadcast(mm.deserialize((String) kickC.get("warn-message")));
        }

        // Trigger async entity filtering if TPS is low
        if (tps <= (double) killC.get("tps")){
            // Delegate the heavy entity filtering work to async task
            new EntityKillTask(plugin, killC).run();
        }
    }
}
