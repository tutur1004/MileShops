package fr.milekat.shops.api;

import fr.milekat.shops.api.exeptions.CustomShopsApiUnavailable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class CustomShopsAPI {
    public static boolean API_READY = false;
    public static CustomShopsIAPI LOADED_API;

    @Contract(value = " -> new", pure = true)
    public static @NotNull CustomShopsIAPI getAPI() throws CustomShopsApiUnavailable {
        if (!CustomShopsAPI.API_READY) {
            throw new CustomShopsApiUnavailable();
        }
        return LOADED_API;
    }
}
