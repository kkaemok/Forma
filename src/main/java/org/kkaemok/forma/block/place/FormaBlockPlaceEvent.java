package org.kkaemok.forma.block.place;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class FormaBlockPlaceEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final FormaBlockPlaceContext context;
    private final FormaPlacementPlan plan;
    private final Block placedBlock;
    private boolean cancelled;

    public FormaBlockPlaceEvent(
            @NotNull Player player,
            @NotNull FormaBlockPlaceContext context,
            @NotNull FormaPlacementPlan plan,
            @NotNull Block placedBlock
    ) {
        super(player);
        this.context = Objects.requireNonNull(context, "context");
        this.plan = Objects.requireNonNull(plan, "plan");
        this.placedBlock = Objects.requireNonNull(placedBlock, "placedBlock");
    }

    public FormaBlockPlaceContext context() {
        return context;
    }

    public FormaPlacementPlan plan() {
        return plan;
    }

    public Block placedBlock() {
        return placedBlock;
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
