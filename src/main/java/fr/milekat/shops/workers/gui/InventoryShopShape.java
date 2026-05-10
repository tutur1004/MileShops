package fr.milekat.shops.workers.gui;

import fr.milekat.shops.Main;
import fr.milekat.shops.workers.utils.Buttons;
import fr.mrmicky.fastinv.FastInv;
import net.kyori.adventure.text.Component;
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
    //  TODO - 2025/10/16 : Merge with ShopType enum

    //  A shop with 1 column of 4 trades without secondary item
    BASIC_FOUR(InventoryType.CHEST, 54,
            Main.getConfigs().getMessage("messages.gui.chest-shop.title.basic-four", "&3Shop <shop_name>"),
            List.of(new TradeSlots(11, null, 15,
                            Map.of(13, Buttons.HEAD_LEFT.get())),
                    new TradeSlots(20, null, 24,
                            Map.of(22, Buttons.HEAD_LEFT.get())),
                    new TradeSlots(29, null, 33,
                            Map.of(31, Buttons.HEAD_LEFT.get())),
                    new TradeSlots(38, null, 42,
                            Map.of(40, Buttons.HEAD_LEFT.get()))),
            45, 53, 4, 49,
            true
    ),

    //  A shop with 2 columns of 6 trades without secondary item
    DOUBLE_SIX(InventoryType.CHEST, 54,
            Main.getConfigs().getMessage("messages.gui.chest-shop.title.double-six", "&3Shop <shop_name>"),
            List.of(
                    //  First column
                    new TradeSlots(0, null, 3, new HashMap<>()),
                    new TradeSlots(9, null, 12, new HashMap<>()),
                    new TradeSlots(18, null, 21, new HashMap<>()),
                    new TradeSlots(27, null, 30, new HashMap<>()),
                    new TradeSlots(36, null, 39, new HashMap<>()),
                    new TradeSlots(45, null, 48, new HashMap<>()),
                    //  Second column
                    new TradeSlots(5, null, 8, new HashMap<>()),
                    new TradeSlots(14, null, 17, new HashMap<>()),
                    new TradeSlots(23, null, 26, new HashMap<>()),
                    new TradeSlots(32, null, 35, new HashMap<>()),
                    new TradeSlots(41, null, 44, new HashMap<>()),
                    new TradeSlots(50, null, 53, new HashMap<>())
            ),

            0, 0, 0, 0,
            false
    ),

    SINGLE_ONE(InventoryType.CHEST, 27,
            Main.getConfigs().getMessage("messages.gui.chest-shop.title.single-one", "&3Shop <shop_name>"),
            List.of(new TradeSlots(11, null, 15,
                            Map.of(15, Buttons.CUSTOM_ARROW_LEFT.get(), 19, Buttons.FROST_GLOW.get()))),
            0, 0, 0, 0,
            false
    ),

    SINGLE_SIX(InventoryType.CHEST, 54,
            Main.getConfigs().getMessage("messages.gui.chest-shop.title.single-six", "&3Shop <shop_name>"),
            List.of(
                    new TradeSlots(2, null, 6, new HashMap<>()),
                    new TradeSlots(11, null, 15, new HashMap<>()),
                    new TradeSlots(20, null, 24, new HashMap<>()),
                    new TradeSlots(29, null, 33, new HashMap<>()),
                    new TradeSlots(38, null, 42, new HashMap<>()),
                    new TradeSlots(47, null, 51, new HashMap<>())
            ),
            0, 0, 0, 0,
            false
    ),

    COMPACT_ONE(InventoryType.CHEST, 45,
            Main.getConfigs().getMessage("messages.gui.chest-shop.title.compact-one", "&3Shop <shop_name>"),
            List.of(new TradeSlots(23, null, 25,
                    Map.of(24, Buttons.CUSTOM_ARROW_LEFT.get(), 28, Buttons.FROST_GLOW.get()))),
                    0, 0, 0, 0,
                    false
            ),

    COMPACT_FIVE(InventoryType.CHEST, 45,
            Main.getConfigs().getMessage("messages.gui.chest-shop.title.compact-five", "&3Shop <shop_name>"),
            List.of(
                    new TradeSlots(5, null, 7,
                            Map.of(6, Buttons.CUSTOM_ARROW_LEFT.get(), 28, Buttons.FROST_GLOW.get())),
                    new TradeSlots(14, null, 16,
                            Map.of(15, Buttons.CUSTOM_ARROW_LEFT.get())),
                    new TradeSlots(23, null, 25,
                            Map.of(24, Buttons.CUSTOM_ARROW_LEFT.get())),
                    new TradeSlots(32, null, 34,
                            Map.of(33, Buttons.CUSTOM_ARROW_LEFT.get())),
                    new TradeSlots(41, null, 43,
                            Map.of(42, Buttons.CUSTOM_ARROW_LEFT.get()))
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
    public @NotNull Function<FastInv, Inventory> getInventoryFunction(String shopName) {
        if (inventoryType == InventoryType.CHEST && size != 0) {
            return owner -> Bukkit.createInventory(owner, size, Component.text(title.replaceAll("<shop_name>", shopName)));
        } else {
            return owner -> Bukkit.createInventory(owner, inventoryType, Component.text(title.replaceAll("<shop_name>", shopName)));
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
