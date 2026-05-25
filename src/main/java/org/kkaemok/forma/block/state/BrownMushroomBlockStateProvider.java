package org.kkaemok.forma.block.state;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BrownMushroomBlockStateProvider implements VisualStateProvider {
    private static final List<String> ORDERED_PROPERTIES = List.of(
            "down",
            "east",
            "north",
            "south",
            "up",
            "west"
    );
    private static final int MAX_VARIATIONS = 1 << ORDERED_PROPERTIES.size();

    @Override
    public VisualBlockState fromVariation(int variation) {
        if (variation < 1 || variation > MAX_VARIATIONS) {
            return null;
        }

        int value = variation - 1;
        Map<String, String> properties = new LinkedHashMap<>();
        for (int index = 0; index < ORDERED_PROPERTIES.size(); index++) {
            boolean enabled = (value & (1 << index)) != 0;
            properties.put(ORDERED_PROPERTIES.get(index), Boolean.toString(enabled));
        }
        return new VisualBlockState(Material.BROWN_MUSHROOM_BLOCK, properties);
    }

    @Override
    public int maxVariations() {
        return MAX_VARIATIONS;
    }

    @Override
    public List<VisualBlockState> getAllStates() {
        List<VisualBlockState> states = new ArrayList<>(MAX_VARIATIONS);
        for (int variation = 1; variation <= MAX_VARIATIONS; variation++) {
            VisualBlockState state = fromVariation(variation);
            if (state != null) {
                states.add(state);
            }
        }
        return states;
    }

    @Override
    public Material getBaseMaterial() {
        return Material.BROWN_MUSHROOM_BLOCK;
    }

    public static List<String> orderedProperties() {
        return ORDERED_PROPERTIES;
    }
}
