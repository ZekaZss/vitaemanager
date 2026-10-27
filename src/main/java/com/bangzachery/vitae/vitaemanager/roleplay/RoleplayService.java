package com.bangzachery.vitae.vitaemanager.roleplay;

import com.bangzachery.vitae.vitaemanager.core.config.ConfigurationService;
import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import io.papermc.paper.event.player.PlayerUntrackEntityEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class RoleplayService implements Listener, AutoCloseable {
    private final JavaPlugin plugin;
    private final ConfigurationService configuration;
    private final NamespacedKey vanished;
    private final Map<UUID, TextSession> texts = new HashMap<>();
    private BukkitTask task;
    private long ticks;
    private boolean closed;
    private RoleplayDisplayPackets packets;
    private boolean packetWarning;

    public RoleplayService(JavaPlugin plugin, ConfigurationService configuration) {
        this.plugin = plugin;
        this.configuration = configuration;
        vanished = new NamespacedKey(plugin, "is_vanished");
    }

    public void start() {
        if (task != null || closed) return;
        try {
            packets = new RoleplayDisplayPackets();
        } catch (ReflectiveOperationException | LinkageError error) {
            packetFailure(error);
            return;
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void show(Player owner, Component text) {
        if (closed || !owner.isOnline() || owner.isDead()) return;
        if (packets == null) { unavailable(owner); return; }
        remove(owner.getUniqueId());
        TextDisplay display = null;
        try {
            // API-created but never spawned: no tracker, chunk entity, second position authority,
            // passenger mutation on the server, or server movement packets for this display.
            display = owner.getWorld().createEntity(owner.getLocation(), TextDisplay.class);
            display.text(text);
            // Vertical keeps transform Y world-vertical. CENTER rotates it with viewer pitch.
            display.setBillboard(Display.Billboard.VERTICAL);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(100, 0, 0, 0));
            display.setSeeThrough(false); display.setGravity(false); display.setInvulnerable(true);
            display.setPersistent(false); display.setVisibleByDefault(false); display.setLineWidth(240);
            display.setTeleportDuration(0); display.setInterpolationDuration(0); display.setInterpolationDelay(0);
            TextSession session = new TextSession(display, ticks + configuration.current().roleplay().durationTicks());
            placement(owner, session);
            texts.put(owner.getUniqueId(), session);
            visibility(owner, session, true);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            remove(owner.getUniqueId());
            if (display != null) display.remove();
            packetFailure(error); unavailable(owner);
        }
    }

    public boolean visible(Player viewer, Player owner) {
        if (owner == null || !owner.isOnline() || !viewer.canSee(owner)) return false;
        if (owner.getMetadata("vanished").stream().anyMatch(value -> value.asBoolean())) return false;
        var pdc = owner.getPersistentDataContainer();
        if (pdc.has(vanished) && !pdc.has(vanished, PersistentDataType.BOOLEAN)) return false;
        return !Boolean.TRUE.equals(pdc.get(vanished, PersistentDataType.BOOLEAN));
    }

    private void placement(Player owner, TextSession session) throws ReflectiveOperationException {
        double height = owner.getHeight(), clearance = configuration.current().roleplay().textHeadClearance();
        if (height == session.height && clearance == session.clearance && owner.getPose() == session.pose) return;
        float translation = packets.translation(owner, session.display, clearance);
        session.display.setTransformation(new Transformation(new Vector3f(0, translation, 0),
                new Quaternionf(), new Vector3f(1, 1, 1), new Quaternionf()));
        session.height = height; session.clearance = clearance; session.pose = owner.getPose();
        for (UUID id : session.viewers) {
            Player viewer = Bukkit.getPlayer(id);
            if (viewer != null && viewer.isOnline() && viewer.getWorld() == owner.getWorld()) packets.update(viewer, session.display);
        }
    }

    private void tick() {
        ticks++;
        for (UUID id : Set.copyOf(texts.keySet())) {
            TextSession session = texts.get(id);
            Player owner = Bukkit.getPlayer(id);
            if (owner == null || owner.isDead() || !owner.isOnline() || ticks >= session.expires
                    || session.display.getWorld() != owner.getWorld()) {
                remove(id);
                continue;
            }
            try {
                placement(owner, session);
                if (ticks % 5 == 0) visibility(owner, session, false);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
                packetFailure(error); remove(id);
            }
        }
    }

    private void visibility(Player owner, TextSession session, boolean forceAnchor) throws ReflectiveOperationException {
        Set<UUID> online = new HashSet<>();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            UUID id = viewer.getUniqueId();
            online.add(id);
            visibility(viewer, owner, session, forceAnchor);
        }
        session.viewers.retainAll(online);
    }

    private void visibility(Player viewer, Player owner, TextSession session, boolean forceAnchor) throws ReflectiveOperationException {
        UUID id = viewer.getUniqueId();
        boolean show = viewer.getWorld() == owner.getWorld() && visible(viewer, owner)
                && (viewer == owner || owner.getTrackedBy().contains(viewer));
        if (show && session.viewers.add(id)) packets.show(viewer, owner, session.display);
        else if (!show && session.viewers.remove(id)) packets.hide(viewer, owner, session.display, viewer.getWorld() == owner.getWorld() && viewer.canSee(owner));
        else if (show && forceAnchor) packets.attach(viewer, owner, session.display);
    }

    private void remove(UUID owner) {
        TextSession session = texts.remove(owner);
        if (session == null) return;
        Player player = Bukkit.getPlayer(owner);
        for (UUID id : session.viewers) {
            Player viewer = Bukkit.getPlayer(id);
            if (viewer != null && viewer.isOnline()) {
                try {
                    if (player == null) packets.forget(viewer, session.display);
                    else packets.hide(viewer, player, session.display, viewer.getWorld() == player.getWorld() && viewer.canSee(player));
                }
                catch (ReflectiveOperationException | RuntimeException error) { packetFailure(error); }
            }
        }
        session.display.remove();
    }

    private void packetFailure(Throwable error) {
        if (packetWarning) return;
        packetWarning = true;
        plugin.getLogger().log(Level.WARNING, "Tampilan teks roleplay gagal pada adapter Paper 1.21.4; tidak memakai fallback teleport yang dapat bergetar.", error);
    }

    private static void unavailable(Player player) {
        player.sendMessage(Component.text("Teks roleplay belum dapat ditampilkan. Admin perlu memeriksa adapter Paper 1.21.4.", net.kyori.adventure.text.format.NamedTextColor.RED));
    }

    private void refreshMount(Player player) {
        if (!closed && texts.containsKey(player.getUniqueId())) Bukkit.getScheduler().runTask(plugin, () -> {
            TextSession session = texts.get(player.getUniqueId());
            if (!closed && session != null && player.isOnline()) {
                try { visibility(player, session, true); }
                catch (ReflectiveOperationException | RuntimeException error) { packetFailure(error); remove(player.getUniqueId()); }
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void mount(EntityMountEvent event) { if (event.getMount() instanceof Player player) refreshMount(player); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void dismount(EntityDismountEvent event) { if (event.getDismounted() instanceof Player player) refreshMount(player); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void track(PlayerTrackEntityEvent event) {
        if (!(event.getEntity() instanceof Player owner) || !texts.containsKey(owner.getUniqueId())) return;
        UUID ownerId = owner.getUniqueId(), viewerId = event.getPlayer().getUniqueId();
        // Track events fire before vanilla pairing completes. Attach after the owner spawn packet.
        Bukkit.getScheduler().runTask(plugin, () -> {
            TextSession session = texts.get(ownerId); Player viewer = Bukkit.getPlayer(viewerId), player = Bukkit.getPlayer(ownerId);
            if (closed || session == null || viewer == null || player == null || !viewer.isOnline() || !player.isOnline()) return;
            try { visibility(viewer, player, session, false); }
            catch (ReflectiveOperationException | RuntimeException error) { packetFailure(error); remove(ownerId); }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void untrack(PlayerUntrackEntityEvent event) {
        if (!(event.getEntity() instanceof Player owner)) return;
        TextSession session = texts.get(owner.getUniqueId());
        if (session == null || !session.viewers.remove(event.getPlayer().getUniqueId())) return;
        // Only a fake-entity packet; never add/remove world entities in a tracking event.
        try { packets.forget(event.getPlayer(), session.display); }
        catch (ReflectiveOperationException | RuntimeException error) { packetFailure(error); }
    }

    private void forgetViewer(Player viewer) {
        for (var entry : texts.entrySet()) if (entry.getValue().viewers.remove(viewer.getUniqueId())) {
            Player owner = Bukkit.getPlayer(entry.getKey());
            try {
                if (owner == null) packets.forget(viewer, entry.getValue().display);
                else packets.hide(viewer, owner, entry.getValue().display, viewer.getWorld() == owner.getWorld() && viewer.canSee(owner));
            } catch (ReflectiveOperationException | RuntimeException error) { packetFailure(error); }
        }
    }

    @EventHandler public void quit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        remove(id);
        for (TextSession session : texts.values()) session.viewers.remove(id);
    }
    @EventHandler public void death(PlayerDeathEvent event) { remove(event.getEntity().getUniqueId()); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void teleport(PlayerTeleportEvent event) { remove(event.getPlayer().getUniqueId()); forgetViewer(event.getPlayer()); }
    @EventHandler public void world(PlayerChangedWorldEvent event) {
        UUID id = event.getPlayer().getUniqueId(); remove(id);
        forgetViewer(event.getPlayer());
    }

    @Override public void close() {
        closed = true;
        if (task != null) task.cancel();
        for (UUID id : Set.copyOf(texts.keySet())) remove(id);
    }

    private static final class TextSession {
        private final TextDisplay display;
        private final long expires;
        private final Set<UUID> viewers = new HashSet<>();
        private double height = Double.NaN, clearance = Double.NaN;
        private org.bukkit.entity.Pose pose;
        private TextSession(TextDisplay display, long expires) {
            this.display = display;
            this.expires = expires;
        }
    }
}
