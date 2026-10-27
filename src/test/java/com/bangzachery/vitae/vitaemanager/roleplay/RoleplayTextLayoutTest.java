package com.bangzachery.vitae.vitaemanager.roleplay;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class RoleplayTextLayoutTest {
    private static double bottom(double feet, double seat, double riderAttachment, float translation) {
        return feet + seat - riderAttachment + translation;
    }

    @Test void standingTextBottomIsOneHeadHeightPlusSmallClearance() {
        float offset = RoleplayTextLayout.translation(1.8, .12, 1.35, 0);
        assertEquals(65.92, bottom(64, 1.35, 0, offset), .000001);
        assertTrue(offset < 1); // No extra player/armorstand height or old fixed +0.95.
    }

    @Test void sneakAndCrawlUseTheirOwnPoseHeightWithoutHeadOverlap() {
        for (double height : new double[]{1.8, 1.5, .6, .2, 3.6}) {
            double seat = height * .75;
            float offset = RoleplayTextLayout.translation(height, .12, seat, 0);
            assertEquals(height + .12, bottom(0, seat, 0, offset), .000001);
            assertTrue(bottom(0, seat, 0, offset) > height);
        }
    }

    @Test void movementJumpAndWorldHeightDoNotAddLagOrChangeRelativeOffset() {
        float offset = RoleplayTextLayout.translation(1.8, .12, 1.35, 0);
        for (double feet : new double[]{-63.25, 0, 64, 64.42, 65.25, 319})
            assertEquals(feet + 1.92, bottom(feet, 1.35, 0, offset), .000001);
    }

    @Test void nativeVehicleAttachmentIsSubtractedExactlyOnce() {
        float offset = RoleplayTextLayout.translation(1.8, .12, 1.35, .25);
        assertEquals(1.92, bottom(0, 1.35, .25, offset), .000001);
    }

    @Test void acceptsZeroClearanceButRejectsNonFiniteOrNegativeGeometry() {
        assertEquals(1.8, bottom(0, 1.35, 0, RoleplayTextLayout.translation(1.8, 0, 1.35, 0)), .000001);
        for (double invalid : new double[]{Double.NaN, Double.POSITIVE_INFINITY, -.01, 1.01})
            assertThrows(IllegalArgumentException.class, () -> RoleplayTextLayout.translation(1.8, invalid, 1.35, 0));
        assertThrows(IllegalArgumentException.class, () -> RoleplayTextLayout.translation(0, .12, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> RoleplayTextLayout.translation(1.8, .12, Double.NaN, 0));
    }

    @Test void attachmentAndCleanupPreserveEveryAuthoritativeCarryPassengerInOrder() {
        Random random = new Random(1214);
        for (int count = 0; count <= 20; count++) {
            int[] actual = random.ints(count, 1, 100_000).toArray(); int[] original = actual.clone();
            int[] attached = RoleplayTextLayout.passengers(actual, 999_999);
            assertEquals(count + 1, attached.length);
            assertArrayEquals(original, java.util.Arrays.copyOf(attached, count)); assertEquals(999_999, attached[count]);
            int[] cleaned = RoleplayTextLayout.passengers(actual, null);
            assertArrayEquals(original, cleaned); assertNotSame(actual, cleaned); assertArrayEquals(original, actual);
        }
    }

    @Test void duplicateVisualIdIsNeverAppendedAndInputIsNeverMutated() {
        int[] actual = {40, 80, 120}; int[] result = RoleplayTextLayout.passengers(actual, 80);
        assertArrayEquals(actual, result); assertNotSame(actual, result);
        result[0] = -1; assertEquals(40, actual[0]);
    }

    @Test void cleanupUsesCurrentPassengerListRatherThanStaleListFromTextCreation() {
        assertArrayEquals(new int[]{10, 99}, RoleplayTextLayout.passengers(new int[]{10}, 99));
        assertArrayEquals(new int[]{20, 30}, RoleplayTextLayout.passengers(new int[]{20, 30}, null));
        assertArrayEquals(new int[]{20, 30, 99}, RoleplayTextLayout.passengers(new int[]{20, 30}, 99));
    }
}
