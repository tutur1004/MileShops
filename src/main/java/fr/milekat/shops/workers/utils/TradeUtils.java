package fr.milekat.shops.workers.utils;

import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.workers.gui.InventoryStorage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Utility class for handling trade operations including calculation, simulation, and execution.
 * This class provides methods to determine the maximum number of trades a player can perform,
 * simulate trades in virtual inventories, and execute actual trades on player inventories.
 * Supports multiple inventory types: main inventory, ender chest, shulker boxes, and combinations.
 *
 * @author Milekat
 */
public class TradeUtils {
    /** Maximum stack size for items in Minecraft inventory (standard stack) */
    private static final int MAX_STACK_SIZE = 64;

    /** Size of the player's main inventory (36 slots: 4 rows × 9 columns) */
    private static final int INVENTORY_SIZE = 36;

    /** Size of the player's ender chest (27 slots: 3 rows × 9 columns) */
    private static final int ENDER_CHEST_SIZE = 27;

    /** Size of a shulker box (27 slots: 3 rows × 9 columns) */
    private static final int SHULKER_SIZE = 27;

    /**
     * Retrieves the display name for a given material from the configuration file.
     * This allows for customizable material names in messages and UIs.
     *
     * @param material the Bukkit Material to get the display name for
     * @return the configured display name for the material, or the material's default toString() if not configured
     */
    public static @NotNull String getMaterial(@NotNull Material material) {
        return Main.getConfigs().getString("materials-font." + material, material.toString());
    }

    /**
     * Retrieves a Bukkit Tag for the specified tag name.
     * First attempts to find the tag in the REGISTRY_BLOCKS, then falls back to REGISTRY_ITEMS.
     *
     * @param tagName the name of the tag to retrieve (e.g., "logs", "planks")
     * @return the Tag if found, null otherwise
     */
    public static @Nullable Tag<Material> getMaterialTag(@NotNull String tagName) {
        Tag<Material> found = Bukkit.getTag(Tag.REGISTRY_BLOCKS, NamespacedKey.minecraft(tagName), Material.class);
        if (found == null) {
            found = Bukkit.getTag(Tag.REGISTRY_ITEMS, NamespacedKey.minecraft(tagName), Material.class);
        }
        return found;
    }

    /**
     * Retrieves the default trade mode from the configuration file.
     * If the configured mode is invalid, defaults to INVENTORY mode and logs a warning.
     *
     * @return the default TradeMode for this server
     */
    public static TradeMode getDefaultTradeMode() {
        String mode = Main.getConfigs().getString("settings.default-trade-mode", "INVENTORY").toUpperCase();
        try {
            return TradeMode.valueOf(mode);
        } catch (IllegalArgumentException e) {
            Main.getMileLogger().warning("Invalid default trade mode in config '"
                    + mode + "' Using INVENTORY instead.");
            return TradeMode.INVENTORY;
        }
    }

    /**
     * Replaces all trade-related placeholders in a message with actual values.
     * Supports the following placeholders:
     * <ul>
     *     <li>&lt;count&gt; - Number of trades to perform</li>
     *     <li>&lt;shop_name&gt; - Name of the shop</li>
     *     <li>&lt;first_amount&gt; - Amount per trade for the required item</li>
     *     <li>&lt;first_total_amount&gt; - Total amount of required items</li>
     *     <li>&lt;first_material&gt; - Display name of the required material</li>
     *     <li>&lt;result_amount&gt; - Amount per trade for the result item</li>
     *     <li>&lt;result_total_amount&gt; - Total amount of result items</li>
     *     <li>&lt;result_material&gt; - Display name of the result material</li>
     * </ul>
     *
     * @param message the message template containing placeholders
     * @param shop the shop involved in the trade
     * @param trade the trade details (required and result items)
     * @param count the number of trades to perform
     * @return the formatted message with all placeholders replaced
     */
    public static @NotNull String tradeFormatting(@NotNull String message, @NotNull Shop shop,
                                                  @NotNull Trade trade, int count) {
        return message.replaceAll("<count>", String.valueOf(count))
                .replaceAll("<shop_name>", shop.getName())
                .replaceAll("<first_amount>", String.valueOf(trade.getFirstItem().getAmount()))
                .replaceAll("<first_total_amount>", String.valueOf(trade.getFirstItem().getAmount() * count))
                .replaceAll("<first_material>", TradeUtils.getMaterial(trade.getFirstItem().getType()))
                .replaceAll("<result_amount>", String.valueOf(trade.getResultItem().getAmount()))
                .replaceAll("<result_total_amount>", String.valueOf(trade.getResultItem().getAmount() * count))
                .replaceAll("<result_material>", TradeUtils.getMaterial(trade.getResultItem().getType()));
    }

