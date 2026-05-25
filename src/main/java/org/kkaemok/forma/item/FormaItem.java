package org.kkaemok.forma.item;

import org.bukkit.Material;
import org.kkaemok.forma.recipe.FormaRecipeDefinition;

import java.util.List;
import java.util.Objects;

public record FormaItem(
        String id,
        FormaItemType type,
        Material material,
        String name,
        String model,
        Integer customModelData,
        List<String> lore,
        FormaItemSettings settings,
        FormaItemAttributes attributes,
        List<FormaItemBehavior> rightClickBehaviors,
        FormaRecipeDefinition recipe,
        Double damage,
        boolean legacyFormat
) {
    public FormaItem {
        lore = List.copyOf(Objects.requireNonNullElse(lore, List.of()));
        settings = Objects.requireNonNull(settings, "settings");
        attributes = Objects.requireNonNull(attributes, "attributes");
        rightClickBehaviors = List.copyOf(Objects.requireNonNullElse(rightClickBehaviors, List.of()));
    }
}
