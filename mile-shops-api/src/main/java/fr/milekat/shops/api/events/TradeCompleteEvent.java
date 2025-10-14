package fr.milekat.shops.api.events;

import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a Bukkit event triggered when a {@link Player} completes a {@link Trade}.
 */
@SuppressWarnings("unused")
public class TradeCompleteEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    private boolean cancelled;

    private final Player player;
    private final Shop shop;
    private final Trade trade;

    /**
     * Constructs a new TradeCompleteEvent with the specified player, shop, and trade.
     *
     * @param player The player who completed the trade.
     * @param shop   The shop where the trade took place.
     * @param trade  The completed trade.
     */
    public TradeCompleteEvent(Player player, Shop shop, Trade trade) {
        super();
        this.player = player;
        this.shop = shop;
        this.trade = trade;
    }

    /**
     * Retrieves the shop where the trade took place.
     *
     * @return The shop.
     */
    public Shop getShop() {
        return shop;
    }

    /**
     * Retrieves the player who completed the trade.
     *
     * @return The player.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Retrieves the completed trade.
     *
     * @return The trade.
     */
    public Trade getTrade() {
        return trade;
    }

    /**
     * Gets the list of event handlers for this event.
     *
     * @return The handler list.
     */
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    /**
     * Gets the list of handlers for this event.
     *
     * @return The handler list.
     */
    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

    /**
     * Checks if the event is cancelled.
     *
     * @return true if the event is cancelled, false otherwise.
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets the cancelled state of the event.
     *
     * @param cancel true to cancel the event, false otherwise.
     */
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}