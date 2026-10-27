package com.bangzachery.vitae.vitaemanager.magic;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Paper 1.21.4 real, fixed swimming pose; no fake blocks or third-party crawl session. Main-thread only. */
final class InternalCrawlPose {
    private static final String RECORD = "tunduk_pose";
    private static final NamespacedKey TOKEN = new NamespacedKey("vitaemanager", "magic_tunduk_pose");
    private final VictimJournal journal;
    private final Map<UUID, Lease> leases = new HashMap<>();
    private final Set<UUID> changing = new HashSet<>();

    InternalCrawlPose(VictimJournal journal) { this.journal = journal; }

    void validate(Player player) {
        if (player.isDead() || player.isInsideVehicle() || !player.getPassengers().isEmpty())
            throw new IllegalStateException("Target sedang mati atau terikat kendaraan/penumpang lain");
        if (player.hasFixedPose() || leases.containsKey(player.getUniqueId()))
            throw new IllegalStateException("Target memiliki pose tetap dari mekanik lain");
        if (journal.records(player.getUniqueId()).containsKey(RECORD))
            throw new IllegalStateException("Journal pose target belum selesai dipulihkan");
    }

    void acquire(Player player, UUID use) {
        validate(player);
        Lease lease = new Lease(UUID.randomUUID().toString(), use, player.getPose(), player.isSwimming());
        persist(player.getUniqueId(), lease);
        leases.put(player.getUniqueId(), lease);
        player.getPersistentDataContainer().set(TOKEN, PersistentDataType.STRING, lease.token);
        changing.add(player.getUniqueId());
        try {
            // The swim flag lets clients observe the native state; fixed pose prevents vanilla standing updates.
            player.setSwimming(true);
            player.setPose(Pose.SWIMMING, true);
            BoundingBox box = player.getBoundingBox();
            if (!player.hasFixedPose() || player.getPose() != Pose.SWIMMING || !player.isSwimming()
                    || box.getHeight() > box.getWidthX() * 1.1 + 0.001)
                throw new IllegalStateException("Paper tidak menerapkan pose/hitbox crawl yang diperlukan");
        } catch (RuntimeException failure) {
            release(player);
            throw failure;
        } finally {
            changing.remove(player.getUniqueId());
        }
    }

    /** Vanilla/user attempts to leave swimming may not release a Tunduk victim. */
    boolean holdsSwimming(Player player) {
        Lease lease = leases.get(player.getUniqueId());
        return lease != null && !lease.foreign && !changing.contains(player.getUniqueId()) && matches(player, lease);
    }

    void check(Player player) {
        Lease lease = leases.get(player.getUniqueId());
        if (lease == null || lease.foreign || lease.dying) return;
        if (!matches(player, lease)) {
            externalChange(player);
            return;
        }
        // Do not overwrite another plugin's pose. The movement/ability lock remains owned by MagicService.
        if (!player.isSwimming()) {
            changing.add(player.getUniqueId());
            try { player.setSwimming(true); }
            finally { changing.remove(player.getUniqueId()); }
        }
    }

    void externalChange(Player player) {
        externalChange(player, null);
    }

    void externalChange(Player player, Pose nextPose) {
        Lease lease = leases.get(player.getUniqueId());
        if (lease == null || changing.contains(player.getUniqueId()) || lease.foreign) return;
        // A dying transition may be announced before PlayerDeathEvent. Keep our token and
        // journal so death cleanup can release the forced flag rather than treating death as another plugin.
        if (nextPose == Pose.DYING) {
            lease.dying = true;
            return;
        }
        lease.foreign = true;
        clearEvidence(player, lease);
    }

    void release(Player player) {
        Lease lease = leases.get(player.getUniqueId());
        if (lease == null) return;
        if (!lease.foreign && (matches(player, lease) || lease.dying && tokenMatches(player, lease)
                && player.getPose() == Pose.DYING)) restore(player, lease);
        clearEvidence(player, lease);
        leases.remove(player.getUniqueId());
    }

    void recover(Player player) {
        String value = journal.records(player.getUniqueId()).get(RECORD);
        if (value == null) {
            if (!player.hasFixedPose()) player.getPersistentDataContainer().remove(TOKEN);
            return;
        }
        Lease lease = parse(value);
        String token = player.getPersistentDataContainer().get(TOKEN, PersistentDataType.STRING);
        // Fixed pose is transient across a complete server restart. Do not overwrite the new native pose.
        if (!player.hasFixedPose()) {
            clearEvidence(player, lease);
            return;
        }
        if (!lease.token.equals(token) || player.getPose() != Pose.SWIMMING)
            throw new IllegalStateException("Sumber pose aktif tidak sesuai token pemulihan; journal dipertahankan");
        restore(player, lease);
        clearEvidence(player, lease);
    }

    private boolean matches(Player player, Lease lease) {
        return tokenMatches(player, lease)
                && player.hasFixedPose() && player.getPose() == Pose.SWIMMING;
    }

    private boolean tokenMatches(Player player, Lease lease) {
        return lease.token.equals(player.getPersistentDataContainer().get(TOKEN, PersistentDataType.STRING));
    }

    private void restore(Player player, Lease lease) {
        changing.add(player.getUniqueId());
        try {
            player.setPose(player.isDead() || lease.dying ? Pose.DYING : lease.beforePose, false);
            player.setSwimming(lease.beforeSwimming);
        } finally { changing.remove(player.getUniqueId()); }
    }

    private void persist(UUID victim, Lease lease) {
        try { journal.put(victim, RECORD, "1;" + lease.token + ";" + lease.use + ";" + lease.beforePose.name() + ";" + lease.beforeSwimming); }
        catch (IOException error) { throw new IllegalStateException("Journal pose gagal disimpan sebelum aktivasi", error); }
    }

    private void clearEvidence(Player player, Lease lease) {
        try { journal.remove(player.getUniqueId(), RECORD); }
        catch (IOException error) { throw new IllegalStateException("Journal pose belum berhasil dibersihkan", error); }
        if (lease.token.equals(player.getPersistentDataContainer().get(TOKEN, PersistentDataType.STRING)))
            player.getPersistentDataContainer().remove(TOKEN);
    }

    private static Lease parse(String value) {
        String[] fields = value.split(";", -1);
        if (fields.length != 5 || !fields[0].equals("1") || !(fields[4].equals("true") || fields[4].equals("false")))
            throw new IllegalArgumentException("Record pose Tunduk rusak; journal dipertahankan");
        UUID.fromString(fields[1]);
        return new Lease(fields[1], UUID.fromString(fields[2]), Pose.valueOf(fields[3]), Boolean.parseBoolean(fields[4]));
    }

    private static final class Lease {
        final String token;
        final UUID use;
        final Pose beforePose;
        final boolean beforeSwimming;
        boolean foreign, dying;
        Lease(String token, UUID use, Pose beforePose, boolean beforeSwimming) {
            this.token = token; this.use = use; this.beforePose = beforePose; this.beforeSwimming = beforeSwimming;
        }
    }
}
