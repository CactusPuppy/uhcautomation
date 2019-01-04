package usa.cactuspuppy.uhc_automation.event.events.player;

import org.bukkit.entity.Player;
import usa.cactuspuppy.uhc_automation.GameInstance;

import java.util.UUID;

public class PlayerJoinEvent extends PlayerEvent {

    public PlayerJoinEvent(GameInstance gameInstance, UUID u) {
        super(gameInstance, u);
    }

    public PlayerJoinEvent(GameInstance gameInstance, Player p) {
        super(gameInstance, p);
    }
}
