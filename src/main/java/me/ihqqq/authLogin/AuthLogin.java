package me.ihqqq.authLogin;

import me.ihqqq.authLogin.commands.DisableCommand;
import me.ihqqq.authLogin.commands.LoginCommand;
import me.ihqqq.authLogin.commands.SetSpawnCommand;
import me.ihqqq.authLogin.commands.SetupCommand;
import me.ihqqq.authLogin.listeners.PlayerListener;
import me.ihqqq.authLogin.managers.AuthDataManager;
import me.ihqqq.authLogin.managers.MessageManager;
import me.ihqqq.authLogin.managers.SoundManager;
import me.ihqqq.authLogin.managers.SpawnManager;
import org.bukkit.plugin.java.JavaPlugin;

import static me.ihqqq.authLogin.listeners.PlayerListener.colorize;

public final class AuthLogin extends JavaPlugin {

    private AuthDataManager authDataManager;
    private MessageManager messageManager;
    private SoundManager soundManager;
    private SpawnManager spawnManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        authDataManager = new AuthDataManager(this);
        messageManager  = new MessageManager(this);
        soundManager    = new SoundManager(this);
        spawnManager    = new SpawnManager(this);

        SetupCommand setupCommand = new SetupCommand(this, authDataManager, messageManager);

        PlayerListener playerListener = new PlayerListener(
                this, authDataManager, setupCommand, messageManager, soundManager, spawnManager);
        getServer().getPluginManager().registerEvents(playerListener, this);

        getCommand("login").setExecutor(
                new LoginCommand(this, authDataManager, playerListener, messageManager, soundManager));
        getCommand("2fasetup").setExecutor(setupCommand);
        getCommand("2fadisable").setExecutor(
                new DisableCommand(this, authDataManager, messageManager, soundManager));
        getCommand("setauthspawn").setExecutor(
                new SetSpawnCommand(spawnManager, messageManager));

        log("&f--------------------------------");
        log("&2    _         _   _     _");
        log("&2   / \\  _   _| |_| |__ | |    ___   __ _(_)_ __");
        log("&2  / _ \\| | | | __| '_ \\| |   / _ \\ / _` | | '_ \\");
        log("&2 / ___ \\ |_| | |_| | | | |__| (_) | (_| | | | | |");
        log("&2/_/   \\_\\__,_|\\__|_| |_|_____\\___/ \\__, |_|_| |_|");
        log("&2                                    |___/");
        log("");
        log("&fVersion: &b" + getDescription().getVersion());
        log("&fAuthor: &bihqqq");
        log("&a[ENABLED]");
        log("&f--------------------------------");
    }

    @Override
    public void onDisable() {
        if (authDataManager != null) {
            authDataManager.save();
        }

        log("&f--------------------------------");
        log("&c    _         _   _     _");
        log("&c   / \\  _   _| |_| |__ | |    ___   __ _(_)_ __");
        log("&c  / _ \\| | | | __| '_ \\| |   / _ \\ / _` | | '_ \\");
        log("&c / ___ \\ |_| | |_| | | | |__| (_) | (_| | | | | |");
        log("&c/_/   \\_\\__,_|\\__|_| |_|_____\\___/ \\__, |_|_| |_|");
        log("&c                                    |___/");
        log("");
        log("&fVersion: &b" + getDescription().getVersion());
        log("&fAuthor: &bihqqq");
        log("&4[DISABLED]");
        log("&f--------------------------------");
    }

    private void log(String message) {
        getServer().getConsoleSender().sendMessage(colorize(message));
    }

    public AuthDataManager getAuthDataManager() { return authDataManager; }
    public MessageManager getMessageManager()   { return messageManager; }
    public SoundManager getSoundManager()       { return soundManager; }
    public SpawnManager getSpawnManager()       { return spawnManager; }
}