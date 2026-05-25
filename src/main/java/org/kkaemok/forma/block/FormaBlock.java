package org.kkaemok.forma.block;

import org.bukkit.Instrument;
import org.bukkit.Material;
import org.kkaemok.forma.block.state.VisualBlockState;
import org.kkaemok.forma.block.state.VisualStateProviderType;
import org.kkaemok.forma.recipe.FormaRecipeDefinition;
import org.kkaemok.forma.util.NoteBlockInstrumentUtil;

import java.util.List;
import java.util.Objects;

public record FormaBlock(
        String id,
        FormaBlockType type,
        String name,
        Material itemMaterial,
        String itemModel,
        Integer customModelData,
        List<String> lore,
        String blockModel,
        double hardness,
        String tool,
        FormaBlockSounds sounds,
        VisualStateProviderType providerType,
        VisualBlockState visualState,
        Instrument instrument,
        int note,
        boolean powered,
        Integer variation,
        String variationRaw,
        List<String> drops,
        FormaRecipeDefinition recipe
) {
    public FormaBlock {
        lore = List.copyOf(Objects.requireNonNullElse(lore, List.of()));
        drops = List.copyOf(Objects.requireNonNullElse(drops, List.of()));
        sounds = Objects.requireNonNullElse(sounds, FormaBlockSounds.defaults());
        providerType = Objects.requireNonNull(providerType, "providerType");
        visualState = Objects.requireNonNull(visualState, "visualState");
    }

    public String noteBlockStateKey() {
        if (instrument == null) {
            return null;
        }
        return "instrument=" + NoteBlockInstrumentUtil.toBlockStateName(instrument)
                + ",note=" + note
                + ",powered=" + powered;
    }

    public boolean usesNoteBlockProvider() {
        return providerType.isNoteBlockProvider();
    }

    public Material placedMaterial() {
        return visualState.material();
    }
}
