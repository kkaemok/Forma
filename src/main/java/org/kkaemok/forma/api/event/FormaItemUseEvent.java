package org.kkaemok.forma.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class FormaItemUseEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String itemId;
    private final ItemStack itemStack;
    private final EquipmentSlot hand;
    private final Action action;
    private boolean cancelled;

    public FormaItemUseEvent(
            @NotNull Player player,
            @NotNull String itemId,
            @NotNull ItemStack itemStack,
            @NotNull EquipmentSlot hand,
            @NotNull Action action
    ) {
        super(player);
        this.itemId = Objects.requireNonNull(itemId, "itemId");
        this.itemStack = Objects.requireNonNull(itemStack, "itemStack");
        this.hand = Objects.requireNonNull(hand, "hand");
        this.action = Objects.requireNonNull(action, "action");
    }

    public @NotNull String getItemId() {
        return itemId;
    }

    public @NotNull ItemStack getItemStack() {
        return itemStack;
    }

    public @NotNull EquipmentSlot getHand() {
        return hand;
    }

    public @NotNull Action getAction() {
        return action;
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
