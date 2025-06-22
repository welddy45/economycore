package ru.corearchitect.coreeconomy.manager;

import ru.corearchitect.coreeconomy.CoreEconomy;
import ru.corearchitect.coreeconomy.api.EconomyAPI;

import java.math.BigDecimal;
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

    public CompletableFuture<Void> addCommission(BigDecimal amount) {
        return CompletableFuture.runAsync(() -> dataManager.addCommission(amount));
    }
}