package ru.corearchitect.coreeconomy.manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.corearchitect.coreeconomy.CoreEconomy;
import ru.corearchitect.coreeconomy.model.TransactionRecord;
import ru.corearchitect.coreeconomy.storage.SQLiteStorageProvider;
import ru.corearchitect.coreeconomy.storage.StorageProvider;
import ru.corearchitect.coreeconomy.storage.YamlStorageProvider;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DataManager {

    private final CoreEconomy plugin;
    private final StorageProvider storageProvider;

    public DataManager(CoreEconomy plugin) {
        this.plugin = plugin;
        String storageType = plugin.getConfigManager().getStorageType();

        if (storageType.equalsIgnoreCase("SQLITE")) {
            this.storageProvider = new SQLiteStorageProvider(plugin);
            handleMigration();
        } else {
            this.storageProvider = new YamlStorageProvider(plugin);
        }
    }

    private void handleMigration() {
        File oldDataFile = new File(plugin.getDataFolder(), "balances.yml");
        if (!oldDataFile.exists()) {
            return;
        }

        plugin.getLogger().info("Old balances.yml found. Starting migration to SQLite...");
        FileConfiguration oldConfig = YamlConfiguration.loadConfiguration(oldDataFile);
        boolean success = storageProvider.migrateFrom(oldConfig);

        if (success) {
            File migratedFile = new File(plugin.getDataFolder(), "balances.yml.migrated");
            try {
                Files.move(oldDataFile.toPath(), migratedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("Migration successful. Old data file renamed to balances.yml.migrated");
            } catch (IOException e) {
                plugin.getLogger().severe("Migration was successful, but failed to rename old data file.");
                e.printStackTrace();
            }
        } else {
            plugin.getLogger().severe("MIGRATION FAILED. The server will continue to use SQLite, but old balances were not transferred. Please check logs.");
        }
    }

    public void shutdown() {
        storageProvider.close();
    }

    public void saveAllData() {
        storageProvider.saveData();
    }

    public void createAccount(UUID uuid) {
        storageProvider.createAccount(uuid);
    }

    public boolean hasAccount(UUID uuid) {
        return storageProvider.hasAccount(uuid);
    }

    public BigDecimal getBalance(UUID uuid) {
        return storageProvider.getBalance(uuid);
    }

    public void setBalance(UUID uuid, BigDecimal amount) {
        storageProvider.setBalance(uuid, amount);
    }

    public boolean isFrozen(UUID uuid) {
        return storageProvider.isFrozen(uuid);
    }

    public void setFrozen(UUID uuid, boolean frozen) {
        storageProvider.setFrozen(uuid, frozen);
    }

    public boolean getScoreboardState(UUID uuid) {
        return storageProvider.getScoreboardState(uuid);
    }

    public void setScoreboardState(UUID uuid, boolean enabled) {
        storageProvider.setScoreboardState(uuid, enabled);
    }

    public Map<UUID, BigDecimal> getAllBalances() {
        return storageProvider.getAllBalances();
    }

    public void addCommission(BigDecimal amount) {
        storageProvider.addCommission(amount);
    }

    public BigDecimal getTotalCommission() {
        return storageProvider.getTotalCommission();
    }

    public BigDecimal calculateTotalBalance() {
        return storageProvider.calculateTotalBalance();
    }

    public void logTransaction(TransactionRecord record) {
        storageProvider.logTransaction(record);
    }

    public CompletableFuture<List<TransactionRecord>> getPlayerHistory(UUID playerUuid, int page) {
        return storageProvider.getPlayerHistory(playerUuid, page);
    }

    public CompletableFuture<Integer> countPlayerHistory(UUID playerUuid) {
        return storageProvider.countPlayerHistory(playerUuid);
    }
}