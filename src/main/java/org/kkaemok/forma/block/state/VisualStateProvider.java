package org.kkaemok.forma.block.state;

import org.bukkit.Material;

import java.util.List;

public interface VisualStateProvider {
    VisualBlockState fromVariation(int variation);

    int maxVariations();

    List<VisualBlockState> getAllStates();

    Material getBaseMaterial();
}
