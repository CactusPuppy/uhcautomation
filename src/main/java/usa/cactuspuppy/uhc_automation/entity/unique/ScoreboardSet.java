package usa.cactuspuppy.uhc_automation.entity.unique;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Scoreboard;
import usa.cactuspuppy.uhc_automation.game.GameInstance;
import usa.cactuspuppy.uhc_automation.utils.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents the set of scoreboards (one per player) for a GameInstance
 */
public class ScoreboardSet {
    @Getter private GameInstance parent;
    private Map<UUID, Scoreboard> scoreboards = new HashMap<>();

    public ScoreboardSet(GameInstance parent) {
        this.parent = parent;
    }

    public Scoreboard addPlayer(UUID playerUid) {
        Scoreboard exists =  getPlayerScoreboard(playerUid);
        if (exists != null) {
            return exists;
        }
        Scoreboard newScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        if (newScoreboard == null) {
            Logger.logError(this.getClass(), "", new RuntimeException("Failed to get new scoreboard from Bukkit"));
            return null;
        }
        scoreboards.put(playerUid, newScoreboard);
        return newScoreboard;
    }

    public Scoreboard getPlayerScoreboard(UUID playerUid) {
        if (!scoreboards.containsKey(playerUid)) {
            return addPlayer(playerUid);
        }
        return scoreboards.get(playerUid);
    }
}
