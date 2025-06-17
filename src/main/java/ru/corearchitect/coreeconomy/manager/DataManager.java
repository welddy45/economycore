package ru.corearchitect.coreeconomy.manager;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.corearchitect.coreeconomy.CoreEconomy;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {

    private final CoreEconomy plugin;
    private final File dataFile;
    private final Map<UUID, BigDecimal> balances = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> frozenAccounts = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> scoreboardStates = new ConcurrentHashMap<>();

    public DataManager(CoreEconomy plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "balances.yml");
        loadData();
    }

    public synchronized void loadData() {
        if (!dataFile.exists()) {
            try {
                plugin.saveResource("balances.yml", false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("balances.yml template not found, creating a new empty file.");
                try {
                    dataFile.createNewFile();
                } catch (IOException ioException) {
                    plugin.getLogger().severe("Could not create balances.yml!");
                    ioException.printStackTrace();
                }
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        balances.clear();
        frozenAccounts.clear();
        scoreboardStates.clear();

        ConfigurationSection balancesSection = config.getConfigurationSection("balances");
        if (balancesSection != null) {
            for (String key : balancesSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    BigDecimal balance = new BigDecimal(balancesSection.getString(key, "0"));
                    balances.put(uuid, balance);
                } catch (IllegalArgumentException ignored) {}
            }
        }

        ConfigurationSection frozenSection = config.getConfigurationSection("frozen_accounts");
        if (frozenSection != null) {
            for (String key : frozenSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    boolean isFrozen = frozenSection.getBoolean(key + ".frozen", false);
                    if (isFrozen) {
                        frozenAccounts.put(uuid, true);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }

        ConfigurationSection scoreboardSection = config.getConfigurationSection("scoreboard_states");
        if (scoreboardSection != null) {
            for (String key : scoreboardSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    scoreboardStates.put(uuid, scoreboardSection.getBoolean(key));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    private FileConfiguration prepareConfigForSave() {
        FileConfiguration config = new YamlConfiguration();
        balances.forEach((uuid, balance) -> config.set("balances." + uuid.toString(), balance.toPlainString()));
        frozenAccounts.forEach((uuid, isFrozen) -> {
            if (isFrozen) {
                config.set("frozen_accounts." + uuid.toString() + ".frozen", true);
            }
        });
        scoreboardStates.forEach((uuid, isEnabled) -> config.set("scoreboard_states." + uuid.toString(), isEnabled));
        return config;
    }

    public synchronized void saveDataAsync() {
        FileConfiguration configToSave = prepareConfigForSave();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                configToSave.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save data to balances.yml during autosave!");
                e.printStackTrace();
            }
        });
    }

    public synchronized void saveDataBlocking() {
        try {
            prepareConfigForSave().save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data to balances.yml during shutdown!");
            e.printStackTrace();
        }
    }

    public void createAccount(UUID uuid) {
        balances.putIfAbsent(uuid, BigDecimal.ZERO);
        scoreboardStates.putIfAbsent(uuid, plugin.getConfigManager().isScoreboardEnabledByDefault());
    }

    public boolean hasAccount(UUID uuid) {
        return balances.containsKey(uuid);
    }

    public BigDecimal getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, BigDecimal.ZERO);
    }

    public void setBalance(UUID uuid, BigDecimal amount) {
        balances.put(uuid, amount);
    }

    public boolean isFrozen(UUID uuid) {
        return frozenAccounts.getOrDefault(uuid, false);
    }

    public void setFrozen(UUID uuid, boolean frozen) {
        if (frozen) {
            frozenAccounts.put(uuid, true);
        } else {
            frozenAccounts.remove(uuid);
        }
    }

    public boolean getScoreboardState(UUID uuid) {
        return scoreboardStates.getOrDefault(uuid, plugin.getConfigManager().isScoreboardEnabledByDefault());
    }

    public void setScoreboardState(UUID uuid, boolean enabled) {
        scoreboardStates.put(uuid, enabled);
    }

    public Map<UUID, BigDecimal> getAllBalances() {
        return new ConcurrentHashMap<>(balances);
    }
}