package me.ihqqq.authLogin.managers;

import me.ihqqq.authLogin.utils.SecretEncryptor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

    private final SecretEncryptor encryptor;

    public AuthDataManager(JavaPlugin plugin) {
        this.plugin    = plugin;
        this.dataFile  = new File(plugin.getDataFolder(), "players.yml");
        this.encryptor = loadOrCreateEncryptor();
        reload();
    }


    private SecretEncryptor loadOrCreateEncryptor() {
        File keyFile = new File(plugin.getDataFolder(), "server.key");
        plugin.getDataFolder().mkdirs();

        if (!keyFile.exists()) {
            byte[] key = SecretEncryptor.generateKey();
            try {
                Files.write(keyFile.toPath(), key);
                setFilePermissionsRestricted(keyFile);
                plugin.getLogger().info("Đã tạo server.key mới. " +
                        "BACKUP file này lại — mất key sẽ mất toàn bộ dữ liệu 2FA!");
            } catch (IOException e) {
                throw new RuntimeException("Không thể ghi server.key: " + e.getMessage(), e);
            }
            return new SecretEncryptor(key);
        }

        try {
            byte[] key = Files.readAllBytes(keyFile.toPath());
            if (key.length != 32) {
                throw new RuntimeException(
                        "server.key hỏng — Xóa file để tạo key mới (sẽ mất dữ liệu 2FA cũ).");
            }
            return new SecretEncryptor(key);
        } catch (IOException e) {
            throw new RuntimeException("Không thể đọc server.key: " + e.getMessage(), e);
        }
    }

    private void setFilePermissionsRestricted(File file) {
        try {
            file.setReadable(false, false);
            file.setReadable(true,  true);
            file.setWritable(false, false);
            file.setWritable(true,  true);
        } catch (Exception ignored) {}
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
        boolean needsMigration = false;

        if (dataConfig.contains("players")) {
            for (String uuidStr : dataConfig.getConfigurationSection("players").getKeys(false)) {
                String stored = dataConfig.getString("players." + uuidStr + ".secret");
                if (stored == null || stored.isEmpty()) continue;

                UUID uuid = UUID.fromString(uuidStr);

                if (SecretEncryptor.isEncrypted(stored)) {
                    try {
                        secretCache.put(uuid, encryptor.decrypt(stored));
                    } catch (Exception e) {
                        plugin.getLogger().severe(
                                "Không thể decrypt secret của " + uuidStr +
                                        ": " + e.getMessage() + " — bỏ qua player này.");
                    }
                } else {
                    plugin.getLogger().info("Migrating secret của " + uuidStr + " sang dạng mã hóa.");
                    secretCache.put(uuid, stored);
                    dataConfig.set("players." + uuidStr + ".secret", encryptor.encrypt(stored));
                    needsMigration = true;
                }
            }
        }

        if (needsMigration) {
            save();
            plugin.getLogger().info("Migration hoàn tất — tất cả secret đã được mã hóa.");
        }
    }

    public void save() {
        final File   target = dataFile;
        final String yaml   = dataConfig.saveToString();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Files.writeString(target.toPath(), yaml, StandardCharsets.UTF_8);
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

    public void setSecret(UUID uuid, String plaintextSecret) {
        secretCache.put(uuid, plaintextSecret);
        dataConfig.set("players." + uuid + ".secret", encryptor.encrypt(plaintextSecret));
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