    /**
     * Calculates the maximum number of trades a player can perform based on their current inventory.
     * This method simulates trades across all applicable inventories (main, ender chest, shulkers)
     * according to the specified trade mode without actually modifying the player's inventory.
     *
     * <p>Trade modes:</p>
     * <ul>
     *     <li>INVENTORY - Only main inventory</li>
     *     <li>ENDER_CHEST - Main inventory and ender chest</li>
     *     <li>SHULKER - Main inventory and shulker boxes in main inventory</li>
     *     <li>END_SHULKER - All of the above plus shulker boxes in ender chest</li>
     * </ul>
     *
     * @param player the player whose inventory to check
     * @param requiredItems a list of ItemStacks required for each trade
     * @param resultItem the ItemStack that will be given to the player per trade
     * @param unlimitedTrades if true, calculate trades across all inventories; if false, stop after first inventory
     * @param tradeMode the trade mode determining which inventories to check
     * @return the maximum number of trades the player can perform
     */
    public static int maxDoAbleTrades(@NotNull Player player,
                                      @NotNull List<ItemStack> requiredItems,
                                      @NotNull ItemStack resultItem,
                                      boolean unlimitedTrades,
                                      TradeMode tradeMode) {
        List<InventoryStorage> inventories = buildInventoryList(player, tradeMode);

        int doAbleTrade = 0;
        for (InventoryStorage storage : inventories) {
            int traded = simulateTrades(storage, requiredItems, resultItem, unlimitedTrades);
            doAbleTrade += traded;

            // If unlimited trades is disabled, stop after the first inventory has contributed
            if (!unlimitedTrades && doAbleTrade > 0) break;
        }
        return doAbleTrade;
    }

    /**
     * Simulates trades in a virtual copy of the given inventory storage.
     * This method does not modify the actual inventory; instead, it creates a virtual inventory
     * and performs repeated trade simulations to determine the maximum possible trades.
     *
     * <p>The simulation process:</p>
     * <ol>
     *     <li>Creates a virtual inventory copy</li>
     *     <li>Checks if required items are available</li>
     *     <li>Removes required items from the virtual inventory</li>
     *     <li>Attempts to add result items</li>
     *     <li>Repeats until items are insufficient or inventory is full</li>
     * </ol>
     *
     * @param storage the inventory storage to simulate trades in
     * @param requiredItems the list of items required per trade
     * @param resultItem the item given as a result per trade
     * @param unlimitedTrades if true, simulate as many trades as possible; if false, simulate only one trade
     * @return the number of trades that can be performed in this inventory
     */
    private static int simulateTrades(@NotNull InventoryStorage storage,
                                      @NotNull List<ItemStack> requiredItems,
                                      @NotNull ItemStack resultItem,
                                      boolean unlimitedTrades) {
        Inventory virtualInv = createVirtualInventory(storage);
        int traded = 0;

        // If unlimited trades are enabled, allow up to inventory size * max stack size iterations
        // Otherwise, only simulate a single trade
        int maxIterations = unlimitedTrades ? virtualInv.getSize() * MAX_STACK_SIZE : 1;

        for (int i = 0; i < maxIterations; i++) {
            // Check if the virtual inventory has all required items
            if (!hasAllItems(virtualInv, requiredItems)) {
                break;
            }

            // Remove all required items from the virtual inventory
            requiredItems.forEach(virtualInv::removeItem);

            // Attempt to add the result item to the virtual inventory
            Map<Integer, ItemStack> leftOver = virtualInv.addItem(resultItem.clone());
            if (leftOver.isEmpty()) {
                // Trade successful - increment counter
                traded++;
            } else {
                // Not enough space for result item - stop simulation
                break;
            }
        }

        return traded;
    }

