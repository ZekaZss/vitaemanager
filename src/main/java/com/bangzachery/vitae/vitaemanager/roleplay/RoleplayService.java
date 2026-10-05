package com.bangzachery.vitae.vitaemanager.roleplay;

import com.bangzachery.vitae.vitaemanager.core.config.ConfigurationService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RoleplayService implements Listener, AutoCloseable {
    private final JavaPlugin plugin;
    private final ConfigurationService configuration;
    private final NamespacedKey vanished;
    private final Map<UUID, TextSession> texts = new HashMap<>();
    private BukkitTask task;
    private long ticks;
    private boolean closed;

    public RoleplayService(JavaPlugin plugin, ConfigurationService configuration) {
        this.plugin = plugin;
        this.configuration = configuration;
        vanished = new NamespacedKey(plugin, "is_vanished");
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 5L, 5L);
    }

    public void show(Player owner, Component text) {
        if (closed || !owner.isOnline() || owner.isDead()) return;
        remove(owner.getUniqueId());
        TextDisplay display = owner.getWorld().spawn(position(owner), TextDisplay.class, entity -> {
            entity.text(text);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setDefaultBackground(false);
            entity.setBackgroundColor(Color.fromARGB(100, 0, 0, 0));
            entity.setSeeThrough(false);
            entity.setGravity(false);
            entity.setInvulnerable(true);
            entity.setPersistent(false);
            entity.setVisibleByDefault(false);
            entity.setLineWidth(240);
            entity.setTeleportDuration(5);
        });
        TextSession session = new TextSession(display,
                ticks + configuration.current().roleplay().durationTicks());
        texts.put(owner.getUniqueId(), session);
        visibility(owner, session);
    }

    public boolean visible(Player viewer, Player owner) {
        if (owner == null || !owner.isOnline() || !viewer.canSee(owner)) return false;
        if (owner.getMetadata("vanished").stream().anyMatch(value -> value.asBoolean())) return false;
        var pdc = owner.getPersistentDataContainer();
        if (pdc.has(vanished) && !pdc.has(vanished, PersistentDataType.BOOLEAN)) return false;
        return !Boolean.TRUE.equals(pdc.get(vanished, PersistentDataType.BOOLEAN));
    }

    private Location position(Player player) {
        return player.getLocation().add(0, player.getHeight() + 0.5, 0);
    }

    private void tick() {
        ticks += 5;
        for (UUID id : Set.copyOf(texts.keySet())) {
            TextSession session = texts.get(id);
            Player owner = Bukkit.getPlayer(id);
            if (owner == null || owner.isDead() || !owner.isOnline() || ticks >= session.expires
                    || !session.display.isValid() || session.display.getWorld() != owner.getWorld()) {
                remove(id);
                continue;
            }
            Location destination = position(owner);
            if (session.display.getLocation().distanceSquared(destination) > 0.0001) {
                if (!session.display.teleport(destination)) { remove(id); continue; }
            }
            visibility(owner, session);
        }
    }

    private void visibility(Player owner, TextSession session) {
        Set<UUID> online = new HashSet<>();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            UUID id = viewer.getUniqueId();
            online.add(id);
            boolean show = viewer.getWorld() == owner.getWorld() && visible(viewer, owner);
            if (show && session.viewers.add(id)) viewer.showEntity(plugin, session.display);
            else if (!show && session.viewers.remove(id)) viewer.hideEntity(plugin, session.display);
        }
        session.viewers.retainAll(online);
    }

    private void remove(UUID owner) {
        TextSession session = texts.remove(owner);
        if (session != null) session.display.remove();
    }

    @EventHandler public void quit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        remove(id);
        for (TextSession session : texts.values()) session.viewers.remove(id);
    }
    @EventHandler public void death(PlayerDeathEvent event) { remove(event.getEntity().getUniqueId()); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void teleport(PlayerTeleportEvent event) { remove(event.getPlayer().getUniqueId()); }

    @Override public void close() {
        closed = true;
        if (task != null) task.cancel();
        for (UUID id : Set.copyOf(texts.keySet())) remove(id);
    }

    private static final class TextSession {
        private final TextDisplay display;
        private final long expires;
        private final Set<UUID> viewers = new HashSet<>();
        private TextSession(TextDisplay display, long expires) {
            this.display = display;
            this.expires = expires;
        }
    }
}