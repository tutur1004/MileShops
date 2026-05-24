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
 * Entry point and persistent state for an admin shop-editing session.
 *
 * <p>Because FastInv loses its event handlers whenever an inventory window closes, the
 * {@link AdminEditor} and {@link AdvancedEditor} views are short-lived: a fresh instance is
 * created each time the player returns to a view.  This class is the single object that
 * survives the entire session and holds all mutable state.</p>
 *
 * <p>It also acts as a Bukkit {@link Listener} to detect when {@link AdvancedEditor} closes
 * and automatically re-opens a fresh {@link AdminEditor} — the views themselves have no
 * back-reference to each other.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 * new ShopAdminSession(player, shop, trades).open();
 * </pre>
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>Created once by the caller (e.g. {@code ShopUtils}).</li>
 *   <li>{@link #open()} shows the first {@link AdminEditor} view.</li>
 *   <li>Any number of view instances may be created over this session object.</li>
 *   <li>{@link #dispose()} unregisters the listener when the session ends.</li>
 * </ol>
 */
public class ShopAdminSession implements Listener {

    // =========================================================================
    //  DraftTrade — possibly-incomplete trade held in the in-memory cache
    // =========================================================================

    /**
     * In-memory representation of a trade while being edited. Any field may be {@code null};
     * a draft is "complete" only when both {@code firstItem} and {@code resultItem} are set.
     */
    static class DraftTrade {
        @Nullable ItemStack firstItem;
        @Nullable Tag<Material> firstItemTag;
        @Nullable ItemStack secondItem;
        @Nullable Tag<Material> secondItemTag;
        @Nullable ItemStack resultItem;
        @NotNull Map<String, Integer> moneyResult = new HashMap<>();
        int maxTradeUse = 0;
        @Nullable List<String> maxTradeTagsNames;

        boolean isComplete() { return firstItem != null && resultItem != null; }
        boolean isEmpty() {
            return firstItem == null && resultItem == null && secondItem == null
                    && moneyResult.isEmpty() && maxTradeUse == 0
                    && (maxTradeTagsNames == null || maxTradeTagsNames.isEmpty());
        }
        boolean isPartial() { return !isEmpty() && !isComplete(); }

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
    //  Session state
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
     * {@code true} while {@link AdvancedEditor} is intentionally navigating to an
     * {@link AnvilInput}.  Prevents the state listener from reopening {@link AdminEditor}
     * when it detects the sub-editor closing.
     */
    boolean subEditorGoingToAnvil = false;

    // =========================================================================
    //  Constructor / entry point
    // =========================================================================

    public ShopAdminSession(@NotNull Player player, @NotNull Shop shop,
                            @NotNull List<Trade> trades) {
        this.player = player;
        this.shop   = shop;
        int absPos = 1;
        for (Trade t : trades) drafts.put(absPos++, DraftTrade.from(t));
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    /** Opens the first {@link AdminEditor} view for this session. */
    public void open() {
        new AdminEditor(this).open(player);
    }

    /**
     * Unregisters this listener.
     * Must be called when the editing session ends (save &amp; exit or exit without save).
     */
    void dispose() {
        HandlerList.unregisterAll(this);
        activeSubEditorInventory = null;
    }

    // =========================================================================
    //  Sub-editor lifecycle hooks (called by AdvancedEditor)
    // =========================================================================

    /**
     * Registers the inventory of the currently open {@link AdvancedEditor} so this listener
     * can detect when it closes.
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

        activeSubEditorInventory = null;

        if (subEditorGoingToAnvil) {
            // AdvancedEditor closed to make way for an AnvilInput — do not reopen AdminEditor yet
            return;
        }

        // AdvancedEditor dismissed (Save / Reset / Escape) → open a fresh AdminEditor
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> new AdminEditor(this).open(player));
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

    int absPos(int position) {
        return (currentPage - 1) * AdminEditor.EDITOR_TRADES_PER_PAGE + position + 1;
    }
}
