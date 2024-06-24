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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

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
    public static @Nullable Inventory canTrade(@NotNull Player player,
                                               @NotNull List<ItemStack> tradedItems,
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
        //  Check if any inventory has the required items and space for the result item
        return inventories.stream()
                .filter(shulker -> canTrade(player, shulker, tradedItems, resultItem) != null)
                .findAny().orElse(null);
    }

    /**
     * Method to check if the inventory can hold the result item if traded items are removed
     *
     * @return the {@link Inventory} that can hold the item, or null if the player inventory can't contain the item
     */
    public static @Nullable Inventory canTrade(Player player,
                                               @NotNull Inventory inventory,
                                               @NotNull List<ItemStack> tradedItems,
                                               @NotNull ItemStack resultItem) {
        int size = (inventory.getSize() == 41) ? 36 : inventory.getSize();
        Main.message(player, "Creating virtual inventory or size " + size);
        Inventory virtualInventory = Bukkit.createInventory(null, size,
                UUID.randomUUID().toString());
        Main.message(player, "Setting virtual inventory contents");
        virtualInventory.setContents(inventory.getStorageContents());
        Main.message(player, "Checking if the inventory has the required items");
        boolean hasItems = tradedItems.stream().allMatch(item -> virtualInventory.removeItem(item).isEmpty());
        if (hasItems) {
            Main.message(player, "Checking if the inventory has space for the result item");
            HashMap<Integer, ItemStack> leftOver = virtualInventory.addItem(resultItem.clone());
            if (leftOver.isEmpty()) {
                Main.message(player, "Inventory has space for the result item");
                return inventory;
            }
            Main.message(player, "Inventory doesn't have space for the result item");
        }
        Main.message(player, "Inventory doesn't have the required items " +
                "(or doesn't have space for the result item)");
        return null;
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
