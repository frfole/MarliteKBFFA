package code.frfole.kb;

import net.minestom.server.instance.block.Block;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Nullable;

public final class StructureBlockProcessor {

    public static final Tag<Mode> MODE = Tag.String("mode")
            .map(Mode::valueOf, Mode::name);
    public static final Tag<String> NAME = Tag.String("name");

    public static @Nullable BlockData extract(Block block) {
        Mode mode = block.getTag(MODE);
        String value = block.getTag(NAME);
        if (mode == null || value == null) return null;
        return new BlockData(mode, value);
    }

    public record BlockData(Mode mode, String value) { }

    public enum Mode {
        SAVE, LOAD, CORNER
    }
}
