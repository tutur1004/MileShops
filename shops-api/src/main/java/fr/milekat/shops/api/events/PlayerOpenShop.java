package fr.milekat.shops.api.events;

import fr.milekat.shops.api.classes.Shop;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a Bukkit event triggered when a {@link Player} opens a shop.
 */
@SuppressWarnings("unused")
public class PlayerOpenShop extends Event implements Cancellable {
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    private boolean cancelled;

    private final Player player;
    private final Shop shop;

    /**
     * Constructs a new PlayerOpenShop event with the specified player and shop.
     *
     * @param player    The player who opened the shop.
     * @param shop      The shop where the trade took place.
     */
    public PlayerOpenShop(Player player, Shop shop) {
        super();
        this.player = player;
        this.shop = shop;
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
     * Retrieves the player who opened the shop.
     *
     * @return The player.
     */
    public Player getPlayer() {
        return player;
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
