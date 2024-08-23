package fr.milekat.shops.workers.utils;

import fr.milekat.milenpc.api.classes.NPC;
import fr.milekat.shops.Main;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class NPCUtils {
    public static @NotNull NPC create(UUID uuid, String name, Location location) {
        NPC npc = Main.getNpcManager().create(uuid, name);
        npc.teleport(location);
        npc.show();
        return npc;
    }

    public static void teleport(@NotNull UUID uuid, Location location) {
        NPC npc = Main.getNpc(uuid);
        if (npc != null) {
            npc.teleport(location);
        }
    }

    public static void updateSkin(@NotNull UUID uuid, String texture, String signature) {
        NPC npc = Main.getNpc(uuid);
        if (npc != null) {
            npc.updateSkin(texture, signature);
        }
    }

    public static void ensureVisible(@NotNull UUID uuid) {
        NPC npc = Main.getNpc(uuid);
        if (npc != null) {
            npc.show();
        }
    }

    public static void ensureInvisible(@NotNull UUID uuid) {
        NPC npc = Main.getNpc(uuid);
        if (npc != null) {
            npc.hide();
        }
    }

    public static void destroy(@NotNull UUID uuid) {
        NPC npc = Main.getNpc(uuid);
        if (npc != null) {
            npc.remove();
        }
    }
}
