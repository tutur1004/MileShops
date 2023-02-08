package fr.milekat.shops.storage.adapter.elasticsearch.mappers.minecraft;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.io.IOException;

public class LocationSerializer extends StdSerializer<Location> {
    private final ObjectMapper mapper;

    public LocationSerializer(ObjectMapper mapper) {
        super(Location.class);
        this.mapper = mapper;
    }

    @Override
    public void serialize(@Nonnull Location value, @NotNull JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeStartObject();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("location", value);
        gen.writeFieldName("location");
        gen.writeObject(mapper.readValue(yaml.saveToString(), JsonNode.class).get("location"));
        gen.writeEndObject();
    }
}
