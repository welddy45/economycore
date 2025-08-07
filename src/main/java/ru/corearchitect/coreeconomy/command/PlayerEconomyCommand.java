package ru.corearchitect.coreeconomy.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import ru.corearchitect.coreeconomy.CoreEconomy;
import ru.corearchitect.coreeconomy.manager.ConfigManager;
import ru.corearchitect.coreeconomy.manager.EconomyManager;
import ru.corearchitect.coreeconomy.manager.LeaderboardManager;
import ru.corearchitect.coreeconomy.model.LeaderboardEntry;
import ru.corearchitect.coreeconomy.model.TransactionResult;
import ru.corearchitect.coreeconomy.util.NumberFormatter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PlayerEconomyCommand implements CommandExecutor, TabCompleter {

    private final CoreEconomy plugin;
    private final EconomyManager economyManager;
    private final ConfigManager configManager;
    private final LeaderboardManager leaderboardManager;

    public PlayerEconomyCommand(CoreEconomy plugin) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
        this.configManager = plugin.getConfigManager();
        this.leaderboardManager = plugin.getLeaderboardManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(configManager.getPrefixedMessage("command.player-usage").replace("{command}", label));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "balance":
            case "bal":
                handleBalance(sender, args, label);
                break;
            case "pay":
                handlePay(sender, args, label);
                break;
            case "balancetop":
            case "baltop":
                handleBalanceTop(sender);
                break;
            case "scoreboardtoggle":
            case "sbtoggle":
                handleScoreboardToggle(sender);
                break;
            default:
                sender.sendMessage(configManager.getPrefixedMessage("command.player-usage").replace("{command}", label));
                break;
        }
        return true;
    }

    private void handleBalance(CommandSender sender, String[] args, String commandLabel) {
        if (!sender.hasPermission("coreeconomy.command.balance")) {
            sender.sendMessage(configManager.getPrefixedMessage("no-permission"));
            return;
        }

        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(configManager.getPrefixedMessage("command.player-only"));
                return;
            }
            Player player = (Player) sender;
            economyManager.getBalance(player.getUniqueId()).thenAccept(balance -> {
                String formattedBalance = NumberFormatter.format(balance);
                player.sendMessage(configManager.getPrefixedMessage("balance")
                        .replace("{balance}", formattedBalance)
                        .replace("{symbol}", configManager.getCurrencySymbol()));
            });
        } else if (args.length == 2) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            economyManager.hasAccount(target.getUniqueId()).thenAccept(hasAccount -> {
                if (!hasAccount) {
                    sender.sendMessage(configManager.getPrefixedMessage("player-does-not-exist"));
                    return;
                }
                economyManager.getBalance(target.getUniqueId()).thenAccept(balance -> {
                    String formattedBalance = NumberFormatter.format(balance);
                    sender.sendMessage(configManager.getPrefixedMessage("balance-other")
                            .replace("{player}", target.getName())
                            .replace("{balance}", formattedBalance)
                            .replace("{symbol}", configManager.getCurrencySymbol()));
                });
            });
        } else {
            sender.sendMessage(configManager.getPrefixedMessage("command.player-usage").replace("{command}", commandLabel));
        }
    }

    private void handlePay(CommandSender sender, String[] args, String commandLabel) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(configManager.getPrefixedMessage("command.player-only"));
            return;
        }
        if (!sender.hasPermission("coreeconomy.command.pay")) {
            sender.sendMessage(configManager.getPrefixedMessage("no-permission"));
            return;
        }
        if (args.length != 3) {
            sender.sendMessage(configManager.getPrefixedMessage("command.player-usage").replace("{command}", commandLabel));
            return;
        }

        Player player = (Player) sender;
        Player recipient = Bukkit.getPlayer(args[1]);

        if (recipient == null) {
            player.sendMessage(configManager.getPrefixedMessage("player-not-found"));
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(args[2].replace(',', '.'));
            if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage(configManager.getPrefixedMessage("invalid-amount"));
            return;
        }

        final BigDecimal finalAmount = amount;
        economyManager.transfer(player.getUniqueId(), recipient.getUniqueId(), amount).thenAccept(result -> {
            handleTransactionResult(player, recipient, finalAmount, result);
        });
    }

    private void handleTransactionResult(Player sender, Player recipient, BigDecimal amount, TransactionResult result) {
        String formattedAmount = NumberFormatter.format(amount);
        switch (result) {
            case SUCCESS:
                sender.sendMessage(configManager.getPrefixedMessage("payment-sent")
                        .replace("{amount}", formattedAmount)
                        .replace("{recipient}", recipient.getName())
                        .replace("{symbol}", configManager.getCurrencySymbol()));

                recipient.sendMessage(configManager.getPrefixedMessage("payment-received")
                        .replace("{amount}", formattedAmount)
                        .replace("{sender}", sender.getName())
                        .replace("{symbol}", configManager.getCurrencySymbol()));
                break;
            case INSUFFICIENT_FUNDS:
                sender.sendMessage(configManager.getPrefixedMessage("insufficient-funds"));
                break;
            case SENDER_FROZEN:
                sender.sendMessage(configManager.getPrefixedMessage("account-frozen"));
                break;
            case RECIPIENT_FROZEN:
                sender.sendMessage(configManager.getPrefixedMessage("account-frozen-other").replace("{player}", recipient.getName()));
                break;
            case CANNOT_PAY_SELF:
                sender.sendMessage(configManager.getPrefixedMessage("cannot-pay-self"));
                break;
            case CANCELLED_BY_EVENT:
                sender.sendMessage(configManager.getPrefixedMessage("transaction-cancelled"));
                break;
        }
    }

    private void handleBalanceTop(CommandSender sender) {
        if (!sender.hasPermission("coreeconomy.command.balancetop")) {
            sender.sendMessage(configManager.getPrefixedMessage("no-permission"));
            return;
        }

        List<LeaderboardEntry> topPlayers = leaderboardManager.getTopPlayers();

        if (topPlayers.isEmpty()) {
            sender.sendMessage(configManager.getPrefixedMessage("balancetop-empty"));
            return;
        }

        sender.sendMessage(configManager.getMessage("balancetop-header")
                .replace("{size}", String.valueOf(configManager.getLeaderboardSize())));

        AtomicInteger rank = new AtomicInteger(1);
        topPlayers.forEach(entry -> {
            String formattedBalance = NumberFormatter.format(entry.getBalance());
            TextComponent message = Component.text(configManager.getMessage("balancetop-entry")
                            .replace("{rank}", String.valueOf(rank.getAndIncrement()))
                            .replace("{player}", entry.getName())
                            .replace("{balance}", formattedBalance)
                            .replace("{symbol}", configManager.getCurrencySymbol()))
                    .hoverEvent(HoverEvent.showText(Component.text("Нажмите, чтобы посмотреть баланс")))
                    .clickEvent(ClickEvent.runCommand("/edu balance " + entry.getName()));
            sender.sendMessage(message);
        });

        sender.sendMessage(configManager.getMessage("balancetop-footer"));
    }

    private void handleScoreboardToggle(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(configManager.getPrefixedMessage("command.player-only"));
            return;
        }
        if (!sender.hasPermission("coreeconomy.command.scoreboardtoggle")) {
            sender.sendMessage(configManager.getPrefixedMessage("no-permission"));
            return;
        }
        plugin.getScoreboardManager().toggleScoreboard((Player) sender);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0],
                    Arrays.asList("balance", "pay", "baltop", "sbtoggle"),
                    new ArrayList<>());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("pay") || args[0].equalsIgnoreCase("balance"))) {
            List<String> playerNames = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> playerNames.add(p.getName()));
            return StringUtil.copyPartialMatches(args[1], playerNames, new ArrayList<>());
        }
        return Collections.emptyList();
    }
}