package fr.milekat.shops.workers.listeners;

import fr.milekat.shops.Main;
import fr.milekat.shops.api.events.TradeCompleteEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class TradeListeners implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void logTrade(@NotNull TradeCompleteEvent event) {
        if (event.isCancelled()) return;
        if (Main.PLAYER_TAGS.containsKey(event.getPlayer().getUniqueId())) {
            Main.getStorage().logTrade(
                    Main.PLAYER_TAGS.get(event.getPlayer().getUniqueId()),
                    event.getTrade()
            );
        } else {
            Main.getMileLogger().warning("No tags found for player " + event.getPlayer().getName() + " (" +
                    event.getPlayer().getUniqueId() + "), cannot log trade.");
        }
    }
}
