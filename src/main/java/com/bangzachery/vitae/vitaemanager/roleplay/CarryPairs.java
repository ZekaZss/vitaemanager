package com.bangzachery.vitae.vitaemanager.roleplay;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-thread ownership of pairs created by this module, never other plugins' mounts. */
public final class CarryPairs {
    private final Map<UUID, UUID> riders = new HashMap<>();
    private final Map<UUID, Integer> releasing = new HashMap<>();

    public boolean available(UUID player) {
        return !riders.containsKey(player) && !riders.containsValue(player);
    }

    public void add(UUID carrier, UUID rider) {
        if (carrier == null || rider == null || carrier.equals(rider)
                || !available(carrier) || !available(rider)) {
            throw new IllegalArgumentException("Player is already in a carry pair");
        }
        riders.put(carrier, rider);
    }

    public UUID rider(UUID carrier) { return riders.get(carrier); }

    public boolean locksDismount(UUID carrier, UUID rider) {
        return rider != null && rider.equals(riders.get(carrier)) && !releasing.containsKey(rider);
    }

    public void beginRelease(UUID carrier, UUID rider) {
        if (rider == null || !rider.equals(riders.get(carrier))) {
            throw new IllegalArgumentException("Bukan pasangan carry milik Vitae");
        }
        releasing.merge(rider, 1, Integer::sum);
    }

    public void endRelease(UUID rider) {
        releasing.computeIfPresent(rider, (id, depth) -> depth == 1 ? null : depth - 1);
    }

    public void remove(UUID carrier, UUID rider) { riders.remove(carrier, rider); }
    public Map<UUID, UUID> snapshot() { return Map.copyOf(riders); }
}