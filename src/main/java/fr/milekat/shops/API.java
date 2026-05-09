package fr.milekat.shops;

import fr.milekat.shops.api.MileShopsIAPI;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.api.exceptions.StorageException;
import fr.milekat.shops.workers.utils.ShopUtils;
import fr.milekat.shops.api.classes.TradeMode;
import fr.milekat.shops.workers.utils.TradeUtils;
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
            return Main.getStorage().getCacheAllShops();
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
    public int processedTrades(@NotNull Player player, @NotNull TradeMode tradeMode,
                               @NotNull Shop shop, @NotNull Trade trade, boolean multiple) {
        return TradeUtils.processedTrades(player, tradeMode, shop, trade, multiple);
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
        return getPlayerTagsStatic(uuid);
    }
    public static @Nullable Map<String, Object> getPlayerTagsStatic(@NotNull UUID uuid) {
        return Main.PLAYER_TAGS.getOrDefault(uuid, null);
    }

    @Override
    public void removePlayerTags(@NotNull UUID uuid) {
        removePlayerTagsStatic(uuid);
    }
    public static void removePlayerTagsStatic(@NotNull UUID uuid) {
        Main.PLAYER_TAGS.remove(uuid);
    }

    @Override
    public void setPlayerTags(@NotNull UUID uuid, @NotNull Map<String, Object> tags) {
        setPlayerTagsStatic(uuid, tags);
    }
    public static void setPlayerTagsStatic(@NotNull UUID uuid, @NotNull Map<String, Object> tags) {
        Main.PLAYER_TAGS.put(uuid, tags);
    }

    @Override
    public double getPlayerModifier(@NotNull UUID uuid) {
        return getPlayerModifierStatic(uuid);
    }
    public static double getPlayerModifierStatic(@NotNull UUID uuid) {
        return Main.PLAYER_MODIFIERS.getOrDefault(uuid, 1.0);
    }

    @Override
    public void setPlayerModifier(@NotNull UUID uuid, double modifier) {
        setPlayerModifierStatic(uuid, modifier);
    }
    public static void setPlayerModifierStatic(@NotNull UUID uuid, double modifier) {
        Main.PLAYER_MODIFIERS.put(uuid, modifier);
    }
}
