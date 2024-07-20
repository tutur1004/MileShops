package fr.milekat.shops;

import fr.milekat.milenpc.api.classes.NPC;
import fr.milekat.shops.api.MileShopsIAPI;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.api.exceptions.StorageException;
import fr.milekat.shops.workers.listeners.LogTrade;
import fr.milekat.utils.storage.exceptions.StorageExecuteException;
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
    public @NotNull List<Trade> getNpcShopTrades(@NotNull UUID uuid) throws StorageException {
        try {
            NPC npc = Main.getNpcManager().getNpc(uuid);
            if (npc == null) {
                throw new StorageException(new Exception(), "NPC not found.");
            }
            return Main.getStorage().getCacheTrades(npc.getName());
        } catch (StorageExecuteException exception) {
            throw new StorageException(exception, exception.getMessage());
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
