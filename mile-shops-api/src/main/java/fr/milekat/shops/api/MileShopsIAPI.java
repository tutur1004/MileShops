package fr.milekat.shops.api;

import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.api.classes.TradeMode;
import fr.milekat.shops.api.exceptions.StorageException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
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
     * Checks if the player identified by UUID has a malus.
     * @param uuid The UUID of the player.
     * @return true if the player has currently a malus applied
     */
    boolean hasPlayerMalus(@NotNull UUID uuid);
    /**
     * Checks if the player identified by UUID has a malus.
     *
     * @param uuid The UUID of the player.
     * @return true if the player has a malus, false otherwise.
     */
    void setPlayerMalus(@NotNull UUID uuid, boolean malus);
}
