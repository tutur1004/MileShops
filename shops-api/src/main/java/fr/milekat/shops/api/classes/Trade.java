package fr.milekat.shops.api.classes;

import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

/**
 * Represents a trade in a shop.
 */
@SuppressWarnings("unused")
public class Trade {
    private final UUID shopUuid;
    private int tradePosition;
    private ItemStack firstItem;
    private ItemStack secondItem;
    private ItemStack resultItem;
    private int maxTradeUse;
    private Map<String, Object> maxTradeTags;
    private boolean enabled; // TODO implement

    /**
     * Constructs a new Trade instance with the specified parameters.
     *
     * @param shopUuid       The UUID of the shop that contains the trade.
     * @param tradePosition  The position of the trade in the shop.
     * @param firstItem      The first item of the trade.
     * @param secondItem     The second item of the trade.
     * @param resultItem     The resulting item of the trade.
     */
    public Trade(UUID shopUuid, int tradePosition, ItemStack firstItem, ItemStack secondItem, ItemStack resultItem) {
        this.shopUuid = shopUuid;
        this.tradePosition = tradePosition;
        this.firstItem = firstItem;
        this.secondItem = secondItem;
        this.resultItem = resultItem;
        enabled = true;
        //  TODO trades uses
        this.maxTradeUse = 0;
    }

    /**
     * Retrieves the UUID of the shop that contains the trade.
     *
     * @return The UUID of the shop.
     */
    public UUID getShopUuid() {
        return shopUuid;
    }

    /**
     * Retrieves the position of the trade in the shop.
     *
     * @return The position of the trade.
     */
    public int getTradePosition() {
        return tradePosition;
    }

    /**
     * Sets the position of the trade in the shop.
     *
     * @param tradePosition The new position of the trade.
     */
    public void setTradePosition(int tradePosition) {
        this.tradePosition = tradePosition;
    }

    /**
     * Retrieves the first item of the trade.
     *
     * @return The first item.
     */
    public ItemStack getFirstItem() {
        return firstItem;
    }

    /**
     * Sets the first item of the trade.
     *
     * @param firstItem The new first item.
     */
    public void setFirstItem(ItemStack firstItem) {
        this.firstItem = firstItem;
    }

    /**
     * Retrieves the second item of the trade.
     *
     * @return The second item.
     */
    public ItemStack getSecondItem() {
        return secondItem;
    }

    /**
     * Sets the second item of the trade.
     *
     * @param secondItem The new second item.
     */
    public void setSecondItem(ItemStack secondItem) {
        this.secondItem = secondItem;
    }

    /**
     * Retrieves the resulting item of the trade.
     *
     * @return The resulting item.
     */
    public ItemStack getResultItem() {
        return resultItem;
    }

    /**
     * Sets the resulting item of the trade.
     *
     * @param resultItem The new resulting item.
     */
    public void setResultItem(ItemStack resultItem) {
        this.resultItem = resultItem;
    }

    /**
     * Retrieves the maximum number of times the trade can be used.
     *
     * @return The maximum trade uses.
     */
    public int getMaxTradeUse() {
        return maxTradeUse;
    }

    /**
     * Sets the maximum number of times the trade can be used.
     *
     * @param maxTradeUse The new maximum trade uses.
     */
    public void setMaxTradeUse(int maxTradeUse) {
        this.maxTradeUse = maxTradeUse;
    }

    /**
     * Retrieves the tags associated with the maximum trade uses.
     *
     * @return The tags associated with the maximum trade uses.
     */
    public Map<String, Object> getMaxTradeTags() {
        return maxTradeTags;
    }

    /**
     * Sets the tags associated with the maximum trade uses.
     *
     * @param maxTradeTags The new tags associated with the maximum trade uses.
     */
    public void setMaxTradeTags(Map<String, Object> maxTradeTags) {
        this.maxTradeTags = maxTradeTags;
    }

    /**
     * Checks if the trade is enabled.
     *
     * @return true if the trade is enabled, false otherwise.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the trade enabled status.
     *
     * @param enabled true to enable the trade, false to disable it.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
