package fr.milekat.shops.workers.listeners;

import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.api.events.TradeCompleteEvent;
import fr.milekat.shops.storage.CacheManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public class TradeListeners implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void logTrade(@NotNull TradeCompleteEvent event) {
        if (event.isCancelled()) return;
        UUID playerUuid = event.getPlayer().getUniqueId();
        Map<String, Object> playerTags = Main.PLAYER_TAGS.get(playerUuid);
        if (playerTags == null) {
            Main.getMileLogger().warning("No tags found for player " + event.getPlayer().getName()
                    + " (" + playerUuid + "), cannot log trade.");
            return;
        }
        Trade trade = event.getTrade();
        Main.getStorage().logTrade(playerTags, trade);

        // Keep the trade-uses cache in sync so the next limit check reflects this trade
        if (trade.isUsageLimited()) {
            for (String tagName : trade.getMaxTradeUses().keySet()) {
                Object value = playerTags.get(tagName);
                if (value != null) {
                    CacheManager.bumpTradeUses(trade.getShopUuid(), trade.getTradePosition(),
                            tagName, value, 1);
                }
            }
        }
    }
}
