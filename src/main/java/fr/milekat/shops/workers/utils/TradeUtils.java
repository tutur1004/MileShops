package fr.milekat.shops.workers.utils;

import fr.milekat.shops.Main;

public class TradeUtils {
    public static String getMaterial(String material) {
        return Main.getConfigs().getString("materials-font." + material, material);
    }
}
