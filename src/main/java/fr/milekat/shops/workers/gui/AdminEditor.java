package fr.milekat.shops.workers.gui;

import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.ShopType;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.workers.ShopsManager;
import fr.milekat.shops.workers.utils.Buttons;
import fr.milekat.shops.workers.utils.TradeUtils;
import fr.mrmicky.fastinv.FastInv;
import fr.mrmicky.fastinv.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.IntStream;

public class AdminEditor extends FastInv {
    private final Player player;
    private int currentPage = 1;
    private final Shop shop;
    private final Map<Integer, List<Trade>> trades;

    public AdminEditor(Player player, @NotNull Shop shop, @NotNull List<Trade> trades) {
        super(54, Main.getConfigs()
                .getMessage("messages.gui.admin-shop.title", "&3Editing <shop_name>")
                .replaceAll("<shop_name>", shop.getName()));
        this.player = player;
        this.shop = shop;
        Map<Integer, List<Trade>> tradesPages = new HashMap<>();
        int pageTradeCount = 1;
        int page = 1;
        List<Trade> tradesLoop = new LinkedList<>();
        for (Trade trade : trades) {
            tradesLoop.add(trade);
            pageTradeCount++;
            tradesPages.put(page, tradesLoop);
            if (pageTradeCount > ShopsManager.EDITOR_TRADES_PER_PAGE) {
                tradesLoop = new LinkedList<>();
                pageTradeCount = 1;
                page++;
            }
        }
        this.trades = tradesPages;
        //  Setup base inventory
        setItems(0, 53, Buttons.PANE_BLACK.get());
        //  Setup exit button
        setItem(getInventory().getSize() - 5, Buttons.EXIT.get(), event -> event.getWhoClicked().closeInventory());
        //  Show content to player
        updatePageContent();
    }

    private void pageButtons() {
        setItem(4, new ItemBuilder(Material.PAPER)
                .amount(this.currentPage)
                .name(ChatColor.GOLD + "Click to save page (" + (this.currentPage) + ")")
                .build(),
                event -> savePage());
        if (this.currentPage > 1 ) {
            setItem(45, Buttons.PREVIOUS.get(), event -> {
                savePage();
                if (this.currentPage > 1) {
                    this.currentPage--;
                }
                updatePageContent();
            });
        } else {
            setItem(45, Buttons.PANE_BLACK.get());
        }
        if ((nonNullItem(getFirstItemPos(8)) && nonNullItem(getResultItemPos(8))) ||
                trades.containsKey(currentPage + 1)) {
            setItem(53, Buttons.NEXT.get(), event -> {
                if (this.currentPage >= 64) return;
                savePage();
                this.currentPage++;
                updatePageContent();
            });
        } else {
            setItem(53, Buttons.PANE_BLACK.get());
        }
        setItem(49, Buttons.EXIT.get(), inventoryClickEvent -> player.closeInventory());
    }

    private void updatePageContent() {
        setItems(9, 17, new ItemStack(Material.AIR));
        if (shop.getType().equals(ShopType.VANILLA)) {
            setItems(18, 26, new ItemStack(Material.AIR));
        }
        setItems(36, 45, new ItemStack(Material.AIR));
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
        setItem(9 + pagePosition, trade.getFirstItem());
        if (shop.getType().equals(ShopType.VANILLA) && trade.getSecondItem() != null){
            setItem(18 + pagePosition, trade.getSecondItem());
        }
        setItem(36 + pagePosition, trade.getResultItem());
        if (trade.isComplex()) {
            //  TODO: Add complex trade display with null values (if not set)
            setItem(27 + pagePosition, new ItemBuilder(Material.COMMAND_BLOCK)
                    .name(ChatColor.GOLD + "Complex trade")
                    .lore("firstItemTag:" + trade.getFirstItemTag().getKey().getKey())
                    .lore("secondItemTag:" + trade.getSecondItemTag().getKey().getKey())
                    .lore("maxTradeUse:" + trade.getMaxTradeUse())
                    .lore("maxTradeTagsNames:" + String.join(",", trade.getMaxTradeTagsNames()))
                    .build());
        }
    }

