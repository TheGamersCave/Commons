package com.caved_in.commons.game.event;

import com.caved_in.commons.player.User;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public class UserJoinEvent extends Event {
    private static HandlerList handlers = new HandlerList();

    private JavaPlugin parent;

    private User user;

    public UserJoinEvent(JavaPlugin parent, User user) {
        this.parent = parent;
        this.user = user;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public JavaPlugin getPlugin() {
        return parent;
    }

    public User getUser() {
        return user;
    }
}
