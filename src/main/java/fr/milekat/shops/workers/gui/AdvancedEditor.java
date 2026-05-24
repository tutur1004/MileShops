package fr.milekat.shops.workers.gui;

import fr.milekat.shops.hooks.MileBanks;
import fr.milekat.shops.workers.utils.Buttons;
import fr.milekat.shops.workers.utils.TradeUtils;
import fr.mrmicky.fastinv.FastInv;
import fr.mrmicky.fastinv.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

/**
 * Unified sub-editor opened from {@link AdminEditor} in advanced mode.
 *
 * <ul>
 *   <li><b>MATERIAL_TAG</b> — attach a Bukkit {@link Tag}{@code <Material>} to a
 *       first/second item. At most one tag active; clicking the active tag deselects it.</li>
 *   <li><b>MONEY_TAG</b> — configure the money-result map. Multiple currencies selectable;
 *       amounts entered via an Anvil prompt (0 / negative removes the currency).</li>
 *   <li><b>USES_TAG</b> — configure {@code maxTradeUse} (Anvil prompt) and the set of
 *       MileBanks player-tags that track per-player usage (multi-select toggle).</li>
 * </ul>
 *
 * <h3>Lifecycle / re-instantiation</h3>
 * FastInv loses its event handlers when an inventory window closes. Two transitions require
 * a fresh instance:
 * <ol>
 *   <li><b>AnvilInput → AdvancedEditor</b>: {@link #openAnvilAndReturn} sets
 *       {@code state.subEditorGoingToAnvil = true} so the state listener does not reopen
 *       {@link AdminEditor}, then creates a fresh AdvancedEditor via {@link #createReopened()}
 *       after the anvil closes.</li>
 *   <li><b>AdvancedEditor → AdminEditor</b>: calling {@link #returnToParent()} (or Escape)
 *       just closes this window. {@link AdminEditorState} detects the close and creates a
 *       fresh AdminEditor automatically — <em>no direct call back to AdminEditor here</em>.</li>
 * </ol>
 *
 * <h3>Layout (45 slots, 5 rows)</h3>
 * <pre>
 * [BK][BK][BK][BK][BK][BK][BK][BK][BK]   row 0
 * [BK][BK][BK][BK][BK][T0][T1][T2][BK]   row 1
 * [BK][BK][IT][BK][BK][T3][T4][T5][BK]   row 2
 * [BK][BK][BK][BK][BK][T6][T7][T8][BK]   row 3
 * [BK][BK][PR][BK][EX][BK][NX][BK][BK]   row 4
 * </pre>
 * IT=20  T0-T8={14,15,16,23,24,25,32,33,34}  PR=38  EX=40  NX=42
 */
@SuppressWarnings("deprecation")
public class AdvancedEditor extends FastInv {

    public enum Mode { MATERIAL_TAG, MONEY_TAG, USES_TAG }

    // ---- Layout constants ----
    private static final int   ITEM_SLOT = 20;
    private static final int   PREV_SLOT = 38;
    private static final int   NEXT_SLOT = 42;
    private static final int[] TAG_SLOTS = {14, 15, 16, 23, 24, 25, 32, 33, 34};

    // ---- Shared session state ----
    private final AdminEditorState state;
    private final Player           player; // convenience alias for state.player

    // ---- Editor identity ----
    private final int       position;
    private final boolean   firstItem;  // only meaningful in MATERIAL_TAG mode
    private final Mode      mode;
    private final ItemStack editedItem;

    // ---- MATERIAL_TAG ----
    private final List<Tag<Material>> materialTags;
    private @Nullable Tag<Material>   selectedMaterialTag;

    // ---- MONEY_TAG ----
    private final List<String>         moneyCurrencies;
    private final Map<String, Integer> selectedMoney;

    // ---- USES_TAG ----
    private final List<String> usesCurrencies;
    private int                selectedMaxUse;
    private final Set<String>  selectedUsesTags;

    // ---- Pagination ----
    private int tagPage = 0;

