package org.kkaemok.forma.api.recipe;

import org.bukkit.NamespacedKey;

import java.util.Set;

public interface FormaRecipeAPI {
    void reloadRecipes();

    Set<NamespacedKey> getRecipeKeys();

    boolean isFormaRecipe(NamespacedKey key);

    boolean registerItemRecipe(String itemId);

    boolean registerBlockRecipe(String blockId);

    boolean unregisterRecipe(NamespacedKey key);
}
