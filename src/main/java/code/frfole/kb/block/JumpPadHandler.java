package code.frfole.kb.block;

import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

public class JumpPadHandler implements BlockHandler {
    private static final NamespaceID NAMESPACE_ID = NamespaceID.from("kbffa:jump_pad");
    public static final JumpPadHandler INSTANCE = new JumpPadHandler();
    public static final long DECAY_TIME = 5000L;
    public static final Tag<Long> DECAY_TIME_TAG = Tag.Long("decay_time").defaultValue(Long.MAX_VALUE);
    public static final Tag<Long> LAST_JUMP_TIME_TAG = Tag.Long("last_jump_time").defaultValue(0L);
    public static final Tag<Float> JUMP_POWER_TAG = Tag.Float("jump_power").defaultValue(0f);

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return NAMESPACE_ID;
    }

    @Override
    public void onTouch(@NotNull Touch touch) {
        Entity entity = touch.getTouching();
        if (entity.getTag(LAST_JUMP_TIME_TAG) < System.currentTimeMillis() - 500L) {
            float power = touch.getBlock().getTag(JUMP_POWER_TAG);
            Vec velocity = entity.getVelocity()
                    .add(entity.getPosition().direction().mul(power * 0.95, 0, power * 0.95)
                            .add(0, power, 0)
                    );
            entity.setVelocity(velocity);
            entity.setTag(LAST_JUMP_TIME_TAG, System.currentTimeMillis());
        }
    }

    @Override
    public void tick(@NotNull Tick tick) {
        if (tick.getBlock().getTag(DECAY_TIME_TAG) < System.currentTimeMillis()) {
            tick.getInstance().setBlock(tick.getBlockPosition(), Block.AIR);
        }
    }

    @Override
    public boolean isTickable() {
        return true;
    }
}
