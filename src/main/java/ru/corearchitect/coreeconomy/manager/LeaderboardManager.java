package ru.corearchitect.coreeconomy.manager;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import ru.corearchitect.coreeconomy.CoreEconomy;
import ru.corearchitect.coreeconomy.model.LeaderboardEntry;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class LeaderboardManager {

    private final CoreEconomy plugin;
    private final List<LeaderboardEntry> topPlayers = new CopyOnWriteArrayList<>();
    private BukkitTask updateTask;

    public LeaderboardManager(CoreEconomy plugin) {
        this.plugin = plugin;
    }

    public void startUpdateTask() {
        long interval = plugin.getConfigManager().getLeaderboardUpdateInterval() * 20L * 60L;
        this.updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateLeaderboard();
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 10, interval);
    }

    public void cancelUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
        }
    }

    public void updateLeaderboard() {
        int leaderboardSize = plugin.getConfigManager().getLeaderboardSize();
        List<LeaderboardEntry> sortedList = plugin.getDataManager().getAllBalances().entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getValue(), Comparator.reverseOrder()))
                .limit(leaderboardSize)
                .map(entry -> {
                    UUID uuid = entry.getKey();
                    BigDecimal balance = entry.getValue();
                    OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                    String name = player.getName() != null ? player.getName() : "Unknown";
                    return new LeaderboardEntry(uuid, name, balance);
                })
                .collect(Collectors.toList());

        topPlayers.clear();
        topPlayers.addAll(sortedList);
    }

    public List<LeaderboardEntry> getTopPlayers() {
        return topPlayers;
    }

    public LeaderboardEntry getEntry(int rank) {
        if (rank > 0 && rank <= topPlayers.size()) {
            return topPlayers.get(rank - 1);
        }
        return null;
    }
}