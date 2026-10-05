package com.bangzachery.vitae.vitaemanager.command;

import com.bangzachery.vitae.vitaemanager.evolution.*;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Consumer;

import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionAltar.*;

public final class EvolutionCommand implements CommandExecutor, TabCompleter {
    private final EvolutionService service;
    private final Map<String, String> usage = new LinkedHashMap<>();

    public EvolutionCommand(EvolutionService service) {
        this.service = service;

        usage.put("create", "create <id> — lihat crafting table");
        usage.put("source", "source <id> — pegang senjata awal");
        usage.put("result", "result <id> — pegang hasil evolusi");
        usage.put("ingredient",
                "ingredient <id> add <jumlah> | remove <nomor> — pegang bahan, lihat pedestal");
        usage.put("target", "target <id> add|remove <player|uuid>");
        usage.put("preset", "preset <id> inferno|stormforge|void");
        usage.put("settings",
                "settings <id> duration|scale|density|soundradius|damageradius|damage <nilai>");
        usage.put("text", "text <id> <tahap> title | subtitle");
        usage.put("sound",
                "sound <id> <tahap> private|public <namespace:sound|off> [volume] [pitch]");

        for (String action : List.of("enable", "disable", "info", "preview")) {
            usage.put(action, action + " <id>");
        }
        for (String action : List.of("pause", "resume", "cancel")) {
            usage.put(action, action + " [player]");
        }

        usage.put("delete", "delete <id> confirm");
        usage.put("recover",
                "recover <player|uuid> refund|result|discard confirm");

        for (String action : List.of("list", "pending", "reload")) {
            usage.put(action, action);
        }
    }

