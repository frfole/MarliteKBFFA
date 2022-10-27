package code.frfole.kb.game;

import code.frfole.kb.Statistic;
import code.frfole.kb.Tags;
import net.minestom.server.ServerProcess;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.inventory.TransactionOption;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.*;
import org.jglrxavpok.hephaistos.nbt.mutable.MutableNBTCompound;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public final class PlayerManager {

    private final EventNode<Event> eventNode = EventNode.all("playerManager");

    public PlayerManager() {
        eventNode.addListener(PlayerLoginEvent.class, this::onLogin);
        eventNode.addListener(PlayerDisconnectEvent.class, this::onDisconnect);
    }

    private void onLogin(@NotNull PlayerLoginEvent event) {
        UUID uuid = event.getPlayer().getUuid();
        try (FileInputStream inputStream = new FileInputStream("players/" + uuid + ".dat")) {
            deserialize(event.getPlayer(), inputStream);
        } catch (FileNotFoundException ignored) {
        } catch (IOException | NBTException e) {
            e.printStackTrace();
            event.getPlayer().kick("Failed to load your data");
        }
    }

    private void onDisconnect(@NotNull PlayerDisconnectEvent event) {
        UUID uuid = event.getPlayer().getUuid();
        File file = new File("players/" + uuid + ".dat");
        if (!file.isFile()) {
            //noinspection ResultOfMethodCallIgnored
            file.getParentFile().mkdirs();
        }
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            serialize(event.getPlayer(), outputStream);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public void hook(@NotNull ServerProcess serverProcess) {
        serverProcess.connection().setUuidProvider(PlayerManager::uuidProvider);
        serverProcess.eventHandler().addChild(eventNode);
    }

    @NotNull
    @Contract(pure = true, value = "_,_ -> new")
    private static UUID uuidProvider(PlayerConnection ignoredConnection, @NotNull String player) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + player).getBytes(StandardCharsets.UTF_8));
    }

    public static final List<ItemStack> DEFAULT_INVENTORY = List.of(
            ItemStack.of(Material.STONE, 64),
            ItemStack.of(Material.GRAY_CARPET, 1),
            ItemStack.of(Material.ENDER_PEARL, 1),
            ItemStack.of(Material.STICK, 1),
            ItemStack.of(Material.BOW, 1),
            ItemStack.of(Material.ARROW, 1)
    );

    public static void refillItems(@NotNull Player player) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.addItemStacks(DEFAULT_INVENTORY, TransactionOption.ALL);
    }

    public static void setStatus(@NotNull Player player, @NotNull Instance instance) {
        player.setRespawnPoint(instance.getTag(Tags.MAP_SPAWN));
        player.setGameMode(GameMode.SURVIVAL);
    }

    public static void serialize(@NotNull Player player, @NotNull OutputStream outputStream) throws IOException {
        MutableNBTCompound nbt = new MutableNBTCompound();
        for (Statistic<?> value : Statistic.VALUES) {
            value.writeToNBT(player, nbt);
        }
        NBTWriter writer = new NBTWriter(outputStream);
        writer.writeNamed("player", nbt.toCompound());
        writer.close();
    }

    public static void deserialize(@NotNull Player player, @NotNull InputStream inputStream) throws IOException, NBTException {
        NBTReader reader = new NBTReader(inputStream);
        NBT read = reader.read();
        if (read instanceof NBTCompound compound) {
            for (Statistic<?> value : Statistic.VALUES) {
                value.readFromNBT(player, compound);
            }
        }
        reader.close();
    }
}
