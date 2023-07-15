package fr.milekat.shops.api.classes;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Trade {
    private final UUID shopUuid;
    private int tradePosition;
    private ItemStack firstItem;
    private ItemStack secondItem;
    private ItemStack resultItem;
    private boolean enabled; // TODO implement
    private int maxTradeUse; // TODO Add group feature ?

    public Trade(UUID shopUuid, int tradePosition, ItemStack firstItem, ItemStack secondItem, ItemStack resultItem) {
        this.shopUuid = shopUuid;
        this.tradePosition = tradePosition;
        this.firstItem = firstItem;
        this.secondItem = secondItem;
        this.resultItem = resultItem;
        enabled = true;
        this.maxTradeUse = 0;
    }

    public UUID getShopUuid() {
        return shopUuid;
    }

    public int getTradePosition() {
        return tradePosition;
    }

    public void setTradePosition(int tradePosition) {
        this.tradePosition = tradePosition;
    }

    public ItemStack getFirstItem() {
        return firstItem;
    }

    public void setFirstItem(ItemStack firstItem) {
        this.firstItem = firstItem;
    }

    public ItemStack getSecondItem() {
        return secondItem;
    }

    public void setSecondItem(ItemStack secondItem) {
        this.secondItem = secondItem;
    }

    public ItemStack getResultItem() {
        return resultItem;
    }

    public void setResultItem(ItemStack resultItem) {
        this.resultItem = resultItem;
    }

    public int getMaxTradeUse() {
        return maxTradeUse;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setMaxTradeUse(int maxTradeUse) {
        this.maxTradeUse = maxTradeUse;
    }
}
