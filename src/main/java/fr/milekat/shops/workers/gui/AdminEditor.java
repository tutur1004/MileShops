package fr.milekat.shops.workers.gui;

import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.ShopType;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.workers.gui.ShopAdminSession.DraftTrade;
import fr.milekat.shops.workers.utils.Buttons;
import fr.milekat.shops.workers.utils.TradeUtils;
import fr.mrmicky.fastinv.FastInv;
import fr.mrmicky.fastinv.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin GUI view for editing a shop's trades.
 *
 * <h3>Architecture</h3>
 * All mutable state lives in {@link ShopAdminSession}, which outlives any individual FastInv
 * window. Because FastInv loses its event handlers when an inventory closes, this class is
 * re-created every time the player returns from a sub-editor — the
 * {@link ShopAdminSession} reference is simply passed to the new instance.
 *
 * <p>Escape-to-close is blocked by {@code setCloseFilter} while there are unsaved changes.
 * Only the <b>Save&nbsp;&amp;&nbsp;Exit</b> and <b>Exit without Save</b> buttons can dismiss
 * the editor.</p>
 *
 * <h3>Inventory layout (54 slots, 6 rows)</h3>
 * <pre>
 * Row 0 (0 – 8)  : page indicator (slot 0), advanced toggle (slot 4)
 * Row 1 (9 – 17) : first items
 * Row 2 (18 – 26): second items (VANILLA shop only)
 * Row 3 (27 – 35): uses / trade-limit items
 * Row 4 (36 – 44): result items
 * Row 5 (45 – 53): prev page (45), exit without save (48), save &amp; exit (50), next page (53)
 * </pre>
 */
@SuppressWarnings("deprecation")
public class AdminEditor extends FastInv {

    public static final int EDITOR_TRADES_PER_PAGE = 9;

    // ---- Slot constants ----
    private static final int SLOT_PAGE_INDICATOR  = 0;
    private static final int SLOT_ADVANCED_TOGGLE = 4;
    private static final int SLOT_PREV_PAGE       = 45;
    private static final int SLOT_EXIT_NO_SAVE    = 48;
    private static final int SLOT_SAVE_AND_EXIT   = 50;
    private static final int SLOT_NEXT_PAGE       = 53;

    // ---- Lore decoration ----
    static final String LORE_MARKER  = ChatColor.DARK_GRAY.toString() + ChatColor.ITALIC + "[MS] ";
    static final String KEY_TAG      = "Tag";
    static final String KEY_MONEY    = "Money";
    static final String KEY_USES     = "Uses";

    // =========================================================================
    //  Fields
    // =========================================================================

    /** Persistent session state shared with sub-editors and preserved across re-instantiations. */
    private final ShopAdminSession session;

    /**
     * Set to {@code true} just before opening a sub-editor so the close filter allows the
     * implicit close that Minecraft fires when the new inventory takes over.
     */
    private boolean subEditorOpen    = false;
    /**
     * Set to {@code true} when a button (save / exit) triggered the close, so the filter
     * allows it through.
     */
    private boolean closingViaButton = false;

    // =========================================================================
    //  Constructor
    // =========================================================================

    /**
     * Opens (or re-opens after a sub-editor) an editor view over {@code session}.
     * Called by {@link ShopAdminSession#open()} and by the state's close listener.
     */
    AdminEditor(@NotNull ShopAdminSession session) {
        super(54, Main.getConfigs()
                .getMessage("messages.gui.admin-shop.title", "&3Editing <shop_name>")
                .replaceAll("<shop_name>", session.shop.getName()));
        this.session = session;

        setItems(0, getInventory().getSize(), Buttons.PANE_BLACK.get());
        updatePageContent();

        // Block Escape-close while there are unsaved changes.
        setCloseFilter(p -> {
            if (subEditorOpen)       return false;              // allow: sub-editor is taking over
            if (closingViaButton)  { closingViaButton = false; return false; } // allow: button
            if (!session.hasChanges) return false;              // allow: nothing to lose
            Main.message(p, "&eUse the &aSave & Exit &eor &cExit without Save &ebutton to close.");
            return true; // block
        });
    }

    // =========================================================================
    //  Display / rendering  (session → inventory)
    // =========================================================================

    private void updatePageContent() {
        setItems(9,  18, new ItemStack(Material.AIR));
        if (session.shop.getType().equals(ShopType.VANILLA))
            setItems(18, 27, new ItemStack(Material.AIR));
        setItems(27, 36, new ItemStack(Material.AIR));
        setItems(36, 45, new ItemStack(Material.AIR));

        int startAbs = (session.currentPage - 1) * EDITOR_TRADES_PER_PAGE + 1;
        for (int i = 0; i < EDITOR_TRADES_PER_PAGE; i++) {
            DraftTrade d = session.drafts.get(startAbs + i);
            if (d != null) displayDraft(i, d);
        }

        updateControls();
    }

