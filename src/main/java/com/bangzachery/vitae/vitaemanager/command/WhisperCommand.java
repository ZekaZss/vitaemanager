package com.bangzachery.vitae.vitaemanager.command;

import com.bangzachery.vitae.vitaemanager.core.message.MessageService;
import com.bangzachery.vitae.vitaemanager.whisper.*;
import net.kyori.adventure.text.Component;
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
    private static final List<String> ACTIONS = List.of("help", "pos1", "pos2", "create", "area", "target", "line",
            "sound", "plays", "enable", "disable", "list", "info", "preview", "stop", "reset", "delete", "reload");
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
                case "pos1", "pos2" -> {
                    length(args, 1); Player player = player(sender); service.select(player, action.equals("pos1"));
                    var location = player.getLocation();
                    say(sender, "Titik " + (action.equals("pos1") ? "A" : "B") + " = " + location.getWorld().getName()
                            + " " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ". Titik memakai blok tempat berdiri.");
                }
                case "create" -> { length(args, 2); save(sender, args[1], state -> state.create(args[1])); }
                case "area" -> {
                    length(args, 2); var area = service.selection(player(sender));
                    save(sender, args[1], state -> state.put(state.require(args[1]).withArea(area)));
                }
                case "target" -> {
                    length(args, 4); String operation = args[1].toLowerCase(Locale.ROOT);
                    if (!List.of("add", "remove").contains(operation)) throw new IllegalArgumentException("Target gunakan add atau remove.");
                    UUID target = target(args[3]);
                    save(sender, args[2], state -> {
                        var definition = state.require(args[2]); var targets = new HashSet<>(definition.targets());
                        boolean changed = operation.equals("add") ? targets.add(target) : targets.remove(target);
                        if (!changed) throw new IllegalArgumentException("Penerima sudah ada atau belum terdaftar untuk dihapus.");
                        return state.put(definition.withTargets(targets));
                    });
                }
                case "line" -> editLine(sender, args);
                case "sound" -> {
                    if (args.length < 4 || args.length > 6) throw new IllegalArgumentException("Sound memerlukan ID, nomor dialog, key/off, dan volume/pitch opsional.");
                    int index = index(args[2]); WhisperLine.Audio audio = null;
                    if (args[3].equalsIgnoreCase("off")) {
                        if (args.length != 4) throw new IllegalArgumentException("Sound off tidak menerima volume/pitch.");
                    } else {
                        String key = args[3].contains(":") ? args[3] : "minecraft:" + args[3];
                        audio = new WhisperLine.Audio(key, args.length > 4 ? decimal(args[4]) : 0.35f,
                                args.length > 5 ? decimal(args[5]) : 1f);
                        service.validateAudio(audio);
                    }
                    WhisperLine.Audio selected = audio;
                    save(sender, args[1], state -> {
                        var definition = state.require(args[1]); var lines = new ArrayList<>(definition.lines());
                        if (index >= lines.size()) throw new IllegalArgumentException("Nomor dialog tidak ada; lihat info.");
                        lines.set(index, lines.get(index).withAudio(selected));
                        return state.put(definition.withLines(lines));
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
                case "preview" -> { length(args, 2); service.preview(player(sender), args[1]); say(sender, "Preview masuk antreanmu; tidak mengubah progres."); }
                case "stop" -> { length(args, 1); service.stop(player(sender)); say(sender, "Bisikan/preview yang berjalan dan antreanmu dihentikan."); }
                case "reload" -> { length(args, 1); service.reload(message -> say(sender, message)); }
                case "list" -> {
                    length(args, 1);
                    if (service.current().definitions().isEmpty()) say(sender, "Belum ada bisikan. Mulai dengan create <id>.");
                    service.current().definitions().values().stream().sorted(Comparator.comparing(WhisperDefinition::id))
                            .forEach(definition -> say(sender, definition.id() + " | " + (definition.enabled() ? "ON" : "draft/OFF")
                                    + " | " + definition.targets().size() + " penerima | " + definition.lines().size() + " dialog | batas " + definition.limit()));
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
        if (args.length < 3) throw new IllegalArgumentException("Line gunakan add, set atau remove.");
        String operation = args[1].toLowerCase(Locale.ROOT), id = args[2];
        if (operation.equals("remove")) {
            length(args, 4); int index = index(args[3]);
            save(sender, id, state -> {
                var definition = state.require(id); var lines = new ArrayList<>(definition.lines());
                if (index >= lines.size()) throw new IllegalArgumentException("Nomor dialog tidak ada.");
                lines.remove(index); return state.put(definition.withLines(lines));
            });
            return;
        }
        if (!List.of("add", "set").contains(operation)) throw new IllegalArgumentException("Line gunakan add, set atau remove.");
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
        say(sender, definition.id() + " | " + (definition.enabled() ? "ON" : "OFF") + " | batas rangkaian " + definition.limit());
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
                    + line.title() + " | " + line.subtitle() + " | suara " + (line.audio() == null ? "off" : line.audio()));
        }
    }
    private void save(CommandSender sender, String id, UnaryOperator<WhisperState> operation) {
        service.change(id, operation, message -> say(sender, message));
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
            throw new IllegalArgumentException("Nama belum dikenal. Pemain harus pernah join, atau masukkan UUID lengkap (tanpa lookup internet).");
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
        try {
            int value = new BigDecimal(seconds).multiply(BigDecimal.valueOf(20)).intValueExact();
            if (value < (gap ? 0 : 20) || value > 1200) throw new ArithmeticException();
            return value;
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IllegalArgumentException("Waktu gunakan detik dengan kelipatan 0.05; tampil 1–60, jeda 0–60. Contoh: 3 atau 1.5.");
        }
    }
    public void help(CommandSender sender, String topic) {
        String usage = switch (Objects.toString(topic, "")) {
            case "pos1", "pos2" -> "pos1 / pos2 — berdiri di blok A/B; lalu area <id>.";
            case "create" -> "create <id> — contoh create lorong_lunar. Dibuat sebagai draft OFF.";
            case "area" -> "area <id> — simpan kedua titik untuk ID ini; contoh area lorong_lunar.";
            case "target" -> "target add|remove <id> <nama|uuid> — contoh target add lorong_lunar Lunar.";
            case "line" -> "line add <id> <tampil-detik> <jeda-detik> <title> | <subtitle>\n"
                    + "/vitae bisikan line set <id> <nomor> <tampil-detik> <jeda-detik> <title> | <subtitle>\n"
                    + "/vitae bisikan line remove <id> <nomor>\nContoh: /vitae bisikan line add lorong_lunar 3 2 Hei Lunar | Beraninya kau datang.";
            case "sound" -> "sound <id> <nomor> <namespace:key|off> [volume 0–1] [pitch 0.5–2]\n"
                    + "Contoh: /vitae bisikan sound lorong_lunar 1 minecraft:entity.villager.no 0.3 1";
            case "plays" -> "plays <id> <1–100> — batas rangkaian selesai per pemain; default 1.";
            case "enable", "disable" -> "enable|disable <id> — enable setelah area, penerima dan dialog lengkap.";
            case "preview" -> "preview <id> — tampilkan semua dialog ke dirimu tanpa mengubah progres, termasuk draft OFF.";
            case "stop" -> "stop — hentikan seluruh bisikan/preview dan antrean dirimu, tanpa menandai selesai.";
            case "reset" -> "reset <id> <nama|uuid|all> — reset jumlah rangkaian selesai pada area ini saja.";
            case "delete" -> "delete <id> confirm — hapus ID beserta progres. Gunakan disable untuk menonaktifkan sementara.";
            case "reload" -> "reload — muat ulang whispers.yml; setup invalid tidak menggantikan yang aktif.";
            case "list", "info" -> "list / info <id> — lihat area, penerima, progres dan nomor dialog.";
            default -> "help [topik]\nSetup: pos1 → pos2 → create <id> → area <id> → target add <id> <nama> → line add ... → preview <id> → enable <id>.\n"
                    + "Subcommand: " + String.join(", ", ACTIONS) + ".\nGunakan /vitae bisikan help line untuk contoh dialog. Teks literal; '-' berarti kosong.";
        };
        say(sender, "/vitae bisikan " + usage);
    }
    public List<String> complete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vitae.admin.whisper") || args.length == 0) return List.of();
        List<String> choices = new ArrayList<>();
        String action = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 1) choices.addAll(ACTIONS);
        else if (args.length == 2) {
            if (action.equals("help")) choices.addAll(ACTIONS);
            else if (action.equals("target")) choices.addAll(List.of("add", "remove"));
            else if (action.equals("line")) choices.addAll(List.of("add", "set", "remove"));
            else if (List.of("area", "sound", "plays", "enable", "disable", "info", "preview", "reset", "delete").contains(action))
                choices.addAll(service.current().definitions().keySet());
        } else if (args.length == 3 && List.of("target", "line").contains(action)) choices.addAll(service.current().definitions().keySet());
        else if (args.length == 3 && action.equals("delete")) choices.add("confirm");
        else if ((args.length == 4 && action.equals("target")) || (args.length == 3 && action.equals("reset"))) {
            Bukkit.getOnlinePlayers().forEach(player -> choices.add(player.getName()));
            if (action.equals("reset")) choices.add("all");
        } else if (args.length == 3 && action.equals("plays")) choices.addAll(List.of("1", "2", "3"));
        else if ((args.length == 3 && action.equals("sound"))
                || (args.length == 4 && action.equals("line") && !args[1].equalsIgnoreCase("add"))) {
            String id = args[action.equals("sound") ? 1 : 2];
            var definition = service.current().definitions().get(id);
            if (definition != null) for (int i = 1; i <= definition.lines().size(); i++) choices.add(Integer.toString(i));
        } else if (args.length == 4 && action.equals("sound"))
            choices.addAll(List.of("off", "minecraft:entity.villager.no", "minecraft:block.note_block.hat", "minecraft:block.note_block.pling"));
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return choices.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix)).sorted().toList();
    }
}