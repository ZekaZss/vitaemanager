package com.bangzachery.vitae.vitaemanager.whisper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import java.util.*;

/** Colour codes only: no MiniMessage events, commands, or player placeholders. */
public final class WhisperText {
    public static final int PAGE_WIDTH = 56;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder().character('&').hexColors().build();
    private WhisperText() {}
    public static Component component(String text, NamedTextColor fallback) {
        return LEGACY.deserialize(normalize(text)).colorIfAbsent(fallback);
    }
    public static String plain(String text) { return PlainTextComponentSerializer.plainText().serialize(LEGACY.deserialize(normalize(text))); }
    public static int length(String text) { String plain = plain(text); return plain.codePointCount(0, plain.length()); }
    private static String normalize(String original) {
        String text = Objects.requireNonNull(original).replace('\u00a7', '&');
        StringBuilder normalized = new StringBuilder(text.length());
        for (int at = 0; at < text.length();) {
            int length = code(text, at);
            if (length == 2) {
                normalized.append('&').append(Character.toLowerCase(text.charAt(at + 1))); at += length;
            } else {
                int next = at + (length > 0 ? length : Character.charCount(text.codePointAt(at)));
                normalized.append(text, at, next); at = next;
            }
        }
        return normalized.toString();
    }
    private static int code(String text, int at) {
        if (at + 1 >= text.length() || text.charAt(at) != '&') return 0;
        char c = Character.toLowerCase(text.charAt(at + 1));
        if ("0123456789abcdefklmnor".indexOf(c) >= 0) return 2;
        if (c == '#' && at + 8 <= text.length() && text.substring(at + 2, at + 8).matches("[0-9a-fA-F]{6}")) return 8;
        return 0;
    }
    public static String prefix(String original, int visible) {
        String text = normalize(original); int at = 0, count = 0;
        if (visible <= 0) return "";
        while (at < text.length() && count < visible) {
            int code = code(text, at);
            if (code > 0) at += code;
            else { at += Character.charCount(text.codePointAt(at)); count++; }
        }
        return text.substring(0, at);
    }
    private static String carry(String text) {
        String colour = ""; Set<Character> styles = new LinkedHashSet<>();
        for (int at = 0; at < text.length();) {
            int length = code(text, at);
            if (length == 0) { at += Character.charCount(text.codePointAt(at)); continue; }
            char c = Character.toLowerCase(text.charAt(at + 1));
            if (length == 8 || "0123456789abcdefr".indexOf(c) >= 0) { colour = text.substring(at, at + length); styles.clear(); }
            else styles.add(c);
            at += length;
        }
        StringBuilder result = new StringBuilder(colour); for (char style : styles) result.append('&').append(style); return result.toString();
    }
    /** Split at words, preserve formatting and Unicode boundaries, and fit the stored line size. */
    public static List<String> pages(String original) {
        String remaining = normalize(original).strip();
        if (remaining.codePointCount(0, remaining.length()) > 4000 || remaining.codePoints().anyMatch(Character::isISOControl)
                || remaining.indexOf('\u2028') >= 0 || remaining.indexOf('\u2029') >= 0 || plain(remaining).isBlank())
            throw new IllegalArgumentException("Dialog perlu teks 1–4000 karakter tanpa baris baru/kontrol.");
        List<String> pages = new ArrayList<>(); String format = "";
        while (!remaining.isEmpty()) {
            int at = 0, count = 0, space = -1;
            while (at < remaining.length() && count < PAGE_WIDTH) {
                int length = code(remaining, at);
                int next = at + (length > 0 ? length : Character.charCount(remaining.codePointAt(at)));
                if (next + format.length() > 160) break;
                if (length == 0) { if (Character.isWhitespace(remaining.codePointAt(at))) space = at; count++; }
                at = next;
            }
            if (at == 0) throw new IllegalArgumentException("Kode warna terlalu panjang sebelum teks.");
            int cut = at < remaining.length() && space > 0 ? space : at;
            String page = (format + remaining.substring(0, cut)).strip();
            if (plain(page).isBlank()) throw new IllegalArgumentException("Kode warna terlalu banyak sebelum teks.");
            pages.add(page); format = carry(page); remaining = remaining.substring(cut).stripLeading();
        }
        return List.copyOf(pages);
    }
}
