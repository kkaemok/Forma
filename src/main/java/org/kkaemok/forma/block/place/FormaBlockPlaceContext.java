package org.kkaemok.forma.block.place;

import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.kkaemok.forma.block.FormaBlock;
import org.kkaemok.forma.block.state.VisualBlockState;

import java.util.Objects;

public record FormaBlockPlaceContext(
        FormaUseOnContext useContext,
        FormaBlock formaBlock,
        Block againstBlock,
        Block targetBlock,
        boolean replaceClicked,
        VisualBlockState visualState,
        BlockData blockDataToPlace
) {
    public FormaBlockPlaceContext {
        Objects.requireNonNull(useContext, "useContext");
        Objects.requireNonNull(formaBlock, "formaBlock");
        Objects.requireNonNull(againstBlock, "againstBlock");
        Objects.requireNonNull(targetBlock, "targetBlock");
        Objects.requireNonNull(visualState, "visualState");
        Objects.requireNonNull(blockDataToPlace, "blockDataToPlace");
    }
}
