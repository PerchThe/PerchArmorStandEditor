package io.github.rypofalem.armorstandeditor.api;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

public class ArmorStandRenameEvent extends ArmorStandEvent implements Cancellable {

    private boolean cancelled = false;
    private String name;
    private final Player player;

    public ArmorStandRenameEvent(ArmorStand armorStand, Player player, String name) {
        super(armorStand);
        this.player = player;
        this.name = name;
    }

    public Player getPlayer() {
        return player;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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