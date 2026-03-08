package fr.milekat.shops.api.classes;

import fr.milekat.shops.api.MileShopsAPI;
import fr.milekat.shops.api.exceptions.ApiUnavailable;
import fr.milekat.shops.api.exceptions.StorageException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Represents a shop in the CustomShops system.
 */
@SuppressWarnings("unused")
public class Shop {
    private final UUID uuid;
    private String name;
    private ShopNpc shopNpc;
    private ShopType type;
    private Date spawnIn;
    private Date spawnOut;


    /**
     * Constructs a new Shop instance with the specified UUID, name, and shop type.
     *
     * @param uuid The UUID of the shop.
     * @param name The name of the shop.
     * @param type The type of the shop.
     */
    public Shop(@NotNull UUID uuid, @NotNull String name, @NotNull ShopType type) {
        this.uuid = uuid;
        this.name = name;
        this.type = type;
    }

    /**
     * Constructs a new Shop instance with the specified UUID, name, NPC UUID, and shop type.
     *
     * @param uuid The UUID of the shop.
     * @param name The name of the shop.
     * @param shopNpc  The ShopNpc associated with the shop (can be null).
     * @param type The type of the shop.
     */
    public Shop(@NotNull UUID uuid, @NotNull String name, @Nullable ShopNpc shopNpc, @NotNull ShopType type) {
        this.uuid = uuid;
        this.name = name;
        this.shopNpc = shopNpc;
        this.type = type;
    }

    /**
     * Retrieves the UUID of the shop.
     *
     * @return The UUID of the shop.
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Retrieves the name of the shop.
     *
     * @return The name of the shop.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the shop.
     *
     * @param name The new name of the shop.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Retrieves the UUID of the NPC associated with the shop.
     *
     * @return The UUID of the NPC associated with the shop.
     */
    @Nullable
    public ShopNpc getNpc() {
        return shopNpc;
    }

    /**
     * Sets the UUID of the NPC associated with the shop.
     *
     * @param shopNpc The new UUID of the NPC associated with the shop.
     */
    public void setNpc(@Nullable ShopNpc shopNpc) {
        this.shopNpc = shopNpc;
    }

    /**
     * Retrieves the type of the shop.
     *
     * @return The type of the shop.
     */
    @NotNull
    public ShopType getType() {
        return type;
    }

    /**
     * Sets the type of the shop.
     *
     * @param type The new type of the shop.
     */
    public void setType(@NotNull ShopType type) {
        this.type = type;
    }

    /**
     * Retrieves a list of trades associated with the shop.
     *
     * @return The list of trades associated with the shop.
     * @throws ApiUnavailable       if the CustomShops API is unavailable.
     * @throws StorageException     if there is an error accessing the storage.
     */
    public List<Trade> getTrades() throws ApiUnavailable, StorageException {
        return MileShopsAPI.getAPI().getShopTrades(this.uuid);
    }

    /**
     * Retrieves whether the shop is timed.
     *
     * @return Whether the shop is timed.
     */
    public boolean isTimed() {
        return shopNpc != null && spawnIn != null && spawnOut != null;
    }

    /**
     * Retrieves whether the shop should be spawned in.
     *
     * @return Whether the shop should be spawned in.
     */
    public boolean shouldBeSpawned() {
        return isTimed() && spawnIn.before(new Date()) && spawnOut.after(new Date());
    }

    /**
     * Retrieves the date when the shop will spawn in.
     *
     * @return The date when the shop will spawn in.
     */
    @Nullable
    public Date getSpawnIn() {
        return spawnIn;
    }

    /**
     * Sets the date when the shop will spawn in.
     *
     * @param spawnIn The date when the shop will spawn in.
     */
    public void setSpawnIn(Date spawnIn) {
        this.spawnIn = spawnIn;
    }

    /**
     * Retrieves the date when the shop will spawn out.
     *
     * @return The date when the shop will spawn out.
     */
    @Nullable
    public Date getSpawnOut() {
        return spawnOut;
    }

    /**
     * Sets the date when the shop will spawn out.
     *
     * @param spawnOut The date when the shop will spawn out.
     */
    public void setSpawnOut(Date spawnOut) {
        this.spawnOut = spawnOut;
    }
}
