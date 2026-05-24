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
import org.bukkit.event.inventory.InventoryCloseEvent;
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
 *   <li><b>MATERIAL_TAG</b> — attach a Bukkit {@link Tag}{@code <Material>} to a first/second
 *       item slot. At most one tag active at a time; clicking the active tag deselects it.</li>
 *   <li><b>MONEY_TAG</b> — configure the money-result map for the result slot.
 *       Multiple currencies selectable; each amount is entered via an Anvil prompt.</li>
 *   <li><b>USES_TAG</b> — configure maxTradeUse (single Anvil prompt for the count) and the
 *       set of MileBanks player-tags that track per-player usage (multi-select toggle).</li>
 * </ul>
 *
 * <h3>Layout (45 slots, 5 rows)</h3>
 * <pre>
 * [BK][BK][BK][BK][BK][BK][BK][BK][BK]   row 0
 * [BK][BK][BK][BK][BK][T0][T1][T2][BK]   row 1
 * [BK][BK][IT][BK][BK][T3][T4][T5][BK]   row 2
 * [BK][BK][BK][BK][BK][T6][T7][T8][BK]   row 3
 * [EX][BK][BK][BK][BK][PR][BK][NX][BK]   row 4
 * </pre>
 * IT=slot 20, T0-T8={14,15,16,23,24,25,32,33,34}, EX=36, PR=41, NX=43
 */
@SuppressWarnings("deprecation")
public class AdvancedEditor extends FastInv {

    public enum Mode { MATERIAL_TAG, MONEY_TAG, USES_TAG }

    // ---- Layout constants ----
    private static final int   ITEM_SLOT = 20;
    private static final int[] TAG_SLOTS = {14, 15, 16, 23, 24, 25, 32, 33, 34};

    // ---- Parent / identity ----
    private final AdminEditor parent;
    private final Player      player;
    private final int         position;
    private final boolean     firstItem;  // only used in MATERIAL_TAG mode
    private final Mode        mode;
    private final ItemStack   editedItem; // for MATERIAL_TAG / MONEY_TAG

    // ---- MATERIAL_TAG state ----
    private final List<Tag<Material>> materialTags;
    private @Nullable Tag<Material>   selectedMaterialTag;

    // ---- MONEY_TAG state ----
    private final List<String>         moneyCurrencies;
    private final Map<String, Integer> selectedMoney;

    // ---- USES_TAG state ----
    private final List<String> usesCurrencies;
    private int                selectedMaxUse;
    private final Set<String>  selectedUsesTags;

    // ---- Pagination / navigation ----
    private int     tagPage   = 0;
    private boolean anvilEdit = false;

    // =========================================================================
    //  Constructors
    // =========================================================================

    /** MATERIAL_TAG — editing a first/second item's required-material tag. */
    public AdvancedEditor(@NotNull AdminEditor parent, @NotNull Player player,
                          int position, boolean firstItem,
                          @NotNull ItemStack editedItem,
                          @Nullable Tag<Material> currentTag) {
        super(45, ChatColor.DARK_AQUA + "Tag editor: " + editedItem.getType().name());
        this.parent              = parent;
        this.player              = player;
        this.position            = position;
        this.firstItem           = firstItem;
        this.mode                = Mode.MATERIAL_TAG;
        this.editedItem          = editedItem;
        this.selectedMaterialTag = currentTag;
        this.materialTags        = TradeUtils.getMaterialTagsContaining(editedItem.getType());
        this.moneyCurrencies     = List.of();
        this.selectedMoney       = new HashMap<>();
        this.usesCurrencies      = List.of();
        this.selectedMaxUse      = 0;
        this.selectedUsesTags    = new LinkedHashSet<>();
        parent.advancedEditorGuiOpen();
    }

