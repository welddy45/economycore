package ru.corearchitect.coreeconomy.manager;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.corearchitect.coreeconomy.CoreEconomy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

public class BackupManager {

    private final CoreEconomy plugin;
    private final ConfigManager configManager;
    private BukkitTask backupTask;

    public BackupManager(CoreEconomy plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    public void start() {
        if (!configManager.isBackupEnabled() || !configManager.getStorageType().equalsIgnoreCase("SQLITE")) {
            return;
        }

        long interval = configManager.getBackupIntervalHours() * 20L * 60L * 60L;
        this.backupTask = new BukkitRunnable() {
            @Override
            public void run() {
                createBackup();
            }
        }.runTaskTimerAsynchronously(plugin, interval, interval);
    }

    public void stop() {
        if (backupTask != null) {
            backupTask.cancel();
        }
    }

    private void createBackup() {
        File dbFile = new File(plugin.getDataFolder(), configManager.getSQLiteFilename());
        if (!dbFile.exists()) {
            return;
        }

        File backupDir = new File(plugin.getDataFolder(), "backups");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File backupFile = new File(backupDir, "database-" + timestamp + ".db.bak");

        try {
            Files.copy(dbFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Database backup created successfully: " + backupFile.getName());
            rotateBackups(backupDir);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create database backup!");
            e.printStackTrace();
        }
    }

    private void rotateBackups(File backupDir) {
        File[] backupFiles = backupDir.listFiles((dir, name) -> name.endsWith(".bak"));
        if (backupFiles == null || backupFiles.length <= configManager.getMaxBackupFiles()) {
            return;
        }

        Arrays.sort(backupFiles, Comparator.comparingLong(File::lastModified));

        int filesToDelete = backupFiles.length - configManager.getMaxBackupFiles();
        for (int i = 0; i < filesToDelete; i++) {
            if (backupFiles[i].delete()) {
                plugin.getLogger().info("Deleted old backup: " + backupFiles[i].getName());
            } else {
                plugin.getLogger().warning("Failed to delete old backup: " + backupFiles[i].getName());
            }
        }
    }
}