package org.kkaemok.forma.recipe;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.kkaemok.forma.Forma;
import org.kkaemok.forma.block.FormaBlock;
import org.kkaemok.forma.block.FormaBlockItemBuilder;
import org.kkaemok.forma.item.FormaItem;
import org.kkaemok.forma.item.FormaItemBuilder;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

public final class FormaRecipeManager {
    private final Forma plugin;
    private final FormaItemBuilder itemBuilder;
    private final FormaBlockItemBuilder blockItemBuilder;
    private final Set<NamespacedKey> registeredKeys = new LinkedHashSet<>();
    private final Map<String, FormaRecipeType> registeredRecipes = new LinkedHashMap<>();
    private final Map<NamespacedKey, String> registeredRecipeLabels = new LinkedHashMap<>();

    public FormaRecipeManager(Forma plugin) {
        this.plugin = plugin;
        this.itemBuilder = new FormaItemBuilder(plugin);
        this.blockItemBuilder = new FormaBlockItemBuilder(plugin);
    }

    public void reload() {
        clearRegisteredRecipes();
        for (FormaItem item : plugin.getItemManager().getItems().values()) {
            if (item.recipe() != null) {
                registerItem(item);
            }
        }
        for (FormaBlock block : plugin.getBlockManager().getBlocks().values()) {
            if (block.recipe() != null) {
                registerBlock(block);
            }
        }
        plugin.getLogger().info("Forma 조합법 등록 완료: " + registeredRecipes.size() + "개");
    }

    public Map<String, FormaRecipeType> getRegisteredRecipes() {
        return Collections.unmodifiableMap(registeredRecipes);
    }

    public Set<NamespacedKey> getRegisteredKeys() {
        return Set.copyOf(registeredKeys);
    }

    public boolean isRegistered(NamespacedKey key) {
        return key != null && registeredKeys.contains(key);
    }

    public boolean registerItemRecipe(String itemId) {
        FormaItem item = plugin.getItemManager().getItem(itemId);
        return item != null && item.recipe() != null && registerItem(item);
    }

    public boolean registerBlockRecipe(String blockId) {
        FormaBlock block = plugin.getBlockManager().getBlock(blockId);
        return block != null && block.recipe() != null && registerBlock(block);
    }

    public boolean unregisterRecipe(NamespacedKey key) {
        if (key == null || !registeredKeys.remove(key)) {
            return false;
        }
        Bukkit.removeRecipe(key);
        String label = registeredRecipeLabels.remove(key);
        if (label != null) {
            registeredRecipes.remove(label);
        }
        return true;
    }

    private void clearRegisteredRecipes() {
        for (NamespacedKey key : registeredKeys) {
            Bukkit.removeRecipe(key);
        }
        registeredKeys.clear();
        registeredRecipes.clear();
        registeredRecipeLabels.clear();
    }

    private boolean registerItem(FormaItem item) {
        NamespacedKey key = itemRecipeKeyOrNull(item.id());
        if (key == null) {
            warn(item.id(), "NamespacedKey를 만들 수 없습니다.");
            return false;
        }

        ItemStack result = itemBuilder.buildWithDebug(item).itemStack();
        result.setAmount(item.recipe().amount());
        return register(key, result, item.id(), item.id(), item.recipe());
    }

    private boolean registerBlock(FormaBlock block) {
        NamespacedKey key = blockRecipeKeyOrNull(block.id());
        if (key == null) {
            warn("block:" + block.id(), "NamespacedKey를 만들 수 없습니다.");
            return false;
        }

        ItemStack result = blockItemBuilder.build(block);
        result.setAmount(block.recipe().amount());
        return register(key, result, "block:" + block.id(), "block:" + block.id(), block.recipe());
    }