    @Override
    public boolean onCommand(
            CommandSender sender, Command command, String label, String[] args
    ) {
        String action = args.length == 0
                ? "help" : args[0].toLowerCase(Locale.ROOT);

        try {
            if (action.equals("help")) {
                help(sender, args.length > 1 ? args[1] : "");
                return true;
            }

            if (action.equals("cancel")) {
                require(sender.hasPermission("vitae.evolution") || admin(sender),
                        "Tidak memiliki izin ritual.");
                require(args.length <= 2, "Gunakan /evo cancel [player].");
                service.control(
                        args.length == 2
                                ? adminPlayer(sender, args[1]) : player(sender),
                        action);
                return true;
            }

            require(admin(sender), "Command ini khusus admin.");

            switch (action) {
                case "create" -> {
                    exact(args, 2);
                    service.create(id(args[1]), looked(player(sender)), sender);
                }
                case "source", "result" -> {
                    exact(args, 2);
                    String sample = service.sample(player(sender));
                    edit(sender, args[1], section -> section.set(action, sample));
                }
                case "ingredient" -> ingredient(sender, args);
                case "target" -> {
                    exact(args, 4);
                    var targets = new HashSet<>(
                            service.altar(id(args[1])).targets());
                    UUID target = target(args[3]);

                    if (args[2].equalsIgnoreCase("add")) {
                        require(targets.add(target),
                                "Pemain sudah menjadi penerima.");
                    } else if (args[2].equalsIgnoreCase("remove")) {
                        require(targets.remove(target),
                                "Pemain belum menjadi penerima.");
                    } else {
                        throw new IllegalArgumentException(
                                "Target: add atau remove.");
                    }

                    edit(sender, args[1], section -> section.set(
                            "targets",
                            targets.stream().map(UUID::toString).sorted().toList()));
                }
                case "preset" -> {
                    exact(args, 3);
                    Preset preset = Preset.parse(args[2]);
                    edit(sender, args[1],
                            section -> section.set("preset", preset.name()));
                }
                case "settings" -> {
                    exact(args, 4);
                    var keys = Map.of(
                            "duration", "duration",
                            "scale", "scale",
                            "density", "density",
                            "soundradius", "sound-radius",
                            "damageradius", "damage-radius",
                            "damage", "damage"
                    );
                    String key = keys.get(args[2].toLowerCase(Locale.ROOT));
                    require(key != null, "Pengaturan tidak dikenal.");

                    double value = Double.parseDouble(args[3]);
                    edit(sender, args[1], section -> section.set(key, value));
                }
                case "text" -> {
                    require(args.length >= 4,
                            "Title/subtitle belum diberikan.");
                    String id = id(args[1]);
                    Stage stage = Stage.parse(args[2]);
                    Cue old = service.altar(id).cue(stage);

                    String[] text = String.join(" ",
                                    Arrays.copyOfRange(args, 3, args.length))
                            .split("\\|", 2);

                    cue(sender, id, stage, new Cue(
                            empty(text[0]),
                            text.length == 2 ? empty(text[1]) : "",
                            old.sound(), old.privateSound(),
                            old.volume(), old.pitch()
                    ));
                }
                case "sound" -> {
                    require(args.length >= 5 && args.length <= 7,
                            "Jumlah argumen sound tidak valid.");
                    String id = id(args[1]);
                    Stage stage = Stage.parse(args[2]);
                    Cue old = service.altar(id).cue(stage);

                    require(args[3].equalsIgnoreCase("private")
                                    || args[3].equalsIgnoreCase("public"),
                            "Audience: private atau public.");

                    cue(sender, id, stage, new Cue(
                            old.title(), old.subtitle(),
                            args[4].equalsIgnoreCase("off") ? "" : args[4],
                            args[3].equalsIgnoreCase("private"),
                            args.length >= 6
                                    ? Float.parseFloat(args[5]) : old.volume(),
                            args.length >= 7
                                    ? Float.parseFloat(args[6]) : old.pitch()
                    ));
                }
                case "enable", "disable" -> {
                    exact(args, 2);
                    edit(sender, args[1],
                            section -> section.set(
                                    "enabled", action.equals("enable")));
                }
                case "list" -> {
                    exact(args, 1);
                    service.say(sender, "Altar: " + String.join(", ",
                            service.ids().stream().sorted().toList()));
                }
                case "info" -> {
                    exact(args, 2);
                    var altar = service.altar(id(args[1]));

                    service.say(sender, altar.id() + " | "
                            + (altar.enabled() ? "aktif" : "nonaktif")
                            + " | " + altar.preset()
                            + " | " + altar.ticks() / 20.0 + "s");
                    service.say(sender, "Bahan " + altar.ingredients().size()
                            + " | target " + altar.targets().size()
                            + " | scale " + altar.scale()
                            + " | density " + altar.density());
                    service.say(sender, "Radius suara " + altar.soundRadius()
                            + " | damage " + altar.damage()
                            + " | radius damage " + altar.damageRadius());

                    for (int i = 0; i < altar.ingredients().size(); i++) {
                        service.say(sender, "Bahan " + (i + 1) + ": x"
                                + altar.ingredients().get(i).count()
                                + " di " + altar.ingredients().get(i).point().encode());
                    }
                }
                case "preview" -> {
                    exact(args, 2);
                    service.preview(player(sender), id(args[1]));
                }
                case "pause", "resume" -> {
                    require(args.length <= 2, "Terlalu banyak argumen.");
                    service.control(
                            args.length == 2
                                    ? adminPlayer(sender, args[1]) : player(sender),
                            action);
                }
                case "pending" -> {
                    exact(args, 1);
                    var pending = service.pending();
                    if (pending.isEmpty()) {
                        service.say(sender, "Tidak ada ritual tertunda.");
                    } else {
                        pending.forEach(ticket -> service.say(sender,
                                ticket.owner() + " | " + ticket.altar()
                                        + " | " + ticket.phase()
                                        + " | ticket " + ticket.id()));
                        service.say(sender,
                                "Periksa inventory dan item world: input, refund, atau hasil mungkin sudah ada. Recovery tidak memeriksa item yang dipindahkan pemain.");
                        help(sender, "recover");
                    }
                }
                case "recover" -> {
                    exact(args, 4);
                    confirm(args[3]);
                    String choice = args[2].toLowerCase(Locale.ROOT);
                    require(List.of("refund", "result", "discard").contains(choice),
                            "Recovery: refund, result, discard.");
                    service.recover(target(args[1]), choice, sender);
                }
                case "delete" -> {
                    exact(args, 3);
                    confirm(args[2]);
                    service.delete(id(args[1]), sender);
                }
                case "reload" -> {
                    exact(args, 1);
                    service.reload(sender);
                }
                default -> throw new IllegalArgumentException(
                        "Command tidak dikenal.");
            }
        } catch (NumberFormatException exception) {
            service.say(sender, "Angka tidak valid.");
            help(sender, action);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            service.say(sender, exception.getMessage());
            help(sender, action);
        }
        return true;
    }

