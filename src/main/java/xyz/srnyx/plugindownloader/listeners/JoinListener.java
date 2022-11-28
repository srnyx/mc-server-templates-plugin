package xyz.srnyx.plugindownloader.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import org.jetbrains.annotations.NotNull;


public class JoinListener implements Listener {
    /**
     * Stores details for players attempting to log in.
     * This event is asynchronous, and not run using main thread.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerPreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        String message = "&cServer administrator(s) must finish plugin installation & file download!";
        if (Bukkit.getOfflinePlayer(event.getUniqueId()).isOp()) message = "&cPlease restart the server to finish plugin installation & file download!";
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ChatColor.translateAlternateColorCodes('&', message));
    }
}
