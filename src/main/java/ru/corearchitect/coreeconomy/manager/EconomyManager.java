package ru.corearchitect.coreeconomy.manager;

import org.bukkit.Bukkit;
import ru.corearchitect.coreeconomy.CoreEconomy;
import ru.corearchitect.coreeconomy.api.EconomyAPI;
import ru.corearchitect.coreeconomy.model.TransactionResult;
import ru.corearchitect.coreeconomy.util.NumberFormatter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EconomyManager implements EconomyAPI {

    private final CoreEconomy plugin;
    private final DataManager dataManager;

    public EconomyManager(CoreEconomy plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
    }

    @Override
    public CompletableFuture<BigDecimal> getBalance(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> dataManager.getBalance(playerUUID));
    }

    @Override
    public CompletableFuture<Boolean> hasAccount(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> dataManager.hasAccount(playerUUID));
    }

    @Override
    public CompletableFuture<Boolean> withdraw(UUID playerUUID, BigDecimal amount) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (dataManager) {
                if (amount.compareTo(BigDecimal.ZERO) < 0) {
                    return false;
                }
                BigDecimal currentBalance = dataManager.getBalance(playerUUID);
                if (currentBalance.compareTo(amount) >= 0) {
                    dataManager.setBalance(playerUUID, currentBalance.subtract(amount));
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> deposit(UUID playerUUID, BigDecimal amount) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (dataManager) {
                if (amount.compareTo(BigDecimal.ZERO) < 0) {
                    return false;
                }
                BigDecimal currentBalance = dataManager.getBalance(playerUUID);
                dataManager.setBalance(playerUUID, currentBalance.add(amount));
                return true;
            }
        });
    }

    @Override
    public CompletableFuture<Void> setBalance(UUID playerUUID, BigDecimal amount) {
        return CompletableFuture.runAsync(() -> {
            synchronized (dataManager) {
                if (amount.compareTo(BigDecimal.ZERO) >= 0) {
                    dataManager.setBalance(playerUUID, amount);
                }
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> isFrozen(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> dataManager.isFrozen(playerUUID));
    }

    @Override
    public CompletableFuture<Void> setFrozen(UUID playerUUID, boolean frozen) {
        return CompletableFuture.runAsync(() -> dataManager.setFrozen(playerUUID, frozen));
    }

    @Override
    public String getCurrencySymbol() {
        return plugin.getConfigManager().getCurrencySymbol();
    }

    @Override
    public CompletableFuture<BigDecimal> getTotalServerBalance() {
        return CompletableFuture.supplyAsync(dataManager::calculateTotalBalance);
    }

    @Override
    public CompletableFuture<BigDecimal> getTotalCommission() {
        return CompletableFuture.supplyAsync(dataManager::getTotalCommission);
    }

    @Override
    public String format(BigDecimal amount) {
        return NumberFormatter.format(amount);
    }

    @Override
    public CompletableFuture<TransactionResult> transfer(UUID from, UUID to, BigDecimal amount) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized(dataManager) {
                if (from.equals(to)) return TransactionResult.CANNOT_PAY_SELF;
                if (dataManager.isFrozen(from)) return TransactionResult.SENDER_FROZEN;
                if (dataManager.isFrozen(to)) return TransactionResult.RECIPIENT_FROZEN;

                double commissionPercentage = plugin.getConfigManager().getCommissionPercentage();
                BigDecimal commissionAmount = amount.multiply(BigDecimal.valueOf(commissionPercentage / 100.0)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal totalCost = amount.add(commissionAmount);
                BigDecimal senderBalance = dataManager.getBalance(from);

                if (senderBalance.compareTo(totalCost) < 0) {
                    return TransactionResult.INSUFFICIENT_FUNDS;
                }

                dataManager.setBalance(from, senderBalance.subtract(totalCost));
                BigDecimal recipientBalance = dataManager.getBalance(to);
                dataManager.setBalance(to, recipientBalance.add(amount));
                addCommission(commissionAmount);

                logTransaction(from, to, amount, commissionAmount, totalCost);

                return TransactionResult.SUCCESS;
            }
        });
    }

    private void addCommission(BigDecimal amount) {
        dataManager.addCommission(amount);
    }

    private void logTransaction(UUID from, UUID to, BigDecimal amount, BigDecimal commission, BigDecimal total) {
        String fromName = Bukkit.getOfflinePlayer(from).getName();
        String toName = Bukkit.getOfflinePlayer(to).getName();

        plugin.getTransactionLogger().log(String.format("[PAY] %s -> %s | Amount: %s | Commission: %s | Total: %s",
                fromName, toName, format(amount), format(commission), format(total)));
    }
}