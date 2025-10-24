package fr.milekat.shops.workers.utils;

import net.kyori.adventure.text.format.Style;

/**
 * Utility class for common text styles used across the proxy
 */
@SuppressWarnings("unused")
public class ColorStyles {

    // Style for informational messages
    public static final Style INFO_LABEL = PastelColor.OFF_WHITE.styleDefault();
    public static final Style INFO_VALUE = PastelColor.AZURE.styleUniform(PastelColor.EQUINOX());

    // Styles for error messages
    public static final Style ERROR_LABEL = PastelColor.RED.styleDefaultBold();
    public static final Style ERROR_VALUE = PastelColor.RED_METAL.styleUniform(PastelColor.EQUINOX());

    // Styles for pause messages
    public static final Style PAUSE_LABEL = PastelColor.ORANGE.styleDefaultBold();
    public static final Style PAUSE_VALUE = PastelColor.YELLOW.styleUniform(PastelColor.EQUINOX());

    // Styles for warning messages
    public static final Style WARNING_LABEL = PastelColor.YELLOW.styleDefaultBold();
    public static final Style WARNING_VALUE = PastelColor.ORANGE.styleUniform(PastelColor.EQUINOX());

    // Styles for success messages
    public static final Style ACCENT = PastelColor.LIME.styleUniform(PastelColor.EQUINOX());
    public static final Style SECONDARY = PastelColor.GRAY_LIGHT.styleUniform(PastelColor.EQUINOX());
    public static final Style BULLET = PastelColor.bullet();

    private ColorStyles() {
        throw new UnsupportedOperationException("Utility class");
    }
}