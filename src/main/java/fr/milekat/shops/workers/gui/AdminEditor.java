package fr.milekat.shops.workers.gui;

import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.ShopType;
import fr.milekat.shops.api.classes.Trade;
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
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Admin GUI for editing a shop's trades.
 *
 * <h3>Inventory layout (54 slots, 6 rows)</h3>
 * <pre>
 * Row 0 (0 – 8)  : controls (save, page nav, exit, advanced toggle)
 * Row 1 (9 – 17) : first items
 * Row 2 (18 – 26): second items (VANILLA shop only)
 * Row 3 (27 – 35): uses / trade-limit configuration  ← new
 * Row 4 (36 – 44): result items
 * Row 5 (45 – 53): save / page nav / exit
 * </pre>
 */
@SuppressWarnings("deprecation")
public class AdminEditor extends FastInv {
    public static final int EDITOR_TRADES_PER_PAGE = 9;
    private static final int ADVANCED_TOGGLE_SLOT = 4;

    /** Marker prefix for every decoration lore line we add/detect. */
    static final String LORE_MARKER = ChatColor.DARK_GRAY.toString() + ChatColor.ITALIC + "[MS] ";

    // Lore keys
    static final String KEY_TAG      = "Tag";
    static final String KEY_MONEY    = "Money";
    static final String KEY_USES     = "Uses";
    static final String KEY_USES_TAG = "UsesTag";

