package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.whisper.WhisperArea;

/** Inclusive, chunk-ordered traversal without allocating a list of every selected block. */
final class NpcFarmCursor {
    private final WhisperArea area;
    private int chunkX, chunkZ, x, y, z;
    private long visited;
    private boolean done;

    NpcFarmCursor(WhisperArea area) {
        this.area = area; chunkX = area.minX() >> 4; chunkZ = area.minZ() >> 4;
        firstInChunk();
    }
    int x() { return x; }
    int y() { return y; }
    int z() { return z; }
    int chunkX() { return chunkX; }
    int chunkZ() { return chunkZ; }
    long visited() { return visited; }
    long volume() {
        return com.bangzachery.vitae.vitaemanager.selection.AreaScanner.volume(area);
    }
    boolean done() { return done; }
    void next() {
        if (done) return;
        visited++;
        if (++y <= area.maxY()) return;
        y = area.minY();
        if (++x <= Math.min(area.maxX(), (chunkX << 4) + 15)) return;
        x = Math.max(area.minX(), chunkX << 4);
        if (++z <= Math.min(area.maxZ(), (chunkZ << 4) + 15)) return;
        nextChunk();
    }
    private void nextChunk() {
        if (++chunkX > (area.maxX() >> 4)) { chunkX = area.minX() >> 4; chunkZ++; }
        if (chunkZ > (area.maxZ() >> 4)) { done = true; return; }
        firstInChunk();
    }
    private void firstInChunk() {
        x = Math.max(area.minX(), chunkX << 4); y = area.minY(); z = Math.max(area.minZ(), chunkZ << 4);
    }
}
