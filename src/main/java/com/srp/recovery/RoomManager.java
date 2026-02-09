package com.srp.recovery;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class RoomManager {
    private final Map<String, Location> rooms = new LinkedHashMap<>();
    private final DatabaseManager databaseManager;
    private final JavaPlugin plugin;

    public RoomManager(FileConfiguration config, DatabaseManager databaseManager, JavaPlugin plugin) {
        this.databaseManager = databaseManager;
        this.plugin = plugin;
        loadRooms(config);
    }

    public void syncRooms() {
        for (String roomId : rooms.keySet()) {
            databaseManager.ensureRoom(roomId);
        }
    }

    public Map<String, Location> getRooms() {
        return Collections.unmodifiableMap(rooms);
    }

    public Optional<Location> getRoomLocation(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    public Optional<String> findAvailableRoom() {
        for (String roomId : rooms.keySet()) {
            if (!databaseManager.isRoomUsed(roomId)) {
                return Optional.of(roomId);
            }
        }
        return Optional.empty();
    }

    private void loadRooms(FileConfiguration config) {
        rooms.clear();
        ConfigurationSection section = config.getConfigurationSection("rooms");
        if (section == null) {
            plugin.getLogger().warning("No rooms configured.");
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection roomSection = section.getConfigurationSection(key);
            if (roomSection == null) {
                continue;
            }
            String worldName = roomSection.getString("world");
            if (worldName == null) {
                plugin.getLogger().warning("Room " + key + " missing world setting.");
                continue;
            }
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("Room " + key + " world not found: " + worldName);
                continue;
            }
            double x = roomSection.getDouble("x");
            double y = roomSection.getDouble("y");
            double z = roomSection.getDouble("z");
            float yaw = (float) roomSection.getDouble("yaw", 0);
            float pitch = (float) roomSection.getDouble("pitch", 0);
            rooms.put(key, new Location(world, x, y, z, yaw, pitch));
        }
    }
}
