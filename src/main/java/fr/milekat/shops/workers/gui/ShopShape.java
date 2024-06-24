package fr.milekat.shops.workers.gui;

import fr.milekat.shops.Main;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@SuppressWarnings("unused")
public enum ShopShape {
    //  A shop with 1 column of 4 trades without secondary item
    BASIC_FOUR(InventoryType.CHEST, 54,
            Main.getConfigs().getMessage("messages.gui.chest-shop.title", "&3Shop <shop_name>"),
            List.of(11, 20, 29, 38),
            new ArrayList<>(),
            List.of(15, 25, 34, 43),
            List.of(13, 22, 31, 40),
            45, 53, 4, 49,
            true
    ),

    //  A shop with 2 columns of 6 trades without secondary item
    DOUBLE_SIX(InventoryType.CHEST, 54,
            Main.getConfigs().getMessage("messages.gui.chest-shop.title", "&3Shop <shop_name>"),
            List.of(1, 10, 19, 28, 37, 46, 5, 14, 23, 32, 41, 50),
            new ArrayList<>(),
            List.of(3, 12, 21, 30, 39, 48, 7, 16, 25, 34, 43, 52),
            new ArrayList<>(),
            0, 0, 0, 0,
            false
    );

    private final InventoryType inventoryType;
    private final int size;
    private final String title;
    private final List<Integer> firstItemSlots;
    private final List<Integer> secondItemSlots;
    private final List<Integer> resultItemSlots;
    private final List<Integer> tradesArrowSlots;
    private final int previousPageSlot;
    private final int nextPageSlot;
    private final int inventoryModeSlot;
    private final int closeSlot;
    private final boolean fillBackground;

    ShopShape(InventoryType type, int size, String title,
              List<Integer> firstItemSlots, List<Integer> secondItemSlots, List<Integer> resultItemSlots,
              List<Integer> tradesArrowSlots,
              int previousPageSlot, int nextPageSlot, int inventoryModeSlot, int closeSlot,
              boolean fillBackground
              ) {
        this.inventoryType = type;
        this.size = size;
        this.title = title;
        this.firstItemSlots = firstItemSlots;
        this.secondItemSlots = secondItemSlots;
        this.resultItemSlots = resultItemSlots;
        this.tradesArrowSlots = tradesArrowSlots;
        this.previousPageSlot = previousPageSlot;
        this.nextPageSlot = nextPageSlot;
        this.inventoryModeSlot = inventoryModeSlot;
        this.closeSlot = closeSlot;
        this.fillBackground = fillBackground;
    }

    @Contract(pure = true)
    public @NotNull Function<InventoryHolder, Inventory> getInventoryFunction(String shopName) {
        if (inventoryType == InventoryType.CHEST && size != 0) {
            return owner -> Bukkit.createInventory(owner, size, title.replaceAll("<shop_name>", shopName));
        } else {
            return owner -> Bukkit.createInventory(owner, inventoryType, title.replaceAll("<shop_name>", shopName));
        }
    }

    public List<Integer> getFirstItemSlots() {
        return firstItemSlots;
    }

    public List<Integer> getSecondItemSlots() {
        return secondItemSlots;
    }

    public List<Integer> getResultItemSlots() {
        return resultItemSlots;
    }

    public List<Integer> getTradesArrowSlots() {
        return tradesArrowSlots;
    }

    public int getPreviousPageSlot() {
        return previousPageSlot;
    }

    public int getNextPageSlot() {
        return nextPageSlot;
    }

    public int getInventoryModeSlot() {
        return inventoryModeSlot;
    }

    public int getCloseSlot() {
        return closeSlot;
    }

    public boolean isFillBackground() {
        return fillBackground;
    }
}
