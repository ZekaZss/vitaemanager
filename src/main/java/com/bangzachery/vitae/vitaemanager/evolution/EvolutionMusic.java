package com.bangzachery.vitae.vitaemanager.evolution;

import org.bukkit.*;
import org.bukkit.entity.Player;
import java.util.*;

/** Musik memakai kategori RECORDS, terpisah dari VOICE untuk cue dialog/tahap. */
public final class EvolutionMusic implements AutoCloseable {
    private record Track(EvolutionAltar altar, Set<UUID> audience) { }
    private final Map<UUID, Track> tracks = new HashMap<>();
    private final Map<UUID, Map<String, UUID>> ownership = new HashMap<>();
    public void start(UUID id, UUID owner, EvolutionAltar altar) {
        if (tracks.containsKey(id) || altar.music().sound().isEmpty() || altar.music().volume() == 0) return;
        World world = Bukkit.getWorld(altar.table().world()); if (world == null) return;
        Player player = Bukkit.getPlayer(owner);
        List<Player> audience = altar.music().privateSound() ? player == null ? List.of() : List.of(player)
                : world.getPlayers().stream().filter(p -> inside(p, altar)).toList();
        Set<UUID> ids = new HashSet<>();
        for (Player listener : audience) {
            listener.playSound(listener.getLocation(), altar.music().sound(), SoundCategory.RECORDS, altar.music().volume(), 1);
            ids.add(listener.getUniqueId());
            ownership.computeIfAbsent(listener.getUniqueId(), key -> new HashMap<>()).put(altar.music().sound(), id);
        }
        tracks.put(id, new Track(altar, ids));
    }
    public void trim(UUID id) {
        Track track = tracks.get(id); if (track == null || track.altar().music().privateSound()) return;
        for (UUID listenerId : Set.copyOf(track.audience())) {
            Player listener = Bukkit.getPlayer(listenerId);
            if (listener == null || !inside(listener, track.altar())) stopFor(id, track, listenerId);
        }
    }
    private static boolean inside(Player player, EvolutionAltar altar) {
        EvolutionAltar.Point point = altar.table(); Location location = player.getLocation();
        if (!location.getWorld().getUID().equals(point.world())) return false;
        double dx=location.getX()-point.x()-.5, dy=location.getY()-point.y()-.5, dz=location.getZ()-point.z()-.5;
        return dx*dx+dy*dy+dz*dz <= altar.soundRadius()*altar.soundRadius();
    }
    private void stopFor(UUID id, Track track, UUID listenerId) {
        String key = track.altar().music().sound(); Map<String, UUID> sounds = ownership.get(listenerId);
        if (sounds != null && sounds.remove(key, id)) {
            Player player = Bukkit.getPlayer(listenerId); if (player != null) player.stopSound(key, SoundCategory.RECORDS);
            if (sounds.isEmpty()) ownership.remove(listenerId);
        }
        track.audience().remove(listenerId);
    }
    public void stop(UUID id) {
        Track track = tracks.remove(id); if (track != null)
            for (UUID listener : Set.copyOf(track.audience())) stopFor(id, track, listener);
    }
    public void quit(UUID listener) {
        for (var entry : tracks.entrySet()) if (entry.getValue().audience().contains(listener))
            stopFor(entry.getKey(), entry.getValue(), listener);
    }
    @Override public void close() { for (UUID id : Set.copyOf(tracks.keySet())) stop(id); }
}
