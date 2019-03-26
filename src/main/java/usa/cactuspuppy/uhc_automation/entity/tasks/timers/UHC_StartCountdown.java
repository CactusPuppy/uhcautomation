package usa.cactuspuppy.uhc_automation.entity.tasks.timers;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import usa.cactuspuppy.uhc_automation.game.GameInstance;
import usa.cactuspuppy.uhc_automation.utils.Logger;

import java.util.Objects;
import java.util.UUID;

public class UHC_StartCountdown extends TimerTask {
    /**
     * Whether or not this timer has started counting down
     */
    private boolean countdown;
    /**
     * At last run, the time of the server tick
     */
    private long lastTick;
    /**
     * Seconds to wait from landing and tick stabilization to
     */
    private int secsToCountdown;
    /**
     * Calculated time at which game should start
     */
    private long startTime = -1;

    @Getter @Setter
    private int ticksPerCycle = 1;
    @Getter @Setter
    private double maxTickDeviance = 0.1;

    /**
     * Full constructor for start-game countdown
     * @param gameInstance game to countdown for
     * @param delay Seconds to countdown after all players land and ticks stabilize
     * @param ticksPerCycle Number of ticks between each run of this task.
     *                      Higher values increase precision, but may introduce more load onto the server
     * @param maxTickDeviance Maximum allowable variation from 20 ticks per second as a proportion of tick time.
     *                        Higher values may be needed on servers that do not achieve 20 TPS, but this may result in
     *                        the countdown starting before teleport lag has cleared.
     */
    public UHC_StartCountdown(GameInstance gameInstance, int delay, int ticksPerCycle, double maxTickDeviance) {
        super(gameInstance, true, 0L, ticksPerCycle);
        countdown = false;
        gameInstance.getUtils().log(Logger.Level.INFO, this.getClass(), "Initiating final game start countdown...");
        this.maxTickDeviance = maxTickDeviance;
        this.ticksPerCycle = ticksPerCycle;
        lastTick = System.currentTimeMillis();
        secsToCountdown = delay;
    }

    /**
     * Overloaded constructor
     */
    public UHC_StartCountdown(GameInstance gameInstance, int delay) {
        super(gameInstance, true, 0L, 1L);
        countdown = false;
        gameInstance.getUtils().log(Logger.Level.INFO, this.getClass(), "Initiating final game start countdown...");
        lastTick = System.currentTimeMillis();
        secsToCountdown = delay;
    }

    @Override
    public void run() {
        if (!countdown) { //Wait for ticks to stabilize + players to land
            long currTick = System.currentTimeMillis();
            //Check current tick delay
            if (Math.abs(currTick - lastTick - ticksPerCycle * 50) > maxTickDeviance * ticksPerCycle * 50) {
                gameInstance.getUtils().log(Logger.Level.FINE, this.getClass(),
                        String.format("Current run: %d | Last run: %d (%d ticks ago) | Avg Tick Time: %d (need %f)",
                                currTick, lastTick, ticksPerCycle, (currTick - lastTick) / ticksPerCycle, maxTickDeviance * 50));
                lastTick = currTick;
                gameInstance.getUtils().broadcastTitle(ChatColor.GOLD + "Initiating Match", "Loading chunks...", 0, 20, 10);
            } else {
                //Check that all players are on the ground
                boolean allOnGround = gameInstance.getAlivePlayers().stream().map(Bukkit::getPlayer).filter(Objects::nonNull).allMatch(Player::isOnGround);
                if (allOnGround) {
                    countdown = true;
                } else {
                    gameInstance.getUtils().broadcastTitle(ChatColor.GOLD + "Initiating Match", "Waiting for all players to land...", 0, 20, 10);
                }
            }
        }
        if (!countdown) {
            return;
        }
        if (startTime == -1) {//Set time to start
            startTime = System.currentTimeMillis() + secsToCountdown * 1000;
        }
    }
}
