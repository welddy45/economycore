package ru.corearchitect.coreeconomy.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import ru.corearchitect.coreeconomy.CoreEconomy;

import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ScoreboardManager {

    private final CoreEconomy plugin;
    private final Set<UUID> scoreboardEnabledPlayers = new HashSet<>();
    private BukkitTask updateTask;

    public ScoreboardManager(CoreEconomy plugin) {
        this.plugin = plugin;
    }

    public void startUpdateTask() {
        this.updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllScoreboards, 0L, 40L);
    }

    public void cancelUpdateTask() {
        if (this.updateTask != null) {
            this.updateTask.cancel();
        }
    }

    public void toggleScoreboard(Player player) {
        UUID uuid = player.getUniqueId();
        boolean isCurrentlyEnabled = plugin.getDataManager().getScoreboardState(uuid);

        if (isCurrentlyEnabled) {
            scoreboardEnabledPlayers.remove(uuid);
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            plugin.getDataManager().setScoreboardState(uuid, false);
            player.sendMessage(plugin.getConfigManager().getPrefixedMessage("scoreboard.toggle-off"));
        } else {
            scoreboardEnabledPlayers.add(uuid);
            plugin.getDataManager().setScoreboardState(uuid, true);
            updateScoreboard(player);
            player.sendMessage(plugin.getConfigManager().getPrefixedMessage("scoreboard.toggle-on"));
        }
    }

    public void playerJoined(Player player) {
        if (plugin.getDataManager().getScoreboardState(player.getUniqueId())) {
            scoreboardEnabledPlayers.add(player.getUniqueId());
            updateScoreboard(player);
        }
    }

    public void playerQuit(Player player) {
        scoreboardEnabledPlayers.remove(player.getUniqueId());
    }

    private void updateAllScoreboards() {
        for (UUID uuid : scoreboardEnabledPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                updateScoreboard(player);
            }
        }
    }

    private void updateScoreboard(Player player) {
        Scoreboard board = player.getScoreboard();
        if (board == Bukkit.getScoreboardManager().getMainScoreboard()) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
        }

        Objective objective = board.getObjective("CoreEcoSidebar");
        if (objective == null) {
            Component title = LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfigManager().getScoreboardTitle());
            objective = board.registerNewObjective("CoreEcoSidebar", "dummy", title);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        final Scoreboard finalBoard = board;
        final Objective finalObjective = objective;

        plugin.getEconomyManager().getBalance(player.getUniqueId()).thenAcceptAsync(balance -> {
            String balanceText = balance.setScale(2, RoundingMode.HALF_UP).toPlainString();
            String symbol = plugin.getConfigManager().getCurrencySymbol();
            String lineText = plugin.getConfigManager().getScoreboardLineFormat()
                    .replace("{balance}", balanceText)
                    .replace("{symbol}", symbol);

            Bukkit.getScheduler().runTask(plugin, () -> {
                for (String entry : finalBoard.getEntries()) {
                    finalBoard.resetScores(entry);
                }
                finalObjective.getScore(lineText).setScore(1);
                player.setScoreboard(finalBoard);
            });
        });
    }
}