package code.frfole.kb.world;

import code.frfole.kb.CombatListener;
import code.frfole.kb.Statistic;
import code.frfole.kb.StructureBlockProcessor;
import code.frfole.kb.Tags;
import code.frfole.kb.block.DecayHandler;
import code.frfole.kb.block.JumpPadHandler;
import code.frfole.kb.entity.EnderPearlEntity;
import code.frfole.kb.game.GameMap;
import code.frfole.kb.world.zone.Flag;
import code.frfole.kb.world.zone.Zone;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.item.ItemUpdateStateEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerItemAnimationEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.AnvilLoader;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.DynamicChunk;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.SetCooldownPacket;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.chunk.ChunkUtils;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class GameInstance extends InstanceContainer {

    public final Map<String, Zone> zones;

    public GameInstance(@NotNull UUID uniqueId, @NotNull GameMap gameMap) {
        //noinspection UnstableApiUsage
        super(uniqueId, DimensionType.OVERWORLD, new AnvilLoader(gameMap.dir()));

        // load chunks
        Vec sub = gameMap.boundsMax().sub(gameMap.boundsMin()).add(1, 0, 1);
        int totalArea = sub.blockX() * sub.blockZ();
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        AtomicInteger counter = new AtomicInteger(0);
        for (int x = gameMap.boundsMin().blockX(); x <= gameMap.boundsMax().blockX(); ++x) {
            for (int z = gameMap.boundsMin().blockZ(); z <= gameMap.boundsMax().blockZ(); ++z) {
                loadChunk(x, z).thenAccept(chunk -> {
                    if (counter.incrementAndGet() == totalArea) {
                        completableFuture.complete(null);
                    }
                });
            }
        }
        completableFuture.join();

        // scan loaded chunks for structure blocks and replace them with air
        HashSet<Point> toAir = new HashSet<>();
        HashMap<Point, StructureBlockProcessor.BlockData> toProcess = new HashMap<>();
        for (int chunkX = gameMap.boundsMin().blockX(); chunkX <= gameMap.boundsMax().blockX(); ++chunkX) {
            for (int chunkZ = gameMap.boundsMin().blockZ(); chunkZ <= gameMap.boundsMax().blockZ(); ++chunkZ) {
                Chunk chunk = getChunk(chunkX, chunkZ);
                if (chunk instanceof DynamicChunk dynamicChunk) {
                    try {
                        Field field = dynamicChunk.getClass().getDeclaredField("entries");
                        field.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        Int2ObjectOpenHashMap<Block> entries = (Int2ObjectOpenHashMap<Block>) field.get(dynamicChunk);
                        for (Map.Entry<Integer, Block> value : entries.int2ObjectEntrySet()) {
                            //noinspection UnstableApiUsage
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
            }
        }
        for (Point point : toAir) {
            setBlock(point, Block.AIR);
        }

        // process structure blocks
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
                        setTag(Tags.MAP_SPAWN, Pos.fromPoint(blockPos).add(0.5, 0, 0.5));
                    } else if (value.equals("core:jump_pad")) {
                        setBlock(blockPos, Block.GRAY_CARPET
                                .withTag(JumpPadHandler.JUMP_POWER_TAG, 30f)
                                .withTag(JumpPadHandler.DECAY_TIME_TAG, Long.MAX_VALUE)
                                .withHandler(JumpPadHandler.INSTANCE));
                    } else if (value.startsWith("core:block/")) {
                        Block newBlock = Block.fromNamespaceId(value.substring(11));
                        if (newBlock != null) {
                            setBlock(blockPos, newBlock);
                        } else {
                            System.err.println("Unknown block: " + value);
                        }
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
        //noinspection UnstableApiUsage
        eventNode().addListener(EntityAttackEvent.class, CombatListener::onAttack)
                .addListener(ItemUpdateStateEvent.class, CombatListener::onItemUpdateState)
                .addListener(PlayerItemAnimationEvent.class, event -> {
                    if (event.getItemAnimationType() == PlayerItemAnimationEvent.ItemAnimationType.BOW)
                        event.getPlayer().setTag(Tags.BOW_CHARGE_START, System.currentTimeMillis());
                })
                .addListener(EntityDamageEvent.class, event -> {
                    if (event.getDamageType() == DamageType.VOID && event.getEntity() instanceof Player)
                        event.setCancelled(true);
                })
                .addListener(ItemDropEvent.class, event -> event.setCancelled(true))
                .addListener(PlayerBlockPlaceEvent.class, this::onPlace)
                .addListener(PlayerTickEvent.class, this::onPlayerTick)
                .addListener(PlayerUseItemEvent.class, this::onPlayerUseItem);
    }

    private void onPlace(@NotNull PlayerBlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!Zone.flagValue(this.zones.values(), Flag.FlagType.PLACE, event.getBlockPosition(), false)) {
            event.setCancelled(true);
            return;
        }
        ItemStack handItem = player.getItemInHand(event.getHand());
        event.consumeBlock(false);
        if (handItem.material() == Material.GRAY_CARPET) {
            // TODO: consume block
            event.setBlock(event.getBlock()
                    .withTag(JumpPadHandler.JUMP_POWER_TAG, 30f)
                    .withTag(JumpPadHandler.DECAY_TIME_TAG, System.currentTimeMillis() + JumpPadHandler.DECAY_TIME)
                    .withHandler(JumpPadHandler.INSTANCE));
        } else if (handItem.material() == Material.STONE) {
            Statistic.BLOCKS_PLACED.modify(player, i -> i + 1L);
            event.setBlock(event.getBlock().withHandler(DecayHandler.INSTANCE));
        }
    }

    private void onPlayerTick(@NotNull PlayerTickEvent event) {
        Player player = event.getPlayer();
        if (player.getPosition().y() < this.getTag(Tag.Integer("player_low").defaultValue(0))
                && !player.getTag(Tags.HAS_FLYING_PEARL)) {
            Statistic.DEATHS.modify(player, Statistic.OP_INCREMENT);
            Tags.HitRecord hitRecord = player.getTag(Tags.LAST_HIT);
            if (hitRecord != null && !hitRecord.attacker().equals(player.getUuid()) && hitRecord.time() < System.currentTimeMillis() + 15000) {
                Player attacker = MinecraftServer.getConnectionManager().getPlayer(hitRecord.attacker());
                if (attacker != null) {
                    Statistic.KILLS.modify(attacker, Statistic.OP_INCREMENT);
                }
                player.removeTag(Tags.LAST_HIT);
            }
            //noinspection UnstableApiUsage
            player.sendPacket(new SetCooldownPacket(Material.ENDER_PEARL.id(), 0));
            player.teleport(this.getTag(Tags.MAP_SPAWN));
        }
    }

    private void onPlayerUseItem(@NotNull PlayerUseItemEvent event) {
        ItemStack itemStack = event.getItemStack();
        Player player = event.getPlayer();
        if (itemStack.material() == Material.ENDER_PEARL) {
            event.setCancelled(true);
            if (player.getTag(Tags.HAS_FLYING_PEARL)) return;
            if (!Zone.flagValue(this.zones.values(), Flag.FlagType.PEARL, player.getPosition(), true)) {
                //noinspection UnstableApiUsage
                player.sendPacket(new SetCooldownPacket(Material.ENDER_PEARL.id(), 0));
                return;
            }
            // TODO: consume ender pearl
            EnderPearlEntity entity = new EnderPearlEntity(player);
            final Pos position = player.getPosition().add(0, player.getEyeHeight(), 0);
            entity.setInstance(this, position);
            entity.shoot(position.add(entity.getPosition().direction()).sub(0, 0.2, 0), 1.5, 1.0);
        }
    }
}
