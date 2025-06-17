package ru.corearchitect.coreeconomy.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import ru.corearchitect.coreeconomy.CoreEconomy;
import ru.corearchitect.coreeconomy.manager.ConfigManager;
import ru.corearchitect.coreeconomy.manager.EconomyManager;
import ru.corearchitect.coreeconomy.manager.LeaderboardManager;
import ru.corearchitect.coreeconomy.manager.TransactionLogger;
import ru.corearchitect.coreeconomy.model.LeaderboardEntry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class PlayerEconomyCommand extends Command {

    private final CoreEconomy plugin;
    private final EconomyManager economyManager;
    private final ConfigManager configManager;
    private final TransactionLogger logger;
    private final LeaderboardManager leaderboardManager;

    public PlayerEconomyCommand(CoreEconomy plugin) {
        super(plugin.getConfigManager().getPlayerCommandName());
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
        this.configManager = plugin.getConfigManager();
        this.logger = plugin.getTransactionLogger();
        this.leaderboardManager = plugin.getLeaderboardManager();

        this.setAliases(plugin.getConfigManager().getPlayerCommandAliases());
        this.setDescription("Player economy commands.");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(configManager.getPrefixedMessage("command.player-usage").replace("{command}", commandLabel));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "balance":
            case "bal":
                handleBalance(sender, args);
                break;
            case "pay":
                handlePay(sender, args);
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
                sender.sendMessage(configManager.getPrefixedMessage("command.player-usage").replace("{command}", commandLabel));
                break;
        }
        return true;
    }

    private void handleBalance(CommandSender sender, String[] args) {
        if (!sender.hasPermission("coreeconomy.command.balance")) {
            sender.sendMessage(configManager.getPrefixedMessage("no-permission"));
            return;
        }

        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be run by a player.");
                return;
            }
            Player player = (Player) sender;
            economyManager.getBalance(player.getUniqueId()).thenAccept(balance -> {
                String formattedBalance = balance.setScale(2, RoundingMode.HALF_UP).toPlainString();
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
                    String formattedBalance = balance.setScale(2, RoundingMode.HALF_UP).toPlainString();
                    sender.sendMessage(configManager.getPrefixedMessage("balance-other")
                            .replace("{player}", target.getName())
                            .replace("{balance}", formattedBalance)
                            .replace("{symbol}", configManager.getCurrencySymbol()));
                });
            });
        } else {
            sender.sendMessage(configManager.getPrefixedMessage("command.player-usage").replace("{command}", this.getName()));
        }
    }

    private void handlePay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return;
        }
        if (!sender.hasPermission("coreeconomy.command.pay")) {
            sender.sendMessage(configManager.getPrefixedMessage("no-permission"));
            return;
        }
        if (args.length != 3) {
            sender.sendMessage(configManager.getPrefixedMessage("command.player-usage").replace("{command}", this.getName()));
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
            amount = new BigDecimal(args[2]);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage(configManager.getPrefixedMessage("invalid-amount"));
            return;
        }

        if (player.getUniqueId().equals(recipient.getUniqueId())) {
            player.sendMessage(configManager.getPrefixedMessage("cannot-pay-self"));
            return;
        }

        economyManager.isFrozen(player.getUniqueId()).thenCompose(isFrozen -> {
            if (isFrozen) {
                player.sendMessage(configManager.getPrefixedMessage("account-frozen"));
                return CompletableFuture.completedFuture(null);
            }
            return economyManager.isFrozen(recipient.getUniqueId());
        }).thenCompose(isRecipientFrozen -> {
            if (isRecipientFrozen == null) return CompletableFuture.completedFuture(null);
            if (isRecipientFrozen) {
                player.sendMessage(configManager.getPrefixedMessage("account-frozen-other").replace("{player}", recipient.getName()));
                return CompletableFuture.completedFuture(null);
            }
            return economyManager.withdraw(player.getUniqueId(), amount);
        }).thenAccept(success -> {
            if (success == null) return;
            if (success) {
                economyManager.deposit(recipient.getUniqueId(), amount);
                String formattedAmount = amount.setScale(2, RoundingMode.HALF_UP).toPlainString();

                logger.log(String.format("[PAY] %s (%s) -> %s (%s) | Amount: %s",
                        player.getName(), player.getUniqueId(), recipient.getName(), recipient.getUniqueId(), formattedAmount));

                player.sendMessage(configManager.getPrefixedMessage("payment-sent")
                        .replace("{amount}", formattedAmount)
                        .replace("{recipient}", recipient.getName())
                        .replace("{symbol}", configManager.getCurrencySymbol()));

                recipient.sendMessage(configManager.getPrefixedMessage("payment-received")
                        .replace("{amount}", formattedAmount)
                        .replace("{sender}", player.getName())
                        .replace("{symbol}", configManager.getCurrencySymbol()));
            } else {
                player.sendMessage(configManager.getPrefixedMessage("insufficient-funds"));
            }
        });
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
            String balance = entry.getBalance().setScale(2, RoundingMode.HALF_UP).toPlainString();
            String message = configManager.getMessage("balancetop-entry")
                    .replace("{rank}", String.valueOf(rank.getAndIncrement()))
                    .replace("{player}", entry.getName())
                    .replace("{balance}", balance)
                    .replace("{symbol}", configManager.getCurrencySymbol());
            sender.sendMessage(message);
        });

        sender.sendMessage(configManager.getMessage("balancetop-footer"));
    }

    private void handleScoreboardToggle(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return;
        }
        if (!sender.hasPermission("coreeconomy.command.scoreboardtoggle")) {
            sender.sendMessage(configManager.getPrefixedMessage("no-permission"));
            return;
        }
        plugin.getScoreboardManager().toggleScoreboard((Player) sender);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
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