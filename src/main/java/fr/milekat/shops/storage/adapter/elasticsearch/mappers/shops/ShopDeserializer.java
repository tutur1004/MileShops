package fr.milekat.shops.storage.adapter.elasticsearch.mappers.shops;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import fr.milekat.milenpc.api.classes.NPC;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.ShopType;

import java.io.IOException;
import java.util.UUID;

/**
 * {
 *     "shop": {
 *         "uuid": {@link UUID},
 *         "name": {@link String},
 *         "npc": {@link NPC},
 *         "type": {@link ShopType}
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
        NPC npc = mapper.treeToValue(node.get("npc"), NPC.class);
        if (npc == null) return null;
        ShopType shopType = ShopType.valueOf(node.get("type").asText());

        return new Shop(shopUuid, shopName, npc, shopType);
    }
}

