package fr.milekat.shops.workers.utils;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.format.TextDecoration.BOLD;
import static net.kyori.adventure.text.format.TextDecoration.ITALIC;

/**
 * Enum defining pastel colors and utility methods for text styling
 */
@SuppressWarnings("unused")
public enum PastelColor {
    CARDBOARD     ("#E4C9A7"),
    GREEN         ("#6FB04A"),
    LIME          ("#5ED34E"),
    AZURE         ("#4AA9B0"),
    LIGHT_BLUE    ("#00D5FF"),
    DARK_BLUE     ("#00819B"),
    RED_METAL     ("#A33431"),
    RED           ("#C22C28"),
    OFF_WHITE     ("#EFECE4"),
    OFF_WHITE_DARK("#CBC6B9"),
    ORANGE        ("#FF6017"),
    GRAY_GENERATOR("#6A6E78"),
    GRAY_LIGHT    ("#81858f"),
    PURPLE        ("#D700D7"),
    YELLOW        ("#FFE941"),
    PINK          ("#EE198E");

    private final String hex;
    private volatile TextColor cached;

    PastelColor(String hex) {
        this.hex = hex;
    }

    public String hex() {
        return hex;
    }

    @Contract(pure = true)
    public @NotNull String legacyAmp() {
        return "&" + hex;
    }

    public TextColor tc() {
        TextColor c = cached;
        if (c == null) {
            c = TextColor.fromCSSHexString(hex);
            cached = c;
        }
        return c;
    }

    /* ---------- Styles Ready-to-use ---------- */

    @Contract(" -> new")
    public @NotNull Style styleDefault() {
        return Style.style().decoration(ITALIC, false).color(tc()).build();
    }

    @Contract("_ -> new")
    public @NotNull Style styleUniform(Key fontKey) {
        return Style.style().font(fontKey).decoration(ITALIC, false).color(tc()).build();
    }

    public @NotNull Style styleDefaultBold() {
        return styleDefault().decoration(BOLD, true);
    }

    public @NotNull Style styleUniformBold(Key fontKey) {
        return styleUniform(fontKey).decoration(BOLD, true);
    }

    /* ---------- Utility Styles ---------- */

    @Contract(" -> new")
    public static @NotNull Key EQUINOX() {
        return Key.key("equinox", "font");
    }

    @Contract(" -> new")
    public static @NotNull Style label() {
        return OFF_WHITE.styleDefault();
    }

    @Contract(" -> new")
    public static @NotNull Style value() {
        return AZURE.styleUniform(EQUINOX());
    }

    @Contract("_ -> new")
    public static @NotNull Style equinoxColor(@NotNull PastelColor color) {
        return color.styleUniform(EQUINOX());
    }

    public static @NotNull Style bullet() {
        return OFF_WHITE_DARK.styleDefaultBold();
    }
}