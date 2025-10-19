package fr.milekat.shops.workers.gui;

import fr.milekat.shops.Main;
import fr.milekat.shops.api.MileShopsAPI;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.api.events.PlayerOpenShop;
import fr.milekat.shops.api.events.TradeCompleteEvent;
import fr.milekat.shops.api.exceptions.ApiUnavailable;
import fr.milekat.shops.workers.utils.Buttons;
import fr.milekat.shops.workers.utils.TradeMode;
import fr.milekat.shops.workers.utils.TradeUtils;
import fr.milekat.utils.storage.exceptions.StorageExecuteException;
import fr.mrmicky.fastinv.FastInv;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

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

    //  Shop & player variables
    private final Shop shop;
    private final Player player;
    private final Map<Integer, List<Trade>> pagesTrades;
    private final Map<Trade, Integer> tradeCompleted = new HashMap<>();
    private TradeMode tradeMode;
    private int currentPage = 1;
    private boolean isProcessingTrade = false;

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
        this.shop = shop;
        this.player = player;
        try {
            this.tradeMode = Main.getStorage().getCacheTradeMode(player.getUniqueId());
        } catch (Exception ignore) {
            this.tradeMode = TradeUtils.getDefaultTradeMode();
        }
        //  Get trades for this shop
        List<Trade> trades = Main.getStorage().getCacheTrades(shop.getUuid());
        //  Sort trades per pages
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
        //  Sort trades by trade positions
        trades.sort(Comparator.comparingInt(Trade::getTradePosition));

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
        setItems(0, getInventory().getSize() - 1, Buttons.PANE_WHITE.get());
        setItems(getBorders(), Buttons.PANE_BLACK.get());
    }

    private void updateTradeModeButton() {
        switch (tradeMode) {
            case INVENTORY -> setItem(this.inventoryModeSlot, Buttons.MODE_INVENTORY.get(),
                    event -> {
                        tradeMode = TradeMode.ENDER_CHEST;
                        try {
                            Main.getStorage().saveTradeMode(player.getUniqueId(), tradeMode);
                        } catch (Exception ignore) {}
                        updateTradeModeButton();
                    });
            case ENDER_CHEST -> setItem(this.inventoryModeSlot, Buttons.MODE_ENDER_CHEST.get(),
                    event -> {
                        tradeMode = TradeMode.SHULKER;
                        try {
                            Main.getStorage().saveTradeMode(player.getUniqueId(), tradeMode);
                        } catch (Exception ignore) {}
                        updateTradeModeButton();
                    });
            case SHULKER -> setItem(this.inventoryModeSlot, Buttons.MODE_SHULKER.get(),
                    event -> {
                        tradeMode = TradeMode.END_SHULKER;
                        try {
                            Main.getStorage().saveTradeMode(player.getUniqueId(), tradeMode);
                        } catch (Exception ignore) {}
                        updateTradeModeButton();
                    });
            case END_SHULKER -> setItem(this.inventoryModeSlot, Buttons.MODE_END_SHULKER.get(),
                    event -> {
                        tradeMode = TradeMode.INVENTORY;
                        try {
                            Main.getStorage().saveTradeMode(player.getUniqueId(), tradeMode);
                        } catch (Exception ignore) {}
                        updateTradeModeButton();
                    });
        }
    }

    private void updatePageButtons() {
        if (this.currentPage > 1) {
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
                event -> {
            // Ensure the click is on the result item slot
            if (event.getSlot() == tradeSlots.resultItemSlot()) {
                requestTrade(trade, event.getClick());
            }
        });
    }

    private void requestTrade(@NotNull Trade trade, @NotNull ClickType click) {
        //  Check if a trade is already being processed
        if (isProcessingTrade) {
            Main.message(player, Main.getConfigs().getMessage("messages.gui.chest-shop.messages.trade-processing",
                    "&cA trade is already being processed, please wait..."));
            return;
        }

        int processedTrades = processedTrades(trade, click.isShiftClick());
        if (processedTrades > 0) {
            tradeCompleted.put(trade, tradeCompleted.getOrDefault(trade, 0) + processedTrades);
        }
    }

    private int processedTrades(@NotNull Trade trade, boolean fullInventories) {
        //  Lock the trade processing
        isProcessingTrade = true;

        try {
            //  Set the trade items
            List<ItemStack> tradeItems = new LinkedList<>();
            tradeItems.add(trade.getFirstItem().clone());
            if (trade.getSecondItem() != null) {
                tradeItems.add(trade.getSecondItem().clone());
            }

            //  Calculate the max doable trades
            int maxDoAbleTrades = TradeUtils.maxDoAbleTrades(this.player, tradeItems,
                    trade.getResultItem().clone(), fullInventories, tradeMode);

            //  If no trades can be done, return 0
            if (maxDoAbleTrades <= 0) {
                Main.message(player, Main.getConfigs().getMessage("messages.gui.chest-shop.messages.no-trade",
                        "&cYou don't have the required items to trade, or your inventory is full"));
                return 0;
            }

            //  Trade usage limitation
            if (trade.isUsageLimited()) {
                try {
                    Map<String, Object> playerTags = MileShopsAPI.getAPI().getPlayerTags(player.getUniqueId());
                    if (playerTags != null && !playerTags.isEmpty()) {
                        Map<String, Object> playerTradeTags = new HashMap<>();
                        trade.getMaxTradeTagsNames().stream()
                                .filter(playerTags::containsKey)
                                .forEach(tag -> playerTradeTags.put(tag, playerTags.get(tag)));
                        if (!playerTradeTags.isEmpty()) {
                            int tradeUses = Main.getStorage().getTradeUses(playerTradeTags, trade);
                            int maxDoAllowedTrades = trade.getMaxTradeUse() - tradeUses;
                            if (maxDoAllowedTrades < maxDoAbleTrades) {
                                Main.message(player, Main.getConfigs().getMessage(
                                                "messages.gui.chest-shop.messages.max-trade",
                                                "&cYou have reached the maximum number of uses for this trade(<trade_limit>).")
                                        .replace("<trade_limit>", String.valueOf(tradeUses)));
                                if (maxDoAllowedTrades <= 0) return 0;
                                maxDoAbleTrades = maxDoAllowedTrades;
                            }
                        }
                    }
                } catch (ApiUnavailable ignore) {}
            }

            //  Execute the trade
            TradeUtils.executeTrade(this.player, tradeItems, trade.getResultItem().clone(),
                    maxDoAbleTrades, tradeMode);

            //  Call the TradeCompleteEvent
            for (int i = 0; i < maxDoAbleTrades; i++) {
                TradeCompleteEvent event = new TradeCompleteEvent(player, shop, trade);
                Main.getInstance().getServer().getPluginManager().callEvent(event);
            }
            return maxDoAbleTrades;
        } finally {
            //  Unlock the trade processing
            isProcessingTrade = false;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onClose(InventoryCloseEvent event) {
        super.onClose(event);
        if (!player.hasPermission("shops.admin")) return;
        this.tradeCompleted.forEach((trade, count) -> {
            BaseComponent message = new TextComponent(TradeUtils.tradeFormatting(Main.getConfigs()
                    .getMessage("messages.gui.chest-shop.messages.trade-result",
                            "&2You trade <first_amount>x<first_material>, " +
                                    "for <result_amount>x<result_material>."), shop, trade, count));
            message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text(Main.getConfigs()
                            .getMessages("messages.gui.chest-shop.messages.trade-result-hover")
                            .stream()
                            .map(line -> TradeUtils.tradeFormatting(line, shop, trade, count))
                            .collect(Collectors.joining(System.lineSeparator(), "", "")))));
            Main.message(player, message);
        });
    }
}
