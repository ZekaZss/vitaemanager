package com.bangzachery.vitae.vitaemanager.roleplay;

import com.bangzachery.vitae.vitaemanager.core.config.ConfigurationService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
    private Constructor<?> friendlyBuffer;
    private Method writeInt, writeInts, decodePacket, getHandle, sendPacket;
    private Method createBuffer, releaseBuffer;
    private Field connection;
    private Object passengerCodec;
    private boolean packetWarning;

    public RoleplayService(JavaPlugin plugin, ConfigurationService configuration) {
        this.plugin = plugin;
        this.configuration = configuration;
        vanished = new NamespacedKey(plugin, "is_vanished");
    }

    public void start() {
        try {
            Class<?> friendly = Class.forName("net.minecraft.network.FriendlyByteBuf");
            Class<?> byteBuf = Class.forName("io.netty.buffer.ByteBuf");
            createBuffer = Class.forName("io.netty.buffer.Unpooled").getMethod("buffer");
            releaseBuffer = byteBuf.getMethod("release");
            friendlyBuffer = friendly.getConstructor(byteBuf);
            writeInt = friendly.getMethod("writeVarInt", int.class);
            writeInts = friendly.getMethod("writeVarIntArray", int[].class);
            decodePacket = Class.forName("net.minecraft.network.codec.StreamDecoder").getMethod("decode", Object.class);
            passengerCodec = Class.forName("net.minecraft.network.protocol.game.ClientboundSetPassengersPacket")
                    .getField("STREAM_CODEC").get(null);
            connection = Class.forName("net.minecraft.server.level.ServerPlayer").getField("connection");
            sendPacket = Class.forName("net.minecraft.server.network.ServerGamePacketListenerImpl")
                    .getMethod("send", Class.forName("net.minecraft.network.protocol.Packet"));
        } catch (ReflectiveOperationException | LinkageError error) {
            passengerCodec = null;
            packetFailure(error);
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
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
            entity.setTeleportDuration(0);
            entity.setInterpolationDuration(0);
            entity.setTransformation(new Transformation(new Vector3f(0, passengerCodec == null ? 0 : .95f, 0),
                    new Quaternionf(), new Vector3f(1, 1, 1), new Quaternionf()));
        });
        TextSession session = new TextSession(display,
                ticks + configuration.current().roleplay().durationTicks());
        texts.put(owner.getUniqueId(), session);
        visibility(owner, session, true);
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
        ticks++;
        for (UUID id : Set.copyOf(texts.keySet())) {
            TextSession session = texts.get(id);
            Player owner = Bukkit.getPlayer(id);
            if (owner == null || owner.isDead() || !owner.isOnline() || ticks >= session.expires
                    || !session.display.isValid() || session.display.getWorld() != owner.getWorld()) {
                remove(id);
                continue;
            }
            // The server position keeps tracking/chunks current. Clients anchor the text to the
            // rendered player, without making the player a real vehicle that blocks teleporting.
            Location destination = position(owner);
            if (session.display.getLocation().distanceSquared(destination) > .0001) {
                if (!session.display.teleport(destination)) { remove(id); continue; }
            }
            if (ticks % 5 == 0) visibility(owner, session, false);
        }
    }

    private void visibility(Player owner, TextSession session, boolean forceAnchor) {
        Set<UUID> online = new HashSet<>();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            UUID id = viewer.getUniqueId();
            online.add(id);
            boolean show = viewer.getWorld() == owner.getWorld() && visible(viewer, owner)
                    && (viewer == owner || owner.getTrackedBy().contains(viewer));
            if (show && session.viewers.add(id)) {
                viewer.showEntity(plugin, session.display);
                anchor(viewer, owner, session.display);
            } else if (!show && session.viewers.remove(id)) {
                viewer.hideEntity(plugin, session.display);
                if (viewer.getWorld() == owner.getWorld() && viewer.canSee(owner)) anchor(viewer, owner, null);
            } else if (show && (forceAnchor || ticks % 20 == 0)) anchor(viewer, owner, session.display);
        }
        session.viewers.retainAll(online);
    }

    private void remove(UUID owner) {
        TextSession session = texts.remove(owner);
        if (session == null) return;
        Player player = Bukkit.getPlayer(owner);
        if (player != null) for (UUID id : session.viewers) {
            Player viewer = Bukkit.getPlayer(id);
            if (viewer != null && viewer.getWorld() == player.getWorld() && viewer.canSee(player)) anchor(viewer, player, null);
        }
        session.display.remove();
    }

    private void anchor(Player viewer, Player owner, TextDisplay display) {
        if (passengerCodec == null || !viewer.isOnline()) return;
        Object buffer = null;
        try {
            buffer = createBuffer.invoke(null);
            int[] passengers = owner.getPassengers().stream().mapToInt(Entity::getEntityId).toArray();
            if (display != null) {
                passengers = java.util.Arrays.copyOf(passengers, passengers.length + 1);
                passengers[passengers.length - 1] = display.getEntityId();
            }
            Object data = friendlyBuffer.newInstance(buffer);
            writeInt.invoke(data, owner.getEntityId());
            writeInts.invoke(data, (Object) passengers);
            Object packet = decodePacket.invoke(passengerCodec, data);
            if (getHandle == null) getHandle = viewer.getClass().getMethod("getHandle");
            sendPacket.invoke(connection.get(getHandle.invoke(viewer)), packet);
        } catch (ReflectiveOperationException | RuntimeException error) { packetFailure(error); }
        finally {
            if (buffer != null) try { releaseBuffer.invoke(buffer); }
            catch (ReflectiveOperationException error) { packetFailure(error); }
        }
    }

    private void packetFailure(Throwable error) {
        if (packetWarning) return;
        packetWarning = true;
        plugin.getLogger().log(Level.WARNING, "Sinkronisasi teks roleplay belum dapat dipasang; periksa versi Paper 1.21.4.", error);
    }

    private void refreshMount(Player player) {
        if (!closed && texts.containsKey(player.getUniqueId())) Bukkit.getScheduler().runTask(plugin, () -> {
            TextSession session = texts.get(player.getUniqueId());
            if (!closed && session != null && player.isOnline()) visibility(player, session, true);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void mount(EntityMountEvent event) { if (event.getMount() instanceof Player player) refreshMount(player); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void dismount(EntityDismountEvent event) { if (event.getDismounted() instanceof Player player) refreshMount(player); }

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
