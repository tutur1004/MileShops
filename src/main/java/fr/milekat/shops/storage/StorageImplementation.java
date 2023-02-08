package fr.milekat.shops.storage;

import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.storage.exeptions.StorageExecuteException;
import fr.milekat.shops.workers.utils.TradeMode;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("unused")
public interface StorageImplementation {
    /**
     * Check if all storages are loaded
     * @return true if all storages are loaded
     */
    boolean checkStorages() throws StorageExecuteException;

    /**
     * Get the implemented (Used) storage type
     * @return storage type
     */
    String getImplementationName();

    /**
     * Disconnect from Storage provider
     */
    void disconnect();

    /*
        ES Queries execution
     */
    void asyncSaveShop(@NotNull Shop shop, CommandSender sender);

    Shop getShop(@NotNull UUID shopUuid) throws StorageExecuteException;
    Shop getShop(@NotNull String shopName) throws StorageExecuteException;

    Shop getCacheShop(@NotNull UUID shopUuid) throws StorageExecuteException;
    Shop getCacheShop(@NotNull String shopName) throws StorageExecuteException;

    List<Shop> getAllShops() throws StorageExecuteException;

    List<Shop> getCacheAllShops() throws StorageExecuteException;

    void asyncSaveShopTrades(@NotNull Shop shop, @NotNull List<Trade> trades, CommandSender sender);

    List<Trade> getTrades(@NotNull UUID shopUuid) throws StorageExecuteException;
    List<Trade> getTrades(@NotNull String shopName) throws StorageExecuteException;

    List<Trade> getCacheTrades(@NotNull UUID shopUuid) throws StorageExecuteException;
    List<Trade> getCacheTrades(@NotNull String shopName) throws StorageExecuteException;

    //  TODO: Convert to UUID ? Or Not ?
    void asyncSaveTradeMode(@NotNull String uuid, @NotNull TradeMode mode);
    //  TODO: Convert to UUID ? Or Not ?
    TradeMode getCacheTradeMode(@NotNull String playerUuid) throws StorageExecuteException;

    int getTradeUses(@NotNull UUID player, @NotNull UUID tradeUuid);
}
