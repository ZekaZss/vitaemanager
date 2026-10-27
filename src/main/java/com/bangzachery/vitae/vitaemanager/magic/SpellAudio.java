package com.bangzachery.vitae.vitaemanager.magic;

import java.util.*;
import java.util.function.Consumer;

/** One use owns one audio clock. No Bukkit tasks, delayed callbacks, or catch-up sound bursts. */
final class SpellAudio {
    private final SpellSoundPlan plan;
    private final Consumer<SpellSoundPlan.Cue> play;
    private final Runnable stopRepeats;
    private final List<Long> next = new ArrayList<>();
    private String phase;
    private boolean started, closed;
    SpellAudio(SpellSoundPlan plan, Consumer<SpellSoundPlan.Cue> play) { this(plan, play, () -> {}); }
    SpellAudio(SpellSoundPlan plan, Consumer<SpellSoundPlan.Cue> play, Runnable stopRepeats) {
        this.plan = plan; this.play = play; this.stopRepeats = stopRepeats;
    }
    boolean closed() { return closed; }
    void start(String initialPhase, long now) {
        if (started || closed) return;
        started = true;
        for (var cue : plan.cues("activation")) {
            if (closed) return;
            play.accept(cue);
        }
        phase(initialPhase, now);
    }
    void phase(String name, long now) {
        if (!started || closed || Objects.equals(phase, name)) return;
        if (!SpellSoundPlan.PHASES.contains(name)) throw new IllegalArgumentException("Fase sound tidak dikenal: " + name);
        stopRepeats.run(); phase = name; next.clear();
        for (var cue : plan.cues(phase)) {
            if (closed) return;
            next.add(cue.intervalTicks() > 0 ? now + cue.intervalTicks() : Long.MAX_VALUE);
            play.accept(cue);
        }
    }
    void tick(long now) {
        if (!started || closed || phase == null) return;
        List<SpellSoundPlan.Cue> cues = plan.cues(phase);
        for (int index = 0; index < cues.size(); index++) {
            if (closed) return;
            if (now >= next.get(index)) {
            var cue = cues.get(index);
            next.set(index, cue.intervalTicks() > 0 ? now + cue.intervalTicks() : Long.MAX_VALUE);
            play.accept(cue);
            }
        }
    }
    void finish(boolean closingSound) {
        if (closed) return;
        closed = true; next.clear(); phase = null; stopRepeats.run();
        if (started && closingSound) emitOnce("ending");
    }
    private void emitOnce(String name) { for (var cue : plan.cues(name)) play.accept(cue); }
}
