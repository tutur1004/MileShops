package fr.milekat.shops.workers.utils;

import fr.milekat.shops.API;
import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.api.classes.TradeMode;
import fr.milekat.shops.api.events.TradeCompleteEvent;
import fr.milekat.shops.hooks.MileBanks;
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
     * Process trade(s) for a player, with the option to process multiple trades if possible.
     *
     * @param player performing the trade
     * @param tradeMode the mode of trade to process, determining which inventories to consider for the trade
     * @param shop the shop where the trade is taking place
     * @param trade to process
     * @param multiple if true, the method will trade as much has the player can
     * @return the number of trade processed
     */
    public static int processedTrades(@NotNull Player player,
                                @NotNull TradeMode tradeMode,
                                @NotNull Shop shop,
                                @NotNull Trade trade,
                                boolean multiple) {
        //  Set the trade items
        List<ItemStack> tradeItems = new LinkedList<>();
        tradeItems.add(trade.getFirstItem().clone());
        if (trade.getSecondItem() != null) {
            tradeItems.add(trade.getSecondItem().clone());
        }

        //  Calculate the max doable trades
        int maxDoAbleTrades = TradeUtils.maxDoAbleTrades(player, tradeItems,
                trade.getResultItem().clone(), multiple, tradeMode);

        //  If no trades can be done, return 0
        if (maxDoAbleTrades <= 0) {
            Main.message(player, Main.getConfigs().getMessage("messages.gui.chest-shop.messages.no-trade",
                    "&cYou don't have the required items to trade, or your inventory is full"));
            return 0;
        }

        //  Trade usage limitation
        if (trade.isUsageLimited()) {
            Map<String, Object> playerTags = API.getPlayerTagsStatic(player.getUniqueId());
            if (playerTags != null && !playerTags.isEmpty()) {
                Map<String, Object> playerTradeTags = new HashMap<>();
                trade.getMaxTradeTagsNames().stream()
                        .filter(playerTags::containsKey)
                        .forEach(tag -> playerTradeTags.put(tag, playerTags.get(tag)));
                if (!playerTradeTags.isEmpty()) {
                    int tradeUses = Main.getStorage().getTradeUses(playerTradeTags, trade);
                    int maxDoAllowedTrades = trade.getMaxTradeUse() - tradeUses;
                    if (maxDoAllowedTrades < maxDoAbleTrades) {
                        Main.message(player, Main.getConfigs().getMessage(
                                        "messages.gui.chest-shop.messages.max-trade",
                                        "&cYou have reached the maximum number of uses for this trade(<trade_limit>).")
                                .replace("<trade_limit>", String.valueOf(tradeUses)));
                        if (maxDoAllowedTrades <= 0) return 0;
                        maxDoAbleTrades = maxDoAllowedTrades;
                    }
                }
            }
        }

        //  Execute the trade
        TradeUtils.executeTrade(player, trade, tradeItems, trade.getResultItem().clone(),
                maxDoAbleTrades, tradeMode);

        //  Call the TradeCompleteEvent
        for (int i = 0; i < maxDoAbleTrades; i++) {
            TradeCompleteEvent event = new TradeCompleteEvent(player, shop, trade);
            Main.getInstance().getServer().getPluginManager().callEvent(event);
        }
        return maxDoAbleTrades;
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

        int maxTrades = 1;
        if (unlimitedTrades) {
            for (InventoryStorage storage : inventories) {
                maxTrades += storage.size() * MAX_STACK_SIZE;
            }
        }

        return calculateMaxTrades(inventories, requiredItems, resultItem, maxTrades);
    }

    /**
    * Calculates the maximum number of possible trades by simulating real exchanges.
    * This method creates virtual copies of all inventories and progressively performs
    * trades (removing requiredItems and adding the resultItem) until no further trade can be made.
    *
    * @param inventories list of all available inventories
    * @param requiredItems items required per trade
    * @param resultItem item given per trade
    * @param maxTrades upper limit (for single trade mode)
    * @return maximum number of possible trades
    */
    private static int calculateMaxTrades(@NotNull List<InventoryStorage> inventories,
                                          @NotNull List<ItemStack> requiredItems,
                                          @NotNull ItemStack resultItem,
                                          int maxTrades) {
        List<Inventory> virtualStorages = new ArrayList<>();
        for (InventoryStorage storage : inventories) {
            virtualStorages.add(createVirtualInventory(storage));
        }

        int totalDoAbleTrades = 0;

        // Loop until we reach the maximum trades or can no longer trade
        while (totalDoAbleTrades < maxTrades) {
            // Phase 1 : Check and remove all requiredItems
            List<ItemStack> itemsToRemove = new ArrayList<>();
            for (ItemStack required : requiredItems) {
                ItemStack toRemove = required.clone();
                itemsToRemove.add(toRemove);
            }
            boolean canRemoveAll = true;
            for (ItemStack itemToRemove : itemsToRemove) {
                boolean found = false;
                for (Inventory vInv : virtualStorages) {
                    if (vInv.containsAtLeast(itemToRemove, itemToRemove.getAmount())) {
                        vInv.removeItem(itemToRemove.clone());
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    canRemoveAll = false;
                    break;
                }
            }
            if (!canRemoveAll) {
                // Cannot remove all required items, stop trading
                break;
            }

            // Phase 2 : Add the resultItem
            ItemStack toAdd = resultItem.clone();
            boolean added = false;
            for (Inventory vInv : virtualStorages) {
                Map<Integer, ItemStack> leftover = vInv.addItem(toAdd.clone());
                if (leftover.isEmpty()) {
                    added = true;
                    totalDoAbleTrades++;
                    break;
                }
            }
            if (!added) {
                // All inventories are full, cannot add the result item, stop trading
                break;
            }
        }

        return totalDoAbleTrades;
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
     * @param trade the trade being executed (used for money transactions if applicable)
     * @param requiredItems the list of items to remove from the player's inventories
     * @param resultItem the item to add to the player's inventories
     * @param tradeCount the number of trades to execute
     * @param tradeMode the trade mode determining which inventories to access
     */
    public static void executeTrade(@NotNull Player player,
                                    @NotNull Trade trade,
                                    @NotNull List<ItemStack> requiredItems,
                                    @NotNull ItemStack resultItem,
                                    int tradeCount,
                                    TradeMode tradeMode) {
        List<InventoryStorage> inventories = buildInventoryList(player, tradeMode);

        // First, remove all required items from inventories
        removeItemsFromInventories(inventories, requiredItems, tradeCount);

        // Then, add result items to inventories
        if (!trade.isMoneyTrade()) {
            addItemsToInventories(inventories, resultItem, tradeCount);
        } else {
            addMoneyToPlayer(player, trade, tradeCount);
        }
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
     * @param trades the number of trades (not the number of item stacks, but actual trade operations)
     */
    private static void removeItemsFromInventories(@NotNull List<InventoryStorage> inventories,
                                                   @NotNull List<ItemStack> requiredItems,
                                                   int trades) {
        // Process each required item type
        for (ItemStack requiredItem : requiredItems) {
            // Calculate total items to remove: amount per trade × number of trades
            int totalItemsToRemove = requiredItem.getAmount() * trades;
            int itemsRemoved = 0;

            // Iterate through all inventories
            for (InventoryStorage storage : inventories) {
                if (itemsRemoved >= totalItemsToRemove) break; // All items removed

                Inventory inventory = storage.inventory();
                ItemStack clonedItem = requiredItem.clone();

                // Keep removing items until we've removed enough or inventory runs out
                while (itemsRemoved < totalItemsToRemove && inventory.containsAtLeast(clonedItem, clonedItem.getAmount())) {
                    // Set the amount to remove for this iteration
                    int remainingToRemove = totalItemsToRemove - itemsRemoved;
                    clonedItem.setAmount(Math.min(clonedItem.getAmount(), remainingToRemove));

                    inventory.removeItem(clonedItem);
                    itemsRemoved += clonedItem.getAmount();

                    // Reset amount for next iteration
                    clonedItem.setAmount(requiredItem.getAmount());
                }

                // If this inventory is a shulker box, update its metadata
                updateShulkerIfNeeded(storage);
            }

            // Log warning if we couldn't remove all required items
            if (itemsRemoved < totalItemsToRemove) {
                Main.getMileLogger().warning("Not enough items to remove from the inventories. "
                        + "Expected to remove: " + totalItemsToRemove + ", but only removed: " + itemsRemoved);
            }
        }
    }

    /**
     * Adds result items to player inventories.
     * Items are distributed across all available inventories in order until the specified number of trades is reached.
     * This method uses Bukkit's addItem() which handles stack merging and slot management automatically.
     *
     * <p>Addition order (same as removal):</p>
     * <ol>
     *     <li>Main inventory</li>
     *     <li>Ender chest (if applicable)</li>
     *     <li>Shulker boxes in main inventory (if applicable)</li>
     *     <li>Shulker boxes in ender chest (if applicable)</li>
     * </ol>
     *
     * <p><b>Important:</b> Bukkit's addItem() method handles stack consolidation automatically.
     * We add items one by one and track successfully added items until we reach the trade count.</p>
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

            // Continue adding items until we reach the trade count or run out of space
            while (given < trades) {
                // Create a fresh clone for each addition
                ItemStack itemToAdd = resultItem.clone();

                // Bukkit's addItem() merges into existing stacks and creates new ones as needed
                Map<Integer, ItemStack> leftOver = inventory.addItem(itemToAdd);

                if (leftOver.isEmpty()) {
                    // Successfully added the entire item
                    given++;
                } else {
                    // Item couldn't be added - inventory full or other constraint
                    // Remove the partially added item from inventory if any (left over)
                    for (ItemStack leftoverItem : leftOver.values()) {
                        ItemStack toRemove = leftoverItem.clone();
                        toRemove.setAmount(resultItem.getAmount() - leftoverItem.getAmount());
                        inventory.removeItem(toRemove);
                    }

                    break;
                }
            }

            // If this inventory is a shulker box, update its metadata
            updateShulkerIfNeeded(storage);
        }
    }

    //  Handle money transactions
    private static void addMoneyToPlayer(@NotNull Player player, Trade trade, int trades) {
        try {
            Map<String, Object> playerTags = API.getPlayerTagsStatic(player.getUniqueId());
            if (playerTags != null && !playerTags.isEmpty()) {
                for (Map.Entry<String, Integer> entry : trade.getMoneyResult().entrySet()) {
                    double amount = entry.getValue() * trades * API.getPlayerModifierStatic(player.getUniqueId());
                    amount = Math.floor(amount);
                    Map<String, Object> moneyTags = new HashMap<>(playerTags);
                    moneyTags.put("currency", entry.getKey());
                    MileBanks.addMoneyByTags(
                            moneyTags,
                            (int) Math.floor(amount),
                            "trade_" + trade.getFirstItem().getType().name().toLowerCase(Locale.ROOT)
                    );
                }
            }
        } catch (RuntimeException exception) {
            Main.getMileLogger().warning("Failed to process " + trades + " money trade for player " + player.getName());
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
    @SuppressWarnings("deprecation")
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

        // Add shulker boxes from main inventory if trade mode allows it
        if (tradeMode == TradeMode.SHULKER || tradeMode == TradeMode.END_SHULKER) {
            inventories.addAll(getShulkersFromInventory(player.getInventory()));
        }

        // Add shulker boxes from ender chest if trade mode allows it (only for END_SHULKER mode)
        if (tradeMode == TradeMode.END_SHULKER) {
            inventories.addAll(getShulkersFromInventory(player.getEnderChest()));
        }

        // Add ender chest if trade mode allows it
        if (tradeMode == TradeMode.ENDER_CHEST || tradeMode == TradeMode.END_SHULKER) {
            inventories.add(new InventoryStorage(player.getEnderChest(), ENDER_CHEST_SIZE));
        }

        // Always include the main inventory
        inventories.add(new InventoryStorage(player.getInventory(), INVENTORY_SIZE));

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