package com.bangzachery.vitae.vitaemanager.servercontrol;

import com.bangzachery.vitae.vitaemanager.core.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.logging.Level;

public final class ServerControlService implements AutoCloseable {
    public enum Result {
        APPLIED, BUSY, FAILED
    }

    private final JavaPlugin plugin;
    private final ServerControlStore store;
    private final MessageService messages;
    private final ExecutorService writer =
            Executors.newSingleThreadExecutor();

    private volatile ServerControlState current;
    private volatile boolean closed;
    private boolean busy;

    public ServerControlService(
            JavaPlugin plugin,
            ServerControlStore store,
            ServerControlState initial,
            MessageService messages) {
        this.plugin = plugin;
        this.store = store;
        this.current = initial;
        this.messages = messages;
    }

    public ServerControlState current() {
        return current;
    }

    public boolean maintenanceBypass(Player player) {
        return player.isOp()
                || player.hasPermission("vitae.admin.maintenance");
    }

    public void applyWorld(World world) {
        ServerControlState.WorldRules rules =
                current.worlds().get(world.getUID());

        if (rules == null) return;

        world.setPVP(rules.pvp());
        world.setGameRule(
                GameRule.DO_MOB_SPAWNING, rules.mobSpawning());
    }

    public void enforceMaintenance() {
        if (!current.maintenance()) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!maintenanceBypass(player)) {
                player.kick(messages.component("maintenance-kick"));
            }
        }
    }

    public void maintenance(
            boolean enabled, Consumer<Result> completion) {
        update(state -> state.withMaintenance(enabled), completion);
    }

    public void chatMuted(
            boolean muted, Consumer<Result> completion) {
        update(state -> state.withChatMuted(muted), completion);
    }

    public void world(
            World world,
            Boolean pvp,
            Boolean spawning,
            Consumer<Result> completion) {
        ServerControlState.WorldRules existing =
                current.worlds().get(world.getUID());

        boolean oldPvp = existing == null
                ? world.getPVP()
                : existing.pvp();

        boolean oldSpawn = existing == null
                ? Boolean.TRUE.equals(
                world.getGameRuleValue(GameRule.DO_MOB_SPAWNING))
                : existing.mobSpawning();

        var rules = new ServerControlState.WorldRules(
                pvp == null ? oldPvp : pvp,
                spawning == null ? oldSpawn : spawning);

        var id = world.getUID();

        update(state -> state.withWorld(id, rules), result -> {
            World loaded = Bukkit.getWorld(id);

            if (result == Result.APPLIED && loaded != null) {
                applyWorld(loaded);
            }

            completion.accept(result);
        });
    }

    private void update(
            UnaryOperator<ServerControlState> change,
            Consumer<Result> completion) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException(
                    "Perubahan state harus di server thread");
        }

        if (closed) {
            completion.accept(Result.FAILED);
            return;
        }

        if (busy) {
            completion.accept(Result.BUSY);
            return;
        }

        ServerControlState candidate = change.apply(current);

        if (candidate.equals(current)) {
            completion.accept(Result.APPLIED);
            return;
        }

        busy = true;

        writer.execute(() -> {
            try {
                store.save(candidate);

                post(() -> {
                    ServerControlState previous = current;
                    current = candidate;
                    busy = false;

                    Bukkit.getWorlds().stream()
                            .filter(world -> !Objects.equals(
                                    previous.worlds().get(world.getUID()),
                                    candidate.worlds().get(world.getUID())))
                            .forEach(this::applyWorld);

                    enforceMaintenance();
                    completion.accept(Result.APPLIED);
                });
            } catch (IOException | RuntimeException exception) {
                plugin.getLogger().log(
                        Level.SEVERE,
                        "Penyimpanan kontrol server gagal; "
                                + "state lama dipertahankan.",
                        exception);

                post(() -> {
                    busy = false;
                    completion.accept(Result.FAILED);
                });
            }
        });
    }

    private void post(Runnable action) {
        if (closed) return;

        try {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!closed) action.run();
            });
        } catch (IllegalPluginAccessException exception) {
            plugin.getLogger().fine(
                    "Callback kontrol server dilewati karena "
                            + "plugin sedang dimatikan.");
        }
    }

    @Override
    public void close() {
        closed = true;
        writer.shutdown();

        try {
            if (!writer.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning(
                        "Writer kontrol server belum selesai "
                                + "setelah 10 detik.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            plugin.getLogger().warning(
                    "Menunggu writer kontrol server terinterupsi.");
        }
    }
}