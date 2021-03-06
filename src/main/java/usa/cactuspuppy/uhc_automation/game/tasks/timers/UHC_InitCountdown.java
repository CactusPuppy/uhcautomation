package usa.cactuspuppy.uhc_automation.game.tasks.timers;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import usa.cactuspuppy.uhc_automation.command.commands.Start;
import usa.cactuspuppy.uhc_automation.game.GameInstance;
import usa.cactuspuppy.uhc_automation.game.GameStateEvent;
import usa.cactuspuppy.uhc_automation.utils.Logger;

public class UHC_InitCountdown extends TimerTask {
    private long initTime;
    private long lastSecs;

    private GameInstance instance;

    public UHC_InitCountdown(GameInstance gameInstance, int secsDelay) {
        super(gameInstance, true, 0L, 2L);
        instance = gameInstance;
        long currTime = System.currentTimeMillis();
        initTime = currTime + secsDelay * 1000;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        //TODO: init countdown
        long currTime = System.currentTimeMillis();
        instance.getUtils().log(Logger.Level.FINE, this.getClass(), "Running init countdown");
        if (currTime >= initTime) {
            Start.startComplete(getGameInstance());
            getGameInstance().updateState(GameStateEvent.INIT);
            cancel();
            return;
        }
        long timeTo = initTime - currTime;
        long secs = timeTo / 1000 + (timeTo % 1000 == 0 ? 0 : 1);
        if (secs != lastSecs) {
            getGameInstance().getUtils().broadcastSoundTitle(Sound.BLOCK_NOTE_BLOCK_PLING, 1.17F, Long.toString(secs), ChatColor.GOLD + "Initiating match in...", 0, 20, 10);
        } else {
            getGameInstance().getUtils().broadcastTitle(Long.toString(secs), ChatColor.GOLD + "Initiating match in...", 0, 20, 10);
        }
        lastSecs = secs;
    }
}
