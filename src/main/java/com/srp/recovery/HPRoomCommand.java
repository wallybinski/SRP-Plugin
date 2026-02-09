package com.srp.recovery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class HPRoomCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final RoomManager roomManager;

    public HPRoomCommand(JavaPlugin plugin, DatabaseManager databaseManager, RoomManager roomManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.roomManager = roomManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 2) {
            sendMessage(sender, "messages.room-usage");
            return true;
        }

        if (!hasRoomAccess(sender)) {
            sendMessage(sender, "messages.room-no-permission");
            return true;
        }

        String roomId = args[0];
        String action = args[1].toLowerCase(Locale.ROOT);
        if (!roomManager.getRooms().containsKey(roomId)) {
            sendMessage(sender, "messages.room-invalid");
            return true;
        }

        if (!action.equals("open") && !action.equals("close")) {
            sendMessage(sender, "messages.room-usage");
            return true;
        }

        boolean inUse = action.equals("close");
        databaseManager.setRoomUsed(roomId, inUse);
        String message = getMessage("messages.room-updated")
            .replace("%room%", roomId)
            .replace("%state%", inUse ? "closed" : "open");
        sender.sendMessage(colorize(message));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return roomManager.getRooms().keySet().stream()
                .filter(roomId -> roomId.toLowerCase(Locale.ROOT).startsWith(prefix))
                .collect(Collectors.toList());
        }
        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Arrays.asList("open", "close").stream()
                .filter(option -> option.startsWith(prefix))
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private boolean hasRoomAccess(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        if (sender.hasPermission("srprecovery.hproom")) {
            return true;
        }
        String tag = plugin.getConfig().getString("room-manager-tag", "");
        if (tag.isEmpty()) {
            return false;
        }
        Set<String> tags = player.getScoreboardTags();
        return tags.contains(tag);
    }

    private void sendMessage(CommandSender sender, String path) {
        sender.sendMessage(colorize(getMessage(path)));
    }

    private String getMessage(String path) {
        return plugin.getConfig().getString(path, "");
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