    // =========================================================================
    //  Public constructors (initial opening from AdminEditor)
    // =========================================================================

    /** MATERIAL_TAG — edit the required-material tag of a first/second item. */
    public AdvancedEditor(@NotNull AdminEditorState state,
                          int position, boolean firstItem,
                          @NotNull ItemStack editedItem,
                          @Nullable Tag<Material> currentTag) {
        this(state, position, firstItem, Mode.MATERIAL_TAG, editedItem,
                currentTag, new HashMap<>(), 0, new LinkedHashSet<>(),
                List.of(), TradeUtils.getMaterialTagsContaining(editedItem.getType()), 0);
    }

    /** MONEY_TAG — edit the money-result map of a result slot. */
    public AdvancedEditor(@NotNull AdminEditorState state,
                          int position,
                          @NotNull ItemStack editedItem,
                          @NotNull Map<String, Integer> currentMoney) {
        this(state, position, true, Mode.MONEY_TAG, editedItem,
                null, new HashMap<>(currentMoney), 0, new LinkedHashSet<>(),
                safeGetTags(state.player), List.of(), 0);
    }

    /** USES_TAG — edit maxTradeUse count and the player-tag tracking set. */
    public AdvancedEditor(@NotNull AdminEditorState state,
                          int position,
                          int currentMax, @NotNull List<String> currentTags) {
        this(state, position, true, Mode.USES_TAG, new ItemStack(Material.PAPER),
                null, new HashMap<>(), currentMax, new LinkedHashSet<>(currentTags),
                safeGetTags(state.player), List.of(), 0);
    }

    // =========================================================================
    //  Master (all-fields) constructor
    // =========================================================================

    private AdvancedEditor(
            @NotNull AdminEditorState state,
            int position, boolean firstItem,
            @NotNull Mode mode,
            @NotNull ItemStack editedItem,
            @Nullable Tag<Material> selectedMaterialTag,
            @NotNull Map<String, Integer> selectedMoney,
            int selectedMaxUse,
            @NotNull Set<String> selectedUsesTags,
            @NotNull List<String> usesCurrencies,
            @NotNull List<Tag<Material>> materialTags,
            int tagPage
    ) {
        super(45, buildTitle(mode, editedItem));
        this.state               = state;
        this.player              = state.player;
        this.position            = position;
        this.firstItem           = firstItem;
        this.mode                = mode;
        this.editedItem          = editedItem;
        this.selectedMaterialTag = selectedMaterialTag;
        this.selectedMoney       = selectedMoney;
        this.selectedMaxUse      = selectedMaxUse;
        this.selectedUsesTags    = selectedUsesTags;
        this.usesCurrencies      = usesCurrencies;
        this.materialTags        = materialTags;
        this.moneyCurrencies     = (mode == Mode.MONEY_TAG) ? safeGetTags(state.player) : List.of();
        this.tagPage             = tagPage;
    }

    // ---- helpers ----

    private static @NotNull String buildTitle(@NotNull Mode mode, @NotNull ItemStack item) {
        return switch (mode) {
            case MATERIAL_TAG -> ChatColor.DARK_AQUA + "Tag editor: " + item.getType().name();
            case MONEY_TAG    -> ChatColor.DARK_AQUA + "Money result editor";
            case USES_TAG     -> ChatColor.DARK_AQUA + "Uses limit editor";
        };
    }

    private static @NotNull List<String> safeGetTags(@NotNull Player player) {
        try { return MileBanks.getExistingTags(player); }
        catch (RuntimeException e) { return new ArrayList<>(); }
    }

    /**
     * Creates a fresh AdvancedEditor preserving all current UI state (selections, page).
     * Used after an {@link AnvilInput} returns, to re-register FastInv event handlers.
     */
    private @NotNull AdvancedEditor createReopened() {
        return new AdvancedEditor(
                state, position, firstItem, mode, editedItem,
                selectedMaterialTag, new HashMap<>(selectedMoney),
                selectedMaxUse, new LinkedHashSet<>(selectedUsesTags),
                usesCurrencies, materialTags, tagPage);
    }

