package usa.cactuspuppy.uhc_automation.event.game.update;

import org.bukkit.event.HandlerList;
import usa.cactuspuppy.uhc_automation.event.game.GameEvent;
import usa.cactuspuppy.uhc_automation.game.GameInstance;

public class GamePauseEvent extends GameEvent {
    public GamePauseEvent(GameInstance gameInstance) {
        super(gameInstance);
    }

    private static final HandlerList handlers = new HandlerList();

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
