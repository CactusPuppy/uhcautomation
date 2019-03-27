package usa.cactuspuppy.uhc_automation.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

public final class MojangAPIHook {
    private static BiHashMap<String, UUID> nameUUIDCache = new BiHashMap<>();

    public static void updateCache(String username, UUID uuid) {
        nameUUIDCache.put(username, uuid);
    }

    public static void invalidateCache() {
        nameUUIDCache.clear();
    }

    public static UUID getUUID(String username) {
        if (!validUsername(username)) {
            return null;
        }
        OfflinePlayer p = Bukkit.getPlayer(username);
        if (p != null) {
            return p.getUniqueId();
        } else {
            try {
                URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);
                String response = queryURL(url);
                JSONObject responseJSON = (JSONObject) new JSONParser().parse(response);
                String uuidString = (String) responseJSON.get("id");
                uuidString = uuidString.replaceAll("(.{8})(.{4})(.{4})(.{4})(.+)", "$1-$2-$3-$4-$5");
                return UUID.fromString(uuidString);
            } catch (MalformedURLException e) {
                Logger.logWarning(MojangAPIHook.class, "API URL invalid!", e);
            } catch (IOException e) {
                Logger.logWarning(MojangAPIHook.class, "Issue retrieving JSON payload", e);
            } catch (ParseException e) {
                Logger.logWarning(MojangAPIHook.class, "Issue parsing JSON payload", e);
            }
            return null;
        }
    }

    public static String nameFromUUID(UUID u) {
        if (u == null) {
            return null;
        }
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/" + u + "/names");
            String response = queryURL(url);
            JSONArray responseJSON = (JSONArray) new JSONParser().parse(response);
            if (responseJSON.isEmpty()) {
                Logger.logWarning(MojangAPIHook.class, "Could not find UUID for username");
            }
            JSONObject obj = (JSONObject) responseJSON.get(0);
            return (String) obj.get("name");

        } catch (MalformedURLException e) {
            Logger.logWarning(MojangAPIHook.class, "API URL invalid!", e);
        } catch (IOException e) {
            Logger.logWarning(MojangAPIHook.class, "Issue retrieving JSON payload", e);
        } catch (ParseException e) {
            Logger.logWarning(MojangAPIHook.class, "Issue parsing JSON payload", e);
        } catch (RuntimeException e) {
            Logger.logWarning(MojangAPIHook.class, "", e);
        }
        return null;
    }

    public static boolean validUsername(String name) {
        if (name.length() < 3 || name.length() > 16) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || (c == '_'))) {
                return false;
            }
        }
        return true;
    }

    private static String queryURL(URL url) throws IOException {
        StringBuilder responseBuilder = new StringBuilder();

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();

        int responseCode = connection.getResponseCode();
        String responseMessage = connection.getResponseMessage();
        if (responseCode != 200) {
            String response = "Bad response code while querying Mojang API.\n" +
                    "Code: " + responseCode + "\n" +
                    "Message: " + responseMessage + "\n" +
                    "Queried: " + url.toString();
            throw new RuntimeException(response);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line = reader.readLine();
            while (line != null) {
                responseBuilder.append(line);
                line = reader.readLine();
            }
        }
        return responseBuilder.toString();
    }
}
