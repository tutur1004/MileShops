package fr.milekat.shops.storage.adapter.elasticsearch.mappers.shops;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import fr.milekat.milenpc.api.classes.NPC;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.ShopType;
import fr.milekat.utils.DateMileKat;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

/**
 * {
 *     "shop": {
 *         "uuid": {@link UUID},
 *         "name": {@link String},
 *         "npc": {@link NPC},
 *         "type": {@link ShopType},
 *         "spawnIn": {@link Date},
 *         "spawnOut": {@link Date}
 *     }
 * }
 */
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
        if (value.getNpc() != null) {
            gen.writeObjectField("npc", value.getNpc());
        }
        gen.writeStringField("type", String.valueOf(value.getType()));

        if (value.isTimed()) {
            gen.writeStringField("spawnIn", DateMileKat.getDateEs(value.getSpawnIn()));
            gen.writeStringField("spawnOut", DateMileKat.getDateEs(value.getSpawnOut()));
        }

        gen.writeEndObject();
    }
}

