package usa.cactuspuppy.uhc_automation.Tasks;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import usa.cactuspuppy.uhc_automation.Main;

import java.util.logging.Level;

public class GenerateChunksHelper implements Runnable {
    @Getter private static GenerateChunksHelper instance;
    private long startTime;
    private int minChunkX, maxChunkX, minChunkZ, maxChunkZ;
    private int chunkX, chunkZ;
    private World world;

    @Getter private int schedulerID;

    public GenerateChunksHelper() {
        startTime = System.currentTimeMillis();
        world = Main.getInstance().getGameInstance().getWorld();
        int radius = blockCoordtoChunkCoord(Main.getInstance().getGameInstance().getInitSize() / 2) + 1;
        this.minChunkX = -radius;
        this.maxChunkX = radius;
        this.minChunkZ = -radius;
        this.maxChunkZ = radius;
        chunkX = minChunkX;
        chunkZ = minChunkZ;
        instance = this;
        Main.getInstance().getLogger().info("Chunk pre-generation initiated. Lag may occur during this time...");
        Bukkit.broadcastMessage("[" + ChatColor.GOLD + "UHC" + ChatColor.RESET + "] " + ChatColor.DARK_RED + ChatColor.BOLD + "Chunk pre-generation beginning. Severe lag may occur.");
    }

    @Override
    public void run() {
        if (chunkX == maxChunkX && chunkZ == maxChunkZ) {
            generateChunk(world.getChunkAt(chunkX, chunkZ));
            Main.getInstance().getLogger().info("Generated chunk at chunk coords X: " + chunkX + ", Z: " + chunkZ);
            long timeElapsed = System.currentTimeMillis() - startTime;
            Main.getInstance().getLogger().info(ChatColor.GREEN + "Chunk pre-generation complete! Took " + timeElapsed / 1000 + " seconds (" + timeElapsed + " ms)");
            Bukkit.getScheduler().cancelTask(schedulerID);
            instance = null;
            return;
        }
        generateChunk(world.getChunkAt(chunkX, chunkZ));
        Main.getInstance().getLogger().log(Level.FINE, "Generated chunk at chunk coords X: " + chunkX + ", Z: " + chunkZ);
        if (chunkZ == maxChunkZ) {
            chunkX++;
            chunkZ = minChunkZ;
        } else {
            chunkZ++;
        }
    }

    private static void generateChunk(Chunk chunk) {
        if (chunk.isLoaded()) { return; }
        chunk.load(true);
        chunk.unload(true);
    }

    private static int blockCoordtoChunkCoord(int i) {
        return (i - Math.floorMod(i, 16)) / 16;
    }

    public void schedule() {
        schedulerID = Bukkit.getScheduler().scheduleSyncRepeatingTask(Main.getInstance(), this, 0L, 2L);
    }
}
