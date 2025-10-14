package fr.milekat.shops.storage.adapter.elasticsearch.mappers.shops;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import fr.milekat.milenpc.api.classes.NPC;
import fr.milekat.shops.Main;
import fr.milekat.shops.workers.utils.NPCUtils;
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
public class NPCDeserializer extends StdDeserializer<NPC> {
    private final ObjectMapper mapper;

    public NPCDeserializer(ObjectMapper mapper) {
        super(NPC.class);
        this.mapper = mapper;
    }

    @Override
    public NPC deserialize(JsonParser p, DeserializationContext context) throws IOException {
        JsonNode node = mapper.readTree(p);
        if (node.isEmpty() || !node.isContainerNode()) return null;
        if (!node.has("uuid") || !node.has("name") || !node.has("location")) return null;
        UUID uuid = UUID.fromString(node.get("uuid").asText());
        String name = node.get("name").asText();
        Location location = mapper.treeToValue(node, Location.class);

        if (!Main.IS_NPC_LIB_LOADED) return new NPC(uuid, name);

        NPC npc = Main.getNpc(uuid);
        if (npc == null) {
            NPCUtils.create(uuid, name, location);
        } else {
            NPCUtils.teleport(uuid, location);
        }

        if (node.has("skin")) {
            JsonNode skin = node.get("skin");
            if (skin.has("texture") && skin.has("signature")) {
                NPCUtils.updateSkin(uuid, skin.get("texture").asText(), skin.get("signature").asText());
            }
        }
        return npc;
    }
}

