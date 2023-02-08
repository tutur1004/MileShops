package fr.milekat.shops.storage.adapter.elasticsearch.mappers.milekat.shops;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import dev.sergiferry.playernpc.api.NPC;
import dev.sergiferry.playernpc.api.NPCLib;
import fr.milekat.shops.Main;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * {
 *     "npc": {
 *         "location": Location,
 *         "tracking": NEAREST_PLAYER,
 *         "text": ["String","Array"],
 *         "cooldown" int (In ms),
 *         "equip": {
 *             "mainHand": ItemStack,
 *             "offHand": ItemStack,
 *             "helmet": ItemStack,
 *             "chest": ItemStack,
 *             "leggings": ItemStack,
 *             "boots": ItemStack,
 *         },
 *         "skin" : {
 *             "texture": String,
 *             "signature": String
 *         }
 *     }
 * }
 */
public class NPCDeserializer extends StdDeserializer<NPC.Global> {
    private final ObjectMapper mapper;

    public NPCDeserializer(ObjectMapper mapper) {
        super(NPC.class);
        this.mapper = mapper;
    }

    @Override
    public NPC.Global deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = mapper.readTree(p);
        if (node.isEmpty() || !node.isContainerNode()) return null;
        if (!node.has("uuid") || !node.has("location")) return null;
        String npcSimpleCode = node.get("uuid").asText();
        Location location = mapper.treeToValue(node, Location.class);
        NPC.Global npc = NPCLib.getInstance().getGlobalNPC(Main.getInstance(), npcSimpleCode);
        if (npc == null) {
            npc = NPCLib.getInstance().generateGlobalNPC(Main.getInstance(), npcSimpleCode, location);
        } else {
            npc.teleport(location);
        }
        if (node.has("cooldown")) {
            npc.setInteractCooldown(node.get("cooldown").asInt());
        }
        if (node.has("tracking")) {
            npc.setGazeTrackingType(NPC.GazeTrackingType.valueOf(node.get("tracking").asText()));
        }
        if (node.has("text")) {
            List<String> text = new ArrayList<>();
            if (node.get("text").isArray()) {
                for (final JsonNode objNode : node.get("text")) {
                    text.add(objNode.asText());
                }
            }
            npc.setText(text);
        }
        if (node.has("equip")) {
            JsonNode equip = node.get("equip");
            if (equip.has("mainHand")) {
                npc.setItem(NPC.Slot.MAINHAND, mapper.treeToValue(equip.get("mainHand"), ItemStack.class));
            }
            if (equip.has("offHand")) {
                npc.setItem(NPC.Slot.OFFHAND, mapper.treeToValue(equip.get("offHand"), ItemStack.class));
            }
            if (equip.has("helmet")) {
                npc.setItem(NPC.Slot.HELMET, mapper.treeToValue(equip.get("helmet"), ItemStack.class));
            }
            if (equip.has("chest")) {
                npc.setItem(NPC.Slot.CHESTPLATE, mapper.treeToValue(equip.get("chest"), ItemStack.class));
            }
            if (equip.has("leggings")) {
                npc.setItem(NPC.Slot.LEGGINGS, mapper.treeToValue(equip.get("leggings"), ItemStack.class));
            }
            if (equip.has("boots")) {
                npc.setItem(NPC.Slot.BOOTS, mapper.treeToValue(equip.get("boots"), ItemStack.class));
            }
        }
        if (node.has("skin")) {
            JsonNode skin = node.get("skin");
            if (skin.has("texture") && skin.has("signature")) {
                npc.setSkin(skin.get("texture").asText(), skin.get("signature").asText());
            }
        }
        return npc;
    }
}
