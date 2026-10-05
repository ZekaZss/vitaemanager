package com.bangzachery.vitae.vitaemanager.items;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.logging.Level;

public final class ItemRulesService implements AutoCloseable {
    private final JavaPlugin plugin;
    private final ItemRulesStore store;
    private final ItemCatalog catalog;
    private final ExecutorService writer;
    private final StackLimits stacks;
    private ItemRules current;
    private boolean busy;
    private volatile boolean closed;

    public ItemRulesService(JavaPlugin plugin, ItemRulesStore store, ItemRules initial, ItemCatalog catalog) {
        this.catalog = catalog;
        catalog.validate(initial);
        this.plugin = plugin;
        this.store = store;
        current = initial;
        stacks = new StackLimits(plugin, this::current);
        writer = Executors.newSingleThreadExecutor(action -> new Thread(action, "Vitae-Item-Rules-Writer"));
    }

    public ItemCatalog catalog() { return catalog; }
    public ItemRules current() { return current; }
    public StackLimits stacks() { return stacks; }
    public boolean blocked(ItemStack item) { return item != null && current.blockedItems().contains(item.getType()); }
    public void toggleItem(Material item, Consumer<String> done) { change(rules -> rules.toggleItem(item), done); }
    public void toggleMob(EntityType type, Consumer<String> done) { change(rules -> rules.toggleMob(type), done); }
    public void stack(Material item, Integer limit, Consumer<String> done) { change(rules -> rules.stack(item, limit), done); }

    private void change(UnaryOperator<ItemRules> operation, Consumer<String> done) {
        if (!ready(done)) return;
        ItemRules candidate = operation.apply(current);
        catalog.validate(candidate);
        busy = true;
        writer.execute(() -> {
            try { store.save(candidate); post(() -> commit(candidate, "item-rules-saved", done)); }
            catch (Exception exception) { fail(exception, done); }
        });
    }

    public void reload(Consumer<String> done) {
        if (!ready(done)) return;
        busy = true;
        writer.execute(() -> {
            try { ItemRules loaded = store.load(); post(() -> commit(loaded, "item-rules-reloaded", done)); }
            catch (Exception exception) { fail(exception, done); }
        });
    }

    private void commit(ItemRules rules, String key, Consumer<String> done) {
        current = rules;
        busy = false;
        // Per-player tasks bound each normalization and recheck online state on execution.
        for (var player : Bukkit.getOnlinePlayers()) {
            Bukkit.getScheduler().runTask(plugin, () -> { if (!closed && player.isOnline()) stacks.normalize(player); });
        }
        done.accept(key);
    }

    private boolean ready(Consumer<String> done) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Aturan item harus di server thread");
        if (closed) { done.accept("item-rules-failed"); return false; }
        if (busy) { done.accept("item-rules-busy"); return false; }
        return true;
    }

    private void fail(Exception exception, Consumer<String> done) {
        plugin.getLogger().log(Level.WARNING, "Operasi blocked.yml gagal; aturan aktif dipertahankan.", exception);
        post(() -> { busy = false; done.accept("item-rules-failed"); });
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
            if (!writer.awaitTermination(10, TimeUnit.SECONDS)) plugin.getLogger().warning("Writer aturan item belum selesai setelah 10 detik.");
        } catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
    }
}