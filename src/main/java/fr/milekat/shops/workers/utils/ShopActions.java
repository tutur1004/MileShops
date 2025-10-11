package fr.milekat.shops.workers.utils;

import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.workers.gui.AdminEditor;
import fr.milekat.shops.workers.gui.InventoryShop;
import fr.milekat.shops.workers.gui.InventoryShopShape;
import fr.milekat.utils.storage.exceptions.StorageExecuteException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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

    public static void openAdminShop(@NotNull Player player, @NotNull Shop shop) throws StorageExecuteException {
        if (shop.getType().isShaped()) {
            List<Trade> trades = Main.getStorage().getCacheTrades(shop.getUuid());
            AdminEditor adminEditor = new AdminEditor(player, shop, trades);
            adminEditor.open(player);
        }
    }
}
