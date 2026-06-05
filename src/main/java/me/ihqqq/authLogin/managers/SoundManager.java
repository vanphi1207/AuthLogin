package me.ihqqq.authLogin.managers;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SoundManager {

    private final JavaPlugin plugin;

    public SoundManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void play(Player player, String key) {
        String path = "sounds." + key;

        if (!plugin.getConfig().getBoolean(path + ".enabled", false)) {
            return;
        }

        String soundName = plugin.getConfig().getString(path + ".sound");
        if (soundName == null || soundName.isEmpty()) {
            plugin.getLogger().warning("Không tìm thấy sound key: " + path + ".sound");
            return;
        }

        float volume = (float) plugin.getConfig().getDouble(path + ".volume", 1.0);
        float pitch  = (float) plugin.getConfig().getDouble(path + ".pitch",  1.0);

        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Sound không hợp lệ '" + soundName + "' tại key: " + path
                    + ". Xem danh sách sound tại: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Sound.html");
        }
    }
}