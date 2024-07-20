package fr.milekat.shops.workers.utils;

import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.workers.gui.InventoryShop;
import fr.milekat.shops.workers.gui.InventoryShopShape;
import fr.milekat.utils.storage.exceptions.StorageExecuteException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class ShopActions {

    public static void openShop(@NotNull Player player, @NotNull Shop shop) throws StorageExecuteException {
        if (shop.getType().isShaped()) {
            InventoryShopShape inventoryShopShape = InventoryShopShape.valueOf(shop.getType().name());
            InventoryShop inventoryShop = new InventoryShop(inventoryShopShape, shop, player);
        } /*else {
            //  TODO: Vanilla shops
        }*/
    }
}
