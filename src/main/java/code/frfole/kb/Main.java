package code.frfole.kb;

import code.frfole.kb.block.DecayHandler;
import code.frfole.kb.block.JumpPadHandler;
import code.frfole.kb.entity.EnderPearlEntity;
import code.frfole.kb.world.GameInstance;
import code.frfole.kb.world.zone.Flag;
import code.frfole.kb.world.zone.Zone;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.AnvilLoader;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.SetCooldownPacket;
import net.minestom.server.tag.Tag;
import net.minestom.server.world.DimensionType;

import java.util.UUID;

public class Main {
    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init();

        GameInstance gameMap = new GameInstance(UUID.randomUUID(), DimensionType.OVERWORLD, new AnvilLoader("worlds/KBFFA-test-alpha"));
        MinecraftServer.getInstanceManager().registerInstance(gameMap);

        MinecraftServer.getGlobalEventHandler().addListener(PlayerLoginEvent.class, event -> {
            event.setSpawningInstance(gameMap);
            Player player = event.getPlayer();
            //noinspection UnstableApiUsage
            player.setRespawnPoint(gameMap.getTag(Tag.Structure("spawn", Pos.class).defaultValue(Pos.ZERO)));
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().setItemStack(0, ItemStack.of(Material.STONE, 64));
            player.getInventory().setItemStack(1, ItemStack.of(Material.GRAY_CARPET, 1));
            player.getInventory().setItemStack(2, ItemStack.of(Material.ENDER_PEARL, 1));
            player.getInventory().setItemStack(3, ItemStack.of(Material.STICK, 1));
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerBlockPlaceEvent.class, event -> {
            Player player = event.getPlayer();
            if (player.getInstance() instanceof GameInstance gameInstance) {
                Boolean flagValue = Zone.flagValue(gameInstance.zones.values(), Flag.FlagType.PLACE, event.getBlockPosition());
                if (flagValue != null && flagValue) {
                    ItemStack handItem = player.getItemInHand(event.getHand());
                    event.consumeBlock(false);
                    if (handItem.material() == Material.GRAY_CARPET) {
                        // TODO: consume block
                        event.setBlock(event.getBlock()
                                .withTag(JumpPadHandler.JUMP_POWER_TAG, 30f)
                                .withTag(JumpPadHandler.DECAY_TIME_TAG, System.currentTimeMillis() + JumpPadHandler.DECAY_TIME)
                                .withHandler(JumpPadHandler.INSTANCE));
                    } else if (handItem.material() == Material.STONE) {
                        event.setBlock(event.getBlock().withHandler(DecayHandler.INSTANCE));
                    }
                } else {
                    event.setCancelled(true);
                }
            }
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerMoveEvent.class, event -> {
            Player player = event.getPlayer();
            //noinspection UnstableApiUsage
            if (event.getNewPosition().y() < event.getInstance().getTag(Tag.Integer("player_low").defaultValue(0))
                    && !player.getTag(Tags.HAS_FLYING_PEARL)) {
                //noinspection UnstableApiUsage
                player.sendPacket(new SetCooldownPacket(Material.ENDER_PEARL.id(), 0));
                //noinspection UnstableApiUsage
                player.teleport(event.getInstance().getTag(Tag.Structure("spawn", Pos.class).defaultValue(Pos.ZERO)));
            }
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerUseItemEvent.class, event -> {
            ItemStack itemStack = event.getItemStack();
            Player player = event.getPlayer();
            event.setCancelled(true);
            if (itemStack.material() == Material.ENDER_PEARL) {
                if (!(player.getInstance() instanceof GameInstance gameInstance) || player.getTag(Tags.HAS_FLYING_PEARL)) return;
                Boolean flagValue = Zone.flagValue(gameInstance.zones.values(), Flag.FlagType.PEARL, player.getPosition());
                if (flagValue != null && !flagValue) {
                    //noinspection UnstableApiUsage
                    player.sendPacket(new SetCooldownPacket(Material.ENDER_PEARL.id(), 0));
                    return;
                }
                // TODO: consume ender pearl
                EnderPearlEntity entity = new EnderPearlEntity(player);
                final Pos position = player.getPosition().add(0, player.getEyeHeight(), 0);
                entity.setInstance(gameInstance, position);
                entity.shoot(position.add(entity.getPosition().direction()).sub(0, 0.2, 0), 1.5, 1.0);
            }
        });

        MinecraftServer.getGlobalEventHandler().addListener(ItemDropEvent.class, event -> event.setCancelled(true));

        MinecraftServer.getGlobalEventHandler().addListener(EntityDamageEvent.class, event -> {
            if (event.getDamageType() == DamageType.VOID && event.getEntity() instanceof Player) {
                event.setCancelled(true);
            }
        });

        MinecraftServer.getGlobalEventHandler().addListener(EntityAttackEvent.class, event -> {
            if (event.getEntity() instanceof LivingEntity attacker) {
                ItemStack attackItem = attacker.getItemInMainHand();
                if (attackItem.material() == Material.STICK) {
                    if (!(attacker.getInstance() instanceof GameInstance gameInstance)) return;
                    Boolean flagValueAttacker = Zone.flagValue(gameInstance.zones.values(), Flag.FlagType.SAFE, attacker.getPosition());
                    Boolean flagValueTarget = Zone.flagValue(gameInstance.zones.values(), Flag.FlagType.SAFE, event.getTarget().getPosition());
                    if ((flagValueAttacker != null && flagValueAttacker) || (flagValueTarget != null && flagValueTarget)) {
                        return;
                    }
                    Vec direction = attacker.getPosition().withPitch(0f).direction().neg();
                    event.getTarget().takeKnockback(1.0f, direction.x(), direction.z());
                }
            }
        });

        MinecraftServer.getCommandManager().register(new DebugCommand("debug", "test"));

        server.start("localhost", 25565);
    }
}
