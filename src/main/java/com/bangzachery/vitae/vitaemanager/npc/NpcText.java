package com.bangzachery.vitae.vitaemanager.npc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/** Only formatting codes are parsed; names cannot create clickable commands. */
public final class NpcText {
    private NpcText() { }
    public static Component name(String text) { return LegacyComponentSerializer.legacyAmpersand().deserialize(text); }
    public static String plain(String text) { return PlainTextComponentSerializer.plainText().serialize(name(text)); }
    public static Component dialogName(String text) {
        Component name = name(text);
        return name.colorIfAbsent(NamedTextColor.GOLD);
    }
}
