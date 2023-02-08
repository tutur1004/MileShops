package fr.milekat.shops.storage.adapter.elasticsearch.mappers.minecraft;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.io.IOException;

public class ItemStackSerializer extends StdSerializer<ItemStack> {
    private final ObjectMapper mapper;

    public ItemStackSerializer(ObjectMapper mapper) {
        super(ItemStack.class);
        this.mapper = mapper;
    }

    @Override
    public void serialize(@Nonnull ItemStack value, @NotNull JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeStartObject();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("itemStack", value);
        gen.writeFieldName("itemStack");
        gen.writeObject(mapper.readValue(yaml.saveToString(), JsonNode.class).get("itemStack"));
        gen.writeEndObject();
    }
}
