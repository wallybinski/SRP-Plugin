package com.srp.recovery;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class RecoveryCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final RoomManager roomManager;

    public RecoveryCommand(JavaPlugin plugin, DatabaseManager databaseManager, RoomManager roomManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.roomManager = roomManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 1) {
            handleRequest(player, args[0]);
            return true;
        }

        if (args.length == 0) {
            handleTeleport(player);
            return true;
        }

        sender.sendMessage("Usage: /recovery [time]");
        return true;
    }

    private void handleRequest(Player sender, String timeArg) {
        Optional<Duration> duration = DurationParser.parseDuration(timeArg);
        if (duration.isEmpty()) {
            sendMessage(sender, "messages.request-invalid-duration");
            return;
        }

        Player target = findTargetPlayer(sender, 6);
        if (target == null) {
            sendMessage(sender, "messages.request-missing-target");
            return;
        }

        long expiresAt = Instant.now().plus(duration.get()).toEpochMilli();
        String targetNick = target.getDisplayName();
        databaseManager.upsertRecoveryRequest(target.getUniqueId(), target.getName(), targetNick, expiresAt);

        String message = getMessage("messages.request-created")
            .replace("%target%", target.getName())
            .replace("%duration%", timeArg);
        sender.sendMessage(colorize(message));
    }

    private void handleTeleport(Player player) {
        Optional<RecoveryRequest> request = databaseManager.getRecoveryRequest(player.getUniqueId());
        if (request.isEmpty()) {
            sendMessage(player, "messages.request-not-found");
            return;
        }

        RecoveryRequest recoveryRequest = request.get();
        long now = Instant.now().toEpochMilli();
        if (recoveryRequest.getExpiresAt() < now) {
            databaseManager.deleteRecoveryRequest(player.getUniqueId());
            sendMessage(player, "messages.request-expired");
            return;
        }

        if (!recoveryRequest.getTargetName().equals(player.getName())
            || !recoveryRequest.getTargetNick().equals(player.getDisplayName())) {
            sendMessage(player, "messages.request-mismatch");
            return;
        }

        Optional<String> availableRoom = roomManager.findAvailableRoom();
        if (availableRoom.isEmpty()) {
            sendMessage(player, "messages.room-none-available");
            return;
        }

        String roomId = availableRoom.get();
        Optional<Location> location = roomManager.getRoomLocation(roomId);
        if (location.isEmpty()) {
            sendMessage(player, "messages.room-invalid");
            return;
        }

        databaseManager.setRoomUsed(roomId, true);
        databaseManager.deleteRecoveryRequest(player.getUniqueId());
        player.teleport(location.get());
        String message = getMessage("messages.room-teleport").replace("%room%", roomId);
        player.sendMessage(colorize(message));
    }

    private Player findTargetPlayer(Player player, int range) {
        Entity target = player.getTargetEntity(range);
        if (target instanceof Player targetPlayer) {
            return targetPlayer;
        }
        return null;
    }

    private void sendMessage(Player player, String path) {
        player.sendMessage(colorize(getMessage(path)));
    }

    private String getMessage(String path) {
        return plugin.getConfig().getString(path, "");
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
