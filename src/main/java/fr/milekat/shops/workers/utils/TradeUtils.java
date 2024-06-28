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
     * Method to check if the player inventory can hold the result item if traded items are removed
     *
     * @return the {@link Inventory} that can hold the item, or null if the player inventory can't contain the item
     */
    public static int maxDoAbleTrades(@NotNull Player player,
                                      @NotNull List<ItemStack> requiredItems,
                                      @NotNull ItemStack resultItem,
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
            Main.message(player, "Creating virtual inventory of size " + size);
            Inventory virtualInventory = Bukkit.createInventory(null, size, UUID.randomUUID().toString());
            virtualInventory.setContents(inventory.getStorageContents());
            virtualInventories.add(virtualInventory);
        }

        Main.message(player, "Number of inventories to check: " + inventories.size());

        //  Output variables
        int minRequiredHold = 0;
        int resultHoldAble = 0;

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
            requiredItemLoop.setAmount(1);

            //  Iterate over all the inventories
            for (Inventory virtualInventory : virtualInventories) {
                while (true) {
                    Main.message(player, "Checking if loop var is higher than required amount");
                    if (itemHeld >= requiredItems.get(i).getAmount()) {
                        Main.message(player, "Loop var is higher than required amount");
                        requiredHeld.set(i, requiredHeld.get(i) + 1);
                        itemHeld = 0;
                    }

                    //  Ensure the item is present in this inventory
                    Main.message(player, "Checking if the inventory contains the required item");
                    if (!virtualInventory.contains(requiredItemLoop.getType())) break;

                    //  Remove the item from the inventory
                    Main.message(player, "Removing the required item from the inventory");
                    HashMap<Integer, ItemStack> leftOver = virtualInventory.removeItem(requiredItemLoop);

                    if (leftOver.isEmpty()) {
                        itemHeld++;
                    } else {
                        Main.message(player, "Inventory doesn't have enough of the required item");
                        break;
                    }
                }
            }

            //  Get the minimum amount of items held
            //  TODO: Be careful, it can remove more than trade-able items, result space should be impacted
            minRequiredHold = requiredHeld.stream().min(Integer::compareTo).orElse(0);
            Main.message(player, "Minimum required hold: " + minRequiredHold);

            for (int j = 0; j < minRequiredHold; j++) {
                for (Inventory virtualInventory : virtualInventories) {
                    Map<Integer, ItemStack> leftOver = virtualInventory.addItem(resultItem.clone());
                    if (leftOver.isEmpty()) {
                        resultHoldAble++;
                    } else {
                        Main.message(player, "Inventory doesn't have enough space for the result item");
                        break;
                    }
                }
            }
        }

        //  Return the minimum between the required item and the result items
        return Math.min(minRequiredHold, resultHoldAble);
    }

    public static void executeTrade(@NotNull Player player,
                                    @NotNull List<ItemStack> tradedItems,
                                    @NotNull ItemStack resultItem,
                                    int trades,
                                    boolean shulkerMode) {
        //  List of all inventories to proceed
        List<Inventory> inventories = new ArrayList<>();
        inventories.add(player.getInventory());
        if (shulkerMode) {
            //  Add shulkers from player inventory and ender chest to the list
            inventories.addAll(getShulkersFromInventory(player.getInventory()));
            inventories.addAll(getShulkersFromInventory(player.getEnderChest()));
        }
        //  List of items to remove from the inventories
        Map<ItemStack, Integer> toRemove = new HashMap<>();
        tradedItems.forEach(loopItem -> {
            ItemStack itemStack = loopItem.clone();
            itemStack.setAmount(1);
            toRemove.put(itemStack, loopItem.getAmount() * trades);
        });
        //  Remove the traded items from the inventories
        for (Inventory inventory : inventories) {
            new HashMap<>(toRemove).forEach((item, amount) -> {
                int reamingAmount = amount;
                while (true) {
                    ItemStack itemToRemove = item.clone();
                    itemToRemove.setAmount(Math.min(reamingAmount, item.getMaxStackSize()));
                    HashMap<Integer, ItemStack> notRemove = inventory.removeItem(item);
                    if (notRemove.isEmpty()) {
                        reamingAmount -= itemToRemove.getAmount();
                        if (reamingAmount == 0) break;
                    } else {
                        reamingAmount -= (itemToRemove.getAmount() - notRemove.get(0).getAmount());
                        break;
                    }
                }
                if (reamingAmount == 0) toRemove.remove(item);
                else toRemove.put(item, reamingAmount);
            });

            if (toRemove.isEmpty()) break;
        }

        //  Add the result item
        int resultToAdd = resultItem.getAmount() * trades;
        for (Inventory inventory : inventories) {
            while (true) {
                if (resultToAdd <= 0) return;
                ItemStack resultItemClone = resultItem.clone();
                resultItemClone.setAmount(Math.min(resultToAdd, resultItem.getMaxStackSize()));
                HashMap<Integer, ItemStack> leftOver = inventory.addItem(resultItemClone);
                if (leftOver.isEmpty()) {
                    resultToAdd -= resultItemClone.getAmount();
                } else {
                    resultToAdd -= resultItemClone.getAmount() - leftOver.get(0).getAmount();
                    break;
                }
            }
        }
    }

    /**
     * Method to check if the inventory can hold the result item if traded items are removed
     *
     * @return the {@link Inventory} that can hold the item, or null if the player inventory can't contain the item
     */
//    public static int maxDoAbleTrades(Player player,
//                               @NotNull Inventory inventory,
//                               @NotNull List<ItemStack> tradedItems,
//                               @NotNull ItemStack resultItem) {
//        //  Get the real size of the inventory
//        int size = (inventory.getSize() == 41) ? 36 : inventory.getSize();
//        //  Get the highest value from getMaxStackSize() of traded items
//        int maxStackSize = tradedItems.stream().mapToInt(ItemStack::getMaxStackSize).max().orElse(64);
//        int maxTheoreticalTrades = size * maxStackSize;
//        int maxDoAbleTrades = 0;
//
//        Inventory virtualInventory = Bukkit.createInventory(null, size, UUID.randomUUID().toString());
//        virtualInventory.setContents(inventory.getStorageContents());
//
//
//
//        Main.message(player, "Creating virtual inventory or size " + size);
//        Inventory virtualInventory = Bukkit.createInventory(null, size,
//                UUID.randomUUID().toString());
//        Main.message(player, "Setting virtual inventory contents");
//        virtualInventory.setContents(inventory.getStorageContents());
//        Main.message(player, "Checking if the inventory has the required items");
//        boolean hasItems = tradedItems.stream().allMatch(item -> virtualInventory.removeItem(item).isEmpty());
//        if (hasItems) {
//            Main.message(player, "Checking if the inventory has space for the result item");
//            HashMap<Integer, ItemStack> leftOver = virtualInventory.addItem(resultItem.clone());
//            if (leftOver.isEmpty()) {
//                Main.message(player, "Inventory has space for the result item");
//                return inventory;
//            }
//            Main.message(player, "Inventory doesn't have space for the result item");
//        }
//        Main.message(player, "Inventory doesn't have the required items " +
//                "(or doesn't have space for the result item)");
//        return null;
//    }

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
