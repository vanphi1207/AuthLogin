package me.ihqqq.authLogin.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.ihqqq.authLogin.commands.SetupCommand;
import me.ihqqq.authLogin.managers.AuthDataManager;
import me.ihqqq.authLogin.managers.MessageManager;
import me.ihqqq.authLogin.managers.SoundManager;
import me.ihqqq.authLogin.managers.SpawnManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {
    private final JavaPlugin plugin;
    private final AuthDataManager authData;
    private final SetupCommand setupCommand;
    private final MessageManager messages;
    private final SoundManager sounds;
    private final SpawnManager spawnManager;
    private final Map<UUID, BukkitRunnable> timeoutTasks = new HashMap<>();

    public PlayerListener(JavaPlugin plugin, AuthDataManager authData, SetupCommand setupCommand,
                          MessageManager messages, SoundManager sounds, SpawnManager spawnManager) {
        this.plugin        = plugin;
        this.authData      = authData;
        this.setupCommand  = setupCommand;
        this.messages      = messages;
        this.sounds        = sounds;
        this.spawnManager  = spawnManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (player.hasPermission("totpauth.bypass")) {
            authData.setLoggedIn(uuid, true);
            return;
        }

        authData.setLoggedIn(uuid, false);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if(!player.isOnline()) return;

            for (Player other : plugin.getServer().getOnlinePlayers()) {
                if (!other.equals(player)) {
                    other.hidePlayer(plugin, player);
                }
            }

            Location authSpawn = spawnManager.getAuthSpawn();
            if (authSpawn != null) {
                player.teleport(authSpawn);
            }

            if (!authData.hasSecret(uuid)) {
                player.sendMessage(messages.get("messages.setup-prompt"));
            } else {
                player.sendMessage(messages.get("messages.join-prompt"));
                startLoginTimeout(player);
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (authData.isLoggedIn(uuid)) {
            spawnManager.saveLastLocation(uuid, player.getLocation());
        }

        setupCommand.cleanupPendingQr(uuid, player);

        cancelTimeout(uuid);
        authData.clearSession(uuid);

        for (Player other : plugin.getServer().getOnlinePlayers()) {
            other.showPlayer(plugin, player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack dropped = event.getItemDrop().getItemStack();
        if (setupCommand.isPendingQrMap(player.getUniqueId(), dropped)) {
            event.setCancelled(true);
            player.sendMessage(messages.get("messages.setup-no-drop-qr"));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack offHand = event.getOffHandItem();
        if (setupCommand.isPendingQrMap(player.getUniqueId(), offHand)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncChatEvent event) {
        if (!authData.isLoggedIn(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!authData.isLoggedIn(uuid)) {
            String cmd = event.getMessage().toLowerCase().split(" ")[0];
            if (!cmd.equals("/login") && !cmd.equals("/l")
                    && !cmd.equals("/2fasetup") && !cmd.equals("/setup2fa")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(messages.get("messages.must-login-first"));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onMove(PlayerMoveEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!authData.isLoggedIn(uuid)) {
            if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                    || event.getFrom().getBlockY() != event.getTo().getBlockY()
                    || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!authData.isLoggedIn(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!authData.isLoggedIn(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            if (!authData.isLoggedIn(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (!authData.isLoggedIn(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
        if (event.getEntity() instanceof Player player) {
            if (!authData.isLoggedIn(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    public void onLoginSuccess(Player player) {
        UUID uuid = player.getUniqueId();
        cancelTimeout(uuid);
        authData.setLoggedIn(uuid, true);
        authData.resetFailAttempts(uuid);

        player.sendActionBar(Component.empty());

        if (spawnManager.hasLastLocation(uuid)) {
            Location lastLoc = spawnManager.getLastLocation(uuid);
            if (lastLoc != null) {
                player.teleport(lastLoc);
            }
        }

        for (Player other : plugin.getServer().getOnlinePlayers()) {
            if (!other.equals(player)) {
                other.showPlayer(plugin, player);
                player.showPlayer(plugin, other);
            }
        }

        sounds.play(player, "login-success");
        player.sendMessage(messages.get("messages.login-success"));
    }

    private void startLoginTimeout(Player player) {
        int timeout = plugin.getConfig().getInt("login-timeout", 60);

        BukkitRunnable task = new BukkitRunnable() {
            int remaining = timeout;

            @Override
            public void run() {
                if (!player.isOnline() || authData.isLoggedIn(player.getUniqueId())) {
                    cancel();
                    return;
                }

                if (remaining <= 0) {
                    cancel();
                    player.sendActionBar(Component.empty());
                    player.kick(Component.text(messages.get("messages.login-timeout")));
                    return;
                }

                double ratio = (double) remaining / timeout;
                TextColor color;
                if (ratio > 0.5) {
                    color = NamedTextColor.GREEN;
                } else if (ratio > 0.25) {
                    color = NamedTextColor.YELLOW;
                } else {
                    color = NamedTextColor.RED;
                }

                int barLength = 20;
                int filled    = (int) Math.round(ratio * barLength);
                StringBuilder bar = new StringBuilder();
                for (int i = 0; i < barLength; i++) {
                    bar.append(i < filled ? "█" : "░");
                }

                String timeStr = remaining >= 60
                        ? String.format("%d:%02d", remaining / 60, remaining % 60)
                        : remaining + "s";

                Component actionbar = Component.empty()
                        .append(Component.text("Đăng nhập trong: ", NamedTextColor.WHITE))
                        .append(Component.text(timeStr + " ", color).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD))
                        .append(Component.text("[", NamedTextColor.DARK_GRAY))
                        .append(Component.text(bar.toString(), color))
                        .append(Component.text("]", NamedTextColor.DARK_GRAY));

                player.sendActionBar(actionbar);

                sounds.play(player, "countdown-tick");

                remaining--;
            }
        };

        task.runTaskTimer(plugin, 0L, 20L);
        timeoutTasks.put(player.getUniqueId(), task);
    }

    private void cancelTimeout(UUID uuid) {
        BukkitRunnable task = timeoutTasks.remove(uuid);
        if (task != null) task.cancel();
    }

}