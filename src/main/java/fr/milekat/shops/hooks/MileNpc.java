package fr.milekat.shops.hooks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import fr.milekat.milenpc.api.MileNpcIAPI;
import fr.milekat.milenpc.api.classes.NPC;
import fr.milekat.milenpc.api.classes.NpcClickType;
import fr.milekat.milenpc.api.events.PlayerNpcInteractEvent;
import fr.milekat.milenpc.api.exceptions.ApiUnavailable;
import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.shops.api.classes.ShopNpc;
import fr.milekat.shops.hooks.npc.NPCDeserializer;
import fr.milekat.shops.hooks.npc.NPCSerializer;
import fr.milekat.shops.workers.utils.ShopUtils;
import fr.milekat.utils.storage.exceptions.StorageExecuteException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

public class MileNpc implements Listener {
    /**
     * Get the NPC API (With caching)
     */
    private static MileNpcIAPI loadedNpcApi = null;
    public static @NotNull MileNpcIAPI getNpcApi() {
        if (loadedNpcApi != null) return loadedNpcApi;

        RegisteredServiceProvider<MileNpcIAPI> provider =
                Bukkit.getServicesManager().getRegistration(MileNpcIAPI.class);

        if (provider == null)
            throw new RuntimeException("MileNpc API not found ! Is the plugin loaded ?");

        loadedNpcApi = provider.getProvider();
        return loadedNpcApi;
    }

    /**
     * NPC Shops Events
     */
    @EventHandler
    public void playerOpenNpcShop(@NotNull PlayerNpcInteractEvent event) throws StorageExecuteException {
        if (event.isCancelled()) return;
        //  Get shop
        Shop shop = Main.getStorage().getCacheShop(event.getNpc().getUuid());
        if (shop == null) return;
        //  Open shop
        ShopUtils.openShop(event.getPlayer(), shop);
        event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.LOW)
    public void playerOpenNpcShopEditor(@NotNull PlayerNpcInteractEvent event) throws StorageExecuteException {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        if (!player.hasPermission("shops.edit")) return;
        if (event.getClickType().equals(NpcClickType.SHIFT_LEFT_CLICK) ||
                event.getClickType().equals(NpcClickType.SHIFT_RIGHT_CLICK)) {
            //  Get shop
            Shop shop = Main.getStorage().getCacheShop(event.getNpc().getUuid());
            if (shop == null) return;
            //  Open admin editor
            ShopUtils.openAdminShop(player, shop);
            event.setCancelled(true);
        }
    }

    /**
     * Fetch a NPC
     *
     * @param uuid UUID of the NPC
     * @return NPC or null if not found
     */
    public static @Nullable NPC getNpc(@NotNull UUID uuid) {
        return getNpcApi().getNPCManager().getNpc(uuid);
    }

    public static @NotNull ShopNpc getShopNpc(@NotNull NPC npc) {
        return new ShopNpc(npc.getUuid(), npc.getName(), npc.getLocation(), npc.isSpawned(), npc.shouldBeSpawned(),
                npc.isNameplateVisible(), npc.getTexture(), npc.getSignature());
    }

    public static @NotNull ShopNpc create(UUID uuid, String name, Location location) {
        if (!Main.IS_NPC_LIB_LOADED) return new ShopNpc(uuid, name);
        NPC npc = getNpcApi().getNPCManager().create(uuid, name);
        try {
            npc.teleport(location);
            npc.show();
        } catch (ApiUnavailable ignore) {}
        return getShopNpc(npc);
    }

    public static void teleport(@Nullable UUID uuid, Location location) {
        if (uuid==null) return;
        if (!Main.IS_NPC_LIB_LOADED) return;
        NPC npc = getNpc(uuid);
        if (npc != null) {
            try {
                npc.teleport(location);
            } catch (ApiUnavailable ignore) {}
        }
    }

    public static void updateSkin(@Nullable UUID uuid, String texture, String signature) {
        if (uuid==null) return;
        if (!Main.IS_NPC_LIB_LOADED) return;
        NPC npc = getNpc(uuid);
        if (npc != null) {
            try {
                npc.updateSkin(texture, signature);
            } catch (ApiUnavailable ignore) {}
        }
    }

    public static void ensureVisible(@Nullable UUID uuid) {
        if (uuid==null) return;
        if (!Main.IS_NPC_LIB_LOADED) return;
        NPC npc = getNpc(uuid);
        if (npc != null) {
            try {
                npc.show();
            } catch (ApiUnavailable ignore) {}
        }
    }

    public static void ensureInvisible(@Nullable UUID uuid) {
        if (uuid==null) return;
        if (!Main.IS_NPC_LIB_LOADED) return;
        NPC npc = getNpc(uuid);
        if (npc != null) {
            try {
                npc.hide();
            } catch (ApiUnavailable ignore) {}
        }
    }

    public static void remove(@Nullable ShopNpc shopNpc) {
        if (shopNpc == null) return;
        destroy(shopNpc.uuid());
    }

    public static void destroy(@Nullable UUID uuid) {
        if (uuid==null) return;
        if (!Main.IS_NPC_LIB_LOADED) return;
        NPC npc = getNpc(uuid);
        if (npc != null) {
            try {
                npc.remove();
            } catch (ApiUnavailable ignore) {}
        }
    }

    /**
     * Add NPC mappers to the given ObjectMapper and module
     *
     * @param mapper The ObjectMapper to add the mappers to
     * @param module The SimpleModule to add the mappers to
     */
    public static void addNpcMappers(@NotNull ObjectMapper mapper, @NonNull SimpleModule module) {
        //  NPC
        module.addSerializer(ShopNpc.class, new NPCSerializer(mapper));
        module.addDeserializer(ShopNpc.class, new NPCDeserializer(mapper));
    }
}
