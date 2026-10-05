package com.bangzachery.vitae.vitaemanager.roleplay;

import com.bangzachery.vitae.vitaemanager.core.config.ConfigurationService;
import com.bangzachery.vitae.vitaemanager.core.message.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public final class CarryService implements Listener, AutoCloseable {
    private final JavaPlugin plugin;
    private final ConfigurationService configuration;
    private final MessageService messages;
    private final RoleplayService roleplay;
    private final CarryPairs pairs = new CarryPairs();
    private BukkitTask task;
    private boolean closed;

    public CarryService(JavaPlugin plugin, ConfigurationService configuration,
                        MessageService messages, RoleplayService roleplay) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.messages = messages;
        this.roleplay = roleplay;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::reconcile, 5L, 5L);
    }

    public void toggle(Player carrier) {
        if (closed) return;
        reconcile();
        UUID riderId = pairs.rider(carrier.getUniqueId());
        if (riderId != null) {
            boolean released = release(carrier.getUniqueId(), riderId);
            messages.send(carrier, released ? "carry-released" : "carry-failed");
            return;
        }
        if (!eligible(carrier) || !pairs.available(carrier.getUniqueId())) {
            messages.send(carrier, "carry-unavailable"); return;
        }
        double range = configuration.current().roleplay().carryDistance();
        var eye = carrier.getEyeLocation();
        var hit = carrier.getWorld().rayTrace(eye, eye.getDirection(), range,
                FluidCollisionMode.NEVER, true, 0.05,
                entity -> entity instanceof Player && entity != carrier);
        if (hit == null || !(hit.getHitEntity() instanceof Player rider)
                || !roleplay.visible(carrier, rider)) {
            messages.send(carrier, "carry-no-target"); return;
        }
        if (!eligible(rider) || !pairs.available(rider.getUniqueId())) {
            messages.send(carrier, "carry-unavailable"); return;
        }
        // The platform can reject/cancel mounting; never announce success before checking it.
        if (!carrier.addPassenger(rider) || rider.getVehicle() != carrier) {
            messages.send(carrier, "carry-failed"); return;
        }
        pairs.add(carrier.getUniqueId(), rider.getUniqueId());
        messages.send(carrier, "carry-started", Component.text(" " + rider.getName()));
        messages.send(rider, "carry-riding", Component.text(" " + carrier.getName()
                + ". Shift tidak dapat menurunkanmu; penggendong memakai /carry lagi untuk menurunkan."));
    }

    private boolean eligible(Player player) {
        return player.isOnline() && !player.isDead() && player.getGameMode() != GameMode.SPECTATOR
                && player.getVehicle() == null
                && player.getPassengers().stream().noneMatch(entity -> entity instanceof Player);
    }

    private boolean release(UUID carrierId, UUID riderId) {
        if (!riderId.equals(pairs.rider(carrierId))) return true;
        Player carrier = Bukkit.getPlayer(carrierId), rider = Bukkit.getPlayer(riderId);
        if (carrier != null && rider != null && rider.getVehicle() == carrier) {
            pairs.beginRelease(carrierId, riderId);
            try { carrier.removePassenger(rider); }
            finally { pairs.endRelease(riderId); }
            if (rider.getVehicle() == carrier) return false;
        }
        pairs.remove(carrierId, riderId);
        return true;
    }

    private boolean validPair(Player carrier, Player rider) {
        return carrier.isOnline() && rider.isOnline() && !carrier.isDead() && !rider.isDead()
                && carrier.getVehicle() == null
                && carrier.getGameMode() != GameMode.SPECTATOR && rider.getGameMode() != GameMode.SPECTATOR
                && roleplay.visible(carrier, rider);
    }

    private void releasePlayer(UUID player) {
        for (var pair : pairs.snapshot().entrySet()) {
            if (pair.getKey().equals(player) || pair.getValue().equals(player)) {
                release(pair.getKey(), pair.getValue());
            }
        }
    }

    private void reconcile() {
        for (var pair : pairs.snapshot().entrySet()) {
            Player carrier = Bukkit.getPlayer(pair.getKey()), rider = Bukkit.getPlayer(pair.getValue());
            if (carrier == null || rider == null || rider.getVehicle() != carrier) {
                pairs.remove(pair.getKey(), pair.getValue());
            } else if (!validPair(carrier, rider)) {
                release(pair.getKey(), pair.getValue());
            }
        }
    }

    @EventHandler public void quit(PlayerQuitEvent event) { releasePlayer(event.getPlayer().getUniqueId()); }
    @EventHandler public void death(PlayerDeathEvent event) { releasePlayer(event.getEntity().getUniqueId()); }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void teleport(PlayerTeleportEvent event) { releasePlayer(event.getPlayer().getUniqueId()); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void sneak(PlayerToggleSneakEvent event) {
        Player rider = event.getPlayer();
        if (!closed && event.isSneaking() && rider.getVehicle() instanceof Player carrier
                && validPair(carrier, rider)
                && pairs.locksDismount(carrier.getUniqueId(), rider.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void dismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player rider) || !(event.getDismounted() instanceof Player carrier)
                || !rider.getUniqueId().equals(pairs.rider(carrier.getUniqueId()))) return;
        if (!closed && event.isCancellable() && validPair(carrier, rider)
                && pairs.locksDismount(carrier.getUniqueId(), rider.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        // Forced/platform cleanup and our own /carry release are allowed.
        if (!closed) Bukkit.getScheduler().runTask(plugin, () -> { if (!closed) reconcile(); });
    }

    @Override public void close() {
        closed = true;
        if (task != null) task.cancel();
        for (var pair : pairs.snapshot().entrySet()) {
            if (!release(pair.getKey(), pair.getValue())) {
                plugin.getLogger().warning("Plugin lain menolak pelepasan carry: " + pair.getValue());
            }
        }
    }
}