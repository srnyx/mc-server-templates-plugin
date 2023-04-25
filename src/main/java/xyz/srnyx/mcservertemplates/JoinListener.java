package xyz.srnyx.mcservertemplates;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import xyz.srnyx.annoyingapi.AnnoyingListener;
import xyz.srnyx.annoyingapi.AnnoyingMessage;


public class JoinListener implements AnnoyingListener {
    private final MCServerTemplates plugin;

    @Contract(pure = true)
    public JoinListener(@NotNull MCServerTemplates plugin) {
        this.plugin = plugin;
    }

    @Override @NotNull
    public MCServerTemplates getPlugin() {
        return plugin;
    }

    /**
     * Stores details for players attempting to log in.
     * This event is asynchronous, and not run using main thread.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerPreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        String key = "kick.normal";
        if (Bukkit.getOfflinePlayer(event.getUniqueId()).isOp()) key = "kick.op";
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, new AnnoyingMessage(plugin, key).toString());
    }
}
