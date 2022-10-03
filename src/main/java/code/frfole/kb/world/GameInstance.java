package code.frfole.kb.world;

import code.frfole.kb.StructureBlockProcessor;
import code.frfole.kb.world.zone.Flag;
import code.frfole.kb.world.zone.Zone;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.DynamicChunk;
import net.minestom.server.instance.IChunkLoader;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.chunk.ChunkUtils;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class GameInstance extends InstanceContainer {

    private final Map<String, Zone> zones;

    @SuppressWarnings("UnstableApiUsage")
    public GameInstance(@NotNull UUID uniqueId, @NotNull DimensionType dimensionType, @Nullable IChunkLoader loader) {
        super(uniqueId, dimensionType, loader);

        int range = 10;

        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        AtomicInteger counter = new AtomicInteger(0);
        for (int x = -range; x <= range; ++x) {
            for (int z = -range; z <= range; ++z) {
                loadChunk(x, z).thenAccept(chunk -> {
                    if (counter.incrementAndGet() == (range * 2 + 1) * (range * 2 + 1)) {
                        completableFuture.complete(null);
                    }
                });
            }
        }
        completableFuture.join();
        HashSet<Point> toAir = new HashSet<>();
        HashMap<Point, StructureBlockProcessor.BlockData> toProcess = new HashMap<>();
        ChunkUtils.forChunksInRange(0, 0, 10, (chunkX, chunkZ) -> {
            Chunk chunk = getChunk(chunkX, chunkZ);
            if (chunk instanceof DynamicChunk dynamicChunk) {
                try {
                    Field field = dynamicChunk.getClass().getDeclaredField("entries");
                    field.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    Int2ObjectOpenHashMap<Block> entries = (Int2ObjectOpenHashMap<Block>) field.get(dynamicChunk);
                    for (Map.Entry<Integer, Block> value : entries.int2ObjectEntrySet()) {
                        Point blockPosition = ChunkUtils.getBlockPosition(value.getKey(), chunkX, chunkZ);
                        Block block = value.getValue();
                        if (Block.STRUCTURE_BLOCK.compare(block)) {
                            StructureBlockProcessor.BlockData data = StructureBlockProcessor.extract(block);
                            if (data != null) {
                                toProcess.put(blockPosition, data);
                            }
                            toAir.add(blockPosition);
                            System.out.println(blockPosition);
                        }
                    }
                } catch (NoSuchFieldException | IllegalAccessException ignored) { }
            }
        });
        for (Point point : toAir) {
            setBlock(point, Block.AIR);
        }
        HashMap<String, Point> zonesQueue = new HashMap<>();
        HashMap<String, Zone> zonesTemp = new HashMap<>();
        for (Map.Entry<Point, StructureBlockProcessor.BlockData> entry : toProcess.entrySet()) {
            String value = entry.getValue().value();
            Point blockPos = entry.getKey();
            System.out.println(value);
            switch (entry.getValue().mode()) {
                case SAVE, LOAD -> { }
                case CORNER -> {
                    if (value.equals("core:spawn")) {
                        setTag(Tag.Structure("spawn", Pos.class), Pos.fromPoint(blockPos).add(0.5, 0, 0.5));
                    } else if (value.startsWith("config:")) {
                        String[] split = value.substring(7).split("/");
                        if (split.length != 2) throw new RuntimeException("Invalid config: " + value);
                        String entryName = split[0];
                        String entryValue = split[1];
                        switch (entryName) {
                            case "player_low" -> setTag(Tag.Integer("player_low"), Integer.parseInt(entryValue));
                            case "pearl_low" -> setTag(Tag.Integer("pearl_low"), Integer.parseInt(entryValue));
                        }
                    } else if (value.startsWith("zone:")) {
                        String[] split = value.substring(5).split("/");
                        String zoneName = split[0];
                        if (zonesTemp.containsKey(zoneName)) {
                            throw new RuntimeException("Zone " + zoneName + " already exists");
                        }
                        if (zonesQueue.containsKey(zoneName)) {
                            zonesTemp.put(zoneName, new Zone(zonesQueue.get(zoneName), blockPos, Flag.parse(split[1])));
                            zonesQueue.remove(zoneName);
                        } else {
                            zonesQueue.put(zoneName, blockPos);
                        }
                    }
                }
            }
        }
        if (!zonesQueue.isEmpty()) throw new RuntimeException("Zone " + zonesQueue.keySet().iterator().next() + " is missing a corner");
        zones = Map.copyOf(zonesTemp);

        zonesTemp.forEach((s, zone) -> System.out.println(s + " " + zone));

        enableAutoChunkLoad(false);

        eventNode().addListener(PlayerMoveEvent.class, event -> {
            if (event.getNewPosition().y() < getTag(Tag.Integer("player_low").defaultValue(0))) {
                event.getPlayer().teleport(getTag(Tag.Structure("spawn", Pos.class).defaultValue(Pos.ZERO)));
            }
        });
    }
}