package code.frfole.kb;

import code.frfole.kb.command.DebugCommand;
import code.frfole.kb.command.VoteCommand;
import code.frfole.kb.game.GameManager;
import code.frfole.kb.game.GameMap;
import code.frfole.kb.game.PlayerManager;
import com.google.gson.Gson;
import net.minestom.server.MinecraftServer;

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
        PlayerManager playerManager = new PlayerManager();

        //noinspection UnstableApiUsage
        gameManager.hook(MinecraftServer.process());
        //noinspection UnstableApiUsage
        playerManager.hook(MinecraftServer.process());

        MinecraftServer.getCommandManager().register(new VoteCommand("vote", gameManager));
        MinecraftServer.getCommandManager().register(new DebugCommand("debug", "test"));

        server.start("localhost", 25565);
    }
}
