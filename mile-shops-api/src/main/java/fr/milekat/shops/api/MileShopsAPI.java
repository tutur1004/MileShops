package fr.milekat.shops.api;

import fr.milekat.shops.api.exceptions.ApiUnavailable;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * The MileShopsAPI class provides access to the MileShops API via the Bukkit ServicesManager.
 */
public class MileShopsAPI {

    /**
     * Retrieves the instance of the MileShops API from the Bukkit ServicesManager.
     *
     * @return The MileShops API instance.
     * @throws ApiUnavailable if the API is not registered (plugin not loaded or not ready).
     */
    public static @NotNull MileShopsIAPI getAPI() throws ApiUnavailable {
        MileShopsIAPI api = Bukkit.getServicesManager().load(MileShopsIAPI.class);
        if (api == null) {
            throw new ApiUnavailable();
        }
        return api;
    }
}
