package ru.corearchitect.coreeconomy.model;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

public class PendingAdminCommand {
    private final CommandSender sender;
    private final OfflinePlayer target;
    private final String type;
    private final String[] args;
    private final long timestamp;
    private static final long EXPIRATION_TIME_MS = 15_000;

    public PendingAdminCommand(CommandSender sender, OfflinePlayer target, String type, String[] args) {
        this.sender = sender;
        this.target = target;
        this.type = type;
        this.args = args;
        this.timestamp = System.currentTimeMillis();
    }

    public CommandSender getSender() {
        return sender;
    }

    public OfflinePlayer getTarget() {
        return target;
    }

    public String getType() {
        return type;
    }

    public String[] getArgs() {
        return args;
    }

    public boolean isExpired() {
        return (System.currentTimeMillis() - timestamp) > EXPIRATION_TIME_MS;
    }
}