package com.bangzachery.vitae.vitaemanager.roleplay;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/** User input is always literal text; only bundled/configured messages use MiniMessage. */
public final class RoleplayText {
    private RoleplayText() { }

    public static String validate(String text, int maximum) {
        if (text == null || text.isBlank() || text.codePointCount(0, text.length()) > maximum
                || text.codePoints().anyMatch(c -> Character.isISOControl(c) || c == 0x2028 || c == 0x2029)) {
            throw new IllegalArgumentException("Invalid roleplay text");
        }
        return text;
    }

    public static Component floating(String text, boolean condition) {
        return Component.text("\"" + text + "\"", condition ? NamedTextColor.AQUA : NamedTextColor.GREEN);
    }

    public static Component ooc(String name, String text, boolean global) {
        return Component.text((global ? "[OOC Global] " : "[OOC] ") + name + ": " + text,
                global ? TextColor.color(0x3b82f6) : NamedTextColor.GRAY);
    }

    public static boolean inRange(double distanceSquared, double radius) {
        return Double.isFinite(distanceSquared) && distanceSquared >= 0 && radius > 0
                && Double.isFinite(radius) && distanceSquared <= radius * radius;
    }
}