    private void ingredient(CommandSender sender, String[] args) {
        exact(args, 4);
        String id = id(args[1]);
        var altar = service.altar(id);

        if (args[2].equalsIgnoreCase("add")) {
            Player player = player(sender);
            var ingredient = new Ingredient(
                    service.sample(player),
                    Integer.parseInt(args[3]),
                    EvolutionService.point(looked(player))
            );

            edit(sender, id, section -> EvolutionStore.writeIngredient(
                    section.createSection(
                            "ingredients." + (altar.ingredients().size() + 1)),
                    ingredient
            ));
        } else if (args[2].equalsIgnoreCase("remove")) {
            int index = Integer.parseInt(args[3]) - 1;
            require(index >= 0 && index < altar.ingredients().size(),
                    "Nomor bahan tidak ditemukan.");

            var next = new ArrayList<>(altar.ingredients());
            next.remove(index);

            edit(sender, id, section -> {
                section.set("ingredients", null);
                var root = section.createSection("ingredients");
                for (int i = 0; i < next.size(); i++) {
                    EvolutionStore.writeIngredient(
                            root.createSection(Integer.toString(i + 1)),
                            next.get(i)
                    );
                }
            });
        } else {
            throw new IllegalArgumentException("Ingredient: add atau remove.");
        }
    }

    private void edit(
            CommandSender sender, String id,
            Consumer<ConfigurationSection> change
    ) {
        service.edit(id(id), change, sender);
    }

    private void cue(
            CommandSender sender, String id, Stage stage, Cue cue
    ) {
        edit(sender, id, section -> {
            String path = "cues." + stage.name().toLowerCase(Locale.ROOT);
            section.set(path, null);
            EvolutionStore.writeCue(section.createSection(path), cue);
        });
    }

