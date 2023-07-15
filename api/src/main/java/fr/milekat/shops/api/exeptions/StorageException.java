package fr.milekat.shops.api.exeptions;

import fr.milekat.shops.api.CustomShopsAPI;

public class StorageException extends Exception {
    private final String message;

    /**
     * Issue during a storage execution
     */
    public StorageException(Throwable exception, String message) {
        super(exception);
        this.message = message;
        try {
            if (CustomShopsAPI.getAPI().isDebug()) {
                exception.printStackTrace();
            }
        } catch (CustomShopsApiUnavailable ignore) {
            //  Not possible since you can't except a Storage Exception from the API... if the API is not loaded...
        }
    }

    /**
     * Get error message (If exist)
     */
    public String getMessage() {
        return message;
    }
}
