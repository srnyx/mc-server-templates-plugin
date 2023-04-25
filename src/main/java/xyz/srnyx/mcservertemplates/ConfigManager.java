package xyz.srnyx.mcservertemplates;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.logging.Level;


public class ConfigManager {
    private final MCServerTemplates plugin;
    private String path;
    private File newFile;
    private final String world = Bukkit.getWorlds().get(0).getName();

    /**
     * Constructs a new {@link ConfigManager} with the given path
     *
     * @param   plugin  the {@link MCServerTemplates} instance
     */
    public ConfigManager(@NotNull MCServerTemplates plugin) {
        this.plugin = plugin;
    }

    /**
     * Initiate download process of the file
     *
     * @param   path    the path to the file
     */
    public void download(@NotNull String path) {
        final InputStream stream = getClass().getResourceAsStream("/configs/" + path);
        this.path = (path = path.replace("$world$", world));
        this.newFile = new File(plugin.getDataFolder().getParentFile().getParentFile(), path);
        if (stream == null) {
            plugin.log(Level.SEVERE, "&4" + path + " &8|&c File not found!");
            return;
        }

        // Initiate transfer
        try (final Reader reader = new InputStreamReader(stream)) {
            // YML
            if (path.endsWith(".yml")) {
                yml(reader);
                return;
            }

            // Properties
            if (path.endsWith(".properties")) {
                properties(reader);
                return;
            }

            // Other
            final File configs = new File(plugin.getDataFolder(), "configs");
            plugin.saveResource("configs/" + path, true);
            Files.move(new File(configs, path).toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.delete(configs.toPath());
            finish();
        } catch (IOException e) {
            plugin.log(Level.SEVERE, "&4" + path + " &8|&c Failed to download file!");
        }
    }

    /**
     * Transfers the values from the resource {@code .yml} file to the new {@code .yml} file
     *
     * @param   reader      the {@link Reader} of the resource file
     *
     * @throws  IOException if an I/O error occurs
     */
    private void yml(@NotNull Reader reader) throws IOException {
        final YamlConfiguration jarFile = YamlConfiguration.loadConfiguration(reader);
        final YamlConfiguration serverFile = YamlConfiguration.loadConfiguration(newFile);
        for (final String key : jarFile.getKeys(true)) {
            final Object value = jarFile.get(key);
            if (!(value instanceof ConfigurationSection)) serverFile.set(key.replace("$world$", world), value);
        }
        serverFile.save(newFile);
        finish();
    }

    /**
     * Transfers the values from the resource {@code .properties} file to the new {@code .properties} file
     *
     * @param   reader      the {@link Reader} of the resource file
     *
     * @throws  IOException if an I/O error occurs
     */
    private void properties(@NotNull Reader reader) throws IOException {
        final Properties jarFile = new Properties();
        final Properties serverFile = new Properties();
        jarFile.load(reader);
        try (final FileInputStream input = new FileInputStream(newFile);
             final FileOutputStream output = new FileOutputStream(newFile)) {
            serverFile.load(input);
            jarFile.stringPropertyNames().forEach(key -> serverFile.setProperty(key.replace("$world$", world), jarFile.getProperty(key)));
            serverFile.store(output, null);
            finish();
        }
    }

    /**
     * Finishes the download process
     */
    private void finish() {
        plugin.log(Level.INFO, "&2" + path + " &8|&a File downloaded!");
        plugin.files.add(path);
    }
}
