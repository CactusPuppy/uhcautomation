package usa.cactuspuppy.uhc_automation.Listeners;

import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import usa.cactuspuppy.uhc_automation.Main;
import usa.cactuspuppy.uhc_automation.UHCUtils;

@NoArgsConstructor
public class PlayerConnectionListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (UHCUtils.worldEqualsExt(p.getWorld(), Main.getInstance().getGameInstance().getWorld())) {
            Main.getInstance().getGameInstance().bindPlayertoScoreboard(p);
            if (Main.getInstance().getGameInstance().getBlacklistPlayers().contains(p.getUniqueId())) {
                p.sendMessage(ChatColor.YELLOW.toString() + ChatColor.BOLD + "You have joined the game as a spectator. Contact an administrator if you think you should be in the game.");
                p.setGameMode(GameMode.SPECTATOR);
                Main.getInstance().getGameInstance().registerPlayer(p);
                return;
            }
            if (Main.getInstance().getGameInstance().isActive()) {
                if (Main.getInstance().getGameInstance().getRegPlayers().contains(p.getUniqueId())) {
                    Main.getInstance().getGameInstance().registerPlayerSilent(p);
                } else {
                    p.sendMessage(ChatColor.YELLOW.toString() + ChatColor.BOLD + "You have joined the game as a spectator. Contact an administrator if you think you should be in the game.");
                    p.setGameMode(GameMode.SPECTATOR);
                    Main.getInstance().getGameInstance().registerPlayer(p);
                }
            } else if (!Main.getInstance().getGameInstance().getActivePlayers().contains(p.getUniqueId())) {
                p.sendTitle(ChatColor.GOLD + "Welcome", "to the " + Main.getInstance().getConfig().getString("event-name"), 20, 60, 20);
                p.setHealth(19);
                p.setHealth(20);
                Main.getInstance().getGameInstance().registerPlayer(p);
            }
        } else {
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        if (Main.getInstance().getGameInstance().getActivePlayers().contains(e.getPlayer().getUniqueId())) {
            Main.getInstance().getGameInstance().lostConnectPlayer(e.getPlayer());
        }
    }
}
