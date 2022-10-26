package code.frfole.kb;

import net.minestom.server.entity.Entity;
import net.minestom.server.tag.Tag;

import java.util.UUID;

public final class Tags {
    public static final Tag<Boolean> HAS_FLYING_PEARL = Tag.Boolean("hasFlyingPearl").defaultValue(false);
    public static final Tag<Long> BOW_CHARGE_START = Tag.Long("bowChargeStart").defaultValue(Long.MAX_VALUE);
    @SuppressWarnings("UnstableApiUsage")
    public static final Tag<HitRecord> LAST_HIT = Tag.Structure("lastHit", HitRecord.class);
    public static final Tag<Integer> VOTED_MAP = Tag.Integer("votedMap").defaultValue(-1);

    public record HitRecord(UUID attacker, long time) {
        public HitRecord(Entity attacker) {
            this(attacker.getUuid(), System.currentTimeMillis());
        }
    }
}
