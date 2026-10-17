package com.bangzachery.vitae.vitaemanager.story;

import com.bangzachery.vitae.vitaemanager.npc.NpcData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class StoryNpcDialogEvent extends Event {
    private static final HandlerList HANDLERS=new HandlerList();
    private final Player player;
    private final String npc;
    private NpcData.Node node;
    private boolean managed;
    public StoryNpcDialogEvent(Player player,String npc){this.player=player;this.npc=npc;}
    public Player player(){return player;}
    public String npc(){return npc;}
    public NpcData.Node node(){return node;}
    public void node(NpcData.Node node){this.node=node;}
    public boolean managed(){return managed;}
    public void managed(boolean managed){this.managed=managed;}
    @Override public HandlerList getHandlers(){return HANDLERS;}
    public static HandlerList getHandlerList(){return HANDLERS;}
}
