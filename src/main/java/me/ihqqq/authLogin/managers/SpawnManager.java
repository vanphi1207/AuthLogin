package me.ihqqq.authLogin.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class SpawnManager {

    private final JavaPlugin plugin;
    private final File spawnFile;
    private YamlConfiguration spawnConfig;

    public SpawnManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.spawnFile = new File(plugin.getDataFolder(), "spawn.yml");
        reload();
    }

    public void reload() {
        if (!spawnFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                spawnFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Không thể tạo spawn.yml: " + e.getMessage());
            }
        }
        spawnConfig = YamlConfiguration.loadConfiguration(spawnFile);
    }

    private void save() {
        try {
            spawnConfig.save(spawnFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Không thể lưu spawn.yml: " + e.getMessage());
        }
    }

    // ── Auth Spawn ────────────────────────────────────────────────────────────

    public boolean hasAuthSpawn() {
        return spawnConfig.contains("auth-spawn.world");
    }

    public void setAuthSpawn(Location loc) {
        spawnConfig.set("auth-spawn.world", loc.getWorld().getName());
        spawnConfig.set("auth-spawn.x", loc.getX());
        spawnConfig.set("auth-spawn.y", loc.getY());
        spawnConfig.set("auth-spawn.z", loc.getZ());
        spawnConfig.set("auth-spawn.yaw",   (double) loc.getYaw());
        spawnConfig.set("auth-spawn.pitch", (double) loc.getPitch());
        save();
    }

    public Location getAuthSpawn() {
        String worldName = spawnConfig.getString("auth-spawn.world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        double x     = spawnConfig.getDouble("auth-spawn.x");
        double y     = spawnConfig.getDouble("auth-spawn.y");
        double z     = spawnConfig.getDouble("auth-spawn.z");
        float  yaw   = (float) spawnConfig.getDouble("auth-spawn.yaw");
        float  pitch = (float) spawnConfig.getDouble("auth-spawn.pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }

    // ── Last Location ─────────────────────────────────────────────────────────

    public void saveLastLocation(UUID uuid, Location loc) {
        String path = "last-location." + uuid;
        spawnConfig.set(path + ".world", loc.getWorld().getName());
        spawnConfig.set(path + ".x", loc.getX());
        spawnConfig.set(path + ".y", loc.getY());
        spawnConfig.set(path + ".z", loc.getZ());
        spawnConfig.set(path + ".yaw",   (double) loc.getYaw());
        spawnConfig.set(path + ".pitch", (double) loc.getPitch());
        save();
    }

    public boolean hasLastLocation(UUID uuid) {
        return spawnConfig.contains("last-location." + uuid + ".world");
    }

    public Location getLastLocation(UUID uuid) {
        String path = "last-location." + uuid;
        String worldName = spawnConfig.getString(path + ".world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        double x     = spawnConfig.getDouble(path + ".x");
        double y     = spawnConfig.getDouble(path + ".y");
        double z     = spawnConfig.getDouble(path + ".z");
        float  yaw   = (float) spawnConfig.getDouble(path + ".yaw");
        float  pitch = (float) spawnConfig.getDouble(path + ".pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }

    public void clearLastLocation(UUID uuid) {
        spawnConfig.set("last-location." + uuid, null);
        save();
    }
}