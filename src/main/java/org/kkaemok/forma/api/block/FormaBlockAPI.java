package org.kkaemok.forma.api.block;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.Set;

public interface FormaBlockAPI {
    Optional<FormaBlockData> getBlock(String id);

    boolean hasBlock(String id);

    Set<String> getBlockIds();

    ItemStack createBlockItem(String id);

    ItemStack createBlockItem(String id, int amount);

    Optional<String> getBlockId(ItemStack item);

    boolean isFormaBlockItem(ItemStack item);

    Optional<PlacedFormaBlockData> getPlacedBlock(Location location);

    boolean isFormaBlock(Location location);

    boolean removePlacedBlock(Location location, boolean drop);

    boolean setPlacedBlock(Location location, String blockId);

    boolean forceSetPlacedBlock(Location location, String blockId);

    Set<PlacedFormaBlockData> getPlacedBlocks(World world);
}
