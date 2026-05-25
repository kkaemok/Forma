package org.kkaemok.forma.block.place;

import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.List;
import java.util.Objects;

public final class FormaPlacementPlan {
    private final List<BlockPlacementEntry> entries;

    private FormaPlacementPlan(List<BlockPlacementEntry> entries) {
        this.entries = List.copyOf(entries);
    }

    public static FormaPlacementPlan single(FormaBlockPlaceContext context) {
        return new FormaPlacementPlan(List.of(
                new BlockPlacementEntry(context.targetBlock(), context.blockDataToPlace())
        ));
    }

    public List<BlockPlacementEntry> entries() {
        return entries;
    }

    public BlockPlacementEntry primary() {
        return entries.getFirst();
    }

    public record BlockPlacementEntry(Block block, BlockData blockData) {
        public BlockPlacementEntry {
            Objects.requireNonNull(block, "block");
            Objects.requireNonNull(blockData, "blockData");
        }
    }
}
