package fr.milekat.shops.api.classes;

import fr.milekat.shops.api.CustomShopsAPI;
import fr.milekat.shops.api.exeptions.CustomShopsApiUnavailable;
import fr.milekat.shops.api.exeptions.StorageException;

import java.util.List;
import java.util.UUID;

/**
 * Represents a shop in the CustomShops system.
 */
public class Shop {
    private final UUID uuid;
    private String name;
    private UUID npc;
    private ShopType type;

    /**
     * Constructs a new Shop instance with the specified name, NPC UUID, and shop type.
     *
     * @param name The name of the shop.
     * @param npc  The UUID of the NPC associated with the shop.
     * @param type The type of the shop.
     */
    public Shop(String name, UUID npc, ShopType type) {
        this.uuid = UUID.randomUUID();
        this.name = name;
        this.npc = npc;
        this.type = type;
    }

    /**
     * Constructs a new Shop instance with the specified UUID, name, NPC UUID, and shop type.
     *
     * @param uuid The UUID of the shop.
     * @param name The name of the shop.
     * @param npc  The UUID of the NPC associated with the shop.
     * @param type The type of the shop.
     */
    public Shop(UUID uuid, String name, UUID npc, ShopType type) {
        this.uuid = uuid;
        this.name = name;
        this.npc = npc;
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
    public UUID getNpc() {
        return npc;
    }

    /**
     * Sets the UUID of the NPC associated with the shop.
     *
     * @param npc The new UUID of the NPC associated with the shop.
     */
    public void setNpc(UUID npc) {
        this.npc = npc;
    }

    /**
     * Retrieves the type of the shop.
     *
     * @return The type of the shop.
     */
    public ShopType getType() {
        return type;
    }

    /**
     * Sets the type of the shop.
     *
     * @param type The new type of the shop.
     */
    public void setType(ShopType type) {
        this.type = type;
    }

    /**
     * Retrieves a list of trades associated with the shop.
     *
     * @return The list of trades associated with the shop.
     * @throws CustomShopsApiUnavailable if the CustomShops API is unavailable.
     * @throws StorageException          if there is an error accessing the storage.
     */
    public List<Trade> getTrades() throws CustomShopsApiUnavailable, StorageException {
        return CustomShopsAPI.getAPI().getShopTrades(this.uuid);
    }
}
