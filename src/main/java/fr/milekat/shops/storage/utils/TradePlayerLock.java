package fr.milekat.shops.storage.utils;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Blocks a single {@code (shopUuid, position)} trade for one specific player.
 *
 * <p>Used by the trade-uses cache warm-up: while the async preload populates the cache
 * for a player who just opened a shop, this lock prevents that player from racing ahead
 * and trading on stale (or absent) counter data. Released as soon as the relevant
 * cache entries are filled.</p>
 */
public record TradePlayerLock(@NotNull UUID shopUuid, int position, @NotNull UUID playerUuid) {
}
