package fr.milekat.shops.api.classes;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
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
    private int maxTradeUse;
    private List<String> maxTradeTagsNames;
    private boolean enabled; // TODO implement

    /**
     * Constructs a new Trade instance with the specified parameters.
     *
     * @param shopUuid          The UUID of the shop that contains the trade.
     * @param tradePosition     The position of the trade in the shop.
     * @param firstItem         The first item of the trade.
     * @param firstItemTag      If set, all materials from this tags will be allowed.
     * @param secondItem        The second item of the trade.
     * @param secondItemTag     If set, all materials from this tags will be allowed.
     * @param resultItem        The resulting item of the trade.
     * @param maxTradeUse       The maximum number of times the trade can be used. (0 to disable limit)
     * @param maxTradeTagsNames The tags associated with the maximum trade uses.
     */
    public Trade(@NotNull UUID shopUuid, int tradePosition,
                 @NotNull ItemStack firstItem, @Nullable Tag<Material> firstItemTag,
                 @Nullable ItemStack secondItem, @Nullable Tag<Material> secondItemTag,
                 @NotNull ItemStack resultItem,
                 int maxTradeUse, @Nullable List<String> maxTradeTagsNames) {
        this.shopUuid = shopUuid;
        this.tradePosition = tradePosition;
        this.firstItem = firstItem;
        this.firstItemTag = firstItemTag;
        this.secondItem = secondItem;
        this.secondItemTag = secondItemTag;
        this.resultItem = resultItem;
        enabled = true;
        this.maxTradeUse = maxTradeUse;
        this.maxTradeTagsNames = maxTradeTagsNames;
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
    public List<String> getMaxTradeTagsNames() {
        return maxTradeTagsNames;
    }

    /**
     * Sets the tags associated with the maximum trade uses.
     *
     * @param maxTradeTagsNames The new tags associated with the maximum trade uses.
     */
    public void setMaxTradeTags(List<String> maxTradeTagsNames) {
        this.maxTradeTagsNames = maxTradeTagsNames;
    }

    /**
     * Checks if the trade is limited by usage.
     *
     * @return true if the trade is limited by usage, false otherwise.
     */
    public boolean isUsageLimited() {
        return maxTradeUse > 0 && maxTradeTagsNames != null && !maxTradeTagsNames.isEmpty();
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
        return firstItemTag != null || secondItemTag != null || maxTradeUse > 0 || maxTradeTagsNames != null;
    }
}