    private void savePage() {
        List<Trade> newTrades = new LinkedList<>();
        IntStream.rangeClosed(0, 8).forEach(index -> {
            if (nonNullItem(getFirstItemPos(index)) && nonNullItem(getResultItemPos(index))) {
                //  Check if trade is complex
                Trade complexTrade = getComplexTrade(index);
                //  Trade without specifications
                newTrades.add(Objects.requireNonNullElseGet(complexTrade, () -> new Trade(
                        shop.getUuid(),
                        index + (9 * currentPage),
                        getFirstItemPos(index), null,
                        shop.getType().equals(ShopType.VANILLA) ? getSecondItemPos(index) : null, null,
                        getResultItemPos(index),
                        0, null)));
            }
        });
        this.trades.put(this.currentPage, newTrades);
    }

    private boolean nonNullItem(ItemStack item) {
        return item != null && !item.getType().equals(Material.AIR);
    }

    private @NotNull ItemStack getFirstItemPos(int position) {
        return Objects.requireNonNullElse(this.getInventory().getItem(9 + position),
                new ItemStack(Material.AIR)).clone();
    }
    private @NotNull ItemStack getSecondItemPos(int position) {
        return Objects.requireNonNullElse(this.getInventory().getItem(18 + position),
                new ItemStack(Material.AIR)).clone();
    }
    private @NotNull ItemStack getResultItemPos(int position) {
        return Objects.requireNonNullElse(this.getInventory().getItem(36 + position),
                new ItemStack(Material.AIR)).clone();
    }
    private @NotNull ItemStack getComplexItemPos(int position) {
        return Objects.requireNonNullElse(this.getInventory().getItem(27 + position),
                new ItemStack(Material.AIR)).clone();
    }

    private @Nullable Trade getComplexTrade(int position) {
        //  Check if trade is complex
        ItemStack firstItem = getFirstItemPos(position);
        if (firstItem.getType().equals(Material.AIR)) return null;
        ItemStack complexItem = getComplexItemPos(position);
        if (!complexItem.getType().equals(Material.COMMAND_BLOCK)) return null;

        //  Get complexity from lore of complex item
        ItemMeta meta = complexItem.getItemMeta();
        if (meta==null || meta.getLore()==null) return null;
        List<String> lore = meta.getLore();
        if (lore.isEmpty()) return null;
        Tag<Material> firstItemTag = null;
        Tag<Material> secondItemTag = null;
        int maxTradeUse = 0;
        List<String> maxTradeTagsNames = null;
        for (String line : lore) {
            if (line.split(":").length!=2) continue;
            String value = line.split(":")[1];
            switch (line.split(":")[0]) {
                case "firstItemTag":
                    firstItemTag = TradeUtils.getMaterialTag(value);
                    break;
                case "secondItemTag":
                    secondItemTag = TradeUtils.getMaterialTag(value);
                    break;
                case "maxTradeUse":
                    maxTradeUse = Integer.parseInt(value);
                    break;
                case "maxTradeTagsNames":
                    maxTradeTagsNames = List.of(value.split(","));
                    break;
            }
        }

        //  Set trade with complex specifications
        return new Trade(
                shop.getUuid(),
                position + (9 * currentPage),
                firstItem, firstItemTag,
                getSecondItemPos(position), secondItemTag,
                getResultItemPos(position),
                maxTradeUse, maxTradeTagsNames);
    }

    @Override
    protected void onClick(@NotNull InventoryClickEvent event) {
        if (event.getClickedInventory()==null || event.getClickedInventory().getType().equals(InventoryType.CHEST)) {
            int slot = event.getSlot();
            if (shop.getType().equals(ShopType.VANILLA)) {
                if (slot <= 8 || (slot >= 27 && slot <= 35) || (slot >= 45 && slot <= 53)) return;
            } else {
                if (slot <= 8 || (slot >= 18 && slot <= 35) || (slot >= 45 && slot <= 53)) return;
            }
        }
        event.setCancelled(false);
        savePage();
        pageButtons();
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event) {
        Main.message(event.getPlayer(), "&6Saving trades...");
        savePage();
        List<Trade> tradeList = new LinkedList<>();
        this.trades.values().forEach(trades -> trades
                .stream()
                .sorted(Comparator.comparingInt(Trade::getTradePosition))
                .forEach(tradeList::add));
        if (tradeList.isEmpty()) {
            Main.message(event.getPlayer(), "&cNo valid trades, disabling shop");
            return;
        }
        int position = 1;
        for (Trade trade : tradeList) {
            trade.setTradePosition(position);
            position++;
        }
        Main.getStorage().asyncSaveShopTrades(this.shop, tradeList, player);
    }
}