    private boolean register(
            NamespacedKey key,
            ItemStack result,
            String label,
            String resultId,
            FormaRecipeDefinition definition
    ) {
        removeRegisteredMetadata(key);
        Bukkit.removeRecipe(key);
        Recipe recipe = createRecipe(key, result, resultId, definition);
        if (recipe == null) {
            return false;
        }
        try {
            if (!Bukkit.addRecipe(recipe)) {
                warn(resultId, "동일 키의 조합법이 이미 등록되어 등록하지 못했습니다.");
                return false;
            }
            registeredKeys.add(key);
            registeredRecipes.put(label, definition.type());
            registeredRecipeLabels.put(key, label);
            debug("recipe registered: key=" + key
                    + ", type=" + definition.type()
                    + ", result=" + resultId);
            return true;
        } catch (IllegalArgumentException ex) {
            warn(resultId, "조합법 등록 실패: " + ex.getMessage());
            return false;
        }
    }

    private void removeRegisteredMetadata(NamespacedKey key) {
        registeredKeys.remove(key);
        String previousLabel = registeredRecipeLabels.remove(key);
        if (previousLabel != null) {
            registeredRecipes.remove(previousLabel);
        }
    }

    private NamespacedKey itemRecipeKey(String itemId) {
        return new NamespacedKey(plugin, itemId.toLowerCase(Locale.ROOT));
    }

    private NamespacedKey itemRecipeKeyOrNull(String itemId) {
        try {
            return itemRecipeKey(itemId);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private NamespacedKey blockRecipeKeyOrNull(String blockId) {
        try {
            return new NamespacedKey(plugin, "block/" + blockId.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Recipe createRecipe(
            NamespacedKey key,
            ItemStack result,
            String resultId,
            FormaRecipeDefinition definition
    ) {
        if (definition.type() == FormaRecipeType.SHAPED) {
            return createShapedRecipe(key, result, resultId, definition);
        }
        return createShapelessRecipe(key, result, resultId, definition);
    }

    private Recipe createShapedRecipe(
            NamespacedKey key,
            ItemStack result,
            String itemId,
            FormaRecipeDefinition definition
    ) {
        try {
            ShapedRecipe recipe = new ShapedRecipe(key, result);
            recipe.shape(definition.pattern().toArray(String[]::new));
            for (Map.Entry<Character, String> entry : definition.shapedIngredients().entrySet()) {
                RecipeChoice choice = resolveChoice(itemId, entry.getValue());
                if (choice == null) {
                    return null;
                }
                recipe.setIngredient(entry.getKey(), choice);
            }
            return recipe;
        } catch (IllegalArgumentException ex) {
            warn(itemId, "SHAPED 조합법 형식이 잘못되었습니다: " + ex.getMessage());
            return null;
        }
    }

    private Recipe createShapelessRecipe(
            NamespacedKey key,
            ItemStack result,
            String itemId,
            FormaRecipeDefinition definition
    ) {
        ShapelessRecipe recipe = new ShapelessRecipe(key, result);
        for (String ingredient : definition.shapelessIngredients()) {
            RecipeChoice choice = resolveChoice(itemId, ingredient);
            if (choice == null) {
                return null;
            }
            recipe.addIngredient(choice);
        }
        return recipe;
    }

    private RecipeChoice resolveChoice(String resultItemId, String ingredientId) {
        FormaItem formaItem = plugin.getItemManager().getItem(ingredientId);
        if (formaItem != null) {
            debug("recipe ingredient: " + resultItemId + " <- item:" + ingredientId);
            return new RecipeChoice.ExactChoice(itemBuilder.buildWithDebug(formaItem).itemStack());
        }

        FormaBlock formaBlock = plugin.getBlockManager().getBlock(ingredientId);
        if (formaBlock != null) {
            debug("recipe ingredient: " + resultItemId + " <- block:" + ingredientId);
            return new RecipeChoice.ExactChoice(blockItemBuilder.build(formaBlock));
        }

        Material material = Material.matchMaterial(ingredientId);
        if (material != null) {
            debug("recipe ingredient: " + resultItemId + " <- material:" + material);
            return new RecipeChoice.MaterialChoice(material);
        }

        warn(resultItemId, "알 수 없는 recipe 재료입니다: " + ingredientId);
        return null;
    }

    private void warn(String itemId, String message) {
        plugin.getLogger().warning("[FormaRecipe] " + itemId + ": " + message);
    }

    private void debug(String message) {
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }
}