    // =========================================================================
    //  onOpen — registers this inventory with the state orchestrator
    // =========================================================================

    @Override
    protected void onOpen(@NotNull InventoryOpenEvent event) {
        // Tell the state which inventory to watch so it knows when we close
        state.registerSubEditorInventory(event.getInventory());
        refreshDisplay();
    }

    // =========================================================================
    //  Rendering
    // =========================================================================

    private void refreshDisplay() {
        // Fill all 45 slots with panes (end-exclusive → 0..44 ✓)
        setItems(0, 45, Buttons.PANE_BLACK.get());
        //  Hollow 3x3 left & right areas
        removeItems(10, 11, 12, 19, 20, 21, 28, 29, 30);
        removeItems(14, 15, 16, 23, 24, 25, 32, 33, 34);

        // IT slot — shows edited item / uses config; click = remove all complexity
        setItem(ITEM_SLOT, buildItemSlotDisplay(), event -> onItemSlotClick());

        // Exit
        setItem(getInventory().getSize() - 5, Buttons.EXIT.get(), event -> returnToParent());

        // Right-side tag grid (selected entries sorted first)
        List<?> sorted = buildSortedTagList();
        int start = tagPage * TAG_SLOTS.length;
        int end   = Math.min(start + TAG_SLOTS.length, sorted.size());

        for (int i = start; i < end; i++) {
            int slot = TAG_SLOTS[i - start];
            switch (mode) {
                case MATERIAL_TAG -> {
                    @SuppressWarnings("unchecked") Tag<Material> tag = (Tag<Material>) sorted.get(i);
                    boolean sel = selectedMaterialTag != null
                            && selectedMaterialTag.getKey().equals(tag.getKey());
                    setItem(slot, buildMaterialTagItem(tag, sel), event -> onMaterialTagClick(tag));
                }
                case MONEY_TAG -> {
                    String currency = (String) sorted.get(i);
                    boolean sel = selectedMoney.containsKey(currency);
                    int     amt = selectedMoney.getOrDefault(currency, 0);
                    setItem(slot, buildMoneyTagItem(currency, sel, amt),
                            event -> onMoneyTagClick(currency));
                }
                case USES_TAG -> {
                    String tag  = (String) sorted.get(i);
                    boolean sel = selectedUsesTags.contains(tag);
                    setItem(slot, buildUsesTagItem(tag, sel), event -> onUsesTagClick(tag));
                }
            }
        }

        // Pagination arrows
        if (tagPage > 0)
            setItem(PREV_SLOT, Buttons.PREVIOUS.get(), event -> { tagPage--; refreshDisplay(); });
        if (end < sorted.size())
            setItem(NEXT_SLOT, Buttons.NEXT.get(), event -> { tagPage++; refreshDisplay(); });
    }

    private @NotNull List<?> buildSortedTagList() {
        return switch (mode) {
            case MATERIAL_TAG -> {
                List<Tag<Material>> sorted = new ArrayList<>(materialTags);
                if (selectedMaterialTag != null) {
                    Tag<Material> sel = selectedMaterialTag;
                    sorted.removeIf(t -> t.getKey().equals(sel.getKey()));
                    sorted.addFirst(sel);
                }
                yield sorted;
            }
            case MONEY_TAG -> {
                List<String> sel   = new ArrayList<>();
                List<String> unSelect = new ArrayList<>();
                for (String c : moneyCurrencies)
                    (selectedMoney.containsKey(c) ? sel : unSelect).add(c);
                sel.addAll(unSelect);
                yield sel;
            }
            case USES_TAG -> {
                List<String> sel   = new ArrayList<>(selectedUsesTags);
                List<String> unSelect = new ArrayList<>();
                for (String c : usesCurrencies)
                    if (!selectedUsesTags.contains(c)) unSelect.add(c);
                sel.addAll(unSelect);
                yield sel;
            }
        };
    }

    // ---- IT slot ----

