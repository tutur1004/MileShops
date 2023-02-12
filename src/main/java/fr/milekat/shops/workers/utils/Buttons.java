package fr.milekat.shops.workers.utils;

import fr.milekat.shops.Main;
import fr.mrmicky.fastinv.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum Buttons {
    //  TODO: Default lore ?
    //  Basics
    NEXT(new ItemBuilder(Material.ARROW)
            .name(Main.getConfigs().getMessage("messages.gui.commons.buttons.next.title",
                    "Next page"))
            .addLore(Main.getConfigs().getMessages("messages.gui.commons.buttons.next.lore"))
            .build()),
    PREVIOUS(new ItemBuilder(Material.ARROW)
            .name(Main.getConfigs().getMessage("messages.gui.commons.buttons.previous.title",
                    "Previous page"))
            .addLore(Main.getConfigs().getMessages("messages.gui.commons.buttons.previous.lore"))
            .build()),
    EXIT(new ItemBuilder(Material.BARRIER)
            .name(Main.getConfigs().getMessage("messages.gui.commons.buttons.exit.title",
                    "Close"))
            .addLore(Main.getConfigs().getMessages("messages.gui.commons.buttons.exit.lore"))
            .build()),

    //  ChestShop
    HEAD_LEFT(new ItemBuilder(HeadsUtils.ARROW_LEFT.getItem().clone()).name(" ").build()),
    MODE_CHEST(new ItemBuilder(Material.CHEST)
            .name(Main.getConfigs().getMessage("messages.gui.chest-shop.buttons.mode-chest.title",
                    "Mode chest"))
            .addLore(Main.getConfigs().getMessages("messages.gui.chest-shop.buttons.mode-chest.lore"))
            .build()),
    MODE_SHULKER(new ItemBuilder(Material.SHULKER_BOX)
            .name(Main.getConfigs().getMessage("messages.gui.chest-shop.buttons.mode-shulker.title",
                    "Mode chest"))
            .addLore(Main.getConfigs().getMessages("messages.gui.chest-shop.buttons.mode-shulker.lore"))
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
