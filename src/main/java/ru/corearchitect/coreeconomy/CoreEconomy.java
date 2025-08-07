package ru.corearchitect.coreeconomy;

import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import ru.corearchitect.coreeconomy.api.EconomyAPI;
import ru.corearchitect.coreeconomy.command.EconomyAdminCommand;
import ru.corearchitect.coreeconomy.command.PlayerEconomyCommand;
import ru.corearchitect.coreeconomy.listener.PlayerConnectionListener;
import ru.corearchitect.coreeconomy.manager.*;

import java.util.Objects;

public final class CoreEconomy extends JavaPlugin {

    private static CoreEconomy instance;
    private DataManager dataManager;
    private EconomyManager economyManager;
    private ConfigManager configManager;
    private ScoreboardManager scoreboardManager;
    private TransactionLogger transactionLogger;
    private LeaderboardManager leaderboardManager;
    private BackupManager backupManager;
    private BukkitTask autosaveTask;

    @Override
    public void onEnable() {
        instance = this;

        this.configManager = new ConfigManager(this);
        this.dataManager = new DataManager(this);
        this.economyManager = new EconomyManager(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.transactionLogger = new TransactionLogger(this);
        this.leaderboardManager = new LeaderboardManager(this);
        this.backupManager = new BackupManager(this);

        registerAPI();
        registerCommands();
        registerListeners();

        this.scoreboardManager.startUpdateTask();
        this.leaderboardManager.startUpdateTask();
        this.backupManager.start();
        startAutosaveTask();
    }

    @Override
    public void onDisable() {
        if (this.scoreboardManager != null) {
            this.scoreboardManager.cancelUpdateTask();
        }
        if (this.leaderboardManager != null) {
            this.leaderboardManager.cancelUpdateTask();
        }
        if (this.backupManager != null) {
            this.backupManager.stop();
        }
        if (this.autosaveTask != null) {
            this.autosaveTask.cancel();
        }
        if (this.dataManager != null) {
            this.dataManager.shutdown();
        }
        if (this.transactionLogger != null) {
            this.transactionLogger.close();
        }
    }

    private void registerAPI() {
        getServer().getServicesManager().register(EconomyAPI.class, this.economyManager, this, ServicePriority.Normal);
    }

    private void registerCommands() {
        PlayerEconomyCommand playerCommand = new PlayerEconomyCommand(this);
        EconomyAdminCommand adminCommand = new EconomyAdminCommand(this);

        Objects.requireNonNull(getCommand(configManager.getPlayerCommandName())).setExecutor(playerCommand);
        Objects.requireNonNull(getCommand(configManager.getPlayerCommandName())).setTabCompleter(playerCommand);

        Objects.requireNonNull(getCommand(configManager.getAdminCommandName())).setExecutor(adminCommand);
        Objects.requireNonNull(getCommand(configManager.getAdminCommandName())).setTabCompleter(adminCommand);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
    }

    private void startAutosaveTask() {
        if (!configManager.getStorageType().equalsIgnoreCase("YAML")) {
            return;
        }
        long interval = configManager.getAutosaveInterval() * 20L * 60L;
        this.autosaveTask = getServer().getScheduler().runTaskTimerAsynchronously(this, this.dataManager::saveAllData, interval, interval);
    }

    public static CoreEconomy getInstance() {
        return instance;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public TransactionLogger getTransactionLogger() {
        return transactionLogger;
    }

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }
}