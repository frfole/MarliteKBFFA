package code.frfole.kb;

import code.frfole.kb.block.DecayHandler;
import code.frfole.kb.block.JumpPadHandler;
import code.frfole.kb.command.VoteCommand;
import code.frfole.kb.entity.EnderPearlEntity;
import code.frfole.kb.game.GameManager;
import code.frfole.kb.game.GameMap;
import code.frfole.kb.world.GameInstance;
import code.frfole.kb.world.zone.Flag;
import code.frfole.kb.world.zone.Zone;
import com.google.gson.Gson;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.entity.EntityDamageEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.item.ItemUpdateStateEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerItemAnimationEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.SetCooldownPacket;
import net.minestom.server.tag.Tag;

import java.io.FileReader;
import java.io.IOException;

public class Main {
    private static final Gson GSON = new Gson();
    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init();
        GameMap[] maps;
        try (FileReader fileReader = new FileReader("maps.json")) {
            maps = GSON.fromJson(fileReader, GameMap[].class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        GameManager gameManager = new GameManager(maps);
        //noinspection UnstableApiUsage
        gameManager.hook(MinecraftServer.process());
        MinecraftServer.getCommandManager().register(new VoteCommand("vote", gameManager));

        MinecraftServer.getGlobalEventHandler()
                .addListener(PlayerBlockPlaceEvent.class, event -> {
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
                })
                .addListener(PlayerMoveEvent.class, event -> {
                    Player player = event.getPlayer();
                    //noinspection UnstableApiUsage
                    if (event.getNewPosition().y() < event.getInstance().getTag(Tag.Integer("player_low").defaultValue(0))
                            && !player.getTag(Tags.HAS_FLYING_PEARL)) {
                        //noinspection UnstableApiUsage
                        player.sendPacket(new SetCooldownPacket(Material.ENDER_PEARL.id(), 0));
                        //noinspection UnstableApiUsage
                        player.teleport(event.getInstance().getTag(Tag.Structure("spawn", Pos.class).defaultValue(Pos.ZERO)));
                    }
                })
                .addListener(PlayerUseItemEvent.class, event -> {
                    ItemStack itemStack = event.getItemStack();
                    Player player = event.getPlayer();
                    if (itemStack.material() == Material.ENDER_PEARL) {
                        event.setCancelled(true);
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
                })
                .addListener(ItemDropEvent.class, event -> event.setCancelled(true))
                .addListener(EntityDamageEvent.class, event -> {
                    if (event.getDamageType() == DamageType.VOID && event.getEntity() instanceof Player) {
                        event.setCancelled(true);
                    }
                })
                .addListener(EntityAttackEvent.class, CombatListener::onAttack)
                .addListener(PlayerItemAnimationEvent.class, event -> {
                    if (event.getItemAnimationType() == PlayerItemAnimationEvent.ItemAnimationType.BOW)
                        event.getPlayer().setTag(Tags.BOW_CHARGE_START, System.currentTimeMillis());
                })
                .addListener(ItemUpdateStateEvent.class, CombatListener::onItemUpdateState);

        MinecraftServer.getCommandManager().register(new DebugCommand("debug", "test"));

        server.start("localhost", 25565);
    }
}
