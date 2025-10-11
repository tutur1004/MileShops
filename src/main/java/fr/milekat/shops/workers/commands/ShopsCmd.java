package fr.milekat.shops.workers.commands;

import fr.milekat.milenpc.api.classes.NPC;
import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.ShopType;
import fr.milekat.shops.workers.utils.NPCUtils;
import fr.milekat.shops.workers.utils.ShopActions;
import fr.milekat.utils.McTools;
import fr.milekat.utils.storage.exceptions.StorageExecuteException;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ShopsCmd implements TabExecutor {
    private final List<String> shopTypes = Arrays
            .stream(ShopType.values())
            .map(Enum::name)
            .map(String::toLowerCase)
            .toList();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            Main.getMileLogger().warning("Only players can use this command.");
            return true;
        }
        if (args.length == 1) {
            //  Reload shops
            if (args[0].equalsIgnoreCase("reload") && player.hasPermission("shops.reload")) {
                Main.getMileLogger().info("Reloading shops...");
                Main.message(player, "&6Reloading shops...");
                int loaded = Main.reloadShops();
                Main.message(player, "&2" + loaded + " shops reloaded !");
            }
            //  List shops
            if (args[0].equalsIgnoreCase("list") && player.hasPermission("shops.list")) {
                try {
                    List<Shop> shops = Main.getStorage().getAllShops();
                    if (shops.isEmpty()) {
                        Main.message(player, "&cNo shop found.");
                        return true;
                    }
                    Main.message(player, "&6--- Shops list (" + shops.size() + ") ---");
                    for (Shop shop : shops) {
                        Main.message(player, "&e- " + shop.getName() + " &7(" +
                                shop.getType().name().toLowerCase() + ")");
                    }
                } catch (StorageExecuteException exception) {
                    Main.getMileLogger().warning("Storage error while trying to fetch shops list.");
                    Main.message(player, "&cStorage error while trying to fetch shops list.");
                }
            }
            return true;

        } else if (args.length == 2) {
            //  Open shop gui
            if (args[0].equalsIgnoreCase("open") && player.hasPermission("shops.open")) {
                try {
                    Shop shop = Main.getStorage().getCacheShop(args[1]);
                    if (shop == null) {
                        Main.message(player, "&cShop not found.");
                        return true;
                    }
                    ShopActions.openShop(player, shop);

                } catch (StorageExecuteException e) {
                    Main.message(player, "&cStorage error");
                }

                return true;
            }

            //  Edit shop (admin)
            if (args[0].equalsIgnoreCase("edit") && player.hasPermission("shops.edit")) {
                try {
                    Shop shop = Main.getStorage().getCacheShop(args[1]);
                    if (shop == null) {
                        Main.message(player, "&cShop not found.");
                        return true;
                    }
                    ShopActions.openAdminShop(player, shop);
                } catch (StorageExecuteException e) {
                    Main.message(player, "&cStorage error");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("remove") && player.hasPermission("shops.remove")) {
                try {
                    Shop shop = Main.getStorage().getCacheShop(args[1]);
                    if (shop == null) {
                        Main.message(player, "&cShop not found.");
                        return true;
                    }

                    Main.getStorage().asyncDeleteShop(shop, player);

                    NPC npc = shop.getNpc();
                    if (npc != null) {
                        npc.remove();
                    } else {
                        Main.message(player, "&cNPC not found.");
                    }

                    Main.message(player, "&2Shop removed !");
                } catch (StorageExecuteException e) {
                    Main.getMileLogger().warning(e.getMessage());
                    Main.getMileLogger().stack(e.getStackTrace());
                    Main.message(player, "&cStorage error");
                }

                return true;
            }

        } else if (args.length == 3) {

            if (args[0].equalsIgnoreCase("create") && player.hasPermission("shops.create")) {

                if (!shopTypes.contains(args[2].toLowerCase(Locale.ROOT))) {
                    Main.message(player, "&cUnknown shape !");
                    Main.message(player, "&cPlease use one of " + shopTypes);
                    return true;
                }

                UUID shopUuid = UUID.randomUUID();
                try {
                    NPC npc = null;
                    if (Main.IS_NPC_LIB_LOADED) {
                        npc = NPCUtils.create(shopUuid, args[1], player.getLocation());
                    }
                    Shop shop = new Shop(shopUuid, args[1], npc, ShopType.valueOf(args[2].toUpperCase(Locale.ROOT)));
                    Main.getStorage().asyncSaveShop(shop, true, player);
                } catch (IllegalArgumentException exception) {
                    NPCUtils.destroy(shopUuid);
                    Main.message(player, "&cUnknown NPC type !");
                    Main.getMileLogger().info("Creation cancelled, unknown NPC type " + args[2]);
                    Main.message(player, "&cPlease use one of " + Arrays.toString(ShopType.values()));
                } catch (Exception exception) {
                    NPCUtils.destroy(shopUuid);
                    Main.message(player, "&cError while trying to create the shop");
                    Main.getMileLogger().stack(exception.getStackTrace());
                }

                return true;
            }

        }
        sendHelp(player, label);
        return true;
    }

    private void sendHelp(@NotNull CommandSender sender, String lbl) {
        Main.message(sender, "&6/" + lbl + " create <name> <type>");
        Main.message(sender, "&6/" + lbl + " remove <name>");
        Main.message(sender, "&6/" + lbl + " list");
        Main.message(sender, "&6/" + lbl + " open <name>");
        Main.message(sender, "&6/" + lbl + " edit <name>");
        Main.message(sender, "&6/" + lbl + " reload");
        Main.message(sender, "&6/" + lbl + " help");
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, String @NotNull [] args) {
        if (args.length <= 1) {
            return McTools.getTabArgs(args[0], List.of("create", "remove", "list", "open", "edit", "reload", "help"));
        } else if (args.length >= 3 && args[0].equalsIgnoreCase("create")) {
            return McTools.getTabArgs(args[2], shopTypes);
        }

        return null;
    }
}
