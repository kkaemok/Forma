package org.kkaemok.forma.api.block;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.List;

public record FormaBlockData(
        String id,
        String provider,
        String visualState,
        Material visualMaterial,
        Component displayName,
        String model,
        double hardness,
        String tool,
        List<String> drops
) {
    public FormaBlockData {
        drops = List.copyOf(drops);
    }
}
