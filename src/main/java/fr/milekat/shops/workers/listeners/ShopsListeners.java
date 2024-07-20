package fr.milekat.shops.workers.listeners;

import fr.milekat.milenpc.api.classes.NpcClickType;
import fr.milekat.milenpc.api.events.PlayerNpcInteractEvent;
import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.workers.gui.AdminEditor;
import fr.milekat.shops.workers.utils.ShopActions;
import fr.milekat.utils.storage.exceptions.StorageExecuteException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ShopsListeners implements Listener {
    @EventHandler
    public void loadShops(PluginEnableEvent event) {
        Main.reloadShops();
    }

    @EventHandler
    public void openPlayerShop(@NotNull PlayerNpcInteractEvent event) throws StorageExecuteException {
        if (event.isCancelled()) return;
        //  Get shop
        Shop shop = Main.getStorage().getCacheShop(event.getNpc().getUuid());
        if (shop == null) return;
        //  Open shop
        ShopActions.openShop(event.getPlayer(), shop);
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void adminEditShop(@NotNull PlayerNpcInteractEvent event) throws StorageExecuteException {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        if (!player.hasPermission("shops.edit")) return;
        if (event.getClickType().equals(NpcClickType.SHIFT_LEFT_CLICK) ||
                event.getClickType().equals(NpcClickType.SHIFT_RIGHT_CLICK)) {
            //  Get shop
            Shop shop = Main.getStorage().getCacheShop(event.getNpc().getUuid());
            if (shop == null) return;
            //  Open admin editor
            List<Trade> trades = Main.getStorage().getCacheTrades(shop.getUuid());
            AdminEditor adminEditor = new AdminEditor(player, shop, trades);
            adminEditor.open(player);
            event.setCancelled(true);
        }
    }
}
