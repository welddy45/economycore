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
    public synchronized CompletableFuture<BigDecimal> getBalance(UUID playerUUID) {
        return CompletableFuture.completedFuture(dataManager.getBalance(playerUUID));
    }

    @Override
    public CompletableFuture<Boolean> hasAccount(UUID playerUUID) {
        return CompletableFuture.completedFuture(dataManager.hasAccount(playerUUID));
    }

    @Override
    public synchronized CompletableFuture<Boolean> withdraw(UUID playerUUID, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return CompletableFuture.completedFuture(false);
        }
        BigDecimal currentBalance = dataManager.getBalance(playerUUID);
        if (currentBalance.compareTo(amount) >= 0) {
            dataManager.setBalance(playerUUID, currentBalance.subtract(amount));
            return CompletableFuture.completedFuture(true);
        }
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public synchronized CompletableFuture<Boolean> deposit(UUID playerUUID, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return CompletableFuture.completedFuture(false);
        }
        BigDecimal currentBalance = dataManager.getBalance(playerUUID);
        dataManager.setBalance(playerUUID, currentBalance.add(amount));
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public synchronized CompletableFuture<Void> setBalance(UUID playerUUID, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) >= 0) {
            dataManager.setBalance(playerUUID, amount);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Boolean> isFrozen(UUID playerUUID) {
        return CompletableFuture.completedFuture(dataManager.isFrozen(playerUUID));
    }

    @Override
    public synchronized CompletableFuture<Void> setFrozen(UUID playerUUID, boolean frozen) {
        dataManager.setFrozen(playerUUID, frozen);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public String getCurrencySymbol() {
        return plugin.getConfigManager().getCurrencySymbol();
    }
}