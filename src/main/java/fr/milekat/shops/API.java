package fr.milekat.shops;

import fr.milekat.shops.api.MileShopsIAPI;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.api.exceptions.StorageException;
import fr.milekat.shops.workers.listeners.LogTrade;
import fr.milekat.shops.workers.utils.ShopUtils;
import fr.milekat.utils.storage.exceptions.StorageExecuteException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class API implements MileShopsIAPI {
    @Override
    public boolean isDebug() {
        return Main.DEBUG;
    }

    @Override
    public @NotNull List<Shop> getShops() throws StorageException {
        try {
            return Main.getStorage().getAllShops();
        } catch (StorageExecuteException exception) {
            throw new StorageException(exception, exception.getMessage());
        }
    }

    @Override
    public @NotNull List<Trade> getShopTrades(@NotNull Shop shop) throws StorageException {
        return getShopTrades(shop.getUuid());
    }

    @Override
    public @NotNull List<Trade> getShopTrades(@NotNull UUID uuid) throws StorageException {
        try {
            return Main.getStorage().getCacheTrades(uuid);
        } catch (StorageExecuteException exception) {
            throw new StorageException(exception, exception.getMessage());
        }
    }

    @Override
    public @NotNull List<Trade> getShopTrades(@NotNull String name) throws StorageException {
        try {
            return Main.getStorage().getCacheTrades(name);
        } catch (StorageExecuteException exception) {
            throw new StorageException(exception, exception.getMessage());
        }
    }

    @Override
    public boolean openShop(@NotNull UUID uuid, @NotNull Shop shop) {
        Player player = Main.getInstance().getServer().getPlayer(uuid);
        if (player != null) {
            try {
                ShopUtils.openShop(player, shop);
                return true;
            } catch (Exception e) {
                if (isDebug()) Main.getMileLogger().stack(e.getStackTrace());
            }
        }
        return false;
    }

    @Override
    public boolean openAdminShop(@NotNull UUID uuid, @NotNull Shop shop) {
        Player player = Main.getInstance().getServer().getPlayer(uuid);
        if (player != null) {
            try {
                ShopUtils.openAdminShop(player, shop);
                return true;
            } catch (Exception e) {
                if (isDebug()) Main.getMileLogger().stack(e.getStackTrace());
            }
        }
        return false;
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
