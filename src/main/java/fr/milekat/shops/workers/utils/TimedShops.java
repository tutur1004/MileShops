package fr.milekat.shops.workers.utils;

import fr.milekat.shops.Main;
import fr.milekat.shops.api.classes.Shop;
import fr.milekat.utils.storage.exceptions.StorageExecuteException;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class TimedShops {
    private final BukkitTask task;

    public TimedShops() {
        task = prepareTask();
    }

    public BukkitTask prepareTask() {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(), () -> {
            try {
                // Get all shops
                List<Shop> shops = Main.getStorage().getCacheAllShops();
                // Get shops that should be spawned
                List<Shop> shouldBeSpawned = shops.stream()
                        .filter(Shop::isTimed)
                        .filter(Shop::shouldBeSpawned)
                        .toList();
                // Get shops that should not be spawned
                List<Shop> shouldNotBeSpawned = shops.stream()
                        .filter(Shop::isTimed)
                        .filter(shop -> !shop.shouldBeSpawned())
                        .toList();

                // Ensure that all shops that should be spawned are spawned
                shouldBeSpawned.forEach(shop -> {
                    // Create the NPC
                    NPCUtils.ensureVisible(shop.getNpc().getUuid());
                });
                // Ensure that all shops that should not be spawned are not spawned
                shouldNotBeSpawned.forEach(shop -> {
                    // Destroy the NPC
                    NPCUtils.ensureInvisible(shop.getNpc().getUuid());
                });
            } catch (StorageExecuteException e) {
                Main.getMileLogger().warning("Error while getting all shops from database.");
            }
        }, 600, 600);
    }

    public void cancel() {
        task.cancel();
    }
}
