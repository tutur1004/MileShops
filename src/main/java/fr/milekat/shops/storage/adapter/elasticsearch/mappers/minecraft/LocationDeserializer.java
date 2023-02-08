package fr.milekat.shops.storage.adapter.elasticsearch.mappers.minecraft;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;

public class LocationDeserializer extends StdDeserializer<Location> {
    private final ObjectMapper mapper;

    public LocationDeserializer(ObjectMapper mapper) {
        super(Location.class);
        this.mapper = mapper;
    }

    @Override
    public Location deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        try {
            JsonNode node = mapper.readTree(p);
            if (node.isEmpty()) return null;
            YamlConfiguration yamlSlot = new YamlConfiguration();
            yamlSlot.loadFromString(new YAMLMapper().writeValueAsString(node));
            Location location = yamlSlot.getLocation("location");
            if (location!=null) return location;
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
        return null;
    }
}
