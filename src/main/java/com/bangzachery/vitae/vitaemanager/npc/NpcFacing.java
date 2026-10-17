package com.bangzachery.vitae.vitaemanager.npc;

/** Rotation only: never moves the NPC or changes its saved idle direction. */
public final class NpcFacing {
    private NpcFacing() { }
    public static NpcData.Point towards(NpcData.Definition npc, NpcData.Point eyes, NpcData.Point current) {
        var at = npc.location();
        if (!at.world().equals(eyes.world())) return npc.facingLocation();
        double dx = eyes.x() - at.x(), dy = eyes.y() - at.y() - npc.eyeHeight(), dz = eyes.z() - at.z();
        if (dx * dx + dy * dy + dz * dz < .000001) return current;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        if (npc.kind() == NpcData.Kind.MODEL) { yaw += npc.facing().yaw(); pitch += npc.facing().pitch(); }
        pitch = Math.max(-90, Math.min(90, pitch));
        return new NpcData.Point(at.world(), at.x(), at.y(), at.z(), current.yaw() + step(shortest(yaw - current.yaw())),
                current.pitch() + step(pitch - current.pitch()));
    }
    private static float step(float delta) { return Math.max(-15, Math.min(15, delta)); }
    private static float shortest(float angle) { return (angle % 360 + 540) % 360 - 180; }
}
