package me.ihqqq.authLogin.commands;

import me.ihqqq.authLogin.managers.AuthDataManager;
import me.ihqqq.authLogin.managers.MessageManager;
import me.ihqqq.authLogin.utils.QrMapRenderer;
import me.ihqqq.authLogin.utils.TotpUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SetupCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final AuthDataManager authData;
    private final MessageManager messages;

    private final Map<UUID, String>  pendingSecrets = new HashMap<>();
    private final Map<UUID, Integer> pendingMapIds  = new HashMap<>();

    public SetupCommand(JavaPlugin plugin, AuthDataManager authData, MessageManager messages) {
        this.plugin   = plugin;
        this.authData = authData;
        this.messages = messages;
    }

    public boolean isPendingQrMap(UUID uuid, ItemStack item) {
        if (item == null || item.getType() != Material.FILLED_MAP) return false;
        Integer mapId = pendingMapIds.get(uuid);
        if (mapId == null) return false;
        if (!(item.getItemMeta() instanceof MapMeta mapMeta)) return false;
        MapView view = mapMeta.getMapView();
        return view != null && view.getId() == mapId;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("messages.players-only"));
            return true;
        }

        UUID uuid = player.getUniqueId();

        // /2fasetup <mã> — xác nhận mã sau khi quét QR
        if (args.length == 1) {
            String pendingSecret = pendingSecrets.get(uuid);
            if (pendingSecret == null) {
                player.sendMessage(messages.get("messages.setup-no-pending"));
                return true;
            }

            int code;
            try {
                code = Integer.parseInt(args[0].trim());
            } catch (NumberFormatException e) {
                player.sendMessage(messages.get("messages.invalid-code-format"));
                return true;
            }

            if (TotpUtil.verifyCode(pendingSecret, code)) {
                authData.setSecret(uuid, pendingSecret);
                authData.setLoggedIn(uuid, true);
                pendingSecrets.remove(uuid);
                pendingMapIds.remove(uuid);
                removeAllMaps(player);
                player.sendMessage(messages.get("messages.setup-success"));
                player.sendMessage(messages.get("messages.setup-auto-login"));
            } else {
                player.sendMessage(messages.get("messages.setup-wrong-code"));
            }
            return true;
        }

        // /2fasetup — tạo QR mới
        if (pendingMapIds.containsKey(uuid)) {
            removeQrMap(player, pendingMapIds.remove(uuid));
        }

        String secret = TotpUtil.generateSecret();
        pendingSecrets.put(uuid, secret);

        String serverName = plugin.getConfig().getString("server-name", "MinecraftServer");
        String otpUri     = TotpUtil.buildOtpAuthUri(secret, player.getName(), serverName);

        MapView mapView = Bukkit.createMap(player.getWorld());
        mapView.getRenderers().forEach(mapView::removeRenderer);
        mapView.addRenderer(new QrMapRenderer(otpUri));
        mapView.setLocked(true);
        mapView.setTrackingPosition(false);

        pendingMapIds.put(uuid, mapView.getId());

        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) mapItem.getItemMeta();
        if (meta != null) {
            meta.setMapView(mapView);
            meta.setDisplayName(messages.get("messages.setup-qr-map-name"));
            mapItem.setItemMeta(meta);
        }

        player.getInventory().setItemInMainHand(mapItem);

        for (String line : messages.getList("messages.setup-guide", "%secret%", secret)) {
            player.sendMessage(line);
        }

        return true;
    }

    private void removeQrMap(Player player, Integer mapId) {
        if (mapId == null) return;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != Material.FILLED_MAP) continue;
            if (!(item.getItemMeta() instanceof MapMeta mapMeta)) continue;
            MapView view = mapMeta.getMapView();
            if (view != null && view.getId() == mapId) {
                player.getInventory().setItem(i, null);
            }
        }
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand.getType() == Material.FILLED_MAP
                && offHand.getItemMeta() instanceof MapMeta mm
                && mm.getMapView() != null && mm.getMapView().getId() == mapId) {
            player.getInventory().setItemInOffHand(null);
        }
    }

    private void removeAllMaps(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && contents[i].getType() == Material.FILLED_MAP) {
                player.getInventory().setItem(i, null);
            }
        }
        if (player.getInventory().getItemInOffHand().getType() == Material.FILLED_MAP) {
            player.getInventory().setItemInOffHand(null);
        }
    }
}