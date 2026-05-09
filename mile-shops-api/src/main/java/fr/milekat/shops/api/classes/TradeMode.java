package fr.milekat.shops.api.classes;

public enum TradeMode {
    // Trade items from player inventory
    INVENTORY,
    // Trade items from player inventory and ender chest
    ENDER_CHEST,
    // Trade items from player inventory and shulker boxes from player inventory
    SHULKER,
    // Trade items from player inventory, ender chest and shulker boxes from player inventory and ender chest
    END_SHULKER
}
