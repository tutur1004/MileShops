package fr.milekat.shops;

import fr.milekat.shops.api.MileShopsIAPI;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.api.exceptions.StorageException;
import fr.milekat.shops.storage.exceptions.StorageExecuteException;
import fr.milekat.shops.workers.ShopsManager;
import fr.milekat.shops.workers.listeners.LogTrade;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class API implements MileShopsIAPI {
    @Override
    public boolean isDebug() {
        return Main.DEBUG;
    }

    @Override
    public @NotNull List<Trade> getShopTrades(@NotNull Shop shop) throws StorageException {
        return getShopTrades(shop.getUuid());
    }

    @Override
    public @NotNull List<Trade> getShopTrades(@NotNull UUID uuid) throws StorageException {
        try {
            return Main.getStorage().getTrades(uuid);
        } catch (StorageExecuteException exception) {
            throw new StorageException(exception, exception.getMessage());
        }
    }

    @Override
    public @NotNull List<Trade> getShopTrades(@NotNull String name) throws StorageException {
        try {
            return Main.getStorage().getTrades(name);
        } catch (StorageExecuteException exception) {
            throw new StorageException(exception, exception.getMessage());
        }
    }

    @Override
    public @NotNull List<Trade> getNpcShopTrades(@NotNull UUID uuid) throws StorageException {
        try {
            Map.Entry<Shop, List<Trade>> shop = ShopsManager.getShop(uuid);
            return Objects.requireNonNull(shop).getValue();
        } catch (StorageExecuteException exception) {
            throw new StorageException(exception, exception.getMessage());
        } catch (NullPointerException exception) {
            throw new StorageException(exception, "NPC trades not found");
        }
    }

    @Override
    public @Nullable Map<String, Object> getPlayerTags(@NotNull UUID uuid) {
        return LogTrade.playerTags.getOrDefault(uuid, null);
    }

    @Override
    public void removePlayerTags(@NotNull UUID uuid) {
        LogTrade.playerTags.remove(uuid);
    }

    @Override
    public void setPlayerTags(@NotNull UUID uuid, @NotNull Map<String, Object> tags) {
        LogTrade.playerTags.put(uuid, tags);
    }
}
