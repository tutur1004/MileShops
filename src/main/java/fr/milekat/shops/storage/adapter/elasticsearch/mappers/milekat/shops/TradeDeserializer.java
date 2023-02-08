package fr.milekat.shops.storage.adapter.elasticsearch.mappers.milekat.shops;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import fr.milekat.shops.api.classes.Trade;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.UUID;

public class TradeDeserializer  extends StdDeserializer<Trade> {
    private final ObjectMapper mapper;

    public TradeDeserializer(ObjectMapper mapper) {
        super(Trade.class);
        this.mapper = mapper;
    }

    @Override
    public Trade deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = mapper.readTree(p);
        if (node.isEmpty() || !node.isContainerNode()) return null;
        UUID shopUuid = UUID.fromString(node.get("shopUuid").asText());
        int tradePosition = node.get("position").asInt();
        ItemStack firstItem = mapper.treeToValue(node.get("firstItem"), ItemStack.class);
        ItemStack resultItem = mapper.treeToValue(node.get("resultItem"), ItemStack.class);
        if (node.has("secondItem")) {
            ItemStack secondItem = mapper.treeToValue(node.get("secondItem"), ItemStack.class);
            return new Trade(shopUuid, tradePosition, firstItem, secondItem, resultItem);
        } else {
            return new Trade(shopUuid, tradePosition, firstItem, null, resultItem);
        }
    }
}
