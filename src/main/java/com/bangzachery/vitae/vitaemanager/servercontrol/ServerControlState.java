package com.bangzachery.vitae.vitaemanager.servercontrol;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record ServerControlState(boolean maintenance, boolean chatMuted,
                                 Map<UUID, WorldRules> worlds) {
    public ServerControlState {
        worlds = Map.copyOf(worlds);
    }

    public ServerControlState withMaintenance(boolean enabled) {
        return new ServerControlState(enabled, chatMuted, worlds);
    }

    public ServerControlState withChatMuted(boolean muted) {
        return new ServerControlState(maintenance, muted, worlds);
    }

    public ServerControlState withWorld(UUID id, WorldRules rules) {
        Map<UUID, WorldRules> updated = new HashMap<>(worlds);
        updated.put(id, rules);
        return new ServerControlState(maintenance, chatMuted, updated);
    }

    public record WorldRules(boolean pvp, boolean mobSpawning) {}
}