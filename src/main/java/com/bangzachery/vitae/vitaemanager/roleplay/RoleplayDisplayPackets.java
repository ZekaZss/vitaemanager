package com.bangzachery.vitae.vitaemanager.roleplay;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import java.lang.reflect.*;
import java.util.List;

/** Paper 1.21.4 packet boundary. The display is never spawned or tracked by the world.
 * No movement packet exists in this transport: the client passenger attachment owns position.
 */
final class RoleplayDisplayPackets {
    private final Constructor<?> friendlyBuffer, addEntity, metadata, removeEntities;
    private final Method createBuffer, releaseBuffer, writeInt, writeInts, decode, entityData, nonDefaults,
            passengerPosition, vehicleAttachment, send;
    private final Field connection, vectorY;
    private final Object passengerCodec, displayType, zeroVector;

    RoleplayDisplayPackets() throws ReflectiveOperationException {
        Class<?> friendly = Class.forName("net.minecraft.network.FriendlyByteBuf");
        Class<?> byteBuf = Class.forName("io.netty.buffer.ByteBuf");
        Class<?> nativeEntity = Class.forName("net.minecraft.world.entity.Entity");
        Class<?> entityType = Class.forName("net.minecraft.world.entity.EntityType");
        Class<?> vector = Class.forName("net.minecraft.world.phys.Vec3");
        String prefix = "net.minecraft.network.protocol.game.";
        createBuffer = Class.forName("io.netty.buffer.Unpooled").getMethod("buffer");
        releaseBuffer = byteBuf.getMethod("release"); friendlyBuffer = friendly.getConstructor(byteBuf);
        writeInt = friendly.getMethod("writeVarInt", int.class); writeInts = friendly.getMethod("writeVarIntArray", int[].class);
        decode = Class.forName("net.minecraft.network.codec.StreamDecoder").getMethod("decode", Object.class);
        passengerCodec = Class.forName(prefix + "ClientboundSetPassengersPacket").getField("STREAM_CODEC").get(null);
        addEntity = Class.forName(prefix + "ClientboundAddEntityPacket").getConstructor(int.class, java.util.UUID.class,
                double.class, double.class, double.class, float.class, float.class, entityType, int.class, vector, double.class);
        metadata = Class.forName(prefix + "ClientboundSetEntityDataPacket").getConstructor(int.class, List.class);
        removeEntities = Class.forName(prefix + "ClientboundRemoveEntitiesPacket").getConstructor(int[].class);
        entityData = nativeEntity.getMethod("getEntityData");
        nonDefaults = Class.forName("net.minecraft.network.syncher.SynchedEntityData").getMethod("getNonDefaultValues");
        passengerPosition = nativeEntity.getMethod("getPassengerRidingPosition", nativeEntity);
        vehicleAttachment = nativeEntity.getMethod("getVehicleAttachmentPoint", nativeEntity);
        vectorY = vector.getField("y"); zeroVector = vector.getField("ZERO").get(null); displayType = entityType.getField("TEXT_DISPLAY").get(null);
        connection = Class.forName("net.minecraft.server.level.ServerPlayer").getField("connection");
        send = Class.forName("net.minecraft.server.network.ServerGamePacketListenerImpl")
                .getMethod("send", Class.forName("net.minecraft.network.protocol.Packet"));
    }

    float translation(Player owner, TextDisplay display, double clearance) throws ReflectiveOperationException {
        Object rider = handle(display), vehicle = handle(owner);
        double seat = vectorY.getDouble(passengerPosition.invoke(vehicle, rider)) - owner.getLocation().getY();
        double attachment = vectorY.getDouble(vehicleAttachment.invoke(rider, vehicle));
        return RoleplayTextLayout.translation(owner.getHeight(), clearance, seat, attachment);
    }

    void show(Player viewer, Player owner, TextDisplay display) throws ReflectiveOperationException {
        Location at = owner.getLocation();
        send(viewer, addEntity.newInstance(display.getEntityId(), display.getUniqueId(), at.getX(), at.getY(), at.getZ(),
                0f, 0f, displayType, 0, zeroVector, 0d));
        update(viewer, display);
        attach(viewer, owner, display);
    }

    void update(Player viewer, TextDisplay display) throws ReflectiveOperationException {
        Object values = nonDefaults.invoke(entityData.invoke(handle(display)));
        send(viewer, metadata.newInstance(display.getEntityId(), values == null ? List.of() : values));
    }

    void hide(Player viewer, Player owner, TextDisplay display, boolean ownerTracked) throws ReflectiveOperationException {
        try { if (ownerTracked) attach(viewer, owner, null); }
        finally { forget(viewer, display); }
    }

    void forget(Player viewer, TextDisplay display) throws ReflectiveOperationException {
        send(viewer, removeEntities.newInstance((Object) new int[]{display.getEntityId()}));
    }

    void attach(Player viewer, Player owner, TextDisplay display) throws ReflectiveOperationException {
        Object buffer = createBuffer.invoke(null);
        try {
            int[] actual = owner.getPassengers().stream().mapToInt(Entity::getEntityId).toArray();
            int[] passengers = RoleplayTextLayout.passengers(actual, display == null ? null : display.getEntityId());
            Object data = friendlyBuffer.newInstance(buffer);
            writeInt.invoke(data, owner.getEntityId()); writeInts.invoke(data, (Object) passengers);
            send(viewer, decode.invoke(passengerCodec, data));
        } finally { releaseBuffer.invoke(buffer); }
    }

    private static Object handle(Entity entity) throws ReflectiveOperationException { return entity.getClass().getMethod("getHandle").invoke(entity); }
    private void send(Player viewer, Object packet) throws ReflectiveOperationException { send.invoke(connection.get(handle(viewer)), packet); }
}
