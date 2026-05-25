package org.kkaemok.forma.api;

import org.kkaemok.forma.Forma;
import org.kkaemok.forma.api.block.FormaBlockAPI;
import org.kkaemok.forma.api.internal.DefaultFormaBlockAPI;
import org.kkaemok.forma.api.internal.DefaultFormaItemAPI;
import org.kkaemok.forma.api.internal.DefaultFormaRecipeAPI;
import org.kkaemok.forma.api.item.FormaItemAPI;
import org.kkaemok.forma.api.recipe.FormaRecipeAPI;

import java.util.Objects;

public final class FormaAPI {
    private static volatile FormaAPI instance;

    private final Forma plugin;
    private final FormaItemAPI items;
    private final FormaBlockAPI blocks;
    private final FormaRecipeAPI recipes;
    private volatile boolean active = true;

    private FormaAPI(Forma plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.items = new DefaultFormaItemAPI(plugin);
        this.blocks = new DefaultFormaBlockAPI(plugin);
        this.recipes = new DefaultFormaRecipeAPI(plugin);
    }

    public static FormaAPI initialize(Forma plugin) {
        FormaAPI api = new FormaAPI(plugin);
        instance = api;
        return api;
    }

    public static FormaAPI get() {
        FormaAPI api = instance;
        if (api == null || !api.active || !api.plugin.isEnabled()) {
            throw new IllegalStateException("Forma API is not active.");
        }
        return api;
    }

    public static void shutdown(FormaAPI api) {
        if (api != null) {
            api.active = false;
            if (instance == api) {
                instance = null;
            }
        }
    }

    public FormaItemAPI getItems() {
        requireActive();
        return items;
    }

    public FormaBlockAPI getBlocks() {
        requireActive();
        return blocks;
    }

    public FormaRecipeAPI getRecipes() {
        requireActive();
        return recipes;
    }

    private void requireActive() {
        if (!active || !plugin.isEnabled()) {
            throw new IllegalStateException("Forma API is not active.");
        }
    }
}
