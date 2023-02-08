package fr.milekat.shops.storage.adapter.elasticsearch.mappers.minecraft;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.stream.IntStream;

/**
 * Json model
 * {
 *     "inventory": {
 *         "type": "InventoryType",
 *         "slots": [
 *         {
 *             "slot": int,
 *             "itemStack": object
 *         }
 *         ]
 *     }
 * }
 */
public class InventorySerializer extends StdSerializer<Inventory> {
    private final ObjectMapper mapper;

    public InventorySerializer(ObjectMapper mapper) {
        super(Inventory.class);
        this.mapper = mapper;
    }

    @Override
    public void serialize(@Nonnull Inventory value, @NotNull JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeStartObject();
        gen.setCodec(mapper);
        gen.writeStringField("type", String.valueOf(value.getType()));
        ArrayNode slots = mapper.createArrayNode();
        IntStream.range(0, value.getSize()).forEach(slot-> {
            ItemStack itemStack = value.getItem(slot);
            if (itemStack==null || itemStack.getType().isAir()) return;
            ObjectNode slotNode = mapper.createObjectNode();
            slotNode.put("slot", slot);
            slotNode.set("itemStack", mapper.valueToTree(itemStack).get("itemStack"));
            slots.add(slotNode);
        });
        gen.writeFieldName("slots");
        gen.writeObject(slots);
        gen.writeEndObject();
    }
}
