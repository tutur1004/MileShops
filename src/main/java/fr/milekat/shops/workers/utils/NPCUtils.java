package fr.milekat.shops.workers.utils;

import fr.milekat.milenpc.api.classes.NPC;
import fr.milekat.shops.Main;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class NPCUtils {
    public static @NotNull NPC create(UUID uuid, String name, Location location) {
        if (!Main.IS_NPC_LIB_LOADED) return new NPC(uuid, name);
        NPC npc = Main.getNpcApi().getNPCManager().create(uuid, name);
        try {
            npc.teleport(location);
            npc.show();
        } catch (fr.milekat.milenpc.api.exceptions.ApiUnavailable ignore) {}
        return npc;
    }

    public static void teleport(@NotNull UUID uuid, Location location) {
        if (!Main.IS_NPC_LIB_LOADED) return;
        NPC npc = Main.getNpc(uuid);
        if (npc != null) {
            try {
                npc.teleport(location);
            } catch (fr.milekat.milenpc.api.exceptions.ApiUnavailable ignore) {}
        }
    }

    public static void updateSkin(@NotNull UUID uuid, String texture, String signature) {
        if (!Main.IS_NPC_LIB_LOADED) return;
        NPC npc = Main.getNpc(uuid);
        if (npc != null) {
            try {
                npc.updateSkin(texture, signature);
            } catch (fr.milekat.milenpc.api.exceptions.ApiUnavailable ignore) {}
        }
    }

    public static void ensureVisible(@NotNull UUID uuid) {
        if (!Main.IS_NPC_LIB_LOADED) return;
        NPC npc = Main.getNpc(uuid);
        if (npc != null) {
            try {
                npc.show();
            } catch (fr.milekat.milenpc.api.exceptions.ApiUnavailable ignore) {}
        }
    }

    public static void ensureInvisible(@NotNull UUID uuid) {
        if (!Main.IS_NPC_LIB_LOADED) return;
        NPC npc = Main.getNpc(uuid);
        if (npc != null) {
            try {
                npc.hide();
            } catch (fr.milekat.milenpc.api.exceptions.ApiUnavailable ignore) {}
        }
    }

    public static void destroy(@NotNull UUID uuid) {
        if (!Main.IS_NPC_LIB_LOADED) return;
        NPC npc = Main.getNpc(uuid);
        if (npc != null) {
            try {
                npc.remove();
            } catch (fr.milekat.milenpc.api.exceptions.ApiUnavailable ignore) {}
        }
    }
}
