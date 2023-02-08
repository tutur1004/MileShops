package fr.milekat.shops.storage.adapter.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dev.sergiferry.playernpc.api.NPC;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.storage.adapter.elasticsearch.mappers.milekat.shops.*;
import fr.milekat.shops.storage.adapter.elasticsearch.mappers.minecraft.*;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class CustomMappers {
    private final ObjectMapper mapper;
    private final SimpleModule module;

    public CustomMappers() {
        mapper = new ObjectMapper(new YAMLFactory());
        module = new SimpleModule();
        //  ItemStack
        module.addSerializer(ItemStack.class, new ItemStackSerializer(this.mapper));
        module.addDeserializer(ItemStack.class, new ItemStackDeserializer(this.mapper));
        // Inventory
        module.addSerializer(Inventory.class, new InventorySerializer(this.mapper));
        module.addDeserializer(Inventory.class, new InventoryDeserializer(this.mapper));
        //  Location
        module.addSerializer(Location.class, new LocationSerializer(this.mapper));
        module.addDeserializer(Location.class, new LocationDeserializer(this.mapper));
        //  Block
        module.addSerializer(Block.class, new BlockSerializer(this.mapper));
        module.addDeserializer(Block.class, new BlockDeserializer(this.mapper));
        //  Shop
        module.addSerializer(Shop.class, new ShopSerializer(this.mapper));
        module.addDeserializer(Shop.class, new ShopDeserializer(this.mapper));
        //  Trade
        module.addSerializer(Trade.class, new TradeSerializer(this.mapper));
        module.addDeserializer(Trade.class, new TradeDeserializer(this.mapper));
        //  NPC
        module.addSerializer(NPC.Global.class, new NPCSerializer(this.mapper));
        module.addDeserializer(NPC.Global.class, new NPCDeserializer(this.mapper));
        //  Register modules to mapper
        mapper.registerModule(module);
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public SimpleModule getModule() {
        return module;
    }
}
