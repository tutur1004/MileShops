package fr.milekat.shops.workers.utils;

import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class TradeUtils {
    public static @NotNull String getMaterial(@NotNull Material material) {
        return Main.getConfigs().getString("materials-font." + material, material.toString());
    }

    /**
     * Method to replace all trade placeholders
     * @return the message with all trade placeholders replaced
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
     * Method to calculate the maximum amount of trades that can be done
     *
     * @param player            the player who will do the trades (used to get the player's inventory and ender chest)
     * @param requiredItems     the list of items required for the trade
     * @param resultItem        the item that will be given to the player
     * @param unlimitedTrades   if it needs to check for unlimited trades
     * @param shulkerMode       if it needs to check for shulker boxes
     * @return                  the maximum amount of trades that can be done
     */
    public static int maxDoAbleTrades(@NotNull Player player,
                                      @NotNull List<ItemStack> requiredItems,
                                      @NotNull ItemStack resultItem,
                                      boolean unlimitedTrades,
                                      boolean shulkerMode) {
        //  List of all inventories to check
        List<Inventory> inventories = new ArrayList<>();
        inventories.add(player.getInventory());
        if (shulkerMode) {
            //  Add shulkers from player inventory and ender chest to the list
            inventories.addAll(getShulkersFromInventory(player.getInventory()));
            inventories.addAll(getShulkersFromInventory(player.getEnderChest()));
        }

        List<Inventory> virtualInventories = new ArrayList<>();
        for (Inventory inventory : inventories) {
            int size = (inventory.getSize() == 41) ? 36 : inventory.getSize();
            Inventory virtualInventory = Bukkit.createInventory(null, size, UUID.randomUUID().toString());
            virtualInventory.setContents(inventory.getStorageContents());
            virtualInventories.add(virtualInventory);
        }

        //  Check how many items can be removed for each required item (In required stacks)
        List<Integer> requiredHeld = new LinkedList<>();
        //  Iterate over the required items
        for (int i = 0; i < requiredItems.size(); i++) {
            //  Load the required item
            requiredHeld.add(i, 0);
            int itemHeld = 0;
            if (requiredItems.get(i) == null) continue;
            ItemStack requiredItemLoop = requiredItems.get(i).clone();
            //  TODO: Here add tag support
            if (requiredItemLoop.getType() == Material.AIR || requiredItemLoop.getAmount() < 1) continue;

            //  Iterate over all the inventories
            for (Inventory loopInv : virtualInventories) {
                Inventory virtualInventory = Bukkit.createInventory(null, loopInv.getSize(),
                        UUID.randomUUID().toString());
                virtualInventory.setContents(loopInv.getContents());
                while (true) {
                    if (itemHeld >= requiredItems.get(i).getAmount()) {
                        if (!unlimitedTrades) {
                            requiredHeld.set(i, 1);
                            break;
                        }
                        requiredHeld.set(i, requiredHeld.get(i) + 1);
                        itemHeld = 0;
                    }

                    //  Ensure the item is present in this inventory
                    if (!virtualInventory.contains(requiredItemLoop.getType())) break;

                    //  Remove the item from the inventory and increment the itemHeld
                    int removed = getStackRemoved(virtualInventory, requiredItemLoop);
                    itemHeld += removed;
                    if (removed != requiredItemLoop.getAmount()) break;
                }
            }
        }

        //  Output variables
        //  TODO: Be careful, it can remove more than trade-able items, result space should be impacted
        int minRequiredHold = requiredHeld.stream().min(Integer::compareTo).orElse(0);
        minRequiredHold -= requiredItems.stream().mapToInt(ItemStack::getAmount).max().orElse(resultItem.getMaxStackSize());
        int resultHoldAble = 0;

        for (int j = 0; j < minRequiredHold; j++) {
            for (Inventory virtualInventory : virtualInventories) {
                for (int k = 0; k < minRequiredHold; k++) {
                    requiredItems.forEach(item -> getStackRemoved(virtualInventory, item));
                }
                Map<Integer, ItemStack> leftOver = virtualInventory.addItem(resultItem.clone());
                if (leftOver.isEmpty()) {
                    resultHoldAble++;
                } else break;
            }
        }

        //  Return the minimum between the required item and the result items
        return Math.min(minRequiredHold, resultHoldAble);
    }

    /**
     * Method to execute a trade
     *
     * @param player        the player who will do the trade
     * @param requiredItems the list of items required for the trade
     * @param resultItem    the item that will be given to the player
     * @param trades        the amount of trades to do
     * @param shulkerMode   if it needs to include player's shulker boxes
     */
    public static void executeTrade(@NotNull Player player,
                                    @NotNull List<ItemStack> requiredItems,
                                    @NotNull ItemStack resultItem,
                                    int trades,
                                    boolean shulkerMode) {
        //  TODO: Fully rework this part
        Main.message(player, "Number of trades: " + trades);
        //  List of all inventories to proceed
        List<Inventory> inventories = new ArrayList<>();
        inventories.add(player.getInventory());
        if (shulkerMode) {
            //  Add shulkers from player inventory and ender chest to the list
            inventories.addAll(getShulkersFromInventory(player.getInventory()));
            inventories.addAll(getShulkersFromInventory(player.getEnderChest()));
        }

        //  List all items to remove and add by stacks (Performances improvement)
        List<ItemStack> requestItemsToRemove = new ArrayList<>();
        requiredItems.forEach(item -> requestItemsToRemove.addAll(getAllByStacks(item, trades)));
        List<ItemStack> resultItemsToAdd = getAllByStacks(resultItem, trades);

        //  Add and remove the items from the inventories
        inventories.forEach(inventory -> {
            //  Remove the traded items
            for (ItemStack item : new ArrayList<>(requestItemsToRemove)) {
                HashMap<Integer, ItemStack> leftOver = inventory.removeItem(item);
                if (leftOver.isEmpty()) {
                    requestItemsToRemove.remove(item);
                } else if (inventory.contains(item.getType())) {
                    //  TODO: Remove as many items as is still possible to remove from the inventory -- TEST
                    int removed = item.getAmount();
                    ItemStack lastFewSpace = item.clone();
                    lastFewSpace.setAmount(1);
                    while (true) {
                        leftOver = inventory.removeItem(lastFewSpace);
                        if (leftOver.isEmpty()) {
                            removed--;
                            if (removed <= 0) {
                                requestItemsToRemove.remove(item);
                                break;
                            }
                        } else {
                            requestItemsToRemove.remove(item);
                            ItemStack remainingItems = item.clone();
                            remainingItems.setAmount(removed);
                            requestItemsToRemove.add(remainingItems);
                            break;
                        }
                    }
                } else break;
            }
            //  Add the result items
            for (ItemStack item : new ArrayList<>(resultItemsToAdd)) {
                HashMap<Integer, ItemStack> leftOver = inventory.addItem(item);
                if (leftOver.isEmpty()) {
                    resultItemsToAdd.remove(item);
                } else {
                    //  TODO: Add as many items as is still possible to fill the inventory -- TEST
                    int toAdd = item.getAmount();
                    ItemStack lastFewSpace = item.clone();
                    lastFewSpace.setAmount(1);
                    while (true) {
                        leftOver = inventory.addItem(lastFewSpace);
                        if (leftOver.isEmpty()) {
                            toAdd--;
                            if (toAdd <= 0) {
                                resultItemsToAdd.remove(item);
                                break;
                            }
                        } else {
                            resultItemsToAdd.remove(item);
                            ItemStack remainingItems = item.clone();
                            remainingItems.setAmount(toAdd);
                            resultItemsToAdd.add(remainingItems);
                            break;
                        }
                    }
                    break;
                }
            }
        });
    }

    public static int getStackRemoved(@NotNull Inventory inventory, @NotNull ItemStack itemToRemove) {
        ItemStack item = itemToRemove.clone();
        HashMap<Integer, ItemStack> leftOver = inventory.removeItem(item);
        if (leftOver.isEmpty()) {
            return item.getAmount();
        } else {
            item.setAmount(1);
            int removed = 0;
            while (true) {
                leftOver = inventory.removeItem(item);
                if (leftOver.isEmpty()) {
                    removed++;
                } else {
                    break;
                }
            }
            return removed;
        }
    }

    public static @NotNull List<ItemStack> getAllByStacks(@NotNull ItemStack item, int trades) {
        List<ItemStack> output = new ArrayList<>();
        int totalAmount = item.getAmount() * trades;
        while (true) {
            if (totalAmount <= 0) break;
            if (totalAmount >= item.getMaxStackSize()) {
                ItemStack itemToAdd = item.clone();
                itemToAdd.setAmount(item.getMaxStackSize());
                output.add(itemToAdd);
                totalAmount -= item.getMaxStackSize();
            } else {
                ItemStack itemToAdd = item.clone();
                itemToAdd.setAmount(totalAmount);
                output.add(itemToAdd);
                break;
            }
        }
        return output;
    }

    public static @NotNull List<Inventory> getShulkersFromInventory(@NotNull Inventory inventory) {
        List<Inventory> shulkers = new ArrayList<>();
        for (ItemStack loopItem : inventory.getStorageContents()) {
            if (loopItem != null) {
                if (loopItem.getItemMeta() instanceof BlockStateMeta im) {
                    if (im.getBlockState() instanceof ShulkerBox shulkerBox) {
                        shulkers.add(shulkerBox.getInventory());
                    }
                }
            }
        }
        return shulkers;
    }
}
