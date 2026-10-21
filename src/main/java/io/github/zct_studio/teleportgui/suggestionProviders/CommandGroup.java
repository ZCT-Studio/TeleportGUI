package io.github.zct_studio.teleportgui.suggestionProviders;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.zct_studio.teleportgui.commands.Registry;
import net.minecraft.commands.CommandSourceStack;

import java.util.concurrent.CompletableFuture;

public class CommandGroup implements SuggestionProvider<CommandSourceStack> {

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context,
                                                         SuggestionsBuilder builder) {
        for (String name : Registry.groupNames()) {
            builder.suggest(name);
        }
        return builder.buildFuture();
    }
}
