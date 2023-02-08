package fr.milekat.shops.storage.adapter.elasticsearch.mappers.milekat.shops;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import dev.sergiferry.playernpc.api.NPC;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

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
public class NPCSerializer extends StdSerializer<NPC.Global> {
    private final ObjectMapper mapper;

    public NPCSerializer(ObjectMapper mapper) {
        super(NPC.Global.class);
        this.mapper = mapper;
    }

    @Override
    public void serialize(@NotNull NPC.Global value, @NotNull JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeStartObject();
        gen.setCodec(mapper);

        gen.writeStringField("uuid", value.getSimpleCode());

        gen.writeFieldName("location");
        gen.writeObject(mapper.valueToTree(value.getLocation()).get("location"));

        if (!value.getGazeTrackingType().equals(NPC.GazeTrackingType.NONE)) {
            gen.writeStringField("tracking", value.getGazeTrackingType().toString());
        }

        if (!value.getText().isEmpty()) {
            gen.writeFieldName("text");
            gen.writeObject(value.getText());
        }

        if (value.getInteractCooldown()!=0) {
            gen.writeNumberField("cooldown", value.getInteractCooldown());
        }

        ObjectNode equipNode = mapper.createObjectNode();
        if (!value.getEquipment(NPC.Slot.MAINHAND).isSimilar(new ItemStack(Material.AIR))) {
            equipNode.set("mainHand", mapper.valueToTree(value.getEquipment(NPC.Slot.MAINHAND)).get("itemStack"));
        }
        if (!value.getEquipment(NPC.Slot.OFFHAND).isSimilar(new ItemStack(Material.AIR))) {
            equipNode.set("offHand", mapper.valueToTree(value.getEquipment(NPC.Slot.OFFHAND)).get("itemStack"));
        }
        if (!value.getEquipment(NPC.Slot.HELMET).isSimilar(new ItemStack(Material.AIR))) {
            equipNode.set("helmet", mapper.valueToTree(value.getEquipment(NPC.Slot.HELMET)).get("itemStack"));
        }
        if (!value.getEquipment(NPC.Slot.CHESTPLATE).isSimilar(new ItemStack(Material.AIR))) {
            equipNode.set("chest", mapper.valueToTree(value.getEquipment(NPC.Slot.CHESTPLATE)).get("itemStack"));
        }
        if (!value.getEquipment(NPC.Slot.LEGGINGS).isSimilar(new ItemStack(Material.AIR))) {
            equipNode.set("leggings", mapper.valueToTree(value.getEquipment(NPC.Slot.LEGGINGS)).get("itemStack"));
        }
        if (!value.getEquipment(NPC.Slot.BOOTS).isSimilar(new ItemStack(Material.AIR))) {
            equipNode.set("boots", mapper.valueToTree(value.getEquipment(NPC.Slot.BOOTS)).get("itemStack"));
        }
        if (equipNode.size() > 0) {
            gen.writeFieldName("equip");
            gen.writeObject(equipNode);
        }

        ObjectNode skinNode = mapper.createObjectNode();
        equipNode.put("texture", value.getSkin().getTexture());
        equipNode.put("signature", value.getSkin().getSignature());

        if (skinNode.size() > 0) {
            gen.writeFieldName("skin");
            gen.writeObject(skinNode);
        }

        gen.writeEndObject();
    }
}
