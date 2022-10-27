package code.frfole.kb;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.TextColor.color;

public class Messages {
    public static class Vote {
        public static final Component CANCEL_VOTE = text("You have cancelled your vote.", color(0xFFEB7C));

        public static Component voteResponse(@NotNull String mapName) {
            return text("You have voted for ", color(0xFFEB7C))
                    .append(text(mapName, NamedTextColor.GREEN));
        }

        public static Component infoResponse(@Nullable String mapName) {
            Component message;
            if (mapName == null) {
                message = text("You have not voted yet", color(0xFFEB7C));
            } else {
                message = text("You have voted for ", color(0xFFEB7C))
                        .append(text(mapName, NamedTextColor.GREEN));
            }
            return message;
        }
    }

    public static class Stats {
        public static final Component NO_TARGET = text("You must specify a target.", color(0xFF3333));

        public static Component statsResponse(@NotNull Player target) {
            TextComponent.Builder text = Component.text();
            Component displayName = target.getDisplayName();
            if (displayName == null) displayName = text(target.getUsername(), NamedTextColor.GREEN);
            displayName = displayName.hoverEvent(target);
            text.append(
                    displayName,
                    text("'s stats:", color(0xFFEB7C))
            );
            for (Statistic<?> value : Statistic.VALUES) {
                text.append(newline(),
                        text(" - ", color(0x929292)),
                        text(value.name(), color(0xDBFF58)),
                        text(": ", color(0x929292)),
                        text(value.get(target).toString(), NamedTextColor.GREEN));
            }
            return text.build();
        }
    }
}
