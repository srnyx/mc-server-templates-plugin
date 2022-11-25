package xyz.srnyx.plugindownloader;

import org.apache.commons.lang3.StringUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;


public class PluginDownloader extends JavaPlugin {
    /**
     * Called when this plugin is enabled
     */
    @Override
    public void onEnable() {
        // Start messages
        final Logger logger = getLogger();
        final String name = getName() + " v" + getDescription().getVersion();
        final String authors = "By " + String.join(", ", getDescription().getAuthors());
        final String line = StringUtils.repeat("-", Math.max(name.length(), authors.length()));
        logger.info(ChatColor.DARK_AQUA + line);
        logger.info(ChatColor.AQUA + name);
        logger.info(ChatColor.AQUA + authors);
        logger.info(ChatColor.DARK_AQUA + line);

        // Get config
        saveDefaultConfig();
        final FileConfiguration config = getConfig();

        // Get plugins list
        final ConfigurationSection pluginsSection = config.getConfigurationSection("plugins");
        if (pluginsSection == null) return;
        final Set<String> pluginKeys = pluginsSection.getKeys(false);
        if (pluginKeys.isEmpty()) return;

        // Get messages
        final String download = ChatColor.translateAlternateColorCodes('&', config.getString("messages.download", "messages.download"));
        final String external = ChatColor.translateAlternateColorCodes('&', config.getString("messages.external", "messages.external"));
        final String failed = ChatColor.translateAlternateColorCodes('&', config.getString("messages.failed", "messages.failed"));

        // Variables
        final PluginManager manager = Bukkit.getPluginManager();
        final String pluginsFolder = getDataFolder().getParent();
        final String userAgent  = "PluginDownloader/" + getDescription().getVersion();

        // Delay plugin downloading (3 seconds)
        new BukkitRunnable() {
            public void run() {
                // Get plugins that need to be downloaded
                boolean shutdown = false;
                final Set<String> plugins = new HashSet<>();
                for (final String plugin : pluginKeys) if (manager.getPlugin(plugin) == null && !new File(pluginsFolder, plugin + ".jar").exists()) {
                    final Object object = pluginsSection.get(plugin);
                    if (object == null) continue;

                    // External plugin
                    if (object instanceof String string) {
                        logger.warning(external
                                .replace("%plugin%", plugin)
                                .replace("%link%", string));
                        shutdown = true;
                        continue;
                    }

                    // Spigot plugin
                    plugins.add(plugin);
                }
                if (plugins.isEmpty()) {
                    if (shutdown) Bukkit.shutdown();
                    return;
                }
                logger.info(download.replace("%plugins%", String.join(ChatColor.GREEN + ", " + ChatColor.DARK_GREEN, plugins)));

                // Download plugins
                for (final String plugin : plugins) {
                    final int id = pluginsSection.getInt(plugin);
                    final String failMessage = failed
                            .replace("%plugin%", plugin)
                            .replace("%link%", "https://spigotmc.org/resources/" + id);

                    // Get plugin file connection
                    final HttpURLConnection fileConnection;
                    try {
                        final URL url = new URL("https://api.spiget.org/v2/resources/" + id + "/download");
                        fileConnection = (HttpURLConnection) url.openConnection();
                        fileConnection.setRequestMethod("GET");
                        fileConnection.addRequestProperty("User-Agent", userAgent);

                        // Connection failed
                        if (fileConnection.getResponseCode() == 404) {
                            logger.severe(failMessage);
                            continue;
                        }
                    } catch (final IOException e) {
                        logger.severe(failMessage);
                        continue;
                    }

                    // Attempt to download file
                    try (
                            final InputStream in = fileConnection.getInputStream();
                            final FileOutputStream fileOut = new FileOutputStream(new File(pluginsFolder, plugin + ".jar"));
                            final OutputStream out = new BufferedOutputStream(fileOut)
                    ) {
                        final byte[] buffer = new byte[1024];
                        int numRead;
                        while ((numRead = in.read(buffer)) != -1) out.write(buffer, 0, numRead);
                    } catch (final IOException ignored) {
                        //ignored
                    }
                }

                // Shutdown server
                Bukkit.shutdown();
            }
        }.runTaskLater(this, 60L);
    }
}