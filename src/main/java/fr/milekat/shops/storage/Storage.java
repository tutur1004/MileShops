package fr.milekat.shops.storage;

import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.storage.adapter.elasticsearch.ESStorage;
import fr.milekat.shops.storage.adapter.sql.SQLStorage;
import fr.milekat.shops.storage.exeptions.StorageExecuteException;
import fr.milekat.shops.storage.exeptions.StorageLoaderException;
import fr.milekat.shops.storage.utils.PlayerTradeMode;
import fr.milekat.shops.storage.utils.ShopTrades;
import fr.milekat.utils.Configs;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class Storage {
    public static final long SHOP_DELAY = TimeUnit.MILLISECONDS.convert(5L, TimeUnit.MINUTES);
    public static Map<Shop, Date> SHOP_CACHE = new HashMap<>();
    public static final long TRADE_DELAY = TimeUnit.MILLISECONDS.convert(1L, TimeUnit.MINUTES);
    public static Map<ShopTrades, Date> TRADE_CACHE = new HashMap<>();
    public static final long TRADE_MODE_DELAY = TimeUnit.MILLISECONDS.convert(5L, TimeUnit.MINUTES);
    public static Map<PlayerTradeMode, Date> TRADE_MODE_CACHE = new HashMap<>();
    private final StorageImplementation executor;

    public Storage(@NotNull Configs config) throws StorageLoaderException {
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
}
