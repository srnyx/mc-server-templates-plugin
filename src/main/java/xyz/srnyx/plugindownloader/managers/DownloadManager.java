package xyz.srnyx.plugindownloader.managers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.srnyx.plugindownloader.PluginDownloader;
import xyz.srnyx.plugindownloader.enums.Platform;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;


public class DownloadManager {
    public static int total;
    public static int remaining;

    @NotNull private final String name;
    @NotNull private final Map<Platform, String> platforms = new EnumMap<>(Platform.class);

    /**
     * Constructs a new {@link DownloadManager} for the specified plugin
     *
     * @param   name    the name of the plugin
     */
    @Contract(pure = true)
    public DownloadManager(@NotNull String name) {
        this.name = name;

        // Get platforms
        final ConfigurationSection section = PluginDownloader.config.getConfigurationSection("plugins." + name);
        if (section != null) for (final String platform : section.getKeys(false)) {
            final String id = section.getString(platform);
            if (id == null) continue;
            try {
                platforms.put(Platform.valueOf(platform.toUpperCase()), id);
            } catch (final IllegalArgumentException e) {
                PluginDownloader.log(Level.SEVERE, "&4&l" + name + " &8|&4 " + platform + "&c is not a valid platform!");
            }
        }
    }

    /**
     * Initiate download process for the plugin
     */
    public void download() {
        // Modrinth
        if (platforms.containsKey(Platform.MODRINTH)) {
            modrinth();
            return;
        }

        // Spigot
        if (platforms.containsKey(Platform.SPIGOT)) {
            spigot();
            return;
        }

        // Bukkit
        if (platforms.containsKey(Platform.BUKKIT)) {
            downloadFile(Platform.BUKKIT, "https://dev.bukkit.org/projects/" + platforms.get(Platform.BUKKIT) + "/files/latest");
            return;
        }

        // External
        if (platforms.containsKey(Platform.EXTERNAL)) {
            downloadFile(Platform.EXTERNAL, platforms.get(Platform.EXTERNAL));
            return;
        }

        // Manual
        if (platforms.containsKey(Platform.MANUAL)) {
            PluginDownloader.log(Level.WARNING, "&6" + name + " &8|&e Please install this plugin manually at &6" + platforms.get(Platform.MANUAL));
            finish();
            return;
        }

        // Ran out of platforms
        PluginDownloader.log(Level.SEVERE, "&4" + name + " &8|&c Ran out of platforms!");
        finish();
    }

    /**
     * Downloads the plugin from Modrinth using their Labrinth API
     * <p>This will download the appropriate Minecraft version of the plugin
     */
    private void modrinth() {
        final String[] version = Bukkit.getBukkitVersion().split("\\.");
        final String url = "https://api.modrinth.com/v2/project/" + platforms.get(Platform.MODRINTH) + "/version" +
                "?loaders=%5B%22spigot%22,%22paper%22,%22purpur%22%5D" +
                "&game_versions=%5B%22" + version[0] + "." + version[1] + "." + version[2].split("-")[0] + "%22%5D";
        final JsonElement json = getJson(url);

        // Request failed
        if (json == null) {
            platforms.remove(Platform.MODRINTH);
            download();
            return;
        }

        // Download file
        downloadFile(Platform.MODRINTH, json.getAsJsonArray().get(0).getAsJsonObject()
                .getAsJsonArray("files").get(0).getAsJsonObject()
                .get("url").getAsString());
    }

    /**
     * Downloads the plugin from Spigot using Spiget API
     * <p>This will check if the plugin is premium and/or external before attempting to download
     */
    private void spigot() {
        final String url = "https://api.spiget.org/v2/resources/" + platforms.get(Platform.SPIGOT);
        final JsonElement json = getJson(url);

        // Request failed
        if (json == null) {
            platforms.remove(Platform.SPIGOT);
            download();
            return;
        }
        final JsonObject object = json.getAsJsonObject();

        // Resource is premium
        if (object.get("premium").getAsBoolean()) {
            platforms.remove(Platform.SPIGOT);
            download();
            return;
        }

        // Resource is external
        if (object.get("external").getAsBoolean()) {
            platforms.put(Platform.MANUAL, object
                    .get("file").getAsJsonObject()
                    .get("externalUrl").getAsString());
            platforms.remove(Platform.SPIGOT);
            download();
            return;
        }

        // Download file
        downloadFile(Platform.SPIGOT,  url + "/download");
    }

    /**
     * Retrieves the {@link JsonElement} from the specified URL
     *
     * @param   url the URL to retrieve the {@link JsonElement} from
     *
     * @return      the {@link JsonElement} retrieved from the specified URL
     */
    @Nullable
    private JsonElement getJson(@NotNull String url) {
        try {
            final HttpResponse<String> response = HttpClient.newBuilder().build().send(HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) return null;
            return new JsonParser().parse(response.body());
        } catch (final IOException e) {
            return null;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Downloads a file from a URL
     *
     * @param   platform    the platform of the URL
     * @param   urlString   the URL of the file
     */
    private void downloadFile(@NotNull Platform platform, @NotNull String urlString) {
        // Get URL
        final URL url;
        try {
            url = new URL(urlString);
        } catch (final MalformedURLException e) {
            platforms.remove(platform);
            download();
            return;
        }

        // Download file
        try (final BufferedInputStream in = new BufferedInputStream(url.openStream());
             final FileOutputStream out = new FileOutputStream(new File(PluginDownloader.pluginsFolder, name + ".jar"))) {
            final byte[] buffer = new byte[1024];
            int numRead;
            while ((numRead = in.read(buffer)) != -1) out.write(buffer, 0, numRead);
        } catch (final IOException ignored) {
            //ignored
        }

        // Send success message
        PluginDownloader.log(Level.INFO, "&2" + name + " &8|&a Successfully installed from &2" + platform.name());
        finish();
    }

    /**
     * Finishes the download process
     */
    private void finish() {
        remaining--;
        if (remaining == 0) PluginDownloader.log(Level.INFO, "\n&a&lAll &2&l" + total + "&a&l plugins have been processed!\n&aPlease resolve any errors and then restart the server.");
    }
}
