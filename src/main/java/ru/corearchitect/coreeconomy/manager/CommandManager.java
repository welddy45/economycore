package ru.corearchitect.coreeconomy.manager;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import ru.corearchitect.coreeconomy.CoreEconomy;
import ru.corearchitect.coreeconomy.command.EconomyAdminCommand;
import ru.corearchitect.coreeconomy.command.PlayerEconomyCommand;

import java.lang.reflect.Field;

public class CommandManager {

    private final CoreEconomy plugin;
    private CommandMap commandMap;

    public CommandManager(CoreEconomy plugin) {
        this.plugin = plugin;
        try {
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            this.commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());
        } catch (Exception e) {
            plugin.getLogger().severe("Could not access CommandMap!");
            e.printStackTrace();
        }
    }

    public void registerPlayerCommand() {
        if (commandMap == null) return;
        PlayerEconomyCommand command = new PlayerEconomyCommand(plugin);
        commandMap.register(plugin.getName().toLowerCase(), command);
    }

    public void registerAdminCommand() {
        if (commandMap == null) return;
        EconomyAdminCommand command = new EconomyAdminCommand(plugin);
        commandMap.register(plugin.getName().toLowerCase(), command);
    }
}