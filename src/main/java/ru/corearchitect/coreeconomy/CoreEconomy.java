package ru.corearchitect.coreeconomy;

import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import ru.corearchitect.coreeconomy.api.EconomyAPI;
import ru.corearchitect.coreeconomy.listener.PlayerConnectionListener;
import ru.corearchitect.coreeconomy.manager.*;

public final class CoreEconomy extends JavaPlugin {

    private static CoreEconomy instance;
    private DataManager dataManager;
    private EconomyManager economyManager;
    private ConfigManager configManager;
    private ScoreboardManager scoreboardManager;
    private CommandManager commandManager;
    private TransactionLogger transactionLogger;
    private LeaderboardManager leaderboardManager;
    private BukkitTask autosaveTask;

    @Override
    public void onEnable() {
        instance = this;

        this.configManager = new ConfigManager(this);
        this.dataManager = new DataManager(this);
        this.economyManager = new EconomyManager(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.commandManager = new CommandManager(this);
        this.transactionLogger = new TransactionLogger(this);
        this.leaderboardManager = new LeaderboardManager(this);

        registerAPI();
        registerCommands();
        registerListeners();

        this.scoreboardManager.startUpdateTask();
        this.leaderboardManager.startUpdateTask();
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
        if (this.autosaveTask != null) {
            this.autosaveTask.cancel();
        }
        if (this.dataManager != null) {
            this.dataManager.saveDataBlocking();
        }
        if (this.transactionLogger != null) {
            this.transactionLogger.close();
        }
    }

    private void registerAPI() {
        getServer().getServicesManager().register(EconomyAPI.class, this.economyManager, this, ServicePriority.Normal);
    }

    private void registerCommands() {
        this.commandManager.registerPlayerCommand();
        this.commandManager.registerAdminCommand();
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
    }

    private void startAutosaveTask() {
        long interval = configManager.getAutosaveInterval() * 20L * 60L;
        this.autosaveTask = getServer().getScheduler().runTaskTimerAsynchronously(this, this.dataManager::saveDataAsync, interval, interval);
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