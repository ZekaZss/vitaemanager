package com.bangzachery.vitae.vitaemanager.whisper;

import java.util.Objects;
import java.util.UUID;

/** Inclusive block corners; containment uses the whole selected blocks, not their centers. */
public record WhisperArea(UUID world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public WhisperArea {
        Objects.requireNonNull(world);
        if (minX > maxX || minY > maxY || minZ > maxZ
                || minX < -30_000_000 || maxX > 30_000_000 || minZ < -30_000_000 || maxZ > 30_000_000
                || minY < -4096 || maxY > 4096
                || (long) maxX - minX > 255 || (long) maxZ - minZ > 255 || maxY - minY > 511) {
            throw new IllegalArgumentException("Area tidak valid; maksimum 256 x 512 x 256 blok, kedua titik dalam dunia yang sama.");
        }
    }

    public static WhisperArea between(UUID world, int ax, int ay, int az, int bx, int by, int bz) {
        return new WhisperArea(world, Math.min(ax, bx), Math.min(ay, by), Math.min(az, bz),
                Math.max(ax, bx), Math.max(ay, by), Math.max(az, bz));
    }

    public boolean contains(UUID worldId, double x, double y, double z) {
        return world.equals(worldId) && finite(x, y, z)
                && x >= minX && x < (double) maxX + 1 && y >= minY && y < (double) maxY + 1
                && z >= minZ && z < (double) maxZ + 1;
    }

    /** Swept movement also catches walking completely across a one-block trigger in one update. */
    public boolean crossed(UUID worldId, double ax, double ay, double az, double bx, double by, double bz) {
        if (!world.equals(worldId) || !finite(ax, ay, az) || !finite(bx, by, bz)
                || contains(worldId, ax, ay, az)) return false;
        double lo = 0, hi = 1;
        double[] from = {ax, ay, az}, to = {bx, by, bz};
        double[] min = {minX, minY, minZ}, max = {maxX + 1.0, maxY + 1.0, maxZ + 1.0};
        for (int axis = 0; axis < 3; axis++) {
            double delta = to[axis] - from[axis];
            if (delta == 0) {
                if (from[axis] < min[axis] || from[axis] >= max[axis]) return false;
            } else {
                double enter = (min[axis] - from[axis]) / delta;
                double exit = (max[axis] - from[axis]) / delta;
                lo = Math.max(lo, Math.min(enter, exit));
                hi = Math.min(hi, Math.max(enter, exit));
                if (lo > hi) return false;
            }
        }
        // Test an interior point; touching the exclusive upper face does not count as entering.
        double t = (lo + hi) / 2;
        return contains(worldId, ax + (bx - ax) * t, ay + (by - ay) * t, az + (bz - az) * t);
    }

    private static boolean finite(double x, double y, double z) {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
    }
}