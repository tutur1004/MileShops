package fr.milekat.shops.storage;

import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.storage.utils.PlayerTradeMode;
import fr.milekat.shops.storage.utils.ShopTrades;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CacheManager {
    /**
     * Add a Shop to the cache Shop map
     */
    public static void addCache(@NotNull Map<Shop, Date> cache, @NotNull Shop shop) {
        List<Shop> instances = new ArrayList<>(cache.keySet());
        if (instances.stream().anyMatch(loop -> loop.getUuid().equals(shop.getUuid()))) {
            Map<Shop, Date> tempCache = new HashMap<>(cache);
            cache.keySet().stream().filter(loop -> loop.getUuid().equals(shop.getUuid())).forEach(tempCache::remove);
            tempCache.put(shop, new Date());
            cache = new HashMap<>(tempCache);
        } else {
            cache.put(shop, new Date());
        }
        Storage.SHOP_CACHE = cache;
    }

    /**
     * Add a Trade to the cache Trade map
     */
    public static void addCache(@NotNull Map<ShopTrades, Date> cache, @NotNull ShopTrades shopTrades) {
        List<ShopTrades> instances = new ArrayList<>(cache.keySet());
        if (instances.stream().anyMatch(loop -> loop.shopUuid().equals(shopTrades.shopUuid()))) {
            Map<ShopTrades, Date> tempCache = new HashMap<>(cache);
            cache.keySet().stream().filter(loop -> loop.shopUuid().equals(shopTrades.shopUuid()))
                    .forEach(tempCache::remove);
            tempCache.put(shopTrades, new Date());
            cache = new HashMap<>(tempCache);
        } else {
            cache.put(shopTrades, new Date());
        }
        Storage.TRADE_CACHE = cache;
    }

    /**
     * Add a TradeMode to the cache TradeMode map
     */
    public static void addCache(@NotNull Map<PlayerTradeMode, Date> cache, @NotNull PlayerTradeMode playerTradeMode) {
        List<PlayerTradeMode> instances = new ArrayList<>(cache.keySet());
        if (instances.stream().anyMatch(loop -> loop.playerUuid().equals(playerTradeMode.playerUuid()))) {
            Map<PlayerTradeMode, Date> tempCache = new HashMap<>(cache);
            cache.keySet().stream().filter(loop -> loop.playerUuid().equals(playerTradeMode.playerUuid()))
                    .forEach(tempCache::remove);
            tempCache.put(playerTradeMode, new Date());
            cache = new HashMap<>(tempCache);
        } else {
            cache.put(playerTradeMode, new Date());
        }
        Storage.TRADE_MODE_CACHE = cache;
    }
}
