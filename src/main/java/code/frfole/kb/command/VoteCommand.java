package code.frfole.kb.command;

import code.frfole.kb.Messages;
import code.frfole.kb.Tags;
import code.frfole.kb.game.GameManager;
import code.frfole.kb.game.GameMap;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentLiteral;
import net.minestom.server.command.builder.arguments.ArgumentWord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class VoteCommand extends Command {
    private final GameManager gameManager;
    private final @NotNull ArgumentWord argMap;

    public VoteCommand(@NotNull String name, @NotNull GameManager gameManager, @Nullable String... aliases) {
        super(name, aliases);
        this.gameManager = gameManager;
        argMap = new ArgumentWord("map").from(Arrays.stream(gameManager.getMaps()).map(GameMap::name).toArray(String[]::new));

        addSyntax(this::vote, argMap);
        addSyntax(this::unvote, new ArgumentLiteral("unvote"));
        addSyntax(this::info, new ArgumentLiteral("info"));
    }

    private void info(@NotNull CommandSender sender, @NotNull CommandContext ignoredContext) {
        int mapIndex = sender.getTag(Tags.VOTED_MAP);
        GameMap[] maps = gameManager.getMaps();
        if (mapIndex < 0 || mapIndex >= maps.length) {
            sender.sendMessage(Messages.Vote.infoResponse(null));
        } else {
            sender.sendMessage(Messages.Vote.infoResponse(maps[mapIndex].name()));
        }
    }

    private void unvote(@NotNull CommandSender sender, @NotNull CommandContext ignoredContext) {
        sender.removeTag(Tags.VOTED_MAP);
        sender.sendMessage(Messages.Vote.CANCEL_VOTE);
    }

    private void vote(@NotNull CommandSender sender, @NotNull CommandContext context) {
        String mapName = context.get(argMap);
        for (int i = 0; i < gameManager.getMaps().length; i++) {
            if (gameManager.getMaps()[i].name().equalsIgnoreCase(mapName)) {
                sender.setTag(Tags.VOTED_MAP, i);
                sender.sendMessage(Messages.Vote.voteResponse(mapName));
                return;
            }
        }
        sender.sendMessage("Invalid map name " + mapName);
    }
}
