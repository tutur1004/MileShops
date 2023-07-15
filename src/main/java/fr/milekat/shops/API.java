package fr.milekat.shops;

import dev.sergiferry.playernpc.api.NPC;
import fr.milekat.shops.api.CustomShopsIAPI;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.api.exeptions.StorageException;
import fr.milekat.shops.storage.exeptions.StorageExecuteException;
import fr.milekat.shops.workers.ShopsManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class API implements CustomShopsIAPI {
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
    public @NotNull List<Trade> getShopTrades(@NotNull NPC.Global npc) throws StorageException {
        try {
            Map.Entry<Shop, List<Trade>> shop = ShopsManager.getShop(UUID.fromString(npc.getSimpleID()));
            return Objects.requireNonNull(shop).getValue();
        } catch (StorageExecuteException exception) {
            throw new StorageException(exception, exception.getMessage());
        } catch (NullPointerException exception) {
            throw new StorageException(exception, "NPC trades not found");
        }
    }

    @Override
    public @Nullable List<String> getPlayerTags(@NotNull UUID uuid) {
        return null;
    }

    @Override
    public void setPlayerTags(@NotNull UUID uuid, @NotNull List<String> tags) {

    }
}
