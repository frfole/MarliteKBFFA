package code.frfole.kb;

import net.minestom.server.tag.Tag;

public final class Tags {
    public static final Tag<Boolean> HAS_FLYING_PEARL = Tag.Boolean("hasFlyingPearl").defaultValue(false);
    public static final Tag<Long> BOW_CHARGE_START = Tag.Long("bowChargeStart").defaultValue(Long.MAX_VALUE);
}
