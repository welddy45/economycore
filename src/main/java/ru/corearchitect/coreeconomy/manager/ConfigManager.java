package ru.corearchitect.coreeconomy.manager;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.corearchitect.coreeconomy.CoreEconomy;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigManager {

    private final CoreEconomy plugin;
    private FileConfiguration mainConfig;
    private FileConfiguration messagesConfig;

    public ConfigManager(CoreEconomy plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    public void reload() {
        plugin.reloadConfig();
        loadConfigs();
    }

    private void loadConfigs() {
        plugin.saveDefaultConfig();
        mainConfig = plugin.getConfig();

        String lang = mainConfig.getString("language", "ru");
        File messagesFile = new File(plugin.getDataFolder(), "messages_" + lang + ".yml");

        if (!messagesFile.exists()) {
            plugin.saveResource("messages_" + lang + ".yml", false);
        }

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(messagesFile), StandardCharsets.UTF_8)) {
            messagesConfig = YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not load messages file with UTF-8 encoding: " + messagesFile.getName());
            e.printStackTrace();
            messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        }

        try (InputStream defaultStream = plugin.getResource("messages_" + lang + ".yml")) {
            if (defaultStream != null) {
                messagesConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not load default messages resource with UTF-8 encoding.");
            e.printStackTrace();
        }
    }

    public String getMessage(String path) {
        String message = messagesConfig.getString(path, "&cMessage not found: " + path);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getPrefixedMessage(String path) {
        String prefix = messagesConfig.getString("prefix", "");
        String message = messagesConfig.getString(path, "&cMessage not found: " + path);
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }

    public String getCurrencySymbol() {
        return mainConfig.getString("currency-symbol", "₽");
    }

    public String getScoreboardTitle() {
        return getMessage("scoreboard.title");
    }

    public List<String> getScoreboardLines() {
        List<String> lines = messagesConfig.getStringList("scoreboard.lines");
        if (lines.isEmpty()) {
            return Collections.singletonList("&cScoreboard format not found!");
        }
        return lines.stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());
    }

    public String getPlayerCommandName() {
        return mainConfig.getString("player-command.name", "eco");
    }

    public String getAdminCommandName() {
        return mainConfig.getString("admin-command.name", "ecoadmin");
    }

    public int getLeaderboardUpdateInterval() {
        return mainConfig.getInt("leaderboard.update-interval-minutes", 1);
    }

    public int getLeaderboardSize() {
        return mainConfig.getInt("leaderboard.size", 10);
    }

    public int getAutosaveInterval() {
        return mainConfig.getInt("data-storage.autosave-interval-minutes", 5);
    }

    public boolean isScoreboardEnabledByDefault() {
        return mainConfig.getBoolean("scoreboard.enabled-by-default", true);
    }

    public double getCommissionPercentage() {
        return mainConfig.getDouble("commission.percentage", 0.0);
    }

    public String getStorageType() {
        return mainConfig.getString("data-storage.type", "SQLITE");
    }

    public String getSQLiteFilename() {
        return mainConfig.getString("data-storage.sqlite.filename", "database.db");
    }

    public boolean isBackupEnabled() {
        return mainConfig.getBoolean("backups.enabled", true);
    }

    public int getBackupIntervalHours() {
        return mainConfig.getInt("backups.interval-hours", 24);
    }

    public int getMaxBackupFiles() {
        return mainConfig.getInt("backups.max-files", 7);
    }

    public int getHistoryEntriesPerPage() {
        return mainConfig.getInt("history.entries-per-page", 10);
    }
}
