package fr.milekat.shops.workers.gui;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record TradeSlots(@NotNull Integer firstItemSlot, @Nullable Integer secondItemSlot,
                         @NotNull Integer resultItemSlot, @NotNull Map<Integer, ItemStack> additionalTradeItems) {
    public TradeSlots {
        if (firstItemSlot < 0 || firstItemSlot > 53) {
            throw new IllegalArgumentException("First item slot must be between 0 and 53");
        }
        if (secondItemSlot != null && (secondItemSlot < 0 || secondItemSlot > 53)) {
            throw new IllegalArgumentException("Second item slot must be between 0 and 53");
        }
        if (resultItemSlot < 0 || resultItemSlot > 53) {
            throw new IllegalArgumentException("Result item slot must be between 0 and 53");
        }
    }
}
