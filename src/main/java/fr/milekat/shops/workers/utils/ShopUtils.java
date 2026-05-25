package fr.milekat.shops.workers.utils;

import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.workers.gui.InventoryShop;
import fr.milekat.shops.workers.gui.ShopAdminSession;
import fr.milekat.shops.workers.gui.InventoryShopShape;
import fr.milekat.utils.storage.exceptions.StorageExecuteException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unused")
public class ShopUtils {

    public static void openShop(@NotNull Player player, @NotNull Shop shop) throws StorageExecuteException {
        if (shop.getType().isShaped()) {
            InventoryShopShape inventoryShopShape = InventoryShopShape.valueOf(shop.getType().name());
            InventoryShop inventoryShop = new InventoryShop(inventoryShopShape, shop, player);
            // Async-warm the trade-uses cache for the limited trades visible in this shop.
            warmTradeUsesCache(player.getUniqueId(), shop.getUuid());
        } /*else {
            //  TODO: Vanilla shops
        }*/
    }

    public static void openAdminShop(@NotNull Player player, @NotNull Shop shop) throws StorageExecuteException {
        if (shop.getType().isShaped()) {
            List<Trade> trades = Main.getStorage().getCacheTrades(shop.getUuid());
            new ShopAdminSession(player, shop, trades).open();
        }
    }

    /**
     * Pre-fetches the {@code TradeUsesCache} entries that the limit check in
     * {@code TradeUtils.processedTrades} will look up, so they are already warm
     * by the time the player actually clicks a trade. No-op when the trade-uses
     * cache is disabled, the player has no tags, or none of the shop's trades
     * declare usage limits.
     */
    private static void warmTradeUsesCache(@NotNull UUID playerUuid, @NotNull UUID shopUuid) {
        if (Main.TRADE_USES_DELAY <= 0) return;
        Map<String, Object> playerTags = Main.PLAYER_TAGS.get(playerUuid);
        if (playerTags == null || playerTags.isEmpty()) return;

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                List<Trade> trades = Main.getStorage().getCacheTrades(shopUuid);
                for (Trade trade : trades) {
                    if (!trade.isUsageLimited()) continue;
                    for (String tagName : trade.getMaxTradeUses().keySet()) {
                        Object value = playerTags.get(tagName);
                        if (value == null) continue;
                        Map<String, Object> singleTag = new HashMap<>();
                        singleTag.put(tagName, value);
                        // getCacheTradeUses stores the count in TRADE_USES_CACHE
                        Main.getStorage().getCacheTradeUses(singleTag, trade);
                    }
                }
            } catch (Exception ex) {
                Main.getMileLogger().debug("Trade-uses cache warm-up failed for shop "
                        + shopUuid + ": " + ex.getMessage());
            }
        });
    }
}