    /** MONEY_TAG — editing the money-result map on a result slot. */
    public AdvancedEditor(@NotNull AdminEditor parent, @NotNull Player player,
                          int position,
                          @NotNull ItemStack editedItem,
                          @NotNull Map<String, Integer> currentMoney) {
        super(45, ChatColor.DARK_AQUA + "Money result editor");
        this.parent              = parent;
        this.player              = player;
        this.position            = position;
        this.firstItem           = true;
        this.mode                = Mode.MONEY_TAG;
        this.editedItem          = editedItem;
        this.selectedMaterialTag = null;
        this.materialTags        = List.of();
        this.moneyCurrencies     = safeGetTags(player);
        this.selectedMoney       = new HashMap<>(currentMoney);
        this.usesCurrencies      = List.of();
        this.selectedMaxUse      = 0;
        this.selectedUsesTags    = new LinkedHashSet<>();
        parent.advancedEditorGuiOpen();
    }

    /**
     * USES_TAG — editing maxTradeUse count and the player-tag tracking set.
     *
     * @param currentMax   current maxTradeUse (0 = no limit)
     * @param currentTags  currently selected player-tag names
     */
    public AdvancedEditor(@NotNull AdminEditor parent, @NotNull Player player,
                          int position,
                          int currentMax, @NotNull List<String> currentTags) {
        super(45, ChatColor.DARK_AQUA + "Uses limit editor");
        this.parent              = parent;
        this.player              = player;
        this.position            = position;
        this.firstItem           = true;
        this.mode                = Mode.USES_TAG;
        this.editedItem          = new ItemStack(Material.PAPER);
        this.selectedMaterialTag = null;
        this.materialTags        = List.of();
        this.moneyCurrencies     = List.of();
        this.selectedMoney       = new HashMap<>();
        this.usesCurrencies      = safeGetTags(player);
        this.selectedMaxUse      = currentMax;
        this.selectedUsesTags    = new LinkedHashSet<>(currentTags);
        parent.advancedEditorGuiOpen();
    }

    @Override
    protected void onOpen(InventoryOpenEvent event) {
        refreshDisplay();
    }

