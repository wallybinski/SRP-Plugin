package com.srp.recovery;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class RecoveryPlugin extends JavaPlugin {
    private DatabaseManager databaseManager;
    private RoomManager roomManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        roomManager = new RoomManager(config, databaseManager, this);
        roomManager.syncRooms();

        RecoveryCommand recoveryCommand = new RecoveryCommand(this, databaseManager, roomManager);
        HPRoomCommand hpRoomCommand = new HPRoomCommand(this, databaseManager, roomManager);

        getCommand("recovery").setExecutor(recoveryCommand);
        getCommand("hproom").setExecutor(hpRoomCommand);
        getCommand("hproom").setTabCompleter(hpRoomCommand);
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }
}
