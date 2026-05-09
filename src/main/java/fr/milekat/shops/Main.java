package fr.milekat.shops;

import fr.milekat.shops.api.MileShopsIAPI;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.hooks.MileBanks;
import fr.milekat.shops.hooks.MileNpc;
import fr.milekat.shops.hooks.npc.NPCTimedShops;
import fr.milekat.shops.listeners.DefaultTags;
import fr.milekat.shops.storage.StorageImplementation;
import fr.milekat.shops.storage.adapter.elasticsearch.ESStorage;
import fr.milekat.shops.storage.adapter.sql.SQLStorage;
import fr.milekat.shops.storage.utils.PlayerTradeMode;
import fr.milekat.shops.storage.utils.ShopTrades;
import fr.milekat.shops.workers.commands.ShopsCmd;
import fr.milekat.shops.workers.listeners.ShopsListeners;
import fr.milekat.shops.workers.listeners.TradeListeners;
import fr.milekat.shops.workers.utils.ColorStyles;
import fr.milekat.utils.Configs;
import fr.milekat.utils.McTools;
import fr.milekat.utils.MileLogger;
import fr.milekat.utils.storage.StorageConnection;
import fr.milekat.utils.storage.StorageLoader;
import fr.milekat.utils.storage.StorageVendor;
import fr.milekat.utils.storage.adapter.elasticsearch.connection.ESConnection;
import fr.milekat.utils.storage.adapter.sql.connection.SQLConnection;
import fr.milekat.utils.storage.exceptions.StorageExecuteException;
import fr.milekat.utils.storage.exceptions.StorageLoadException;
import fr.milekat.utils.storage.utils.StorageConfig;
import fr.mrmicky.fastinv.FastInvManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Main extends JavaPlugin {
    private static JavaPlugin plugin;
    private static MileLogger logger;
    private static Configs config;
    public static Boolean DEBUG = false;
    public static String PREFIX;
    public static boolean IS_NPC_LIB_LOADED = false;
    public static boolean IS_BANKS_LIB_LOADED = false;
    private static StorageImplementation STORAGE;
    public static final Map<String, Class<?>> TAGS = new HashMap<>();
    public static final Map<UUID, Map<String, Object>> PLAYER_TAGS = new HashMap<>();
    private NPCTimedShops NPCTimedShops;
    public static Map<UUID, Double> PLAYER_MODIFIERS = new HashMap<>();
    /*
        Shop cache
     */
    public static long SHOP_DELAY = TimeUnit.MILLISECONDS.convert(5L, TimeUnit.MINUTES);
    public static Map<Shop, Date> SHOP_CACHE = new HashMap<>();
    public static long TRADE_DELAY = TimeUnit.MILLISECONDS.convert(1L, TimeUnit.MINUTES);
    public static Map<ShopTrades, Date> TRADE_CACHE = new HashMap<>();
    public static long TRADE_MODE_DELAY = TimeUnit.MILLISECONDS.convert(5L, TimeUnit.MINUTES);
    public static Map<PlayerTradeMode, Date> TRADE_MODE_CACHE = new HashMap<>();

    @Override
    public void onEnable() {
        plugin = this;
        logger = new MileLogger(this.getLogger());
        //  Hook to MileNpc for NPC shops
        if (Bukkit.getPluginManager().getPlugin("MileNpc") != null) {
            logger.info("MileNPC detected, hooking into it..");
            try {
                MileNpc.getNpcApi();
                plugin.getServer().getPluginManager().registerEvents(new MileNpc(), this);
                IS_NPC_LIB_LOADED = true;
                logger.info("Hooked into MileNPC successfully !");
            } catch (RuntimeException exception) {
                logger.warning("Failed to hook into MileNPC, NPC features will be unavailable !");
            }
        }
        //  Hook to MileBanks for money trades
        if (Bukkit.getPluginManager().getPlugin("MileBanks") != null) {
            logger.info("MileBanks detected, hooking into it..");
            try {
                MileBanks.getBankApi();
                IS_BANKS_LIB_LOADED = true;
                logger.info("Hooked into MileBanks successfully !");
            } catch (RuntimeException exception) {
                logger.warning("Failed to hook into MileBanks, money features will be unavailable !");
            }
        }
        //  Load configs
        try {
            reloadConfigs();
        } catch (NullPointerException exception) {
            logger.warning("Error: " + exception.getLocalizedMessage());
            logger.warning("Configs load failed, disabling plugin..");
            this.onDisable();
            return;
        }
        //  Init internal lib
        FastInvManager.register(plugin);
        //  Load storage
        try {
            reloadStorage();
        } catch (StorageLoadException exception) {
            logger.warning("Error: " + exception.getLocalizedMessage());
            logger.stack(exception.getStackTrace());
            logger.warning("Storage load failed, disabling plugin..");
            this.onDisable();
            return;
        }
        //  Load API
        Bukkit.getServicesManager().register(MileShopsIAPI.class, new API(), this, ServicePriority.Normal);
        //  Load plugin workers
        plugin.getServer().getPluginManager().registerEvents(new ShopsListeners(), this);
        plugin.getServer().getPluginManager().registerEvents(new TradeListeners(), this);
        if (config.getBoolean("tags.enable_builtin_tags", true)) {
            plugin.getServer().getPluginManager().registerEvents(new DefaultTags(), this);
        }
        if (IS_NPC_LIB_LOADED) {
            NPCTimedShops = new NPCTimedShops();
        }
        PluginCommand shopCommand = plugin.getCommand("shop");
        if (shopCommand != null) {
            shopCommand.setExecutor(new ShopsCmd());
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getServicesManager().unregisterAll(this);
        try {
            getStorage().disconnect();
            if (IS_NPC_LIB_LOADED) {
                NPCTimedShops.cancel();
            }
        } catch (Exception ignored) {}
    }

    @Contract(" -> new")
    public static @NotNull MileLogger getMileLogger() {
        return logger;
    }

    /**
     * Send a formatted message to sender
     */
    public static void message(@NotNull CommandSender sender, @NotNull String message) {
        if (sender instanceof Player player) {
            message(player, message);
        } else {
            getMileLogger().info(message);
        }
    }

    /**
     * Send a formatted message to sender
     */
    public static void message(@NotNull Player player, @NotNull String message) {
        message(player, Component.text(McTools.minecraftColorCodes(message)));
    }

    /**
     * Send a formatted BaseComponent message to sender
     */
    public static void message(@NotNull Player player, @NotNull Component message) {
        player.sendMessage(message.style(message.style().merge(ColorStyles.INFO_VALUE)));
    }

    /**
     * Get Storage
     *
     * @return Storage implementation
     */
    public static StorageImplementation getStorage() {
        return STORAGE;
    }

    /**
     * Get config file
     *
     * @return Config file
     */
    public static Configs getConfigs() {
        return config;
    }

    /**
     * Reload configs
     */
    public static void reloadConfigs() throws NullPointerException {
        // If config file doesn't exist, create it
        plugin.saveDefaultConfig();
        config = new Configs(new File(plugin.getDataFolder(), "config.yml"));
        TAGS.clear();
        if (config.getBoolean("tags.enable_builtin_tags", true)) {
            TAGS.put("player-uuid", String.class);
            TAGS.put("player-name", String.class);
        } else {
            config.getStringList("tags.custom.string").forEach(tag -> TAGS.put(tag, String.class));
            config.getStringList("tags.custom.integer").forEach(tag -> TAGS.put(tag, Integer.class));
            config.getStringList("tags.custom.long").forEach(tag -> TAGS.put(tag, Float.class));
            config.getStringList("tags.custom.double").forEach(tag -> TAGS.put(tag, Double.class));
            config.getStringList("tags.custom.boolean").forEach(tag -> TAGS.put(tag, Boolean.class));
        }
        DEBUG = config.getBoolean("debug", false);
        logger.setDebug(DEBUG);
        PREFIX = PlainTextComponentSerializer.plainText().serialize(LegacyComponentSerializer.legacyAmpersand()
                .deserialize(config.getString("messages.prefix", "[" + plugin.getName() + "] ")));
        logger.debug("Debug enable");
        logger.info("Config loaded");
    }

    /**
     * Reload storage
     */
    public static void reloadStorage() throws StorageLoadException {
        try {
            getStorage().disconnect();
        } catch (Exception ignored) {}
        StorageConfig storageConfig = StorageConfig.fromConfig(config);
        StorageConnection connection = new StorageLoader(storageConfig, logger).getLoadedConnection();
        if (storageConfig.type() == StorageVendor.ELASTICSEARCH) {
            STORAGE = new ESStorage((ESConnection) connection, config);
        } else if (storageConfig.type() == StorageVendor.MYSQL) {
            STORAGE = new SQLStorage((SQLConnection) connection, config);
        } else {
            throw new StorageLoadException("Unsupported storage type");
        }
        try {
            if (!STORAGE.checkStorages()) {
                throw new StorageLoadException("Storages are not loaded properly");
            }
        } catch (StorageExecuteException exception) {
            throw new StorageLoadException("Unsupported database type");
        }
        if (config.getBoolean("storage.cache.enable", true)) {
            long delay = TimeUnit.MILLISECONDS.convert(
                    config.getLong("storage.cache.time", 5L), TimeUnit.SECONDS);
            Main.SHOP_DELAY = delay;
            Main.TRADE_DELAY = delay;
            Main.TRADE_MODE_DELAY = delay;
            logger.debug("Storage cache delay set to " + delay + "ms");
        } else {
            Main.SHOP_DELAY = 0L;
            Main.TRADE_DELAY = 0L;
            Main.TRADE_MODE_DELAY = 0L;
            logger.debug("Storage cache disabled");
        }
        Main.SHOP_CACHE.clear();
        Main.TRADE_CACHE.clear();
        Main.TRADE_MODE_CACHE.clear();
        logger.debug("Storage enable, API is now available");
    }

    /**
     * Reload all shops
     *
     * @return number of loaded shops
     */
    public static int reloadShops() {
        int loaded = 0;
        logger.info("Loading all shops...");
        Main.SHOP_CACHE.clear();
        Main.TRADE_CACHE.clear();
        Main.TRADE_MODE_CACHE.clear();
        try {
            List<Shop> shops = getStorage().getAllShops();
            loaded = shops.size();
            logger.info(loaded + " shops loaded !");
        } catch (StorageExecuteException e) {
            logger.warning(e.getMessage());
            logger.stack(e.getStackTrace());
            logger.warning("Storage error");
        }
        return loaded;
    }

    /**
     * Get the plugin instance
     *
     * @return bukkit plugin instance
     */
    public static JavaPlugin getInstance() {
        return plugin;
    }
}
