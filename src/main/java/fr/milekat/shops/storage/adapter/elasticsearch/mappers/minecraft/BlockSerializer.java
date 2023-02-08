package fr.milekat.shops.storage.adapter.elasticsearch.mappers.minecraft;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Json model
 * {
 *     "block": {
 *         "location": object,
 *         "blockData": "block_data_as_string"
 *     }
 * }
 */
public class BlockSerializer extends StdSerializer<Block> {
    private final ObjectMapper mapper;

    public BlockSerializer(ObjectMapper mapper) {
        super(Block.class);
        this.mapper = mapper;
    }

    @Override
    public void serialize(@NotNull Block value, @NotNull JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeStartObject();
        gen.setCodec(mapper);

        gen.writeFieldName("location");
        gen.writeObject(mapper.valueToTree(value.getLocation()).get("location"));

        gen.writeStringField("blockData", value.getBlockData().getAsString());

        gen.writeEndObject();
    }
}
