package fr.milekat.shops;

import dev.sergiferry.playernpc.api.NPCLib;
import fr.milekat.shops.storage.Storage;
import fr.milekat.shops.storage.StorageImplementation;
import fr.milekat.shops.storage.exeptions.StorageLoaderException;
import fr.milekat.shops.workers.Test;
import fr.milekat.shops.workers.commands.ShopsCmd;
import fr.milekat.shops.workers.listeners.ShopsListeners;
import fr.mrmicky.fastinv.FastInvManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class Main extends JavaPlugin {
    private static JavaPlugin plugin;
    private static FileConfiguration configFile;
    public static Boolean DEBUG = false;
    private static Storage LOADED_STORAGE;

    @Override
    public void onEnable() {
        plugin = this;
        configFile = this.getConfig();
        DEBUG = configFile.getBoolean("debug");
        debug("Debug enable");
        //  Init internal lib
        NPCLib.getInstance().registerPlugin(plugin);
        FastInvManager.register(plugin);
        //  Load storage
        try {
            LOADED_STORAGE = new Storage(configFile);
            debug("Storage enable, API is now available");
        } catch (StorageLoaderException exception) {
            warning("Storage load failed, disabling plugin..");
            warning("Error: " + exception.getLocalizedMessage());
            if (DEBUG) exception.printStackTrace();
            this.onDisable();
        }
        plugin.getServer().getPluginManager().registerEvents(new Test(), this);
        plugin.getServer().getPluginManager().registerEvents(new ShopsListeners(), this);
        plugin.getCommand("shop").setExecutor(new ShopsCmd());
    }

    @Override
    public void onDisable() {
        try {
            getStorage().disconnect();
        } catch (Exception ignored) {}
    }

    /**
     * Log a debug if debug is enable
     * @param message to debug
     */
    public static void debug(String message) {
        if (DEBUG) plugin.getLogger().info("[DEBUG] " + message);
    }

    /**
     * If debug are enabled, stack traces will be logged at warning level
     * @param stacks to log
     */
    public static void stack(StackTraceElement[] stacks) {
        if (DEBUG) Arrays.stream(stacks).distinct().forEach(stackTraceElement -> warning(stackTraceElement.toString()));
    }

    /**
     * Log a message
     * @param message to send
     */
    public static void info(String message) {
        plugin.getLogger().info(message);
    }

    /**
     * Log a warning
     * @param message to raise
     */
    public static void warning(String message) {
        plugin.getLogger().warning(message);
    }

    /**
     * Send a formatted message to sender
     */
    public static void message(@NotNull CommandSender sender, @NotNull String message) {
        if (sender instanceof Player player) {
            message(player, message);
        } else {
            info(message);
        }
    }

    /**
     * Send a formatted message to sender
     */
    public static void message(@NotNull Player player, @NotNull String message) {
        player.sendMessage(ChatColor.GOLD + "[CustomShop] " + ChatColor.RESET +
                ChatColor.translateAlternateColorCodes('&', message));
    }

    /**
     * Get Storage
     * @return Storage implementation
     */
    public static StorageImplementation getStorage() {
        return LOADED_STORAGE.getStorageImplementation();
    }

    /**
     * Get config file
     * @return Config file
     */
    public static FileConfiguration getFileConfig() {
        return configFile;
    }

    /**
     * Get the plugin instance
     * @return bukkit plugin instance
     */
    public static JavaPlugin getInstance() {
        return plugin;
    }
}
