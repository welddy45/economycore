package ru.corearchitect.coreeconomy.integrations;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import ru.corearchitect.coreeconomy.CoreEconomy;
import ru.corearchitect.coreeconomy.manager.ConfigManager;
import ru.corearchitect.coreeconomy.manager.EconomyManager;
import ru.corearchitect.coreeconomy.manager.LeaderboardManager;
import ru.corearchitect.coreeconomy.model.LeaderboardEntry;

import java.math.RoundingMode;

public class EconomyPlaceholderExpansion extends PlaceholderExpansion {

    private final CoreEconomy plugin;
    private final EconomyManager economyManager;
    private final ConfigManager configManager;
    private final LeaderboardManager leaderboardManager;

    public EconomyPlaceholderExpansion(CoreEconomy plugin) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
        this.configManager = plugin.getConfigManager();
        this.leaderboardManager = plugin.getLeaderboardManager();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "coreeconomy";
    }

    @Override
    public @NotNull String getAuthor() {
        return "CoreArchitect";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.startsWith("top_")) {
            return handleTopPlaceholder(params);
        }

        if (player == null) {
            return "";
        }

        switch (params.toLowerCase()) {
            case "balance":
                return economyManager.getBalance(player.getUniqueId()).join().toPlainString();
            case "balance_formatted":
                return economyManager.getBalance(player.getUniqueId()).join().setScale(2, RoundingMode.HALF_UP).toPlainString();
            case "balance_formatted_symbol":
                String balance = economyManager.getBalance(player.getUniqueId()).join().setScale(2, RoundingMode.HALF_UP).toPlainString();
                return balance + configManager.getCurrencySymbol();
            default:
                return null;
        }
    }

    private String handleTopPlaceholder(String params) {
        String[] parts = params.split("_");
        if (parts.length != 3) {
            return null;
        }

        int rank;
        try {
            rank = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }

        if (rank < 1 || rank > 5) {
            return null;
        }

        LeaderboardEntry entry = leaderboardManager.getEntry(rank);
        if (entry == null) {
            return "—";
        }

        String type = parts[2];
        switch (type.toLowerCase()) {
            case "name":
                return entry.getName();
            case "balance":
                return entry.getBalance().toPlainString();
            case "balance_formatted":
                return entry.getBalance().setScale(2, RoundingMode.HALF_UP).toPlainString();
            default:
                return null;
        }
    }
}