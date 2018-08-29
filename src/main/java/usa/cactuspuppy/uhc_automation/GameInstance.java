package usa.cactuspuppy.uhc_automation;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import usa.cactuspuppy.uhc_automation.Listeners.GameModeChangeListener;
import usa.cactuspuppy.uhc_automation.Listeners.PlayerMoveListener;
import usa.cactuspuppy.uhc_automation.Tasks.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class GameInstance {
    @Getter private Main main;
    @Getter @Setter private long startT;
    @Getter @Setter private boolean teamMode;
    @Getter @Setter private boolean active;
    @Getter @Setter private World world;
    @Getter private Set<UUID> livePlayers;
    @Getter private Set<UUID> activePlayers;
    @Getter @Setter private Set<UUID> regPlayers;
    @Getter @Setter private Set<UUID> blacklistPlayers;
    @Getter @Setter private int minsToShrink;
    @Getter @Setter private long secsToPVP;
    @Getter private int initSize;
    @Getter @Setter private int finalSize;
    @Getter @Setter private int spreadDistance;
    @Getter @Setter private int epLength;
    @Getter @Setter private boolean respectTeams;
    private boolean uhcMode;
    @Getter private Scoreboard scoreboard;
    @Setter private int borderCountdown;
    @Getter private boolean borderShrinking;
    @Getter private PlayerMoveListener freezePlayers;
    @Getter @Setter private InfoAnnouncer infoAnnouncer;
    @Getter @Setter private Set<Player> giveBoats;
    private int loadChunksCDID;
    private int teamsRemaining;

    public static final boolean DEBUG = true;

    public GameInstance(Main p) {
        main = p;
        startT = 0;
        teamsRemaining = 0;
        world = UHCUtils.getWorldFromString(main, Bukkit.getServer(), p.getConfig().getString("world"));
        minsToShrink = p.getConfig().getInt("game.mins-to-shrink");
        initSize = p.getConfig().getInt("game.init-size");
        finalSize = p.getConfig().getInt("game.final-size");
        teamMode = p.getConfig().getBoolean("game.team-mode");
        respectTeams = p.getConfig().getBoolean("game.respect-teams");
        spreadDistance = p.getConfig().getInt("game.spread-distance");
        uhcMode = p.getConfig().getBoolean("game.uhc-mode");
        epLength = p.getConfig().getInt("game.episode-length");
        secsToPVP = p.getConfig().getLong("game.secs-to-pvp");
        livePlayers = new HashSet<>();
        activePlayers = new HashSet<>();
        regPlayers = new HashSet<>();
        blacklistPlayers = new HashSet<>();
        borderShrinking = false;
        active = false;
        (new DelayReactivate(this)).schedule();
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        if (scoreboard.getObjective("Health") == null) {
            scoreboard.registerNewObjective("Health", "health", ChatColor.RED + "Health").setDisplaySlot(DisplaySlot.PLAYER_LIST);
        } else if (!scoreboard.getObjective("Health").getCriteria().equals("health")) {
            scoreboard.getObjective("Health").unregister();
            scoreboard.registerNewObjective("Health", "health", ChatColor.RED + "Health").setDisplaySlot(DisplaySlot.PLAYER_LIST);
        }
    }

    public void prep() {
        if (UHCUtils.isWorldData(main)) {
            UHCUtils.clearWorldData(main);
        }
        //Wall Pair 1
        for (int x = -10; x <= 10; x++) {
            for (int y = 253; y <= 255; y++) {
                world.getBlockAt(x, y, -10).setType(Material.BARRIER);
                world.getBlockAt(x, y, 10).setType(Material.BARRIER);
            }
        }
        //Wall Pair 2
        for (int z = -10; z <= 10; z++) {
            for (int y = 253; y <= 255; y++) {
                world.getBlockAt(-10, y, z).setType(Material.BARRIER);
                world.getBlockAt(10, y, z).setType(Material.BARRIER);
            }
        }
        //Floor
        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                world.getBlockAt(x, 253, z).setType(Material.BARRIER);
            }
        }
        world.setSpawnLocation(0, 254, 0);
        Location spawn = new Location(world, 0, 254, 0);
        new GameModeChangeListener();
        for (Player p : activePlayers.stream().map(Bukkit::getPlayer).collect(Collectors.toList())) {
            p.teleport(spawn);
            p.setGameMode(GameMode.SURVIVAL);
        }
        world.setGameRule(GameRule.SPAWN_RADIUS, 0);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setTime(0L);
        world.setStorm(false);
        world.setPVP(false);
        world.getWorldBorder().setCenter(0D, 0D);
        world.getWorldBorder().setSize(initSize);
        world.setGameRule(GameRule.NATURAL_REGENERATION, !uhcMode);
    }

    public void start(CommandSender s) {
        if (teamMode) {
            List<UUID> copyLive = new ArrayList<>(livePlayers);
            copyLive.stream().map(Bukkit::getPlayer).forEach(this::spectateNotTeamPlayer);
        }
        if (!checkNumPlayers() && !DEBUG) {
            main.getLogger().warning("Not enough players are in the UHC!");
            s.sendMessage(ChatColor.RED + "UHC aborted! Not enough players in the UHC!");
            UHCUtils.broadcastMessage(this, ChatColor.RED.toString() + ChatColor.BOLD + "Could not start UHC.");
            prep();
            return;
        }
        long initT = System.currentTimeMillis();
        UHCUtils.broadcastMessage(this, ChatColor.GREEN + "Game starting!");
        HandlerList.unregisterAll(GameModeChangeListener.getInstance());
        livePlayers.stream().map(Bukkit::getPlayer).forEach(this::prepPlayer);
        for (int x = -10; x <= 10; x++) {
            for (int y = 253; y <= 255; y++) {
                for (int z = -10; z <= 10; z++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR);
                }
            }
        }
        boolean spread = UHCUtils.spreadplayers(this);
        if (!spread) {
            s.sendMessage(ChatColor.RED + "Unable to spread this many players within specified gamespace! Consider decreasing the spread distance between players or increasing the initial size of the border with /uhcoptions. UHC aborted.");
            UHCUtils.broadcastMessage(this, ChatColor.RED.toString() + ChatColor.BOLD + "Could not start UHC.");
            prep();
            return;
        }
        UHCUtils.saveWorldPlayers(main);
        if (teamMode) {
            teamsRemaining = getNumTeams();
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M-dd HH:mm:ss");
        main.getLogger().info("Game Initiate Time - " + sdf.format(new Date(initT)));
        active = true;
        freezePlayers = new PlayerMoveListener();
        Bukkit.getServer().getPluginManager().registerEvents(freezePlayers, main);
        loadChunksCDID = (new LoadingChunksCountdown(main, 5)).schedule();
    }

    private void spectateNotTeamPlayer(Player p) {
        if (scoreboard.getEntryTeam(p.getName()) != null && !blacklistPlayers.contains(p.getUniqueId())) return;
        p.setGameMode(GameMode.SPECTATOR);
    }

    private boolean checkNumPlayers() {
        if (teamMode) {
            return getNumTeams() >= 2;
        }
        return livePlayers.size() >= 2;
    }

    private void prepPlayer(Player p) {
        p.getInventory().clear();
        p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "effect give " + p.getName() + " minecraft:resistance 10 10 true");
        p.setGameMode(GameMode.ADVENTURE);
    }

    public void release() {
        Bukkit.getScheduler().cancelTask(loadChunksCDID);
        startT = System.currentTimeMillis();
        UHCUtils.saveAuxData(main);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M-dd HH:mm:ss");
        main.getLogger().info("Game Start Time - " + sdf.format(new Date(startT)));
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, true);
        world.setStorm(false);
        world.setWeatherDuration((new Random()).nextInt(48000) + 24000);
        if (minsToShrink > 0) {
            borderCountdown = (new BorderCountdown(minsToShrink * 60, startT, Main.getInstance().getConfig().getBoolean("warnings.border", true))).schedule();
        } else if (minsToShrink == 0) {
            borderShrinking = true;
            world.getWorldBorder().setSize(finalSize, calcBorderShrinkTime());
            main.getLogger().info("Game border shrinking from " + initSize + " to " + finalSize
                    + " over " + calcBorderShrinkTime() + " secs");
            (new BorderAnnouncer()).schedule();
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            world.setTime(6000L);
        }
        if (epLength != 0) {
            (new EpisodeAnnouncer(epLength, startT)).schedule();
        }
        if (secsToPVP == 0) {
            world.setPVP(true);
        } else if (secsToPVP > 0) {
            (new PVPEnableCountdown(secsToPVP, startT, Main.getInstance().getConfig().getBoolean("warnings.pvp", true))).schedule();
        }
        HandlerList.unregisterAll(freezePlayers);
        livePlayers.forEach(this::startPlayer);
        giveBoats = null;
        infoAnnouncer.schedule();
        infoAnnouncer.showBoard();
    }

    public void stop() {
        (new RestartTasks()).schedule();
        infoAnnouncer.clearBoard();
        long stopT = System.currentTimeMillis();
        long timeElapsed;
        if (startT == 0) {
            timeElapsed = 0;
        } else {
            timeElapsed = stopT - startT;
        }
        timeElapsed /= 1000;
        startT = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M-dd HH:mm:ss");
        main.getLogger().info("Game Stop Time - " + sdf.format(new Date(stopT)));
        main.getLogger().info("Time Elapsed: " + UHCUtils.secsToFormatString((int) timeElapsed));
        active = false;
        borderShrinking = false;
        UHCUtils.clearWorldData(main);
        blacklistPlayers.clear();
    }

    public void startBorderShrink() {
        borderShrinking = true;
        world.getWorldBorder().setSize(finalSize, calcBorderShrinkTime());
        main.getLogger().info("Game border shrinking from " + initSize + " to " + finalSize
                + " over " + calcBorderShrinkTime() + " secs");
        Bukkit.getScheduler().cancelTask(borderCountdown);
        for (UUID u : activePlayers) {
            alertPlayerBorder(u);
        }
        (new BorderAnnouncer()).schedule();
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setTime(6000L);
        world.setStorm(false);
    }

