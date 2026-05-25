package org.kkaemok.forma.recipe;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.kkaemok.forma.Forma;
import org.kkaemok.forma.util.KeyUtil;

import java.util.ArrayList;
import java.util.List;

public final class FormaRecipeListener implements Listener {
    private final Forma plugin;

    public FormaRecipeListener(Forma plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (!isPreventionEnabled() || canUseCustomInputs(event.getRecipe(), event.getInventory().getMatrix())) {
            return;
        }

        event.getInventory().setResult(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!isPreventionEnabled() || canUseCustomInputs(event.getRecipe(), event.getInventory().getMatrix())) {
            return;
        }

        event.setCancelled(true);
        event.getInventory().setResult(null);
    }

    private boolean canUseCustomInputs(Recipe recipe, ItemStack[] matrix) {
        List<CustomInput> customInputs = collectCustomInputs(matrix);
        if (customInputs.isEmpty()) {
            return true;
        }

        List<RecipeChoice.ExactChoice> availableExactChoices = collectExactChoices(recipe);
        for (CustomInput customInput : customInputs) {
            int matchingChoice = findMatchingChoice(availableExactChoices, customInput.itemStack());
            if (matchingChoice < 0) {
                debug("recipe blocked: custom ingredient " + customInput.id()
                        + " cannot be consumed as its base material " + customInput.itemStack().getType());
                return false;
            }
            availableExactChoices.remove(matchingChoice);
        }
        return true;
    }

    private List<CustomInput> collectCustomInputs(ItemStack[] matrix) {
        List<CustomInput> customInputs = new ArrayList<>();
        for (ItemStack itemStack : matrix) {
            if (itemStack == null || itemStack.getType() == Material.AIR) {
                continue;
            }
            ItemMeta meta = itemStack.getItemMeta();
            if (meta == null) {
                continue;
            }

            String blockId = KeyUtil.readBlockId(plugin, meta.getPersistentDataContainer());
            if (blockId != null) {
                customInputs.add(new CustomInput("block:" + blockId, itemStack));
                continue;
            }

            String itemId = KeyUtil.readItemId(plugin, meta.getPersistentDataContainer());
            if (itemId != null) {
                customInputs.add(new CustomInput("item:" + itemId, itemStack));
            }
        }
        return customInputs;
    }

    private List<RecipeChoice.ExactChoice> collectExactChoices(Recipe recipe) {
        List<RecipeChoice.ExactChoice> choices = new ArrayList<>();
        if (recipe instanceof ShapedRecipe shapedRecipe) {
            for (String row : shapedRecipe.getShape()) {
                for (int index = 0; index < row.length(); index++) {
                    RecipeChoice choice = shapedRecipe.getChoiceMap().get(row.charAt(index));
                    if (choice instanceof RecipeChoice.ExactChoice exactChoice) {
                        choices.add(exactChoice);
                    }
                }
            }
            return choices;
        }

        if (recipe instanceof ShapelessRecipe shapelessRecipe) {
            for (RecipeChoice choice : shapelessRecipe.getChoiceList()) {
                if (choice instanceof RecipeChoice.ExactChoice exactChoice) {
                    choices.add(exactChoice);
                }
            }
        }
        return choices;
    }

    private int findMatchingChoice(List<RecipeChoice.ExactChoice> choices, ItemStack itemStack) {
        for (int index = 0; index < choices.size(); index++) {
            RecipeChoice.ExactChoice choice = choices.get(index);
            if (choice.getChoices().stream().anyMatch(candidate -> candidate.isSimilar(itemStack))) {
                return index;
            }
        }
        return -1;
    }

    private boolean isPreventionEnabled() {
        return plugin.getConfig().getBoolean("recipes.prevent-custom-items-as-vanilla-ingredients", true);
    }

    private void debug(String message) {
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }

    private record CustomInput(String id, ItemStack itemStack) {
    }
}