    private void displayDraft(int pagePosition, @NotNull DraftTrade d) {
        if (d.firstItem != null)
            setItem(9  + pagePosition, decorateTagItem(d.firstItem.clone(), d.firstItemTag));

        if (session.shop.getType().equals(ShopType.VANILLA) && d.secondItem != null)
            setItem(18 + pagePosition, decorateTagItem(d.secondItem.clone(), d.secondItemTag));

        // Show uses paper as soon as the trade has at least one item placed
        if (d.firstItem != null || d.resultItem != null)
            setItem(27 + pagePosition, decorateUsesItem(d.maxTradeUses));

        if (d.resultItem != null)
            setItem(36 + pagePosition, decorateResultItem(d.resultItem.clone(), d.moneyResult));
    }

    private void updateControls() {
        // Page indicator
        setItem(SLOT_PAGE_INDICATOR, new ItemBuilder(Material.PAPER)
                .amount(Math.min(session.currentPage, 64))
                .name(ChatColor.GOLD + "Page " + session.currentPage)
                .lore(ChatColor.GRAY + "Drafts loaded: " + ChatColor.WHITE + session.drafts.size())
                .build());

        // Advanced toggle
        if (session.advancedMode) {
            setItem(SLOT_ADVANCED_TOGGLE, new ItemBuilder(Material.ENCHANTED_BOOK)
                            .name(ChatColor.LIGHT_PURPLE + "Advanced Edit: ON")
                            .lore(ChatColor.GRAY + "Click an item to edit tags / money / uses")
                            .lore(ChatColor.GRAY + "Click here to disable")
                            .build(),
                    event -> { session.advancedMode = false; updatePageContent(); });
        } else {
            setItem(SLOT_ADVANCED_TOGGLE, new ItemBuilder(Material.BOOK)
                            .name(ChatColor.GRAY + "Advanced Edit: OFF")
                            .lore(ChatColor.GRAY + "Click to enable advanced editing")
                            .lore(ChatColor.DARK_GRAY + "(tag selector + money result + uses)")
                            .build(),
                    event -> { session.advancedMode = true; updatePageContent(); });
        }

        // Previous page
        if (session.currentPage > 1) {
            setItem(SLOT_PREV_PAGE, Buttons.PREVIOUS.get(), event -> {
                session.currentPage--;
                updatePageContent();
            });
        } else {
            setItem(SLOT_PREV_PAGE, Buttons.PANE_BLACK.get());
        }

        // Next page: visible when the 9th slot of the current page is complete, or drafts exist beyond
        if (canShowNextPage()) {
            setItem(SLOT_NEXT_PAGE, Buttons.NEXT.get(), event -> {
                if (session.currentPage >= 64) return;
                session.currentPage++;
                updatePageContent();
            });
        } else {
            setItem(SLOT_NEXT_PAGE, Buttons.PANE_BLACK.get());
        }

        // Exit without save (always available)
        setItem(SLOT_EXIT_NO_SAVE, new ItemBuilder(Material.RED_DYE)
                        .name(ChatColor.RED + "Exit without saving")
                        .lore(ChatColor.GRAY + "Discard all unsaved changes")
                        .build(),
                event -> exitWithoutSave());

        // Save & Exit: orange dye when incomplete drafts prevent saving, green dye otherwise
        if (hasPartialDrafts()) {
            setItem(SLOT_SAVE_AND_EXIT, new ItemBuilder(Material.ORANGE_DYE)
                    .name(ChatColor.GOLD + "Cannot save yet")
                    .lore(ChatColor.GRAY + "At least one trade is incomplete")
                    .lore(ChatColor.DARK_GRAY + "(missing first OR result item)")
                    .build());
        } else {
            setItem(SLOT_SAVE_AND_EXIT, new ItemBuilder(Material.LIME_DYE)
                            .name(ChatColor.GREEN + "Save & Exit")
                            .lore(ChatColor.GRAY + "Commit all changes")
                            .lore(ChatColor.DARK_GRAY + "Empty slots will be shrunk")
                            .build(),
                    event -> saveAndExit());
        }
    }

    private boolean canShowNextPage() {
        int lastAbs = session.currentPage * EDITOR_TRADES_PER_PAGE;
        DraftTrade last = session.drafts.get(lastAbs);
        if (last != null && last.isComplete()) return true;
        for (Integer abs : session.drafts.keySet()) if (abs > lastAbs) return true;
        return false;
    }

    private boolean hasPartialDrafts() {
        for (DraftTrade d : session.drafts.values()) if (d.isPartial()) return true;
        return false;
    }

    // =========================================================================
    //  Click handling
    // =========================================================================

