package fr.milekat.shops.api;

import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.api.exceptions.StorageException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The CustomShopsIAPI interface provides access to the CustomShops API functionality.
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
     * Retrieves a list of trades associated with the NPC shop identified by UUID.
     *
     * @param uuid The UUID of the NPC shop.
     * @return The list of trades for the NPC shop.
     * @throws StorageException if there is an error accessing the storage.
     */
    @NotNull
    List<Trade> getNpcShopTrades(@NotNull UUID uuid) throws StorageException;

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
}
