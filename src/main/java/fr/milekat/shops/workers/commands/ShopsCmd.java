package fr.milekat.shops.workers.commands;

import fr.milekat.milenpc.api.classes.NPC;
import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.ShopType;
import fr.milekat.shops.api.exceptions.ApiUnavailable;
import fr.milekat.shops.api.exceptions.StorageException;
import fr.milekat.shops.workers.gui.InventorySmall;
import fr.milekat.shops.workers.utils.NPCUtils;
import fr.milekat.utils.storage.exceptions.StorageExecuteException;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ShopsCmd implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length==1) {
            if (args[0].equalsIgnoreCase("reload") && sender.hasPermission("shop.reload")) {
                Main.getMileLogger().info("Reloading shops...");
                Main.message(sender, "&6Reloading shops...");
                try {
                    List<Shop> shops = Main.getStorage().getCacheAllShops();
                    AtomicInteger loaded = new AtomicInteger();
                    shops.forEach(shop -> {
                        try {
                            Main.getStorage().getCacheTrades(shop.getUuid());
                            loaded.getAndIncrement();
                        } catch (StorageExecuteException | NullPointerException exception) {
                            Main.message(sender, "&cError while trying to reload trades for shop " +
                                    shop.getName());
                            Main.getMileLogger().warning("Error while trying to reload trades for shop " + shop.getName());
                        }
                    });
                    Main.getMileLogger().info(loaded + " shops reloaded !");
                    Main.message(sender, "&2" + loaded + " shops reloaded !");
                } catch (StorageExecuteException e) {
                    Main.getMileLogger().warning(e.getMessage());
                    Main.getMileLogger().stack(e.getStackTrace());
                    Main.message(sender, "&cStorage error");
                }
                return true;
            }

        } else if (args.length==2) {
            if (args[0].equalsIgnoreCase("open") && sender.hasPermission("shop.open")) {
                Shop shop;
                try {
                    shop = Main.getStorage().getShop(args[1]);
                } catch (StorageExecuteException e) {
                    Main.message(sender, ChatColor.RED + "Shop not found.");
                    return true;
                }
                if (shop.getType().equals(ShopType.INVENTORY_SMALL) ||
                        shop.getType().equals(ShopType.INVENTORY_LARGE) ||
                        shop.getType().equals(ShopType.INVENTORY_LARGE_NO_FILL)) {
                    try {
                        new InventorySmall((Player) sender, shop, shop.getTrades());
                    } catch (ApiUnavailable exception) {
                        Main.getMileLogger().warning("Can't load shop trades from the Trade API.");
                    } catch (StorageException exception) {
                        Main.getMileLogger().warning("Storage error while trying to fetch trades from shop.");
                    }
                }
            }
            if (args[0].equalsIgnoreCase("remove") && sender.hasPermission("shop.remove")) {
                NPC npc = Main.getNpc(UUID.fromString(args[1]));
                if (npc==null) {
                    Main.message(sender, "&cNPC not found.");
                    return true;
                }

                try {
                    Shop shop = Main.getStorage().getCacheShop(npc.getUuid());
                    if (shop==null) {
                        Main.message(sender, "&cShop not found.");
                        return true;
                    }

                    Main.getStorage().asyncDeleteShop(shop, sender);
                    npc.remove();
                    Main.message(sender, "&2Shop removed !");
                } catch (StorageExecuteException e) {
                    Main.getMileLogger().warning(e.getMessage());
                    Main.getMileLogger().stack(e.getStackTrace());
                    Main.message(sender, "&cStorage error");
                }
            }

        } else if (args.length==3) {
            if (args[0].equalsIgnoreCase("create") && sender.hasPermission("shop.create")) {
                UUID npcUuid = UUID.randomUUID();
                try {
                    NPC npc = NPCUtils.create(npcUuid, args[1], ((Player) sender).getLocation());
                    Shop shop = new Shop(npcUuid, args[1], npc, ShopType.valueOf(args[2].toUpperCase(Locale.ROOT)));
                    Main.getStorage().asyncSaveShop(shop, true, player);
                } catch (IllegalArgumentException exception) {
                    NPCUtils.destroy(npcUuid);
                    Main.message(sender, "&cUnknown NPC type !");
                    Main.getMileLogger().info("Creation cancelled, unknown NPC type " + args[2]);
                    Main.message(sender, "&cPlease use one of " + Arrays.toString(ShopType.values()));
                } catch (Exception exception) {
                    NPCUtils.destroy(npcUuid);
                    Main.message(sender, "&cError while trying to create the shop");
                    Main.getMileLogger().stack(exception.getStackTrace());
                }
            }
        }
        return true;
    }
}
