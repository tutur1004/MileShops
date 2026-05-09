package fr.milekat.shops.hooks.npc;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import fr.milekat.milenpc.api.classes.NPC;
import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.ShopNpc;
import fr.milekat.shops.hooks.MileNpc;
import org.bukkit.Location;

import java.io.IOException;
import java.util.UUID;

/**
 *  {
 *      "npc": {
 *          "uuid": UUID,
 *          "name": String,
 *          "location": Location,
 *          "skin" : {
 *              "texture": String,
 *              "signature": String
 *          }
 *      }
 *  }
 */
public class NPCDeserializer extends StdDeserializer<ShopNpc> {
    private final ObjectMapper mapper;

    public NPCDeserializer(ObjectMapper mapper) {
        super(ShopNpc.class);
        this.mapper = mapper;
    }

    @Override
    public ShopNpc deserialize(JsonParser p, DeserializationContext context) throws IOException {
        JsonNode node = mapper.readTree(p);
        if (node.isEmpty() || !node.isContainerNode()) return null;
        if (!node.has("uuid") || !node.has("name") || !node.has("location")) return null;
        UUID uuid = UUID.fromString(node.get("uuid").asText());
        String name = node.get("name").asText();
        Location location = mapper.treeToValue(node, Location.class);

        if (!Main.IS_NPC_LIB_LOADED) return new ShopNpc(uuid, name);

        NPC npc = MileNpc.getNpc(uuid);
        if (npc == null) {
            MileNpc.create(uuid, name, location);
        } else {
            MileNpc.teleport(uuid, location);
        }

        if (node.has("skin")) {
            JsonNode skin = node.get("skin");
            if (skin.has("texture") && skin.has("signature")) {
                MileNpc.updateSkin(uuid, skin.get("texture").asText(), skin.get("signature").asText());
            }
        }

        if (npc == null) {
            return null;
        }

        return MileNpc.getShopNpc(npc);
    }
}

