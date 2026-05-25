package fr.milekat.shops.workers.utils;

import fr.milekat.shops.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.jetbrains.annotations.NotNull;
import fr.milekat.utils.lib.json.JSONArray;
import fr.milekat.utils.lib.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TagExporter {
    public static void asyncExportItemTags(@NotNull File dataFolder) {
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> exportItemTags(dataFolder));
    }

    public static void exportItemTags(@NotNull File dataFolder) {
        JSONObject families = new JSONObject();
        int tagCount = 0;
        for (Tag<Material> tag : Bukkit.getTags(Tag.REGISTRY_ITEMS, Material.class)) {
            String family = tag.getKey().getNamespace();
            String name = tag.getKey().getKey();
            List<String> materials = new ArrayList<>();
            for (Material m : tag.getValues()) materials.add(m.name());
            Collections.sort(materials);
            JSONObject familyObj = families.optJSONObject(family);
            if (familyObj == null) {
                familyObj = new JSONObject();
                families.put(family, familyObj);
            }
            familyObj.put(name, new JSONArray(materials));
            tagCount++;
        }
        File out = new File(dataFolder, "item-tags.json");
        try {
            if (!dataFolder.exists()) //noinspection ResultOfMethodCallIgnored
                dataFolder.mkdirs();
            Files.writeString(out.toPath(), families.toString(4));
            Main.getMileLogger().info("Exported " + tagCount + " item tags to " + out.getName());
        } catch (IOException e) {
            Main.getMileLogger().warning("Failed to export item tags: " + e.getMessage());
        }
    }
}
