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
    private static final int maxStackSize = 64;

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
     * @param tradeMode       the trade mode to use
     * @return the maximum amount of trades that can be done
     */
    public static int maxDoAbleTrades(@NotNull Player player,
                                      @NotNull List<ItemStack> requiredItems,
                                      @NotNull ItemStack resultItem,
                                      boolean unlimitedTrades,
                                      TradeMode tradeMode) {
        //  List of all inventories to check
        List<InventoryStorage> inventories = new ArrayList<>();
        inventories.add(new InventoryStorage(player.getInventory(), 4 * 9));
        if (tradeMode == TradeMode.ENDER_CHEST || tradeMode == TradeMode.END_SHULKER) {
            //  Add ender chest to the list
            inventories.add(new InventoryStorage(player.getEnderChest(), 3 * 9));
        }
        if (tradeMode == TradeMode.SHULKER || tradeMode == TradeMode.END_SHULKER) {
            //  Add shulkers from player inventory
            inventories.addAll(getShulkersFromInventory(player.getInventory()));
        }
        if (tradeMode == TradeMode.END_SHULKER) {
            //  Add shulkers from ender chest
            inventories.addAll(getShulkersFromInventory(player.getEnderChest()));
        }

        List<Inventory> virtualInventories = new ArrayList<>();
        for (InventoryStorage inventory : inventories) {
            int size = (inventory.inventory().getSize() == 43) ? 36 : inventory.inventory().getSize();
            Inventory virtualInventory = Bukkit.createInventory(null, size, UUID.randomUUID().toString());
            virtualInventory.setContents(inventory.inventory().getStorageContents());
            virtualInventories.add(virtualInventory);
        }

        //  Check how many items can be removed and added per inventories
        int doAbleTrade = 0;
        for (Inventory virtualInv : virtualInventories) {
            int maxInvItemAmount = virtualInv.getSize() * maxStackSize;
            for (int i = 0; i < maxInvItemAmount; i++) {
                if (requiredItems.stream().anyMatch(item -> !virtualInv.containsAtLeast(item, item.getAmount()))) {
                    Main.getMileLogger().debug("Inventory doesn't have one of the required items");
                    break;
                }
                //  Remove the required items
                requiredItems.forEach(virtualInv::removeItem);
                //  Try to add the result item
                Map<Integer, ItemStack> leftOver = virtualInv.addItem(resultItem.clone());
                if (leftOver.isEmpty()) {
                    //  Increment the doAbleTrade counter
                    doAbleTrade++;
                } else {
                    Main.getMileLogger().debug("Inventory doesn't have enough space for the result item");
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
     * @param tradeMode     the trade mode to use
     */
    public static void executeTrade(@NotNull Player player,
                                    @NotNull List<ItemStack> requiredItems,
                                    @NotNull ItemStack resultItem,
                                    int trades,
                                    TradeMode tradeMode) {
        //  List of all inventories to proceed
        List<InventoryStorage> inventories = new ArrayList<>();
        inventories.add(new InventoryStorage(player.getInventory(), 4 * 9));
        if (tradeMode == TradeMode.ENDER_CHEST || tradeMode == TradeMode.END_SHULKER) {
            //  Add ender chest to the list
            inventories.add(new InventoryStorage(player.getEnderChest(), 3 * 9));
        }
        if (tradeMode == TradeMode.SHULKER || tradeMode == TradeMode.END_SHULKER) {
            //  Add shulkers from player inventory
            inventories.addAll(getShulkersFromInventory(player.getInventory()));
        }
        if (tradeMode == TradeMode.END_SHULKER) {
            //  Add shulkers from ender chest
            inventories.addAll(getShulkersFromInventory(player.getEnderChest()));
        }

        //  Remove all required items from the inventories
        for (ItemStack item : requiredItems) {
            int toRemove = trades;
            for (InventoryStorage inventory : inventories) {
                int maxInvItemAmount = inventory.size() * maxStackSize;
                for (int i = 0; i < maxInvItemAmount; i++) {
                    if (toRemove <= 0) break;
                    if (inventory.inventory().containsAtLeast(item.clone(), item.getAmount())) {
                        inventory.inventory().removeItem(item.clone());
                        toRemove--;
                    } else {
                        break;
                    }
                }
                if (inventory.isShulkerBox()) {
                    assert inventory.blockStateMeta() != null;
                    assert inventory.shulkerBox() != null;
                    assert inventory.itemStack() != null;
                    inventory.blockStateMeta().setBlockState(inventory.shulkerBox());
                    inventory.itemStack().setItemMeta(inventory.blockStateMeta());
                }
            }
            if (toRemove > 0) {
                Main.getMileLogger().warning("Not enough items to remove from the inventories");
            }
        }

        //  Add the result items to the inventories
        int given = 0;
        for (InventoryStorage storage : inventories) {
            int maxInvItemAmount = storage.size() * maxStackSize;
            for (int i = 0; i < maxInvItemAmount; i++) {
                if (given >= trades) break;
                Map<Integer, ItemStack> leftOver = storage.inventory().addItem(resultItem.clone());
                if (leftOver.isEmpty()) {
                    given++;
                } else {
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
        }
    }

    public static @NotNull List<InventoryStorage> getShulkersFromInventory(@NotNull Inventory inventory) {
        List<InventoryStorage> shulkers = new ArrayList<>();
        for (ItemStack loopItem : inventory.getStorageContents()) {
            if (loopItem != null) {
                if (loopItem.getItemMeta() instanceof BlockStateMeta stateMeta) {
                    if (stateMeta.getBlockState() instanceof ShulkerBox shulkerBox) {
                        shulkers.add(new InventoryStorage(shulkerBox.getInventory(),
                                3 * 9, loopItem, stateMeta, shulkerBox));
                    }
                }
            }
        }
        return shulkers;
    }
}
