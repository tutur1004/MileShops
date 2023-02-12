package fr.milekat.shops.api.events;

import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TradeCompleteEvent  extends Event implements Cancellable {
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    private boolean CANCELLED;

    private final Player player;
    private final Shop shop;
    private final Trade trade;

    public TradeCompleteEvent(Player player, Shop shop, Trade trade) {
        super();
        this.shop = shop;
        this.player = player;
        this.trade = trade;
    }

    public Shop getShop() {
        return shop;
    }

    public Player getPlayer() {
        return player;
    }

    public Trade getTrade() {
        return trade;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

    @Override
    public boolean isCancelled() {
        return this.CANCELLED;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.CANCELLED = cancel;
    }
}