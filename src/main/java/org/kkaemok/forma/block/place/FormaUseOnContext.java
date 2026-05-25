package org.kkaemok.forma.block.place;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public record FormaUseOnContext(
        Player player,
        EquipmentSlot hand,
        ItemStack item,
        Block clickedBlock,
        BlockFace clickedFace,
        Location clickedLocation
) {
    public FormaUseOnContext {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(hand, "hand");
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(clickedBlock, "clickedBlock");
        Objects.requireNonNull(clickedFace, "clickedFace");
        Objects.requireNonNull(clickedLocation, "clickedLocation");
        clickedLocation = clickedLocation.clone();
    }
}
