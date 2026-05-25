package org.kkaemok.forma.api.block;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;

public record PlacedFormaBlockData(
        String id,
        World world,
        int x,
        int y,
        int z,
        Location location,
        String visualState
) {
    public PlacedFormaBlockData {
        Objects.requireNonNull(world, "world");
        location = location.clone();
    }

    @Override
    public Location location() {
        return location.clone();
    }
}
