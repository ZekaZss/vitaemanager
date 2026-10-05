package com.bangzachery.vitae.vitaemanager.whisper;

import java.util.*;

/** Pure tick-driven queue. A completed real run remains reserved until persistence acknowledges it. */
public final class WhisperPlayback {
    public record Run(UUID ticket, WhisperDefinition definition, boolean preview) {
        public Run(WhisperDefinition definition, boolean preview) { this(UUID.randomUUID(), definition, preview); }
    }
    public record Step(Run run, WhisperLine line, boolean complete) { }
    private static final int CAPACITY = 32;
    private final Map<UUID, Slot> players = new HashMap<>();
    private static final class Slot {
        final Deque<Run> queue = new ArrayDeque<>();
        int index;
        long due;
        boolean started, waiting;
    }

    public boolean enqueue(UUID player, Run run) {
        if (run.definition().lines().isEmpty()) return false;
        Slot slot = players.computeIfAbsent(player, ignored -> new Slot());
        if (slot.queue.size() >= CAPACITY || slot.queue.stream().anyMatch(existing ->
                existing.definition().id().equals(run.definition().id()))) return false;
        slot.queue.addLast(run);
        return true;
    }
    public Run head(UUID player) { var slot = players.get(player); return slot == null ? null : slot.queue.peekFirst(); }
    public Set<UUID> players() { return Set.copyOf(players.keySet()); }
    public boolean started(UUID player) { var slot = players.get(player); return slot != null && slot.started; }
    public Step advance(UUID player, long tick) {
        Slot slot = players.get(player);
        if (slot == null || slot.waiting || (slot.started && tick < slot.due)) return null;
        Run run = slot.queue.peekFirst();
        if (slot.index == run.definition().lines().size()) {
            slot.waiting = true;
            return new Step(run, null, true);
        }
        WhisperLine line = run.definition().lines().get(slot.index++);
        slot.started = true;
        slot.due = Math.addExact(tick, line.durationTicks()
                + (slot.index < run.definition().lines().size() ? line.gapTicks() : 0));
        return new Step(run, line, false);
    }
    public void acknowledge(UUID player, Run run) {
        Slot slot = players.get(player);
        if (slot == null || !Objects.equals(slot.queue.peekFirst(), run)) return;
        slot.queue.removeFirst(); slot.index = 0; slot.started = false; slot.waiting = false;
        if (slot.queue.isEmpty()) players.remove(player);
    }
    /** Returns true only when an active, displayed title belonged to the canceled definition. */
    public boolean cancel(UUID player, String id) {
        Slot slot = players.get(player);
        if (slot == null) return false;
        Run head = slot.queue.peekFirst();
        boolean removedHead = head.definition().id().equals(id), displayed = removedHead && slot.started;
        slot.queue.removeIf(run -> run.definition().id().equals(id));
        if (removedHead) { slot.index = 0; slot.started = false; slot.waiting = false; }
        if (slot.queue.isEmpty()) players.remove(player);
        return displayed;
    }
    public void clear(UUID player) { players.remove(player); }
}