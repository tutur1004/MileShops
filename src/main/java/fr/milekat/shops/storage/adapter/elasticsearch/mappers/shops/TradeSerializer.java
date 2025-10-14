package fr.milekat.shops.storage.adapter.elasticsearch.mappers.shops;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import fr.milekat.shops.api.classes.Trade;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * {
 *     "trade": {
 *         "shopUuid": UUID,
 *         "position": Location,
 *         "firstItem": ItemStack,
 *         "resultItem": ItemStack,
 *         "secondItem": ItemStack,
 *         "maxTradeUse": int,
 *         "maxTradeTagsNames": String[]
 *     }
 * }
 */
public class TradeSerializer extends StdSerializer<Trade> {
    private final ObjectMapper mapper;

    public TradeSerializer(ObjectMapper mapper) {
        super(Trade.class);
        this.mapper = mapper;
    }

    @Override
    public void serialize(@NotNull Trade value, @NotNull JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeStartObject();
        gen.setCodec(mapper);
        gen.writeStringField("shopUuid", String.valueOf(value.getShopUuid()));
        gen.writeNumberField("position", value.getTradePosition());
        gen.writeFieldName("firstItem");
        gen.writeObject(value.getFirstItem());
        if (value.getSecondItem() != null) {
            gen.writeFieldName("secondItem");
            gen.writeObject(value.getSecondItem());
        }
        gen.writeFieldName("resultItem");
        gen.writeObject(value.getResultItem());
        if (value.isUsageLimited()) {
            gen.writeNumberField("maxTradeUse", value.getMaxTradeUse());
            gen.writeArrayFieldStart("maxTradeTagsNames");
            for (String tag : value.getMaxTradeTagsNames()) {
                gen.writeString(tag);
            }
            gen.writeEndArray();
        }
        gen.writeEndObject();
    }
}

