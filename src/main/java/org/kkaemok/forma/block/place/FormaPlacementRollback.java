package org.kkaemok.forma.block.place;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class FormaPlacementRollback {
    private final List<BlockState> blockStates = new ArrayList<>();

    public void add(Block block) {
        blockStates.add(block.getState(true));
    }

    public BlockState previousPrimaryState() {
        return blockStates.getFirst();
    }

    public void rollback() {
        for (int index = blockStates.size() - 1; index >= 0; index--) {
            blockStates.get(index).update(true, false);
        }
    }
}
