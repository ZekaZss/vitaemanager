package com.bangzachery.vitae.vitaemanager.whisper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.Level;

public final class WhisperService implements Listener, AutoCloseable {
    private final JavaPlugin plugin;
    private final WhisperStore store;
    private final WhisperPlayback playback = new WhisperPlayback();
    private final ExecutorService writer;
    private WhisperState current;
    private WhisperState persisted; // Used only by the one writer after construction.
    private Map<UUID, List<WhisperDefinition>> byTarget = Map.of();
    private final Map<UUID, Point[]> selections = new HashMap<>();
    private final Map<UUID, String> playingAudio = new HashMap<>();
    public record Point(UUID world, int x, int y, int z) { }
    private BukkitTask task;
    private long tick;
    private int pendingWrites;
    private volatile boolean closed;

    public WhisperService(JavaPlugin plugin, WhisperStore store, WhisperState initial) {
        this.plugin = plugin; this.store = store; current = initial; persisted = initial;
        writer = Executors.newSingleThreadExecutor(action -> new Thread(action, "Vitae-Whisper-Writer"));
        index();
    }
    public WhisperState current() { return current; }
    public void validateAudio(WhisperLine.Audio audio) { store.validateAudio(audio); }
    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::advance, 1, 1);
        // Also handles players online when the plugin is enabled without restarting the server.
        for (Player player : Bukkit.getOnlinePlayers()) enterAt(player);
    }
    public void select(Player player, boolean first) {
        Location location = player.getLocation();
        Point[] points = selections.computeIfAbsent(player.getUniqueId(), ignored -> new Point[2]);
        Point point = new Point(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        if (first && points[1] != null && !points[1].world().equals(point.world())) points[1] = null;
        if (!first && points[0] != null && !points[0].world().equals(point.world()))
            throw new IllegalArgumentException("A dan B harus dalam dunia yang sama. Mulai seleksi baru dengan pos1 di dunia ini.");
        points[first ? 0 : 1] = point;
    }
    public WhisperArea selection(Player player) {
        Point[] points = selections.get(player.getUniqueId());
        if (points == null || points[0] == null || points[1] == null)
            throw new IllegalArgumentException("Tentukan /vitae bisikan pos1 dan pos2 dulu; titik adalah blok tempat kamu berdiri.");
        Point a = points[0], b = points[1];
        return WhisperArea.between(a.world(), a.x(), a.y(), a.z(), b.x(), b.y(), b.z());
    }

    /** Validate on the main thread before stopping sessions or submitting disk work. */
    public void change(String affectedId, UnaryOperator<WhisperState> operation, Consumer<String> result) {
        if (!ready(result)) return;
        operation.apply(current);
        cancel(affectedId);
        submit(operation, () -> result.accept("Tersimpan. Edit/reset menghentikan sesi area ini; masuk kembali untuk memicu."),
                () -> result.accept("Gagal menyimpan; setup/progres sebelumnya dipertahankan. Periksa log server."));
    }
    public void reload(Consumer<String> result) {
        if (!ready(result)) return;
        pendingWrites++;
        writer.execute(() -> {
            try {
                WhisperState loaded = store.load(); persisted = loaded;
                post(() -> { clearAll(); current = loaded; index(); pendingWrites--; result.accept("whispers.yml dimuat ulang."); });
            } catch (Exception exception) {
                failure(exception);
                post(() -> { pendingWrites--; result.accept("Reload gagal; bisikan aktif dipertahankan. Periksa whispers.yml dan log server."); });
            }
        });
    }
    public void preview(Player player, String id) {
        if (closed || !playback.enqueue(player.getUniqueId(), new WhisperPlayback.Run(current.require(id), true)))
            throw new IllegalArgumentException("Dialog kosong, preview/area ini sudah antre, atau antrean penuh (32).");
    }
    public void stop(Player player) { clear(player); }

    private boolean ready(Consumer<String> result) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Bisikan harus dipanggil di server thread");
        if (closed || pendingWrites > 0) {
            result.accept("Masih menyimpan bisikan/progres. Tunggu sebentar, lalu ulangi command."); return false;
        }
        return true;
    }
    private void submit(UnaryOperator<WhisperState> operation, Runnable success, Runnable failed) {
        pendingWrites++;
        writer.execute(() -> {
            try {
                WhisperState candidate = operation.apply(persisted);
                store.save(candidate); persisted = candidate;
                post(() -> {
                    boolean definitionsChanged = !current.definitions().equals(candidate.definitions());
                    current = candidate;
                    if (definitionsChanged) index();
                    pendingWrites--; success.run();
                });
            } catch (Exception exception) {
                failure(exception); post(() -> { pendingWrites--; failed.run(); });
            }
        });
    }
    private void failure(Exception exception) { plugin.getLogger().log(Level.WARNING, "Operasi whispers.yml gagal; snapshot terakhir dipertahankan.", exception); }
    private void post(Runnable action) {
        if (closed) return;
        try { Bukkit.getScheduler().runTask(plugin, () -> { if (!closed) action.run(); }); }
        catch (IllegalPluginAccessException ignored) { }
    }
    private void index() {
        var index = new HashMap<UUID, List<WhisperDefinition>>();
        for (var definition : current.definitions().values().stream().sorted(Comparator.comparing(WhisperDefinition::id)).toList()) {
            if (definition.enabled()) for (UUID target : definition.targets())
                index.computeIfAbsent(target, ignored -> new ArrayList<>()).add(definition);
        }
        var immutable = new HashMap<UUID, List<WhisperDefinition>>();
        index.forEach((id, list) -> immutable.put(id, List.copyOf(list)));
        byTarget = Map.copyOf(immutable);
    }
    private void trigger(Player player, Location from, Location to, boolean teleport) {
        if (closed || player.isDead() || to.getWorld() == null) return;
        UUID uuid = player.getUniqueId(), world = to.getWorld().getUID();
        for (var definition : byTarget.getOrDefault(uuid, List.of())) {
            WhisperArea area = definition.area();
            boolean entered;
            if (from == null || from.getWorld() == null || !from.getWorld().getUID().equals(world))
                entered = area.contains(world, to.getX(), to.getY(), to.getZ());
            else if (teleport)
                entered = area.contains(world, to.getX(), to.getY(), to.getZ())
                        && !area.contains(world, from.getX(), from.getY(), from.getZ());
            else entered = area.crossed(world, from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ());
            if (entered && current.eligible(definition, uuid))
                playback.enqueue(uuid, new WhisperPlayback.Run(definition, false));
        }
    }
    private void enterAt(Player player) { if (player.isOnline()) trigger(player, null, player.getLocation(), true); }
    private void advance() {
        tick++;
        for (UUID id : playback.players()) {
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline() || player.isDead()) { playback.clear(id); playingAudio.remove(id); continue; }
            WhisperPlayback.Run run = playback.head(id);
            if (!run.preview() && (!run.definition().equals(current.definitions().get(run.definition().id()))
                    || !current.eligible(run.definition(), id) || !run.definition().area().world().equals(player.getWorld().getUID()))) {
                if (playback.started(id)) player.clearTitle();
                stopAudio(player);
                playback.acknowledge(id, run); continue;
            }
            WhisperPlayback.Step step = playback.advance(id, tick);
            if (step == null) continue;
            if (step.complete()) {
                stopAudio(player);
                if (run.preview()) playback.acknowledge(id, run);
                else submit(state -> state.finish(run.definition(), id), () -> playback.acknowledge(id, run), () -> {
                    playback.acknowledge(id, run);
                    if (player.isOnline()) player.sendMessage(Component.text("[Vitae] Progres bisikan belum tersimpan; hubungi admin.", NamedTextColor.RED));
                });
            } else {
                stopAudio(player);
                var line = step.line();
                Title.Times times = Title.Times.times(Duration.ofMillis(250),
                        Duration.ofMillis((line.durationTicks() - 10L) * 50), Duration.ofMillis(250));
                player.showTitle(Title.title(Component.text(line.title(), NamedTextColor.WHITE),
                        Component.text(line.subtitle(), NamedTextColor.GRAY), times));
                if (line.audio() != null) {
                    var audio = line.audio();
                    player.playSound(player.getLocation(), audio.key(), SoundCategory.VOICE, audio.volume(), audio.pitch());
                    playingAudio.put(id, audio.key());
                }
            }
        }
    }
    private void cancel(String id) {
        for (UUID uuid : playback.players()) {
            boolean displayed = playback.cancel(uuid, id);
            Player player = Bukkit.getPlayer(uuid);
            if (displayed && player != null) { player.clearTitle(); stopAudio(player); }
        }
    }
    private void clear(Player player) {
        if (playback.started(player.getUniqueId())) player.clearTitle();
        playback.clear(player.getUniqueId());
        stopAudio(player);
    }
    private void stopAudio(Player player) {
        String key = playingAudio.remove(player.getUniqueId());
        if (key != null) player.stopSound(key, SoundCategory.VOICE);
    }
    private void clearAll() {
        for (UUID id : playback.players()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) clear(player); else { playback.clear(id); playingAudio.remove(id); }
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void move(PlayerMoveEvent event) {
        if (!(event instanceof PlayerTeleportEvent) && event.hasChangedPosition())
            trigger(event.getPlayer(), event.getFrom(), event.getTo(), false);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void teleport(PlayerTeleportEvent event) {
        // Teleports do not trigger areas lying between the source and destination.
        trigger(event.getPlayer(), event.getFrom(), event.getTo(), true);
    }
    @EventHandler public void join(PlayerJoinEvent event) { Bukkit.getScheduler().runTask(plugin, () -> enterAt(event.getPlayer())); }
    @EventHandler public void respawn(PlayerRespawnEvent event) { Bukkit.getScheduler().runTask(plugin, () -> enterAt(event.getPlayer())); }
    @EventHandler public void quit(PlayerQuitEvent event) { clear(event.getPlayer()); selections.remove(event.getPlayer().getUniqueId()); }
    @EventHandler public void death(PlayerDeathEvent event) { clear(event.getEntity()); }
    @Override public void close() {
        closed = true;
        if (task != null) task.cancel();
        clearAll(); selections.clear(); writer.shutdown();
        try {
            if (!writer.awaitTermination(10, TimeUnit.SECONDS)) plugin.getLogger().warning("Writer bisikan belum selesai setelah 10 detik.");
        } catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
    }
}