    private void help(CommandSender sender, String topic) {
        if (!admin(sender)) {
            service.say(sender,
                    "Klik kanan altar yang ditujukan untukmu. /evo cancel untuk membatalkan ritualmu.");
            return;
        }

        topic = topic.toLowerCase(Locale.ROOT);

        if (topic.equals("setup")) {
            for (String key : List.of(
                    "create", "source", "result", "ingredient", "target", "enable")) {
                service.say(sender, "/evo " + usage.get(key));
            }
            service.say(sender,
                    "Pedestal berbeda, solid, maksimal 8 block dari altar. Tunggu pesan tersimpan sebelum command berikutnya.");
        } else if (usage.containsKey(topic)) {
            service.say(sender, "/evo " + usage.get(topic));
        } else {
            service.say(sender,
                    "/evo help setup. Bantuan command: /evo help <command>.");
            usage.values().forEach(line -> service.say(sender, "/evo " + line));
        }

        if (topic.equals("settings")) {
            service.say(sender,
                    "Duration 5–120 detik; scale 0.5–2; density 8–64; soundradius 1–64; damageradius 0–16; damage 0–1000. Damage default 0.");
        }
        if (topic.equals("text") || topic.equals("sound")) {
            service.say(sender,
                    "Tahap: awaken/lift/orbit/absorb/finish. Teks - berarti kosong. Volume 0–1; pitch 0.5–2. Private hanya pemilik; public mengikuti soundradius.");
        }
        if (topic.equals("recover")) {
            service.say(sender,
                    "Periksa item dahulu. Tidak ada payout otomatis setelah crash. Refund/result memerlukan pemain online.");
        }
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender, Command command, String alias, String[] args
    ) {
        if (args.length == 0) return List.of();

        String action = args[0].toLowerCase(Locale.ROOT);
        List<String> choices = List.of();

        if (args.length == 1) {
            var list = new ArrayList<String>();
            list.add("help");
            list.addAll(admin(sender) ? usage.keySet() : List.of("cancel"));
            choices = list;
        } else if (admin(sender)) {
            if (args.length == 2) {
                choices = switch (action) {
                    case "help" -> {
                        var topics = new ArrayList<>(usage.keySet());
                        topics.add("setup");
                        yield topics;
                    }
                    case "pause", "resume", "cancel", "recover" ->
                            Bukkit.getOnlinePlayers().stream()
                                    .map(Player::getName).toList();
                    case "source", "result", "ingredient", "target", "preset",
                         "settings", "text", "sound", "enable", "disable",
                         "info", "preview", "delete" ->
                            service.ids().stream().toList();
                    default -> List.of();
                };
            } else if (args.length == 3) {
                choices = switch (action) {
                    case "ingredient", "target" -> List.of("add", "remove");
                    case "preset" -> List.of("inferno", "stormforge", "void");
                    case "settings" -> List.of(
                            "duration", "scale", "density",
                            "soundradius", "damageradius", "damage");
                    case "text", "sound" -> List.of(
                            "awaken", "lift", "orbit", "absorb", "finish");
                    case "recover" -> List.of("refund", "result", "discard");
                    case "delete" -> List.of("confirm");
                    default -> List.of();
                };
            } else if (args.length == 4) {
                choices = switch (action) {
                    case "target" -> Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName).toList();
                    case "sound" -> List.of("private", "public");
                    case "ingredient" -> List.of("1", "2", "4", "8");
                    case "recover" -> List.of("confirm");
                    default -> List.of();
                };
            } else if (args.length == 5 && action.equals("sound")) {
                var keys = new ArrayList<String>();
                keys.add("off");
                Registry.SOUNDS.stream()
                        .map(sound -> Registry.SOUNDS.getKeyOrThrow(sound).toString())
                        .forEach(keys::add);
                choices = keys;
            } else if (args.length == 6 && action.equals("sound")) {
                choices = List.of("0.2", "0.5", "0.8", "1.0");
            } else if (args.length == 7 && action.equals("sound")) {
                choices = List.of("0.5", "1.0", "1.5", "2.0");
            }
        }

        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return choices.stream()
                .filter(choice -> choice.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted().limit(50).toList();
    }

    private static boolean admin(CommandSender sender) {
        return sender.hasPermission("vitae.admin")
                && sender.hasPermission("vitae.admin.evolution");
    }

    private static Player player(CommandSender sender) {
        require(sender instanceof Player,
                "Command ini harus digunakan pemain.");
        return (Player) sender;
    }

    private static Player adminPlayer(CommandSender sender, String name) {
        require(admin(sender), "Hanya admin dapat memilih pemain lain.");
        Player player = Bukkit.getPlayerExact(name);
        require(player != null, "Pemain harus online dengan nama tepat.");
        return player;
    }

    private static Block looked(Player player) {
        Block block = player.getTargetBlockExact(6);
        require(block != null, "Lihat block dalam jarak 6 block.");
        return block;
    }

    private static String id(String text) {
        String id = text.toLowerCase(Locale.ROOT);
        require(id.matches("[a-z0-9_-]{1,32}"),
                "ID: 1–32 huruf kecil, angka, _ atau -.");
        return id;
    }

    private static UUID target(String name) {
        try {
            return UUID.fromString(name);
        } catch (IllegalArgumentException ignored) {
        }

        Player player = Bukkit.getPlayerExact(name);
        if (player != null) return player.getUniqueId();

        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
        require(cached != null,
                "Pemain belum dikenal; minta join dulu atau gunakan UUID.");
        return cached.getUniqueId();
    }

    private static String empty(String value) {
        String text = value.trim();
        return text.equals("-") ? "" : text;
    }

    private void exact(String[] args, int length) {
        require(args.length == length,
                "Gunakan /evo " + usage.get(args[0].toLowerCase(Locale.ROOT)));
    }

    private static void confirm(String word) {
        require(word.equalsIgnoreCase("confirm"),
                "Tambahkan confirm setelah memeriksa tindakan.");
    }
}