package me.ihqqq.authLogin.managers;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthDataManager {

    private final JavaPlugin plugin;
    private final File dataFile;
    private YamlConfiguration dataConfig;

    private final Map<UUID, String>  secretCache  = new HashMap<>();
    private final Map<UUID, Boolean> loggedIn     = new HashMap<>();
    private final Map<UUID, Integer> failAttempts = new HashMap<>();

    public AuthDataManager(JavaPlugin plugin) {
        this.plugin   = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "players.yml");
        reload();
    }

    public void reload() {
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Không thể tạo players.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        secretCache.clear();
        if (dataConfig.contains("players")) {
            for (String uuidStr : dataConfig.getConfigurationSection("players").getKeys(false)) {
                String secret = dataConfig.getString("players." + uuidStr + ".secret");
                if (secret != null && !secret.isEmpty()) {
                    secretCache.put(UUID.fromString(uuidStr), secret);
                }
            }
        }
    }

    public void save() {
        final File   target = dataFile;
        final String yaml   = dataConfig.saveToString();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                java.nio.file.Files.writeString(target.toPath(), yaml,
                        java.nio.charset.StandardCharsets.UTF_8);
            } catch (IOException e) {
                plugin.getLogger().severe("Không thể lưu players.yml: " + e.getMessage());
            }
        });
    }

    public void saveSync() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Không thể lưu players.yml (sync): " + e.getMessage());
        }
    }


    public boolean hasSecret(UUID uuid) {
        return secretCache.containsKey(uuid);
    }

    public String getSecret(UUID uuid) {
        return secretCache.get(uuid);
    }

    public void setSecret(UUID uuid, String secret) {
        secretCache.put(uuid, secret);
        dataConfig.set("players." + uuid + ".secret", secret);
        save();
    }

    public void removeSecret(UUID uuid) {
        secretCache.remove(uuid);
        dataConfig.set("players." + uuid + ".secret", null);
        save();
    }


    public boolean isLoggedIn(UUID uuid) {
        return loggedIn.getOrDefault(uuid, false);
    }

    public void setLoggedIn(UUID uuid, boolean value) {
        loggedIn.put(uuid, value);
    }

    public void clearSession(UUID uuid) {
        loggedIn.remove(uuid);
        failAttempts.remove(uuid);
    }



    public int getFailAttempts(UUID uuid) {
        return failAttempts.getOrDefault(uuid, 0);
    }

    public int incrementFailAttempts(UUID uuid) {
        int current = getFailAttempts(uuid) + 1;
        failAttempts.put(uuid, current);
        return current;
    }

    public void resetFailAttempts(UUID uuid) {
        failAttempts.remove(uuid);
    }
}