package com.bangzachery.vitae.vitaemanager.economy;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.logging.Level;

public final class VitiService implements AutoCloseable {
    private final JavaPlugin plugin;
    private final VitiStore store;
    private final VitiPaper paper;
    private final ExecutorService writer = Executors.newSingleThreadExecutor();
    private final java.util.Set<UUID> invalidAccounts = new HashSet<>();
    private volatile VitiLedger current;
    private volatile boolean closed;
    private boolean busy;

    public VitiService(JavaPlugin plugin, VitiStore store, VitiPaper paper, VitiLedger initial) {
        this.plugin = plugin; this.store = store; this.paper = paper; this.current = initial;
    }

    public VitiLedger current() { return current; }
    public BigDecimal balance(Player player) {
        var account = current.accounts().get(player.getUniqueId());
        return account == null ? paper.oldBalance(player) : account.balance();
    }

    // Called once per second, on the server thread. Registers/imports online accounts in one write.
    public void tick() {
        if (busy || closed) return;
        VitiLedger candidate = claimOnline(current);
        boolean pendingReady = current.notes().entrySet().stream().anyMatch(entry -> {
            Player owner = Bukkit.getPlayer(entry.getValue().owner());
            return entry.getValue().pending() && owner != null
                    && (paper.contains(owner, entry.getKey(), entry.getValue().amount()) || paper.emptySlot(owner) >= 0);
        });
        if (!candidate.equals(current)) {
            save(candidate, () -> {}, ignored -> {});
        } else {
            mirrorOnline();
            if (pendingReady) { busy = true; deliver(ignored -> {}); }
        }
    }