    private @NotNull ItemStack buildItemSlotDisplay() {
        return switch (mode) {
            case MATERIAL_TAG -> {
                ItemBuilder b = new ItemBuilder(editedItem.clone())
                        .name(ChatColor.GREEN + editedItem.getType().name())
                        .lore(ChatColor.GRAY + "Click to clear tag");
                if (selectedMaterialTag != null)
                    b.lore(ChatColor.DARK_GRAY + "Tag: "
                            + ChatColor.AQUA + selectedMaterialTag.getKey().getKey());
                yield b.build();
            }
            case MONEY_TAG -> {
                ItemBuilder b = new ItemBuilder(editedItem.clone())
                        .name(ChatColor.GREEN + editedItem.getType().name())
                        .lore(ChatColor.GRAY + "Click to clear all currencies");
                if (!selectedMoney.isEmpty())
                    for (Map.Entry<String, Integer> e : selectedMoney.entrySet())
                        b.lore(ChatColor.DARK_GRAY + e.getKey()
                                + ChatColor.GRAY + " ×" + ChatColor.WHITE + e.getValue());
                yield b.build();
            }
            case USES_TAG -> {
                if (selectedMaxUse > 0) {
                    ItemBuilder b = new ItemBuilder(Material.PAPER)
                            .name(ChatColor.GREEN + "Max uses: " + selectedMaxUse);
                    for (String t : selectedUsesTags)
                        b.lore(ChatColor.GRAY + "Tag: " + ChatColor.WHITE + t);
                    b.lore("").lore(ChatColor.GRAY + "Click to change count");
                    ItemStack item = b.build();
                    applyGlint(item);
                    yield item;
                }
                yield new ItemBuilder(Material.PAPER)
                        .name(ChatColor.GRAY + "No uses limit")
                        .lore(ChatColor.GRAY + "Click to set max uses")
                        .build();
            }
        };
    }

    // ---- Tag slot items ----

    private @NotNull ItemStack buildMaterialTagItem(@NotNull Tag<Material> tag, boolean selected) {
        ItemBuilder b = new ItemBuilder(Material.NAME_TAG)
                .name((selected ? ChatColor.GREEN : ChatColor.AQUA) + tag.getKey().getKey())
                .lore(ChatColor.GRAY + "Namespace: " + ChatColor.WHITE + tag.getKey().getNamespace())
                .lore(ChatColor.GRAY + "Materials: " + ChatColor.WHITE + tag.getValues().size());
        int preview = 0;
        for (Material m : tag.getValues()) {
            if (preview++ >= 5) { b.lore(ChatColor.DARK_GRAY + "..."); break; }
            b.lore(ChatColor.DARK_GRAY + "- " + m.name());
        }
        b.lore("");
        if (selected) {
            b.lore(ChatColor.GREEN + "✔ Active  —  click to deselect");
            ItemStack item = b.build(); applyGlint(item); return item;
        }
        return b.lore(ChatColor.GRAY + "Click to select").build();
    }

    private @NotNull ItemStack buildMoneyTagItem(@NotNull String currency,
                                                 boolean selected, int amount) {
        ItemBuilder b = new ItemBuilder(Material.EMERALD)
                .name((selected ? ChatColor.GOLD : ChatColor.YELLOW) + currency);
        if (selected) {
            b.lore(ChatColor.GRAY + "Amount: " + ChatColor.WHITE + amount)
             .lore("").lore(ChatColor.GOLD + "✔ Active  —  click to change");
            ItemStack item = b.build(); applyGlint(item); return item;
        }
        return b.lore(ChatColor.GRAY + "Click to set amount").build();
    }

    private @NotNull ItemStack buildUsesTagItem(@NotNull String tag, boolean selected) {
        ItemBuilder b = new ItemBuilder(Material.PAPER)
                .name((selected ? ChatColor.GREEN : ChatColor.YELLOW) + tag);
        if (selected) {
            b.lore("").lore(ChatColor.GREEN + "✔ Active  —  click to remove");
            ItemStack item = b.build(); applyGlint(item); return item;
        }
        return b.lore(ChatColor.GRAY + "Click to add").build();
    }

