package org.kkaemok.forma.block.state;

import org.bukkit.Material;
import org.kkaemok.forma.util.NoteBlockInstrumentUtil;
import org.kkaemok.forma.util.NoteBlockState;
import org.kkaemok.forma.util.NoteBlockVariationUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NoteBlockVisualStateProvider implements VisualStateProvider {
    @Override
    public VisualBlockState fromVariation(int variation) {
        NoteBlockState state = NoteBlockVariationUtil.fromVariation(variation);
        return state == null ? null : fromNoteBlockState(state);
    }

    @Override
    public int maxVariations() {
        return NoteBlockVariationUtil.maxVariations();
    }

    @Override
    public List<VisualBlockState> getAllStates() {
        List<VisualBlockState> states = new ArrayList<>(maxVariations());
        for (int variation = 1; variation <= maxVariations(); variation++) {
            VisualBlockState state = fromVariation(variation);
            if (state != null) {
                states.add(state);
            }
        }
        return states;
    }

    @Override
    public Material getBaseMaterial() {
        return Material.NOTE_BLOCK;
    }

    public VisualBlockState fromNoteBlockState(NoteBlockState state) {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("instrument", NoteBlockInstrumentUtil.toBlockStateName(state.instrument()));
        properties.put("note", Integer.toString(state.note()));
        properties.put("powered", Boolean.toString(state.powered()));
        return new VisualBlockState(Material.NOTE_BLOCK, properties);
    }
}
