package fr.milekat.shops.storage.adapter.elasticsearch.mappers.milekat.shops;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import dev.sergiferry.playernpc.api.NPC;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.ShopType;

import java.io.IOException;
import java.util.UUID;

/**
 * {
 *     "shop": {
 *         "playerUuid": UUID,
 *         "name": String,
 *         "npc": NPC.Global,
 *         "type": ShopType
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
        NPC.Global npc = mapper.treeToValue(node.get("npc"), NPC.Global.class);
        ShopType shopType = ShopType.valueOf(node.get("type").asText());

        return new Shop(shopUuid, shopName, UUID.fromString(npc.getSimpleID()), shopType);
    }
}