    private static void applyGlint(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setEnchantmentGlintOverride(true); item.setItemMeta(meta); }
    }

    // =========================================================================
    //  Click handlers
    // =========================================================================

    private void onItemSlotClick() {
        switch (mode) {
            case MATERIAL_TAG -> {
                selectedMaterialTag = null;
                state.applyTagEdit(position, firstItem, null);
                returnToParent();
            }
            case MONEY_TAG -> {
                selectedMoney.clear();
                state.applyResultEdit(position, new HashMap<>());
                returnToParent();
            }
            case USES_TAG -> onUsesCountClick();
        }
    }

    private void onMaterialTagClick(@NotNull Tag<Material> tag) {
        selectedMaterialTag = (selectedMaterialTag != null
                && selectedMaterialTag.getKey().equals(tag.getKey())) ? null : tag;
        state.applyTagEdit(position, firstItem, selectedMaterialTag);
        refreshDisplay();
    }

    private void onMoneyTagClick(@NotNull String currency) {
        String initial = String.valueOf(selectedMoney.getOrDefault(currency, 1));
        openAnvilAndReturn(initial, input -> {
            if (input != null && !input.isBlank()) {
                int amount;
                try { amount = Integer.parseInt(input.trim()); }
                catch (NumberFormatException e) { amount = 0; }
                if (amount <= 0) selectedMoney.remove(currency);
                else             selectedMoney.put(currency, amount);
                state.applyResultEdit(position, new HashMap<>(selectedMoney));
            }
        });
    }

    private void onUsesCountClick() {
        String initial = selectedMaxUse > 0 ? String.valueOf(selectedMaxUse) : "1";
        openAnvilAndReturn(initial, input -> {
            if (input != null && !input.isBlank()) {
                int count;
                try { count = Integer.parseInt(input.trim()); }
                catch (NumberFormatException e) { count = 0; }
                selectedMaxUse = Math.max(0, count);
                if (selectedMaxUse == 0) selectedUsesTags.clear();
                state.applyUsesEdit(position, selectedMaxUse, new ArrayList<>(selectedUsesTags));
            }
        });
    }

    private void onUsesTagClick(@NotNull String tag) {
        if (selectedUsesTags.contains(tag)) selectedUsesTags.remove(tag);
        else                                selectedUsesTags.add(tag);
        state.applyUsesEdit(position, selectedMaxUse, new ArrayList<>(selectedUsesTags));
        refreshDisplay();
    }

    // =========================================================================
    //  Navigation
    // =========================================================================

    /**
     * Dismisses this sub-editor and hands control back to AdminEditor.
     *
     * <p>Closing this inventory is all that is needed: {@link AdminEditorState} is
     * listening for the {@code InventoryCloseEvent} and will create a fresh
     * {@link AdminEditor} automatically.</p>
     */
    private void returnToParent() {
        player.closeInventory();
        // AdminEditorState.onInventoryClose() detects this and opens a new AdminEditor
    }

    /**
     * Opens an {@link AnvilInput} for the given {@code initial} text, runs {@code action}
     * with the result (null = canceled), then re-opens a fresh copy of this editor.
     *
     * <p>{@code state.subEditorGoingToAnvil} is set to {@code true} before the AnvilInput
     * opens so the state listener ignores the close of this window.  It is reset to
     * {@code false} just before the fresh AdvancedEditor is opened, so the state can once
     * again track the sub-editor lifecycle.</p>
     */
    private void openAnvilAndReturn(@NotNull String initial,
                                    @NotNull Consumer<@Nullable String> action) {
        // Tell the state: "don't reopen AdminEditor when this window closes"
        state.subEditorGoingToAnvil = true;
        new AnvilInput(player, initial, input -> {
            action.accept(input);
            // Re-enable state tracking, then open a fresh AdvancedEditor
            state.subEditorGoingToAnvil = false;
            createReopened().open(player);
        });
    }
}
