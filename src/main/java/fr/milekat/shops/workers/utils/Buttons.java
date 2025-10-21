package fr.milekat.shops.workers.utils;

import fr.milekat.shops.Main;
import fr.mrmicky.fastinv.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum Buttons {
    //  TODO: Default lore ?
    //  Basics
    NEXT(new ItemBuilder(Material.ARROW)
            .name(Main.getConfigs().getMessage("messages.gui.commons.buttons.next.title", "Next page"))
            .addLore(Main.getConfigs().getMessages("messages.gui.commons.buttons.next.lore"))
            .build()),
    PREVIOUS(new ItemBuilder(Material.ARROW)
            .name(Main.getConfigs().getMessage("messages.gui.commons.buttons.previous.title",
                    "Previous page"))
            .addLore(Main.getConfigs().getMessages("messages.gui.commons.buttons.previous.lore"))
            .build()),
    EXIT(new ItemBuilder(Material.BARRIER)
            .name(Main.getConfigs().getMessage("messages.gui.commons.buttons.exit.title", "Close"))
            .addLore(Main.getConfigs().getMessages("messages.gui.commons.buttons.exit.lore"))
            .build()),

    //  ChestShop
    HEAD_LEFT(new ItemBuilder(HeadsUtils.ARROW_LEFT.getItem().clone())
            .meta(m -> m.setHideTooltip(true)).build()),
    CUSTOM_ARROW_LEFT(new ItemBuilder(Material.PAPER)
            .meta(itemMeta -> {
                itemMeta.setCustomModelData(Main.getConfigs().getInt(
                        "messages.gui.commons.buttons.arrow-left.custom-model-data", 1));
                itemMeta.setHideTooltip(true);
            })
            .build()),
    MODE_INVENTORY(new ItemBuilder(Material.CHEST)
            .name(Main.getConfigs().getMessage("messages.gui.chest-shop.buttons.mode-inventory.title",
                    "Mode Inventory"))
            .addLore(Main.getConfigs().getMessages("messages.gui.chest-shop.buttons.mode-inventory.lore"))
            .build()),
    MODE_ENDER_CHEST(new ItemBuilder(Material.ENDER_CHEST)
            .name(Main.getConfigs().getMessage("messages.gui.chest-shop.buttons.mode-ender.title",
                    "Mode EnderChest"))
            .addLore(Main.getConfigs().getMessages("messages.gui.chest-shop.buttons.mode-ender.lore"))
            .build()),
    MODE_SHULKER(new ItemBuilder(Material.SHULKER_BOX)
            .name(Main.getConfigs().getMessage("messages.gui.chest-shop.buttons.mode-shulker.title",
                    "Mode Shulker"))
            .addLore(Main.getConfigs().getMessages("messages.gui.chest-shop.buttons.mode-shulker.lore"))
            .build()),
    MODE_END_SHULKER(new ItemBuilder(Material.NETHER_STAR)
            .name(Main.getConfigs().getMessage("messages.gui.chest-shop.buttons.mode-end-shulker.title",
                    "God mode"))
            .addLore(Main.getConfigs().getMessages("messages.gui.chest-shop.buttons.mode-end-shulker.lore"))
            .build()),
    FROST_GLOW(new ItemBuilder(Material.PAPER)
            .meta(itemMeta -> {
                itemMeta.setCustomModelData(Main.getConfigs().getInt(
                        "messages.gui.commons.buttons.frost-glow.custom-model-data", 1));
                itemMeta.setHideTooltip(true);
            })
            .build()),

    //  Panes
    PANE_BLACK(new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build()),
    PANE_WHITE(new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE).name(" ").build());

    private final ItemStack item;

    Buttons(ItemStack item) {
        this.item = item;
    }

    public ItemStack get() {
        return item;
    }
}
