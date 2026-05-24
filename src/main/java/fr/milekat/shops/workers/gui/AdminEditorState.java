package fr.milekat.shops.workers.gui;

import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Source-of-truth for an in-progress admin edit session.
 *
 * <p>This class is intentionally decoupled from FastInv.  Because FastInv loses its event
 * handlers whenever an inventory window is closed, <em>every</em> {@link AdminEditor} and
 * {@link AdvancedEditor} window is a short-lived view over this object.  New view instances
 * are created on demand; the state here persists for the entire editing session.</p>
 *
 * <p>This class also owns the lifecycle glue: it registers a Bukkit {@link Listener} so it
 * can detect when the player closes the {@link AdvancedEditor} window (by any means) and
 * automatically re-open a fresh {@link AdminEditor}.</p>
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>Created once, via {@link AdminEditor#AdminEditor(Player, Shop, List)}.</li>
 *   <li>Stays alive until {@link #dispose()} is called (save or discard).</li>
 *   <li>Any number of {@link AdminEditor} / {@link AdvancedEditor} views are created over it
 *       during the session.</li>
 * </ol>
 */
class AdminEditorState implements Listener {

    // =========================================================================
    //  DraftTrade — mutable, possibly-incomplete trade kept in the draft cache
    // =========================================================================

    /**
     * In-memory representation of a trade while being edited. Any field may be {@code null};
     * a draft is "complete" only when both {@code firstItem} and {@code resultItem} are set.
     */
    public static class DraftTrade {
        public @Nullable ItemStack firstItem;
        public @Nullable Tag<Material> firstItemTag;
        public @Nullable ItemStack secondItem;
        public @Nullable Tag<Material> secondItemTag;
        public @Nullable ItemStack resultItem;
        public @NotNull Map<String, Integer> moneyResult = new HashMap<>();
        public int maxTradeUse = 0;
        public @Nullable List<String> maxTradeTagsNames;

        public boolean isComplete() { return firstItem != null && resultItem != null; }
        public boolean isEmpty() {
            return firstItem == null && resultItem == null && secondItem == null
                    && moneyResult.isEmpty() && maxTradeUse == 0
                    && (maxTradeTagsNames == null || maxTradeTagsNames.isEmpty());
        }
        public boolean isPartial() { return !isEmpty() && !isComplete(); }

        static @NotNull DraftTrade from(@NotNull Trade t) {
            DraftTrade d = new DraftTrade();
            d.firstItem         = t.getFirstItem();
            d.firstItemTag      = t.getFirstItemTag();
            d.secondItem        = t.getSecondItem();
            d.secondItemTag     = t.getSecondItemTag();
            d.resultItem        = t.getResultItem();
            d.moneyResult       = new HashMap<>(t.getMoneyResult());
            d.maxTradeUse       = t.getMaxTradeUse();
            d.maxTradeTagsNames = t.getMaxTradeTagsNames();
            return d;
        }
    }

    // =========================================================================
    //  Editor state
    // =========================================================================

    final Player player;
    final Shop   shop;
    /** Draft cache — keyed by absolute 1-based trade position (across all pages). */
    final Map<Integer, DraftTrade> drafts = new TreeMap<>();
    int     currentPage  = 1;
    boolean advancedMode = false;
    boolean hasChanges   = false;

    // =========================================================================
    //  Sub-editor lifecycle tracking
    // =========================================================================

    /**
     * The inventory of the currently open {@link AdvancedEditor}, or {@code null} when the
     * admin editor is in front.  Set by {@link #registerSubEditorInventory(Inventory)};
     * cleared when that inventory closes.
     */
    private @Nullable Inventory activeSubEditorInventory = null;

    /**
     * {@code true} while the {@link AdvancedEditor} is intentionally navigating away to an
     * {@link AnvilInput}.  In that case the state listener must NOT reopen the AdminEditor when
     * it detects the AdvancedEditor closing.
     */
    boolean subEditorGoingToAnvil = false;

    // =========================================================================
    //  Constructor / dispose
    // =========================================================================

    AdminEditorState(@NotNull Player player, @NotNull Shop shop, @NotNull List<Trade> trades) {
        this.player = player;
        this.shop   = shop;
        int absPos = 1;
        for (Trade t : trades) {
            drafts.put(absPos++, DraftTrade.from(t));
        }
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    /**
     * Unregisters this listener.  Must be called when the editing session ends
     * (save &amp; exit or exit without save).
     */
    void dispose() {
        HandlerList.unregisterAll(this);
        activeSubEditorInventory = null;
    }

    // =========================================================================
    //  Sub-editor lifecycle hooks (called by AdvancedEditor)
    // =========================================================================

    /**
     * Registers the inventory of the currently open {@link AdvancedEditor} so that the
     * Bukkit listener can detect when it closes.
     */
    void registerSubEditorInventory(@NotNull Inventory inventory) {
        this.activeSubEditorInventory = inventory;
    }

    /**
     * Detects when the tracked {@link AdvancedEditor} inventory closes and, unless we are
     * transitioning to an {@link AnvilInput}, re-opens a fresh {@link AdminEditor}.
     */
    @EventHandler
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (!event.getPlayer().equals(player)) return;
        if (activeSubEditorInventory == null) return;
        if (!event.getInventory().equals(activeSubEditorInventory)) return;

        // Clear the reference immediately — the inventory is gone
        activeSubEditorInventory = null;

        if (subEditorGoingToAnvil) {
            // AdvancedEditor closed because an AnvilInput is taking over; don't reopen admin yet
            return;
        }

        // AdvancedEditor fully dismissed (exit button or Escape) → open a fresh AdminEditor
        Bukkit.getScheduler().runTask(Main.getInstance(),
                () -> new AdminEditor(this).open(player));
    }

    // =========================================================================
    //  Mutation helpers — called by AdvancedEditor to update draft data
    // =========================================================================

    void applyTagEdit(int position, boolean firstItem, @Nullable Tag<Material> tag) {
        DraftTrade d = drafts.get(absPos(position));
        if (d == null) return;
        if (firstItem) d.firstItemTag  = tag;
        else           d.secondItemTag = tag;
        hasChanges = true;
    }

    void applyResultEdit(int position, @NotNull Map<String, Integer> moneyResult) {
        DraftTrade d = drafts.get(absPos(position));
        if (d == null) return;
        d.moneyResult = new HashMap<>(moneyResult);
        hasChanges = true;
    }

    void applyUsesEdit(int position, int maxTradeUse, @NotNull List<String> tagNames) {
        DraftTrade d = drafts.get(absPos(position));
        if (d == null) return;
        d.maxTradeUse       = maxTradeUse;
        d.maxTradeTagsNames = tagNames.isEmpty() ? null : new ArrayList<>(tagNames);
        hasChanges = true;
    }

    private int absPos(int position) {
        return (currentPage - 1) * AdminEditor.EDITOR_TRADES_PER_PAGE + position + 1;
    }
}
