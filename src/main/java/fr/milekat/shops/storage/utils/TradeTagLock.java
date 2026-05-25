package fr.milekat.shops.storage.utils;

import fr.milekat.shops.Main;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Blocks a {@code (shopUuid, position)} trade for any player that holds
 * {@code (tagName, tagValue)} on {@link Main#PLAYER_TAGS}.
 *
 * <p>Held by the public API ({@code MileShopsIAPI.lockTrade}). Unlike
 * {@link TradePlayerLock}, this scope applies to every matching player at once
 * (e.g. lock everyone in {@code team=red}). Also applies to trades with no
 * usage limit configured.</p>
 */
public record TradeTagLock(@NotNull UUID shopUuid, int position,
                           @NotNull String tagName, @NotNull Object tagValue) {
}
