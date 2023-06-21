package fr.milekat.shops.storage.adapter.elasticsearch.mappers.minecraft;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Json model
 * {
 *     "block": {
 *         "block_data": "block_data_as_string"
 *         "location": object
 *     }
 * }
 */
public class BlockDeserializer extends StdDeserializer<Block> {
    private final ObjectMapper mapper;
    // TODO: 07/12/2022 To test
    public BlockDeserializer(ObjectMapper mapper) {
        super(Block.class);
        this.mapper = mapper;
    }

    @Nullable
    @Override
    public Block deserialize(JsonParser p, DeserializationContext context) throws IOException {
        JsonNode node = mapper.readTree(p);
        if (node.isEmpty() || !node.isContainerNode()) return null;
        Location location = mapper.treeToValue(node, Location.class);
        if (location==null) return null;
        Block block = location.getBlock();
        block.setBlockData(Bukkit.getServer().createBlockData(node.get("blockData").asText()));
        return block;
    }
}
