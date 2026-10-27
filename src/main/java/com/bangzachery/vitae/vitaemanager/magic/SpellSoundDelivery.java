package com.bangzachery.vitae.vitaemanager.magic;

import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.function.Consumer;

/** Sends bounded sound packets to selected players; never uses global world broadcasts. */
final class SpellSoundDelivery {
    record Key(UUID player, String sound) {}
    private record Source(UUID victim, Location location) {}
    private final Map<Key, Set<SpellSoundDelivery>> loopOwners;
    private final Map<Key, Set<UUID>> owned = new HashMap<>();
    private final Consumer<Key> stop;
    SpellSoundDelivery(Map<Key, Set<SpellSoundDelivery>> loopOwners) {
        this(loopOwners, key -> {
            Player player = Bukkit.getPlayer(key.player());
            if (player != null && player.isOnline()) player.stopSound(key.sound(), SoundCategory.PLAYERS);
        });
    }
    SpellSoundDelivery(Map<Key, Set<SpellSoundDelivery>> loopOwners, Consumer<Key> stop) {
        this.loopOwners = loopOwners; this.stop = stop;
    }
    void emit(SpellSoundPlan.Cue cue, Player owner, Location center, Collection<Player> victims) {
        Set<UUID> victimIds = new HashSet<>();
        for (Player player : victims) victimIds.add(player.getUniqueId());
        List<Source> sources = switch (cue.origin()) {
            case CENTER -> List.of(new Source(null, center));
            case OWNER -> owner == null ? List.of() : List.of(new Source(null, owner.getLocation()));
            case VICTIM -> victims.stream().filter(Player::isOnline).filter(p -> !p.isDead()).map(p -> new Source(p.getUniqueId(), p.getLocation())).toList();
        };
        String key = SpellConfig.soundKey(cue);
        for (Source source : sources) {
            Location origin = source.location();
            if (origin.getWorld() == null) continue;
            for (Player receiver : origin.getWorld().getPlayers()) {
                if (!receiver.isOnline() || receiver.isDead()) continue;
                boolean isOwner = owner != null && owner.getUniqueId().equals(receiver.getUniqueId());
                if (audible(cue, isOwner, victimIds.contains(receiver.getUniqueId()),
                        true, receiver.getLocation().distanceSquared(origin))) {
                    // A victim-local cue is heard only from that victim's body, avoiding N overlapping bat cues.
                    if (cue.origin() == SpellSoundPlan.Origin.VICTIM && cue.audience() == SpellSoundPlan.Audience.VICTIMS
                            && !receiver.getUniqueId().equals(source.victim())) continue;
                    if (cue.intervalTicks() > 0) {
                        Key token = new Key(receiver.getUniqueId(), key);
                        rememberLoop(token, source.victim());
                    }
                    receiver.playSound(origin, key, SoundCategory.PLAYERS, cue.volume(), cue.pitch());
                }
            }
        }
    }
    void rememberLoop(Key token) {
        rememberLoop(token, null);
    }
    void rememberLoop(Key token, UUID sourceVictim) {
        owned.computeIfAbsent(token, ignored -> new HashSet<>()).add(sourceVictim);
        loopOwners.computeIfAbsent(token, ignored -> Collections.newSetFromMap(new IdentityHashMap<>())).add(this);
    }
    /** Retire both this victim's audio sources and every loop sent to their old client session. */
    void releaseVictim(UUID victim) {
        Set<Key> release = new HashSet<>();
        for (var entry : owned.entrySet()) {
            if (entry.getKey().player().equals(victim)) release.add(entry.getKey());
            else if (entry.getValue().remove(victim) && entry.getValue().isEmpty()) release.add(entry.getKey());
        }
        releaseKeys(release);
    }
    void stopRepeats() {
        releaseKeys(Set.copyOf(owned.keySet()));
    }
    private void releaseKeys(Set<Key> keys) {
        RuntimeException failure = null;
        for (Key key : keys) {
            Set<SpellSoundDelivery> owners = loopOwners.get(key);
            owned.remove(key);
            if (owners != null) {
                owners.remove(this);
                if (owners.isEmpty()) {
                    loopOwners.remove(key);
                    try { stop.accept(key); }
                    catch (RuntimeException error) { if (failure == null) failure = error; else failure.addSuppressed(error); }
                }
            }
        }
        if (failure != null) throw failure;
    }
    static boolean audible(SpellSoundPlan.Cue cue, boolean owner, boolean victim, boolean sameWorld, double distanceSquared) {
        if (!sameWorld || distanceSquared > cue.range() * cue.range()) return false;
        return switch (cue.audience()) {
            case OWNER -> owner;
            case VICTIMS -> victim;
            case PARTICIPANTS -> owner || victim;
            case NEARBY -> true;
        };
    }
}
