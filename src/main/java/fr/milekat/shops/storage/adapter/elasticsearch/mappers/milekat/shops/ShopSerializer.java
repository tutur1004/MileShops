package fr.milekat.shops.storage.adapter.elasticsearch.mappers.milekat.shops;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import fr.milekat.shops.api.classes.Shop;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class ShopSerializer extends StdSerializer<Shop> {
    private final ObjectMapper mapper;

    public ShopSerializer(ObjectMapper mapper) {
        super(Shop.class);
        this.mapper = mapper;
    }

    @Override
    public void serialize(@NotNull Shop value, @NotNull JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeStartObject();
        gen.setCodec(mapper);

        gen.writeStringField("uuid", String.valueOf(value.getUuid()));
        gen.writeStringField("name", value.getName());
        gen.writeFieldName("npc");
        gen.writeObject(value.getNpc());
        gen.writeStringField("type", String.valueOf(value.getType()));

        gen.writeEndObject();
    }
}
