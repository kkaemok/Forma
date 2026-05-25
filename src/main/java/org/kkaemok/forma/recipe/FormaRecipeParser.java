package org.kkaemok.forma.recipe;

import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class FormaRecipeParser {
    private FormaRecipeParser() {
    }

    public static FormaRecipeDefinition parse(ConfigurationSection recipe, Consumer<String> warningConsumer) {
        if (recipe == null) {
            return null;
        }

        FormaRecipeType type = FormaRecipeType.fromString(recipe.getString("type"));
        if (type == null) {
            warningConsumer.accept("recipe.type이 잘못되어 조합법을 스킵합니다.");
            return null;
        }

        int amount = recipe.getInt("amount", 1);
        if (amount < 1 || amount > 64) {
            warningConsumer.accept("recipe.amount는 1~64 범위여야 하므로 조합법을 스킵합니다.");
            return null;
        }

        if (type == FormaRecipeType.SHAPED) {
            return parseShaped(recipe, amount, warningConsumer);
        }

        List<String> ingredients = recipe.getStringList("ingredients");
        if (ingredients.isEmpty()) {
            warningConsumer.accept("SHAPELESS recipe에는 ingredients가 필요합니다.");
            return null;
        }
        return new FormaRecipeDefinition(type, List.of(), Map.of(), ingredients, amount);
    }

    private static FormaRecipeDefinition parseShaped(
            ConfigurationSection recipe,
            int amount,
            Consumer<String> warningConsumer
    ) {
        List<String> pattern = recipe.getStringList("pattern");
        ConfigurationSection ingredients = recipe.getConfigurationSection("ingredients");
        if (pattern.isEmpty() || ingredients == null) {
            warningConsumer.accept("SHAPED recipe에는 pattern과 ingredients가 필요합니다.");
            return null;
        }

        Map<Character, String> values = new LinkedHashMap<>();
        for (String key : ingredients.getKeys(false)) {
            String ingredient = ingredients.getString(key);
            if (key.length() != 1 || ingredient == null || ingredient.isBlank()) {
                warningConsumer.accept("SHAPED recipe ingredients 키 또는 값이 잘못되었습니다: " + key);
                return null;
            }
            values.put(key.charAt(0), ingredient);
        }
        return new FormaRecipeDefinition(FormaRecipeType.SHAPED, pattern, values, List.of(), amount);
    }
}
