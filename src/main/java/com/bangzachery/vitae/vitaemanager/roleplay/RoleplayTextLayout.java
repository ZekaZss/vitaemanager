package com.bangzachery.vitae.vitaemanager.roleplay;

import java.util.Arrays;

/** Coordinates are relative to the owner's feet, never to another entity's head. */
final class RoleplayTextLayout {
    private RoleplayTextLayout() { }

    static float translation(double poseHeight, double clearance, double passengerAnchorHeight,
                             double vehicleAttachmentHeight) {
        if (!Double.isFinite(poseHeight) || poseHeight <= 0 || !Double.isFinite(clearance)
                || clearance < 0 || clearance > 1 || !Double.isFinite(passengerAnchorHeight)
                || !Double.isFinite(vehicleAttachmentHeight)) {
            throw new IllegalArgumentException("Invalid roleplay text attachment geometry");
        }
        // Vanilla positions the passenger at seat minus the passenger's vehicle attachment.
        // TextDisplay's renderer origin is the bottom of the text, including wrapped lines.
        return (float) (poseHeight + clearance - passengerAnchorHeight + vehicleAttachmentHeight);
    }

    static int[] passengers(int[] actual, Integer textId) {
        // Keep the authoritative carry passenger ordering/IDs, and append only our visual ID.
        if (textId == null || Arrays.stream(actual).anyMatch(id -> id == textId)) return actual.clone();
        int[] result = Arrays.copyOf(actual, actual.length + 1); result[actual.length] = textId; return result;
    }
}
