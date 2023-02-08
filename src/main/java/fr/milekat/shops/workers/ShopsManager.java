package fr.milekat.shops.workers;

import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.storage.exeptions.StorageExecuteException;
import fr.mrmicky.fastinv.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ShopsManager {
    public static final int CHEST_TRADES_PER_PAGE = 4;
    public static final int EDITOR_TRADES_PER_PAGE = 9;
    //  Inventory items shortcuts
    public static final ItemStack PANE_BLACK = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
    public static final ItemStack PANE_WHITE = new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE).name(" ").build();
    public static final ItemStack PAGE_LEFT = new ItemBuilder(Material.ARROW).name("Previous page").build();
    public static final ItemStack PAGE_RIGHT = new ItemBuilder(Material.ARROW).name("Next page").build();



    public static @Nullable Map.Entry<Shop, List<Trade>> getShop(@NotNull UUID npcUuid) throws StorageExecuteException {
        Shop shop = Main.getStorage().getCacheShop(npcUuid);
        if (shop==null) return null;
        List<Trade> trades = Main.getStorage().getCacheTrades(shop.getUuid());
        trades.sort(Comparator.comparingInt(Trade::getTradePosition));
        return  new AbstractMap.SimpleEntry<>(shop, trades);
    }
}
