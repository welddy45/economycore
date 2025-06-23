package ru.corearchitect.coreeconomy.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;
import ru.corearchitect.coreeconomy.CoreEconomy;
import ru.corearchitect.coreeconomy.manager.ConfigManager;
import ru.corearchitect.coreeconomy.manager.DataManager;
import ru.corearchitect.coreeconomy.manager.EconomyManager;
import ru.corearchitect.coreeconomy.manager.TransactionLogger;
import ru.corearchitect.coreeconomy.util.NumberFormatter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EconomyAdminCommand extends Command {

    private final CoreEconomy plugin;
    private final EconomyManager economyManager;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final TransactionLogger logger;

    public EconomyAdminCommand(CoreEconomy plugin) {
        super(plugin.getConfigManager().getAdminCommandName());
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
        this.configManager = plugin.getConfigManager();
        this.dataManager = plugin.getDataManager();
        this.logger = plugin.getTransactionLogger();

        this.setAliases(plugin.getConfigManager().getAdminCommandAliases());
        this.setPermission("coreeconomy.command.admin");
        this.setPermissionMessage(configManager.getPrefixedMessage("no-permission"));
        this.setDescription("Admin economy commands.");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!testPermission(sender)) {
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(configManager.getPrefixedMessage("command.admin-usage").replace("{command}", commandLabel));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                handleReload(sender);
                return true;
            case "total":
                handleTotal(sender);
                return true;
        }

        if (args.length < 2) {
            sender.sendMessage(configManager.getPrefixedMessage("command.admin-usage").replace("{command}", commandLabel));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

        economyManager.hasAccount(target.getUniqueId()).thenAccept(hasAccount -> {
            if (!hasAccount && !target.isOnline()) {
                sender.sendMessage(configManager.getPrefixedMessage("player-does-not-exist"));
                return;
            }
            if (!hasAccount && target.isOnline()) {
                dataManager.createAccount(target.getUniqueId());
            }

            switch (subCommand) {
                case "set":
                case "add":
                case "remove":
                    handleBalanceModification(sender, subCommand, target, args, commandLabel);
                    break;
                case "freeze":
                    handleFreeze(sender, target, true);
                    break;
                case "unfreeze":
                    handleFreeze(sender, target, false);
                    break;
                default:
                    sender.sendMessage(configManager.getPrefixedMessage("command.admin-usage").replace("{command}", commandLabel));
                    break;
            }
        });

        return true;
    }

    private void handleReload(CommandSender sender) {
        configManager.reload();
        dataManager.loadData();
        plugin.getLeaderboardManager().updateLeaderboard();
        sender.sendMessage(configManager.getPrefixedMessage("admin.reload-success"));
    }

    private void handleTotal(CommandSender sender) {
        economyManager.getTotalServerBalance().thenAccept(totalBalance -> {
            String formattedTotal = NumberFormatter.format(totalBalance);
            sender.sendMessage(configManager.getPrefixedMessage("admin.total-balance")
                    .replace("{total}", formattedTotal)
                    .replace("{symbol}", configManager.getCurrencySymbol()));
        });
    }

    private void handleBalanceModification(CommandSender sender, String type, OfflinePlayer target, String[] args, String commandLabel) {
        if (args.length != 3) {
            sender.sendMessage(configManager.getPrefixedMessage("command.admin-usage").replace("{command}", commandLabel));
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(args[2].replace(',', '.'));
            if (amount.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(configManager.getPrefixedMessage("invalid-amount"));
            return;
        }

        String messagePath;
        String logType;
        switch (type) {
            case "set":
                economyManager.setBalance(target.getUniqueId(), amount);
                messagePath = "admin.balance-set";
                logType = "SET";
                break;
            case "add":
                economyManager.deposit(target.getUniqueId(), amount);
                messagePath = "admin.balance-added";
                logType = "ADD";
                break;
            case "remove":
                economyManager.withdraw(target.getUniqueId(), amount);
                messagePath = "admin.balance-removed";
                logType = "REMOVE";
                break;
            default:
                return;
        }

        String formattedAmount = NumberFormatter.format(amount);

        logger.log(String.format("[%s] Admin: %s | Target: %s (%s) | Amount: %s",
                logType, sender.getName(), target.getName(), target.getUniqueId(), formattedAmount));

        sender.sendMessage(configManager.getPrefixedMessage(messagePath)
                .replace("{player}", target.getName())
                .replace("{amount}", formattedAmount)
                .replace("{symbol}", configManager.getCurrencySymbol()));
    }

    private void handleFreeze(CommandSender sender, OfflinePlayer target, boolean freeze) {
        economyManager.setFrozen(target.getUniqueId(), freeze);
        String messagePath = freeze ? "admin.account-frozen-success" : "admin.account-unfrozen-success";
        String logType = freeze ? "FREEZE" : "UNFREEZE";

        logger.log(String.format("[%s] Admin: %s | Target: %s (%s)",
                logType, sender.getName(), target.getName(), target.getUniqueId()));

        sender.sendMessage(configManager.getPrefixedMessage(messagePath).replace("{player}", target.getName()));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (!sender.hasPermission(this.getPermission())) return Collections.emptyList();
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], Arrays.asList("set", "add", "remove", "freeze", "unfreeze", "reload", "total"), new ArrayList<>());
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("reload") && !args[0].equalsIgnoreCase("total")) {
            List<String> playerNames = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> playerNames.add(p.getName()));
            return StringUtil.copyPartialMatches(args[1], playerNames, new ArrayList<>());
        }
        return Collections.emptyList();
    }
}