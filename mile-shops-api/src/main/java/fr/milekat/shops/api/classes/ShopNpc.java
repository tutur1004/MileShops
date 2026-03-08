package fr.milekat.shops.api.classes;

import org.bukkit.Location;

import java.util.UUID;

/**
 * Wrapper for NPC data, used to store NPCs in shops without depending on the NPC library
 */
public record ShopNpc(UUID uuid, String name, Location location,
                      boolean spawned, boolean shouldBeSpawned, boolean nameplateVisible,
                      String texture, String signature) {

    public ShopNpc(UUID uuid, String name) {
        this(uuid, name, null, false,
                false, true, null, null);
    }
}