    /**
     * Checks if the inventory contains at least one of each required item (with their respective amounts).
     * This is a helper method used for verifying trade prerequisites.
     *
     * @param inventory the inventory to check
     * @param requiredItems the list of required items with their amounts
     * @return true if the inventory contains all required items, false otherwise
     */
    private static boolean hasAllItems(@NotNull Inventory inventory, @NotNull List<ItemStack> requiredItems) {
        for (ItemStack item : requiredItems) {
            if (!inventory.containsAtLeast(item, item.getAmount())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Executes a trade by removing required items from player inventories and adding result items.
     * This method processes the actual trade transaction across all applicable inventories.
     *
     * <p>Trade execution steps:</p>
     * <ol>
     *     <li>Build the list of inventories based on trade mode</li>
     *     <li>Remove all required items across inventories</li>
     *     <li>Add result items to available inventory slots</li>
     *     <li>Update shulker box metadata if applicable</li>
     * </ol>
     *
     * <p><b>Note:</b> This method assumes the trade has already been validated.
     * Use {@link #maxDoAbleTrades(Player, List, ItemStack, boolean, TradeMode)} to validate before calling this.</p>
     *
     * @param player the player performing the trade
     * @param requiredItems the list of items to remove from the player's inventories
     * @param resultItem the item to add to the player's inventories
     * @param trades the number of trades to execute
     * @param tradeMode the trade mode determining which inventories to access
     */
    public static void executeTrade(@NotNull Player player,
                                    @NotNull List<ItemStack> requiredItems,
                                    @NotNull ItemStack resultItem,
                                    int trades,
                                    TradeMode tradeMode) {
        List<InventoryStorage> inventories = buildInventoryList(player, tradeMode);

        // First, remove all required items from inventories
        removeItemsFromInventories(inventories, requiredItems, trades);

        // Then, add result items to inventories
        addItemsToInventories(inventories, resultItem, trades);
    }

    /**
     * Removes the required items from player inventories.
     * Items are removed in order across all available inventories until the required amount is reached.
     * If insufficient items are found, a warning is logged.
     *
     * <p>Removal order:</p>
     * <ol>
     *     <li>Main inventory</li>
     *     <li>Ender chest (if applicable)</li>
     *     <li>Shulker boxes in main inventory (if applicable)</li>
     *     <li>Shulker boxes in ender chest (if applicable)</li>
     * </ol>
     *
     * @param inventories the list of inventories to remove items from
     * @param requiredItems the list of items required for the trades
     * @param trades the number of trades (affects total items to remove)
     */
    private static void removeItemsFromInventories(@NotNull List<InventoryStorage> inventories,
                                                   @NotNull List<ItemStack> requiredItems,
                                                   int trades) {
        // Process each required item type
        for (ItemStack item : requiredItems) {
            int toRemove = trades; // Total amount to remove (total items = amount per trade × trades)

            // Iterate through all inventories
            for (InventoryStorage storage : inventories) {
                if (toRemove <= 0) break; // All items removed

                ItemStack clonedItem = item.clone();
                Inventory inventory = storage.inventory();

                // Remove items from this specific inventory until we have removed enough or run out
                while (toRemove > 0 && inventory.containsAtLeast(clonedItem, clonedItem.getAmount())) {
                    inventory.removeItem(clonedItem);
                    toRemove--;
                }

                // If this inventory is a shulker box, update its metadata
                updateShulkerIfNeeded(storage);
            }

            // Log warning if we couldn't remove all required items
            if (toRemove > 0) {
                Main.getMileLogger().warning("Not enough items to remove from the inventories");
            }
        }
    }

    /**
     * Adds result items to player inventories.
     * Items are distributed across all available inventories in order until the specified number of trades is reached.
     * If no more space is available in any inventory, the process stops.
     *
     * <p>Addition order (same as removal):</p>
     * <ol>
     *     <li>Main inventory</li>
     *     <li>Ender chest (if applicable)</li>
     *     <li>Shulker boxes in main inventory (if applicable)</li>
     *     <li>Shulker boxes in ender chest (if applicable)</li>
     * </ol>
     *
     * @param inventories the list of inventories to add items to
     * @param resultItem the item to add to the inventories
     * @param trades the number of trades (number of result items to add)
     */
    private static void addItemsToInventories(@NotNull List<InventoryStorage> inventories,
                                              @NotNull ItemStack resultItem,
                                              int trades) {
        int given = 0; // Counter for items successfully added

        // Iterate through each inventory
        for (InventoryStorage storage : inventories) {
            if (given >= trades) break; // All items have been added

            Inventory inventory = storage.inventory();
            int maxSlots = inventory.getSize();

            // Attempt to add items to each slot in the inventory
            for (int i = 0; i < maxSlots && given < trades; i++) {
                Map<Integer, ItemStack> leftOver = inventory.addItem(resultItem.clone());
                if (leftOver.isEmpty()) {
                    // Successfully added the item
                    given++;
                } else {
                    // Inventory is full, move to next inventory
                    break;
                }
            }

            // If this inventory is a shulker box, update its metadata
            updateShulkerIfNeeded(storage);
        }
    }

    /**
     * Updates the shulker box metadata if the given inventory storage represents a shulker box.
     * This is necessary because shulker boxes are stored as ItemStacks with metadata,
     * and changes to their internal inventory must be reflected back to the item metadata.
     *
     * <p><b>Important:</b> This method must be called whenever a shulker box inventory is modified,
     * otherwise changes will not persist.</p>
     *
     * @param storage the inventory storage to update (if it's a shulker box)
     */
    private static void updateShulkerIfNeeded(@NotNull InventoryStorage storage) {
        if (storage.isShulkerBox()) {
            BlockStateMeta blockStateMeta = storage.blockStateMeta();
            ShulkerBox shulkerBox = storage.shulkerBox();
            ItemStack itemStack = storage.itemStack();

            // Ensure all references are non-null before updating
            if (blockStateMeta != null && shulkerBox != null && itemStack != null) {
                blockStateMeta.setBlockState(shulkerBox);
                itemStack.setItemMeta(blockStateMeta);
            }
        }
    }

    /**
     * Creates a virtual copy of an inventory storage.
     * This virtual inventory is used for simulation without affecting the actual inventory.
     * The UUID is used as a unique identifier for the virtual inventory.
     *
     * <p><b>Use case:</b> Simulating trades to calculate the maximum number of possible trades
     * without modifying the player's actual inventory.</p>
     *
     * @param storage the inventory storage to create a virtual copy of
     * @return a new virtual Inventory with the same contents as the storage
     */
    private static @NotNull Inventory createVirtualInventory(@NotNull InventoryStorage storage) {
        Inventory virtualInventory = Bukkit.createInventory(
                null,
                storage.size(),
                UUID.randomUUID().toString()
        );
        virtualInventory.setContents(storage.inventory().getStorageContents());
        return virtualInventory;
    }

    /**
     * Builds the list of inventory storages based on the trade mode.
     * Different trade modes include different combinations of inventories.
     *
     * <p>Trade modes and their inventories:</p>
     * <ul>
     *     <li><b>INVENTORY:</b> Main inventory only</li>
     *     <li><b>ENDER_CHEST:</b> Main inventory + Ender chest</li>
     *     <li><b>SHULKER:</b> Main inventory + Shulker boxes in main inventory</li>
     *     <li><b>END_SHULKER:</b> Main inventory + Ender chest + All shulker boxes</li>
     * </ul>
     *
     * @param player the player whose inventories to load
     * @param tradeMode the trade mode determining which inventories to include
     * @return a list of InventoryStorage objects containing all applicable inventories
     */
    private static @NotNull List<InventoryStorage> buildInventoryList(@NotNull Player player,
                                                                      @NotNull TradeMode tradeMode) {
        List<InventoryStorage> inventories = new ArrayList<>();

        // Always include the main inventory
        inventories.add(new InventoryStorage(player.getInventory(), INVENTORY_SIZE));

        // Add ender chest if trade mode allows it
        if (tradeMode == TradeMode.ENDER_CHEST || tradeMode == TradeMode.END_SHULKER) {
            inventories.add(new InventoryStorage(player.getEnderChest(), ENDER_CHEST_SIZE));
        }

        // Add shulker boxes from main inventory if trade mode allows it
        if (tradeMode == TradeMode.SHULKER || tradeMode == TradeMode.END_SHULKER) {
            inventories.addAll(getShulkersFromInventory(player.getInventory()));
        }

        // Add shulker boxes from ender chest if trade mode allows it (only for END_SHULKER mode)
        if (tradeMode == TradeMode.END_SHULKER) {
            inventories.addAll(getShulkersFromInventory(player.getEnderChest()));
        }

        return inventories;
    }

    /**
     * Extracts all shulker boxes from a given inventory and returns them as InventoryStorage objects.
     * This method iterates through the inventory, identifies items that contain shulker box block states,
     * and wraps them in InventoryStorage objects for unified handling.
     *
     * <p><b>How it works:</b></p>
     * <ol>
     *     <li>Iterates through all items in the inventory</li>
     *     <li>Checks if the item has BlockStateMeta</li>
     *     <li>Verifies if the block state is a ShulkerBox</li>
     *     <li>Creates an InventoryStorage with the shulker's inventory and metadata</li>
     * </ol>
     *
     * @param inventory the inventory to scan for shulker boxes
     * @return a list of InventoryStorage objects representing all shulker boxes found, empty list if none found
     */
    public static @NotNull List<InventoryStorage> getShulkersFromInventory(@NotNull Inventory inventory) {
        List<InventoryStorage> shulkers = new ArrayList<>();

        // Iterate through all items in the storage contents (ignores armor slots)
        for (ItemStack loopItem : inventory.getStorageContents()) {
            if (loopItem != null && loopItem.getItemMeta() instanceof BlockStateMeta stateMeta) {
                if (stateMeta.getBlockState() instanceof ShulkerBox shulkerBox) {
                    // Found a shulker box - wrap it in an InventoryStorage object
                    shulkers.add(new InventoryStorage(
                            shulkerBox.getInventory(),
                            SHULKER_SIZE,
                            loopItem,
                            stateMeta,
                            shulkerBox
                    ));
                }
            }
        }

        return shulkers;
    }
}
