package org.kkaemok.forma.api.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FormaBlockBreakEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String blockId;
    private final Location location;
    private final List<ItemStack> drops;
    private boolean dropItems = true;
    private boolean cancelled;

    public FormaBlockBreakEvent(
            @NotNull Player player,
            @NotNull String blockId,
            @NotNull Location location,
            @NotNull List<ItemStack> drops
    ) {
        super(player);
        this.blockId = Objects.requireNonNull(blockId, "blockId");
        this.location = Objects.requireNonNull(location, "location").clone();
        this.drops = new ArrayList<>();
        for (ItemStack drop : Objects.requireNonNull(drops, "drops")) {
            this.drops.add(drop.clone());
        }
    }

    public @NotNull String getBlockId() {
        return blockId;
    }

    public @NotNull Location getLocation() {
        return location.clone();
    }

    /**
     * 파괴 성공 시 드롭할 mutable 목록입니다.
     */
    public @NotNull List<ItemStack> getDrops() {
        return drops;
    }

    public boolean isDropItems() {
        return dropItems;
    }

    public void setDropItems(boolean dropItems) {
        this.dropItems = dropItems;
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
