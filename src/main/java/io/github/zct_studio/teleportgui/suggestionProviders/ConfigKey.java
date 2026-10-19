package io.github.zct_studio.teleportgui.suggestionProviders;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.zct_studio.teleportgui.commands.GUI;
import net.minecraft.commands.CommandSourceStack;

import java.util.concurrent.CompletableFuture;

public class ConfigKey implements SuggestionProvider<CommandSourceStack> {

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context,
                                                         SuggestionsBuilder builder) {
        for (String key : GUI.configKeys()) {
            builder.suggest(key);
        }
        return builder.buildFuture();
    }
}
