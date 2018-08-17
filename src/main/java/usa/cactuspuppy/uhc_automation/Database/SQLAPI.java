package usa.cactuspuppy.uhc_automation.Database;

import org.bukkit.Bukkit;
import usa.cactuspuppy.uhc_automation.InfoDisplayMode;
import usa.cactuspuppy.uhc_automation.InfoModeCache;
import usa.cactuspuppy.uhc_automation.Main;

import java.net.ConnectException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class SQLAPI {
    private static SQLAPI sqlapi;

    private LinkedList<UUID> uuidQueue;

    public SQLAPI() {
        sqlapi = this;
        uuidQueue = new LinkedList<>();
    }

    public static SQLAPI getInstance() {
        return sqlapi;
    }

    public void createUHCTimeTable() {
        try {
            if (noUHCTimeTable()) {
                Bukkit.getLogger().info("uhcinfo_mode table does not exist, creating...");
                Optional<Connection> connection = Main.getInstance().getConnection();
                if (!connection.isPresent()) {
                    Main.getInstance().getLogger().warning("Could not get connection to database from connection info! Please check your config.yml.");
                    return;
                }
                PreparedStatement statement = connection.get().prepareStatement("CREATE TABLE uhcinfo_mode (UniqueID VARCHAR(255) NOT NULL, Mode TINYTEXT NOT NULL, Primary Key (UniqueID));");
                statement.execute();
                statement.close();
                connection.get().close();
            } else {
                Main.getInstance().getLogger().info("uhcinfo_mode table exists, using that...");
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("SQLAPI Error: Could not create uhcinfo_mode table.");
            e.printStackTrace();
        } catch (ConnectException e) {
            Bukkit.getLogger().severe("Could not establish connection to database! Check your config.yml");
        }
    }

    public boolean noUHCTimeTable() throws ConnectException {
        try {
            Optional<Connection> connection = Main.getInstance().getConnection();
            if (!connection.isPresent()) {
                Main.getInstance().getLogger().warning("Could not get connection to database from connection info! Please check your config.yml.");
                throw new ConnectException();
            }
            PreparedStatement statement = connection.get().prepareStatement("SHOW TABLES LIKE 'uhcinfo_mode'");
            ResultSet resultSet = statement.executeQuery();
            boolean isTable = !resultSet.next();
            statement.close();
            connection.get().close();
            return isTable;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new ConnectException();
        }
    }

    private void storePlayerPref(UUID u, InfoDisplayMode tdm) {
        try {
            if (noUHCTimeTable()) {
                createUHCTimeTable();
            }
            if (tdm == null) {
                tdm = InfoDisplayMode.CHAT;
            }
            Optional<Connection> connection = Main.getInstance().getConnection();
            if (!connection.isPresent()) {
                Main.getInstance().getLogger().warning("Could not get connection to database from connection info! Please check your config.yml.");
                throw new ConnectException();
            }
            PreparedStatement statement = connection.get().prepareStatement("INSERT INTO uhcinfo_mode VALUES ('" + u.toString() + "', '" + tdm.name() +"') ON DUPLICATE KEY UPDATE Mode = '" + tdm.name() + "';");
            statement.execute();
            statement.close();
            connection.get().close();
        } catch (SQLException | ConnectException e) {
            Bukkit.getLogger().warning("SQLAPI Error: Could not update player timemode for UUID: " + u.toString());
            e.printStackTrace();
        }
    }

    public void storeQueue(List<UUID> prefs) {
        for (UUID u : prefs) {
            storePlayerPref(u, InfoModeCache.getInstance().getPlayerPref(u));
        }
    }

    public Map<UUID, InfoDisplayMode> getPlayerPrefs() {
        try {
            Map<UUID, InfoDisplayMode> rv = new HashMap<>();
            if (noUHCTimeTable()) {
                return rv;
            }
            Optional<Connection> connection = Main.getInstance().getConnection();
            if (!connection.isPresent()) {
                throw new ConnectException();
            }
            ResultSet result = connection.get().prepareStatement("SELECT * FROM uhcinfo_mode;").executeQuery();
            while (result.next()) {
                rv.put(UUID.fromString(result.getString("UniqueID")), InfoDisplayMode.fromString(result.getString("Mode")));
            }
            return rv;
        } catch (SQLException | ConnectException e) {
            Main.getInstance().getLogger().severe("SQLAPI Error: Could not get player timemodes from timetable.");
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public boolean queueEmpty() {
        return uuidQueue.isEmpty();
    }

    public List<UUID> getQueue() {
        return uuidQueue;
    }

    public void enqueuePlayerUpdate(UUID uuid) {
        uuidQueue.addLast(uuid);
    }

    public void executePlayerUpdate() {
        UUID u = uuidQueue.pollFirst();
        if (u == null) {
            return;
        }
        storePlayerPref(u, InfoModeCache.getInstance().getPlayerPref(u));
    }
}
