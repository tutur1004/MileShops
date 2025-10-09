package fr.milekat.shops.workers.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public enum HeadsUtils {
    ARROW_LEFT("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR" +
            "1cmUvMTliZjMyOTJlMTI2YTEwNWI1NGViYTcxM2FhMWIxNTJkNTQxYTFkODkzODgyOWM1NjM2NGQxNzhlZDIyYmYifX19");


    private final String texture;

    HeadsUtils(String texture) {
        this.texture = texture;
    }

    /**
     * A method used to set the skin of a player skull via a base64 encoded string
     *
     * @param meta   the skull meta to modify
     * @param base64 the base64 encoded string
     */
    private static void setSkinViaBase64(@NotNull SkullMeta meta, String base64) {
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), null);
        profile.setProperty(new ProfileProperty("textures", base64));
        meta.setPlayerProfile(profile);
    }

    public String getTexture() {
        return texture;
    }

    public @NotNull ItemStack getItem() {
        ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();

        assert skullMeta != null;

        setSkinViaBase64(skullMeta, getTexture());
        itemStack.setItemMeta(skullMeta);

        return itemStack;
    }
}
