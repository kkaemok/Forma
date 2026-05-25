package org.kkaemok.forma.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class FormaItemGiveEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String itemId;
    private final ItemStack itemStack;
    private boolean cancelled;

    public FormaItemGiveEvent(@NotNull Player target, @NotNull String itemId, @NotNull ItemStack itemStack) {
        super(target);
        this.itemId = Objects.requireNonNull(itemId, "itemId");
        this.itemStack = Objects.requireNonNull(itemStack, "itemStack");
    }

    public @NotNull Player getTarget() {
        return getPlayer();
    }

    public @NotNull String getItemId() {
        return itemId;
    }

    /**
     * 지급될 템플릿 스택입니다. 이벤트 listener는 메타데이터를 수정할 수 있습니다.
     */
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
