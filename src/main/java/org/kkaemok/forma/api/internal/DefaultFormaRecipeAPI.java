package org.kkaemok.forma.api.internal;

import org.bukkit.NamespacedKey;
import org.kkaemok.forma.Forma;
import org.kkaemok.forma.api.recipe.FormaRecipeAPI;

import java.util.Set;

public final class DefaultFormaRecipeAPI implements FormaRecipeAPI {
    private final Forma plugin;

    public DefaultFormaRecipeAPI(Forma plugin) {
        this.plugin = plugin;
    }

    @Override
    public void reloadRecipes() {
        requireActive();
        plugin.getRecipeManager().reload();
    }

    @Override
    public Set<NamespacedKey> getRecipeKeys() {
        requireActive();
        return plugin.getRecipeManager().getRegisteredKeys();
    }

    @Override
    public boolean isFormaRecipe(NamespacedKey key) {
        requireActive();
        return plugin.getRecipeManager().isRegistered(key);
    }

    @Override
    public boolean registerItemRecipe(String itemId) {
        requireActive();
        return plugin.getRecipeManager().registerItemRecipe(itemId);
    }

    @Override
    public boolean registerBlockRecipe(String blockId) {
        requireActive();
        return plugin.getRecipeManager().registerBlockRecipe(blockId);
    }

    @Override
    public boolean unregisterRecipe(NamespacedKey key) {
        requireActive();
        return plugin.getRecipeManager().unregisterRecipe(key);
    }

    private void requireActive() {
        if (!plugin.isEnabled()) {
            throw new IllegalStateException("Forma API is not active.");
        }
    }
}
