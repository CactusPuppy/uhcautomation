package usa.cactuspuppy.uhc_automation.event.game.group;

import lombok.Getter;
import org.bukkit.event.HandlerList;
import usa.cactuspuppy.uhc_automation.entity.unique.Group;
import usa.cactuspuppy.uhc_automation.entity.unique.Team;
import usa.cactuspuppy.uhc_automation.game.GameInstance;

public class GroupSetTeamEvent extends GroupEvent {
    @Getter private Team team;

    public GroupSetTeamEvent(GameInstance gameInstance, Group group, Team team) {
        super(gameInstance, group);
        this.team = team;
    }

    private static final HandlerList handlers = new HandlerList();

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
