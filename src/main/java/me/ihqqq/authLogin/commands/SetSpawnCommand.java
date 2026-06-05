package me.ihqqq.authLogin.commands;

import me.ihqqq.authLogin.managers.MessageManager;
import me.ihqqq.authLogin.managers.SpawnManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetSpawnCommand implements CommandExecutor {

    private final SpawnManager spawnManager;
    private final MessageManager messages;

    public SetSpawnCommand(SpawnManager spawnManager, MessageManager messages) {
        this.spawnManager = spawnManager;
        this.messages     = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("messages.players-only"));
            return true;
        }

        if (!player.hasPermission("totpauth.setspawn")) {
            player.sendMessage(messages.get("messages.no-permission"));
            return true;
        }

        spawnManager.setAuthSpawn(player.getLocation());
        player.sendMessage(messages.get("messages.setspawn-success"));
        return true;
    }
}