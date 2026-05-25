package fr.milekat.shops.storage.utils;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Identifies a per-tag trade-usage counter: how many times trade {@code (shopUuid, position)}
 * has been logged for the player-tag value {@code (tagName, tagValue)}.
 *
 * <p>Used as the key of {@code Main.TRADE_USES_CACHE}. {@code tagValue} mirrors whatever
 * type the tag carries in {@link fr.milekat.shops.Main#TAGS} (String / Integer / Long /
 * Double / Boolean), all of which implement {@code equals} / {@code hashCode}.</p>
 */
public record TradeUsesKey(@NotNull UUID shopUuid, int position,
                           @NotNull String tagName, @NotNull Object tagValue) {
}