//    @SuppressWarnings("deprecation")
    public void win() {
        int timeElapsed = UHCUtils.getSecsElapsed(main);
        if (teamMode) {
            if (getNumTeams() > 1) {
                logStatus(Bukkit.getConsoleSender());
                main.getLogger().severe("Win condition called for game instance running on world " + world.getName() + " under invalid circumstances. Game status dumped to log.");
                return;
            }
            if (livePlayers.size() == 0) {
                UHCUtils.broadcastMessagewithTitle(this, ChatColor.RED + "\nWait... what? The game ended in a tie!",
                        ChatColor.DARK_RED.toString() + ChatColor.BOLD + "DRAW", ChatColor.RESET + "Game ended in a tie!", 0, 80, 40);
            } else {
                Team t = livePlayers.stream().findFirst().map(u -> Bukkit.getScoreboardManager().getMainScoreboard().getTeam(Bukkit.getPlayer(u).getName())).orElse(null);
                if (t == null) {
                    Main.getInstance().getLogger().severe("Could not determine winning team in " + Main.getInstance().getConfig().getString("event-name"));
                    return;
                }
                List<String> onlineWinners = new ArrayList<>();
                for (OfflinePlayer p : t.getPlayers()) {
                    if (p.isOnline()) {
                        onlineWinners.add(((Player) p).getDisplayName());
                    }
                }
                StringBuilder winningTeamPlayers = new StringBuilder();
                collectNames(onlineWinners, winningTeamPlayers);
                String winners = winningTeamPlayers.toString();
                UHCUtils.broadcastMessagewithTitle(this, "\n" + t.getName() + ChatColor.GREEN + " has emerged victorious!\nMembers: " + ChatColor.RESET + winners,
                        t.getName(), ChatColor.GREEN + "wins!", 0 , 80, 40);
                UHCUtils.broadcastMessage(this, ChatColor.AQUA + "\nTime Elapsed: " + ChatColor.RESET + WordUtils.capitalize(UHCUtils.secsToFormatString(timeElapsed)));
            }
        } else {
            if (livePlayers.size() == 1) {
                Player winner = livePlayers.stream().findFirst().map(Bukkit::getPlayer).orElse(null);
                assert winner != null;
                UHCUtils.broadcastMessagewithTitle(this, "\n" + ChatColor.GREEN + winner.getDisplayName() + ChatColor.WHITE + " wins!\n"
                        + ChatColor.AQUA + "\nTime Elapsed: " + ChatColor.RESET + WordUtils.capitalize(UHCUtils.secsToFormatString((int) timeElapsed)),
                        winner.getDisplayName(), "Wins!", 0, 80, 40);
            } else if (livePlayers.size() == 0) {
                UHCUtils.broadcastMessagewithTitle(this, ChatColor.RED + "\nWait... what? The game ended in a tie!", ChatColor.DARK_RED.toString() + ChatColor.BOLD + "DRAW", ChatColor.YELLOW + "Game ended in a tie!", 0, 80, 40);
            } else {
                logStatus(Bukkit.getConsoleSender());
                main.getLogger().severe("Win condition called early for game instance running on world " + world.getName() + ". Game status dumped to log.");
                return;
            }
        }
        stop();
    }

    private void collectNames(List<String> names, StringBuilder winningTeamPlayers) {
        if (names.size() == 1) {
            winningTeamPlayers.append(names.get(0));
        } else if (names.size() == 2) {
            winningTeamPlayers.append(names.get(0)).append(" and ").append(names.get(1));
        } else {
            winningTeamPlayers.append(names.get(0));
            for (int i = 1; i < names.size(); i++) {
                if (i == names.size() - 1) {
                    winningTeamPlayers.append(", and ").append(names.get(i));
                } else {
                    winningTeamPlayers.append(", ").append(names.get(i));
                }
            }
        }
    }

    /**
     *  Helper/Access methods
     */
    public void checkForWin() {
        if (teamMode) {
            int numTeams = getNumTeams();
            if (numTeams <= 1) {
                win();
            } else if (numTeams < teamsRemaining) {
                UHCUtils.broadcastMessage(this, ChatColor.DARK_RED.toString() + ChatColor.BOLD + "\nA team has been eliminated! " + ChatColor.RESET + "\n" + numTeams + " teams remain!");
                teamsRemaining = numTeams;
            }
        } else {
            if (livePlayers.size() <= 1) {
                win();
            }
        }
    }

    public void startPlayer(UUID u) {
        Player p = Bukkit.getPlayer(u);
        p.sendTitle(ChatColor.GREEN.toString() + ChatColor.BOLD + "GO!", UHCUtils.randomStartMSG(), 0, 80, 40);
        p.playSound(p.getLocation(), "minecraft:block.note_block.pling", 1F, 1.18F);
        p.playSound(p.getLocation(), "minecraft:entity.ender_dragon.growl", 1F, 1F);
        p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
        p.setFoodLevel(20);
        p.setSaturation(5);
        p.setHealth(20);
        p.setGameMode(GameMode.SURVIVAL);
        if (giveBoats.contains(p)) {
            p.getInventory().addItem(new ItemStack(Material.OAK_BOAT, 1));
            p.sendMessage(ChatColor.GREEN + "Since you spawned in an ocean biome, you have received a boat to reach land faster.");
        }
    }

    private void alertPlayerBorder(UUID u) {
        Player p = Bukkit.getPlayer(u);
        if (p == null) {
            return;
        }
        p.sendMessage(ChatColor.DARK_RED.toString() + ChatColor.BOLD + "\nBorder shrinking!");
        p.sendTitle(ChatColor.DARK_RED.toString() + ChatColor.BOLD + "Border Shrinking!", "", 0, 80, 40);
        p.playSound(p.getLocation(), "minecraft:entity.ender_dragon.death", 1F, 1F);
    }

    public boolean validate(CommandSender s) {
        boolean valid;
        valid = initSize > finalSize;
        if (!valid) {
            s.sendMessage(String.format(ChatColor.RED + "Initial size %d is not greater than final size %d. UHC aborted.", initSize, finalSize));
        }
        valid = valid && spreadDistance < initSize;
        if (!valid) {
            s.sendMessage(String.format(ChatColor.RED + "Player separation distance %d is not less than spread range %d. UHC aborted.", spreadDistance, initSize / 2));
        }
        return valid;
    }

    public void logStatus(CommandSender s) {
        String worldName;
        if (world != null) {
            worldName = world.getName();
        } else {
            worldName = "NOT BOUND";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("\n").append(ChatColor.GOLD).append(ChatColor.BOLD).append("Current Status:");
        if (DEBUG) { builder.append("\n").append(ChatColor.RED).append(ChatColor.BOLD).append("DEBUG MODE ACTIVE"); }
        builder.append("\n").append(ChatColor.AQUA).append("World: ").append(ChatColor.RESET).append(worldName);
        builder.append("\n").append(ChatColor.YELLOW).append("Event Name: ").append(ChatColor.RESET).append(main.getConfig().getString("event-name"));
        builder.append("\n").append(ChatColor.AQUA).append("Active: ").append((active ? ChatColor.GREEN : ChatColor.RED)).append(active);
        builder.append("\n").append(ChatColor.YELLOW).append("Team Mode: ").append((teamMode ? ChatColor.GREEN : ChatColor.RED)).append(teamMode);
        if (active) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M-dd HH:mm:ss");
            builder.append("\n\n").append(ChatColor.AQUA).append("Game started ").append(sdf.format(startT));
            builder.append("\n").append(ChatColor.GREEN).append("Time Elapsed: ").append(UHCUtils.secsToFormatString(UHCUtils.getSecsElapsed(main)));
            if (teamMode) {
                builder.append("\n").append(ChatColor.AQUA).append("Teams Remaining: ").append((teamsRemaining <= 3 ? ChatColor.RED : ChatColor.GREEN)).append(teamsRemaining);
            } else {
                builder.append("\n").append(ChatColor.AQUA).append("Players Remaining: ").append((livePlayers.size() <= 3 ? ChatColor.RED : ChatColor.GREEN)).append(livePlayers.size());
            }
            builder.append("\n").append(ChatColor.GREEN).append("Border Shrinking: ").append((borderShrinking ? ChatColor.GREEN : ChatColor.RED)).append(borderShrinking);
            builder.append("\n").append(ChatColor.AQUA).append("PVP Enabled: ").append((world.getPVP() ? ChatColor.GREEN : ChatColor.RED)).append(world.getPVP());
        }
        builder.append("\n").append(ChatColor.AQUA).append(ChatColor.UNDERLINE).append("Registered Players:").append(ChatColor.RESET);
        if (regPlayers.isEmpty()) {
            builder.append("\n  ").append(ChatColor.RED).append("NONE");
        } else {
            for (UUID u : regPlayers) {
                String name;
                String online;
                Player p = Bukkit.getPlayer(u);
                if (p == null) {
                    name = u.toString();
                    online = "Offline";
                } else {
                    name = p.getName();
                    online = "Online";
                }
                builder.append("\n  ").append(ChatColor.RESET).append(name).append(" - ").append(online);
            }
        }
        builder.append("\n").append(ChatColor.AQUA).append(ChatColor.UNDERLINE).append("Alive Players:").append(ChatColor.RESET);
        if (livePlayers.isEmpty()) {
            builder.append("\n  ").append(ChatColor.RED).append("NONE");
        } else {
            for (UUID u : livePlayers) {
                builder.append("\n  ").append(ChatColor.RESET).append(Bukkit.getPlayer(u).getName());
            }
        }
        builder.append("\n").append(ChatColor.AQUA).append(ChatColor.UNDERLINE).append("Spectators/Dead/Blacklisted Players:").append(ChatColor.RESET);
        if (blacklistPlayers.isEmpty()) {
            builder.append("\n  ").append(ChatColor.RED).append("NONE");
        } else {
            for (UUID u : blacklistPlayers) {
                Player p = Bukkit.getPlayer(u);
                if (p != null) {
                    builder.append("\n  ").append(ChatColor.RESET).append(p.getName());
                } else {
                    builder.append("\n  ").append(ChatColor.RESET).append(u.toString());
                }
            }
        }
        builder.append("\n").append(ChatColor.AQUA).append("Initial Size: ").append(ChatColor.RESET).append(initSize);
        builder.append("\n").append(ChatColor.GREEN).append("Final Size: ").append(ChatColor.RESET).append(finalSize);
        builder.append("\n").append(ChatColor.AQUA).append("Border Delay Length: ").append(ChatColor.RESET).append(minsToShrink).append(" mins");
        builder.append("\n").append(ChatColor.GREEN).append("Episode Marker Interval: ").append(ChatColor.RESET).append(epLength).append(" mins");
        builder.append("\n").append(ChatColor.AQUA).append("PVP Delay Length: ").append(ChatColor.RESET).append(secsToPVP).append(" secs");
        builder.append("\n\n");
        s.sendMessage(builder.toString());
    }

    @SuppressWarnings("deprecation")
    public int getNumTeams() {
        Set<Team> teams = new HashSet<>();
        for (UUID u : livePlayers) {
            Player p = Bukkit.getPlayer(u);
            //DEBUG
            System.out.println(p.getName() + "'s team is " + scoreboard.getEntryTeam(p.getName()).getName());
            teams.add(scoreboard.getEntryTeam(p.getName()));
        }
        return teams.size();
    }

    public void recalcPlayerSet() {
        Set<UUID> aPCopy = new HashSet<>(activePlayers);
        for (UUID u : aPCopy) {
            if (Bukkit.getOfflinePlayer(u) == null || !Bukkit.getOfflinePlayer(u).isOnline()) {
                activePlayers.remove(u);
                livePlayers.remove(u);
            }
        }
    }

    private int calcBorderShrinkTime() {
        double slowFactor = 1.5;
        return (int) ((initSize - finalSize) * slowFactor);
    }

    public void bindPlayertoScoreboard(Player p) {
        p.setScoreboard(scoreboard);
    }

    public boolean isStarted() {
        return startT != 0;
    }

    public void registerPlayer(Player p) {
        registerPlayerSilent(p);
        if (p.getGameMode() == GameMode.SURVIVAL) {
            UHCUtils.announcePlayerJoin(p);
        } else {
            UHCUtils.announcePlayerSpectate(p);
        }
    }

    public void registerPlayerSilent(Player p) {
        regPlayers.add(p.getUniqueId());
        activePlayers.add(p.getUniqueId());
        if (p.getGameMode() == GameMode.SURVIVAL) {
            livePlayers.add(p.getUniqueId());
            if (teamMode && active) {
                teamsRemaining = getNumTeams();
            }
        }
        if (InfoModeCache.getInstance().getPlayerPref(p.getUniqueId()) == null) {
            InfoModeCache.getInstance().storePlayerPref(p.getUniqueId(), InfoDisplayMode.CHAT);
        }
    }

    public void addPlayerToLive(Player p) {
        livePlayers.add(p.getUniqueId());
        UHCUtils.announcePlayerJoin(p);
    }

    public void removePlayerFromLive(Player p) {
        livePlayers.remove(p.getUniqueId());
        blacklistPlayers.add(p.getUniqueId());
        UHCUtils.announcePlayerSpectate(p);
    }

    public void lostConnectPlayer(OfflinePlayer p) {
        activePlayers.remove(p.getUniqueId());
        livePlayers.remove(p.getUniqueId());
    }

    public void blacklistPlayer(UUID u) {
        blacklistPlayers.add(u);
        regPlayers.remove(u);
        activePlayers.remove(u);
        livePlayers.remove(u);
    }

    public void setInitSize(int s) {
        initSize = s;
        world.getWorldBorder().setSize(initSize);
    }

    public void setUHCMode(boolean um) {
        uhcMode = um;
        world.setGameRuleValue("naturalRegeneration", String.valueOf(!um));
    }
}
