package fr.milekat.shops.storage.adapter.elasticsearch.mappers.minecraft;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;

/**
 * Json model
 * {
 *     "inventory": {
 *         "type": "InventoryType"
 *         "slots": [
 *         {
 *             "slot": int,
 *             "itemStack": object
 *         }
 *         ]
 *     }
 * }
 */
public class InventoryDeserializer extends StdDeserializer<Inventory> {
    private final ObjectMapper mapper;

    public InventoryDeserializer(ObjectMapper mapper) {
        super(Inventory.class);
        this.mapper = mapper;
    }

    @Override
    public Inventory deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = mapper.readTree(p);
        if (node.isEmpty() || !node.isContainerNode()) return null;
        Inventory inventory = Bukkit.createInventory(null, InventoryType.valueOf(node.get("type").textValue()));
        node.get("slots").forEach(slot -> {
            try {
                ItemStack itemStack = mapper.treeToValue(slot, ItemStack.class);
                inventory.setItem(slot.get("slot").asInt(), itemStack);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return inventory;
    }
}
