package io.github.zct_studio.teleportgui.suggestionProviders;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.zct_studio.teleportgui.storage.Manager;
import net.minecraft.commands.CommandSourceStack;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class Warp implements SuggestionProvider<CommandSourceStack> {

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        try {
            String prefix = builder.getRemaining().toLowerCase(Locale.ROOT);
            Manager.getWarps().forEach(w -> {
                if (w.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    builder.suggest(w.getName());
                }
            });
            return builder.buildFuture();
        } catch (Exception e) {
            return Suggestions.empty();
        }
    }
}
