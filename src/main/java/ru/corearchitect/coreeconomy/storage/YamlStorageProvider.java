package ru.corearchitect.coreeconomy.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.corearchitect.coreeconomy.CoreEconomy;
import ru.corearchitect.coreeconomy.model.TransactionRecord;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class YamlStorageProvider implements StorageProvider {

    private final CoreEconomy plugin;
    private final File dataFile;
    private final Map<UUID, BigDecimal> balances = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> frozenAccounts = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> scoreboardStates = new ConcurrentHashMap<>();
    private BigDecimal totalCommission = BigDecimal.ZERO;

    public YamlStorageProvider(CoreEconomy plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "balances.yml");
        loadData();
    }

    private synchronized void loadData() {
        if (!dataFile.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        balances.clear();
        frozenAccounts.clear();
        scoreboardStates.clear();

        this.totalCommission = new BigDecimal(config.getString("economy_stats.total_commission", "0.0"));

        ConfigurationSection balancesSection = config.getConfigurationSection("balances");
        if (balancesSection != null) {
            balancesSection.getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    BigDecimal balance = new BigDecimal(balancesSection.getString(key, "0"));
                    balances.put(uuid, balance);
                } catch (IllegalArgumentException ignored) {}
            });
        }

        ConfigurationSection frozenSection = config.getConfigurationSection("frozen_accounts");
        if (frozenSection != null) {
            frozenSection.getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    if (frozenSection.getBoolean(key, false)) {
                        frozenAccounts.put(uuid, true);
                    }
                } catch (IllegalArgumentException ignored) {}
            });
        }

        ConfigurationSection scoreboardSection = config.getConfigurationSection("scoreboard_states");
        if (scoreboardSection != null) {
            scoreboardSection.getKeys(false).forEach(key -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    scoreboardStates.put(uuid, scoreboardSection.getBoolean(key));
                } catch (IllegalArgumentException ignored) {}
            });
        }
    }

    @Override
    public synchronized void saveData() {
        FileConfiguration config = new YamlConfiguration();
        config.set("economy_stats.total_commission", totalCommission.toPlainString());
        balances.forEach((uuid, balance) -> config.set("balances." + uuid.toString(), balance.toPlainString()));
        frozenAccounts.forEach((uuid, isFrozen) -> {
            if (isFrozen) {
                config.set("frozen_accounts." + uuid.toString(), true);
            }
        });
        scoreboardStates.forEach((uuid, isEnabled) -> config.set("scoreboard_states." + uuid.toString(), isEnabled));

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data to balances.yml!");
            e.printStackTrace();
        }
    }

    @Override
    public void createAccount(UUID uuid) {
        balances.putIfAbsent(uuid, BigDecimal.ZERO);
        scoreboardStates.putIfAbsent(uuid, plugin.getConfigManager().isScoreboardEnabledByDefault());
    }

    @Override
    public boolean hasAccount(UUID uuid) {
        return balances.containsKey(uuid);
    }

    @Override
    public BigDecimal getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, BigDecimal.ZERO);
    }

    @Override
    public void setBalance(UUID uuid, BigDecimal amount) {
        balances.put(uuid, amount);
    }

    @Override
    public boolean isFrozen(UUID uuid) {
        return frozenAccounts.getOrDefault(uuid, false);
    }

    @Override
    public void setFrozen(UUID uuid, boolean frozen) {
        if (frozen) {
            frozenAccounts.put(uuid, true);
        } else {
            frozenAccounts.remove(uuid);
        }
    }

    @Override
    public boolean getScoreboardState(UUID uuid) {
        return scoreboardStates.getOrDefault(uuid, plugin.getConfigManager().isScoreboardEnabledByDefault());
    }

    @Override
    public void setScoreboardState(UUID uuid, boolean enabled) {
        scoreboardStates.put(uuid, enabled);
    }

    @Override
    public Map<UUID, BigDecimal> getAllBalances() {
        return new ConcurrentHashMap<>(balances);
    }

    @Override
    public synchronized void addCommission(BigDecimal amount) {
        this.totalCommission = this.totalCommission.add(amount);
    }

    @Override
    public BigDecimal getTotalCommission() {
        return totalCommission;
    }

    @Override
    public BigDecimal calculateTotalBalance() {
        return balances.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public void close() {
        saveData();
    }

    @Override
    public boolean migrateFrom(FileConfiguration oldConfig) {
        plugin.getLogger().warning("Migration from YAML to YAML is not a valid operation.");
        return false;
    }

    @Override
    public void logTransaction(TransactionRecord record) {
        // Transaction history is not supported for YAML storage.
    }

    @Override
    public CompletableFuture<List<TransactionRecord>> getPlayerHistory(UUID playerUuid, int page) {
        plugin.getLogger().warning("Transaction history is not supported for YAML storage.");
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    @Override
    public CompletableFuture<Integer> countPlayerHistory(UUID playerUuid) {
        return CompletableFuture.completedFuture(0);
    }
}