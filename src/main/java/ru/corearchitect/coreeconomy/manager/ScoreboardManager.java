package ru.corearchitect.coreeconomy.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import ru.corearchitect.coreeconomy.CoreEconomy;
import ru.corearchitect.coreeconomy.util.NumberFormatter;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
        if (board == Bukkit.getScoreboardManager().getMainScoreboard() || board.getObjective("CoreEcoSidebar") == null) {
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
            String balanceText = NumberFormatter.format(balance);
            String symbol = plugin.getConfigManager().getCurrencySymbol();
            List<String> lines = plugin.getConfigManager().getScoreboardLines();

            Bukkit.getScheduler().runTask(plugin, () -> {
                for (String entry : finalBoard.getEntries()) {
                    finalBoard.resetScores(entry);
                }

                int score = lines.size();
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i)
                            .replace("{player}", player.getName())
                            .replace("{balance}", balanceText)
                            .replace("{symbol}", symbol);

                    String uniqueEntry = line + String.join("", Collections.nCopies(i, ChatColor.RESET.toString()));

                    finalObjective.getScore(uniqueEntry).setScore(score--);
                }

                player.setScoreboard(finalBoard);
            });
        });
    }
}