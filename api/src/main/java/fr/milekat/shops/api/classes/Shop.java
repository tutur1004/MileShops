package fr.milekat.shops.api.classes;

import fr.milekat.shops.api.CustomShopsAPI;
import fr.milekat.shops.api.exeptions.CustomShopsApiUnavailable;
import fr.milekat.shops.api.exeptions.StorageException;

import java.util.List;
import java.util.UUID;

public class Shop {
    private final UUID uuid;
    private String name;
    private UUID npc;
    private ShopType type;

    public Shop(String name, UUID npc, ShopType type) {
        this.uuid = UUID.randomUUID();
        this.name = name;
        this.npc = npc;
        this.type = type;
    }

    public Shop(UUID uuid, String name, UUID npc, ShopType type) {
        this.uuid = uuid;
        this.name = name;
        this.npc = npc;
        this.type = type;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getNpc() {
        return npc;
    }

    public void setNpc(UUID npc) {
        this.npc = npc;
    }

    public ShopType getType() {
        return type;
    }

    public void setType(ShopType type) {
        this.type = type;
    }

    public List<Trade> getTrades() throws CustomShopsApiUnavailable, StorageException {
        return CustomShopsAPI.getAPI().getShopTrades(this.uuid);
    }
}
