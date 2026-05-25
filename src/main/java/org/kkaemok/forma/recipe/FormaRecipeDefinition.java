package org.kkaemok.forma.recipe;

import java.util.List;
import java.util.Map;

public record FormaRecipeDefinition(
        FormaRecipeType type,
        List<String> pattern,
        Map<Character, String> shapedIngredients,
        List<String> shapelessIngredients,
        int amount
) {
    public FormaRecipeDefinition {
        pattern = List.copyOf(pattern);
        shapedIngredients = Map.copyOf(shapedIngredients);
        shapelessIngredients = List.copyOf(shapelessIngredients);
    }
}
