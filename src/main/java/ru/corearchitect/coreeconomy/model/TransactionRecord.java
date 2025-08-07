package ru.corearchitect.coreeconomy.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionRecord {
    private final LocalDateTime timestamp;
    private final String initiatorName;
    private final String targetName;
    private final TransactionType transactionType;
    private final BigDecimal amount;
    private final BigDecimal commission;

    public TransactionRecord(String initiatorName, String targetName, TransactionType transactionType, BigDecimal amount, BigDecimal commission) {
        this.timestamp = LocalDateTime.now();
        this.initiatorName = initiatorName;
        this.targetName = targetName;
        this.transactionType = transactionType;
        this.amount = amount;
        this.commission = commission;
    }

    public TransactionRecord(LocalDateTime timestamp, String initiatorName, String targetName, TransactionType transactionType, BigDecimal amount, BigDecimal commission) {
        this.timestamp = timestamp;
        this.initiatorName = initiatorName;
        this.targetName = targetName;
        this.transactionType = transactionType;
        this.amount = amount;
        this.commission = commission;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getInitiatorName() {
        return initiatorName;
    }

    public String getTargetName() {
        return targetName;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getCommission() {
        return commission;
    }
}