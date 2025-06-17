package ru.corearchitect.coreeconomy.model;

import java.math.BigDecimal;
import java.util.UUID;

public class LeaderboardEntry {
    private final UUID uuid;
    private final String name;
    private final BigDecimal balance;

    public LeaderboardEntry(UUID uuid, String name, BigDecimal balance) {
        this.uuid = uuid;
        this.name = name;
        this.balance = balance;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}