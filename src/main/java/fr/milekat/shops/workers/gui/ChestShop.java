package fr.milekat.shops.workers.gui;

import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.workers.ShopsManager;
import fr.milekat.shops.workers.utils.HeadsUtils;
import fr.milekat.shops.workers.utils.TradeMode;
import fr.mrmicky.fastinv.FastInv;
import fr.mrmicky.fastinv.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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

public class ChestShop extends FastInv {
    private final Player player;
    private int currentPage = 1;
    private final Shop shop;
    private final Map<Integer, List<Trade>> trades;
    private TradeMode tradeMode;

    public ChestShop(Player player, @NotNull Shop shop, @NotNull List<Trade> trades) {
        //  9 top line + 9 bottom line + 1 line per trades (Max 4)
        super(54, ChatColor.DARK_AQUA + "Shop " + shop.getName());
        this.player = player;
        this.shop = shop;
        try {
            this.tradeMode = Main.getStorage().getCacheTradeMode(player.getUniqueId().toString());
        } catch (Exception ignore) {
            this.tradeMode = TradeMode.INVENTORY;
        }
        Map<Integer, List<Trade>> tradesPages = new HashMap<>();
        int pageTradeCount = 1;
        int page = 1;
        List<Trade> tradesLoop = new LinkedList<>();
        for (Trade trade : trades) {
            tradesLoop.add(trade);
            pageTradeCount++;
            tradesPages.put(page, tradesLoop);
            if (pageTradeCount > ShopsManager.CHEST_TRADES_PER_PAGE) {
                tradesLoop = new LinkedList<>();
                pageTradeCount = 1;
                page++;
            }
        }
        this.trades = tradesPages;
        //  Setup base inventory
        basicCanvas();
        //  Show content to player
        updatePageContent();
    }

    private void basicCanvas() {
        setItems(0, 53, ShopsManager.PANE_WHITE);
        setItems(getBorders(), ShopsManager.PANE_BLACK);
        //  Setup exit button
        setItem(getInventory().getSize() - 5, new ItemBuilder(Material.BARRIER)
                .name(ChatColor.RED + "Close").build());
        pageButtons();
    }

    private void pageButtons() {
        tradeModeButton();

        if (this.currentPage > 1 ) {
            setItem(45, ShopsManager.PAGE_LEFT, event -> {
                if (this.currentPage > 1) {
                    this.currentPage--;
                }
                updatePageContent();
            });
        } else {
            setItem(45, ShopsManager.PANE_BLACK);
        }
        if (trades.containsKey(currentPage + 1)) {
            setItem(53, ShopsManager.PAGE_RIGHT, event -> {
                if (this.currentPage >= 64) return;
                this.currentPage++;
                updatePageContent();
            });
        } else {
            setItem(53, ShopsManager.PANE_BLACK);
        }
    }

    private void tradeModeButton() {
        Main.getStorage().asyncSaveTradeMode(player.getUniqueId().toString(), tradeMode);
        if (tradeMode.equals(TradeMode.INVENTORY)) {
            setItem(4, new ItemBuilder(Material.CHEST).build(), event -> {
                tradeMode = TradeMode.SHULKER;
                tradeModeButton();
            });
        } else if (tradeMode.equals(TradeMode.SHULKER)) {
            setItem(4, new ItemBuilder(Material.SHULKER_BOX).build(), event -> {
                tradeMode = TradeMode.INVENTORY;
                tradeModeButton();
            });
        }
    }

    private void updatePageContent() {
        basicCanvas();
        if (trades.containsKey(this.currentPage)) {
            int position = 0;
            for (Trade trade : trades.get(this.currentPage)) {
                displayTrade(position, trade);
                position++;
            }
        }
        pageButtons();
    }

