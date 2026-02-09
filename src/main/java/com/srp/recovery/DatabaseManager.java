package com.srp.recovery;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.plugin.java.JavaPlugin;

public class DatabaseManager {
    private final JavaPlugin plugin;
    private Connection connection;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Failed to create plugin data folder.");
        }
        File dbFile = new File(dataFolder, "recovery.db");
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS recovery_requests ("
                    + "target_uuid TEXT PRIMARY KEY,"
                    + "target_name TEXT NOT NULL,"
                    + "target_nick TEXT NOT NULL,"
                    + "expires_at INTEGER NOT NULL"
                    + ")");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS rooms ("
                    + "room_id TEXT PRIMARY KEY,"
                    + "in_use INTEGER NOT NULL"
                    + ")");
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to initialize database: " + exception.getMessage());
        }
    }

    public void close() {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to close database: " + exception.getMessage());
        }
    }

    public void upsertRecoveryRequest(UUID targetUuid, String targetName, String targetNick, long expiresAt) {
        String sql = "INSERT INTO recovery_requests (target_uuid, target_name, target_nick, expires_at) VALUES (?, ?, ?, ?)"
            + "ON CONFLICT(target_uuid) DO UPDATE SET target_name = excluded.target_name, target_nick = excluded.target_nick, expires_at = excluded.expires_at";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, targetUuid.toString());
            statement.setString(2, targetName);
            statement.setString(3, targetNick);
            statement.setLong(4, expiresAt);
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to save recovery request: " + exception.getMessage());
        }
    }

    public Optional<RecoveryRequest> getRecoveryRequest(UUID targetUuid) {
        String sql = "SELECT target_name, target_nick, expires_at FROM recovery_requests WHERE target_uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, targetUuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(new RecoveryRequest(
                        targetUuid,
                        resultSet.getString("target_name"),
                        resultSet.getString("target_nick"),
                        resultSet.getLong("expires_at")
                    ));
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to load recovery request: " + exception.getMessage());
        }
        return Optional.empty();
    }

    public void deleteRecoveryRequest(UUID targetUuid) {
        String sql = "DELETE FROM recovery_requests WHERE target_uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, targetUuid.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to delete recovery request: " + exception.getMessage());
        }
    }

    public void ensureRoom(String roomId) {
        String sql = "INSERT OR IGNORE INTO rooms (room_id, in_use) VALUES (?, 0)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, roomId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to ensure room: " + exception.getMessage());
        }
    }

    public void setRoomUsed(String roomId, boolean inUse) {
        String sql = "INSERT INTO rooms (room_id, in_use) VALUES (?, ?) ON CONFLICT(room_id) DO UPDATE SET in_use = excluded.in_use";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, roomId);
            statement.setInt(2, inUse ? 1 : 0);
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to update room: " + exception.getMessage());
        }
    }

    public boolean isRoomUsed(String roomId) {
        String sql = "SELECT in_use FROM rooms WHERE room_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, roomId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("in_use") == 1;
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to check room: " + exception.getMessage());
        }
        return false;
    }

    public List<String> getRoomsInUse() {
        String sql = "SELECT room_id FROM rooms WHERE in_use = 1";
        List<String> rooms = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                rooms.add(resultSet.getString("room_id"));
            }
        } catch (SQLException exception) {
            plugin.getLogger().severe("Failed to list rooms: " + exception.getMessage());
        }
        return rooms;
    }
}
