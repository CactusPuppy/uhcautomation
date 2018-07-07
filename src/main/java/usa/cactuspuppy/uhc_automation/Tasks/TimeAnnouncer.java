package usa.cactuspuppy.uhc_automation.Tasks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import usa.cactuspuppy.uhc_automation.Main;
import usa.cactuspuppy.uhc_automation.TimeDisplayMode;
import usa.cactuspuppy.uhc_automation.TimeModeCache;
import usa.cactuspuppy.uhc_automation.UHCUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TimeAnnouncer implements Runnable {
    private Main m;
    private boolean overrride;
    private Set<UUID> objectivePlayerSet = new HashSet<>();
    private Scoreboard timeScoreboard;
    private Objective obj;
    private Team timeDisplay;

    private static final String TIME_TEAM_ID = ChatColor.BLACK.toString() + ChatColor.WHITE.toString();

    public TimeAnnouncer(Main main) {
        m = main;
        overrride = false;
        timeScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        if (timeScoreboard.getObjective("TimeDisplay") == null) {
            timeScoreboard.registerNewObjective("TimeDisplay", "dummy");
        } else if (!timeScoreboard.getObjective("TimeDisplay").getCriteria().equals("dummy")) {
            timeScoreboard.getObjective("TimeDisplay").unregister();
            timeScoreboard.registerNewObjective("TimeDisplay", "dummy");
        }
        if (timeScoreboard.getObjective("Health2") == null) {
            timeScoreboard.registerNewObjective("Health2", "health").setDisplaySlot(DisplaySlot.PLAYER_LIST);
        } else if (!timeScoreboard.getObjective("Health2").getCriteria().equals("health")) {
            timeScoreboard.getObjective("Health2").unregister();
            timeScoreboard.registerNewObjective("Health2", "health").setDisplaySlot(DisplaySlot.PLAYER_LIST);
        }
        obj = timeScoreboard.getObjective("TimeDisplay");
        obj.setDisplayName(ChatColor.GOLD + m.getConfig().getString("event-name"));
        timeDisplay = timeScoreboard.registerNewTeam("Time");
        timeDisplay.addEntry(TIME_TEAM_ID);
        obj.getScore(ChatColor.GREEN + "» Time Elapsed:").setScore(15);
        obj.getScore(TIME_TEAM_ID).setScore(14);
    }

    public void removePlayerfromObjectiveSet(Player p) {
        m.gi.bindPlayertoScoreboard(p);
        objectivePlayerSet.remove(p.getUniqueId());
    }

    @Override
    public void run() {
        m.gi.getActivePlayers().stream().map(Bukkit::getPlayer).forEach(this::showTimetoPlayer);
    }

    private void showTimetoPlayer(Player player) {
        TimeDisplayMode tdm = TimeModeCache.getInstance().getPlayerPref(player.getUniqueId());
        if (tdm == null) {
            Bukkit.getLogger().warning(player.getName() + " possess an invalid TimeDisplayMode. Setting to default (CHAT)...");
            TimeModeCache.getInstance().storePlayerPref(player.getUniqueId(), TimeDisplayMode.CHAT);
        } else if (tdm == TimeDisplayMode.SCOREBOARD) {
            if (!objectivePlayerSet.contains(player.getUniqueId())) {
                player.setScoreboard(timeScoreboard);
                objectivePlayerSet.add(player.getUniqueId());
            }
            timeDisplay.setPrefix(ChatColor.WHITE + UHCUtils.secsToFormatString2(UHCUtils.getSecsElapsed(m)));
        } else if (tdm == TimeDisplayMode.SUBTITLE) {
            if (overrride) {
                return;
            }
            player.sendTitle("", "                        " + UHCUtils.secsToFormatString2(UHCUtils.getSecsElapsed(m)), 0, 20, 0);
        }
    }

    public void schedule() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(m, this, 0L, 2L);
    }

    public void setOverride(boolean b) {
        overrride = b;
    }

    public void showBoard() {
        if (timeScoreboard.getObjective("FILL") != null) {
            timeScoreboard.getObjective("FILL").unregister();
        }
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    public void clearBoard() {
        timeScoreboard.registerNewObjective("FILL", "dummy").setDisplaySlot(DisplaySlot.SIDEBAR);
    }
}
