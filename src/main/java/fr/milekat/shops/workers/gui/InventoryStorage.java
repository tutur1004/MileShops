package fr.milekat.shops.workers.gui;

import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public record InventoryStorage(@NotNull Inventory inventory, int size, @Nullable ItemStack itemStack,
                               @Nullable BlockStateMeta blockStateMeta, @Nullable ShulkerBox shulkerBox) {
    public InventoryStorage(@NotNull Inventory inventory, int size) {
        this(inventory, size, null, null, null);
    }

    @Contract(pure = true)
    public boolean isPlayerInventory() {
        return itemStack == null || blockStateMeta == null || shulkerBox == null;
    }

    @Contract(pure = true)
    public boolean isShulkerBox() {
        return blockStateMeta != null && itemStack != null && shulkerBox != null;
    }
}
