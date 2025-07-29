package io.github.rypofalem.armorstandeditor.api;

import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

public class ItemFrameGlowEvent extends ItemFrameEvent implements Cancellable {
    private boolean cancelled = false;
    private final Player player;

    public ItemFrameGlowEvent(ItemFrame itemFrame, Player player) {
        super(itemFrame);
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /* Generated for Bukkit */
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}