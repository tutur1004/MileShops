package fr.milekat.shops.api;

import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.TagValue;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.api.classes.TradeMode;
import fr.milekat.shops.api.exceptions.StorageException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The MileShopsIAPI interface provides access to the CustomShops API.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface MileShopsIAPI {
    /**
     * Checks if the API is running in debug mode.
     *
     * @return true if the API is in debug mode, false otherwise.
     */
    boolean isDebug();

    /**
     * Retrieves a list of all shops.
     *
     * @return The list of shops.
     * @throws StorageException if there is an error accessing the storage.
     */
    @NotNull
    List<Shop> getShops() throws StorageException;

    /**
     * Retrieves a list of trades associated with the specified shop.
     *
     * @param shop The shop to retrieve trades for.
     * @return The list of trades for the shop.
     * @throws StorageException if there is an error accessing the storage.
     */
    @NotNull
    List<Trade> getShopTrades(@NotNull Shop shop) throws StorageException;
    /**
     * Retrieves a list of trades associated with the shop identified by UUID.
     *
     * @param uuid The UUID of the shop.
     * @return The list of trades for the shop.
     * @throws StorageException if there is an error accessing the storage.
     */
    @NotNull
    List<Trade> getShopTrades(@NotNull UUID uuid) throws StorageException;
    /**
     * Retrieves a list of trades associated with the shop identified by name.
     *
     * @param name The name of the shop.
     * @return The list of trades for the shop.
     * @throws StorageException if there is an error accessing the storage.
     */
    @NotNull
    List<Trade> getShopTrades(@NotNull String name) throws StorageException;

    /**
     * Process a single trade for a player, using the player's inventory for the trade.
     *
     * @param player performing the trade
     * @param shop the shop where the trade is taking place
     * @param trade to process
     * @return true if the trade has been processed, false otherwise.
     */
    default boolean processedTrade(@NotNull Player player, @NotNull Shop shop, @NotNull Trade trade) {
        return processedTrades(player, TradeMode.INVENTORY, shop, trade, false) > 0;
    }
    /**
     * Process trade(s) for a player, with the option to process multiple trades if possible.
     *
     * @param player performing the trade
     * @param tradeMode the mode of trade to process, determining which inventories to consider for the trade
     * @param shop the shop where the trade is taking place
     * @param trade to process
     * @param multiple if true, the method will trade as much has the player can
     * @return the number of trade processed
     */
    int processedTrades(@NotNull Player player, @NotNull TradeMode tradeMode,
                        @NotNull Shop shop, @NotNull Trade trade, boolean multiple);

    /**
     * Opens the shop interface for the player identified by UUID.
     *
     * @param uuid The UUID of the player.
     * @param shop The shop to open.
     *
     * @return true if the shop was successfully opened, false otherwise.
     */
    boolean openShop(@NotNull UUID uuid, @NotNull Shop shop);
    /**
     * Opens the admin shop interface for the player identified by UUID.
     *
     * @param uuid The UUID of the player.
     * @param shop The shop to open in admin mode.
     *
     * @return true if the admin shop was successfully opened, false otherwise.
     */
    boolean openAdminShop(@NotNull UUID uuid, @NotNull Shop shop);

    /**
     * Retrieves the tags associated with a player identified by UUID.
     *
     * @param uuid The UUID of the player.
     * @return The player's tags as a map, or null if no tags are found.
     */
    @Nullable
    Map<String, Object> getPlayerTags(@NotNull UUID uuid);
    /**
     * Removes the tags associated with a player identified by UUID.
     *
     * @param uuid The UUID of the player.
     */
    void removePlayerTags(@NotNull UUID uuid);
    /**
     * Sets the tags associated with a player identified by UUID.
     *
     * @param uuid The UUID of the player.
     * @param tags The tags to set for the player.
     */
    void setPlayerTags(@NotNull UUID uuid, @NotNull Map<String, Object> tags);

    /**
     * Returns the names of the player-tags configured on this server — the exact set the
     * admin editor offers in its USES_TAG sub-editor and the same set that gates
     * {@link Trade#getMaxTradeUses()} and the lock-API methods.
     *
     * <p>Use this to discover which {@code tagName} values are valid arguments for
     * {@link #lockTrade}, {@link #unlockTrade} and {@link #refreshTradeUses}.</p>
     *
     * @return An unmodifiable view of the configured tag names. Empty if none are configured.
     */
    @NotNull
    Set<String> getAvailableTags();

    /**
     * Invalidate the cached usage count for a specific {@code (trade, tag, value)} tuple.
     * The next limit check will re-fetch the count from storage.
     *
     * <p>Use this when an external plugin has mutated the underlying trade-history
     * out-of-band (e.g. a cross-server reset of a "team" counter) so this server's
     * in-memory cache doesn't keep serving the stale count.</p>
     *
     * @param trade    The trade whose cached usage count should be invalidated.
     * @param tagName  The name of the player-tag whose limit should be refreshed.
     * @param tagValue The value of that tag, wrapped via one of the
     *                 {@link TagValue#of(String) TagValue.of(...)} factories. Its boxed
     *                 type must match the type registered for {@code tagName} in
     *                 {@code Main.TAGS}, otherwise the lookup will simply not match.
     */
    void refreshTradeUses(@NotNull Trade trade,
                          @NotNull String tagName, @NotNull TagValue tagValue);

    /**
     * Acquires a lock on a single trade scoped to a specific player-tag value. As long
     * as the lock is held, any player whose {@code Main.PLAYER_TAGS} contains
     * {@code tagName} equal to {@code tagValue} will be denied execution of that trade.
     *
     * <p>This is independent of the usage-limit map on the {@link Trade}: locks apply
     * even to trades with no {@code maxTradeUses} configured.</p>
     *
     * @param trade    The trade to lock.
     * @param tagName  The player-tag name to lock against (must be a key registered in
     *                 the {@code tags} config — i.e. one of {@code Main.TAGS}).
     * @param tagValue The value of that tag, wrapped via {@link TagValue#of}. Only players
     *                 whose live tag value {@link Object#equals(Object) equals} the boxed
     *                 underlying value are blocked; the wrapper itself enforces that the
     *                 type is one of the supported shapes
     *                 ({@code String / int / long / double / boolean}).
     */
    void lockTrade(@NotNull Trade trade,
                   @NotNull String tagName, @NotNull TagValue tagValue);

    /**
     * Releases a lock previously acquired through {@link #lockTrade} and, by design,
     * also drops the cached usage count for the same key — so the next limit check
     * re-fetches from storage instead of trusting the value that was potentially
     * mutated while the lock was held.
     *
     * @param trade    The trade to unlock.
     * @param tagName  The player-tag name that was used to acquire the lock.
     * @param tagValue The tag value that was used to acquire the lock, wrapped via
     *                 {@link TagValue#of}. Must equal (boxed value + type) the one passed
     *                 to the originating {@link #lockTrade}.
     */
    void unlockTrade(@NotNull Trade trade,
                     @NotNull String tagName, @NotNull TagValue tagValue);

    /**
     * Tests whether a given trade is currently locked for a specific player. A trade
     * is considered locked when either:
     * <ul>
     *   <li>A cache warm-up is in progress for that player on that trade — i.e. they
     *       just opened a shop that triggered the async preload of their per-tag usage
     *       counts and the relevant entry hasn't settled yet.</li>
     *   <li>One of the player's tag values matches an active API lock placed via
     *       {@link #lockTrade}.</li>
     * </ul>
     *
     * @param trade      The trade to test.
     * @param playerUuid The UUID of the player to test against. The player's tags are
     *                   resolved from {@code Main.PLAYER_TAGS}; an unknown player is
     *                   considered unlocked unless a warm-up lock matches the UUID
     *                   directly.
     * @return {@code true} if any active lock would block this player from this trade,
     *         {@code false} otherwise.
     */
    boolean isTradeLocked(@NotNull Trade trade, @NotNull UUID playerUuid);

    /**
     * Get the current player modifier
     *
     * @param uuid The UUID of the player.
     * @return the current player modifier, where 1.0 means no modification,
     * values greater than 1.0 increase the amount received, and values less than 1.0 decrease it.
     */
    double getPlayerModifier(@NotNull UUID uuid);
    /**
     * Reset the player modifier to 1.0 (100%)
     * @param uuid The UUID of the player.
     */
    default void resetPlayerModifier(@NotNull UUID uuid) {
        setPlayerModifier(uuid, 1.0);
    }
    /**
     * Set player trade result modifier, to increase or decrease the amount received (Rounded to int)
     * Value can be positive or negative, but it's only impact money trades
     *
     * @param uuid The UUID of the player.
     * @param modifier The modifier to set for the player, where 1.0 means no modification,
     *                 values greater than 1.0 increase the amount received, and values less than 1.0 decrease it.
     */
    void setPlayerModifier(@NotNull UUID uuid, double modifier);
}
