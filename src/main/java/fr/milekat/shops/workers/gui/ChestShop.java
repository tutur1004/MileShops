package fr.milekat.shops.workers.gui;

import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.api.events.TradeCompleteEvent;
import fr.milekat.shops.workers.ShopsManager;
import fr.milekat.shops.workers.utils.Buttons;
import fr.milekat.shops.workers.utils.TradeMode;
import fr.milekat.shops.workers.utils.TradeUtils;
import fr.mrmicky.fastinv.FastInv;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChestShop extends FastInv {
    private final Player player;
    private final Shop shop;
    private final List<Trade> shopTrades;
    private int currentPage = 1;
    private final Map<Integer, List<Trade>> pagesTrades;
    private TradeMode tradeMode;
    private final Map<Integer, Integer> tradesDone = new HashMap<>();

    public ChestShop(Player player, @NotNull Shop shop, @NotNull List<Trade> trades) {
        //  9 top line + 9 bottom line + 1 line per trades (Max 4)
        super(54, Main.getConfigs()
                .getMessage("messages.gui.chest-shop.title", "&3Shop <shop_name>")
                .replaceAll("<shop_name>", shop.getName()));
        this.player = player;
        this.shop = shop;
        this.shopTrades = trades;
        try {
            this.tradeMode = Main.getStorage().getCacheTradeMode(player.getUniqueId().toString());
        } catch (Exception ignore) {
            this.tradeMode = TradeMode.INVENTORY;
        }
        Map<Integer, List<Trade>> pagesTrades = new HashMap<>();
        int pageTradeCount = 1;
        int page = 1;
        List<Trade> tradesLoop = new LinkedList<>();
        for (Trade trade : trades) {
            tradesLoop.add(trade);
            pageTradeCount++;
            pagesTrades.put(page, tradesLoop);
            if (pageTradeCount > ShopsManager.CHEST_TRADES_PER_PAGE) {
                tradesLoop = new LinkedList<>();
                pageTradeCount = 1;
                page++;
            }
        }
        this.pagesTrades = pagesTrades;
        //  Setup base inventory
        basicCanvas();
        //  Show content to player
        updatePageContent();
    }

    private void basicCanvas() {
        setItems(0, 53, Buttons.PANE_WHITE.get());
        setItems(getBorders(), Buttons.PANE_BLACK.get());
        //  Setup exit button
        setItem(getInventory().getSize() - 5, Buttons.EXIT.get());
        pageButtons();
    }

    private void pageButtons() {
        tradeModeButton();

        if (this.currentPage > 1 ) {
            setItem(45, Buttons.PREVIOUS.get(), event -> {
                if (this.currentPage > 1) {
                    this.currentPage--;
                }
                updatePageContent();
            });
        } else {
            setItem(45, Buttons.PANE_BLACK.get());
        }
        if (pagesTrades.containsKey(currentPage + 1)) {
            setItem(53, Buttons.NEXT.get(), event -> {
                if (this.currentPage >= 64) return;
                this.currentPage++;
                updatePageContent();
            });
        } else {
            setItem(53, Buttons.PANE_BLACK.get());
        }
    }

    private void tradeModeButton() {
        Main.getStorage().asyncSaveTradeMode(player.getUniqueId().toString(), tradeMode);
        if (tradeMode.equals(TradeMode.INVENTORY)) {
            setItem(4, Buttons.MODE_CHEST.get(), event -> {
                tradeMode = TradeMode.SHULKER;
                tradeModeButton();
            });
        } else if (tradeMode.equals(TradeMode.SHULKER)) {
            setItem(4, Buttons.MODE_SHULKER.get(), event -> {
                tradeMode = TradeMode.INVENTORY;
                tradeModeButton();
            });
        }
    }

    private void updatePageContent() {
        basicCanvas();
        if (pagesTrades.containsKey(this.currentPage)) {
            int position = 0;
            for (Trade trade : pagesTrades.get(this.currentPage)) {
                displayTrade(position, trade);
                position++;
            }
        }
        pageButtons();
    }

    private void displayTrade(int pagePosition, @NotNull Trade trade) {
        setItem(11 + (pagePosition * 9), trade.getFirstItem().clone()); //  TODO: Events
        setItem(13 + (pagePosition * 9), Buttons.HEAD_LEFT.get());
        setItem(15 + (pagePosition * 9), trade.getResultItem().clone(), event -> {
            if (isFull(player, trade.getResultItem())) {
                Main.message(player, TradeUtils.tradeFormatting(Main.getConfigs()
                        .getMessage("messages.gui.chest-shop.messages.inventory-space",
                                "&cNot enough space in inventory to receive result."), shop, trade, -1));
                return;
            }
            int trades = 0;
            if (tradeMode.equals(TradeMode.INVENTORY)) {
                if (event.getClick().equals(ClickType.SHIFT_LEFT)) {
                    boolean tradeDone;
                    do {
                        if (isFull(player, trade.getResultItem())) {
                            Main.message(player, TradeUtils.tradeFormatting(Main.getConfigs()
                                    .getMessage("messages.gui.chest-shop.messages.inventory-space",
                                            "&cNot enough space in inventory to receive result."),
                                    shop, trade, -1));
                            tradeDone = false;
                        } else {
                            tradeDone = checkPlayerInventory(trade);
                        }
                        if (tradeDone) {
                            player.getInventory().addItem(trade.getResultItem());
                            tradesDoneIncrement(trade);
                            trades++;
                        }
                    } while (tradeDone);
                } else if (checkPlayerInventory(trade)) {
                    player.getInventory().addItem(trade.getResultItem());
                    tradesDoneIncrement(trade);
                    trades++;
                }
            } else if (tradeMode.equals(TradeMode.SHULKER)) {
                if (event.getClick().equals(ClickType.SHIFT_LEFT)) {
                    boolean tradeDone;
                    do {
                        if (isFull(player, trade.getResultItem())) {
                            Main.message(player, TradeUtils.tradeFormatting(Main.getConfigs()
                                    .getMessage("messages.gui.chest-shop.messages.inventory-space",
                                            "&cNot enough space in inventory to receive result."),
                                    shop, trade, -1));
                            tradeDone = false;
                        } else {
                            tradeDone = checkPlayerAllInventories(trade);
                            if (tradeDone) {
                                player.getInventory().addItem(trade.getResultItem());
                                tradesDoneIncrement(trade);
                                trades++;
                            }
                        }
                    } while (tradeDone);
                } else if (checkPlayerAllInventories(trade)) {
                    player.getInventory().addItem(trade.getResultItem());
                    tradesDoneIncrement(trade);
                    trades++;
                }
            }
            if (trades==0) {
                Main.message(player, TradeUtils.tradeFormatting(Main.getConfigs().getMessage(
                        "messages.gui.chest-shop.messages.no-item", "&cNot enough items"),
                        shop, trade, -1));
            }
        });
    }

    public boolean isFull(@NotNull Player player, @NotNull ItemStack item) {
        Inventory virtualInventory = Bukkit.createInventory(null, 36, player.getUniqueId().toString());
        virtualInventory.setContents(player.getInventory().getStorageContents());
        HashMap<Integer, ItemStack> leftOver = virtualInventory.addItem(item.clone());
        return !leftOver.isEmpty();
    }

    public boolean checkPlayerInventory(@NotNull Trade trade) {
        ItemStack item = trade.getFirstItem().clone();
        int amount = trade.getFirstItem().getAmount();

        ItemStack offHandItem = player.getInventory().getItemInOffHand();

        if (offHandItem.isSimilar(item)) {
            if (offHandItem.getAmount() >= amount) {
                TradeCompleteEvent completeEvent = new TradeCompleteEvent(player, shop, trade);
                if (!completeEvent.isCancelled()) {
                    int newQuantity = offHandItem.getAmount() - amount;
                    if (newQuantity > 0) {
                        offHandItem.setAmount(newQuantity);
                        player.getInventory().setItemInOffHand(offHandItem);
                    } else {
                        player.getInventory().setItemInOffHand(null);
                    }
                    return true;
                }
            }
        }

        if (player.getInventory().containsAtLeast(item, amount)) {
            TradeCompleteEvent completeEvent = new TradeCompleteEvent(player, shop, trade);
            if (!completeEvent.isCancelled()) {
                player.getInventory().removeItem(new ItemStack(item.getType(), amount));
                return true;
            }
        }

        return false;
    }

    public boolean checkPlayerAllInventories(@NotNull Trade trade) {
        ItemStack item = trade.getFirstItem().clone();
        int amount = trade.getFirstItem().getAmount();

        for (ItemStack invItem : player.getInventory().getContents()) {
            if (invItem != null) {
                if(invItem.getItemMeta() instanceof BlockStateMeta im){
                    if(im.getBlockState() instanceof ShulkerBox shulkerBox){
                        Inventory containerInventory = shulkerBox.getInventory();
                        if (containerInventory.containsAtLeast(item, amount)) {
                            TradeCompleteEvent completeEvent = new TradeCompleteEvent(player, shop, trade);
                            if (!completeEvent.isCancelled()) {
                                containerInventory.removeItem(new ItemStack(item.getType(), amount));
                                im.setBlockState(shulkerBox);
                                invItem.setItemMeta(im);
                                return true;
                            }
                        }
                    }
                }
            }
        }

        Inventory enderChestInventory = player.getEnderChest();
        if (enderChestInventory.containsAtLeast(item, amount)) {
            TradeCompleteEvent completeEvent = new TradeCompleteEvent(player, shop, trade);
            if (!completeEvent.isCancelled()) {
                enderChestInventory.removeItem(new ItemStack(item.getType(), amount));
                return true;
            }
        }

        return checkPlayerInventory(trade);
    }

    private void tradesDoneIncrement(@NotNull Trade trade) {
        this.tradesDone.put(trade.getTradePosition(),
                this.tradesDone.getOrDefault(trade.getTradePosition(), 0) + 1);
    }

    @Override
    protected void onClose(InventoryCloseEvent event) {
        super.onClose(event);
        this.tradesDone.forEach((tradePos, count) -> this.shopTrades.stream()
                .filter(trade -> trade.getTradePosition() == tradePos).findFirst()
                .ifPresent(trade -> {
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
                })
        );
    }
}
