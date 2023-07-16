package fr.milekat.shops.api;

import fr.milekat.shops.api.exeptions.CustomShopsApiUnavailable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The CustomShopsAPI class provides access to the custom shops API.
 */
public class CustomShopsAPI {
    /**
     * Indicates whether the API is ready for use.
     */
    public static boolean API_READY = false;
    /**
     * The loaded API instance.
     */
    public static CustomShopsIAPI LOADED_API;

    /**
     * Retrieves the instance of the custom shops API.
     *
     * @return The custom shops API instance.
     * @throws CustomShopsApiUnavailable if the API is not ready.
     */
    @Contract(value = " -> new", pure = true)
    public static @NotNull CustomShopsIAPI getAPI() throws CustomShopsApiUnavailable {
        if (!CustomShopsAPI.API_READY) {
            throw new CustomShopsApiUnavailable();
        }
        return LOADED_API;
    }
}