    private void displayTrade(int pagePosition, @NotNull Trade trade) {
        setItem(11 + (pagePosition * 9), trade.getFirstItem().clone()); //  TODO: Events
        setItem(13 + (pagePosition * 9), new ItemBuilder(HeadsUtils.ARROW_LEFT.getItem().clone())
                .name(" ").build());
        setItem(15 + (pagePosition * 9), trade.getResultItem().clone(), event -> {
            if (isFull(player, trade.getResultItem())) {
                player.sendMessage("&cNot enough space in inventory to receive result");
                return;
            }
            int trades = 0;
            if (tradeMode.equals(TradeMode.INVENTORY)) {
                if (event.getClick().equals(ClickType.SHIFT_LEFT)) {
                    boolean tradeDone;
                    do {
                        if (isFull(player, trade.getResultItem())) {
                            player.sendMessage("&cNot enough space in inventory to receive result");
                            tradeDone = false;
                        } else {
                            tradeDone = checkPlayerInventory(player, trade.getFirstItem(),
                                    trade.getFirstItem().getAmount());
                        }
                        if (tradeDone) {
                            player.getInventory().addItem(trade.getResultItem());
                            trades++;
                        }
                    } while (tradeDone);
                } else {
                    if (checkPlayerInventory(player, trade.getFirstItem(), trade.getFirstItem().getAmount())) {
                        player.getInventory().addItem(trade.getResultItem());
                        trades++;
                    }
                }
            } else if (tradeMode.equals(TradeMode.SHULKER)) {
                if (event.getClick().equals(ClickType.SHIFT_LEFT)) {
                    boolean tradeDone;
                    do {
                        if (isFull(player, trade.getResultItem())) {
                            player.sendMessage("&cNot enough space in inventory to receive result");
                            tradeDone = false;
                        } else {
                            tradeDone = checkAllPlayerInventories(player, trade.getFirstItem(),
                                    trade.getFirstItem().getAmount());
                            if (tradeDone) {
                                player.getInventory().addItem(trade.getResultItem());
                                trades++;
                            }
                        }
                    } while (tradeDone);
                } else {
                    if (checkAllPlayerInventories(player, trade.getFirstItem(),
                            trade.getFirstItem().getAmount())) {
                        player.getInventory().addItem(trade.getResultItem());
                        trades++;
                    }
                }
            }
            if (trades>0) {
                player.sendMessage("&2You did " + trades + " trade(s), for a total of " +
                        trade.getResultItem().getAmount() * trades + " " + trade.getResultItem().getType());
            } else {
                player.sendMessage("&cNot enough items");
            }
        });
    }

    public boolean isFull(@NotNull Player player, @NotNull ItemStack item) {
        Inventory virtualInventory = Bukkit.createInventory(null, 36, "tmp");
        virtualInventory.setContents(player.getInventory().getStorageContents());
        HashMap<Integer, ItemStack> leftOver = virtualInventory.addItem(item.clone());
        return !leftOver.isEmpty();
    }

    public boolean checkPlayerInventory(@NotNull Player player, @NotNull ItemStack item, int quantity) {
        ItemStack offHandItem = player.getInventory().getItemInOffHand();

        if (offHandItem.isSimilar(item)) {
            if (offHandItem.getAmount() >= quantity) {
                int newQuantity = offHandItem.getAmount() - quantity;
                if (newQuantity > 0) {
                    offHandItem.setAmount(newQuantity);
                    player.getInventory().setItemInOffHand(offHandItem);
                } else {
                    player.getInventory().setItemInOffHand(null);
                }
                return true;
            }
        }

        if (player.getInventory().containsAtLeast(item, quantity)) {
            player.getInventory().removeItem(new ItemStack(item.getType(), quantity));
            return true;
        } else {
            return false;
        }
    }

    public boolean checkAllPlayerInventories(@NotNull Player player, @NotNull ItemStack item, int quantity) {
        for (ItemStack invItem : player.getInventory().getContents()) {
            if (invItem != null) {
                if(invItem.getItemMeta() instanceof BlockStateMeta im){
                    if(im.getBlockState() instanceof ShulkerBox shulker){
                        Inventory containerInventory = shulker.getInventory();
                        if (containerInventory.containsAtLeast(item, quantity)) {
                            containerInventory.removeItem(new ItemStack(item.getType(), quantity));
                            im.setBlockState(shulker);
                            invItem.setItemMeta(im);
                            return true;
                        }
                    }
                }
            }
        }
        Inventory enderChestInventory = player.getEnderChest();
        if (enderChestInventory.containsAtLeast(item, quantity)) {
            enderChestInventory.removeItem(new ItemStack(item.getType(), quantity));
            return true;
        }
        return checkPlayerInventory(player, item, quantity);
    }



    @Override
    protected void onClose(InventoryCloseEvent event) {
        super.onClose(event);
    }
}
