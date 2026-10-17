package com.bangzachery.vitae.vitaemanager.whisper;

import java.util.*;

/** Optional story templates; applying one preserves area, receivers, and completion counters. */
public final class WhisperTemplates {
    private WhisperTemplates() {}
    public static List<String> names() { return List.of("lunar", "ryujin", "outsider", "elysia", "penyamar"); }
    public static WhisperDefinition apply(WhisperDefinition original, String template) {
        List<WhisperLine> lines = new ArrayList<>(); String effect = "minecraft:block.wood.break";
        switch (template.toLowerCase(Locale.ROOT)) {
            case "lunar" -> {
                title(lines, "&9Lunar", 3, 1);
                text(lines, "&fSudah lama kita tidak berbicara", 4, 1);
                text(lines, "&fKau pikir, Dungeon kali ini akan sama dengan yang dulu?", 5, 1);
                title(lines, "&4HAHAHAHAHA", 3, 1);
                title(lines, "&4&lJANGAN HARAP", 3, .5);
                text(lines, "&cKAU BISA SELAMAT DI DUNGEON KALI INI", 4, .5);
                title(lines, "&4HAHAHAHAHA", 3, 0);
            }
            case "ryujin" -> {
                title(lines, "&bRYUJIN", 3, 1);
                text(lines, "&fBerhati-hatilah disana", 3, 1);
                text(lines, "&fKau sedang tidak memegangku saat ini", 4, 1);
                text(lines, "&7Tempat itu terlalu berbahaya sepertinya", 4, 1);
                text(lines, "&7Tempat itu tidak seperti dungeon sebelumnya", 4, 1);
                text(lines, "&fAku merasakannya saat kau menginjakkan kaki di tempat kau berdiri sekarang", 4, 1);
                text(lines, "&eKali ini, kau harus berhati-hati dan beri tahu kepada rekan Revenant-mu", 4, 1);
                title(lines, "&cDAN", 2, 1);
                text(lines, "&cJangan mencoba untuk mendapatkan hadiah dari dungeon yang ada di dalam sana", 4, 1);
                text(lines, "&fKali ini, cukup bertahan sampai selesai", 4, 0);
            }
            case "outsider" -> {
                effect = "minecraft:entity.ghast.scream";
                title(lines, "&4&lOUTSIDER!!!!!", 3, 1);
                text(lines, "&cKALIAN BUKANLAH MAKHLUK DARI DUNIA INI", 5, 1);
                title(lines, "&4PERGI KALIAN", 3, .5);
                title(lines, "&4DARI SINI!", 3, 0);
            }
            case "elysia" -> {
                effect = "minecraft:entity.warden.ambient";
                title(lines, "&dHai Elysia", 3, 1);
                text(lines, "&fAku senang dengan keadaanmu sekarang", 4, 1);
                title(lines, "&5HAHAHAHA", 3, 1);
                text(lines, "&7Aku bisa tenang karena....", 4, 1.5);
                text(lines, "&cKaulah yang tersisa di keluargamu sendiri", 5, 1);
                title(lines, "&5HAHAHAHAHA", 3, 1);
                text(lines, "&fSelamat menikmati hari dimana kau akan melihat seseorang yang kau kenal", 4, 1);
                text(lines, "&7Suatu saat orang itu akan muncul di hadapanmu", 5, 0);
            }
            case "penyamar" -> {
                effect = "minecraft:entity.warden.ambient";
                text(lines, "&7Mau sampai kapan kalian berdua bersembunyi menggunakan wujud itu?", 5, 1);
            }
            default -> throw new IllegalArgumentException("Template: " + String.join(", ", names()));
        }
        WhisperLine.Audio audio = new WhisperLine.Audio(effect, template.equalsIgnoreCase("outsider") ? .6f : .65f, .65f);
        lines.replaceAll(line -> line.withAudio(audio));
        WhisperMusic music = new WhisperMusic(new WhisperLine.Audio("minecraft:music_disc.11", .5f, .85f), 0);
        return original.withEnabled(false).withLines(lines).withMusic(music).withVisual(new WhisperVisual(true, 2, 5, 5)).withLimit(1);
    }
    private static void title(List<WhisperLine> lines, String text, double seconds, double gap) {
        lines.add(new WhisperLine(text, "", (int) (seconds * 20), (int) (gap * 20), null));
    }
    private static void text(List<WhisperLine> lines, String text, double seconds, double gap) {
        List<String> pages = WhisperText.pages(text);
        for (int i = 0; i < pages.size(); i++) lines.add(new WhisperLine("", pages.get(i), (int) (seconds * 20),
                (int) ((i + 1 == pages.size() ? gap : .5) * 20), null));
    }
}
