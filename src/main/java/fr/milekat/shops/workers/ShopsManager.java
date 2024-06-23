package fr.milekat.shops.workers;

import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.utils.storage.exceptions.StorageExecuteException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ShopsManager {
    public static final int INVENTORY_SMALL_TRADES_PER_PAGE = 4;
    public static final int INVENTORY_LARGE_TRADES_PER_PAGE = 12;
    public static final int EDITOR_TRADES_PER_PAGE = 9;

    public static @Nullable Map.Entry<Shop, List<Trade>> getShop(@NotNull UUID npcUuid) throws StorageExecuteException {
        Shop shop = Main.getStorage().getCacheShop(npcUuid);
        if (shop==null) return null;
        List<Trade> trades = Main.getStorage().getCacheTrades(shop.getUuid());
        if (trades.size() > 1) {
            trades.sort(Comparator.comparingInt(Trade::getTradePosition));
        }
        return new AbstractMap.SimpleEntry<>(shop, trades);
    }
}
