package fr.milekat.shops.workers.gui;

import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.api.events.PlayerOpenShop;
import fr.milekat.shops.workers.utils.Buttons;
import fr.milekat.shops.api.classes.TradeMode;
import fr.milekat.shops.workers.utils.TradeUtils;
import fr.milekat.utils.storage.exceptions.StorageExecuteException;
import fr.mrmicky.fastinv.FastInv;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    //  Tag trade carousel
    private @Nullable BukkitTask carouselTask;
    private final Map<Integer, TagCarouselEntry> tagCarousels = new HashMap<>();
    private int carouselTick = 0;
    //  Carrousel delay in ticks (20 ticks = 1 second)
    final long carouselDelay = 16L;

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
        stopCarousel();
        tagCarousels.clear();
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
        startCarousel();
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
        TradeGuiItems guiItems = getGuiItem(trade);

        // First item display (or tag carousel)
        if (trade.getFirstItemTag() != null) {
            List<Material> materials = new ArrayList<>(trade.getFirstItemTag().getValues());
            tagCarousels.put(tradeSlots.firstItemSlot(),
                    new TagCarouselEntry(materials, trade.getFirstItem().getAmount()));
            setItem(tradeSlots.firstItemSlot(), guiItems.firstItem());
        } else {
            setItem(tradeSlots.firstItemSlot(), guiItems.firstItem());
        }

        // Second item display (or tag carousel)
        if (trade.getSecondItem() != null && tradeSlots.secondItemSlot() != null) {
            if (trade.getSecondItemTag() != null) {
                List<Material> materials = new ArrayList<>(trade.getSecondItemTag().getValues());
                tagCarousels.put(tradeSlots.secondItemSlot(),
                        new TagCarouselEntry(materials, trade.getSecondItem().getAmount()));
                setItem(tradeSlots.secondItemSlot(), guiItems.secondItem());
            } else {
                setItem(tradeSlots.secondItemSlot(), guiItems.secondItem());
            }
        }

        // Result item display (always exact)
        setItem(tradeSlots.resultItemSlot(), guiItems.resultItem(),
                event -> {
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

        try {
            //  Lock player from trading
            isProcessingTrade = true;

            int processedTrades = TradeUtils.processedTrades(player, tradeMode, shop, trade, click.isShiftClick());
            if (processedTrades > 0) {
                tradeCompleted.put(trade, tradeCompleted.getOrDefault(trade, 0) + processedTrades);
            }

        } catch (Exception ignore) {} finally {
            //  Unlock player for trading
            isProcessingTrade = false;
        }
    }

    private void startCarousel() {
        if (tagCarousels.isEmpty()) return;
        if (carouselTask != null) carouselTask.cancel();
        carouselTask = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
            carouselTick++;
            for (Map.Entry<Integer, TagCarouselEntry> entry : tagCarousels.entrySet()) {
                int slot = entry.getKey();
                TagCarouselEntry carousel = entry.getValue();
                List<Material> materials = carousel.materials();
                int index = carouselTick % materials.size();
                ItemStack display = new ItemStack(materials.get(index), carousel.amount());
                ItemMeta meta = display.getItemMeta();
                if (meta != null) {
                    meta.getPersistentDataContainer().set(TradeGuiItems.key,
                            PersistentDataType.STRING, UUID.randomUUID().toString());
                }
                display.setItemMeta(meta);
                getInventory().setItem(slot, display);
            }
        }, carouselDelay, carouselDelay);
    }

    private void stopCarousel() {
        if (carouselTask != null) {
            carouselTask.cancel();
            carouselTask = null;
        }
    }

    @Override
    protected void onClose(InventoryCloseEvent event) {
        stopCarousel();
        super.onClose(event);
        if (!player.hasPermission("shops.admin")) return;
        this.tradeCompleted.forEach((trade, count) -> {
            Component message = Component.text(TradeUtils.tradeFormatting(Main.getConfigs()
                    .getMessage("messages.gui.chest-shop.messages.trade-result",
                            "&2You trade <first_amount>x<first_material>, " +
                                    "for <result_amount>x<result_material>."), shop, trade, count))
            .hoverEvent(HoverEvent.showText(Component.text(Main.getConfigs()
                            .getMessages("messages.gui.chest-shop.messages.trade-result-hover")
                            .stream()
                            .map(line -> TradeUtils.tradeFormatting(line, shop, trade, count))
                            .collect(Collectors.joining(System.lineSeparator(), "", "")))));
            Main.message(player, message);
        });
    }

    @Contract("_ -> new")
    private @NotNull TradeGuiItems getGuiItem(@NotNull Trade trade) {
        return new TradeGuiItems(trade.getFirstItem(), trade.getSecondItem(), trade.getResultItem());
    }

    private record TagCarouselEntry(@NotNull List<Material> materials, int amount) {}

    private record TradeGuiItems(@NotNull ItemStack firstItem,
                                 @Nullable ItemStack secondItem,
                                 @NotNull ItemStack resultItem) {
        static NamespacedKey key = new NamespacedKey(Main.getInstance(), "mile_shops_gui_item");

        public TradeGuiItems {
            firstItem = getItem(firstItem);
            if (secondItem != null) {
                secondItem = getItem(secondItem);
            }
            resultItem = getItem(resultItem);
        }

        private @NotNull ItemStack getItem(@NotNull ItemStack item) {
            ItemStack guiItem = item.clone();
            ItemMeta meta = guiItem.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, UUID.randomUUID().toString());
            }
            guiItem.setItemMeta(meta);

            return guiItem;
        }
    }
}
