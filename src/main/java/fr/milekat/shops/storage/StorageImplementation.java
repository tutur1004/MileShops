package fr.milekat.shops.storage;

import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.storage.exeptions.StorageExecuteException;
import fr.milekat.shops.storage.utils.PlayerTradeMode;
import fr.milekat.shops.storage.utils.ShopTrades;
import fr.milekat.shops.workers.utils.TradeMode;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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
        Shops
     */
    void asyncSaveShop(@NotNull Shop shop, CommandSender sender, boolean createIfNotExist);

    Shop getShop(@NotNull UUID shopUuid) throws StorageExecuteException;
    Shop getShop(@NotNull String shopName) throws StorageExecuteException;
    Shop getShopNpc(@NotNull UUID shopNpcUuid) throws StorageExecuteException;

    default Shop getCacheShop(@NotNull UUID shopUuid) throws StorageExecuteException {
        Main.debug("Get cache shop '" + shopUuid + "'.");
        Optional<Map.Entry<Shop, Date>> optionalShop = Storage.SHOP_CACHE.entrySet()
                .stream()
                .filter(entry -> entry.getKey().getUuid().equals(shopUuid))
                .filter(entry -> entry.getValue().getTime() + Storage.SHOP_DELAY > new Date().getTime())
                .findFirst();
        if (optionalShop.isPresent()) {
            Main.debug("Shop '" + shopUuid + "' found.");
            return optionalShop.get().getKey();
        } else  {
            Main.debug("Shop '" + shopUuid + "' not found in cache, try to search it.");
            return getShop(shopUuid);
        }
    }
    default Shop getCacheShopNpc(@NotNull UUID npcUuid) throws StorageExecuteException {
        Main.debug("Get cache shop of npc '" + npcUuid + "'.");
        Optional<Map.Entry<Shop, Date>> optionalShop = Storage.SHOP_CACHE.entrySet()
                .stream()
                .filter(entry -> entry.getKey().getNpc().getSimpleID().equals(npcUuid.toString()))
                .filter(entry -> entry.getValue().getTime() + Storage.SHOP_DELAY > new Date().getTime())
                .findFirst();
        if (optionalShop.isPresent()) {
            Main.debug("Shop '" + optionalShop.get().getKey().getUuid() + "' found.");
            return optionalShop.get().getKey();
        } else  {
            Main.debug("Shop of npc '" + npcUuid + "' not found in cache, try to search it.");
            return getShopNpc(npcUuid);
        }
    }
    default Shop getCacheShop(@NotNull String shopName) throws StorageExecuteException {
        Main.debug("Get cache shop with '" + shopName + "'.");
        Optional<Map.Entry<Shop, Date>> optionalShop = Storage.SHOP_CACHE.entrySet()
                .stream()
                .filter(entry -> entry.getKey().getName().equals(shopName))
                .filter(entry -> entry.getValue().getTime() + Storage.SHOP_DELAY > new Date().getTime())
                .findFirst();
        if (optionalShop.isPresent()) {
            Main.debug("Shop '" + optionalShop.get().getKey().getUuid() + "' found.");
            return optionalShop.get().getKey();
        } else  {
            Main.debug("Shop with name '" + shopName + "' not found in cache, try to search it.");
            return getShop(shopName);
        }
    }

    List<Shop> getAllShops() throws StorageExecuteException;

    default List<Shop> getCacheAllShops() throws StorageExecuteException {
        if (Storage.SHOP_CACHE.values().stream()
                .noneMatch(date -> date.getTime() + Storage.SHOP_DELAY < new Date().getTime())) {
            return getAllShops();
        } else {
            return new ArrayList<>(Storage.SHOP_CACHE.keySet());
        }
    }

    /*
        Trades
     */
    void asyncSaveShopTrades(@NotNull Shop shop, @NotNull List<Trade> trades, CommandSender sender);

    List<Trade> getTrades(@NotNull UUID shopUuid) throws StorageExecuteException;
    List<Trade> getTrades(@NotNull String shopName) throws StorageExecuteException;

    default List<Trade> getCacheTrades(@NotNull UUID shopUuid) throws StorageExecuteException {
        Main.debug("Get cache trades from shop '" + shopUuid + "'.");
        Optional<Map.Entry<ShopTrades, Date>> optionalTrades = Storage.TRADE_CACHE.entrySet()
                .stream()
                .filter(entry -> entry.getKey().shopUuid().equals(shopUuid))
                .filter(entry -> entry.getValue().getTime() + Storage.TRADE_DELAY > new Date().getTime())
                .findFirst();
        if (optionalTrades.isPresent()) {
            Main.debug("Found '" + optionalTrades.get().getKey().trades().size() + "' trades.");
            return optionalTrades.get().getKey().trades();
        } else {
            Main.debug("Trades for shop '" + shopUuid + "' not found in cache, try to search them.");
            return getTrades(shopUuid);
        }
    }
    default List<Trade> getCacheTrades(@NotNull String shopName) throws StorageExecuteException {
        UUID shopUuid = getCacheShop(shopName).getUuid();
        return getCacheTrades(shopUuid);
    }

    /*
        TradeMode
     */
    void asyncSaveTradeMode(@NotNull UUID playerUuid, @NotNull TradeMode mode);
    TradeMode getTradeMode(@NotNull UUID playerUuid) throws StorageExecuteException;
    default TradeMode getCacheTradeMode(@NotNull UUID playerUuid) throws StorageExecuteException {
        Main.debug("Get cache user trade mode of '" + playerUuid + "'.");
        Optional<Map.Entry<PlayerTradeMode, Date>> optionalPlayerMode = Storage.TRADE_MODE_CACHE.entrySet()
                .stream()
                .filter(entry -> entry.getKey().playerUuid().equals(playerUuid))
                .filter(entry -> entry.getValue().getTime() + Storage.TRADE_MODE_DELAY > new Date().getTime())
                .findFirst();

        if (optionalPlayerMode.isPresent()) {
            Main.debug("Trade mode found for player '" + playerUuid + "'.");
            return optionalPlayerMode.get().getKey().tradeMode();
        } else {
            Main.debug("Trades mode for player '" + playerUuid + "' not found in cache, try to search them.");
            return getTradeMode(playerUuid);
        }
    }

    int getTradeUses(@NotNull UUID player, @NotNull UUID tradeUuid);

    void logTrade(@NotNull UUID player, @Nullable List<String> playerTags, @NotNull Trade trade);
}