    private final Player player;
    private final Shop shop;
    private final Map<Integer, List<Trade>> trades;
    private int currentPage = 1;
    private boolean advancedMode = false;
    private boolean preventSave = true;

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
            if (pageTradeCount > EDITOR_TRADES_PER_PAGE) {
                tradesLoop = new LinkedList<>();
                pageTradeCount = 1;
                page++;
            }
        }
        this.trades = tradesPages;
        //  Setup base inventory
        setItems(0, getInventory().getSize(), Buttons.PANE_BLACK.get());
        //  Setup exit button
        setItem(getInventory().getSize() - 5, Buttons.EXIT.get(),
                event -> event.getWhoClicked().closeInventory());
        updatePageContent();
    }

    @Override
    protected void onOpen(InventoryOpenEvent ignored) {
        preventSave = false;
    }

    // =========================================================================
    //  Page controls
    // =========================================================================

    private void pageButtons() {
        if (this.currentPage > 1) {
            setItem(45, Buttons.PREVIOUS.get(), event -> {
                savePage();
                if (this.currentPage > 1) this.currentPage--;
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
        advancedToggleButton();
    }

    private void advancedToggleButton() {
        if (advancedMode) {
            setItem(ADVANCED_TOGGLE_SLOT, new ItemBuilder(Material.ENCHANTED_BOOK)
                            .name(ChatColor.LIGHT_PURPLE + "Advanced Edit: ON")
                            .lore(ChatColor.GRAY + "Click an item to edit tags / money / uses")
                            .lore(ChatColor.GRAY + "Click here to disable")
                            .build(),
                    event -> {
                        savePage();
                        advancedMode = false;
                        updatePageContent();
                    });
        } else {
            setItem(ADVANCED_TOGGLE_SLOT, new ItemBuilder(Material.BOOK)
                            .name(ChatColor.GRAY + "Advanced Edit: OFF")
                            .lore(ChatColor.GRAY + "Click to enable advanced editing")
                            .lore(ChatColor.DARK_GRAY + "(tag selector + money result + uses)")
                            .build(),
                    event -> {
                        savePage();
                        advancedMode = true;
                        updatePageContent();
                    });
        }
    }

    // =========================================================================
    //  Display / update
    // =========================================================================

    private void updatePageContent() {
        // Row 1: first items
        setItems(9, 18, new ItemStack(Material.AIR));
        // Row 2: second items (If not VANILLA shop)
        if (!shop.getType().equals(ShopType.VANILLA)) {
            setItems(18, 27, new ItemStack(Material.AIR));
        }
        // Row 3: uses / trade-limit
        setItems(27, 36, new ItemStack(Material.AIR));
        // Row 4: result items
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
        // Row 1: first items
        setItem(9 + pagePosition, decorateTagItem(
                trade.getFirstItem().clone(), trade.getFirstItemTag()));
        // Row 2: second items (If not VANILLA shop)
        if (shop.getType().equals(ShopType.VANILLA) && trade.getSecondItem() != null) {
            setItem(18 + pagePosition, decorateTagItem(
                    trade.getSecondItem().clone(), trade.getSecondItemTag()));
        }
        // Row 3: uses / trade-limit
        setItem(27 + pagePosition, decorateUsesItem(
                trade.getMaxTradeUse(), trade.getMaxTradeTagsNames()));
        // Row 4: result items
        setItem(36 + pagePosition, decorateResultItem(
                trade.getResultItem().clone(), trade.getMoneyResult()));
    }

    // =========================================================================
    //  Save
    // =========================================================================

    private void savePage() {
        List<Trade> newTrades = new LinkedList<>();
        IntStream.rangeClosed(0, 8).forEach(index -> {
            ItemStack rawFirst  = getFirstItemPos(index);
            ItemStack rawResult = getResultItemPos(index);
            if (nonNullItem(rawFirst) && nonNullItem(rawResult)) {
                ItemStack rawSecond = shop.getType().equals(ShopType.VANILLA) ? getSecondItemPos(index) : null;
                ItemStack rawUses   = getUsesItemPos(index);

                Tag<Material> firstTag  = extractTag(rawFirst);
                Tag<Material> secondTag = nonNullItem(rawSecond) ? extractTag(rawSecond) : null;
                Map<String, Integer> money = extractMoney(rawResult);
                int maxTradeUse = extractMaxTradeUse(rawUses);
                List<String> usesTags = extractUsesTags(rawUses);

                newTrades.add(new Trade(
                        shop.getUuid(),
                        index + (9 * currentPage),
                        cleanup(rawFirst),  firstTag,
                        nonNullItem(rawSecond) ? cleanup(rawSecond) : null, secondTag,
                        cleanup(rawResult),
                        maxTradeUse, usesTags,
                        money));
            }
        });
        this.trades.put(this.currentPage, newTrades);
    }

    // =========================================================================
    //  Click / sub-editor routing
    // =========================================================================

    @Override
    protected void onClick(@NotNull InventoryClickEvent event) {
        if (event.getClickedInventory() == null
                || event.getClickedInventory().getType().equals(InventoryType.CHEST)) {
            int slot = event.getSlot();

            // Row 3 (uses) is always managed — block free editing regardless of mode
            boolean inUsesRow = slot >= 27 && slot <= 35;

            // Trade area: rows where free-form item editing is allowed in normal mode
            boolean inTradeArea;
            if (shop.getType().equals(ShopType.VANILLA)) {
                // rows 1, 2, 4 (first, second, result)
                inTradeArea = (slot >= 9 && slot <= 26) || (slot >= 36 && slot <= 44);
            } else {
                // rows 1, 4 (first, result)
                inTradeArea = (slot >= 9 && slot <= 17) || (slot >= 36 && slot <= 44);
            }

            if (!inTradeArea && !inUsesRow) return;

            if (advancedMode) {
                event.setCancelled(true);
                if (event.getCurrentItem() == null || event.getCurrentItem().getType().equals(Material.AIR)) {
                    advancedMode = false;
                    advancedToggleButton();
                }
                openAdvancedSubEditor(slot);
                return;
            }

            // Non-advanced mode: protect uses row from free editing
            if (inUsesRow) {
                event.setCancelled(true);
                return;
            }
        }
        event.setCancelled(false);
        savePage();
    }

    private void openAdvancedSubEditor(int slot) {
        if (slot >= 9 && slot <= 17) {
            int pos = slot - 9;
            ItemStack item = getFirstItemPos(pos);
            if (!nonNullItem(item)) return;
            savePage();
            new AdvancedEditor(this, player, pos, true, cleanup(item), extractTag(item)).open(player);

        } else if (slot >= 18 && slot <= 26 && shop.getType().equals(ShopType.VANILLA)) {
            int pos = slot - 18;
            ItemStack item = getSecondItemPos(pos);
            if (!nonNullItem(item)) return;
            savePage();
            new AdvancedEditor(this, player, pos, false, cleanup(item), extractTag(item)).open(player);

        } else if (slot >= 27 && slot <= 35) {
            int pos = slot - 27;
            // Only open if a valid trade exists at this position
            if (!nonNullItem(getFirstItemPos(pos)) || !nonNullItem(getResultItemPos(pos))) return;
            ItemStack rawUses = getUsesItemPos(pos);
            int currentMax = extractMaxTradeUse(rawUses);
            List<String> currentTags = extractUsesTags(rawUses);
            savePage();
            new AdvancedEditor(this, player, pos,
                    currentMax, currentTags != null ? currentTags : List.of()).open(player);

        } else if (slot >= 36 && slot <= 44) {
            int pos = slot - 36;
            ItemStack item = getResultItemPos(pos);
            if (!nonNullItem(item)) return;
            savePage();
            new AdvancedEditor(this, player, pos, cleanup(item), extractMoney(item)).open(player);
        }
    }

    // =========================================================================
    //  Callbacks from AdvancedEditor
    // =========================================================================

    /** MATERIAL_TAG mode: update the first/second item slot with the new tag decoration. */
    void applyAdvancedTagEdit(int position, boolean firstItem, @Nullable Tag<Material> newTag) {
        int slot = firstItem ? 9 + position : 18 + position;
        ItemStack current = firstItem ? getFirstItemPos(position) : getSecondItemPos(position);
        if (!nonNullItem(current)) return;
        getInventory().setItem(slot, decorateTagItem(cleanup(current), newTag));
    }

    /** MONEY_TAG mode: update the result slot with the new money-result decoration. */
    void applyAdvancedResultEdit(int position, @NotNull Map<String, Integer> moneyResult) {
        ItemStack current = getResultItemPos(position);
        if (!nonNullItem(current)) return;
        getInventory().setItem(36 + position, decorateResultItem(cleanup(current), moneyResult));
    }

    /** USES_TAG mode: update the uses slot with the new uses' decoration. */
    void applyAdvancedUsesEdit(int position, int maxTradeUse, @NotNull List<String> tagNames) {
        getInventory().setItem(27 + position,
                decorateUsesItem(maxTradeUse, tagNames.isEmpty() ? null : tagNames));
    }

    /** Called by sub-editors before re-opening this editor to skip the storage save in onClose. */
    void advancedEditorGuiOpen() {
        this.preventSave = true;
    }

    // =========================================================================
    //  Item slot readers
    // =========================================================================

    private boolean nonNullItem(ItemStack item) {
        return item != null && !item.getType().equals(Material.AIR);
    }

    private @NotNull ItemStack getFirstItemPos(int position) {
        return Objects.requireNonNullElse(getInventory().getItem(9  + position),
                new ItemStack(Material.AIR)).clone();
    }

    private @NotNull ItemStack getSecondItemPos(int position) {
        return Objects.requireNonNullElse(getInventory().getItem(18 + position),
                new ItemStack(Material.AIR)).clone();
    }

    private @NotNull ItemStack getUsesItemPos(int position) {
        return Objects.requireNonNullElse(getInventory().getItem(27 + position),
                new ItemStack(Material.AIR)).clone();
    }

    private @NotNull ItemStack getResultItemPos(int position) {
        return Objects.requireNonNullElse(getInventory().getItem(36 + position),
                new ItemStack(Material.AIR)).clone();
    }

    // =========================================================================
    //  Item decoration (glint + lore) / cleanup
    // =========================================================================

    private static @NotNull ItemStack decorateTagItem(@NotNull ItemStack base, @Nullable Tag<Material> tag) {
        if (tag == null) return base;
        return applyDecoration(base, List.of(buildLoreLine(KEY_TAG, tag.getKey().toString())));
    }

    private static @NotNull ItemStack decorateResultItem(@NotNull ItemStack base,
                                                         @NotNull Map<String, Integer> money) {
        if (money.isEmpty()) return base;
        List<String> additions = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : money.entrySet()) {
            additions.add(buildLoreLine(KEY_MONEY, entry.getKey() + " x" + entry.getValue()));
        }
        return applyDecoration(base, additions);
    }

    private static @NotNull ItemStack decorateUsesItem(int maxTradeUse,
                                                       @Nullable List<String> tagNames) {
        ItemStack base = new ItemBuilder(Material.PAPER)
                .name(ChatColor.GRAY + "Usage limit")
                .build();
        if (maxTradeUse <= 0) return base;
        List<String> additions = new ArrayList<>();
        additions.add(buildLoreLine(KEY_USES, String.valueOf(maxTradeUse)));
        if (tagNames != null) {
            for (String tag : tagNames) {
                additions.add(buildLoreLine(KEY_USES_TAG, tag));
            }
        }
        return applyDecoration(base, additions);
    }

    static @NotNull ItemStack applyDecoration(@NotNull ItemStack base, @NotNull List<String> additionLines) {
        ItemStack out = base.clone();
        ItemMeta meta = out.getItemMeta();
        if (meta == null) return out;
        meta.setEnchantmentGlintOverride(true);
        List<String> lore = meta.getLore() != null
                ? new ArrayList<>(meta.getLore())
                : new ArrayList<>();
        lore.removeIf(line -> line != null && line.startsWith(LORE_MARKER));
        lore.addAll(additionLines);
        meta.setLore(lore);
        out.setItemMeta(meta);
        return out;
    }

    static @NotNull ItemStack cleanup(@NotNull ItemStack item) {
        if (item.getType().equals(Material.AIR)) return item;
        ItemStack clean = item.clone();
        ItemMeta meta = clean.getItemMeta();
        if (meta == null) return clean;
        if (meta.hasEnchantmentGlintOverride()) meta.setEnchantmentGlintOverride(null);
        if (meta.getLore() != null) {
            List<String> filtered = meta.getLore().stream()
                    .filter(line -> line == null || !line.startsWith(LORE_MARKER))
                    .collect(Collectors.toList());
            meta.setLore(filtered.isEmpty() ? null : filtered);
        }
        clean.setItemMeta(meta);
        return clean;
    }

    static @NotNull String buildLoreLine(@NotNull String key, @NotNull String value) {
        return LORE_MARKER + ChatColor.AQUA + key + ChatColor.DARK_GRAY + ": " + ChatColor.GRAY + value;
    }

    // =========================================================================
    //  Lore extraction helpers
    // =========================================================================

    /** Returns the single value stored under {@code key}, or {@code null} if absent. */
    static @Nullable String extractLoreValue(@NotNull ItemStack item, @NotNull String key) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getLore() == null) return null;
        String prefix = LORE_MARKER + ChatColor.AQUA + key + ChatColor.DARK_GRAY + ": " + ChatColor.GRAY;
        for (String line : meta.getLore()) {
            if (line != null && line.startsWith(prefix)) return line.substring(prefix.length());
        }
        return null;
    }

    /** Returns all values stored under {@code key} (supports multiple lines). */
    static @NotNull List<String> extractLoreValues(@NotNull ItemStack item, @NotNull String key) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getLore() == null) return List.of();
        String prefix = LORE_MARKER + ChatColor.AQUA + key + ChatColor.DARK_GRAY + ": " + ChatColor.GRAY;
        List<String> out = new ArrayList<>();
        for (String line : meta.getLore()) {
            if (line != null && line.startsWith(prefix)) out.add(line.substring(prefix.length()));
        }
        return out;
    }

    static @Nullable Tag<Material> extractTag(@NotNull ItemStack item) {
        String raw = extractLoreValue(item, KEY_TAG);  // e.g. "minecraft:logs"
        if (raw == null || raw.isEmpty()) return null;
        return TradeUtils.getMaterialTag(raw);          // handles full namespaced keys
    }

    static @NotNull Map<String, Integer> extractMoney(@NotNull ItemStack item) {
        Map<String, Integer> result = new HashMap<>();
        for (String line : extractLoreValues(item, KEY_MONEY)) {
            int sep = line.lastIndexOf(" x");
            if (sep < 0) continue;
            try {
                result.put(line.substring(0, sep), Integer.parseInt(line.substring(sep + 2)));
            } catch (NumberFormatException ignore) { /* skip malformed */ }
        }
        return result;
    }

    static int extractMaxTradeUse(@NotNull ItemStack item) {
        String raw = extractLoreValue(item, KEY_USES);
        if (raw == null) return 0;
        try { return Integer.parseInt(raw); } catch (NumberFormatException e) { return 0; }
    }

    static @Nullable List<String> extractUsesTags(@NotNull ItemStack item) {
        List<String> tags = extractLoreValues(item, KEY_USES_TAG);
        return tags.isEmpty() ? null : tags;
    }

    // =========================================================================
    //  onClose
    // =========================================================================

    @Override
    public void onClose(@NotNull InventoryCloseEvent event) {
        if (preventSave) {
            return;
        }
        Main.message(event.getPlayer(), "&6Saving trades...");
        savePage();
        List<Trade> tradeList = new LinkedList<>();
        this.trades.values().forEach(t -> t.stream()
                .sorted(Comparator.comparingInt(Trade::getTradePosition))
                .forEach(tradeList::add));
        if (tradeList.isEmpty()) {
            Main.message(event.getPlayer(), "&cNo valid trades, disabling shop");
            return;
        }
        int position = 1;
        for (Trade trade : tradeList) {
            trade.setTradePosition(position++);
        }
        Main.getStorage().asyncSaveShopTrades(this.shop, tradeList, player);
    }
}
