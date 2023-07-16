package fr.milekat.shops.workers.utils;

import dev.sergiferry.playernpc.api.NPC;
import fr.milekat.shops.Main;

import java.util.UUID;

public class NPCUtils {
    public static void syncDestroy(UUID uuid) {
        NPC.Global npc = Main.getNpc(uuid);
        if (npc!=null) Main.bukkitSync(npc::destroy);
    }

    public static void forceUpdate(UUID uuid) {
        NPC.Global npc = Main.getNpc(uuid);
        if (npc!=null) npc.forceUpdate();
    }
}
