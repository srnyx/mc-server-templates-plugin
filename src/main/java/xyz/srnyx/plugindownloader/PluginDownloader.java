package xyz.srnyx.plugindownloader;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.plugindownloader.listeners.JoinListener;
import xyz.srnyx.plugindownloader.managers.ConfigManager;
import xyz.srnyx.plugindownloader.managers.DownloadManager;

import java.io.File;
import java.io.IOException;
import java.security.CodeSource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class PluginDownloader extends JavaPlugin {
    public static Logger logger;
    public static FileConfiguration config;
    public static File pluginsFolder;

    public List<String> files;
    public String world;

    /**
     * Called when this plugin is enabled
     */
    @Override
    public void onEnable() {
        // Start messages
        logger = getLogger();
        final String name = getName() + " v" + getDescription().getVersion();
        final String authors = "By " + String.join(", ", getDescription().getAuthors());
        final String line = "-".repeat(Math.max(name.length(), authors.length()));
        logger.info(ChatColor.DARK_AQUA + line);
        logger.info(ChatColor.AQUA + name);
        logger.info(ChatColor.AQUA + authors);
        logger.info(ChatColor.DARK_AQUA + line);

        // Get config
        saveDefaultConfig();
        config = getConfig();
        pluginsFolder = new File(getDataFolder().getParent());
        files = config.getStringList("files");
        world = Bukkit.getWorlds().get(0).getName();

        // Get plugins list
        final ConfigurationSection section = config.getConfigurationSection("plugins");
        if (section == null) return;
        final Set<String> keys = section.getKeys(false);
        if (keys.isEmpty()) return;

        // Register JoinListener
        final PluginManager manager = Bukkit.getPluginManager();
        final JoinListener listener = new JoinListener();
        manager.registerEvents(listener, this);

        // Delay plugin/file downloading (3 seconds)
        new BukkitRunnable() {
            public void run() {
                // Get plugins that need to be downloaded
                final Set<String> plugins = keys.stream()
                        .filter(plugin -> manager.getPlugin(plugin) == null && !new File(pluginsFolder, plugin + ".jar").exists())
                        .collect(Collectors.toSet());

                // Install plugins
                if (!plugins.isEmpty()) {
                    final int size = plugins.size();
                    DownloadManager.total = size;
                    DownloadManager.remaining = size;
                    plugins.forEach(name -> new Thread(() -> new DownloadManager(name).download()).start());
                    return;
                }
                log(Level.INFO, "&aAll plugins are installed!");

                // Get files that need to be downloaded
                final Set<String> paths = new HashSet<>();
                final CodeSource src = getClass().getProtectionDomain().getCodeSource();
                if (src == null) {
                    log(Level.SEVERE, "&cCould not get CodeSource!");
                    return;
                }
                try (final ZipInputStream zip = new ZipInputStream(src.getLocation().openStream())) {
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null) {
                        String path = entry.getName();
                        if (!path.endsWith("/") && path.startsWith("configs/")) {
                            path = path.substring(8);
                            if (!files.contains(path)) paths.add(path);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Download files
                if (!paths.isEmpty()) {
                    paths.forEach(path -> new ConfigManager(PluginDownloader.this).download(path));
                    config.set("files", files);
                    try {
                        config.save(new File(getDataFolder(), "config.yml"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    log(Level.INFO, "\n&a&lAll &2&l" + paths.size() + "&a&l files have been downloaded!\n&aPlease restart the server.");
                    return;
                }
                log(Level.INFO, "&aAll files are downloaded!");
                AsyncPlayerPreLoginEvent.getHandlerList().unregister(listener);
            }
        }.runTaskLater(this, 60L);
    }

    /**
     * Logs a message to the console
     *
     * @param   level   the level of the log
     * @param   message the message to log
     */
    public static void log(@Nullable Level level, @NotNull String message) {
        if (level == null) level = Level.INFO;
        logger.log(level, ChatColor.translateAlternateColorCodes('&', message));
    }
}