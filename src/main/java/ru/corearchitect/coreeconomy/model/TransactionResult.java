package ru.corearchitect.coreeconomy.model;

public enum TransactionResult {
    SUCCESS,
    INSUFFICIENT_FUNDS,
    SENDER_FROZEN,
    RECIPIENT_FROZEN,
    CANNOT_PAY_SELF,
    INVALID_RECIPIENT,
    ERROR
}