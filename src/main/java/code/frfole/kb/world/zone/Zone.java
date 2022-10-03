package code.frfole.kb.world.zone;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public record Zone(Point posMin, Point posMax, Set<Flag> flags) {
    public Zone {
        Point tempPos1 = posMin;
        Point tempPos2 = posMax;
        posMin = new Vec(Math.min(tempPos1.x(), tempPos2.x()), Math.min(tempPos1.y(), tempPos2.y()), Math.min(tempPos1.z(), tempPos2.z()));
        posMax = new Vec(Math.max(tempPos1.x(), tempPos2.x()), Math.max(tempPos1.y(), tempPos2.y()), Math.max(tempPos1.z(), tempPos2.z()));
        flags = Set.copyOf(flags);
    }

    /**
     * Checks if the given point is inside the zone.
     * @param point The point to check.
     * @return {@code true} if the point is inside the zone, {@code false} otherwise.
     */
    @Contract(pure = true)
    public boolean contains(Point point) {
        return point.x() >= posMin.x() && point.x() <= posMax.x() && point.y() >= posMin.y() && point.y() <= posMax.y() && point.z() >= posMin.z() && point.z() <= posMax.z();
    }

    /**
     * Gets the value of given flag.
     * @param flagType The flag type to get.
     * @return The value of the flag, or {@code null} if the flag is not set.
     */
    @Contract(pure = true)
    public @Nullable Boolean flagValue(Flag.FlagType flagType) {
        for (Flag flag : flags) {
            if (flag.type() == flagType) {
                return flag.value();
            }
        }
        return null;
    }
}
