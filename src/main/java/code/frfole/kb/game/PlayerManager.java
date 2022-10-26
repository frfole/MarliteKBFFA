package code.frfole.kb.game;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.inventory.TransactionOption;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;

import java.util.List;

public final class PlayerManager {
    public static final List<ItemStack> DEFAULT_INVENTORY = List.of(
            ItemStack.of(Material.STONE, 64),
            ItemStack.of(Material.GRAY_CARPET, 1),
            ItemStack.of(Material.ENDER_PEARL, 1),
            ItemStack.of(Material.STICK, 1),
            ItemStack.of(Material.BOW, 1),
            ItemStack.of(Material.ARROW, 1)
    );

    public static void refillItems(Player player) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.addItemStacks(DEFAULT_INVENTORY, TransactionOption.ALL);
    }

    public static void setStatus(Player player, Instance instance) {
        //noinspection UnstableApiUsage
        player.setRespawnPoint(instance.getTag(Tag.Structure("spawn", Pos.class).defaultValue(Pos.ZERO)));
        player.setGameMode(GameMode.SURVIVAL);
    }
}
