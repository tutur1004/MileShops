package fr.milekat.shops.listeners;

import fr.milekat.shops.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class DefaultTags implements Listener {

    @EventHandler
    public void setPlayerTags(@NotNull PlayerJoinEvent event) {
        if (Main.getConfigs().getBoolean("default_tags", true)) {
            Main.PLAYER_TAGS.put(event.getPlayer().getUniqueId(), Map.of("name", event.getPlayer().getName(),
                    "uuid", event.getPlayer().getUniqueId().toString()));
        }
    }

    @EventHandler
    public void removePlayerTags(@NotNull PlayerQuitEvent event) {
        if (Main.getConfigs().getBoolean("default_tags", true)) {
            Main.PLAYER_TAGS.remove(event.getPlayer().getUniqueId());
        }
    }
}
