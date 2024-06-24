package fr.milekat.shops.workers.gui;

import fr.milekat.shops.Main;
import fr.milekat.shops.workers.utils.Buttons;
import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("unused")
public enum InventoryShopShape {
    //  A shop with 1 column of 4 trades without secondary item
    BASIC_FOUR(InventoryType.CHEST, 54,
            Main.getConfigs().getMessage("messages.gui.chest-shop.title", "&3Shop <shop_name>"),
            List.of(new TradeSlots(11, null, 15,
                            Map.of(13, Buttons.HEAD_LEFT.get())),
                    new TradeSlots(20, null, 25,
                            Map.of(22, Buttons.HEAD_LEFT.get())),
                    new TradeSlots(29, null, 34,
                            Map.of(31, Buttons.HEAD_LEFT.get())),
                    new TradeSlots(38, null, 43,
                            Map.of(40, Buttons.HEAD_LEFT.get()))),
            45, 53, 0, 49,
            true
    ),

    //  A shop with 2 columns of 6 trades without secondary item
    DOUBLE_SIX(InventoryType.CHEST, 54,
            Main.getConfigs().getMessage("messages.gui.chest-shop.title", "&3Shop <shop_name>"),
            List.of(
                    //  First column
                    new TradeSlots(1, null, 3, new HashMap<>()),
                    new TradeSlots(10, null, 12, new HashMap<>()),
                    new TradeSlots(19, null, 21, new HashMap<>()),
                    new TradeSlots(28, null, 30, new HashMap<>()),
                    new TradeSlots(37, null, 39, new HashMap<>()),
                    new TradeSlots(46, null, 48, new HashMap<>()),
                    //  Second column
                    new TradeSlots(5, null, 7, new HashMap<>()),
                    new TradeSlots(14, null, 16, new HashMap<>()),
                    new TradeSlots(23, null, 25, new HashMap<>()),
                    new TradeSlots(32, null, 34, new HashMap<>()),
                    new TradeSlots(41, null, 43, new HashMap<>()),
                    new TradeSlots(50, null, 52, new HashMap<>())
            ),

            0, 0, 0, 0,
            false
    );

    private final InventoryType inventoryType;
    private final int size;
    private final String title;
    private final List<TradeSlots> tradeSlots;
    private final int previousPageSlot;
    private final int nextPageSlot;
    private final int inventoryModeSlot;
    private final int closeSlot;
    private final boolean fillBackground;

    InventoryShopShape(InventoryType type, int size, String title,
                       List<TradeSlots> tradeSlots,
                       int previousPageSlot, int nextPageSlot, int inventoryModeSlot, int closeSlot,
                       boolean fillBackground
              ) {
        this.inventoryType = type;
        this.size = size;
        this.title = title;
        this.tradeSlots = tradeSlots;
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

    public InventoryType getInventoryType() {
        return inventoryType;
    }

    public List<TradeSlots> getTradeSlots() {
        return tradeSlots;
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
