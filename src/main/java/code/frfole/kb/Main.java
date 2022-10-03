package code.frfole.kb;

import code.frfole.kb.block.DecayHandler;
import code.frfole.kb.world.GameInstance;
import code.frfole.kb.world.zone.Flag;
import code.frfole.kb.world.zone.Zone;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.instance.AnvilLoader;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
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
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerBlockPlaceEvent.class, event -> {
            // TODO: Dont make block blink when cancelled if possible
            if (event.getPlayer().getInstance() instanceof GameInstance gameInstance) {
                Boolean flagValue = Zone.flagValue(gameInstance.zones.values(), Flag.FlagType.PLACE, event.getBlockPosition());
                if (flagValue != null && flagValue) {
                    event.consumeBlock(false);
                    event.setBlock(event.getBlock().withHandler(DecayHandler.INSTANCE));
                } else {
                    event.setCancelled(true);
                }
            }
        });

        server.start("localhost", 25565);
    }
}
