package code.frfole.kb;

import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentLiteral;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.fakeplayer.FakePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.UUID;

public class DebugCommand extends Command {
    public DebugCommand(@NotNull String name, @Nullable String... aliases) {
        super(name, aliases);

        addSyntax((sender, context) -> {
            if (sender instanceof Entity entity) {
                UUID uuid = UUID.randomUUID();
                FakePlayer.initPlayer(uuid, uuid.toString().substring(0, 16), fakePlayer -> {
//                    fakePlayer.setInstance(entity.getInstance());
                    fakePlayer.teleport(entity.getPosition());
                    fakePlayer.scheduleRemove(Duration.ofSeconds(60));
                });
            }
        }, new ArgumentLiteral("fakeplayer"));
    }
}
