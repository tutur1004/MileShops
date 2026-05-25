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
import java.util.HashMap;
import java.util.Map;
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
 *         "maxTradeUses": {
 *             "tagA": int,
 *             "tagB": int,
 *             ...
 *         },
 *         "moneyResult": {
 *             "moneyA": int,
 *             "moneyB": int,
 *             ...
 *             "moneyN": int,
 *         }
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
        Map<String, Integer> maxTradeUses = new HashMap<>();
        if (node.has("maxTradeUses")) {
            maxTradeUses = mapper.convertValue(node.get("maxTradeUses"),
                    mapper.getTypeFactory().constructMapType(Map.class, String.class, Integer.class));
        } else if (node.has("maxTradeUse") && node.has("maxTradeTagsNames")) {
            // Legacy format: single int shared by a list of tag names.
            int legacyMax = node.get("maxTradeUse").asInt();
            for (JsonNode tagNode : node.get("maxTradeTagsNames")) {
                maxTradeUses.put(tagNode.asText(), legacyMax);
            }
        }
        Map<String, Integer> moneyResult = new HashMap<>();
        if (node.has("moneyResult")) {
            moneyResult = mapper.convertValue(node.get("moneyResult"),
                    mapper.getTypeFactory().constructMapType(Map.class, String.class, Integer.class));
        }

        return new Trade(shopUuid, tradePosition,
                firstItem, firstItemTag,
                secondItem, secondItemTag,
                resultItem,
                maxTradeUses,
                moneyResult);
    }
}
