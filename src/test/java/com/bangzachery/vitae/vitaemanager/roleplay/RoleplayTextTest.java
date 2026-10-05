package com.bangzachery.vitae.vitaemanager.roleplay;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RoleplayTextTest {
    @Test void userTagsRemainLiteralWithoutClickHoverOrFormattingEvents() {
        String input = "<click:run_command:'/op me'><red>halo</red></click>";
        assertEquals(Component.text("\"" + input + "\"", NamedTextColor.GREEN),
                RoleplayText.floating(RoleplayText.validate(input, 256), false));
        assertEquals(Component.text("[OOC] One: " + input, NamedTextColor.GRAY),
                RoleplayText.ooc("One", input, false));
        assertNull(RoleplayText.floating(input, true).clickEvent());
        assertNull(RoleplayText.ooc("One", input, true).hoverEvent());
    }

    @Test void rejectsBlankMultilineControlAndExcessText() {
        for (String input : new String[]{"", " ", "\n", "a\nb", "a\rb", "a\tb", "a\u0000b", "a\u2028b", "a\u2029b"}) {
            assertThrows(IllegalArgumentException.class, () -> RoleplayText.validate(input, 256));
        }
        assertThrows(IllegalArgumentException.class, () -> RoleplayText.validate("x".repeat(257), 256));
        assertThrows(IllegalArgumentException.class, () -> RoleplayText.validate(null, 256));
    }

    @Test void measuresUnicodeCodePointsWithoutCuttingSurrogatePairs() {
        String text = "\uD83D\uDD25".repeat(256);
        assertEquals(text, RoleplayText.validate(text, 256));
        assertThrows(IllegalArgumentException.class, () -> RoleplayText.validate(text + "x", 256));
    }

    @Test void localRadiusIncludesBoundaryAndRejectsInvalidDistances() {
        assertTrue(RoleplayText.inRange(900, 30));
        assertTrue(RoleplayText.inRange(0, 30));
        assertFalse(RoleplayText.inRange(900.000001, 30));
        assertFalse(RoleplayText.inRange(-1, 30));
        assertFalse(RoleplayText.inRange(Double.NaN, 30));
        assertFalse(RoleplayText.inRange(Double.POSITIVE_INFINITY, 30));
        assertFalse(RoleplayText.inRange(1, Double.NaN));
    }
}