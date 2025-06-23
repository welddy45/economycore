package ru.corearchitect.coreeconomy.api;

import ru.corearchitect.coreeconomy.model.TransactionResult;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface EconomyAPI {
    CompletableFuture<BigDecimal> getBalance(UUID playerUUID);
    CompletableFuture<Boolean> hasAccount(UUID playerUUID);
    CompletableFuture<Boolean> withdraw(UUID playerUUID, BigDecimal amount);
    CompletableFuture<Boolean> deposit(UUID playerUUID, BigDecimal amount);
    CompletableFuture<Void> setBalance(UUID playerUUID, BigDecimal amount);
    CompletableFuture<Boolean> isFrozen(UUID playerUUID);
    CompletableFuture<Void> setFrozen(UUID playerUUID, boolean frozen);
    String getCurrencySymbol();
    CompletableFuture<BigDecimal> getTotalServerBalance();
    CompletableFuture<BigDecimal> getTotalCommission();
    String format(BigDecimal amount);
    CompletableFuture<TransactionResult> transfer(UUID from, UUID to, BigDecimal amount);
}