package fr.milekat.shops.storage;

import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.workers.utils.TradeMode;
import fr.milekat.utils.storage.exceptions.StorageExecuteException;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unused")
public interface StorageImplementation extends CacheManager {
    /**
     * Check if all storages are loaded
     * @return true if all storages are loaded
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean checkStorages() throws StorageExecuteException;

    /**
     * Disconnect from Storage provider
     */
    void disconnect();

    /*
        Shops
     */
    void asyncSaveShop(@NotNull Shop shop, CommandSender sender, boolean createIfNotExist);

    Shop getShop(@NotNull UUID shopUuid) throws StorageExecuteException;
    Shop getShop(@NotNull String shopName) throws StorageExecuteException;

    @NotNull List<Shop> getAllShops() throws StorageExecuteException;

    void asyncDeleteShop(@NotNull Shop shop, CommandSender sender);

    /*
        Trades
     */
    void asyncSaveShopTrades(@NotNull Shop shop, @NotNull List<Trade> trades, CommandSender sender);

    List<Trade> getTrades(@NotNull UUID shopUuid) throws StorageExecuteException;
    List<Trade> getTrades(@NotNull String shopName) throws StorageExecuteException;

    default List<Trade> getCacheTrades(@NotNull String shopName) throws StorageExecuteException {
        UUID shopUuid = getCacheShop(shopName).getUuid();
        return getCacheTrades(shopUuid);
    }

    /*
        TradeMode
     */
    void asyncSaveTradeMode(@NotNull UUID playerUuid, @NotNull TradeMode mode);
    TradeMode getTradeMode(@NotNull UUID playerUuid) throws StorageExecuteException;

    /*
        Trade History
     */

    int getTradeUses(@NotNull Map<String, Object> tags, @NotNull Trade trade);

    void logTrade(@Nullable Map<String, Object> tags, @NotNull Trade trade);
}
