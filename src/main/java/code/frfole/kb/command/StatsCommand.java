package code.frfole.kb.command;

import code.frfole.kb.Messages;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StatsCommand extends Command {
    private static final ArgumentEntity ARG_TARGET = new ArgumentEntity("target")
            .onlyPlayers(true)
            .singleEntity(true);

    public StatsCommand(@NotNull String name, @Nullable String... aliases) {
        super(name, aliases);

        addSyntax((sender, context) -> {
            if (sender instanceof Player player) {
                player.sendMessage(Messages.Stats.statsResponse(player));
            }
        });
        addSyntax((sender, context) -> {
            Player player = context.get(ARG_TARGET).findFirstPlayer(sender);
            if (player == null) {
                sender.sendMessage(Messages.Stats.NO_TARGET);
                return;
            }
            sender.sendMessage(Messages.Stats.statsResponse(player));
        }, ARG_TARGET);
    }
}
