package com.bangzachery.vitae.vitaemanager.npc;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Level;

/** Native humanoid packets for Paper 1.21.4; ModelEngine is an optional adapter. */
public final class NpcRenderer implements AutoCloseable {
    @FunctionalInterface public interface Input { void click(Player player, String npc, boolean attack); }
    private record Visible(NpcData.Definition definition, Interaction hitbox, int avatarId, UUID avatarUuid, ArmorStand base,
                           Object modeled, Set<UUID> viewers, Set<UUID> failed) {}
    private final JavaPlugin plugin;
    private final NamespacedKey key;
    private final Input input;
    private final Map<String, Visible> entities = new HashMap<>();
    private final Map<Integer, Visible> avatars = new HashMap<>();
    private final Listener modelListener = new Listener() {};
    private final Set<Chunk> tickets = new HashSet<>();
    public NpcRenderer(JavaPlugin plugin, Input input) {
        this.plugin = plugin; this.input = input; key = new NamespacedKey(plugin, "npc_id");
        hookModel();
    }
    public static Location location(NpcData.Point p) {
        World world = Bukkit.getWorld(p.world());
        if (world == null) throw new IllegalArgumentException("Dunia NPC belum dimuat.");
        return new Location(world, p.x(), p.y(), p.z(), p.yaw(), p.pitch());
    }
    public static NpcData.Point point(Location p) {
        return new NpcData.Point(p.getWorld().getUID(), p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch());
    }
    public String id(Entity entity) { return entity.getPersistentDataContainer().get(key, PersistentDataType.STRING); }
    /** Only accept packets for a humanoid avatar actually shown to this player. */
    public String id(Player player, int entityId) {
        Visible visible = avatars.get(entityId);
        return visible != null && visible.viewers().contains(player.getUniqueId())
                && player.getWorld().getUID().equals(visible.definition().location().world()) ? visible.definition().id() : null;
    }
    @SuppressWarnings("deprecation")
    private static int nextAvatarId() { return Bukkit.getUnsafe().nextEntityId(); }
    public void refresh(Map<String, NpcData.Definition> definitions) {
        for (String id : List.copyOf(entities.keySet())) {
            NpcData.Definition next = definitions.get(id);
            if (next == null || !next.enabled() || !next.equals(entities.get(id).definition())) remove(id);
        }
        for (var d : definitions.values()) if (d.enabled() && !entities.containsKey(d.id()) && Bukkit.getWorld(d.location().world()) != null) {
            Interaction box = null; ArmorStand base = null; Object modeled = null;
            try {
                Location at = location(d.facingLocation());
                Chunk chunk = at.getChunk(); chunk.addPluginChunkTicket(plugin); tickets.add(chunk);
                box = at.getWorld().spawn(at, Interaction.class, e -> {
                    e.setPersistent(false); e.setInteractionWidth(d.kind() == NpcData.Kind.HUMAN ? .8f : 1.5f);
                    e.setInteractionHeight((float) Math.max(1.8, d.eyeHeight() + .2));
                    e.getPersistentDataContainer().set(key, PersistentDataType.STRING, d.id());
                });
                if (d.kind() == NpcData.Kind.MODEL) {
                    if (!Bukkit.getPluginManager().isPluginEnabled("ModelEngine")) throw new IllegalArgumentException("ModelEngine belum aktif.");
                    base = at.getWorld().spawn(at, ArmorStand.class, e -> {
                        e.setVisible(false); e.setMarker(true); e.setGravity(false); e.setInvulnerable(true);
                        e.setPersistent(false); e.getPersistentDataContainer().set(key, PersistentDataType.STRING, d.id());
                    });
                    Class<?> api = Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");
                    Class<?> modelType = Class.forName("com.ticxo.modelengine.api.model.ModeledEntity");
                    Class<?> activeType = Class.forName("com.ticxo.modelengine.api.model.ActiveModel");
                    modeled = api.getMethod("createModeledEntity", Entity.class).invoke(null, base);
                    modelType.getMethod("setSaved", boolean.class).invoke(modeled, false);
                    modelType.getMethod("setBaseEntityVisible", boolean.class).invoke(modeled, false);
                    modelType.getMethod("setModelRotationLocked", boolean.class).invoke(modeled, true);
                    Object active = api.getMethod("createActiveModel", String.class).invoke(null, d.model());
                    if (active == null) throw new IllegalArgumentException("ID ModelEngine tidak ditemukan: " + d.model());
                    modelType.getMethod("addModel", activeType, boolean.class).invoke(modeled, active, false);
                    modelRotation(modelType, modeled, "setYBodyRot", at.getYaw());
                    modelRotation(modelType, modeled, "setYHeadRot", at.getYaw());
                    modelRotation(modelType, modeled, "setXHeadRot", at.getPitch());
                }
                // Never disguise the real Interaction entity under the same network ID.
                // Cancelling a use packet can resend its metadata, which is not Player metadata.
                int avatarId = d.kind() == NpcData.Kind.HUMAN ? nextAvatarId() : -1;
                Visible visible = new Visible(d, box, avatarId, UUID.randomUUID(), base, modeled, new HashSet<>(), new HashSet<>());
                entities.put(d.id(), visible);
                if (d.kind() == NpcData.Kind.HUMAN) avatars.put(avatarId, visible);
            } catch (Exception e) {
                if (modeled != null) destroyModel(modeled); if (base != null) base.remove(); if (box != null) box.remove();
                releaseUnusedTickets();
                throw new IllegalStateException("NPC " + d.id() + " gagal ditampilkan. Periksa versi server/model.", e);
            }
        }
        releaseUnusedTickets();
    }
    private void releaseUnusedTickets() {
        Set<Chunk> used = new HashSet<>();
        for (Visible v : entities.values()) used.add(v.hitbox().getLocation().getChunk());
        for (Chunk chunk : Set.copyOf(tickets)) if (!used.contains(chunk)) {
            chunk.removePluginChunkTicket(plugin); tickets.remove(chunk);
        }
    }
    public void tick() {
        for (Visible v : entities.values()) {
            if (v.definition().kind() != NpcData.Kind.HUMAN) continue;
            Set<UUID> tracked = new HashSet<>();
            for (Player player : v.hitbox().getTrackedBy()) {
                tracked.add(player.getUniqueId());
                if (v.viewers().contains(player.getUniqueId()) || v.failed().contains(player.getUniqueId())) continue;
                try { show(player, v); v.viewers().add(player.getUniqueId()); }
                catch (Exception e) {
                    hide(player, v);
                    v.failed().add(player.getUniqueId());
                    plugin.getLogger().log(Level.SEVERE, "Paket NPC manusia gagal pada Paper 1.21.4: " + v.definition().id(), e);
                }
            }
            for (UUID old : Set.copyOf(v.viewers())) if (!tracked.contains(old)) {
                Player player = Bukkit.getPlayer(old); if (player != null) hide(player, v);
                v.viewers().remove(old);
            }
            v.failed().retainAll(tracked);
        }
    }
    public void forget(Player player) {
        for (Visible v : entities.values()) {
            if (v.viewers().remove(player.getUniqueId()) && player.isOnline()) hide(player, v);
            v.failed().remove(player.getUniqueId());
        }
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void show(Player player, Visible v) throws ReflectiveOperationException {
        var d = v.definition(); UUID uuid = v.avatarUuid(); int entityId = v.avatarId();
        Class<?> profileType = Class.forName("com.mojang.authlib.GameProfile");
        Object profile = profileType.getConstructor(UUID.class, String.class).newInstance(uuid, "VN_" + d.id().substring(0, Math.min(13, d.id().length())));
        if (!d.texture().isBlank()) {
            Class<?> property = Class.forName("com.mojang.authlib.properties.Property");
            Object texture = property.getConstructor(String.class, String.class, String.class)
                    .newInstance("textures", d.texture(), d.signature().isBlank() ? null : d.signature());
            Object properties = profileType.getMethod("getProperties").invoke(profile);
            Class.forName("com.google.common.collect.Multimap").getMethod("put", Object.class, Object.class).invoke(properties, "textures", texture);
        }
        String prefix = "net.minecraft.network.protocol.game.";
        Class<? extends Enum> action = (Class<? extends Enum>) Class.forName(prefix + "ClientboundPlayerInfoUpdatePacket$Action");
        EnumSet actions = EnumSet.noneOf(action);
        for (String name : List.of("ADD_PLAYER", "UPDATE_LISTED", "UPDATE_GAME_MODE", "UPDATE_HAT")) actions.add(Enum.valueOf(action, name));
        Class<?> game = Class.forName("net.minecraft.world.level.GameType");
        Class<?> component = Class.forName("net.minecraft.network.chat.Component");
        Class<?> chat = Class.forName("net.minecraft.network.chat.RemoteChatSession$Data");
        Class<?> entryType = Class.forName(prefix + "ClientboundPlayerInfoUpdatePacket$Entry");
        Object entry = entryType.getConstructor(UUID.class, profileType, boolean.class, int.class, game, component, boolean.class, int.class, chat)
                .newInstance(uuid, profile, false, 0, game.getField("SURVIVAL").get(null), null, true, 0, null);
        Class<?> infoType = Class.forName(prefix + "ClientboundPlayerInfoUpdatePacket"); Object info;
        try { info = infoType.getConstructor(EnumSet.class, entryType).newInstance(actions, entry); }
        catch (NoSuchMethodException e) {
            info = infoType.getConstructor(EnumSet.class, Collection.class).newInstance(actions, List.of());
            Field entries = infoType.getDeclaredField("entries"); entries.setAccessible(true); entries.set(info, List.of(entry));
        }
        send(player, info);
        Class<?> entityType = Class.forName("net.minecraft.world.entity.EntityType");
        Class<?> vector = Class.forName("net.minecraft.world.phys.Vec3");
        NpcData.Point pose = d.facingLocation();
        Object spawn = Class.forName(prefix + "ClientboundAddEntityPacket")
                .getConstructor(int.class, UUID.class, double.class, double.class, double.class, float.class, float.class, entityType, int.class, vector, double.class)
                .newInstance(entityId, uuid, pose.x(), pose.y(), pose.z(), pose.pitch(), pose.yaw(),
                        entityType.getField("PLAYER").get(null), 0, vector.getField("ZERO").get(null), (double) pose.yaw());
        send(player, spawn);
        Class<?> accessor = Class.forName("net.minecraft.network.syncher.EntityDataAccessor");
        Field skin = Class.forName("net.minecraft.world.entity.player.Player").getDeclaredField("DATA_PLAYER_MODE_CUSTOMISATION"); skin.setAccessible(true);
        Object value = Class.forName("net.minecraft.network.syncher.SynchedEntityData$DataValue")
                .getMethod("create", accessor, Object.class).invoke(null, skin.get(null), (byte) 127);
        send(player, Class.forName(prefix + "ClientboundSetEntityDataPacket").getConstructor(int.class, List.class).newInstance(entityId, List.of(value)));
    }
    private static void send(Player player, Object packet) throws ReflectiveOperationException {
        Object handle = player.getClass().getMethod("getHandle").invoke(player);
        Object connection = handle.getClass().getField("connection").get(handle);
        connection.getClass().getMethod("send", Class.forName("net.minecraft.network.protocol.Packet")).invoke(connection, packet);
    }
    private static void modelRotation(Class<?> type, Object model, String setter, float angle) throws ReflectiveOperationException {
        Method method;
        try { method = type.getMethod(setter + "Immediately", float.class); }
        catch (NoSuchMethodException e) { method = type.getMethod(setter, float.class); }
        method.invoke(model, angle);
    }
    private void hide(Player p, Visible v) {
        try {
            send(p, Class.forName("net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket")
                    .getConstructor(int[].class).newInstance((Object) new int[]{v.avatarId()}));
            send(p, Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket")
                    .getConstructor(List.class).newInstance(List.of(v.avatarUuid())));
        }
        catch (ReflectiveOperationException e) { plugin.getLogger().fine("Profil NPC sudah tidak tersedia bagi " + p.getName()); }
    }
    @SuppressWarnings("unchecked") private void hookModel() {
        if (!Bukkit.getPluginManager().isPluginEnabled("ModelEngine")) return;
        try {
            Class<?> eventType = Class.forName("com.ticxo.modelengine.api.events.BaseEntityInteractEvent");
            Class<?> baseType = Class.forName("com.ticxo.modelengine.api.entity.BaseEntity");
            Bukkit.getPluginManager().registerEvent((Class<? extends Event>) eventType, modelListener, EventPriority.LOWEST, (listener, event) -> {
                try {
                    Player p = (Player) eventType.getMethod("getPlayer").invoke(event);
                    boolean attack = eventType.getMethod("getAction").invoke(event).toString().equals("ATTACK");
                    if (!attack && eventType.getMethod("getSlot").invoke(event) == EquipmentSlot.OFF_HAND) return;
                    Object base = eventType.getMethod("getBaseEntity").invoke(event);
                    UUID uuid = (UUID) baseType.getMethod("getUUID").invoke(base);
                    Runnable dispatch = () -> { for (Visible v : entities.values()) if (v.base() != null && v.base().getUniqueId().equals(uuid)) {
                        if (event instanceof Cancellable cancellable) cancellable.setCancelled(true);
                        input.click(p, v.definition().id(), attack); break;
                    } };
                    if (event.isAsynchronous()) Bukkit.getScheduler().runTask(plugin, dispatch); else dispatch.run();
                } catch (ReflectiveOperationException e) { throw new org.bukkit.event.EventException(e); }
            }, plugin, false);
        } catch (ReflectiveOperationException | LinkageError e) {
            plugin.getLogger().log(Level.WARNING, "Pengait klik ModelEngine tidak tersedia; NPC manusia dan hitbox Vitae tetap dapat dipakai.", e);
        }
    }
    private void destroyModel(Object model) {
        try {
            Class<?> type = Class.forName("com.ticxo.modelengine.api.model.ModeledEntity");
            Object base = type.getMethod("getBase").invoke(model);
            UUID uuid = (UUID) Class.forName("com.ticxo.modelengine.api.entity.BaseEntity").getMethod("getUUID").invoke(base);
            Class.forName("com.ticxo.modelengine.api.ModelEngineAPI").getMethod("removeModeledEntity", UUID.class).invoke(null, uuid);
            if (!(boolean) type.getMethod("isDestroyed").invoke(model)) type.getMethod("destroy").invoke(model);
        }
        catch (ReflectiveOperationException e) { plugin.getLogger().log(Level.WARNING, "Pembersihan model NPC gagal.", e); }
    }
    private void remove(String id) {
        Visible v = entities.remove(id); if (v == null) return;
        avatars.remove(v.avatarId());
        for (UUID viewer : v.viewers()) { Player p = Bukkit.getPlayer(viewer); if (p != null) hide(p, v); }
        if (v.modeled() != null) destroyModel(v.modeled()); if (v.base() != null) v.base().remove(); v.hitbox().remove();
    }
    @Override public void close() {
        for (String id : List.copyOf(entities.keySet())) remove(id);
        for (Chunk chunk : tickets) chunk.removePluginChunkTicket(plugin); tickets.clear(); HandlerList.unregisterAll(modelListener);
    }
}
