package fr.milekat.shops.workers.gui;

import fr.milekat.shops.API;
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
 *   <li><b>USES_TAG</b> — configure the per-player-tag usage limits. Each tag (from
 *       {@link API#getAvailableTagsStatic()}) can have its own max value; the trade is
 *       blocked when the player has any tag whose count reached its configured max.</li>
 * </ul>
 */
@SuppressWarnings("deprecation")
public class AdvancedEditor extends FastInv {

    public enum Mode { MATERIAL_TAG, MONEY_TAG, USES_TAG }

    // ---- Layout constants ----
    private static final int   ITEM_SLOT  = 20;
    private static final int   RESET_SLOT = 39;
    private static final int   PREV_SLOT  = 5;
    private static final int   NEXT_SLOT  = 7;
    private static final int   SAVE_SLOT  = 42;
    private static final int[] TAG_SLOTS  = {14, 15, 16, 23, 24, 25, 32, 33, 34};

    // ---- Shared session state ----
    private final ShopAdminSession session;
    private final Player           player;

    // ---- Editor identity ----
    private final int       position;
    private final boolean   firstItem;  // only meaningful in MATERIAL_TAG mode
    private final Mode      mode;
    private final ItemStack editedItem;

    // ---- MATERIAL_TAG ----
    private final List<Tag<Material>> materialTags;
    private @Nullable Tag<Material>   selectedMaterialTag;
    private final @Nullable Tag<Material> initialMaterialTag;

    // ---- MONEY_TAG ----
    private final List<String>         moneyCurrencies;
    private final Map<String, Integer> selectedMoney;
    private final Map<String, Integer> initialMoney;

    // ---- USES_TAG ----
    private final List<String>         usesTagNames;
    private final Map<String, Integer> selectedUses;
    private final Map<String, Integer> initialUses;

    // ---- Pagination ----
    private int tagPage;

    // =========================================================================
    //  Public constructors (initial opening from AdminEditor)
    // =========================================================================

    /** MATERIAL_TAG — edit the required-material tag of a first/second item. */
    public AdvancedEditor(@NotNull ShopAdminSession session,
                          int position, boolean firstItem,
                          @NotNull ItemStack editedItem,
                          @Nullable Tag<Material> currentTag) {
        this(session, position, firstItem, Mode.MATERIAL_TAG, editedItem,
                currentTag, currentTag,
                new HashMap<>(), new HashMap<>(),
                new HashMap<>(), new HashMap<>(),
                List.of(), TradeUtils.getMaterialTagsContaining(editedItem.getType()),
                List.of(), 0);
    }

    /** MONEY_TAG — edit the money-result map of a result slot. */
    public AdvancedEditor(@NotNull ShopAdminSession session,
                          int position,
                          @NotNull ItemStack editedItem,
                          @NotNull Map<String, Integer> currentMoney) {
        this(session, position, true, Mode.MONEY_TAG, editedItem,
                null, null,
                new HashMap<>(currentMoney), new HashMap<>(currentMoney),
                new HashMap<>(), new HashMap<>(),
                safeGetCurrencies(), List.of(),
                List.of(), 0);
    }

    /** USES_TAG — edit the per-player-tag usage limits. */
    public AdvancedEditor(@NotNull ShopAdminSession session,
                          int position,
                          @NotNull Map<String, Integer> currentUses) {
        this(session, position, true, Mode.USES_TAG, new ItemStack(Material.PAPER),
                null, null,
                new HashMap<>(), new HashMap<>(),
                new HashMap<>(currentUses), new HashMap<>(currentUses),
                List.of(), List.of(),
                new ArrayList<>(API.getAvailableTagsStatic()), 0);
    }

    // =========================================================================
    //  Master (all-fields) constructor
    // =========================================================================

    private AdvancedEditor(
            @NotNull ShopAdminSession session,
            int position, boolean firstItem,
            @NotNull Mode mode,
            @NotNull ItemStack editedItem,
            @Nullable Tag<Material> selectedMaterialTag,
            @Nullable Tag<Material> initialMaterialTag,
            @NotNull Map<String, Integer> selectedMoney,
            @NotNull Map<String, Integer> initialMoney,
            @NotNull Map<String, Integer> selectedUses,
            @NotNull Map<String, Integer> initialUses,
            @NotNull List<String> moneyCurrencies,
            @NotNull List<Tag<Material>> materialTags,
            @NotNull List<String> usesTagNames,
            int tagPage
    ) {
        super(45, buildTitle(mode, editedItem));
        this.session             = session;
        this.player              = session.player;
        this.position            = position;
        this.firstItem           = firstItem;
        this.mode                = mode;
        this.editedItem          = editedItem;
        this.selectedMaterialTag = selectedMaterialTag;
        this.initialMaterialTag  = initialMaterialTag;
        this.selectedMoney       = selectedMoney;
        this.initialMoney        = initialMoney;
        this.selectedUses        = selectedUses;
        this.initialUses         = initialUses;
        this.moneyCurrencies     = moneyCurrencies;
        this.materialTags        = materialTags;
        this.usesTagNames        = usesTagNames;
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

    private static @NotNull List<String> safeGetCurrencies() {
        try { return MileBanks.getCurrencies(); }
        catch (RuntimeException e) { return new ArrayList<>(); }
    }

    /** Creates a fresh editor preserving all UI state — used after an AnvilInput returns. */
    private @NotNull AdvancedEditor createReopened() {
        return new AdvancedEditor(
                session, position, firstItem, mode, editedItem,
                selectedMaterialTag, initialMaterialTag,
                new HashMap<>(selectedMoney), new HashMap<>(initialMoney),
                new HashMap<>(selectedUses),  new HashMap<>(initialUses),
                moneyCurrencies, materialTags, usesTagNames, tagPage);
    }

    // =========================================================================
    //  onOpen — registers this inventory with the session orchestrator
    // =========================================================================

    @Override
    protected void onOpen(@NotNull InventoryOpenEvent event) {
        // Tell the session which inventory to watch so it knows when we close
        session.registerSubEditorInventory(event.getInventory());
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

        // Save / Reset
        setItem(SAVE_SLOT, new ItemBuilder(Material.LIME_DYE)
                        .name(ChatColor.GREEN + "Save")
                        .lore(ChatColor.GRAY + "Apply changes and return")
                        .build(),
                event -> save());
        setItem(RESET_SLOT, new ItemBuilder(Material.RED_DYE)
                        .name(ChatColor.RED + "Reset")
                        .lore(ChatColor.GRAY + "Revert to original values and return")
                        .build(),
                event -> reset());

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
                    boolean sel = selectedUses.containsKey(tag);
                    int     amt = selectedUses.getOrDefault(tag, 0);
                    setItem(slot, buildUsesTagItem(tag, sel, amt),
                            event -> onUsesTagClick(tag));
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
                List<String> sel      = new ArrayList<>();
                List<String> unSelect = new ArrayList<>();
                for (String c : moneyCurrencies)
                    (selectedMoney.containsKey(c) ? sel : unSelect).add(c);
                sel.addAll(unSelect);
                yield sel;
            }
            case USES_TAG -> {
                List<String> sel      = new ArrayList<>();
                List<String> unSelect = new ArrayList<>();
                for (String t : usesTagNames)
                    (selectedUses.containsKey(t) ? sel : unSelect).add(t);
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
                // Build the item and apply lore via meta to guarantee all lines are shown
                ItemStack item = new ItemBuilder(editedItem.clone())
                        .name(ChatColor.GREEN + editedItem.getType().name())
                        .build();
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Click to clear all currencies");
                for (Map.Entry<String, Integer> e : selectedMoney.entrySet())
                    lore.add(ChatColor.DARK_GRAY + e.getKey()
                            + ChatColor.GRAY + " ×" + ChatColor.WHITE + e.getValue());
                ItemMeta meta = item.getItemMeta();
                if (meta != null) { meta.setLore(lore); item.setItemMeta(meta); }
                yield item;
            }
            case USES_TAG -> {
                if (selectedUses.isEmpty()) {
                    yield new ItemBuilder(Material.PAPER)
                            .name(ChatColor.GRAY + "No uses limit")
                            .lore(ChatColor.GRAY + "Click a tag to set a limit")
                            .build();
                }
                ItemBuilder b = new ItemBuilder(Material.PAPER)
                        .name(ChatColor.GREEN + "Uses limits");
                for (Map.Entry<String, Integer> e : selectedUses.entrySet())
                    b.lore(ChatColor.DARK_GRAY + e.getKey()
                            + ChatColor.GRAY + " ×" + ChatColor.WHITE + e.getValue());
                b.lore("").lore(ChatColor.GRAY + "Click to clear all limits");
                ItemStack item = b.build();
                applyGlint(item);
                yield item;
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

    private @NotNull ItemStack buildUsesTagItem(@NotNull String tag,
                                                boolean selected, int amount) {
        ItemBuilder b = new ItemBuilder(Material.PAPER)
                .name((selected ? ChatColor.GREEN : ChatColor.YELLOW) + tag);
        if (selected) {
            b.lore(ChatColor.GRAY + "Max uses: " + ChatColor.WHITE + amount)
             .lore("").lore(ChatColor.GREEN + "✔ Active  —  click to change");
            ItemStack item = b.build(); applyGlint(item); return item;
        }
        return b.lore(ChatColor.GRAY + "Click to set max uses").build();
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
                // Clear tag and stay — the IT slot shows the cleared state immediately
                selectedMaterialTag = null;
                session.applyTagEdit(position, firstItem, null);
                refreshDisplay();
            }
            case MONEY_TAG -> {
                // Clear all currencies and stay so the admin can see the cleared state
                selectedMoney.clear();
                session.applyResultEdit(position, new HashMap<>());
                refreshDisplay();
            }
            case USES_TAG -> {
                selectedUses.clear();
                session.applyUsesEdit(position, new HashMap<>());
                refreshDisplay();
            }
        }
    }

    private void onMaterialTagClick(@NotNull Tag<Material> tag) {
        selectedMaterialTag = (selectedMaterialTag != null
                && selectedMaterialTag.getKey().equals(tag.getKey())) ? null : tag;
        session.applyTagEdit(position, firstItem, selectedMaterialTag);
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
                session.applyResultEdit(position, new HashMap<>(selectedMoney));
            }
        });
    }

    private void onUsesTagClick(@NotNull String tag) {
        String initial = String.valueOf(selectedUses.getOrDefault(tag, 1));
        openAnvilAndReturn(initial, input -> {
            if (input != null && !input.isBlank()) {
                int amount;
                try { amount = Integer.parseInt(input.trim()); }
                catch (NumberFormatException e) { amount = 0; }
                if (amount <= 0) selectedUses.remove(tag);
                else             selectedUses.put(tag, amount);
                session.applyUsesEdit(position, new HashMap<>(selectedUses));
            }
        });
    }

    // =========================================================================
    //  Navigation
    // =========================================================================

    /**
     * Applies the current selection to the session and returns to {@link AdminEditor}.
     * Closing this inventory is all that is needed: {@link ShopAdminSession} detects the
     * close and opens a fresh AdminEditor automatically.
     */
    private void save() {
        player.closeInventory();
    }

    /**
     * Reverts all changes made in this sub-editor to the values present when it was opened,
     * then returns to {@link AdminEditor}.
     */
    private void reset() {
        switch (mode) {
            case MATERIAL_TAG -> session.applyTagEdit(position, firstItem, initialMaterialTag);
            case MONEY_TAG    -> session.applyResultEdit(position, new HashMap<>(initialMoney));
            case USES_TAG     -> session.applyUsesEdit(position, new HashMap<>(initialUses));
        }
        player.closeInventory();
    }

    /**
     * Opens an {@link AnvilInput} for the given {@code initial} text, runs {@code action}
     * with the result (null = canceled), then re-opens a fresh copy of this editor.
     *
     * <p>{@code session.subEditorGoingToAnvil} is set to {@code true} before the AnvilInput
     * opens so the session listener ignores the close of this window.  It is reset just before
     * the fresh AdvancedEditor is opened so the session can once again track the lifecycle.</p>
     */
    private void openAnvilAndReturn(@NotNull String initial,
                                    @NotNull Consumer<@Nullable String> action) {
        // Tell the session: "don't reopen AdminEditor when this window closes"
        session.subEditorGoingToAnvil = true;
        new AnvilInput(player, initial, input -> {
            action.accept(input);
            // Re-enable session tracking, then open a fresh AdvancedEditor
            session.subEditorGoingToAnvil = false;
            createReopened().open(player);
        });
    }
}
