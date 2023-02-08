package fr.milekat.shops.storage;

import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.storage.adapter.elasticsearch.ESStorage;
import fr.milekat.shops.storage.adapter.sql.SQLStorage;
import fr.milekat.shops.storage.exeptions.StorageExecuteException;
import fr.milekat.shops.storage.exeptions.StorageLoaderException;
import fr.milekat.shops.workers.utils.TradeMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Storage {
    public static final long SHOP_DELAY = TimeUnit.MILLISECONDS.convert(5L, TimeUnit.MINUTES);
    public static Map<Shop, Date> SHOP_CACHE = new HashMap<>();
    public static final long TRADE_DELAY = TimeUnit.MILLISECONDS.convert(1L, TimeUnit.MINUTES);
    public static Map<Trade, Date> TRADE_CACHE = new HashMap<>();
    public static final long TRADE_MODE_DELAY = TimeUnit.MILLISECONDS.convert(5L, TimeUnit.MINUTES);
    public static Map<Map.Entry<String, TradeMode>, Date> TRADE_MODE_CACHE = new HashMap<>();
    private final StorageImplementation executor;

    public Storage(@NotNull FileConfiguration config) throws StorageLoaderException {
        String storageType = config.getString("storage.type");
        Main.debug("Loading storage type: " + storageType);
        switch (Objects.requireNonNull(storageType).toLowerCase()) {
            case "mysql":
            case "mariadb":
            case "postgres": {
                executor = new SQLStorage(config);
                break;
            }
            case "elasticsearch": {
                executor = new ESStorage(config);
                break;
            }
            default: throw new StorageLoaderException("Unsupported storage type");
        }
        try {
            if (executor.checkStorages()) {
                Main.debug("Storage loaded");
            } else {
                throw new StorageLoaderException("Storages are not loaded properly");
            }
        } catch (StorageExecuteException exception) {
            throw new StorageLoaderException("Can't load storage properly");
        }
    }

    public StorageImplementation getStorageImplementation() {
        return this.executor;
    }

    /**
     * Add a shop to the cache shop map
     */
    public static void addCache(@NotNull Map<Shop, Date> cache, @NotNull Shop shop) {
        List<Shop> instances = new ArrayList<>(cache.keySet());
        if (instances.stream().anyMatch(shop1 -> shop1.getUuid().equals(shop.getUuid()))) {
            Map<Shop, Date> tempCache = new HashMap<>(cache);
            cache.keySet().stream().filter(shop1 -> shop1.getUuid().equals(shop.getUuid())).forEach(tempCache::remove);
            tempCache.put(shop, new Date());
            cache = new HashMap<>(tempCache);
        } else {
            cache.put(shop, new Date());
        }
        Storage.SHOP_CACHE = cache;
    }

    public static void addCache(@NotNull Map<Map.Entry<String, TradeMode>, Date> cache,
                                @NotNull String uuid, @NotNull TradeMode tradeMode) {
        List<Map.Entry<String, TradeMode>> instances = new ArrayList<>(cache.keySet());
        if (instances.stream().anyMatch(loop -> loop.getKey().equals(uuid))) {
            Map<Map.Entry<String, TradeMode>, Date> tempCache = new HashMap<>(cache);
            cache.keySet().stream().filter(loop -> loop.getKey().equals(uuid)).forEach(tempCache::remove);
            tempCache.put(new AbstractMap.SimpleEntry<>(uuid, tradeMode), new Date());
            cache = new HashMap<>(tempCache);
        } else {
            cache.put(new AbstractMap.SimpleEntry<>(uuid, tradeMode), new Date());
        }
        Storage.TRADE_MODE_CACHE = cache;
    }
}
