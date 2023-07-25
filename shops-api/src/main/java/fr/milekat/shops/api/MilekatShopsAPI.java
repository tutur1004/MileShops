package fr.milekat.shops.api;

import fr.milekat.shops.api.exceptions.ApiUnavailable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The CustomShopsAPI class provides access to the custom shops API.
 */
public class MilekatShopsAPI {
    /**
     * Indicates whether the API is ready for use.
     */
    public static boolean API_READY = false;
    /**
     * The loaded API instance.
     */
    public static MilekatShopsIAPI LOADED_API;

    /**
     * Retrieves the instance of the custom shops API.
     *
     * @return The custom shops API instance.
     * @throws ApiUnavailable if the API is not ready.
     */
    @Contract(value = " -> new", pure = true)
    public static @NotNull MilekatShopsIAPI getAPI() throws ApiUnavailable {
        if (!MilekatShopsAPI.API_READY) {
            throw new ApiUnavailable();
        }
        return LOADED_API;
    }
}
