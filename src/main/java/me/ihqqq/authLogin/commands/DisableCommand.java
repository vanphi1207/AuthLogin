package me.ihqqq.authLogin.commands;

import me.ihqqq.authLogin.managers.AuthDataManager;
import me.ihqqq.authLogin.managers.MessageManager;
import me.ihqqq.authLogin.managers.SoundManager;
import me.ihqqq.authLogin.utils.TotpUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class DisableCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final AuthDataManager authData;
    private final MessageManager messages;
    private final SoundManager sounds;

    public DisableCommand(JavaPlugin plugin, AuthDataManager authData,
                          MessageManager messages, SoundManager sounds) {
        this.plugin   = plugin;
        this.authData = authData;
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

        if (!authData.hasSecret(uuid)) {
            player.sendMessage(messages.get("messages.not-setup"));
            return true;
        }

        if (!authData.isLoggedIn(uuid)) {
            player.sendMessage(messages.get("messages.must-login-to-disable"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(messages.get("messages.disable-usage"));
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
            authData.removeSecret(uuid);
            player.sendMessage(messages.get("messages.disable-success"));
            player.sendMessage(messages.get("messages.disable-no-longer-needed"));
            sounds.play(player, "setup-success");
        } else {
            sounds.play(player, "login-fail");
            player.sendMessage(messages.get("messages.disable-wrong-code"));
        }

        return true;
    }
}