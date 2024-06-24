package fr.milekat.shops.workers.gui;

import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.workers.utils.Buttons;
import fr.milekat.shops.workers.utils.TradeMode;
import fr.milekat.utils.storage.exceptions.StorageExecuteException;
import fr.mrmicky.fastinv.FastInv;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

@SuppressWarnings("unused")
public class InventoryShop extends FastInv {
    //  Shop shape parameters
    private final List<Integer> firstItemSlots;
    private final List<Integer> secondItemSlots;
    private final List<Integer> resultItemSlots;
    private final List<Integer> tradesArrowSlots;
    private final int tradePerPages;
    private final int previousPageSlot;
    private final int nextPageSlot;
    private final int inventoryModeSlot;
    private final int closeSlot;
    private final boolean fillBackground;

    //  Shop & player variables
    private final Shop shop;
    private final Player player;
    private TradeMode tradeMode;
    private int currentPage = 1;
    private final Map<Integer, List<Trade>> pagesTrades;

    public InventoryShop(@NotNull ShopShape shopShape, @NotNull Shop shop, @NotNull Player player)
            throws StorageExecuteException {
        //  Shop shape parameters
        super(shopShape.getInventoryFunction(shop.getName()));
        this.firstItemSlots = shopShape.getFirstItemSlots();
        this.secondItemSlots = shopShape.getSecondItemSlots();
        this.resultItemSlots = shopShape.getResultItemSlots();
        this.tradesArrowSlots = shopShape.getTradesArrowSlots();
        this.tradePerPages = firstItemSlots.size();
        this.previousPageSlot = shopShape.getPreviousPageSlot();
        this.nextPageSlot = shopShape.getNextPageSlot();
        this.inventoryModeSlot = shopShape.getInventoryModeSlot();
        this.closeSlot = shopShape.getCloseSlot();
        this.fillBackground = shopShape.isFillBackground();

        if (this.firstItemSlots.size() != this.resultItemSlots.size()) {
            Main.getMileLogger().log(Level.SEVERE,
                    "The number of first and result item slots must be the same.");
            throw new IllegalStateException("The number of first and result item slots must be the same.");
        }

        //  Shop & player variables
        this.shop = shop;
        this.player = player;
        try {
            this.tradeMode = Main.getStorage().getCacheTradeMode(player.getUniqueId());
        } catch (Exception ignore) {
            this.tradeMode = TradeMode.INVENTORY;
        }
        //  Trades
        List<Trade> trades = Main.getStorage().getCacheTrades(shop.getUuid());
        this.pagesTrades = sortTradesPerPages(trades);

        if (this.firstItemSlots.size() != this.secondItemSlots.size() &&
                trades.stream().anyMatch(trade -> trade.getSecondItem() != null)) {
            Main.getMileLogger().log(Level.SEVERE,
                    "The number of first and second item slots must be the same.");
            throw new IllegalStateException("The number of first and second item slots must be the same.");
        }

        //  Create the inventory
        updatePageContent();
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
        if (this.previousPageSlot != 0 || this.nextPageSlot != 0) updatePageButtons();
        if (pagesTrades.containsKey(this.currentPage)) {
            int pagePosition = 0;
            for (Trade trade : pagesTrades.get(this.currentPage)) {
                displayTrade(pagePosition, trade);
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

    private void displayTrade(int pagePosition, @NotNull Trade trade) {
        //  Display arrow if needed
        if (tradesArrowSlots.size() > pagePosition) {
            setItem(tradesArrowSlots.get(pagePosition), Buttons.HEAD_LEFT.get());
        }
        //  Display trade items
        setItem(firstItemSlots.get(pagePosition), trade.getFirstItem().clone());
        if (trade.getSecondItem() != null) {
            setItem(secondItemSlots.get(pagePosition), trade.getSecondItem().clone());
        }
        setItem(resultItemSlots.get(pagePosition), trade.getResultItem().clone(),
                event -> requestTrade(trade, event.getClick()));
    }

    private void requestTrade(@NotNull Trade trade, ClickType click) {
        //  TODO: Process a trade request (Mean loop as much as needed)
    }

    private boolean processTrade(@NotNull Trade trade) {
        //  TODO: Process a single trade (Mean proceed a trade and return true if success)
        return false;
    }
}
