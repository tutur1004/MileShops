package fr.milekat.shops.workers.commands;

import fr.milekat.milenpc.api.classes.NPC;
import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.ShopType;
import fr.milekat.shops.workers.utils.NPCUtils;
import fr.milekat.shops.workers.utils.ShopUtils;
import fr.milekat.utils.McTools;
import fr.milekat.utils.storage.exceptions.StorageExecuteException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
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

    private static final int SHOPS_PER_PAGE = 8;

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
                displayShopsList(player, 1, label);
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
                    ShopUtils.openShop(player, shop);

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
                    ShopUtils.openAdminShop(player, shop);
                } catch (StorageExecuteException e) {
                    Main.message(player, "&cStorage error");
                }
                return true;
            }

            //  List shop page
            if (args[0].equalsIgnoreCase("list") && player.hasPermission("shops.list")) {
                try {
                    int page = Integer.parseInt(args[1]);
                    if (page < 1) {
                        Main.message(player, "&cPage must be greater than 0.");
                        return true;
                    }
                    displayShopsList(player, page, label);
                } catch (NumberFormatException e) {
                    Main.message(player, "&cPage must be a valid number.");
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

            if (args[0].equalsIgnoreCase("type") && player.hasPermission("shops.type")) {
                try {
                    Shop shop = Main.getStorage().getCacheShop(args[1]);
                    if (shop == null) {
                        Main.message(player, "&cShop not found.");
                        return true;
                    }

                    try {
                        ShopType newType = ShopType.valueOf(args[2].toUpperCase(Locale.ROOT));
                        shop.setType(newType);
                        Main.getStorage().asyncSaveShop(shop, false, player);
                        Main.message(player, "&2Shop type changed to " + newType.name() + " !");
                    } catch (IllegalArgumentException exception) {
                        Main.message(player, "&cUnknown shop type !");
                        Main.message(player, "&cPlease use one of " + Arrays.toString(ShopType.values()));
                    }
                } catch (StorageExecuteException e) {
                    Main.getMileLogger().warning(e.getMessage());
                    Main.getMileLogger().stack(e.getStackTrace());
                    Main.message(player, "&cStorage error");
                }

                return true;
            }

            if (args[0].equalsIgnoreCase("create") && player.hasPermission("shops.create")) {

                UUID shopUuid = UUID.randomUUID();
                try {
                    ShopType type = ShopType.valueOf(args[1].toUpperCase(Locale.ROOT));
                    NPC npc = null;
                    if (Main.IS_NPC_LIB_LOADED) {
                        npc = NPCUtils.create(shopUuid, args[2], player.getLocation());
                    }
                    Shop shop = new Shop(shopUuid, args[2], npc, type);
                    Main.getStorage().asyncSaveShop(shop, true, player);
                } catch (IllegalArgumentException exception) {
                    NPCUtils.destroy(shopUuid);
                    Main.message(player, "&cUnknown NPC type '" + args[1] + "' !");
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

    private void displayShopsList(@NotNull Player player, int page, String label) {
        try {
            List<Shop> allShops = Main.getStorage().getCacheAllShops();
            if (allShops.isEmpty()) {
                Main.message(player, "&cNo shop found.");
                return;
            }

            int totalPages = (int) Math.ceil((double) allShops.size() / SHOPS_PER_PAGE);
            if (page > totalPages) {
                Main.message(player, "&cPage " + page + " does not exist. Max pages: " + totalPages);
                return;
            }

            int startIndex = (page - 1) * SHOPS_PER_PAGE;
            int endIndex = Math.min(startIndex + SHOPS_PER_PAGE, allShops.size());
            List<Shop> pageShops = allShops.subList(startIndex, endIndex);

            Main.message(player, "&6--- Shops list (" + allShops.size() + ") --- Page " + page + "/" + totalPages + " ---");

            for (Shop shop : pageShops) {
                TextComponent line = Component.text("");

                // Open button
                line = line.append(Component.text("[")
                                .color(NamedTextColor.DARK_AQUA))
                        .append(Component.text("open")
                                .color(NamedTextColor.GREEN)
                                .clickEvent(ClickEvent.runCommand("/" + label + " open " + shop.getName()))
                                .hoverEvent(Component.text("Click to open"))
                        )
                        .append(Component.text("] ")
                                .color(NamedTextColor.DARK_AQUA));

                // Edit button
                line = line.append(Component.text("[")
                                .color(NamedTextColor.DARK_AQUA))
                        .append(Component.text("edit")
                                .color(NamedTextColor.GREEN)
                                .clickEvent(ClickEvent.runCommand("/" + label + " edit " + shop.getName()))
                                .hoverEvent(Component.text("Click to edit"))
                        )
                        .append(Component.text("] ")
                                .color(NamedTextColor.DARK_AQUA));

                // Shop name and type
                line = line.append(Component.text(shop.getName())
                                .color(NamedTextColor.YELLOW))
                        .append(Component.text(" (" + shop.getType().name().toLowerCase() + ")")
                                .color(NamedTextColor.DARK_GRAY));

                player.sendMessage(line);
            }

            // Navigation buttons
            if (totalPages > 1) {
                TextComponent nav = Component.text("");

                // Previous button
                if (page > 1) {
                    nav = nav.append(Component.text("[")
                                    .color(NamedTextColor.DARK_AQUA))
                            .append(Component.text("<===")
                                    .color(NamedTextColor.GREEN)
                                    .clickEvent(ClickEvent.runCommand("/" + label + " list " + (page - 1))))
                            .append(Component.text("] ")
                                    .color(NamedTextColor.DARK_AQUA));
                } else {
                    nav = nav.append(Component.text("[")
                                    .color(NamedTextColor.DARK_AQUA))
                            .append(Component.text("<===")
                                    .color(NamedTextColor.RED))
                            .append(Component.text("] ")
                                    .color(NamedTextColor.DARK_AQUA));
                }

                // Page numbers
                for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                    if (pageNum == page) {
                        nav = nav.append(Component.text("[")
                                        .color(NamedTextColor.DARK_AQUA))
                                .append(Component.text(String.valueOf(pageNum))
                                        .color(NamedTextColor.GOLD))
                                .append(Component.text("] ")
                                        .color(NamedTextColor.DARK_AQUA));
                    } else {
                        nav = nav.append(Component.text("[")
                                        .color(NamedTextColor.DARK_AQUA))
                                .append(Component.text(String.valueOf(pageNum))
                                        .color(NamedTextColor.GREEN)
                                        .clickEvent(ClickEvent.runCommand("/" + label + " list " + pageNum)))
                                .append(Component.text("] ")
                                        .color(NamedTextColor.DARK_AQUA));
                    }
                }

                // Next button
                if (page < totalPages) {
                    nav = nav.append(Component.text("[")
                                    .color(NamedTextColor.DARK_AQUA))
                            .append(Component.text("===>")
                                    .color(NamedTextColor.GREEN)
                                    .clickEvent(ClickEvent.runCommand("/" + label + " list " + (page + 1))))
                            .append(Component.text("]")
                                    .color(NamedTextColor.DARK_AQUA));
                } else {
                    nav = nav.append(Component.text("[")
                                    .color(NamedTextColor.DARK_AQUA))
                            .append(Component.text("===>")
                                    .color(NamedTextColor.RED))
                            .append(Component.text("]")
                                    .color(NamedTextColor.DARK_AQUA));
                }

                player.sendMessage(nav);
            }

        } catch (StorageExecuteException exception) {
            Main.getMileLogger().warning("Storage error while trying to fetch shops list.");
            Main.message(player, "&cStorage error while trying to fetch shops list.");
        }
    }

    private void sendHelp(@NotNull CommandSender sender, String lbl) {
        Main.message(sender, "&6/" + lbl + " create <type> <name>");
        Main.message(sender, "&6/" + lbl + " type <name> <newType>");
        Main.message(sender, "&6/" + lbl + " remove <name>");
        Main.message(sender, "&6/" + lbl + " list [page]");
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
            return McTools.getTabArgs(args[0], List.of("create", "type", "remove", "list", "open", "edit", "reload", "help"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("type")) {
            try {
                return Main.getStorage().getCacheAllShops().stream()
                        .map(Shop::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .toList();
            } catch (StorageExecuteException e) {
                return null;
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            return McTools.getTabArgs(args[1], shopTypes);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("type")) {
            return McTools.getTabArgs(args[2], shopTypes);
        }

        return null;
    }
}