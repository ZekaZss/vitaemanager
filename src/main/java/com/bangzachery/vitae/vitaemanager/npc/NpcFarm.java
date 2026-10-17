package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.whisper.WhisperArea;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;

/** Farm world access stays on the server thread, with a shared budget for all NPCs. */
final class NpcFarm implements AutoCloseable {
    static final int BLOCKS_PER_TICK = 512;
    private static final int SLICE = 32;
    private static final long NANOS_PER_TICK = 3_000_000;
    private static final Set<Material> PLANTS = Set.of(Material.CARROTS, Material.POTATOES, Material.WHEAT, Material.BEETROOTS);
    private final Deque<Job> jobs = new ArrayDeque<>();
    private boolean closed;

    final class Handle {
        private final Job job;
        private Handle(Job job) { this.job = job; }
        void cancel() { job.cancelled = true; jobs.remove(job); }
    }
    private abstract static class Job {
        final WhisperArea area;
        final BooleanSupplier active;
        final Consumer<String> failure;
        final Map<String, BlockData> data = new HashMap<>();
        boolean cancelled, done;
        CompletableFuture<Chunk> loading;
        int loadingX, loadingZ;
        long waitingSince;
        Job(WhisperArea area, BooleanSupplier active, Consumer<String> failure) {
            this.area = area; this.active = active; this.failure = failure;
        }
        BlockData data(String value) { return data.computeIfAbsent(value, Bukkit::createBlockData); }
        boolean loaded(World world, int x, int z) {
            int cx = x >> 4, cz = z >> 4;
            if (world.isChunkLoaded(cx, cz)) { loading = null; waitingSince = 0; return true; }
            if (loading == null || cx != loadingX || cz != loadingZ) {
                loadingX = cx; loadingZ = cz; waitingSince = System.currentTimeMillis();
                loading = world.getChunkAtAsync(cx, cz, false); // Existing terrain only; never generate new chunks.
                return false;
            }
            if (loading.isDone()) {
                if (loading.join() == null) throw new IllegalStateException("Chunk ladang belum tersedia. Bangun/muat ladangnya dahulu.");
                loading = null;
            }
            if (waitingSince > 0 && System.currentTimeMillis() - waitingSince > 60_000)
                throw new IllegalStateException("Chunk ladang belum bisa dimuat; coba kembali setelah dunia siap.");
            return false;
        }
        abstract int step(World world, int allowance);
        abstract void success();
    }
    Handle scan(WhisperArea area, BooleanSupplier active, BiConsumer<Long, Long> progress,
                Consumer<List<NpcData.Crop>> success, Consumer<String> failure) {
        if (closed) throw new IllegalStateException("Modul ladang sedang ditutup.");
        World world = Objects.requireNonNull(Bukkit.getWorld(area.world()), "Dunia ladang belum dimuat.");
        if (area.minY() <= world.getMinHeight() || area.maxY() >= world.getMaxHeight())
            throw new IllegalArgumentException("Pilih tanaman di dalam ketinggian dunia; tanah di bawahnya juga harus tersedia.");
        Job job = new Job(area, active, failure) {
            final NpcFarmCursor cursor = new NpcFarmCursor(area);
            final List<NpcData.Crop> crops = new ArrayList<>();
            long lastProgress;
            @Override int step(World current, int allowance) {
                int used = 0;
                while (!cursor.done() && used < allowance) {
                    if (!loaded(current, cursor.x(), cursor.z())) break;
                    Block b = current.getBlockAt(cursor.x(), cursor.y(), cursor.z());
                    if (PLANTS.contains(b.getType())) {
                        Block soil = b.getRelative(BlockFace.DOWN);
                        if (soil.getType() != Material.FARMLAND)
                            throw new IllegalArgumentException("Tanaman pada " + cursor.x() + ", " + cursor.y() + ", " + cursor.z() + " harus di atas farmland.");
                        Ageable plant = (Ageable) b.getBlockData(); plant.setAge(plant.getMaximumAge());
                        crops.add(new NpcData.Crop(cursor.x(), cursor.y(), cursor.z(), plant.getAsString(), soil.getBlockData().getAsString()));
                    }
                    cursor.next(); used++;
                }
                long now = System.currentTimeMillis();
                if (now - lastProgress >= 1000 || cursor.done()) { progress.accept(cursor.visited(), cursor.volume()); lastProgress = now; }
                done = cursor.done();
                return used;
            }
            @Override void success() {
                if (crops.isEmpty()) throw new IllegalArgumentException("Siapkan minimal satu wortel/kentang/gandum/bit di dalam seleksi.");
                success.accept(List.copyOf(crops));
            }
        };
        jobs.addLast(job); return new Handle(job);
    }
    Handle restore(NpcData.Quest quest, BooleanSupplier active, Consumer<NpcData.Crop> planted,
                   Runnable success, Consumer<String> failure) {
        if (closed) throw new IllegalStateException("Modul ladang sedang ditutup.");
        Job job = new Job(quest.area(), active, failure) {
            int index;
            @Override int step(World world, int allowance) {
                int used = 0;
                while (index < quest.crops().size() && used < allowance) {
                    NpcData.Crop crop = quest.crops().get(index);
                    if (!loaded(world, crop.x(), crop.z())) break;
                    if (crop.y() <= world.getMinHeight() || crop.y() >= world.getMaxHeight())
                        throw new IllegalArgumentException("Ketinggian snapshot ladang di luar dunia.");
                    world.getBlockAt(crop.x(), crop.y() - 1, crop.z()).setBlockData(data(crop.soil()), false);
                    world.getBlockAt(crop.x(), crop.y(), crop.z()).setBlockData(data(crop.plant()), false);
                    planted.accept(crop); index++; used++;
                }
                done = index == quest.crops().size(); return used;
            }
            @Override void success() { success.run(); }
        };
        jobs.addLast(job); return new Handle(job);
    }
    boolean busy(WhisperArea area) { return jobs.stream().anyMatch(j -> !j.cancelled && NpcData.overlaps(j.area, area)); }
    boolean contains(NpcData.Point point) { return jobs.stream().anyMatch(j -> !j.cancelled && NpcData.inside(j.area, point)); }
    void tick() {
        long until = System.nanoTime() + NANOS_PER_TICK;
        int used = 0, idle = 0;
        while (!jobs.isEmpty() && used < BLOCKS_PER_TICK && System.nanoTime() < until) {
            Job job = jobs.removeFirst();
            if (job.cancelled) continue;
            try {
                if (!job.active.getAsBoolean()) throw new IllegalStateException("Pemrosesan ladang dibatalkan; setup lama dipertahankan.");
                World world = Bukkit.getWorld(job.area.world());
                int amount = world == null ? 0 : job.step(world, Math.min(SLICE, BLOCKS_PER_TICK - used));
                used += amount;
                if (job.done) { job.success(); idle = 0; }
                else { jobs.addLast(job); idle = amount == 0 ? idle + 1 : 0; }
                if (idle >= jobs.size()) break; // Every queued job is waiting; retry on the next tick.
            } catch (RuntimeException e) {
                job.cancelled = true;
                Throwable cause = e; while (cause.getCause() != null) cause = cause.getCause();
                job.failure.accept(Objects.requireNonNullElse(cause.getMessage(), "Pemrosesan ladang gagal."));
            }
        }
    }
    @Override public void close() { closed = true; for (Job job : jobs) job.cancelled = true; jobs.clear(); }
}
