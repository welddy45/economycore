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
import ru.corearchitect.coreeconomy.manager.DataManager;
import ru.corearchitect.coreeconomy.manager.EconomyManager;
import ru.corearchitect.coreeconomy.manager.TransactionLogger;
import ru.corearchitect.coreeconomy.model.PendingAdminCommand;
import ru.corearchitect.coreeconomy.model.TransactionRecord;
import ru.corearchitect.coreeconomy.model.TransactionType;
import ru.corearchitect.coreeconomy.util.NumberFormatter;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyAdminCommand implements CommandExecutor, TabCompleter {

    private final CoreEconomy plugin;
    private final EconomyManager economyManager;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final TransactionLogger logger;
    private final Map<UUID, PendingAdminCommand> pendingCommands = new ConcurrentHashMap<>();
    private final DateTimeFormatter historyDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public EconomyAdminCommand(CoreEconomy plugin) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
        this.configManager = plugin.getConfigManager();
        this.dataManager = plugin.getDataManager();
        this.logger = plugin.getTransactionLogger();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("coreeconomy.command.admin")) {
            sender.sendMessage(configManager.getPrefixedMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(configManager.getPrefixedMessage("command.admin-usage").replace("{command}", label));
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
            case "confirm":
                handleConfirm(sender);
                return true;
            case "history":
                handleHistory(sender, args);
                return true;
        }

        if (args.length < 2) {
            sender.sendMessage(configManager.getPrefixedMessage("command.admin-usage").replace("{command}", label));
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
                case "remove":
                    handleDangerousBalanceModification(sender, subCommand, target, args, label);
                    break;
                case "add":
                    handleBalanceModification(sender, target, args, label);
                    break;
                case "freeze":
                    handleFreeze(sender, target, true);
                    break;
                case "unfreeze":
                    handleFreeze(sender, target, false);
                    break;
                default:
                    sender.sendMessage(configManager.getPrefixedMessage("command.admin-usage").replace("{command}", label));
                    break;
            }
        });

        return true;
    }

    private void handleHistory(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(configManager.getPrefixedMessage("command.history-usage"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        int page = 1;

        if (args.length > 2) {
            try {
                page = Integer.parseInt(args[2]);
                if (page < 1) page = 1;
            } catch (NumberFormatException e) {
                sender.sendMessage(configManager.getPrefixedMessage("invalid-amount"));
                return;
            }
        }

        final int finalPage = page;
        dataManager.countPlayerHistory(target.getUniqueId()).thenAccept(totalEntries -> {
            if (totalEntries == 0) {
                sender.sendMessage(configManager.getPrefixedMessage("history.no-entries"));
                return;
            }

            int entriesPerPage = configManager.getHistoryEntriesPerPage();
            int maxPages = (int) Math.ceil((double) totalEntries / entriesPerPage);

            if (finalPage > maxPages) {
                sender.sendMessage(configManager.getPrefixedMessage("history.no-entries"));
                return;
            }

            dataManager.getPlayerHistory(target.getUniqueId(), finalPage).thenAccept(history -> {
                sender.sendMessage(configManager.getMessage("history.header")
                        .replace("{player}", target.getName())
                        .replace("{page}", String.valueOf(finalPage))
                        .replace("{max_pages}", String.valueOf(maxPages)));

                for (TransactionRecord record : history) {
                    String formattedLine = formatHistoryLine(record, target.getUniqueId());
                    sender.sendMessage(Component.text(formattedLine));
                }

                sender.sendMessage(createPaginationComponent(target.getName(), finalPage, maxPages));
            });
        });
    }

    private TextComponent createPaginationComponent(String targetName, int currentPage, int maxPages) {
        TextComponent.Builder builder = Component.text();
        builder.append(Component.text(configManager.getMessage("history.footer")));
        builder.append(Component.newline());

        if (currentPage > 1) {
            builder.append(Component.text(configManager.getMessage("history.pagination.previous"))
                    .clickEvent(ClickEvent.runCommand("/eduadmin history " + targetName + " " + (currentPage - 1)))
                    .hoverEvent(HoverEvent.showText(Component.text("Перейти на страницу " + (currentPage - 1)))));
        } else {
            builder.append(Component.text(configManager.getMessage("history.pagination.no-previous")));
        }

        builder.append(Component.text("  "));

        if (currentPage < maxPages) {
            builder.append(Component.text(configManager.getMessage("history.pagination.next"))
                    .clickEvent(ClickEvent.runCommand("/eduadmin history " + targetName + " " + (currentPage + 1)))
                    .hoverEvent(HoverEvent.showText(Component.text("Перейти на страницу " + (currentPage + 1)))));
        } else {
            builder.append(Component.text(configManager.getMessage("history.pagination.no-next")));
        }

        return builder.build();
    }

    private String formatHistoryLine(TransactionRecord record, UUID viewerUuid) {
        String date = record.getTimestamp().format(historyDateFormatter);
        String symbol = configManager.getCurrencySymbol();
        String amount = NumberFormatter.format(record.getAmount());
        String commission = NumberFormatter.format(record.getCommission());

        String formatted;
        switch (record.getTransactionType()) {
            case PAY:
                boolean isSender = record.getInitiatorName().equals(Bukkit.getOfflinePlayer(viewerUuid).getName());
                if (isSender) {
                    formatted = configManager.getMessage("history.entry-pay-sent")
                            .replace("{recipient}", record.getTargetName())
                            .replace("{amount}", amount)
                            .replace("{commission}", commission);
                } else {
                    formatted = configManager.getMessage("history.entry-pay-received")
                            .replace("{sender}", record.getInitiatorName())
                            .replace("{amount}", amount);
                }
                break;
            case ADMIN_ADD:
                formatted = configManager.getMessage("history.entry-admin-add")
                        .replace("{admin}", record.getInitiatorName())
                        .replace("{amount}", amount);
                break;
            case ADMIN_REMOVE:
                formatted = configManager.getMessage("history.entry-admin-remove")
                        .replace("{admin}", record.getInitiatorName())
                        .replace("{amount}", amount);
                break;
            case ADMIN_SET:
                formatted = configManager.getMessage("history.entry-admin-set")
                        .replace("{admin}", record.getInitiatorName())
                        .replace("{amount}", amount);
                break;
            default:
                formatted = "Unknown transaction type";
        }

        return configManager.getMessage("history.entry-format")
                .replace("{date}", date)
                .replace("{details}", formatted)
                .replace("{symbol}", symbol);
    }

    private void handleDangerousBalanceModification(CommandSender sender, String type, OfflinePlayer target, String[] args, String commandLabel) {
        if (args.length != 3) {
            sender.sendMessage(configManager.getPrefixedMessage("command.admin-usage").replace("{command}", commandLabel));
            return;
        }

        UUID senderId = (sender instanceof Player) ? ((Player) sender).getUniqueId() : new UUID(0, 0);
        pendingCommands.put(senderId, new PendingAdminCommand(sender, target, type, args));

        sender.sendMessage(configManager.getPrefixedMessage("admin.confirm-required")
                .replace("{command}", "eduadmin confirm"));
    }

    private void handleConfirm(CommandSender sender) {
        UUID senderId = (sender instanceof Player) ? ((Player) sender).getUniqueId() : new UUID(0, 0);
        PendingAdminCommand pending = pendingCommands.get(senderId);

        if (pending == null) {
            sender.sendMessage(configManager.getPrefixedMessage("admin.no-pending-command"));
            return;
        }

        if (pending.isExpired()) {
            sender.sendMessage(configManager.getPrefixedMessage("admin.pending-command-expired"));
            pendingCommands.remove(senderId);
            return;
        }

        pendingCommands.remove(senderId);
        String[] args = pending.getArgs();
        String type = pending.getType();
        OfflinePlayer target = pending.getTarget();

        BigDecimal amount;
        try {
            amount = new BigDecimal(args[2].replace(',', '.'));
            if (amount.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(configManager.getPrefixedMessage("invalid-amount"));
            return;
        }

        String messagePath;
        TransactionType transactionType;

        switch (type) {
            case "set":
                economyManager.setBalance(target.getUniqueId(), amount);
                messagePath = "admin.balance-set";
                transactionType = TransactionType.ADMIN_SET;
                break;
            case "remove":
                economyManager.withdraw(target.getUniqueId(), amount);
                messagePath = "admin.balance-removed";
                transactionType = TransactionType.ADMIN_REMOVE;
                break;
            default:
                return;
        }

        logAndNotify(sender, target, amount, messagePath, transactionType);
    }

    private void handleBalanceModification(CommandSender sender, OfflinePlayer target, String[] args, String commandLabel) {
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

        economyManager.deposit(target.getUniqueId(), amount);
        logAndNotify(sender, target, amount, "admin.balance-added", TransactionType.ADMIN_ADD);
    }

    private void logAndNotify(CommandSender sender, OfflinePlayer target, BigDecimal amount, String messagePath, TransactionType transactionType) {
        String formattedAmount = NumberFormatter.format(amount);
        String logType = transactionType.name();

        logger.log(String.format("[%s] Admin: %s | Target: %s (%s) | Amount: %s",
                logType, sender.getName(), target.getName(), target.getUniqueId(), formattedAmount));

        dataManager.logTransaction(
                new TransactionRecord(
                        sender.getName(),
                        target.getName(),
                        transactionType,
                        amount,
                        BigDecimal.ZERO
                )
        );

        sender.sendMessage(configManager.getPrefixedMessage(messagePath)
                .replace("{player}", target.getName())
                .replace("{amount}", formattedAmount)
                .replace("{symbol}", configManager.getCurrencySymbol()));
    }


    private void handleReload(CommandSender sender) {
        configManager.reload();
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

    private void handleFreeze(CommandSender sender, OfflinePlayer target, boolean freeze) {
        economyManager.setFrozen(target.getUniqueId(), freeze);
        String messagePath = freeze ? "admin.account-frozen-success" : "admin.account-unfrozen-success";
        String logType = freeze ? "FREEZE" : "UNFREEZE";

        logger.log(String.format("[%s] Admin: %s | Target: %s (%s)",
                logType, sender.getName(), target.getName(), target.getUniqueId()));

        sender.sendMessage(configManager.getPrefixedMessage(messagePath).replace("{player}", target.getName()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("coreeconomy.command.admin")) return Collections.emptyList();
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], Arrays.asList("set", "add", "remove", "freeze", "unfreeze", "reload", "total", "confirm", "history"), new ArrayList<>());
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("reload") && !args[0].equalsIgnoreCase("total") && !args[0].equalsIgnoreCase("confirm")) {
            List<String> playerNames = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> playerNames.add(p.getName()));
            return StringUtil.copyPartialMatches(args[1], playerNames, new ArrayList<>());
        }
        return Collections.emptyList();
    }
}