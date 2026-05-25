package org.kkaemok.forma.api.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class FormaBlockPlaceEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String blockId;
    private final Location location;
    private final ItemStack itemStack;
    private boolean cancelled;

    public FormaBlockPlaceEvent(
            @NotNull Player player,
            @NotNull String blockId,
            @NotNull Location location,
            @NotNull ItemStack itemStack
    ) {
        super(player);
        this.blockId = Objects.requireNonNull(blockId, "blockId");
        this.location = Objects.requireNonNull(location, "location").clone();
        this.itemStack = Objects.requireNonNull(itemStack, "itemStack");
    }

    public @NotNull String getBlockId() {
        return blockId;
    }

    public @NotNull Location getLocation() {
        return location.clone();
    }

    public @NotNull ItemStack getItemStack() {
        return itemStack;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
