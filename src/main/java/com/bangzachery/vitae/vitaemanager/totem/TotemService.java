package com.bangzachery.vitae.vitaemanager.totem;

import com.bangzachery.vitae.vitaemanager.core.config.ConfigurationService;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.java.JavaPlugin;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class TotemService implements AutoCloseable {
    private final JavaPlugin plugin;
    private final ConfigurationService configuration;
    private final TotemLimitStore store;
    private final Clock clock;
    private final NamespacedKey countKey, cooldownKey;
    private final ExecutorService writer;
    private Integer limitOverride;
    private boolean busy;
    private volatile boolean closed;

    public TotemService(JavaPlugin plugin, ConfigurationService configuration,
                        TotemLimitStore store, Integer limitOverride) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.store = store;
        this.limitOverride = limitOverride;
        this.clock = Clock.systemUTC();
        countKey = new NamespacedKey(plugin, "totem_count");
        cooldownKey = new NamespacedKey(plugin, "totem_cooldown");
        writer = Executors.newSingleThreadExecutor(action -> new Thread(action, "Vitae-Totem-Writer"));
    }

    public int limit() { return limitOverride == null ? configuration.current().totem().limit() : limitOverride; }
    public long now() { return clock.millis(); }
    public long cooldownMillis() { return configuration.current().totem().cooldownHours() * 3_600_000L; }
    public boolean blockVanilla() { return configuration.current().totem().blockVanillaResurrection(); }

    public TotemState state(Player player) {
        requireThread();
        var pdc = player.getPersistentDataContainer();
        if ((pdc.has(countKey) && !pdc.has(countKey, PersistentDataType.INTEGER))
                || (pdc.has(cooldownKey) && !pdc.has(cooldownKey, PersistentDataType.LONG))) {
            throw new IllegalArgumentException("Tipe PDC totem tidak valid untuk " + player.getUniqueId());
        }
        return new TotemState(pdc.getOrDefault(countKey, PersistentDataType.INTEGER, 0),
                pdc.getOrDefault(cooldownKey, PersistentDataType.LONG, 0L));
    }

    /** Only mutates PDC; callers own the corresponding inventory change and native save. */
    public void write(Player player, TotemState state) {
        requireThread();
        var pdc = player.getPersistentDataContainer();
        pdc.set(countKey, PersistentDataType.INTEGER, state.count());
        if (state.cooldownUntil() == 0) pdc.remove(cooldownKey);
        else pdc.set(cooldownKey, PersistentDataType.LONG, state.cooldownUntil());
    }

    public void setLimit(int limit, Consumer<String> done) {
        requireThread();
        TotemState.requireLimit(limit);
        if (closed) { done.accept("totem-failed"); return; }
        if (busy) { done.accept("totem-busy"); return; }
        busy = true;
        writer.execute(() -> {
            try {
                store.save(limit);
                post(() -> { limitOverride = limit; busy = false; done.accept("totem-limit-set"); });
            } catch (Exception exception) {
                plugin.getLogger().log(Level.SEVERE, "Batas totem gagal disimpan; batas aktif dipertahankan.", exception);
                post(() -> { busy = false; done.accept("totem-failed"); });
            }
        });
    }

    public boolean resetCooldown(Player player) {
        try {
            TotemState before = state(player);
            if (before.cooldownUntil() == 0) return true;
            try {
                write(player, before.resetCooldown());
                player.saveData();
            } catch (RuntimeException exception) {
                write(player, before);
                throw exception;
            }
            return true;
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Reset cooldown totem gagal: " + player.getUniqueId(), exception);
            return false;
        }
    }

    private void post(Runnable action) {
        if (closed) return;
        try {
            Bukkit.getScheduler().runTask(plugin, () -> { if (!closed) action.run(); });
        } catch (IllegalPluginAccessException ignored) { }
    }

    private void requireThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Totem harus diproses di server thread");
    }

    @Override public void close() {
        closed = true;
        writer.shutdown();
        try {
            if (!writer.awaitTermination(10, TimeUnit.SECONDS)) plugin.getLogger().warning("Writer totem belum selesai setelah 10 detik.");
        } catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
    }
}