    private static @NotNull List<String> safeGetTags(@NotNull Player player) {
        try { return MileBanks.getExistingTags(player); }
        catch (RuntimeException e) { return new ArrayList<>(); }
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
        int start = tagPage * 9;
        int end   = Math.min(start + 9, sorted.size());

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
                    int amt     = selectedMoney.getOrDefault(currency, 0);
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
        if (tagPage > 0)          setItem(getInventory().getSize() - 9, Buttons.PREVIOUS.get(),
                event -> { tagPage--; refreshDisplay(); });
        if (end < sorted.size())  setItem(getInventory().getSize(), Buttons.NEXT.get(),
                event -> { tagPage++; refreshDisplay(); });
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

    // ---- IT slot display ----

    private @NotNull ItemStack buildItemSlotDisplay() {
        return switch (mode) {
            case MATERIAL_TAG -> {
                ItemBuilder b = new ItemBuilder(editedItem.clone())
                        .name(ChatColor.GREEN + editedItem.getType().name())
                        .lore(ChatColor.GRAY + "Click to remove all complexity");
                if (selectedMaterialTag != null)
                    b.lore(ChatColor.DARK_GRAY + "Tag: "
                            + ChatColor.AQUA + selectedMaterialTag.getKey().getKey());
                yield b.build();
            }
            case MONEY_TAG -> {
                ItemBuilder b = new ItemBuilder(editedItem.clone())
                        .name(ChatColor.GREEN + editedItem.getType().name())
                        .lore(ChatColor.GRAY + "Click to remove all complexity");
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
                    b.lore("").lore(ChatColor.GRAY + "Click to change count  |  "
                            + ChatColor.RED + "shift-click to reset");
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
            ItemStack item = b.build();
            applyGlint(item);
            return item;
        }
        return b.lore(ChatColor.GRAY + "Click to select").build();
    }

    private @NotNull ItemStack buildMoneyTagItem(@NotNull String currency, boolean selected, int amount) {
        ItemBuilder b = new ItemBuilder(Material.EMERALD)
                .name((selected ? ChatColor.GOLD : ChatColor.YELLOW) + currency);
        if (selected) {
            b.lore(ChatColor.GRAY + "Amount: " + ChatColor.WHITE + amount)
             .lore("").lore(ChatColor.GOLD + "✔ Active  —  click to change");
            ItemStack item = b.build();
            applyGlint(item);
            return item;
        }
        return b.lore(ChatColor.GRAY + "Click to set amount").build();
    }

    private @NotNull ItemStack buildUsesTagItem(@NotNull String tag, boolean selected) {
        ItemBuilder b = new ItemBuilder(Material.PAPER)
                .name((selected ? ChatColor.GREEN : ChatColor.YELLOW) + tag);
        if (selected) {
            b.lore("").lore(ChatColor.GREEN + "✔ Active  —  click to remove");
            ItemStack item = b.build();
            applyGlint(item);
            return item;
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

    /** IT slot: remove all complexity → push empty state → return to parent. */
    private void onItemSlotClick() {
        switch (mode) {
            case MATERIAL_TAG -> parent.applyAdvancedTagEdit(position, firstItem, null);
            case MONEY_TAG    -> parent.applyAdvancedResultEdit(position, new HashMap<>());
            case USES_TAG     -> onUsesCountClick();
        }
        returnToParent();
    }

    /** MATERIAL_TAG: toggle tag selection and push to parent. */
    private void onMaterialTagClick(@NotNull Tag<Material> tag) {
        selectedMaterialTag = (selectedMaterialTag != null
                && selectedMaterialTag.getKey().equals(tag.getKey())) ? null : tag;
        parent.applyAdvancedTagEdit(position, firstItem, selectedMaterialTag);
        refreshDisplay();
    }

    /** MONEY_TAG: Anvil prompt for amount (0/negative removes the currency). */
    private void onMoneyTagClick(@NotNull String currency) {
        String initial = String.valueOf(selectedMoney.getOrDefault(currency, 1));
        openAnvilAndReturn(initial, input -> {
            if (input != null && !input.isBlank()) {
                int amount;
                try { amount = Integer.parseInt(input.trim()); } catch (NumberFormatException e) { amount = 0; }
                if (amount <= 0) selectedMoney.remove(currency);
                else             selectedMoney.put(currency, amount);
                parent.applyAdvancedResultEdit(position, new HashMap<>(selectedMoney));
            }
        });
    }

    /** USES_TAG: Anvil prompt for the max-uses count. */
    private void onUsesCountClick() {
        String initial = selectedMaxUse > 0 ? String.valueOf(selectedMaxUse) : "1";
        openAnvilAndReturn(initial, input -> {
            if (input != null && !input.isBlank()) {
                int count;
                try { count = Integer.parseInt(input.trim()); } catch (NumberFormatException e) { count = 0; }
                selectedMaxUse = Math.max(0, count);
                if (selectedMaxUse == 0) selectedUsesTags.clear();
                parent.applyAdvancedUsesEdit(position, selectedMaxUse, new ArrayList<>(selectedUsesTags));
            }
        });
    }

    /** USES_TAG: toggle a player-tag on/off (no amount). */
    private void onUsesTagClick(@NotNull String tag) {
        if (selectedUsesTags.contains(tag)) selectedUsesTags.remove(tag);
        else                                selectedUsesTags.add(tag);
        parent.applyAdvancedUsesEdit(position, selectedMaxUse, new ArrayList<>(selectedUsesTags));
        refreshDisplay();
    }

    /**
     * Opens {@link AnvilInput}, navigates away, runs {@code action} on result,
     * then re-opens this editor.
     */
    private void openAnvilAndReturn(@NotNull String initial, @NotNull Consumer<String> action) {
        anvilEdit = true;
        new AnvilInput(player, initial, input -> {
            action.accept(input);
            anvilEdit = false;
            refreshDisplay();
            this.open(player);
        });
    }

    // =========================================================================
    //  Navigation helpers
    // =========================================================================

    private void returnToParent() {
        player.sendMessage(ChatColor.GRAY + "Returning to main editor...");
        anvilEdit = true;
        parent.open(player);
    }

    @Override
    protected void onClose(@NotNull InventoryCloseEvent event) {
        if (anvilEdit) return;
        // User closed without using Exit — go back to parent
        returnToParent();
    }
}
