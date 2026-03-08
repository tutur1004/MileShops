package fr.milekat.shops.storage.adapter.elasticsearch.mappers.shops;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.ShopNpc;
import fr.milekat.shops.api.classes.ShopType;
import fr.milekat.utils.DateMileKat;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

/**
 * {
 *     "shop": {
 *         "uuid": {@link UUID},
 *         "name": {@link String},
 *         "npc": {@link ShopNpc},
 *         "type": {@link ShopType},
 *         "spawnIn": {@link Date},
 *         "spawnOut": {@link Date}
 *     }
 * }
 */
public class ShopDeserializer extends StdDeserializer<Shop> {
    private final ObjectMapper mapper;

    public ShopDeserializer(ObjectMapper mapper) {
        super(Shop.class);
        this.mapper = mapper;
    }

    @Override
    public Shop deserialize(JsonParser p, DeserializationContext context) throws IOException {
        JsonNode node = mapper.readTree(p);
        if (node.isEmpty() || !node.isContainerNode()) return null;

        UUID shopUuid = UUID.fromString(node.get("uuid").asText());
        String shopName = node.get("name").asText();

        ShopType shopType;
        try {
            shopType = ShopType.valueOf(node.get("type").asText());
        } catch (IllegalArgumentException e) {
            Main.getMileLogger().warning("Shop " + shopUuid + " has an invalid shop type: " +
                    node.get("type").asText());
            return null;
        }

        Shop shop = new Shop(shopUuid, shopName, shopType);

        ShopNpc npc = null;
        if (node.has("npc")) {
            npc = mapper.treeToValue(node.get("npc"), ShopNpc.class);
            if (npc != null) shop.setNpc(npc);
        }

        if (npc != null && node.has("spawnIn") && node.has("spawnOut")) {
            try {
                Date spawnIn = DateMileKat.getESStringDate(node.get("spawnIn").asText());
                shop.setSpawnIn(spawnIn);
                Date spawnOut = DateMileKat.getESStringDate(node.get("spawnOut").asText());
                shop.setSpawnOut(spawnOut);
            } catch (ParseException ignored) {}
        }

        return shop;
    }
}