    private VitiLedger claimOnline(VitiLedger state) {
        VitiLedger candidate = state;
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                candidate = seed(candidate, player);
                candidate = candidate.claimGiveaway(player.getUniqueId());
                invalidAccounts.remove(player.getUniqueId());
            } catch (IllegalArgumentException exception) {
                if (invalidAccounts.add(player.getUniqueId())) plugin.getLogger().log(Level.SEVERE,
                        "Akun/giveaway belum dapat diproses: " + player.getUniqueId(), exception);
            }
        }
        return candidate;
    }

    public void giveaway(BigDecimal amount, Consumer<String> done) {
        transact(state -> claimOnline(state.startGiveaway(UUID.randomUUID(), amount)), () -> {}, done);
    }

    public void stopGiveaway(Consumer<String> done) {
        transact(VitiLedger::stopGiveaway, () -> {}, done);
    }

    public void transfer(Player from, Player to, BigDecimal amount, Consumer<String> done) {
        transact(state -> seed(seed(state, from), to).transfer(from.getUniqueId(), to.getUniqueId(), amount), () -> {}, done);
    }

    public void admin(Player target, String operation, BigDecimal amount, Consumer<String> done) {
        transact(state -> {
            VitiLedger seeded = seed(state, target);
            return switch (operation) {
                case "add" -> seeded.add(target.getUniqueId(), amount);
                case "remove" -> seeded.remove(target.getUniqueId(), amount);
                case "set" -> seeded.set(target.getUniqueId(), amount);
                default -> throw new VitiLedger.Failure("viti-invalid");
            };
        }, () -> {}, done);
    }

    public void withdraw(Player player, BigDecimal amount, Consumer<String> done) {
        if (paper.emptySlot(player) < 0) { done.accept("viti-full"); return; }
        UUID id = UUID.randomUUID();
        transact(state -> seed(state, player).withdraw(player.getUniqueId(), id, amount), () -> {}, key -> {
            if (!key.equals("viti-success")) { done.accept(key); return; }
            VitiLedger.Note note = current.notes().get(id);
            done.accept(note != null && note.pending() ? "viti-delivery-pending" : "viti-converted");
        });
    }

    public void redeem(Player player, Consumer<String> done) {
        if (!ready(done)) return;
        try {
            VitiPaper.Value value = paper.read(player.getInventory().getItemInMainHand());
            if (value.serial() == null) {
                value = new VitiPaper.Value(UUID.randomUUID(), value.amount(), true);
                player.getInventory().setItemInMainHand(paper.create(value.serial(), value.amount(), true));
                // A legacy stack has no identity. Persist its new ID before crediting it,
                // otherwise a restart could restore anonymous paper and permit another payout.
            }
            if (value.legacy()) player.saveData();
            VitiPaper.Value sealed = value;
            transact(state -> seed(state, player).redeem(player.getUniqueId(), sealed.serial(),
                    sealed.amount(), sealed.legacy()), () -> {
                if (player.isOnline()) {
                    paper.remove(player, sealed.serial());
                }
            }, done);
        } catch (VitiLedger.Failure exception) { done.accept(exception.key()); }
        catch (IllegalArgumentException exception) { done.accept("viti-note-invalid"); }
        catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "Migrasi uang fisik gagal; saldo belum dikreditkan.", exception);
            done.accept("viti-failed");
        }
    }

    public void reload(Consumer<String> done) {
        if (!ready(done)) return;
        busy = true;
        writer.execute(() -> {
            try {
                VitiLedger loaded = store.load();
                post(() -> { current = loaded; busy = false; mirrorOnline(); done.accept("viti-reloaded"); });
            } catch (Exception exception) { fail(exception, done); }
        });
    }

    private VitiLedger seed(VitiLedger ledger, Player player) {
        var old = ledger.accounts().get(player.getUniqueId());
        return ledger.seed(player.getUniqueId(), player.getName(), old == null ? paper.oldBalance(player) : old.balance());
    }

    private boolean ready(Consumer<String> done) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Transaksi harus di server thread");
        if (closed) { done.accept("viti-failed"); return false; }
        if (busy) { done.accept("viti-busy"); return false; }
        return true;
    }

    private void transact(UnaryOperator<VitiLedger> change, Runnable committed, Consumer<String> done) {
        if (!ready(done)) return;
        try { save(change.apply(current), committed, done); }
        catch (VitiLedger.Failure exception) { done.accept(exception.key()); }
        catch (IllegalArgumentException exception) { done.accept("viti-invalid"); }
    }

    private void save(VitiLedger candidate, Runnable committed, Consumer<String> done) {
        VitiLedger previous = current;
        busy = true;
        writer.execute(() -> {
            try {
                store.save(candidate);
                post(() -> {
                    current = candidate;
                    mirrorOnline();
                    announceGiveaway(previous, candidate);
                    try { committed.run(); }
                    catch (RuntimeException exception) { plugin.getLogger().log(Level.SEVERE,
                            "Ledger tersimpan, tetapi pembaruan inventory gagal. ID note tetap dilindungi ledger.", exception); }
                    deliver(done);
                });
            } catch (Exception exception) { fail(exception, done); }
        });
    }

    private void deliver(Consumer<String> done) {
        var delivered = new HashSet<UUID>();
        for (var entry : current.notes().entrySet()) {
            VitiLedger.Note note = entry.getValue();
            if (!note.pending()) continue;
            Player player = Bukkit.getPlayer(note.owner());
            if (player == null) continue;
            try {
                if (!paper.contains(player, entry.getKey(), note.amount())) {
                    int slot = paper.emptySlot(player);
                    if (slot < 0) continue;
                    player.getInventory().setItem(slot, paper.create(entry.getKey(), note.amount(), false));
                }
                // Paper owns playerdata; save on the required thread before marking delivery complete.
                player.saveData();
                delivered.add(entry.getKey());
            } catch (RuntimeException exception) { plugin.getLogger().log(Level.WARNING,
                    "Pengiriman uang fisik belum selesai: " + entry.getKey(), exception); }
        }
        if (delivered.isEmpty()) { busy = false; done.accept("viti-success"); return; }
        VitiLedger acknowledged = current.delivered(delivered);
        writer.execute(() -> {
            try {
                store.save(acknowledged);
                post(() -> { current = acknowledged; busy = false; done.accept("viti-success"); });
            } catch (Exception exception) {
                plugin.getLogger().log(Level.WARNING, "Ack pengiriman gagal; pengiriman dapat diperiksa ulang.", exception);
                post(() -> { busy = false; done.accept("viti-success"); });
            }
        });
    }

    private void mirrorOnline() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            var account = current.accounts().get(player.getUniqueId());
            if (account != null) try { paper.mirror(player, account.balance()); }
            catch (RuntimeException exception) { plugin.getLogger().log(Level.WARNING,
                    "Mirror saldo belum selesai: " + player.getUniqueId(), exception); }
        }
    }

    private void announceGiveaway(VitiLedger previous, VitiLedger saved) {
        var round = saved.giveaway();
        if (round == null) return;
        var old = previous.giveaway();
        for (UUID id : round.claimed()) {
            if (old != null && old.id().equals(round.id()) && old.claimed().contains(id)) continue;
            Player player = Bukkit.getPlayer(id);
            if (player == null || !player.isOnline()) continue;
            try {
                player.sendMessage(Component.text("[Vitae] Kamu menerima giveaway "
                        + VitiAmount.format(round.amount()) + " Viti!", NamedTextColor.GREEN));
            } catch (RuntimeException exception) { plugin.getLogger().log(Level.WARNING,
                    "Saldo giveaway tersimpan, tetapi notifikasi gagal: " + id, exception); }
        }
    }

    private void fail(Exception exception, Consumer<String> done) {
        plugin.getLogger().log(Level.SEVERE, "Operasi viti.yml gagal; ledger aktif dipertahankan.", exception);
        post(() -> { busy = false; done.accept("viti-failed"); });
    }

    private void post(Runnable action) {
        if (closed) return;
        try { Bukkit.getScheduler().runTask(plugin, () -> { if (!closed) action.run(); }); }
        catch (IllegalPluginAccessException ignored) { }
    }

    @Override public void close() {
        closed = true;
        writer.shutdown();
        try {
            if (!writer.awaitTermination(10, TimeUnit.SECONDS)) plugin.getLogger().warning("Writer Viti belum selesai setelah 10 detik.");
        } catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
    }
}