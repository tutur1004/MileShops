package fr.milekat.shops.workers.gui;

import fr.milekat.shops.Main;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

@SuppressWarnings("UnstableApiUsage")
public class AnvilInput implements Listener {

    private final Player player;
    private final Consumer<@Nullable String> callback;

    private boolean consumed = false;

    public AnvilInput(
            @NotNull Player player,
            @NotNull String initialText,
            @NotNull Consumer<@Nullable String> callback
    ) {

        this.player = player;
        this.callback = callback;

        AnvilView view = MenuType.ANVIL.create(player);

        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        meta.displayName(Component.text(initialText));
        paper.setItemMeta(meta);
        view.getTopInventory().setItem(0, paper);
        view.getTopInventory().setItem(2, paper.clone());
        view.setRepairCost(0);
        view.setMaximumRepairCost(0);

        view.open();

        //  TODO: Ensure inventory is well opened

        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    @EventHandler
    public void onPrepare(@NotNull PrepareAnvilEvent event) {
        if (!event.getView().getPlayer().equals(player)) {
            return;
        }

        AnvilView anvilView = event.getView();

        anvilView.setRepairCost(0);
        anvilView.setMaximumRepairCost(0);

        if (event.getResult() == null) {

            ItemStack first = event.getInventory().getFirstItem();

            if (first != null) {
                event.setResult(first.clone());
            }
        }
    }

    @EventHandler
    public void onClick(@NotNull InventoryClickEvent event) {
        if (!event.getWhoClicked().equals(player)) {
            return;
        }
        if (!(event.getView() instanceof AnvilView anvilView)) {
            return;
        }
        if (event.getView().getTopInventory().getType() != InventoryType.ANVIL) {
            return;
        }

        event.setCancelled(true);

        if (event.getRawSlot() != 2) {
            return;
        }

        String text = anvilView.getRenameText();
        consumed = true;

        HandlerList.unregisterAll(this);

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            player.closeInventory();
            callback.accept(text);
        });
    }

    @EventHandler
    public void onClose(@NotNull InventoryCloseEvent event) {
        if (!event.getPlayer().equals(player)) {
            return;
        }
        if (event.getView().getTopInventory().getType() != InventoryType.ANVIL) {
            return;
        }

        HandlerList.unregisterAll(this);

        if (!consumed) {
            Bukkit.getScheduler().runTask(
                    Main.getInstance(),
                    () -> callback.accept(null)
            );
        }
    }
}