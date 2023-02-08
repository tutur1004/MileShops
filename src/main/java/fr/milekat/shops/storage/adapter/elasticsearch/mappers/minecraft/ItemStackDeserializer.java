package fr.milekat.shops.storage.adapter.elasticsearch.mappers.minecraft;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;

public class ItemStackDeserializer extends StdDeserializer<ItemStack> {
    private final ObjectMapper mapper;

    public ItemStackDeserializer(ObjectMapper mapper) {
        super(ItemStack.class);
        this.mapper = mapper;
    }

    @Override
    public ItemStack deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        try {
            JsonNode node = mapper.readTree(p);
            if (node.isEmpty()) return null;
            YamlConfiguration yamlSlot = new YamlConfiguration();
            yamlSlot.loadFromString(new YAMLMapper().writeValueAsString(node));
            ItemStack stack = yamlSlot.getItemStack("itemStack");
            if (stack!=null) return stack;
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
        return null;
    }
}
