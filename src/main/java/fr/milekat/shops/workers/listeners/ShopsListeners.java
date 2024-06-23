package fr.milekat.shops.workers.listeners;

import fr.milekat.milenpc.api.classes.NPC;
import fr.milekat.milenpc.api.classes.NpcClickType;
import fr.milekat.milenpc.api.events.PlayerNpcInteractEvent;
import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.ShopType;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.workers.ShopsManager;
import fr.milekat.shops.workers.gui.AdminEditor;
import fr.milekat.shops.workers.gui.InventoryLarge;
import fr.milekat.shops.workers.gui.InventorySmall;
import fr.milekat.utils.storage.exceptions.StorageExecuteException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class ShopsListeners implements Listener {
    @EventHandler
    public void loadShops(PluginEnableEvent event) throws StorageExecuteException {
        Main.getMileLogger().info("Loading shops...");
        List<Shop> shops = Main.getStorage().getCacheAllShops();
        Main.TRADE_CACHE.clear();
        Main.TRADE_MODE_CACHE.clear();
        Main.getMileLogger().info(shops.size() + " shops loaded !");
    }

    //  TODO: To test
    @EventHandler
    public void openPlayerShop(@NotNull PlayerNpcInteractEvent event) throws StorageExecuteException {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        try {
            Map.Entry<Shop, List<Trade>> shop = ShopsManager.getShop(event.getNpc().getUuid());
            if (shop == null) return;
            if (shop.getValue().isEmpty()) {
                return;
            }
            if (shop.getKey().getType().equals(ShopType.VANILLA)) {
                //  TODO: Vanilla shop display
            } else if (shop.getKey().getType().equals(ShopType.INVENTORY_SMALL)) {
                InventorySmall inventorySmall = new InventorySmall(player, shop.getKey(), shop.getValue());
                inventorySmall.open(player);
            } else if (shop.getKey().getType().equals(ShopType.INVENTORY_LARGE)) {
                InventoryLarge inventoryLarge = new InventoryLarge(player, shop.getKey(), shop.getValue(), true);
                inventoryLarge.open(player);
            } else if (shop.getKey().getType().equals(ShopType.INVENTORY_LARGE_NO_FILL)) {
                InventoryLarge inventoryLarge = new InventoryLarge(player, shop.getKey(), shop.getValue(), false);
                inventoryLarge.open(player);
            }
            event.setCancelled(true);
        } catch (IllegalArgumentException ignored) {
        }
    }

    @EventHandler (priority = EventPriority.LOW)
    public void adminEditShop(@NotNull PlayerNpcInteractEvent event) throws StorageExecuteException {
        try {
            Player player = event.getPlayer();
            if (event.getClickType().equals(NpcClickType.SHIFT_LEFT_CLICK) ||
                    event.getClickType().equals(NpcClickType.SHIFT_RIGHT_CLICK)) {
                if (!player.hasPermission("shops.edit")) return;
                NPC npc = event.getNpc();
                Map.Entry<Shop, List<Trade>> shop = ShopsManager.getShop(npc.getUuid());
                if (shop == null) return;
                AdminEditor adminEditor = new AdminEditor(player, shop.getKey(), shop.getValue());
                adminEditor.open(player);
                event.setCancelled(true);
            }
        } catch (IllegalArgumentException ignored) {
        }
    }
}
