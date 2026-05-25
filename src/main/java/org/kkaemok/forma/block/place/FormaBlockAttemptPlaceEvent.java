package org.kkaemok.forma.block.place;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class FormaBlockAttemptPlaceEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final FormaBlockPlaceContext context;
    private final FormaPlacementPlan plan;
    private boolean cancelled;

    public FormaBlockAttemptPlaceEvent(
            @NotNull Player player,
            @NotNull FormaBlockPlaceContext context,
            @NotNull FormaPlacementPlan plan
    ) {
        super(player);
        this.context = Objects.requireNonNull(context, "context");
        this.plan = Objects.requireNonNull(plan, "plan");
    }

    public FormaBlockPlaceContext context() {
        return context;
    }

    public FormaPlacementPlan plan() {
        return plan;
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
