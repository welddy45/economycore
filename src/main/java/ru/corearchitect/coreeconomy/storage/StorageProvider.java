package ru.corearchitect.coreeconomy.storage;

import org.bukkit.configuration.file.FileConfiguration;
import ru.corearchitect.coreeconomy.model.TransactionRecord;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StorageProvider {
    void createAccount(UUID uuid);
    boolean hasAccount(UUID uuid);
    BigDecimal getBalance(UUID uuid);
    void setBalance(UUID uuid, BigDecimal amount);
    boolean isFrozen(UUID uuid);
    void setFrozen(UUID uuid, boolean frozen);
    boolean getScoreboardState(UUID uuid);
    void setScoreboardState(UUID uuid, boolean enabled);
    Map<UUID, BigDecimal> getAllBalances();
    void addCommission(BigDecimal amount);
    BigDecimal getTotalCommission();
    BigDecimal calculateTotalBalance();
    void saveData();
    void close();
    boolean migrateFrom(FileConfiguration oldConfig);
    void logTransaction(TransactionRecord record);
    CompletableFuture<List<TransactionRecord>> getPlayerHistory(UUID playerUuid, int page);
    CompletableFuture<Integer> countPlayerHistory(UUID playerUuid);
}