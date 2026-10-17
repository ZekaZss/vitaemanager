package com.bangzachery.vitae.vitaemanager.whisper;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import java.time.Duration;
import java.util.*;

/** Optional typewriter refreshes only while text is growing, at most once every two ticks. */
public final class WhisperScreen {
    private static final class Frame {
        final WhisperLine line;
        final WhisperVisual visual;
        final long start;
        final int titleLength, length, speed;
        int shown = -1;
        Frame(WhisperLine line, WhisperVisual visual, long start) {
            this.line = line; this.visual = visual; this.start = start;
            titleLength = WhisperText.length(line.title()); length = titleLength + WhisperText.length(line.subtitle());
            speed = Math.max(visual.charactersPerTick(), (length + Math.max(1, line.durationTicks() / 2) - 1) / Math.max(1, line.durationTicks() / 2));
        }
    }
    private final Map<UUID, Frame> frames = new HashMap<>();
    public void start(Player player, WhisperLine line, WhisperVisual visual, long tick) {
        Frame frame = new Frame(line, visual, tick); frames.put(player.getUniqueId(), frame); render(player, frame, tick);
    }
    public void tick(Player player, long tick) {
        Frame frame = frames.get(player.getUniqueId()); if (frame == null) return;
        if (tick - frame.start >= frame.line.durationTicks()) { frames.remove(player.getUniqueId()); return; }
        if (frame.visual.typing() && frame.shown < frame.length && (tick - frame.start) % 2 == 0) render(player, frame, tick);
    }
    private void render(Player player, Frame frame, long tick) {
        int age = (int) (tick - frame.start);
        int shown = frame.visual.typing() ? Math.min(frame.length, (age + 1) * frame.speed) : frame.length;
        if (shown == frame.shown) return;
        frame.shown = shown;
        int left = frame.line.durationTicks() - age;
        int fadeIn = frame.visual.typing() ? 0 : Math.min(frame.visual.fadeInTicks(), left / 3);
        int fadeOut = Math.min(frame.visual.fadeOutTicks(), left / 3);
        var times = Title.Times.times(Duration.ofMillis(fadeIn * 50L), Duration.ofMillis((left - fadeIn - fadeOut) * 50L), Duration.ofMillis(fadeOut * 50L));
        player.showTitle(Title.title(WhisperText.component(WhisperText.prefix(frame.line.title(), shown), NamedTextColor.WHITE),
                WhisperText.component(WhisperText.prefix(frame.line.subtitle(), Math.max(0, shown - frame.titleLength)), NamedTextColor.GRAY), times));
    }
    public void clear(Player player) { if (frames.remove(player.getUniqueId()) != null) player.clearTitle(); }
    public void forget(UUID player) { frames.remove(player); }
    public Set<UUID> players() { return Set.copyOf(frames.keySet()); }
}
