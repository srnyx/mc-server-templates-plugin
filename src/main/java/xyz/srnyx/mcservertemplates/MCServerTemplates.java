package xyz.srnyx.mcservertemplates;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;

import xyz.srnyx.annoyingapi.AnnoyingPlugin;
import xyz.srnyx.annoyingapi.dependency.AnnoyingDependency;
import xyz.srnyx.annoyingapi.dependency.AnnoyingDownload;
import xyz.srnyx.annoyingapi.file.AnnoyingResource;

import java.io.File;
import java.io.IOException;
import java.security.CodeSource;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class MCServerTemplates extends AnnoyingPlugin {
    public AnnoyingResource config;
    public List<String> files;
    public final JoinListener listener = new JoinListener(this);

    public MCServerTemplates() {
        super();
        options.bStatsId = 18288;
        options.listenersToRegister.add(listener);
    }

    /**
     * Called when this plugin is enabled
     */
    @Override
    public void enable() {
        // Get config
        saveDefaultConfig();

        // Set variables
        config = new AnnoyingResource(this, "config.yml");
        files = config.getStringList("files");

        // Register JoinListener
        listener.register();

        // Delay downloading (3 seconds)
        new BukkitRunnable() {
            public void run() {
                if (downloadPlugins() && downloadFiles()) listener.unregister();
            }
        }.runTaskLater(this, 1L);
    }

    public boolean downloadPlugins() {
        final ConfigurationSection section = config.getConfigurationSection("plugins");
        if (section == null) return false;
        final Set<String> keys = section.getKeys(false);
        if (keys.isEmpty()) return false;

        // Get plugins that need to be downloaded
        final PluginManager manager = Bukkit.getPluginManager();
        final File pluginsFolder = getDataFolder().getParentFile();
        final Set<String> plugins = keys.stream()
                .filter(plugin -> manager.getPlugin(plugin) == null && !new File(pluginsFolder, plugin + ".jar").exists())
                .collect(Collectors.toSet());
        if (plugins.isEmpty()) return false;

        // Install plugins
        final List<AnnoyingDependency> dependencies = new ArrayList<>();
        for (final String plugin : plugins) {
            final ConfigurationSection pluginSection = section.getConfigurationSection(plugin);
            if (pluginSection == null) continue;
            final Map<AnnoyingDownload.Platform, String> platforms = new EnumMap<>(AnnoyingDownload.Platform.class);
            for (final String platform : pluginSection.getKeys(false)) {
                try {
                    platforms.put(AnnoyingDownload.Platform.valueOf(platform.toUpperCase()), pluginSection.getString(platform));
                } catch (final IllegalArgumentException e) {
                    log(Level.WARNING, "&4" + plugin + " &8|&c Invalid platform:" + platform);
                }
            }
            dependencies.add(new AnnoyingDependency(plugin, platforms, true, false));
        }
        new AnnoyingDownload(this, dependencies).downloadPlugins(null);
        return true;
    }

    public boolean downloadFiles() {
        // Get CodeSource
        final CodeSource source = getClass().getProtectionDomain().getCodeSource();
        if (source == null) {
            log(Level.SEVERE, "&cCould not get CodeSource!");
            return false;
        }

        // Get files that need to be downloaded
        final Set<String> paths = new HashSet<>();
        try (final ZipInputStream zip = new ZipInputStream(source.getLocation().openStream())) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String path = entry.getName();
                if (path.endsWith("/") || !path.startsWith("configs/")) continue;
                path = path.substring(8);
                if (!files.contains(path)) paths.add(path);
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
        if (paths.isEmpty()) return false;

        // Download files
        paths.forEach(path -> new ConfigManager(MCServerTemplates.this).download(path));
        config.setSave("files", files);
        log(Level.INFO, "\n&a&lAll &2&l" + paths.size() + "&a&l files have been processed!\n&aPlease resolve any errors and then restart the server.");
        return true;
    }
}
