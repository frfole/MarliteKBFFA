package code.frfole.kb;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
}
