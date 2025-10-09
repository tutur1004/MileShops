package fr.milekat.shops.api.classes;


/**
 * Represents the type of shop.
 */
public enum ShopType {
    /**
     * Represents a vanilla shop (Using Minecraft vanilla villager GUI).
     */
    VANILLA(false),
    /**
     * A chest shop with 1 column of 4 trades without secondary item
     */
    BASIC_FOUR(true),
    /**
     * A chest shop with 2 columns of 6 trades without secondary item
     */
    DOUBLE_SIX(true);

    private final Boolean shaped;

    /**
     * ShopType.
     *
     * @param shaped true if the shop is compatible with the InventoryShopShape enum, false otherwise
     */
    ShopType(Boolean shaped) {
        this.shaped = shaped;
    }

    /**
     * Check if the shop is compatible with the InventoryShopShape enum.
     *
     * @return true if the shop is shaped, false otherwise
     */
    public Boolean isShaped() {
        return shaped;
    }
}
