package fr.milekat.shops.api;

import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.api.exeptions.StorageException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface CustomShopsIAPI {
    boolean isDebug();

    @NotNull
    List<Trade> getShopTrades(@NotNull Shop shop) throws StorageException;
    @NotNull
    List<Trade> getShopTrades(@NotNull UUID uuid) throws StorageException;
    @NotNull
    List<Trade> getShopTrades(@NotNull String name) throws StorageException;
    @NotNull
    List<Trade> getNpcShopTrades(@NotNull UUID uuid) throws StorageException;

    @Nullable
    Map<String, Object> getPlayerTags(@NotNull UUID uuid);
    void removePlayerTags(@NotNull UUID uuid);
    void setPlayerTags(@NotNull UUID uuid, @NotNull Map<String, Object> tags);
}
