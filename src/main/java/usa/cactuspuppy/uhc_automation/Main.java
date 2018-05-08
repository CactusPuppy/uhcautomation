package usa.cactuspuppy.uhc_automation;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class Main extends JavaPlugin {
    protected GameInstance gi;
    private Connection connection;
    private String host, database, username, password;
    private int port;
    protected Statement statement;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();
        createConfig();
        BukkitRunnable r = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    initSQL();
                    statement = connection.createStatement();
                } catch (SQLException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        };
        r.runTaskAsynchronously(this);
        gi = new GameInstance(this);
        this.getCommand("uhcstart").setExecutor(new CommandStart(this));
        this.getCommand("uhcoptions").setExecutor(new CommandOptions(this));
        Bukkit.getServer().getPluginManager().registerEvents(new WorldChangeListener(), this);

        getLogger().info("UHC Automation loaded in " + ((double) (System.currentTimeMillis() - start)) + " ms");
    }

    @Override
    public void onDisable() {
        saveDefaultConfig();
    }

    /**
     * @source Innectic's Permissify plugin. Blatantly stolen code btw.
     */
    private void createConfig() {
        try {
            if (!getDataFolder().exists()) {
                boolean created = getDataFolder().mkdirs();
                if (!created) {
                    getLogger().log(Level.SEVERE, "Could not create config folder!");
                }
                File config = new File(getDataFolder(), "config.yml");
                if (!config.exists()) {
                    getLogger().info("config.yml not found, creating...");
                    saveDefaultConfig();
                } else {
                    getLogger().info("Loading config.yml...");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initSQL() throws SQLException, ClassNotFoundException {
        host = getConfig().getString("db.host");
        port = getConfig().getInt("db.port");
        database = getConfig().getString("db.database");
        username = getConfig().getString("db.username");
        password = getConfig().getString("db.password");
        if (connection != null && !connection.isClosed()) {
            return;
        }
        synchronized (this) {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + this.host+ ":" + this.port + "/" + this.database, this.username, this.password);
        }
    }
}