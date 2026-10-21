package io.github.zct_studio.teleportgui.suggestionProviders;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.zct_studio.teleportgui.commands.GUI;
import net.minecraft.commands.CommandSourceStack;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class ConfigKey implements SuggestionProvider<CommandSourceStack> {

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context,
                                                         SuggestionsBuilder builder) {
        String prefix = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String key : GUI.configKeys()) {
            if (key.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                builder.suggest(key);
            }
        }
        return builder.buildFuture();
    }
}
