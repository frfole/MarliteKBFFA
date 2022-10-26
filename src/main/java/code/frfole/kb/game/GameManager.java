package code.frfole.kb.game;

import code.frfole.kb.Tags;
import code.frfole.kb.world.GameInstance;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerProcess;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.SetCooldownPacket;
import net.minestom.server.timer.Task;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.*;

public final class GameManager {
    private final GameMap[] maps;
    private int currentMapIndex = 0;
    private final Map<Integer, GameInstance> instances;
    private final EventNode<Event> eventNode = EventNode.all("gameManager");
    private Task taskSwitchMap;

    public GameManager(GameMap[] maps) {
        this.maps = maps;
        if (maps == null || maps.length == 0) {
            throw new RuntimeException("No maps found");
        }
        Map<Integer, GameInstance> instances = new HashMap<>();
        for (int i = 0; i < maps.length; i++) {
            instances.put(i, new GameInstance(UUID.randomUUID(), maps[i]));
        }
        this.instances = Map.copyOf(instances);
        switchMap();
        eventNode.addListener(PlayerLoginEvent.class, this::onPlayerLogin);
    }

    private void onPlayerLogin(@NotNull PlayerLoginEvent event) {
        GameInstance currentInstance = instances.get(currentMapIndex);
        event.setSpawningInstance(currentInstance);
        Player player = event.getPlayer();
        PlayerManager.refillItems(player);
        PlayerManager.setStatus(player, currentInstance);
    }

    public int pickNextMap(Random random) {
        Map<Integer, Integer> votes = new HashMap<>();
        // count votes for each map
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            int playerVote = player.tagHandler().getTag(Tags.VOTED_MAP);
            if (playerVote == -1 || playerVote >= maps.length || playerVote < 0) {
                continue;
            }
            votes.compute(playerVote, (k, v) -> v == null ? 1 : v + 1);
        }
        if (votes.isEmpty()) { // no one voted
            return random.nextInt(maps.length);
        }
        // find the maps with the most votes
        int max = 0;
        Stack<Integer> maxes = new Stack<>();
        for (Map.Entry<Integer, Integer> entry : votes.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                maxes.clear();
                maxes.push(entry.getKey());
            } else if (entry.getValue() == max) {
                maxes.push(entry.getKey());
            }
        }
        return maxes.get(random.nextInt(maxes.size()));
    }

    @SuppressWarnings("UnstableApiUsage")
    public void hook(ServerProcess serverProcess) {
        assert taskSwitchMap == null;
        for (GameInstance gameInstance : instances.values()) {
            serverProcess.instance().registerInstance(gameInstance);
        }
        taskSwitchMap = serverProcess.scheduler().buildTask(this::switchMap)
                .repeat(Duration.ofMinutes(1))
                .schedule();
        serverProcess.eventHandler().addChild(eventNode);
    }

    private void switchMap() {
        int previousMapIndex = currentMapIndex;
        currentMapIndex = pickNextMap(new Random());
        if (currentMapIndex == previousMapIndex) {
            return;
        }
        GameInstance previousInstance = instances.get(previousMapIndex);
        GameInstance currentInstance = instances.get(currentMapIndex);
        for (Player player : previousInstance.getPlayers()) {
            PlayerManager.refillItems(player);
            PlayerManager.setStatus(player, currentInstance);
            if (player.hasTag(Tags.HAS_FLYING_PEARL)) {
                player.setTag(Tags.HAS_FLYING_PEARL, false);
                //noinspection UnstableApiUsage
                player.sendPacket(new SetCooldownPacket(Material.ENDER_PEARL.id(), 0));
            }
            // we can use getRespawnPoint() because we already set the new respawn point
            player.setInstance(currentInstance, player.getRespawnPoint());
        }
    }

    public GameMap[] getMaps() {
        return maps;
    }
}
