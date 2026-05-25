package org.kkaemok.forma.api.item;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.List;
import java.util.OptionalInt;

public record FormaItemData(
        String id,
        String type,
        Material material,
        Component displayName,
        List<Component> lore,
        String model,
        int customModelData,
        boolean glow,
        boolean unbreakable,
        OptionalInt durabilityMax,
        OptionalInt durabilityCurrent
) {
    public FormaItemData {
        lore = List.copyOf(lore);
    }
}
