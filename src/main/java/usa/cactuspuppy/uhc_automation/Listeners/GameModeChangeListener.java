package usa.cactuspuppy.uhc_automation.Listeners;

import lombok.Getter;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import usa.cactuspuppy.uhc_automation.Main;

public class GameModeChangeListener implements Listener {
    @Getter private static GameModeChangeListener instance;

    public GameModeChangeListener() { instance = this; }

    @EventHandler
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent e) {
        Player p = e.getPlayer();
        if (Main.getInstance().getGameInstance().getActivePlayers().contains(p.getUniqueId())) {
            if (Main.getInstance().getGameInstance().getLivePlayers().contains(p.getUniqueId()) && e.getNewGameMode() != GameMode.SURVIVAL) {
                Main.getInstance().getGameInstance().removePlayerFromLive(p);
            } else if (!Main.getInstance().getGameInstance().getLivePlayers().contains(p.getUniqueId()) && e.getNewGameMode() == GameMode.SURVIVAL) {
                Main.getInstance().getGameInstance().addPlayerToLive(p);
            }
        }
    }
}