package fr.milekat.shops.storage.adapter.elasticsearch.mappers.shops;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import fr.milekat.milenpc.api.classes.NPC;
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
public class NPCSerializer extends StdSerializer<NPC> {
    private final ObjectMapper mapper;

    public NPCSerializer(ObjectMapper mapper) {
        super(NPC.class);
        this.mapper = mapper;
    }

    @Override
    public void serialize(@NotNull NPC npc, @NotNull JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeStartObject();
        gen.setCodec(mapper);

        gen.writeStringField("uuid", String.valueOf(npc.getUuid()));
        gen.writeStringField("name", npc.getName());

        gen.writeFieldName("location");
        gen.writeObject(mapper.valueToTree(npc.getLocation()).get("location"));

        if (npc.getTexture() != null && npc.getSignature() != null) {
            ObjectNode skinNode = mapper.createObjectNode();
            skinNode.put("texture", npc.getTexture());
            skinNode.put("signature", npc.getSignature());

            if (!skinNode.isEmpty()) {
                gen.writeFieldName("skin");
                gen.writeObject(skinNode);
            }
        }

        gen.writeEndObject();
    }
}

