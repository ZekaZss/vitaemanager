package com.bangzachery.vitae.vitaemanager.command;

import com.bangzachery.vitae.vitaemanager.core.message.MessageService;
import com.bangzachery.vitae.vitaemanager.whisper.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.UnaryOperator;

/** Subcommand adapter for /vitae bisikan; all parse errors give a specific correction and usage. */
public final class WhisperCommand {
    private final WhisperService service;
    private final MessageService messages;
    private static final List<String> ACTIONS = List.of("help", "wand", "pos1", "pos2", "create", "template", "area", "target", "line",
            "dialog", "sound", "music", "soundtest", "typing", "fade", "pace", "plays", "enable", "disable", "list", "info", "preview", "stop", "reset", "delete", "reload");
    private static final List<String> SOUNDS = List.of("off", "minecraft:block.wood.break", "minecraft:entity.warden.ambient",
            "minecraft:entity.ghast.scream", "minecraft:music_disc.11", "minecraft:music_disc.13");
    public WhisperCommand(WhisperService service, MessageService messages) { this.service = service; this.messages = messages; }

    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vitae.admin.whisper")) { messages.send(sender, "no-permission"); return true; }
        String action = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        try {
            switch (action) {
                case "help" -> {
                    if (args.length > 2) throw new IllegalArgumentException("Gunakan help [topik].");
                    help(sender, args.length == 2 ? args[1].toLowerCase(Locale.ROOT) : null);
                }
                case "wand" -> { length(args, 2); service.wand(player(sender), args[1]); }
                case "pos1", "pos2" -> {
                    length(args, 1); Player player = player(sender); service.select(player, action.equals("pos1"));
                    var location = player.getLocation();
                    say(sender, "Titik " + (action.equals("pos1") ? "A" : "B") + " = " + location.getWorld().getName()
                            + " " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ". Titik memakai blok tempat berdiri.");
                }
                case "create" -> {
                    length(args, 2); save(sender, args[1], state -> state.create(args[1]),
                            "Berikutnya: area " + args[1] + ", target add " + args[1] + " <nick MC>, lalu dialog atau template. Lihat help template.");
                }
                case "template" -> {
                    length(args, 3); String template = args[2].toLowerCase(Locale.ROOT);
                    if (!WhisperTemplates.names().contains(template)) throw new IllegalArgumentException("Template: " + String.join(", ", WhisperTemplates.names()) + ".");
                    var preset = WhisperTemplates.apply(WhisperDefinition.draft(args[1]), template);
                    service.validateAudio(preset.music().sound());
                    preset.lines().forEach(line -> service.validateAudio(line.audio()));
                    save(sender, args[1], state -> {
                        var original = state.definitions().getOrDefault(args[1], WhisperDefinition.draft(args[1]));
                        return state.put(WhisperTemplates.apply(original, template));
                    }, "Template mengganti dialog/musik/tampilan, OFF dan plays 1. Area, penerima, dan progres tetap. Atur area + target, preview, lalu enable " + args[1] + ".");
                }
                case "area" -> {
                    if (args.length == 4 && args[1].equalsIgnoreCase("copy")) {
                        save(sender, args[2], state -> {
                            var area = state.require(args[3]).area();
                            if (area == null) throw new IllegalArgumentException("Area sumber belum diatur.");
                            return state.put(state.require(args[2]).withArea(area));
                        });
                    } else {
                        length(args, 2); var area = service.selection(player(sender));
                        save(sender, args[1], state -> state.put(state.require(args[1]).withArea(area)));
                    }
                }
                case "target" -> {
                    if (args.length < 4) throw new IllegalArgumentException("Target memerlukan add/remove, ID, dan satu atau lebih nick MC/UUID.");
                    String operation = args[1].toLowerCase(Locale.ROOT);
                    if (!List.of("add", "remove").contains(operation)) throw new IllegalArgumentException("Target gunakan add atau remove.");
                    Set<UUID> selected = new HashSet<>();
                    for (int i = 3; i < args.length; i++) selected.add(target(args[i]));
                    Set<UUID> resolved = Set.copyOf(selected); // Resolve Bukkit names before submitting writer work.
                    save(sender, args[2], state -> {
                        var definition = state.require(args[2]); var targets = new HashSet<>(definition.targets());
                        boolean changed = operation.equals("add") ? targets.addAll(resolved) : targets.removeAll(resolved);
                        if (!changed) throw new IllegalArgumentException("Semua penerima sudah ada atau tidak terdaftar untuk dihapus.");
                        return state.put(definition.withTargets(targets));
                    });
                }
                case "line" -> editLine(sender, args);
                case "dialog" -> {
                    if (args.length < 5) throw new IllegalArgumentException("Dialog memerlukan ID, durasi, jeda, dan teks.");
                    int duration = ticks(args[2], false), gap = ticks(args[3], true);
                    var pages = WhisperText.pages(String.join(" ", Arrays.copyOfRange(args, 4, args.length)));
                    save(sender, args[1], state -> {
                        var definition = state.require(args[1]); var lines = new ArrayList<>(definition.lines());
                        for (String page : pages) lines.add(new WhisperLine("", page, duration, gap, null));
                        return state.put(definition.withLines(lines));
                    }, "Ditambahkan " + pages.size() + " halaman subtitle. Gunakan sound <id> all ... setelah semua dialog ditambahkan.");
                }
                case "sound" -> {
                    if (args.length < 4 || args.length > 6) throw new IllegalArgumentException("Sound memerlukan ID, nomor/all, key/off, dan volume/pitch opsional.");
                    int index = args[2].equalsIgnoreCase("all") ? -1 : index(args[2]);
                    WhisperLine.Audio selected = optionalAudio(args, 3, 0.35f);
                    save(sender, args[1], state -> {
                        var definition = state.require(args[1]); var lines = new ArrayList<>(definition.lines());
                        if (lines.isEmpty() || index >= lines.size()) throw new IllegalArgumentException("Nomor dialog tidak ada; lihat info.");
                        if (index < 0) lines.replaceAll(line -> line.withAudio(selected));
                        else lines.set(index, lines.get(index).withAudio(selected));
                        return state.put(definition.withLines(lines));
                    });
                }
                case "music" -> {
                    if (args.length < 3 || args.length > 6) throw new IllegalArgumentException("Music memerlukan ID, key/off, volume, pitch, dan interval ulang opsional.");
                    WhisperMusic music = null;
                    if (args[2].equalsIgnoreCase("off")) {
                        if (args.length != 3) throw new IllegalArgumentException("Music off tidak menerima pengaturan tambahan.");
                    } else {
                        var sound = audio(args[2], args.length > 3 ? args[3] : "0.5", args.length > 4 ? args[4] : "1");
                        int repeat = args.length > 5 && !args[5].equalsIgnoreCase("off")
                                ? rangedTicks(args[5], 20, 12000, "Interval ulang 1–600 detik; pakai off untuk satu kali putar.") : 0;
                        music = new WhisperMusic(sound, repeat);
                    }
                    WhisperMusic selected = music;
                    save(sender, args[1], state -> state.put(state.require(args[1]).withMusic(selected)));
                }
                case "soundtest" -> {
                    if (args.length < 2 || args.length > 4) throw new IllegalArgumentException("Soundtest memerlukan key, volume/pitch opsional.");
                    service.soundTest(player(sender), audio(args[1], args.length > 2 ? args[2] : "0.5", args.length > 3 ? args[3] : "1"));
                    say(sender, "Suara dimainkan hanya untukmu; stop untuk menghentikannya.");
                }
                case "typing" -> {
                    if (args.length < 3 || args.length > 4 || !List.of("on", "off").contains(args[2].toLowerCase(Locale.ROOT)))
                        throw new IllegalArgumentException("Typing gunakan on/off dan kecepatan 1–8 karakter/tick opsional.");
                    boolean typing = args[2].equalsIgnoreCase("on"); Integer speed = args.length == 4 ? integer(args[3], 1, 8) : null;
                    save(sender, args[1], state -> {
                        var definition = state.require(args[1]); var visual = definition.visual();
                        return state.put(definition.withVisual(visual.withTyping(typing, speed == null ? visual.charactersPerTick() : speed)));
                    });
                }
                case "fade" -> {
                    length(args, 4);
                    int in = rangedTicks(args[2], 0, 40, "Fade masuk/keluar 0–2 detik."), out = rangedTicks(args[3], 0, 40, "Fade masuk/keluar 0–2 detik.");
                    save(sender, args[1], state -> {
                        var definition = state.require(args[1]);
                        return state.put(definition.withVisual(definition.visual().withFade(in, out)));
                    });
                }
                case "pace" -> {
                    length(args, 4); int duration = ticks(args[2], false), gap = ticks(args[3], true);
                    save(sender, args[1], state -> {
                        var definition = state.require(args[1]);
                        if (definition.lines().isEmpty()) throw new IllegalArgumentException("Tambahkan dialog dulu.");
                        return state.put(definition.withLines(definition.lines().stream()
                                .map(line -> new WhisperLine(line.title(), line.subtitle(), duration, gap, line.audio())).toList()));
                    });
                }
                case "plays" -> {
                    length(args, 3); int limit = integer(args[2], 1, 100);
                    save(sender, args[1], state -> state.put(state.require(args[1]).withLimit(limit)));
                }
                case "enable", "disable" -> {
                    length(args, 2);
                    save(sender, args[1], state -> state.put(state.require(args[1]).withEnabled(action.equals("enable"))));
                }
                case "delete" -> {
                    length(args, 3);
                    if (!args[2].equalsIgnoreCase("confirm")) throw new IllegalArgumentException("Tambahkan confirm untuk menghapus area beserta progresnya.");
                    save(sender, args[1], state -> state.delete(args[1]));
                }
                case "reset" -> {
                    length(args, 3); UUID target = args[2].equalsIgnoreCase("all") ? null : target(args[2]);
                    save(sender, args[1], state -> state.reset(args[1], target));
                }
                case "preview" -> {
                    if (args.length < 2 || args.length > 3) throw new IllegalArgumentException("Preview memerlukan ID dan nomor awal opsional.");
                    service.preview(player(sender), args[1], args.length == 3 ? index(args[2]) + 1 : 1);
                    say(sender, "Preview masuk antreanmu; tidak mengubah progres. Stop untuk berhenti.");
                }
                case "stop" -> { length(args, 1); service.stop(player(sender)); say(sender, "Bisikan, musik, tes suara, preview, dan antreanmu dihentikan."); }
                case "reload" -> { length(args, 1); service.reload(message -> say(sender, message)); }
                case "list" -> {
                    length(args, 1);
                    if (service.current().definitions().isEmpty()) say(sender, "Belum ada bisikan. Mulai dengan create <id>.");
                    service.current().definitions().values().stream().sorted(Comparator.comparing(WhisperDefinition::id))
                            .forEach(definition -> say(sender, definition.id() + " | " + (definition.enabled() ? "ON" : "draft/OFF")
                                    + " | " + definition.targets().size() + " penerima | " + definition.lines().size() + " dialog | " + definition.durationTicks() / 20.0 + "s | batas " + definition.limit()));
                }
                case "info" -> { length(args, 2); info(sender, service.current().require(args[1])); }
                default -> throw new IllegalArgumentException("Subcommand '" + action + "' tidak dikenal.");
            }
        } catch (IllegalArgumentException exception) {
            sender.sendMessage(messages.component("prefix").append(Component.text("Bisikan: " + exception.getMessage(), NamedTextColor.RED)));
            help(sender, ACTIONS.contains(action) ? action : null);
        }
        return true;
    }
    private void editLine(CommandSender sender, String[] args) {
        if (args.length < 3) throw new IllegalArgumentException("Line gunakan add, set, remove, atau clear.");
        String operation = args[1].toLowerCase(Locale.ROOT), id = args[2];
        if (operation.equals("clear")) {
            length(args, 4);
            if (!args[3].equalsIgnoreCase("confirm")) throw new IllegalArgumentException("Tambahkan confirm untuk menghapus semua dialog; progres tetap.");
            save(sender, id, state -> state.put(state.require(id).withEnabled(false).withLines(List.of()))); return;
        }
        if (operation.equals("remove")) {
            length(args, 4); int index = index(args[3]);
            save(sender, id, state -> {
                var definition = state.require(id); var lines = new ArrayList<>(definition.lines());
                if (index >= lines.size()) throw new IllegalArgumentException("Nomor dialog tidak ada.");
                lines.remove(index); return state.put(definition.withLines(lines));
            });
            return;
        }
        if (!List.of("add", "set").contains(operation)) throw new IllegalArgumentException("Line gunakan add, set, remove, atau clear.");
        int timing = operation.equals("set") ? 4 : 3;
        if (args.length < timing + 3) throw new IllegalArgumentException("Lengkapi durasi, jeda dan teks dialog.");
        int index = operation.equals("set") ? index(args[3]) : -1;
        int duration = ticks(args[timing], false), gap = ticks(args[timing + 1], true);
        String combined = String.join(" ", Arrays.copyOfRange(args, timing + 2, args.length));
        int separator = combined.indexOf('|');
        String title = (separator < 0 ? combined : combined.substring(0, separator)).strip();
        String subtitle = separator < 0 ? "" : combined.substring(separator + 1).strip();
        WhisperLine line = new WhisperLine(title.equals("-") ? "" : title, subtitle.equals("-") ? "" : subtitle, duration, gap, null);
        save(sender, id, state -> {
            var definition = state.require(id); var lines = new ArrayList<>(definition.lines());
            if (index < 0) lines.add(line);
            else {
                if (index >= lines.size()) throw new IllegalArgumentException("Nomor dialog tidak ada; lihat info.");
                // Editing text/timing retains its sound; sound off removes it explicitly.
                lines.set(index, line.withAudio(lines.get(index).audio()));
            }
            return state.put(definition.withLines(lines));
        });
    }
    private void info(CommandSender sender, WhisperDefinition definition) {
        sender.sendMessage(Component.empty());
        say(sender, definition.id() + " | " + (definition.enabled() ? "ON" : "OFF") + " | batas rangkaian " + definition.limit() + " | durasi " + definition.durationTicks() / 20.0 + " detik");
        var music = definition.music(); var visual = definition.visual();
        say(sender, "Musik: " + (music == null ? "off" : music.sound() + " | ulang " + (music.repeatTicks() == 0 ? "off" : music.repeatTicks() / 20.0 + "s")));
        say(sender, "Mengetik: " + (visual.typing() ? "ON, " + visual.charactersPerTick() + " karakter/tick" : "OFF")
                + " | fade masuk/keluar " + visual.fadeInTicks() / 20.0 + "/" + visual.fadeOutTicks() / 20.0 + "s");
        var area = definition.area();
        say(sender, area == null ? "Area belum diatur." : "Dunia " + area.world() + " | A " + area.minX() + "," + area.minY() + "," + area.minZ()
                                                          + " | B " + area.maxX() + "," + area.maxY() + "," + area.maxZ());
        for (UUID target : definition.targets().stream().sorted().toList()) {
            Player online = Bukkit.getPlayer(target);
            say(sender, "Penerima " + (online == null ? target.toString() : online.getName()) + " [" + target + "] | selesai "
                    + service.current().count(definition.id(), target) + "/" + definition.limit());
        }
        for (int i = 0; i < definition.lines().size(); i++) {
            var line = definition.lines().get(i);
            say(sender, "#" + (i + 1) + " " + line.durationTicks() / 20.0 + "s, jeda " + line.gapTicks() / 20.0 + "s | "
                    + "suara " + (line.audio() == null ? "off" : line.audio()));
            sender.sendMessage(Component.text("  ", NamedTextColor.GRAY).append(WhisperText.component(line.title(), NamedTextColor.WHITE))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY)).append(WhisperText.component(line.subtitle(), NamedTextColor.GRAY)));
        }
        sender.sendMessage(Component.empty());
    }
    private void save(CommandSender sender, String id, UnaryOperator<WhisperState> operation) {
        save(sender, id, operation, null);
    }
    private void save(CommandSender sender, String id, UnaryOperator<WhisperState> operation, String next) {
        service.change(id, operation, message -> {
            say(sender, message);
            if (next != null && message.startsWith("Tersimpan.")) say(sender, next);
        });
    }
    private WhisperLine.Audio optionalAudio(String[] args, int key, float defaultVolume) {
        if (args[key].equalsIgnoreCase("off")) {
            if (args.length != key + 1) throw new IllegalArgumentException("Sound off tidak menerima volume/pitch.");
            return null;
        }
        return audio(args[key], args.length > key + 1 ? args[key + 1] : Float.toString(defaultVolume), args.length > key + 2 ? args[key + 2] : "1");
    }
    private WhisperLine.Audio audio(String key, String volume, String pitch) {
        var audio = new WhisperLine.Audio(key.contains(":") ? key : "minecraft:" + key, decimal(volume), decimal(pitch));
        service.validateAudio(audio); return audio;
    }
    private void say(CommandSender sender, String text) {
        if (sender instanceof Player player && !player.isOnline()) return;
        sender.sendMessage(messages.component("prefix").append(Component.text(text, NamedTextColor.AQUA)));
    }
    private Player player(CommandSender sender) {
        if (!(sender instanceof Player player)) throw new IllegalArgumentException("Command ini hanya untuk pemain/admin di dalam game.");
        return player;
    }
    private UUID target(String text) {
        if (text.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")) return UUID.fromString(text);
        Player online = Bukkit.getPlayerExact(text);
        if (online != null) return online.getUniqueId();
        OfflinePlayer known = Bukkit.getOfflinePlayerIfCached(text);
        if (known == null || !known.hasPlayedBefore())
            throw new IllegalArgumentException("Nama '" + text + "' belum dikenal. Pemain harus pernah join, atau gunakan UUID lengkap.");
        return known.getUniqueId();
    }
    private void length(String[] args, int expected) {
        if (args.length != expected) throw new IllegalArgumentException("Jumlah argumen tidak sesuai. Ikuti contoh di bawah.");
    }
    private int index(String value) { return integer(value, 1, 100) - 1; }
    private float decimal(String value) {
        try { return Float.parseFloat(value); }
        catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Volume/pitch harus angka desimal; contoh volume 0.3 dan pitch 1.");
        }
    }
    private int integer(String value, int min, int max) {
        if (!value.matches("[0-9]{1,3}")) throw new IllegalArgumentException("Gunakan angka bulat " + min + "–" + max + ".");
        int parsed = Integer.parseInt(value);
        if (parsed < min || parsed > max) throw new IllegalArgumentException("Angka harus " + min + "–" + max + ".");
        return parsed;
    }
    private int ticks(String seconds, boolean gap) {
        return rangedTicks(seconds, gap ? 0 : 20, 1200, "Tampil 1–60 detik, jeda 0–60 detik.");
    }
    private int rangedTicks(String seconds, int min, int max, String range) {
        try {
            int value = new BigDecimal(seconds).multiply(BigDecimal.valueOf(20)).intValueExact();
            if (value < min || value > max) throw new ArithmeticException();
            return value;
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IllegalArgumentException(range + " Waktu kelipatan 0.05; contoh 3 atau 1.5.");
        }
    }
    public void help(CommandSender sender, String topic) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("━━ Bantuan bisikan ━━", NamedTextColor.GOLD));
        String usage = switch (Objects.toString(topic, "")) {
            case "wand" -> "/vitae bisikan wand <id>\nKlik kiri/kanan dua blok lantai; ruang di atasnya disimpan otomatis sebagai area bisikan.";
            case "pos1", "pos2" -> "/vitae bisikan pos1\n/vitae bisikan pos2\nBerdiri di blok sudut A/B, termasuk tinggi area; lalu area <id>.";
            case "create" -> "/vitae bisikan create <id>\nID huruf kecil, angka, _ atau -. Dibuat sebagai draft OFF.";
            case "template" -> "/vitae bisikan template <id> lunar|ryujin|outsider|elysia|penyamar\nBisa membuat ID baru. Mengganti dialog, musik, tampilan, plays 1 dan OFF. Area/penerima/progres lama tetap.\nContoh: /vitae bisikan template dungeon_lunar lunar";
            case "area" -> "/vitae bisikan area <id>\n/vitae bisikan area copy <id-tujuan> <id-sumber>\nSalin area saja; dialog/penerima/progres tiap ID tetap terpisah.";
            case "target" -> "/vitae bisikan target add|remove <id> <nick-MC|uuid> [pemain-lain...]\nContoh: /vitae bisikan target add dungeon_outsider kael MuunDI Vick0813\nSemua nama diperiksa sebelum menyimpan; gunakan nick MC, bukan nama RP.";
            case "line" -> "/vitae bisikan line add <id> <tampil-detik> <jeda-detik> <title> | <subtitle>\n/vitae bisikan line set <id> <nomor> <tampil-detik> <jeda-detik> <title> | <subtitle>\n/vitae bisikan line remove <id> <nomor>\n/vitae bisikan line clear <id> confirm\nContoh: /vitae bisikan line add dungeon_lunar 3 1 &9Lunar | -\n'-' berarti kosong. Warna &0–&f, &l tebal, &o miring, &r reset, hex &#55AAFF. Set mempertahankan suara baris itu.";
            case "dialog" -> "/vitae bisikan dialog <id> <tampil-detik> <jeda-detik> <teks-subtitle>\nTeks panjang otomatis dibagi per kata menjadi beberapa halaman kecil.\nContoh: /vitae bisikan dialog dungeon_lunar 4 1 &cJangan harap kau bisa selamat di dungeon kali ini.";
            case "sound" -> "/vitae bisikan sound <id> <nomor|all> <namespace:key|off> [volume 0–1] [pitch 0.5–2]\nContoh: /vitae bisikan sound dungeon_lunar all minecraft:block.wood.break 0.65 0.65\nEfek dimainkan sekali pada awal tiap dialog. All mengatur dialog yang sudah ada; tambahkan semua dialog dulu.";
            case "music" -> "/vitae bisikan music <id> <namespace:key|off> [volume 0–1] [pitch 0.5–2] [ulang-detik|off]\nContoh: /vitae bisikan music dungeon_lunar minecraft:music_disc.11 0.5 0.85 off\nMusik berjalan bersama efek dialog, hanya untuk penerima. Ulang off = tidak mengulang; sesuaikan interval dengan panjang file.\nMusik berhenti saat selesai/stop/putus koneksi. Key custom memerlukan resource pack seperti ItemsAdder.";
            case "soundtest" -> "/vitae bisikan soundtest <namespace:key> [volume 0–1] [pitch 0.5–2]\nContoh: /vitae bisikan soundtest minecraft:entity.ghast.scream 0.6 0.65\nSuara tes hanya terdengar olehmu. Stop sebelum tes jika preview masih berjalan.";
            case "typing" -> "/vitae bisikan typing <id> on|off [karakter-per-tick 1–8]\nContoh: /vitae bisikan typing dungeon_lunar on 1\nJika durasi terlalu pendek, pengetikan dipercepat agar teks lengkap masih sempat dibaca.";
            case "fade" -> "/vitae bisikan fade <id> <masuk-detik 0–2> <keluar-detik 0–2>\nContoh: /vitae bisikan fade dungeon_lunar 0.25 0.5\nTyping memakai fade masuk 0 agar teks tidak berkedip; fade keluar tetap dipakai.";
            case "pace" -> "/vitae bisikan pace <id> <tampil-detik 1–60> <jeda-detik 0–60>\nMengubah waktu semua dialog yang sudah ada, sambil mempertahankan teks dan efek suara.";
            case "plays" -> "/vitae bisikan plays <id> <1–100>\nDefault 1 = seluruh rangkaian hanya sekali per pemain, termasuk setelah restart.";
            case "enable", "disable" -> "/vitae bisikan enable <id>\n/vitae bisikan disable <id>\nEnable setelah area, penerima dan dialog lengkap. Masuk kembali dari luar area untuk memicu.";
            case "preview" -> "/vitae bisikan preview <id> [nomor-awal]\nContoh: /vitae bisikan preview dungeon_lunar 5\nPreview ke dirimu tanpa memakai jatah pemain, termasuk draft OFF.";
            case "stop" -> "/vitae bisikan stop\nHentikan bisikan, musik, suara tes dan antrean dirimu tanpa menandai selesai.";
            case "reset" -> "/vitae bisikan reset <id> <nick-MC|uuid|all>\nMenghapus progres hanya pada ID/pemain yang dipilih. Edit/template tidak otomatis meresetnya.";
            case "delete" -> "/vitae bisikan delete <id> confirm\nMenghapus ID beserta progres. Disable untuk menonaktifkan sementara.";
            case "reload" -> "/vitae bisikan reload\nMuat ulang whispers.yml; file tidak valid tidak menggantikan setup aktif.";
            case "list", "info" -> "/vitae bisikan list\n/vitae bisikan info <id>\nLihat area, penerima, progres, durasi, musik, dan nomor dialog.";
            default -> "Setup cepat: template <id> lunar → wand <id> → dua klik lantai → target add <id> <nick-MC> → preview <id> → enable <id>.\n"
                    + "/vitae bisikan help template\n/vitae bisikan help dialog\n/vitae bisikan help music\n/vitae bisikan help target\n/vitae bisikan help line\nSubcommand: " + String.join(", ", ACTIONS) + ".\nTunggu pesan Tersimpan sebelum command setup berikutnya.";
        };
        for (String row : usage.split("\n")) {
            var line = Component.text(row, row.startsWith("/vitae") ? NamedTextColor.AQUA : NamedTextColor.GRAY);
            if (row.startsWith("/vitae") && !row.contains("<") && !row.contains("[")) line = line
                    .clickEvent(ClickEvent.suggestCommand(row)).hoverEvent(HoverEvent.showText(Component.text("Klik untuk menaruh command di chat.", NamedTextColor.YELLOW)));
            sender.sendMessage(line);
        }
        sender.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.empty());
    }
    public List<String> complete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vitae.admin.whisper") || args.length == 0) return List.of();
        List<String> choices = new ArrayList<>(); String action = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 1) choices.addAll(ACTIONS);
        else if (args.length == 2) {
            if (action.equals("help")) choices.addAll(ACTIONS);
            else if (action.equals("target")) choices.addAll(List.of("add", "remove"));
            else if (action.equals("line")) choices.addAll(List.of("add", "set", "remove", "clear"));
            else if (action.equals("soundtest")) choices.addAll(SOUNDS.subList(1, SOUNDS.size()));
            else if (!List.of("create", "pos1", "pos2", "stop", "reload", "list").contains(action)) {
                choices.addAll(service.current().definitions().keySet()); if (action.equals("area")) choices.add("copy");
            }
        } else if ((args.length == 3 && List.of("target", "line").contains(action))
                || (action.equals("area") && args[1].equalsIgnoreCase("copy") && (args.length == 3 || args.length == 4)))
            choices.addAll(service.current().definitions().keySet());
        else if (args.length == 3 && action.equals("template")) choices.addAll(WhisperTemplates.names());
        else if ((args.length == 3 && action.equals("delete")) || (args.length == 4 && action.equals("line") && args[1].equalsIgnoreCase("clear"))) choices.add("confirm");
        else if ((args.length >= 4 && action.equals("target")) || (args.length == 3 && action.equals("reset"))) {
            Bukkit.getOnlinePlayers().forEach(player -> choices.add(player.getName())); if (action.equals("reset")) choices.add("all");
        } else if (args.length == 3 && action.equals("typing")) choices.addAll(List.of("on", "off"));
        else if (args.length == 4 && action.equals("typing")) choices.addAll(List.of("1", "2", "3", "4"));
        else if (args.length == 3 && action.equals("plays")) choices.addAll(List.of("1", "2", "3"));
        else if ((args.length == 3 && List.of("sound", "preview").contains(action))
                || (args.length == 4 && action.equals("line") && List.of("set", "remove").contains(args[1].toLowerCase(Locale.ROOT)))) {
            String id = args[action.equals("line") ? 2 : 1]; var definition = service.current().definitions().get(id);
            if (definition != null) for (int i = 1; i <= definition.lines().size(); i++) choices.add(Integer.toString(i));
            if (action.equals("sound")) choices.add("all");
        } else if ((args.length == 4 && action.equals("sound")) || (args.length == 3 && action.equals("music"))) choices.addAll(SOUNDS);
        else if (args.length == 6 && action.equals("music")) choices.addAll(List.of("off", "30", "60"));
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return choices.stream().distinct().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix)).sorted().toList();
    }
}
