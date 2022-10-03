package code.frfole.kb.block;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.network.packet.server.play.BlockBreakAnimationPacket;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

public class DecayHandler implements BlockHandler {
    private static final NamespaceID NAMESPACE_ID = NamespaceID.from("kbffa:decay");
    public static final DecayHandler INSTANCE = new DecayHandler();
    public static final Tag<Integer> DECAY_TAG = Tag.Integer("decay").defaultValue(0);
    public static final Tag<Long> DECAY_TIME_TAG = Tag.Long("decay_time").defaultValue(0L);
    public static final Tag<Integer> DECAY_ID_TAG = Tag.Integer("decay_id");
    public static final long[] stages = new long[] {
            500, // place -> 0
            500, // 0 -> 1
            500, // 1 -> 2
            500, // 2 -> 3
            500, // 3 -> 4
            500, // 4 -> 5
            500, // 5 -> 6
            500, // 6 -> 7
            500, // 7 -> 8
            500, // 8 -> 9
            500, // 9 -> break
    };

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return NAMESPACE_ID;
    }

    @Override
    public void tick(@NotNull Tick tick) {
        Block block = tick.getBlock();
        long decayNext = block.getTag(DECAY_TIME_TAG);
        if (decayNext > System.currentTimeMillis()) {
            return;
        }
        int decay = block.getTag(DECAY_TAG);
        int decayId = block.getTag(DECAY_ID_TAG.defaultValue(ThreadLocalRandom.current().nextInt()));
        if (decay >= stages.length) {
            tick.getInstance().setBlock(tick.getBlockPosition(), Block.AIR);
            return;
        }
        long decayTime = stages[decay];
        Block newBlock = block.withTag(DECAY_TIME_TAG, System.currentTimeMillis() + decayTime)
                .withTag(DECAY_ID_TAG, decayId)
                .withTag(DECAY_TAG, decay + 1);
        tick.getInstance().setBlock(tick.getBlockPosition(), newBlock);
        tick.getInstance().sendGroupedPacket(new BlockBreakAnimationPacket(decayId, tick.getBlockPosition(), (byte) (decay - 1)));
    }

    @Override
    public boolean isTickable() {
        return true;
    }
}
