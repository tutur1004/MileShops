package fr.milekat.shops.storage.utils;

import java.util.Date;

/**
 * Cached value for a {@link TradeUsesKey}: the counted number of past trades and the
 * timestamp at which it was fetched from storage. The TTL check uses {@code fetchedAt}
 * against {@code Main.TRADE_USES_DELAY}.
 */
public record TradeUsesEntry(int count, Date fetchedAt) {
}
