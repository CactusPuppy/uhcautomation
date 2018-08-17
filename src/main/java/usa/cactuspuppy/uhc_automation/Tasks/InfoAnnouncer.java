package usa.cactuspuppy.uhc_automation.Tasks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import usa.cactuspuppy.uhc_automation.InfoDisplayMode;
import usa.cactuspuppy.uhc_automation.InfoModeCache;
import usa.cactuspuppy.uhc_automation.Main;
import usa.cactuspuppy.uhc_automation.UHCUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class InfoAnnouncer implements Runnable {
    private Set<UUID> objectivePlayerSet = new HashSet<>();
    private Scoreboard timeScoreboard;
    private Objective obj;
    private Team InfoDisplay;

    private static final String TIME_TEAM_ID = ChatColor.BLACK.toString() + ChatColor.WHITE.toString();

    public InfoAnnouncer() {
        timeScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        if (timeScoreboard.getObjective("InfoDisplay") == null) {
            timeScoreboard.registerNewObjective("InfoDisplay", "dummy", "Game Info");
        } else if (!timeScoreboard.getObjective("InfoDisplay").getCriteria().equals("dummy")) {
            timeScoreboard.getObjective("InfoDisplay").unregister();
            timeScoreboard.registerNewObjective("InfoDisplay", "dummy", "Game Info");
        }
        if (timeScoreboard.getObjective("Health2") == null) {
            timeScoreboard.registerNewObjective("Health2", "health", "Health").setDisplaySlot(DisplaySlot.PLAYER_LIST);
        } else if (!timeScoreboard.getObjective("Health2").getCriteria().equals("health")) {
            timeScoreboard.getObjective("Health2").unregister();
            timeScoreboard.registerNewObjective("Health2", "health", "Health").setDisplaySlot(DisplaySlot.PLAYER_LIST);
        }
        obj = timeScoreboard.getObjective("InfoDisplay");
        obj.setDisplayName(ChatColor.GOLD + Main.getInstance().getConfig().getString("event-name", "Game Info"));
        InfoDisplay = timeScoreboard.registerNewTeam("Time");
        InfoDisplay.addEntry(TIME_TEAM_ID);
        obj.getScore(ChatColor.GREEN + "» Time Elapsed:").setScore(15);
        obj.getScore(TIME_TEAM_ID).setScore(14);
    }

    public void removePlayerFromObjectiveSet(Player p) {
        Main.getInstance().getGameInstance().bindPlayertoScoreboard(p);
        objectivePlayerSet.remove(p.getUniqueId());
    }

    @Override
    public void run() {
        InfoDisplay.setPrefix(ChatColor.WHITE + UHCUtils.secsToFormatString2(UHCUtils.getSecsElapsed(Main.getInstance())));
        Main.getInstance().getGameInstance().getActivePlayers().stream().map(Bukkit::getPlayer).forEach(this::showTimetoPlayer);
    }

    private void showTimetoPlayer(Player player) {
        InfoDisplayMode tdm = InfoModeCache.getInstance().getPlayerPref(player.getUniqueId());
        if (tdm == null) {
            Bukkit.getLogger().warning(player.getName() + " possess an invalid InfoDisplayMode. Setting to default (CHAT)...");
            InfoModeCache.getInstance().storePlayerPref(player.getUniqueId(), InfoDisplayMode.CHAT);
        } else if (tdm == InfoDisplayMode.SCOREBOARD) {
            if (!objectivePlayerSet.contains(player.getUniqueId())) {
                player.setScoreboard(timeScoreboard);
                objectivePlayerSet.add(player.getUniqueId());
            }
        }
    }

    public void schedule() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.getInstance(), this, 0L, 2L);
    }

    public void showBoard() {
        if (timeScoreboard.getObjective("FILL") != null) {
            timeScoreboard.getObjective("FILL").unregister();
        }
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    public void clearBoard() {
        if (timeScoreboard.getObjective("FILL") != null) {
            timeScoreboard.getObjective("FILL").unregister();
        }
        timeScoreboard.registerNewObjective("FILL", "dummy").setDisplaySlot(DisplaySlot.SIDEBAR);
    }
}
