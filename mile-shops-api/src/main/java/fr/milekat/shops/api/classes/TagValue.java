package fr.milekat.shops.api.classes;

import org.jetbrains.annotations.NotNull;

/**
 * Typed wrapper for a player-tag value, used across the public API anywhere a tag value
 * has to be passed. The five permitted shapes mirror the types accepted by the {@code tags}
 * config — see {@code fr.milekat.shops.Main#TAGS}: {@code String}, {@code int},
 * {@code long}, {@code double}, {@code boolean}.
 *
 * <p>Construct via one of the {@link #of} factories rather than the records directly:</p>
 * <pre>{@code
 * api.lockTrade(trade, "team",  TagValue.of("red"));
 * api.lockTrade(trade, "level", TagValue.of(42));
 * }</pre>
 *
 * <p>Use {@link #raw()} when you need to bridge to {@code Map<String, Object>} APIs
 * (e.g. {@code Main.PLAYER_TAGS}). Equality matches the boxed underlying value, so a
 * {@code TagValue.of(5)} compares equal across two calls but not against
 * {@code TagValue.of(5L)} — the types must match exactly, like the original tag.</p>
 */
public sealed interface TagValue {

    /** @return the boxed Java value backing this wrapper. */
    @NotNull Object raw();

    static @NotNull TagValue of(@NotNull String value) { return new OfString(value); }
    static @NotNull TagValue of(int value)             { return new OfInt(value); }
    static @NotNull TagValue of(long value)            { return new OfLong(value); }
    static @NotNull TagValue of(double value)          { return new OfDouble(value); }
    static @NotNull TagValue of(boolean value)         { return new OfBoolean(value); }

    record OfString(@NotNull String value) implements TagValue {
        @Override public @NotNull Object raw() { return value; }
    }
    record OfInt(int value) implements TagValue {
        @Override public @NotNull Object raw() { return value; }
    }
    record OfLong(long value) implements TagValue {
        @Override public @NotNull Object raw() { return value; }
    }
    record OfDouble(double value) implements TagValue {
        @Override public @NotNull Object raw() { return value; }
    }
    record OfBoolean(boolean value) implements TagValue {
        @Override public @NotNull Object raw() { return value; }
    }
}
