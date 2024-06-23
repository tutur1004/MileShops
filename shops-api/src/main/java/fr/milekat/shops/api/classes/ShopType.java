package fr.milekat.shops.api.classes;


/**
 * Represents the type of shop.
 */
public enum ShopType {
    /**
     * Represents a vanilla shop (Using Minecraft vanilla villager GUI).
     */
    VANILLA,
    /**
     * Represents an inventory shop (Using Minecraft chest GUI).
     */
    INVENTORY_SMALL,
    /**
     * Represents a large inventory shop (Using Minecraft double chest GUI).
     */
    INVENTORY_LARGE,
    /**
     * Represents a large inventory shop, but without fill items (Using Minecraft double chest GUI).
     */
    INVENTORY_LARGE_NO_FILL,
}
