package fr.milekat.shops.workers.listeners;

import dev.sergiferry.playernpc.api.NPC;
import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.ShopType;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.storage.exeptions.StorageExecuteException;
import fr.milekat.shops.workers.ShopsManager;
import fr.milekat.shops.workers.gui.AdminEditor;
import fr.milekat.shops.workers.gui.ChestShop;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShopsListeners implements Listener {
    @EventHandler
    public void loadShops(PluginEnableEvent event) throws StorageExecuteException {
        Main.info("Loading shops...");
        List<Shop> shops = Main.getStorage().getAllShops();
        shops.forEach(shop -> {
            try {
                Main.getStorage().getTrades(shop.getUuid());
                shop.getNpc().forceUpdate();
            } catch (StorageExecuteException exception) {
                Main.warning("Error while trying to load trades for shop " + shop.getName());
            }
        });
        Main.info(shops.size() + " shops loaded !");
    }

    //  TODO: To test
    @EventHandler
    public void openPlayerShop(@NotNull NPC.Events.Interact event) throws StorageExecuteException {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        NPC npc = event.getNPC().getGlobal();
        Map.Entry<Shop, List<Trade>> shop = ShopsManager.getShop(UUID.fromString(npc.getSimpleCode()));
        if (shop==null) return;
        if (shop.getKey().getType().equals(ShopType.VANILLA)) {
            //  TODO: Vanilla shop display
        } else if (shop.getKey().getType().equals(ShopType.INVENTORY)) {
            ChestShop chestShop = new ChestShop(player, shop.getKey(), shop.getValue());
            chestShop.open(player);
        }
    }

    @EventHandler (priority = EventPriority.LOW)
    public void adminEditShop(@NotNull NPC.Events.Interact event) throws StorageExecuteException {
        Player player = event.getPlayer();
        if (!player.hasPermission("custom-shops.admin.edit")) return;
        if (!player.isSneaking()) return;
        NPC npc = event.getNPC().getGlobal();
        Map.Entry<Shop, List<Trade>> shop = ShopsManager.getShop(UUID.fromString(npc.getSimpleCode()));
        if (shop==null) return;
        AdminEditor adminEditor = new AdminEditor(player, shop.getKey(), shop.getValue());
        adminEditor.open(player);
        event.setCancelled(true);
    }
}
