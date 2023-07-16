package fr.milekat.shops.api.exeptions;

/**
 * Exception thrown when the CustomShops API is unavailable or not loaded.
 */
public class CustomShopsApiUnavailable extends Exception {
    /**
     * Constructs a new CustomShopsApiUnavailable exception.
     * This exception is thrown when the CustomShops API is not loaded.
     */
    public CustomShopsApiUnavailable() {
        super();
    }
}
