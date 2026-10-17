package com.bangzachery.vitae.vitaemanager.story;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** A dialog choice requests an action after its movement lock has been released. */
public final class StoryNpcActionEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String npc, action;
    public StoryNpcActionEvent(Player player, String npc, String action) { this.player = player; this.npc = npc; this.action = action; }
    public Player player() { return player; }
    public String npc() { return npc; }
    public String action() { return action; }
    public static boolean target(String target) { return java.util.Set.of("DUNGEON_GLASS","DUNGEON_MINION","DUNGEON_BOSS","DUNGEON_REWARD").contains(target) || target.matches("ACTION_[A-Za-z0-9_-]{1,41}"); }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
