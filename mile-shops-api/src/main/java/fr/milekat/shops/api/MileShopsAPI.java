package fr.milekat.shops.api;

import fr.milekat.shops.api.classes.Trade;
import fr.milekat.shops.api.exceptions.ApiUnavailable;
import fr.milekat.shops.api.exceptions.StorageException;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.UUID;

/**
 * The MileShopsAPI class provides access to the MileShops API via the Bukkit ServicesManager.
 */
public class MileShopsAPI {

    public static boolean isDebug() {
        try {
            return getLoadShopAPI().isDebug();
        } catch (ApiUnavailable e) {
            return false;
        }
    }

    public static @NonNull List<Trade> getShopTrades(UUID shopUuid) throws ApiUnavailable, StorageException {
        return getLoadShopAPI().getShopTrades(shopUuid);
    }

    private static @NonNull MileShopsIAPI getLoadShopAPI() throws ApiUnavailable {
        RegisteredServiceProvider<MileShopsIAPI> provider =
                Bukkit.getServicesManager().getRegistration(MileShopsIAPI.class);

        if (provider == null) throw new ApiUnavailable();

        return provider.getProvider();
    }
}
