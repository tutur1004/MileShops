package fr.milekat.shops.hooks;

import fr.milekat.banks.api.MileBanksIAPI;
import fr.milekat.banks.api.exceptions.StorageException;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class MileBanks {
    /**
     * Get the NPC API (With caching)
     */
    private static MileBanksIAPI loadedBankApi = null;
    public static @NotNull MileBanksIAPI getBankApi() {
        if (loadedBankApi != null) return loadedBankApi;

        RegisteredServiceProvider<MileBanksIAPI> provider =
                Bukkit.getServicesManager().getRegistration(MileBanksIAPI.class);

        if (provider == null)
            throw new RuntimeException("MileBanks API not found ! Is the plugin loaded ?");

        loadedBankApi = provider.getProvider();
        return loadedBankApi;
    }

    public static @NotNull List<String> getCurrencies() {
        return getBankApi().getCurrencies();
    }

    public static void addMoneyByTags(@NotNull Map<String, Object> tags, int amount,
                                      @Nullable String reason) throws RuntimeException {
        try {
            getBankApi().addMoneyByTags(tags, amount, reason);
        }  catch (StorageException e) {
            throw new RuntimeException(e);
        }
    }
}
