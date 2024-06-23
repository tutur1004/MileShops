package fr.milekat.shops.storage.adapter.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import fr.milekat.milenpc.api.classes.NPC;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.storage.adapter.elasticsearch.mappers.shops.*;
import fr.milekat.utils.storage.adapter.elasticsearch.mappers.MinecraftMappers;

@SuppressWarnings("unused")
public class CustomMappers {
    private final ObjectMapper mapper;
    private final SimpleModule module;

    public CustomMappers() {
        mapper = MinecraftMappers.getMapper();
        module = new SimpleModule();
        //  Shop
        module.addSerializer(Shop.class, new ShopSerializer(this.mapper));
        module.addDeserializer(Shop.class, new ShopDeserializer(this.mapper));
        //  Trade
        module.addSerializer(Trade.class, new TradeSerializer(this.mapper));
        module.addDeserializer(Trade.class, new TradeDeserializer(this.mapper));
        //  NPC
        module.addSerializer(NPC.class, new NPCSerializer(this.mapper));
        module.addDeserializer(NPC.class, new NPCDeserializer(this.mapper));
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
