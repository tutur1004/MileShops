package fr.milekat.shops.workers.gui;

import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.api.events.PlayerOpenShop;
import fr.milekat.shops.workers.utils.Buttons;
import fr.milekat.shops.workers.utils.TradeMode;
import fr.milekat.utils.storage.exceptions.StorageExecuteException;
import fr.mrmicky.fastinv.FastInv;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class InventoryShop extends FastInv {
    //  Shop shape parameters
    private final List<TradeSlots> tradeSlots;
    private final int tradePerPages;
    private final int previousPageSlot;
    private final int nextPageSlot;
    private final int inventoryModeSlot;
    private final int closeSlot;
    private final boolean fillBackground;

    private final Player player;
    private TradeMode tradeMode;
    private int currentPage = 1;
    private final Map<Integer, List<Trade>> pagesTrades;

    public InventoryShop(@NotNull InventoryShopShape inventoryShopShape, @NotNull Shop shop, @NotNull Player player)
            throws StorageExecuteException {
        //  Shop shape parameters
        super(inventoryShopShape.getInventoryFunction(shop.getName()));
        this.tradeSlots = inventoryShopShape.getTradeSlots();
        this.tradePerPages = tradeSlots.size();
        this.previousPageSlot = inventoryShopShape.getPreviousPageSlot();
        this.nextPageSlot = inventoryShopShape.getNextPageSlot();
        this.inventoryModeSlot = inventoryShopShape.getInventoryModeSlot();
        this.closeSlot = inventoryShopShape.getCloseSlot();
        this.fillBackground = inventoryShopShape.isFillBackground();

        //  Shop & player variables
        this.player = player;
        try {
            this.tradeMode = Main.getStorage().getCacheTradeMode(player.getUniqueId());
        } catch (Exception ignore) {
            this.tradeMode = TradeMode.INVENTORY;
        }
        //  Trades
        List<Trade> trades = Main.getStorage().getCacheTrades(shop.getUuid());
        this.pagesTrades = sortTradesPerPages(trades);

        //  Create the inventory
        updatePageContent();

        //  Open the inventory to the player
        PlayerOpenShop event = new PlayerOpenShop(player, shop);
        Main.getInstance().getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            open(player);
        }
    }

    private @NotNull Map<Integer, List<Trade>> sortTradesPerPages(@NotNull List<Trade> trades) {
        //  Sorted trades output
        Map<Integer, List<Trade>> pagesTrades = new HashMap<>();
        // Counter for the number of trades on the current page
        int pageTradeCount = 1;
        // Counter for the current page number
        int page = 1;
        // Temporary list of trades for the current page
        List<Trade> tradesLoop = new LinkedList<>();

        for (Trade trade : trades) {
            tradesLoop.add(trade);
            pageTradeCount++;

            // Add trades to current page
            pagesTrades.put(page, tradesLoop);

            // If current page is full, prepare for the next page
            if (pageTradeCount > this.tradePerPages) {
                tradesLoop = new LinkedList<>();
                pageTradeCount = 1;
                page++;
            }
        }
        return pagesTrades;  // Return sorted trades
    }

    private void updatePageContent() {
        if (this.fillBackground) fillBackground();
        if (this.inventoryModeSlot != 0) updateTradeModeButton();
        if (this.closeSlot != 0) setItem(this.closeSlot, Buttons.EXIT.get(), event -> player.closeInventory());
        if (this.previousPageSlot != 0 || this.nextPageSlot != 0) updatePageButtons();
        if (pagesTrades.containsKey(this.currentPage)) {
            int pagePosition = 0;
            for (Trade trade : pagesTrades.get(this.currentPage)) {
                displayTrade(tradeSlots.get(pagePosition), trade);
                pagePosition++;
            }
        }
    }

    private void fillBackground() {
        setItems(0, getInventory().getSize(), Buttons.PANE_WHITE.get());
        setItems(getBorders(), Buttons.PANE_BLACK.get());
    }

    private void updateTradeModeButton() {
        if (tradeMode.equals(TradeMode.INVENTORY)) {
            setItem(this.inventoryModeSlot, Buttons.MODE_CHEST.get(), event -> {
                tradeMode = TradeMode.SHULKER;
                Main.getStorage().asyncSaveTradeMode(player.getUniqueId(), tradeMode);
                updateTradeModeButton();
            });
        } else if (tradeMode.equals(TradeMode.SHULKER)) {
            setItem(this.inventoryModeSlot, Buttons.MODE_SHULKER.get(), event -> {
                tradeMode = TradeMode.INVENTORY;
                Main.getStorage().asyncSaveTradeMode(player.getUniqueId(), tradeMode);
                updateTradeModeButton();
            });
        }
    }

    private void updatePageButtons() {
        if (this.currentPage > 1 ) {
            setItem(this.previousPageSlot, Buttons.PREVIOUS.get(), event -> {
                if (this.currentPage > 1) {
                    this.currentPage--;
                }
                updatePageContent();
            });
        }
        if (pagesTrades.containsKey(currentPage + 1)) {
            setItem(this.nextPageSlot, Buttons.NEXT.get(), event -> {
                if (this.currentPage >= 64) return;
                this.currentPage++;
                updatePageContent();
            });
        }
    }

    private void displayTrade(@NotNull TradeSlots tradeSlots, @NotNull Trade trade) {
        //  Display additional trade items (Arrow, etc...)
        tradeSlots.additionalTradeItems().forEach(this::setItem);
        //  Display trade items
        setItem(tradeSlots.firstItemSlot(), trade.getFirstItem().clone());
        if (trade.getSecondItem() != null && tradeSlots.secondItemSlot() != null) {
            setItem(tradeSlots.resultItemSlot(), trade.getSecondItem().clone());
        }
        setItem(tradeSlots.resultItemSlot(), trade.getResultItem().clone(),
                event -> requestTrade(trade, event.getClick()));
    }

    private void requestTrade(@NotNull Trade trade, ClickType click) {
        //  TODO: Process a trade request (Mean loop as much as needed)
    }

    private boolean processTrade(@NotNull Trade trade) {
        //  TODO: Process a single trade (Mean proceed a trade and return true if success)
        return false;
    }

    @Override
    protected void onClose(InventoryCloseEvent event) {
        super.onClose(event);
    }
}
