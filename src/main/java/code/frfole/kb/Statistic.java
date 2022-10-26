package code.frfole.kb;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.statistic.PlayerStatistic;
import net.minestom.server.statistic.StatisticCategory;
import net.minestom.server.statistic.StatisticType;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagWritable;
import net.minestom.server.tag.Taggable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.mutable.MutableNBTCompound;

import java.util.Set;
import java.util.function.UnaryOperator;

public record Statistic<T>(@NotNull String name, @NotNull Tag<@NotNull T> tag, @NotNull PlayerStatistic type) {
    public static final Statistic<Integer> DEATHS = new Statistic<>("deaths",
            Tag.Integer("deaths").defaultValue(0),
            new PlayerStatistic(StatisticType.DEATHS)
    );
    public static final Statistic<Integer> KILLS = new Statistic<>("kills",
            Tag.Integer("kills").defaultValue(0),
            new PlayerStatistic(StatisticType.PLAYER_KILLS)
    );
    public static final Statistic<Long> BLOCKS_PLACED = new Statistic<>("blocks_placed",
            Tag.Long("blocks_placed").defaultValue(0L),
            new PlayerStatistic(StatisticCategory.USED, Block.STONE.id())
    );

    public static final Set<Statistic<?>> VALUES = Set.of(DEATHS, KILLS, BLOCKS_PLACED);

    public static final UnaryOperator<Integer> OP_INCREMENT = i -> i + 1;

    public void set(@NotNull TagWritable writable, @Nullable T value) {
        writable.setTag(tag, value);
        if (value instanceof Number number && writable instanceof Player player) {
            player.getStatisticValueMap().put(type, number.intValue());
        }
    }

    public void modify(Taggable handler, UnaryOperator<T> unaryOperator) {
        set(handler, unaryOperator.apply(handler.getTag(tag)));
    }

    public void writeToNBT(@NotNull TagReadable readable, @NotNull MutableNBTCompound nbt) {
        tag.write(nbt, readable.getTag(tag));
    }

    public void readFromNBT(@NotNull TagWritable writable, @NotNull NBTCompound nbt) {
        set(writable, tag.read(nbt));
    }

    public @NotNull T get(@NotNull TagReadable readable) {
        return readable.getTag(tag);
    }
}
