package fr.milekat.shops.api.classes;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    private Tag<Material> firstItemTag;
    private ItemStack secondItem;
    private Tag<Material> secondItemTag;
    private ItemStack resultItem;
    private Map<String, Integer> maxTradeUses;
    private Map<String, Integer> moneyResult;
    private boolean enabled; // TODO implement

    /**
     * Constructs a new Trade instance with the specified parameters.
     *
     * @param shopUuid      The UUID of the shop that contains the trade.
     * @param tradePosition The position of the trade in the shop.
     * @param firstItem     The first item of the trade.
     * @param firstItemTag  If set, all materials from this tags will be allowed.
     * @param secondItem    The second item of the trade.
     * @param secondItemTag If set, all materials from this tags will be allowed.
     * @param resultItem    The resulting item of the trade.
     * @param maxTradeUses  Per-tag usage limits. Each entry is a player-tag name and the
     *                      maximum number of trades allowed when sharing that tag value.
     *                      Empty map disables usage tracking.
     * @param moneyResult   If set, the trade will give money instead of items.
     *                      The map key is the money type and the value is the amount of money to give.
     */
    public Trade(@NotNull UUID shopUuid, int tradePosition,
                 @NotNull ItemStack firstItem, @Nullable Tag<Material> firstItemTag,
                 @Nullable ItemStack secondItem, @Nullable Tag<Material> secondItemTag,
                 @NotNull ItemStack resultItem,
                 @NotNull Map<String, Integer> maxTradeUses,
                 @NotNull Map<String, Integer> moneyResult) {
        this.shopUuid = shopUuid;
        this.tradePosition = tradePosition;
        this.firstItem = firstItem;
        this.firstItemTag = firstItemTag;
        this.secondItem = secondItem;
        this.secondItemTag = secondItemTag;
        this.resultItem = resultItem;
        this.enabled = true;
        this.maxTradeUses = maxTradeUses;
        this.moneyResult = moneyResult;
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
     * Retrieves the tags allowed for the first item.
     * @return The tags allowed for the first item.
     */
    public Tag<Material> getFirstItemTag() {
        return firstItemTag;
    }

    /**
     * Sets the tags allowed for the first item.
     * @param firstItemTag The new tags allowed for the first item.
     */
    public void setFirstItemTag(Tag<Material> firstItemTag) {
        this.firstItemTag = firstItemTag;
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
     * Retrieves the tags allowed for the second item.
     * @return The tags allowed for the second item.
     */
    public Tag<Material> getSecondItemTag() {
        return secondItemTag;
    }

    /**
     * Sets the tags allowed for the second item.
     * @param secondItemTag The new tags allowed for the second item.
     */
    public void setSecondItemTag(Tag<Material> secondItemTag) {
        this.secondItemTag = secondItemTag;
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
     * Retrieves the per-tag usage limits for this trade.
     *
     * @return A map where each entry is a player-tag name and the maximum number of trades
     *         allowed when sharing that tag value. Empty means no usage tracking.
     */
    public @NotNull Map<String, Integer> getMaxTradeUses() {
        return maxTradeUses;
    }

    /**
     * Sets the per-tag usage limits for this trade.
     *
     * @param maxTradeUses Map of tag name to maximum number of trades.
     */
    public void setMaxTradeUses(@NotNull Map<String, Integer> maxTradeUses) {
        this.maxTradeUses = maxTradeUses;
    }

    /**
     * Checks if the trade is limited by usage.
     *
     * @return true if the trade has at least one per-tag usage limit, false otherwise.
     */
    public boolean isUsageLimited() {
        return maxTradeUses != null && !maxTradeUses.isEmpty();
    }

    /**
     * Checks if the trade give money instead of items.
     *
     * @return true if the trade gives money, false otherwise.
     */
    public boolean isMoneyTrade() {
        return !moneyResult.isEmpty();
    }

    /**
     * Get all money results for each money types
     *
     * @return The map of money types and amounts to give.
     */
    public @NotNull Map<String, Integer> getMoneyResult() {
        return moneyResult;
    }

    /**
     * Sets whether the trade gives money instead of items.
     *
     * @param moneyResult The map of money types and amounts to give. If empty, the trade will give items instead.
     */
    public void setMoneyResult(@NotNull Map<String, Integer> moneyResult) {
        this.moneyResult = moneyResult;
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

    /**
     * Checks if the trade is complex.
     *
     * @return true if the trade is complex, false otherwise.
     */
    public boolean isComplex() {
        return firstItemTag != null || secondItemTag != null || isUsageLimited();
    }
}
