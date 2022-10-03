package code.frfole.kb.world.zone;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a flag inside a zone.
 * @param type The type of the flag.
 * @param value The value of the flag.
 */
public record Flag(@NotNull FlagType type, boolean value) {

    /**
     * Parses stream of flags in format {@code flag1.flag2.-flag3.-flagN}.
     * @param input The input to parse.
     * @return The parsed flags.
     */
    @Contract(pure = true)
    public static @NotNull Set<Flag> parse(String input) {
        HashSet<Flag> flags = new HashSet<>();
        // split by .
        String[] parts = input.split("\\.");
        for (String part : parts) {
            // check if it's a negative flag and parse flag type
            if (part.isEmpty()) continue;
            if (part.startsWith("-")) {
                FlagType type = FlagType.fromName(part.substring(1));
                if (type == null) continue;
                flags.add(new Flag(type, false));
            } else {
                FlagType type = FlagType.fromName(part);
                if (type == null) continue;
                flags.add(new Flag(type, true));
            }
        }
        return Set.copyOf(flags);
    }

    /**
     * Represents a flag type.
     */
    public enum FlagType {
        PLACE,
        SAFE,
        PEARL,
        BOW;

        @Contract(pure = true)
        public static @Nullable FlagType fromName(String name) {
            return switch (name) {
                case "place" -> PLACE;
                case "safe" -> SAFE;
                case "pearl" -> PEARL;
                case "bow" -> BOW;
                default -> null;
            };
        }
    }
}
