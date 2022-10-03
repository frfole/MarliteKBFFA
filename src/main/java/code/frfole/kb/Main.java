package code.frfole.kb;

import code.frfole.kb.world.GameInstance;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.AnvilLoader;
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
            event.getPlayer().setRespawnPoint(gameMap.getTag(Tag.Structure("spawn", Pos.class).defaultValue(Pos.ZERO)));
            event.getPlayer().setGameMode(GameMode.CREATIVE);
        });

        server.start("localhost", 25565);
    }
}
