package me.ihqqq.authLogin.managers;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static me.ihqqq.authLogin.listeners.PlayerListener.colorize;

public class MessageManager {

    private final JavaPlugin plugin;
    private YamlConfiguration messagesConfig;
    private final File messagesFile;

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        reload();
    }

    public void reload() {
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            messagesConfig.setDefaults(defaults);
        }
    }

    public String getRaw(String key) {
        String raw = messagesConfig.getString(key);
        if (raw == null) {
            plugin.getLogger().warning("Không tìm thấy message key: " + key);
            return "&c[AuthLogin] &fThiếu message: " + key;
        }
        return raw;
    }

    public String get(String key) {
        return colorize(getRaw(key));
    }

    public String get(String key, String... placeholders) {
        String msg = get(key);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            msg = msg.replace(placeholders[i], placeholders[i + 1]);
        }
        return msg;
    }

    public List<String> getList(String key) {
        List<String> list = messagesConfig.getStringList(key);
        if (list.isEmpty()) {
            plugin.getLogger().warning("Không tìm thấy message list key: " + key);
        }
        return list.stream().map(s -> colorize(s)).collect(Collectors.toList());
    }

    public List<String> getList(String key, String... placeholders) {
        List<String> list = getList(key);
        return list.stream().map(line -> {
            for (int i = 0; i + 1 < placeholders.length; i += 2) {
                line = line.replace(placeholders[i], placeholders[i + 1]);
            }
            return line;
        }).collect(Collectors.toList());
    }
}