    @Override
    protected void onClick(@NotNull InventoryClickEvent event) {
        if (event.getClickedInventory() == null
                || event.getClickedInventory().getType() != InventoryType.CHEST) {
            event.setCancelled(false);
            return;
        }

        int slot = event.getSlot();

        boolean inUsesRow   = slot >= 27 && slot <= 35;
        boolean inTradeArea = session.shop.getType().equals(ShopType.VANILLA)
                ? (slot >= 9 && slot <= 26) || (slot >= 36 && slot <= 44)
                : (slot >= 9 && slot <= 17) || (slot >= 36 && slot <= 44);

        if (!inTradeArea && !inUsesRow) return;

        if (session.advancedMode) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            openAdvancedSubEditor(slot);
            return;
        }

        if (inUsesRow) { event.setCancelled(true); return; }

        // Non-advanced: allow drag-drop; sync cache after event resolves
        event.setCancelled(false);
        final int pageAtClick = session.currentPage;
        final int slotAtClick = slot;
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            if (pageAtClick != session.currentPage) return;
            syncSlotFromInventory(slotAtClick);
            session.hasChanges = true;
            // Full re-render so the uses paper appears as soon as the first item is placed
            updatePageContent();
        });
    }

    private void syncSlotFromInventory(int slot) {
        int posInPage = -1, row = -1;
        if      (slot >= 9  && slot <= 17) { posInPage = slot - 9;  row = 1; }
        else if (slot >= 18 && slot <= 26) { posInPage = slot - 18; row = 2; }
        else if (slot >= 36 && slot <= 44) { posInPage = slot - 36; row = 4; }
        if (posInPage < 0) return;

        int absPos  = (session.currentPage - 1) * EDITOR_TRADES_PER_PAGE + posInPage + 1;
        ItemStack raw = getInventory().getItem(slot);
        boolean isAir = raw == null || raw.getType() == Material.AIR;

        DraftTrade d = session.drafts.computeIfAbsent(absPos, k -> new DraftTrade());

        switch (row) {
            case 1 -> { d.firstItem    = isAir ? null : cleanup(raw.clone());
                        d.firstItemTag = isAir ? null : extractTag(raw); }
            case 2 -> { d.secondItem    = isAir ? null : cleanup(raw.clone());
                        d.secondItemTag = isAir ? null : extractTag(raw); }
            case 4 -> { d.resultItem  = isAir ? null : cleanup(raw.clone());
                        d.moneyResult = isAir ? new HashMap<>() : extractMoney(raw); }
        }

        if (d.isEmpty()) session.drafts.remove(absPos);
    }

    private void openAdvancedSubEditor(int slot) {
        int posInPage, absPos;
        DraftTrade d;

        if (slot >= 9 && slot <= 17) {
            posInPage = slot - 9;
            absPos    = (session.currentPage - 1) * EDITOR_TRADES_PER_PAGE + posInPage + 1;
            d = session.drafts.get(absPos);
            if (d == null || d.firstItem == null) return;
            subEditorOpen = true;
            new AdvancedEditor(session, posInPage, true,
                    d.firstItem.clone(), d.firstItemTag).open(session.player);

        } else if (slot >= 18 && slot <= 26 && session.shop.getType().equals(ShopType.VANILLA)) {
            posInPage = slot - 18;
            absPos    = (session.currentPage - 1) * EDITOR_TRADES_PER_PAGE + posInPage + 1;
            d = session.drafts.get(absPos);
            if (d == null || d.secondItem == null) return;
            subEditorOpen = true;
            new AdvancedEditor(session, posInPage, false,
                    d.secondItem.clone(), d.secondItemTag).open(session.player);

        } else if (slot >= 27 && slot <= 35) {
            posInPage = slot - 27;
            absPos    = (session.currentPage - 1) * EDITOR_TRADES_PER_PAGE + posInPage + 1;
            d = session.drafts.get(absPos);
            if (d == null || !d.isComplete()) return;
            subEditorOpen = true;
            new AdvancedEditor(session, posInPage,
                    new HashMap<>(d.maxTradeUses)).open(session.player);

        } else if (slot >= 36 && slot <= 44) {
            posInPage = slot - 36;
            absPos    = (session.currentPage - 1) * EDITOR_TRADES_PER_PAGE + posInPage + 1;
            d = session.drafts.get(absPos);
            if (d == null || d.resultItem == null) return;
            subEditorOpen = true;
            new AdvancedEditor(session, posInPage,
                    d.resultItem.clone(), new HashMap<>(d.moneyResult)).open(session.player);
        }
    }

    // =========================================================================
    //  Save / Exit
    // =========================================================================

    private void saveAndExit() {
        if (hasPartialDrafts()) return;

        List<Trade> tradeList = new LinkedList<>();
        int newPos = 1;
        for (DraftTrade d : session.drafts.values()) {
            // isComplete() guarantees firstItem and resultItem are non-null
            if (d.firstItem == null || d.resultItem == null) continue;
            tradeList.add(new Trade(
                    session.shop.getUuid(), newPos++,
                    d.firstItem.clone(),  d.firstItemTag,
                    d.secondItem != null ? d.secondItem.clone() : null, d.secondItemTag,
                    d.resultItem.clone(),
                    new HashMap<>(d.maxTradeUses),
                    new HashMap<>(d.moneyResult)));
        }

        closingViaButton = true;
        session.dispose();
        if (tradeList.isEmpty()) {
            Main.message(session.player, "&cNo valid trades — shop disabled.");
        } else {
            Main.message(session.player, "&6Saving " + tradeList.size() + " trade(s)...");
            Main.getStorage().asyncSaveShopTrades(session.shop, tradeList, session.player);
        }
        session.player.closeInventory();
    }

    private void exitWithoutSave() {
        closingViaButton = true;
        session.dispose();
        if (session.hasChanges) Main.message(session.player, "&7Changes discarded.");
        session.player.closeInventory();
    }

    // =========================================================================
    //  Item decoration helpers  (used by AdvancedEditor + sync layer)
    // =========================================================================

    static @NotNull ItemStack decorateTagItem(@NotNull ItemStack base,
                                              @Nullable Tag<Material> tag) {
        if (tag == null) return base;
        return applyDecoration(base, List.of(buildLoreLine(KEY_TAG, tag.getKey().toString())));
    }

    static @NotNull ItemStack decorateResultItem(@NotNull ItemStack base,
                                                 @NotNull Map<String, Integer> money) {
        if (money.isEmpty()) return base;
        List<String> additions = new ArrayList<>();
        for (Map.Entry<String, Integer> e : money.entrySet())
            additions.add(buildLoreLine(KEY_MONEY, e.getKey() + " x" + e.getValue()));
        return applyDecoration(base, additions);
    }

    static @NotNull ItemStack decorateUsesItem(@NotNull Map<String, Integer> maxTradeUses) {
        ItemStack base = new ItemBuilder(Material.PAPER)
                .name(ChatColor.GRAY + "Usage limit").build();
        if (maxTradeUses.isEmpty()) return base;
        List<String> additions = new ArrayList<>();
        for (Map.Entry<String, Integer> e : maxTradeUses.entrySet())
            additions.add(buildLoreLine(KEY_USES, e.getKey() + " x" + e.getValue()));
        return applyDecoration(base, additions);
    }

    static @NotNull ItemStack applyDecoration(@NotNull ItemStack base,
                                              @NotNull List<String> additionLines) {
        ItemStack out  = base.clone();
        ItemMeta  meta = out.getItemMeta();
        if (meta == null) return out;
        meta.setEnchantmentGlintOverride(true);
        List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.removeIf(line -> line != null && line.startsWith(LORE_MARKER));
        lore.addAll(additionLines);
        meta.setLore(lore);
        out.setItemMeta(meta);
        return out;
    }

    static @NotNull ItemStack cleanup(@NotNull ItemStack item) {
        if (item.getType().equals(Material.AIR)) return item;
        ItemStack clean = item.clone();
        ItemMeta  meta  = clean.getItemMeta();
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

    static @Nullable String extractLoreValue(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getLore() == null) return null;
        String prefix = LORE_MARKER + ChatColor.AQUA + KEY_TAG + ChatColor.DARK_GRAY + ": " + ChatColor.GRAY;
        for (String line : meta.getLore())
            if (line != null && line.startsWith(prefix)) return line.substring(prefix.length());
        return null;
    }

    static @NotNull List<String> extractLoreValues(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getLore() == null) return List.of();
        String prefix = LORE_MARKER + ChatColor.AQUA + KEY_MONEY + ChatColor.DARK_GRAY + ": " + ChatColor.GRAY;
        List<String> out = new ArrayList<>();
        for (String line : meta.getLore())
            if (line != null && line.startsWith(prefix)) out.add(line.substring(prefix.length()));
        return out;
    }

    static @Nullable Tag<Material> extractTag(@NotNull ItemStack item) {
        String raw = extractLoreValue(item);
        if (raw == null || raw.isEmpty()) return null;
        return TradeUtils.getMaterialTag(raw);
    }

    static @NotNull Map<String, Integer> extractMoney(@NotNull ItemStack item) {
        Map<String, Integer> result = new HashMap<>();
        for (String line : extractLoreValues(item)) {
            int sep = line.lastIndexOf(" x");
            if (sep < 0) continue;
            try { result.put(line.substring(0, sep), Integer.parseInt(line.substring(sep + 2))); }
            catch (NumberFormatException ignored) { }
        }
        return result;
    }
}
