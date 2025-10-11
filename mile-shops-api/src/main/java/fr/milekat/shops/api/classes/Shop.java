package fr.milekat.shops.api.classes;

import fr.milekat.milenpc.api.classes.NPC;
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
    private NPC npc;
    private ShopType type;
    private Date spawnIn;
    private Date spawnOut;

    /**
     * Constructs a new Shop instance with the specified UUID, name, NPC UUID, and shop type.
     *
     * @param uuid The UUID of the shop.
     * @param name The name of the shop.
     * @param npc  The UUID of the NPC associated with the shop, or null if no NPC is associated.
     * @param type The type of the shop.
     */
    public Shop(@NotNull UUID uuid, @NotNull String name, @Nullable NPC npc, @NotNull ShopType type) {
        this.uuid = uuid;
        this.name = name;
        this.npc = npc;
        this.type = type;
    }

    /**
     * Constructs a new Shop instance with the specified UUID, name, NPC UUID, shop type, spawn in date, and spawn out date.
     *
     * @param uuid The UUID of the shop.
     * @param name The name of the shop.
     * @param npc  The UUID of the NPC associated with the shop.
     * @param type The type of the shop.
     * @param spawnIn The date when the shop will spawn in.
     * @param spawnOut The date when the shop will spawn out.
     */
    public Shop(@NotNull UUID uuid, @NotNull String name, @NotNull NPC npc, @NotNull ShopType type,
                @NotNull Date spawnIn, @NotNull Date spawnOut) {
        this.uuid = uuid;
        this.name = name;
        this.npc = npc;
        this.type = type;
        this.spawnIn = spawnIn;
        this.spawnOut = spawnOut;
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
    public NPC getNpc() {
        return npc;
    }

    /**
     * Sets the UUID of the NPC associated with the shop.
     *
     * @param npc The new UUID of the NPC associated with the shop.
     */
    public void setNpc(@Nullable NPC npc) {
        this.npc = npc;
    }

    /**
     * Removes the NPC associated with the shop, if any.
     */
    public void removeNpc() {
        if (npc != null) npc.remove();
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
        return npc != null && spawnIn != null && spawnOut != null;
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
