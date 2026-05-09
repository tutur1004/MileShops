package fr.milekat.shops.workers.listeners;

import fr.milekat.shops.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;

public class ShopsListeners implements Listener {
    @EventHandler
    public void loadShops(PluginEnableEvent event) {
        Main.reloadShops();
    }
}
