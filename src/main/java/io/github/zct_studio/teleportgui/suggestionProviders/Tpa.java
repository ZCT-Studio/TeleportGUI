package io.github.zct_studio.teleportgui.suggestionProviders;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Tpa implements SuggestionProvider<CommandSourceStack> {

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            var server = context.getSource().getServer();
            //noinspection ConstantValue
            if (server == null) return Suggestions.empty();

            String filter = SuggestionUtils.filter(builder.getRemaining());
            for (io.github.zct_studio.teleportgui.commands.Tpa.TpaRequest req : io.github.zct_studio.teleportgui.commands.Tpa.getRequestsFor(player.getStringUUID())) {
                var fromPlayer = server.getPlayerList().getPlayer(UUID.fromString(req.fromUuid));
                if (fromPlayer != null) {
                    String name = fromPlayer.getName().getString();
                    if (SuggestionUtils.matches(name, filter)) {
                        builder.suggest(name);
                    }
                }
            }
            return builder.buildFuture();
        } catch (Exception e) {
            return Suggestions.empty();
        }
    }
}
