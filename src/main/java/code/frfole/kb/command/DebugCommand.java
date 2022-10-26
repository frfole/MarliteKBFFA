package code.frfole.kb.command;

import code.frfole.kb.Statistic;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
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

        addSyntax((sender, context) -> {
            TextComponent.Builder text = Component.text();
            text.append(Component.text("Statistic:"));
            for (Statistic<?> value : Statistic.VALUES) {
                text.append(Component.newline(),
                        Component.text(value.name()),
                        Component.text(": "),
                        Component.text(value.get(sender).toString()));
            }
            sender.sendMessage(text.build());
        }, new ArgumentLiteral("stats"));
    }
}
