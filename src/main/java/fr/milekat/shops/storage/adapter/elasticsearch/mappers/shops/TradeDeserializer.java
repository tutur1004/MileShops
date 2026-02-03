package fr.milekat.shops.storage.adapter.elasticsearch.mappers.shops;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.workers.utils.TradeUtils;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * {
 *     "trade": {
 *         "shopUuid": UUID,
 *         "position": Location,
 *         "firstItem": ItemStack,
 *         "firstItemTag": String,
 *         "resultItem": ItemStack,
 *         "secondItem": ItemStack,
 *         "secondItemTag": String,
 *         "maxTradeUse": int,
 *         "maxTradeTagsNames": String[]
 *     }
 * }
 */
public class TradeDeserializer extends StdDeserializer<Trade> {
    private final ObjectMapper mapper;

    public TradeDeserializer(ObjectMapper mapper) {
        super(Trade.class);
        this.mapper = mapper;
    }

    @Override
    public Trade deserialize(JsonParser p, DeserializationContext context) throws IOException {
        JsonNode node = mapper.readTree(p);
        if (node.isEmpty() || !node.isContainerNode()) return null;
        UUID shopUuid = UUID.fromString(node.get("shopUuid").asText());
        int tradePosition = node.get("position").asInt();
        ItemStack firstItem = mapper.treeToValue(node.get("firstItem"), ItemStack.class);
        Tag<Material> firstItemTag = null;
        if (node.has("firstItemTag")) {
            firstItemTag = TradeUtils.getMaterialTag(node.get("firstItemTag").asText());
        }
        ItemStack resultItem = mapper.treeToValue(node.get("resultItem"), ItemStack.class);
        ItemStack secondItem = null;
        Tag<Material> secondItemTag = null;
        if (node.has("secondItem")) {
            secondItem = mapper.treeToValue(node.get("secondItem"), ItemStack.class);
            if (node.has("secondItemTag")) {
                secondItemTag = TradeUtils.getMaterialTag(node.get("secondItemTag").asText());
            }
        }
        int maxTradeUse = 0;
        List<String> maxTradeTagsNames = null;
        if (node.has("maxTradeUse") && node.has("maxTradeTagsNames")) {
            maxTradeUse = node.get("maxTradeUse").asInt();
            maxTradeTagsNames = List.of(mapper.convertValue(node.get("maxTradeTagsNames"), String[].class));
        }
        boolean moneyTrade = false;
        if (node.has("moneyTrade")) {
            moneyTrade = node.get("moneyTrade").asBoolean();
        }

        return new Trade(shopUuid, tradePosition,
                firstItem, firstItemTag,
                secondItem, secondItemTag,
                resultItem,
                maxTradeUse, maxTradeTagsNames,
                moneyTrade);
    }
}

