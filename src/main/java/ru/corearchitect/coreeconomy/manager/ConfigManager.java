package ru.corearchitect.coreeconomy.manager;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.corearchitect.coreeconomy.CoreEconomy;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

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

        String lang = mainConfig.getString("language", "en");
        File messagesFile = new File(plugin.getDataFolder(), "messages_" + lang + ".yml");

        if (!messagesFile.exists()) {
            plugin.saveResource("messages_" + lang + ".yml", false);
        }

        if (!messagesFile.exists()) {
            lang = "en";
            messagesFile = new File(plugin.getDataFolder(), "messages_" + lang + ".yml");
            if (!messagesFile.exists()) {
                plugin.saveResource("messages_en.yml", false);
            }
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        InputStream defaultStream = plugin.getResource("messages_" + lang + ".yml");
        if (defaultStream != null) {
            messagesConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream)));
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
        return mainConfig.getString("currency-symbol", "$");
    }

    public String getScoreboardTitle() {
        return getMessage("scoreboard.title");
    }

    public String getScoreboardLineFormat() {
        return getMessage("scoreboard.line");
    }

    public String getPlayerCommandName() {
        return mainConfig.getString("player-command.name", "eco");
    }

    public List<String> getPlayerCommandAliases() {
        return mainConfig.getStringList("player-command.aliases");
    }

    public String getAdminCommandName() {
        return mainConfig.getString("admin-command.name", "ecoadmin");
    }

    public List<String> getAdminCommandAliases() {
        return mainConfig.getStringList("admin-command.aliases");
    }

    public int getLeaderboardUpdateInterval() {
        return mainConfig.getInt("leaderboard.update-interval-minutes", 1);
    }

    public int getLeaderboardSize() {
        return mainConfig.getInt("leaderboard.size", 10);
    }

    public int getAutosaveInterval() {
        return mainConfig.getInt("data-storage.autosave-interval-minutes", 10);
    }

    public boolean isScoreboardEnabledByDefault() {
        return mainConfig.getBoolean("scoreboard.enabled-by-default", true);
    }

    public double getCommissionPercentage() {
        return mainConfig.getDouble("commission.percentage", 0.0);
    }
}