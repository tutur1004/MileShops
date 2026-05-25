package fr.milekat.shops.storage;

import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.storage.utils.PlayerTradeMode;
import fr.milekat.shops.storage.utils.ShopTrades;
import fr.milekat.shops.storage.utils.TradePlayerLock;
import fr.milekat.shops.storage.utils.TradeTagLock;
import fr.milekat.shops.storage.utils.TradeUsesEntry;
import fr.milekat.shops.storage.utils.TradeUsesKey;
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

    /**
     * Cache-aware variant of {@link StorageImplementation#getTradeUses(Map, Trade)}
     * used by the trade-limit check. Falls back to a direct storage call when:
     * <ul>
     *   <li>{@link Main#TRADE_USES_DELAY} is {@code 0} (cache disabled by config), or</li>
     *   <li>{@code tags} doesn't contain exactly one entry — we only cache per-single-tag
     *       lookups, since limits are checked tag-by-tag.</li>
     * </ul>
     *
     * <p>On cache hit, returns the cached count without touching storage. On miss or
     * expired entry, performs the storage call and stores the fresh result.</p>
     */
    default int getCacheTradeUses(@NotNull Map<String, Object> tags,
                                  @NotNull Trade trade) {
        if (Main.TRADE_USES_DELAY <= 0 || tags.size() != 1) {
            return Main.getStorage().getTradeUses(tags, trade);
        }
        Map.Entry<String, Object> e = tags.entrySet().iterator().next();
        TradeUsesKey key = new TradeUsesKey(trade.getShopUuid(), trade.getTradePosition(),
                e.getKey(), e.getValue());
        TradeUsesEntry entry = Main.TRADE_USES_CACHE.get(key);
        if (entry != null
                && entry.fetchedAt().getTime() + Main.TRADE_USES_DELAY > System.currentTimeMillis()) {
            Main.getMileLogger().debug("[Cache] trade-uses hit '" + key + "' = " + entry.count());
            return entry.count();
        }
        int count = Main.getStorage().getTradeUses(tags, trade);
        Main.TRADE_USES_CACHE.put(key, new TradeUsesEntry(count, new Date()));
        return count;
    }

    /**
     * Drops the cache entry for a specific {@code (shop, position, tag, value)} tuple
     * so the next lookup re-fetches from storage. Useful for external plugins that have
     * mutated the underlying count out-of-band.
     */
    static void invalidateTradeUses(@NotNull UUID shopUuid, int position,
                                    @NotNull String tagName, @NotNull Object tagValue) {
        Main.TRADE_USES_CACHE.remove(new TradeUsesKey(shopUuid, position, tagName, tagValue));
    }

    /**
     * Increments the cached count for {@code (shop, position, tag, value)} by {@code delta}
     * if an entry exists. Used right after a successful trade is logged so the next
     * lookup doesn't have to re-hit storage. No-op when no entry is cached — we don't
     * pre-create an entry from a partial increment because the base count is unknown.
     */
    static void bumpTradeUses(@NotNull UUID shopUuid, int position,
                              @NotNull String tagName, @NotNull Object tagValue, int delta) {
        Main.TRADE_USES_CACHE.computeIfPresent(
                new TradeUsesKey(shopUuid, position, tagName, tagValue),
                (k, e) -> new TradeUsesEntry(e.count() + delta, e.fetchedAt())
        );
    }

    // =========================================================================
    //  Trade locks — warm-up (per-player) + API (per-tag-value)
    // =========================================================================

    /** Acquire a warm-up lock blocking this single player from this trade. */
    static void lockWarmup(@NotNull UUID shopUuid, int position, @NotNull UUID playerUuid) {
        Main.TRADE_WARMUP_LOCKS.add(new TradePlayerLock(shopUuid, position, playerUuid));
    }

    /** Release the warm-up lock for this player + trade. */
    static void unlockWarmup(@NotNull UUID shopUuid, int position, @NotNull UUID playerUuid) {
        Main.TRADE_WARMUP_LOCKS.remove(new TradePlayerLock(shopUuid, position, playerUuid));
    }

    /** Acquire a per-tag-value API lock; any player carrying {@code (tagName, tagValue)} is blocked. */
    static void lockTrade(@NotNull UUID shopUuid, int position,
                          @NotNull String tagName, @NotNull Object tagValue) {
        Main.TRADE_API_LOCKS.add(new TradeTagLock(shopUuid, position, tagName, tagValue));
    }

    /**
     * Release a per-tag-value API lock and drop the matching trade-uses cache entry so
     * the next limit check re-fetches from storage. The invalidation is unconditional:
     * a plugin generally locks because it is mutating the underlying counter, so the
     * cached value can no longer be trusted once the lock is released.
     */
    static void unlockTrade(@NotNull UUID shopUuid, int position,
                            @NotNull String tagName, @NotNull Object tagValue) {
        Main.TRADE_API_LOCKS.remove(new TradeTagLock(shopUuid, position, tagName, tagValue));
        invalidateTradeUses(shopUuid, position, tagName, tagValue);
    }

    /**
     * True if any active lock — warm-up for this player, or API lock matching one of the
     * player's tag values — blocks this trade for this player.
     */
    static boolean isTradeLockedForPlayer(@NotNull UUID shopUuid, int position,
                                          @NotNull UUID playerUuid,
                                          @NotNull Map<String, Object> playerTags) {
        if (Main.TRADE_WARMUP_LOCKS.contains(new TradePlayerLock(shopUuid, position, playerUuid))) {
            return true;
        }
        if (Main.TRADE_API_LOCKS.isEmpty()) return false;
        for (Map.Entry<String, Object> e : playerTags.entrySet()) {
            if (Main.TRADE_API_LOCKS.contains(
                    new TradeTagLock(shopUuid, position, e.getKey(), e.getValue()))) {
                return true;
            }
        }
        return false;
    }
}
