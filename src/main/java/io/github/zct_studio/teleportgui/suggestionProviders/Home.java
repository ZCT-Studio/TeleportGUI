package io.github.zct_studio.teleportgui.suggestionProviders;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.zct_studio.teleportgui.storage.Manager;
import io.github.zct_studio.teleportgui.types.PlayerData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class Home implements SuggestionProvider<CommandSourceStack> {

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String prefix = builder.getRemaining().toLowerCase(Locale.ROOT);
            Manager.getPlayer(player.getStringUUID())
                    .map(PlayerData::getHomes)
                    .ifPresent(homes -> homes.forEach(h -> {
                        if (h.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                            builder.suggest(h.getName());
                        }
                    }));
            return builder.buildFuture();
        } catch (Exception e) {
            return Suggestions.empty();
        }
    }
}
