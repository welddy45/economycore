package ru.corearchitect.coreeconomy.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.corearchitect.coreeconomy.CoreEconomy;

public class PlayerConnectionListener implements Listener {

    private final CoreEconomy plugin;

    public PlayerConnectionListener(CoreEconomy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getDataManager().createAccount(player.getUniqueId());
        plugin.getScoreboardManager().playerJoined(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getScoreboardManager().playerQuit(player);
    }
}