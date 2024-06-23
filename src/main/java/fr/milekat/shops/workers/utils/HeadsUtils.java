package fr.milekat.shops.workers.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import fr.milekat.shops.Main;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.UUID;

public enum HeadsUtils {
    ARROW_LEFT("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR" +
            "1cmUvMTliZjMyOTJlMTI2YTEwNWI1NGViYTcxM2FhMWIxNTJkNTQxYTFkODkzODgyOWM1NjM2NGQxNzhlZDIyYmYifX19");


    private final String texture;

    HeadsUtils(String texture) {
        this.texture = texture;
    }

    public String getTexture() {
        return texture;
    }

    public ItemStack getItem() {
        ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();

        assert skullMeta != null;

        setSkinViaBase64(skullMeta, getTexture());
        itemStack.setItemMeta(skullMeta);

        return itemStack;
    }

    /**
     * A method used to set the skin of a player skull via a base64 encoded string
     *
     * @param meta the skull meta to modify
     * @param base64 the base64 encoded string
     */
    private static void setSkinViaBase64(SkullMeta meta, String base64) {
        Field profileField;
        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), "skull-texture");
            profile.getProperties().put("textures", new Property("textures", base64));
            profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (IllegalAccessException | NoSuchFieldException exception) {
            Main.getMileLogger().warning("There was a severe internal reflection " +
                    "error when attempting to set the skin of a player skull via base64!");
            exception.printStackTrace();
        }
    }
}
