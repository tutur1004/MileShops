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

public class TradeUtils {
    public static @NotNull String getMaterial(@NotNull Material material) {
        return Main.getConfigs().getString("materials-font." + material, material.toString());
    }

    public static @Nullable Tag<Material> getMaterialTag(@NotNull String tagName) {
        Tag<Material> found = Bukkit.getTag(Tag.REGISTRY_BLOCKS, NamespacedKey.minecraft(tagName), Material.class);
        if (found == null) {
            found = Bukkit.getTag(Tag.REGISTRY_ITEMS, NamespacedKey.minecraft(tagName), Material.class);
        }
        return found;
    }

    /**
     * Method to replace all trade placeholders
     *
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
     * @param player          the player who will do the trades (used to get the player's inventory and ender chest)
     * @param requiredItems   the list of items required for the trade
     * @param resultItem      the item that will be given to the player
     * @param unlimitedTrades if it needs to check for unlimited trades
     * @param shulkerMode     if it needs to check for shulker boxes
     * @return the maximum amount of trades that can be done
     */
    public static int maxDoAbleTrades(@NotNull Player player,
                                      @NotNull List<ItemStack> requiredItems,
                                      @NotNull ItemStack resultItem,
                                      boolean unlimitedTrades,
                                      boolean shulkerMode) {
        //  List of all inventories to check
        List<InventoryStorage> inventories = new ArrayList<>();
        inventories.add(new InventoryStorage(player.getInventory()));
        if (shulkerMode) {
            //  Add shulkers from player inventory and ender chest to the list
            inventories.addAll(getShulkersFromInventory(player.getInventory()));
            inventories.addAll(getShulkersFromInventory(player.getEnderChest()));
        }

        List<Inventory> virtualInventories = new ArrayList<>();
        for (InventoryStorage inventory : inventories) {
            int size = (inventory.inventory().getSize() == 41) ? 36 : inventory.inventory().getSize();
            Inventory virtualInventory = Bukkit.createInventory(null, size, UUID.randomUUID().toString());
            virtualInventory.setContents(inventory.inventory().getStorageContents());
            virtualInventories.add(virtualInventory);
        }

        //  Check how many items can be removed and added per inventories
        int doAbleTrade = 0;
        for (Inventory virtualInv : virtualInventories) {
            while (true) {
                if (requiredItems.stream().anyMatch(item -> getStackRemoved(virtualInv, item) != item.getAmount())) {
                    break;
                }
                Map<Integer, ItemStack> leftOver = virtualInv.addItem(resultItem.clone());
                if (leftOver.isEmpty()) {
                    doAbleTrade++;
                } else {
                    break;
                }
                if (!unlimitedTrades) break;
            }
        }
        return doAbleTrade;
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
        //  List of all inventories to proceed
        List<InventoryStorage> inventories = new ArrayList<>();
        inventories.add(new InventoryStorage(player.getInventory()));
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
        inventories.forEach(storage -> {
            Inventory inventory = storage.inventory();
            //  Remove the traded items
            for (ItemStack item : new ArrayList<>(requestItemsToRemove)) {
                Map<Integer, ItemStack> leftOver = inventory.removeItem(item);
                if (leftOver.isEmpty()) {
                    requestItemsToRemove.remove(item);
                } else if (inventory.contains(item.getType())) {
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
                Map<Integer, ItemStack> leftOver = inventory.addItem(item);
                if (leftOver.isEmpty()) {
                    resultItemsToAdd.remove(item);
                } else {
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
            if (storage.isShulkerBox()) {
                assert storage.blockStateMeta() != null;
                assert storage.shulkerBox() != null;
                assert storage.itemStack() != null;
                storage.blockStateMeta().setBlockState(storage.shulkerBox());
                storage.itemStack().setItemMeta(storage.blockStateMeta());
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

    public static @NotNull List<InventoryStorage> getShulkersFromInventory(@NotNull Inventory inventory) {
        List<InventoryStorage> shulkers = new ArrayList<>();
        for (ItemStack loopItem : inventory.getStorageContents()) {
            if (loopItem != null) {
                if (loopItem.getItemMeta() instanceof BlockStateMeta stateMeta) {
                    if (stateMeta.getBlockState() instanceof ShulkerBox shulkerBox) {
                        shulkers.add(new InventoryStorage(shulkerBox.getInventory(), loopItem, stateMeta, shulkerBox));
                    }
                }
            }
        }
        return shulkers;
    }
}
