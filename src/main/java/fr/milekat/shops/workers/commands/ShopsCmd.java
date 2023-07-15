package fr.milekat.shops.workers.commands;

import dev.sergiferry.playernpc.api.NPC;
import dev.sergiferry.playernpc.api.NPCLib;
import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.ShopType;
import fr.milekat.shops.api.exeptions.CustomShopsApiUnavailable;
import fr.milekat.shops.api.exeptions.StorageException;
import fr.milekat.shops.storage.exeptions.StorageExecuteException;
import fr.milekat.shops.workers.gui.ChestShop;
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
        if (args.length==0) {

        } else if (args.length==1) {

        } else if (args.length==2) {
            if (args[0].equalsIgnoreCase("admin") && sender.hasPermission("shop.admin")) {
                if (args[1].equalsIgnoreCase("reload")) {
                    Main.info("Reloading shops...");
                    Main.message(sender, "&6Reloading shops...");
                    try {
                        List<Shop> shops = Main.getStorage().getCacheAllShops();
                        AtomicInteger loaded = new AtomicInteger();
                        shops.forEach(shop -> {
                            try {
                                Main.getStorage().getCacheTrades(shop.getUuid());
                                shop.getNpc().forceUpdate();
                                loaded.getAndIncrement();
                            } catch (StorageExecuteException exception) {
                                Main.message(sender, "&cError while trying to reload trades for shop " +
                                        shop.getName());
                                Main.warning("Error while trying to reload trades for shop " + shop.getName());
                            }
                        });
                        Main.info(loaded + " shops reloaded !");
                        Main.message(sender, "&2" + loaded + " shops reloaded !");
                    } catch (StorageExecuteException e) {
                        Main.warning(e.getMessage());
                        Main.stack(e.getStackTrace());
                        Main.message(sender, ChatColor.RED + "Storage error");
                        return true;
                    }
                }
            }

        } else if (args.length==3) {
            if (args[0].equalsIgnoreCase("admin") && sender.hasPermission("shop.admin")) {
                if (args[1].equalsIgnoreCase("open")) {
                    Shop shop;
                    try {
                        shop = Main.getStorage().getShop(args[2]);
                    } catch (StorageExecuteException e) {
                        Main.message(sender, ChatColor.RED + "Shop not found.");
                        return true;
                    }
                    if (shop.getType().equals(ShopType.INVENTORY)) {
                        try {
                            new ChestShop((Player) sender, shop, shop.getTrades());
                        } catch (CustomShopsApiUnavailable exception) {
                            Main.warning("Can't load shop trades from the Trade API.");
                        } catch (StorageException exception) {
                            Main.warning("Storage error while trying to fetch trades from shop.");
                        }
                    }
                }
            }
        } else if (args.length==4) {
            if (args[0].equalsIgnoreCase("admin") && sender.hasPermission("shop.admin")) {
                if (args[1].equalsIgnoreCase("create")) {
                    NPC.Global npc = null;
                    try {
                        npc = NPCLib.getInstance().generateGlobalNPC(Main.getInstance(),
                                UUID.randomUUID().toString(), ((Player) sender).getLocation());
                        Shop shop = new Shop(args[2], npc, ShopType.valueOf(args[3].toUpperCase(Locale.ROOT)));
                        Main.getStorage().asyncSaveShop(shop, sender, false);
                    } catch (IllegalArgumentException exception) {
                        if (npc!=null) {
                            Main.bukkitSync(npc::destroy);
                        }
                        Main.message(sender, "&cUnknown NPC type !");
                        Main.info("Creation cancelled, unknown NPC type " + args[3]);
                        Main.message(sender, "&cPlease use one of " + Arrays.toString(ShopType.values()));
                    } catch (Exception exception) {
                        if (npc!=null) {
                            npc.destroy();
                        }
                        Main.message(sender, "&cError while trying to create the shop");
                        Main.stack(exception.getStackTrace());
                    }
                } else if (args[1].equalsIgnoreCase("update")) {

                }
            }
        }
        return true;
    }
}
