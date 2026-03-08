package fr.milekat.shops.hooks.npc;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import fr.milekat.milenpc.api.classes.NPC;
import fr.milekat.shops.api.classes.ShopNpc;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

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
public class NPCSerializer extends StdSerializer<ShopNpc> {
    private final ObjectMapper mapper;

    public NPCSerializer(ObjectMapper mapper) {
        super(ShopNpc.class);
        this.mapper = mapper;
    }

    @Override
    public void serialize(@NotNull ShopNpc npc, @NotNull JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeStartObject();
        gen.setCodec(mapper);

        gen.writeStringField("uuid", String.valueOf(npc.uuid()));
        gen.writeStringField("name", npc.name());

        gen.writeFieldName("location");
        gen.writeObject(mapper.valueToTree(npc.location()).get("location"));

        if (npc.texture() != null && npc.signature() != null) {
            ObjectNode skinNode = mapper.createObjectNode();
            skinNode.put("texture", npc.texture());
            skinNode.put("signature", npc.signature());

            if (!skinNode.isEmpty()) {
                gen.writeFieldName("skin");
                gen.writeObject(skinNode);
            }
        }

        gen.writeEndObject();
    }
}

