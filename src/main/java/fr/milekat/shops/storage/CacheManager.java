package fr.milekat.shops.storage;

import dev.sergiferry.playernpc.api.NPC;
import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.storage.exeptions.StorageExecuteException;
import fr.milekat.shops.storage.utils.PlayerTradeMode;
import fr.milekat.shops.storage.utils.ShopTrades;
import fr.milekat.shops.workers.utils.TradeMode;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("unused")
public interface CacheManager {
    /**
     * Fetch a shop from cache shop if present, otherwise fetch it from storage
     * @param shopUuid {@link UUID} of shop
     * @return {@link Shop}
     * @throws StorageExecuteException if any issue while fetching from cache
     */
    default Shop getCacheShop(@NotNull UUID shopUuid) throws StorageExecuteException {
        Main.debug("Get cache shop '" + shopUuid + "'.");
        Optional<Map.Entry<Shop, Date>> optionalShop = Storage.SHOP_CACHE.entrySet()
                .stream()
                .filter(entry -> entry.getKey().getUuid().equals(shopUuid))
                .filter(entry -> entry.getValue().getTime() + Storage.SHOP_DELAY > new Date().getTime())
                .findFirst();
        if (optionalShop.isPresent()) {
            Main.debug("Shop '" + shopUuid + "' found.");
            return optionalShop.get().getKey();
        } else  {
            Main.debug("Shop '" + shopUuid + "' not found in cache, try to search it.");
            return Main.getStorage().getShop(shopUuid);
        }
    }

    /**
     * Fetch a shop from cache shop if present, otherwise fetch it from storage
     * @param npcUuid {@link UUID} of {@link NPC.Global}
     * @return {@link Shop}
     * @throws StorageExecuteException if any issue while fetching from cache
     */
    default Shop getCacheShopNpc(@NotNull UUID npcUuid) throws StorageExecuteException {
        Main.debug("Get cache shop of npc '" + npcUuid + "'.");
        Optional<Map.Entry<Shop, Date>> optionalShop = Storage.SHOP_CACHE.entrySet()
                .stream()
                .filter(entry -> entry.getKey().getNpc().getSimpleID().equals(npcUuid.toString()))
                .filter(entry -> entry.getValue().getTime() + Storage.SHOP_DELAY > new Date().getTime())
                .findFirst();
        if (optionalShop.isPresent()) {
            Main.debug("Shop '" + optionalShop.get().getKey().getUuid() + "' found.");
            return optionalShop.get().getKey();
        } else  {
            Main.debug("Shop of npc '" + npcUuid + "' not found in cache, try to search it.");
            return Main.getStorage().getShopNpc(npcUuid);
        }
    }

    /**
     * Fetch a shop from cache shop if present, otherwise fetch it from storage
     * @param shopName name of {@link Shop}
     * @return {@link Shop}
     * @throws StorageExecuteException if any issue while fetching from cache
     */
    default Shop getCacheShop(@NotNull String shopName) throws StorageExecuteException {
        Main.debug("Get cache shop with '" + shopName + "'.");
        Optional<Map.Entry<Shop, Date>> optionalShop = Storage.SHOP_CACHE.entrySet()
                .stream()
                .filter(entry -> entry.getKey().getName().equals(shopName))
                .filter(entry -> entry.getValue().getTime() + Storage.SHOP_DELAY > new Date().getTime())
                .findFirst();
        if (optionalShop.isPresent()) {
            Main.debug("Shop '" + optionalShop.get().getKey().getUuid() + "' found.");
            return optionalShop.get().getKey();
        } else  {
            Main.debug("Shop with name '" + shopName + "' not found in cache, try to search it.");
            return Main.getStorage().getShop(shopName);
        }
    }

    /**
     * Fetch all shops from cache shop if up to date, otherwise fetch it from storage
     * @return {@link List} of all {@link Shop}
     * @throws StorageExecuteException if any issue while fetching from cache
     */
    default List<Shop> getCacheAllShops() throws StorageExecuteException {
        Main.debug("Get all shop from cache.");
        if (Storage.SHOP_CACHE.values().stream()
                .noneMatch(date -> date.getTime() + Storage.SHOP_DELAY < new Date().getTime())) {
            Main.debug("No up to date cache found, try to search it.");
            return Main.getStorage().getAllShops();
        } else {
            Main.debug("Found '" + Storage.SHOP_CACHE.keySet().size() + "' shops in cache.");
            return new ArrayList<>(Storage.SHOP_CACHE.keySet());
        }
    }

    /**
     * Fetch all trades from trade cache if present, otherwise fetch it from storage
     * @param shopUuid {@link UUID} of {@link Shop}
     * @return {@link List} of all {@link Trade} of this {@link Shop}
     * @throws StorageExecuteException if any issue while fetching from cache
     */
    default List<Trade> getCacheTrades(@NotNull UUID shopUuid) throws StorageExecuteException {
        Main.debug("Get cache trades from shop '" + shopUuid + "'.");
        Optional<Map.Entry<ShopTrades, Date>> optionalTrades = Storage.TRADE_CACHE.entrySet()
                .stream()
                .filter(entry -> entry.getKey().shopUuid().equals(shopUuid))
                .filter(entry -> entry.getValue().getTime() + Storage.TRADE_DELAY > new Date().getTime())
                .findFirst();
        if (optionalTrades.isPresent()) {
            Main.debug("Found '" + optionalTrades.get().getKey().trades().size() + "' trades.");
            return optionalTrades.get().getKey().trades();
        } else {
            Main.debug("Trades for shop '" + shopUuid + "' not found in cache, try to search them.");
            return Main.getStorage().getTrades(shopUuid);
        }
    }

    /**
     * Fetch player TradeMode from trade mode cache if present, otherwise fetch it from storage
     * @param playerUuid {@link UUID} of player
     * @return {@link TradeMode}
     * @throws StorageExecuteException if any issue while fetching from cache
     */
    default TradeMode getCacheTradeMode(@NotNull UUID playerUuid) throws StorageExecuteException {
        Main.debug("Get cache user trade mode of '" + playerUuid + "'.");
        Optional<Map.Entry<PlayerTradeMode, Date>> optionalPlayerMode = Storage.TRADE_MODE_CACHE.entrySet()
                .stream()
                .filter(entry -> entry.getKey().playerUuid().equals(playerUuid))
                .filter(entry -> entry.getValue().getTime() + Storage.TRADE_MODE_DELAY > new Date().getTime())
                .findFirst();
        if (optionalPlayerMode.isPresent()) {
            Main.debug("Trade mode found for player '" + playerUuid + "'.");
            return optionalPlayerMode.get().getKey().tradeMode();
        } else {
            Main.debug("Trades mode for player '" + playerUuid + "' not found in cache, try to search them.");
            return Main.getStorage().getTradeMode(playerUuid);
        }
    }

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
        Storage.SHOP_CACHE = cache;
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
        Storage.TRADE_CACHE = cache;
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
        Storage.TRADE_MODE_CACHE = cache;
    }
}
