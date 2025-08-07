package ru.corearchitect.coreeconomy.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import ru.corearchitect.coreeconomy.model.TransactionResult;

import java.math.BigDecimal;
import java.util.UUID;

public class PostTransactionEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final UUID from;
    private final UUID to;
    private final BigDecimal amount;
    private final BigDecimal commission;
    private final TransactionResult result;

    public PostTransactionEvent(UUID from, UUID to, BigDecimal amount, BigDecimal commission, TransactionResult result) {
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.commission = commission;
        this.result = result;
    }

    public UUID getFrom() {
        return from;
    }

    public UUID getTo() {
        return to;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getCommission() {
        return commission;
    }

    public TransactionResult getResult() {
        return result;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}