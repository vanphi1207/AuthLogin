package me.ihqqq.authLogin.commands;

import me.ihqqq.authLogin.listeners.PlayerListener;
import me.ihqqq.authLogin.managers.AuthDataManager;
import me.ihqqq.authLogin.managers.MessageManager;
import me.ihqqq.authLogin.managers.SoundManager;
import me.ihqqq.authLogin.utils.TotpUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class LoginCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final AuthDataManager authData;
    private final PlayerListener listener;
    private final MessageManager messages;
    private final SoundManager sounds;

    public LoginCommand(JavaPlugin plugin, AuthDataManager authData, PlayerListener listener,
                        MessageManager messages, SoundManager sounds) {
        this.plugin   = plugin;
        this.authData = authData;
        this.listener = listener;
        this.messages = messages;
        this.sounds   = sounds;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("messages.players-only"));
            return true;
        }

        UUID uuid = player.getUniqueId();

        if (authData.isLoggedIn(uuid)) {
            player.sendMessage(messages.get("messages.already-logged-in"));
            return true;
        }

        if (!authData.hasSecret(uuid)) {
            player.sendMessage(messages.get("messages.setup-prompt"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(messages.get("messages.setup-usage"));
            return true;
        }

        int code;
        try {
            code = Integer.parseInt(args[0].trim());
        } catch (NumberFormatException e) {
            player.sendMessage(messages.get("messages.invalid-code-format"));
            return true;
        }

        String secret = authData.getSecret(uuid);
        if (TotpUtil.verifyCode(secret, code)) {
            listener.onLoginSuccess(player);
        } else {
            int maxAttempts = plugin.getConfig().getInt("max-attempts", 5);
            int attempts    = authData.incrementFailAttempts(uuid);
            int remaining   = maxAttempts - attempts;

            if (remaining <= 0) {
                player.kick(Component.text(messages.get("messages.too-many-attempts")));
            } else {
                sounds.play(player, "login-fail");
                player.sendMessage(messages.get("messages.login-fail",
                        "%attempts%", String.valueOf(remaining)));
            }
        }

        return true;
    }
}