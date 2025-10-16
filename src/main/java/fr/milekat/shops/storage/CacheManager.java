package fr.milekat.shops.storage;

import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.storage.utils.PlayerTradeMode;
import fr.milekat.shops.storage.utils.ShopTrades;
import fr.milekat.shops.api.classes.TradeMode;
import fr.milekat.utils.storage.exceptions.StorageExecuteException;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("unused")
public interface CacheManager {
    /**
     * Add a Shop to the cache Shop map
     */
    static void addCache(@NotNull Map<Shop, Date> cache, @NotNull Shop shop) {
        List<Shop> instances = new ArrayList<>(cache.keySet());
        if (instances.stream().anyMatch(loop -> loop.getUuid().equals(shop.getUuid()))) {
            Map<Shop, Date> tempCache = new HashMap<>(cache);
            cache.keySet().stream().filter(loop -> loop.getUuid().equals(shop.getUuid())).forEach(tempCache::remove);
            tempCache.put(shop, new Date());
            cache = new HashMap<>(tempCache);
        } else {
            cache.put(shop, new Date());
        }
        //  Order by name
        cache = cache.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Shop::getName)))
                .collect(HashMap::new, (m, e) ->
                        m.put(e.getKey(), e.getValue()), HashMap::putAll);
        Main.SHOP_CACHE = cache;
    }

    /**
     * Add a Trade to the cache Trade map
     */
    static void addCache(@NotNull Map<ShopTrades, Date> cache, @NotNull ShopTrades shopTrades) {
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
        Main.TRADE_CACHE = cache;
    }

    /**
     * Add a TradeMode to the cache TradeMode map
     */
    static void addCache(@NotNull Map<PlayerTradeMode, Date> cache, @NotNull PlayerTradeMode playerTradeMode) {
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
        Main.TRADE_MODE_CACHE = cache;
    }

    /**
     * Fetch a shop from cache shop if present, otherwise fetch it from storage
     *
     * @param shopUuid {@link UUID} of shop
     * @return {@link Shop}
     * @throws StorageExecuteException if any issue while fetching from cache
     */
    default Shop getCacheShop(@NotNull UUID shopUuid) throws StorageExecuteException {
        Main.getMileLogger().debug("Get cache shop '" + shopUuid + "'.");
        Optional<Map.Entry<Shop, Date>> optionalShop = Main.SHOP_CACHE.entrySet()
                .stream()
                .filter(entry -> entry.getKey().getUuid().equals(shopUuid))
                .filter(entry -> entry.getValue().getTime() + Main.SHOP_DELAY > new Date().getTime())
                .findFirst();
        if (optionalShop.isPresent()) {
            Main.getMileLogger().debug("Shop '" + shopUuid + "' found.");
            return optionalShop.get().getKey();
        } else {
            Main.getMileLogger().debug("Shop '" + shopUuid + "' not found in cache, try to search it.");
            return Main.getStorage().getShop(shopUuid);
        }
    }

    /**
     * Fetch a shop from cache shop if present, otherwise fetch it from storage
     *
     * @param shopName name of {@link Shop}
     * @return {@link Shop}
     * @throws StorageExecuteException if any issue while fetching from cache
     */
    default Shop getCacheShop(@NotNull String shopName) throws StorageExecuteException {
        Main.getMileLogger().debug("Get cache shop with '" + shopName + "'.");
        Optional<Map.Entry<Shop, Date>> optionalShop = Main.SHOP_CACHE.entrySet()
                .stream()
                .filter(entry -> entry.getKey().getName().equals(shopName))
                .filter(entry -> entry.getValue().getTime() + Main.SHOP_DELAY > new Date().getTime())
                .findFirst();
        if (optionalShop.isPresent()) {
            Main.getMileLogger().debug("Shop '" + optionalShop.get().getKey().getUuid() + "' found.");
            return optionalShop.get().getKey();
        } else {
            Main.getMileLogger().debug("Shop with name '" + shopName + "' not found in cache, try to search it.");
            return Main.getStorage().getShop(shopName);
        }
    }

    /**
     * Fetch all shops from cache shop if up to date, otherwise fetch it from storage
     *
     * @return {@link List} of all {@link Shop}
     * @throws StorageExecuteException if any issue while fetching from cache
     */
    default @NotNull List<Shop> getCacheAllShops() throws StorageExecuteException {
        Main.getMileLogger().debug("Get all shop from cache.");
        if (Main.SHOP_CACHE.values().stream()
                .noneMatch(date -> date.getTime() + Main.SHOP_DELAY < new Date().getTime())) {
            Main.getMileLogger().debug("No up to date cache found, try to search it.");
            return Main.getStorage().getAllShops();
        } else {
            List<Shop> cache = new ArrayList<>(Main.SHOP_CACHE.keySet());
            Main.getMileLogger().debug("Found '" + cache.size() + "' shops in cache.");
            //  Order by name
            cache.sort(Comparator.comparing(Shop::getName));
            return cache;
        }
    }

    /**
     * Fetch all trades from trade cache if present, otherwise fetch it from storage
     *
     * @param shopUuid {@link UUID} of {@link Shop}
     * @return {@link List} of all {@link Trade} of this {@link Shop}
     * @throws StorageExecuteException if any issue while fetching from cache
     */
    default List<Trade> getCacheTrades(@NotNull UUID shopUuid) throws StorageExecuteException {
        Main.getMileLogger().debug("Get cache trades from shop '" + shopUuid + "'.");
        Optional<Map.Entry<ShopTrades, Date>> optionalTrades = Main.TRADE_CACHE.entrySet()
                .stream()
                .filter(entry -> entry.getKey().shopUuid().equals(shopUuid))
                .filter(entry -> entry.getValue().getTime() + Main.TRADE_DELAY > new Date().getTime())
                .findFirst();
        if (optionalTrades.isPresent()) {
            Main.getMileLogger().debug("Found '" + optionalTrades.get().getKey().trades().size() + "' trades.");
            return optionalTrades.get().getKey().trades();
        } else {
            Main.getMileLogger().debug("Trades for shop '" + shopUuid + "' not found in cache, try to search them.");
            return Main.getStorage().getTrades(shopUuid);
        }
    }

    /**
     * Fetch player TradeMode from trade mode cache if present, otherwise fetch it from storage
     *
     * @param playerUuid {@link UUID} of player
     * @return {@link TradeMode}
     * @throws StorageExecuteException if any issue while fetching from cache
     */
    default TradeMode getCacheTradeMode(@NotNull UUID playerUuid) throws StorageExecuteException {
        Main.getMileLogger().debug("Get cache user trade mode of '" + playerUuid + "'.");
        Optional<Map.Entry<PlayerTradeMode, Date>> optionalPlayerMode = Main.TRADE_MODE_CACHE.entrySet()
                .stream()
                .filter(entry -> entry.getKey().playerUuid().equals(playerUuid))
                .filter(entry -> entry.getValue().getTime() + Main.TRADE_MODE_DELAY > new Date().getTime())
                .findFirst();
        if (optionalPlayerMode.isPresent()) {
            Main.getMileLogger().debug("Trade mode found for player '" + playerUuid + "'.");
            return optionalPlayerMode.get().getKey().tradeMode();
        } else {
            Main.getMileLogger().debug("Trades mode for player '" + playerUuid + "' not found in cache, try to search them.");
            return Main.getStorage().getTradeMode(playerUuid);
        }
    }
}
