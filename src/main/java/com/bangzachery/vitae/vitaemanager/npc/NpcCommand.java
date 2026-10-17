package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.economy.VitiAmount;
import com.bangzachery.vitae.vitaemanager.whisper.WhisperArea;
import com.google.gson.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public final class NpcCommand implements CommandExecutor, TabCompleter {
    private record Corners(Location a, Location b) {}
    private final NpcService service;
    private final Map<UUID, Corners> selections = new HashMap<>();
    private static final List<String> TOPICS = List.of("dialog", "hadiah", "ladang", "mob", "pemicu", "gerbang", "tampilan", "ingatan", "kelola");
    private static final List<String> ACTIONS = List.of("help", "move", "stand", "look", "offset", "name", "nametag", "track", "remember", "memory", "height", "skin", "model", "page", "option",
            "clearoptions", "root", "repeat", "speed", "wand", "pos1", "pos2", "trigger", "gate", "quest", "queststart", "reward", "on", "off", "preview", "reset", "delete");
    public NpcCommand(NpcService service) { this.service = service; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("quest")) {
            if (!(sender instanceof Player p)) { sender.sendMessage(Component.text("Perintah ini khusus pemain.")); return true; }
            if (!p.hasPermission("vitae.quest")) { NpcService.message(p, "Kamu tidak memiliki izin quest."); return true; }
            if (args.length == 1 && args[0].equalsIgnoreCase("quit")) service.quitQuest(p);
            else if (args.length == 1 && args[0].equalsIgnoreCase("status")) service.status(p);
            else sender.sendMessage(Component.text("/quest status • /quest quit"));
            return true;
        }
        if (!sender.hasPermission("vitae.admin.npc")) { sender.sendMessage(Component.text("Perlu izin vitae.admin.npc.", NamedTextColor.RED)); return true; }
        try {
            if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
                sender.sendMessage(Component.text("[Vitae NPC] Modul aktif. NPC tersimpan: " + service.current().npcs().size(), NamedTextColor.GREEN)); return true;
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                service.reloadModule(); sender.sendMessage(Component.text("[Vitae NPC] Reload diproses. /vnpc status untuk hasilnya.", NamedTextColor.AQUA)); return true;
            }
            if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                require(args.length <= 2, "/vnpc help [topik]");
                help(sender, "<id>", args.length == 2 ? args[1] : ""); return true;
            }
            if (args[0].equalsIgnoreCase("list") && args.length == 1) {
                sender.sendMessage(Component.text("[Vitae NPC] ", NamedTextColor.GOLD)
                        .append(Component.text("NPC: " + (service.current().npcs().isEmpty() ? "belum ada" : String.join(", ", new TreeSet<>(service.current().npcs().keySet()))), NamedTextColor.AQUA))); return true;
            }
            if (!(sender instanceof Player p)) throw new IllegalArgumentException("Setup lokasi dilakukan sebagai pemain di dalam server.");
            if (args[0].equalsIgnoreCase("create")) {
                require(args.length >= 3, "/vnpc create <id> <nama>"); service.create(p, args[1], join(args, 2));
                created(p, args[1], join(args, 2)); return true;
            }
            require(args.length >= 2, "/vnpc <id> <pengaturan> atau /vnpc help");
            String id = args[0], action = args[1].toLowerCase(Locale.ROOT); var d = service.get(id);
            switch (action) {
                case "help" -> {
                    require(args.length <= 3, "/vnpc <id> help [topik]");
                    help(sender, id, args.length == 3 ? args[2] : ""); return true;
                }
                case "delete" -> { require(args.length == 2, "/vnpc <id> delete"); service.delete(id); }
                case "name" -> {
                    require(args.length >= 3, "/vnpc <id> name <nama>; warna contoh &6Penjaga &eGerbang");
                    String name = join(args, 2); require(!NpcText.plain(name).isBlank(), "Nama harus memiliki teks, bukan hanya kode warna.");
                    service.edit(id, j -> j.addProperty("name", name), false);
                }
                case "nametag", "track" -> {
                    require(args.length == 3 && Set.of("on", "off").contains(args[2].toLowerCase(Locale.ROOT)), "/vnpc <id> " + action + " on|off");
                    var appearance = new NpcData.Appearance(action.equals("nametag") ? args[2].equalsIgnoreCase("on") : d.appearance().nametag(),
                            action.equals("track") ? args[2].equalsIgnoreCase("on") : d.appearance().track());
                    service.edit(id, j -> j.add("appearance", NpcStore.JSON.toJsonTree(appearance)), false);
                }
                case "remember" -> {
                    require(args.length == 6 || args.length == 5 && args[4].equalsIgnoreCase("off"), "/vnpc <id> remember <halaman> <nomor-pilihan> <kunci> <nilai> | ... off");
                    var node = d.nodes().get(args[2]); require(node != null, "Halaman tidak ditemukan.");
                    int index = Integer.parseInt(args[3]) - 1; require(index >= 0 && index < node.choices().size(), "Nomor pilihan belum ada; buat option dulu.");
                    var choices = new ArrayList<>(node.choices()); var old = choices.get(index);
                    choices.set(index, new NpcData.Choice(old.label(), old.target(), args.length == 5 ? "" : args[4], args.length == 5 ? "" : args[5]));
                    service.edit(id, j -> j.getAsJsonObject("nodes").add(args[2], NpcStore.JSON.toJsonTree(new NpcData.Node(node.text(), choices))), false);
                }
                case "memory" -> {
                    if (args.length == 3 && args[2].equalsIgnoreCase("status")) {
                        for (var rule : d.memory()) NpcService.message(p, rule.key() + "=" + rule.value() + " → " + rule.page());
                        for (var entry : d.nodes().entrySet()) for (int i = 0; i < entry.getValue().choices().size(); i++) {
                            var choice = entry.getValue().choices().get(i);
                            if (!choice.memoryKey().isEmpty()) NpcService.message(p, entry.getKey() + " pilihan " + (i + 1) + " menyimpan " + choice.memoryKey() + "=" + choice.memoryValue());
                        }
                        return true;
                    }
                    require(args.length == 5 || args.length == 3 && args[2].equalsIgnoreCase("clear"), "/vnpc <id> memory <kunci> <nilai> <halaman> | clear | status");
                    var rules = new ArrayList<>(d.memory());
                    if (args.length == 3) rules.clear();
                    else { var rule = new NpcData.MemoryRule(args[2], args[3], args[4]); require(d.nodes().containsKey(rule.page()), "Buat halaman tujuan dengan page terlebih dahulu.");
                        rules.removeIf(old -> old.key().equals(rule.key()) && old.value().equals(rule.value())); rules.add(rule); }
                    service.edit(id, j -> j.add("memory", NpcStore.JSON.toJsonTree(rules)), false);
                }
                case "look" -> {
                    require(args.length == 2, "/vnpc <id> look"); Location look = p.getLocation(); var old = d.location();
                    var location = new NpcData.Point(old.world(), old.x(), old.y(), old.z(), look.getYaw(), look.getPitch());
                    service.edit(id, j -> { j.add("location", NpcStore.JSON.toJsonTree(location)); j.add("facing", NpcStore.JSON.toJsonTree(NpcData.Facing.zero())); }, false);
                }
                case "offset" -> {
                    require(args.length == 3 || args.length == 4, "/vnpc <id> offset <yaw> [pitch] — contoh offset 90 0");
                    var facing = new NpcData.Facing(Float.parseFloat(args[2]), args.length == 4 ? Float.parseFloat(args[3]) : 0);
                    service.edit(id, j -> j.add("facing", NpcStore.JSON.toJsonTree(facing)), false);
                }
                case "move", "stand", "queststart" -> {
                    require(args.length == 2, "/vnpc <id> " + action);
                    if (action.equals("queststart")) {
                        require(d.quest().kind() != NpcData.QuestKind.NONE, "Konfigurasi quest dulu.");
                        service.edit(id, j -> j.getAsJsonObject("quest").add("start", NpcStore.JSON.toJsonTree(NpcRenderer.point(p.getLocation()))), false);
                    } else service.edit(id, j -> j.add(action.equals("move") ? "location" : "stand", NpcStore.JSON.toJsonTree(NpcRenderer.point(p.getLocation()))), false);
                }
                case "height", "speed" -> {
                    require(args.length == 3, "/vnpc <id> " + action + " <angka>");
                    double number = Double.parseDouble(args[2]);
                    if (action.equals("speed")) require(number == Math.rint(number), "Speed harus angka bulat 1–8.");
                    service.edit(id, j -> j.addProperty(action.equals("height") ? "eyeHeight" : "speed", number), false);
                }
                case "skin" -> { require(args.length == 3, "/vnpc <id> skin <nama-pemain>"); service.skin(p, id, args[2]); return true; }
                case "model" -> {
                    require(args.length == 3, "/vnpc <id> model <id-modelengine>");
                    service.edit(id, j -> { j.addProperty("kind", "MODEL"); j.addProperty("model", args[2]); }, false);
                }
                case "page" -> {
                    require(args.length >= 4, "/vnpc <id> page <halaman> <teks>"); NpcData.id(args[2]);
                    List<NpcData.Choice> old = d.nodes().containsKey(args[2]) ? d.nodes().get(args[2]).choices() : List.of();
                    var node = new NpcData.Node(join(args, 3), old);
                    service.edit(id, j -> j.getAsJsonObject("nodes").add(args[2], NpcStore.JSON.toJsonTree(node)), false);
                }
                case "option" -> {
                    require(args.length >= 5, "/vnpc <id> option <halaman> <tujuan|END|QUEST|OPEN|DUNGEON_GLASS|DUNGEON_MINION|DUNGEON_BOSS|ACTION_id> <label>");
                    NpcData.Node old = d.nodes().get(args[2]); require(old != null, "Halaman belum ada; gunakan page dahulu.");
                    String target = Set.of("END", "QUEST", "OPEN").contains(args[3].toUpperCase(Locale.ROOT)) ? args[3].toUpperCase(Locale.ROOT) : args[3];
                    var choices = new ArrayList<>(old.choices()); choices.add(new NpcData.Choice(join(args, 4), target));
                    var node = new NpcData.Node(old.text(), choices);
                    service.edit(id, j -> j.getAsJsonObject("nodes").add(args[2], NpcStore.JSON.toJsonTree(node)), false);
                }
                case "clearoptions" -> {
                    require(args.length == 3, "/vnpc <id> clearoptions <halaman>"); var old = d.nodes().get(args[2]);
                    require(old != null, "Halaman tidak ditemukan.");
                    service.edit(id, j -> j.getAsJsonObject("nodes").add(args[2], NpcStore.JSON.toJsonTree(new NpcData.Node(old.text(), List.of()))), false);
                }
                case "root", "repeat" -> {
                    require(args.length >= 3, "/vnpc <id> " + action + " <teks/id>");
                    service.edit(id, j -> j.addProperty(action, join(args, 2)), false);
                }
                case "wand" -> {
                    require(args.length==3,"/vnpc <id> wand quest|trigger|gate");String mode=args[2].toLowerCase(Locale.ROOT);
                    require(Set.of("quest","trigger","gate").contains(mode),"Wand: quest, trigger, gate.");
                    var tool=com.bangzachery.vitae.vitaemanager.selection.SelectionService.require();
                    var kind=mode.equals("trigger")?com.bangzachery.vitae.vitaemanager.selection.SelectionService.Mode.WALK:
                        mode.equals("quest")?com.bangzachery.vitae.vitaemanager.selection.SelectionService.Mode.VOLUME:com.bangzachery.vitae.vitaemanager.selection.SelectionService.Mode.BLOCKS;
                    tool.arm(p,"vitae.admin.npc","NPC "+id+" • "+mode,kind,(admin,a,b)->{
                        var selected=com.bangzachery.vitae.vitaemanager.selection.SelectionService.area(a,b);
                        if(mode.equals("gate"))service.setupGate(admin,id,selected);
                        else if(mode.equals("trigger")){service.edit(id,j->j.add("trigger",NpcStore.JSON.toJsonTree(selected)),false);NpcService.message(admin,"Pemicu tersimpan. Aktifkan NPC setelah setup selesai.");}
                        else{selections.put(admin.getUniqueId(),new Corners(a,b));NpcService.message(admin,"Area quest dipilih. Berdiri di titik start dalam area, lalu /vnpc "+id+" quest farm <putaran> atau quest mob <jumlah> <jenis>.");}
                    });return true;
                }
                case "pos1", "pos2" -> {
                    require(args.length == 2, "/vnpc <id> " + action); var block = p.getTargetBlockExact(6);
                    Location point = block == null ? p.getLocation().getBlock().getLocation() : block.getLocation();
                    Corners old = selections.getOrDefault(p.getUniqueId(), new Corners(null, null));
                    selections.put(p.getUniqueId(), action.equals("pos1") ? new Corners(point, old.b) : new Corners(old.a, point));
                    NpcService.message(p, "Titik " + (action.equals("pos1") ? "A" : "B") + " tersimpan: " + point.getBlockX() + ", " + point.getBlockY() + ", " + point.getBlockZ()); return true;
                }
                case "trigger" -> {
                    require(args.length == 3 && Set.of("set", "off").contains(args[2]), "/vnpc <id> trigger set|off");
                    var trigger = args[2].equals("set") ? area(p) : null;
                    service.edit(id, j -> j.add("trigger", trigger == null ? JsonNull.INSTANCE : NpcStore.JSON.toJsonTree(trigger)), false);
                }
                case "gate" -> {
                    require(args.length == 3 || args.length == 4 && args[2].equalsIgnoreCase("autoclose"), "/vnpc <id> gate pos1|pos2|set|status|reset|off|cancel | gate autoclose <detik|off>");
                    switch (args[2].toLowerCase(Locale.ROOT)) {
                        case "pos1", "pos2" -> { service.gateSelect(p, id, args[2].equalsIgnoreCase("pos1")); return true; }
                        case "cancel" -> { service.gateCancel(p); NpcService.message(p, "Mode pemilihan block dibatalkan."); return true; }
                        case "autoclose" -> {
                            require(args.length == 4 && d.gate() != null, "/vnpc <id> gate autoclose <1–3600|off>; setup gate terlebih dahulu.");
                            int seconds = args[3].equalsIgnoreCase("off") ? 0 : Integer.parseInt(args[3]);
                            require(seconds > 0 || args[3].equalsIgnoreCase("off"), "Gunakan angka 1–3600 atau off.");
                            var gate = d.gate().withDelay(seconds, System.currentTimeMillis());
                            service.edit(id, j -> j.add("gate", NpcStore.JSON.toJsonTree(gate)), false);
                        }
                        case "set" -> service.gateSet(p, id);
                        case "reset" -> service.gateReset(id);
                        case "off" -> service.edit(id, json -> json.add("gate", JsonNull.INSTANCE), false);
                        case "status" -> {
                            NpcService.message(p, d.gate() == null ? "Gerbang belum diset." : "Gerbang " + d.gate().status() + "; " + d.gate().blocks().size() + " block; tutup otomatis " + (d.gate().timing().seconds() == 0 ? "OFF" : d.gate().timing().seconds() + " detik") + ". OPEN = dapat dilewati semua pemain.");
                            return true;
                        }
                        default -> throw new IllegalArgumentException("Gate: pos1, pos2, set, status, reset, off, cancel.");
                    }
                }
                case "quest" -> {
                    require(args.length >= 3, "/vnpc <id> quest farm <gelombang> | mob <jumlah> <COW> | off | cancel");
                    if (args[2].equalsIgnoreCase("cancel")) {
                        require(args.length == 3, "/vnpc <id> quest cancel"); service.cancelFarmSetup(id); return true;
                    }
                    NpcData.Quest q;
                    if (args[2].equalsIgnoreCase("off") && args.length == 3) q = NpcData.Quest.none();
                    else {
                        require(args.length >= 4, "Masukkan jumlah target quest."); int goal = Integer.parseInt(args[3]); WhisperArea selected = area(p);
                        if (args[2].equalsIgnoreCase("farm") && args.length == 4) {
                            service.setupFarm(p, id, selected, goal); return true;
                        }
                        else {
                            require(args[2].equalsIgnoreCase("mob") && args.length == 5, "/vnpc <id> quest mob <jumlah> <jenis-mob>");
                            var mob = NpcQuest.mobType(args[4]); q = new NpcData.Quest(NpcData.QuestKind.MOB, selected,
                                    NpcRenderer.point(p.getLocation()), goal, mob.name(), List.of(), NpcData.Reward.none());
                        }
                    }
                    service.edit(id, j -> j.add("quest", NpcStore.JSON.toJsonTree(q)), false);
                }
                case "reward" -> {
                    require(args.length >= 4 && Set.of("dialog", "quest").contains(args[2]), "/vnpc <id> reward dialog|quest viti <nominal> | items | none");
                    boolean quest = args[2].equals("quest");
                    if (args[3].equalsIgnoreCase("items") && args.length == 4) { service.items(p, id, quest); return true; }
                    NpcData.Reward reward;
                    if (args[3].equalsIgnoreCase("none") && args.length == 4) reward = NpcData.Reward.none();
                    else { require(args[3].equalsIgnoreCase("viti") && args.length == 5, "/vnpc <id> reward dialog|quest viti <nominal>"); reward = new NpcData.Reward(VitiAmount.positive(VitiAmount.parse(args[4])), List.of()); }
                    require(!quest || d.quest().kind() != NpcData.QuestKind.NONE, "Konfigurasi quest dahulu.");
                    service.edit(id, j -> { if (quest) j.getAsJsonObject("quest").add("reward", NpcStore.JSON.toJsonTree(reward)); else j.add("gift", NpcStore.JSON.toJsonTree(reward)); }, false);
                }
                case "on", "off" -> { require(args.length == 2, "/vnpc <id> " + action); service.edit(id, j -> {}, action.equals("on")); }
                case "preview" -> { require(args.length == 2, "/vnpc <id> preview"); service.preview(p, id); return true; }
                case "reset" -> {
                    require(args.length == 3, "/vnpc <id> reset <nama-tersimpan|UUID>"); UUID player;
                    try { player = UUID.fromString(args[2]); }
                    catch (IllegalArgumentException e) { OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(args[2]); require(cached != null, "Pemain belum tersimpan. Gunakan UUID."); player = cached.getUniqueId(); }
                    service.reset(player, id);
                }
                default -> throw new IllegalArgumentException("Pengaturan tidak dikenal: " + action + ". /vnpc help");
            }
            p.sendMessage(Component.text("[Vitae NPC] ", NamedTextColor.GOLD)
                    .append(Component.text("Tersimpan.", NamedTextColor.GREEN)));
            if (!Set.of("on", "off", "reset", "delete").contains(action)) {
                p.sendMessage(Component.text("NPC menjadi draft OFF selama setup. Aktifkan setelah semua siap.", NamedTextColor.GRAY));
                String nextTopic = (action.equals("quest") && args[2].equalsIgnoreCase("mob"))
                        || (action.equals("queststart") && d.quest().kind() == NpcData.QuestKind.MOB) ? "mob" : topic(action);
                tip(p, "/vnpc " + id + " help " + nextTopic, "Petunjuk tahap berikutnya");
                tip(p, "/vnpc " + id + " on", "Aktifkan NPC setelah setup selesai");
            }
        } catch (Exception e) {
            Throwable cause = e; while (cause.getCause() != null) cause = cause.getCause();
            sender.sendMessage(Component.empty());
            sender.sendMessage(Component.text("[Vitae NPC] ", NamedTextColor.GOLD)
                    .append(Component.text(Objects.requireNonNullElse(cause.getMessage(), "Setup tidak valid."), NamedTextColor.RED)));
            String usage = args.length >= 2 && service.current().npcs().containsKey(args[0])
                    ? "/vnpc " + args[0] + " help " + topic(args[1].toLowerCase(Locale.ROOT)) : "/vnpc help";
            tip(sender, usage, "Klik untuk melihat contoh command");
            sender.sendMessage(Component.empty());
        }
        return true;
    }
    private WhisperArea area(Player p) {
        Corners c = selections.get(p.getUniqueId());
        require(c != null && c.a != null && c.b != null, "Tandai pos1 dan pos2 terlebih dahulu.");
        require(c.a.getWorld().equals(c.b.getWorld()), "Kedua titik harus dalam dunia yang sama.");
        return WhisperArea.between(c.a.getWorld().getUID(), c.a.getBlockX(), c.a.getBlockY(), c.a.getBlockZ(), c.b.getBlockX(), c.b.getBlockY(), c.b.getBlockZ());
    }
    private static void require(boolean valid, String help) { if (!valid) throw new IllegalArgumentException(help); }
    private static String join(String[] a, int start) { return String.join(" ", Arrays.copyOfRange(a, start, a.length)); }
    private static void created(CommandSender sender, String id, String name) {
        header(sender, "NPC " + name + " dibuat • " + id);
        note(sender, "Status draft OFF. Lokasi NPC dua blok di depanmu; posisi bicara memakai tempatmu berdiri.");
        note(sender, "Pilih tujuan NPC. Klik command biru untuk menaruhnya di kotak chat, lalu Enter.");
        purposes(sender, id);
        tip(sender, "/vnpc " + id + " help tampilan", "Atur skin/model dan lokasi");
        note(sender, "Mulai dari dialog, tambahkan hadiah/quest bila perlu, lalu preview dan on.");
        footer(sender);
    }
    private static void header(CommandSender sender, String title) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("──── Vitae NPC • " + title + " ────", NamedTextColor.GOLD, TextDecoration.BOLD));
    }
    private static void footer(CommandSender sender) {
        sender.sendMessage(Component.text("────────────────────────────", NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.empty());
    }
    private static void note(CommandSender sender, String text) { sender.sendMessage(Component.text(text, NamedTextColor.GRAY)); }
    private static void tip(CommandSender sender, String command, String explanation) {
        sender.sendMessage(Component.text(command, NamedTextColor.AQUA)
                .clickEvent(ClickEvent.suggestCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text("Klik untuk menaruh command di kotak chat.", NamedTextColor.YELLOW)))
                .append(Component.text(" — " + explanation, NamedTextColor.GRAY).clickEvent(null).hoverEvent(null)));
    }
    private static void purposes(CommandSender sender, String id) {
        String prefix = id.equals("<id>") ? "/vnpc help " : "/vnpc " + id + " help ";
        tip(sender, prefix + "dialog", "Dialog cerita dan pilihan bercabang");
        tip(sender, prefix + "hadiah", "Hadiah gratis sekali per pemain");
        tip(sender, prefix + "ladang", "Quest memanen ladang");
        tip(sender, prefix + "mob", "Quest membunuh mob");
    }
    private static String topic(String action) {
        return switch (action) {
            case "page", "option", "clearoptions", "root", "speed" -> "dialog";
            case "memory", "remember" -> "ingatan";
            case "reward", "repeat" -> "hadiah";
            case "wand", "pos1", "pos2", "trigger" -> "pemicu";
            case "gate" -> "gerbang";
            case "quest", "queststart" -> "ladang";
            case "skin", "model", "height", "move", "stand", "look", "offset", "name", "nametag", "track" -> "tampilan";
            default -> "kelola";
        };
    }
    private static void help(CommandSender sender, String id, String requested) {
        String topic = requested.toLowerCase(Locale.ROOT), cmd = "/vnpc " + id + " ";
        require(topic.isEmpty() || TOPICS.contains(topic), "Topik bantuan: " + String.join(", ", TOPICS) + ".");
        header(sender, topic.isEmpty() ? "Bantuan setup" : "Bantuan " + topic + " • " + id);
        switch (topic) {
            case "dialog" -> {
                tip(sender, cmd + "page start Halo, ada yang bisa kubantu?", "Tulis dialog awal");
                tip(sender, cmd + "page cerita Aku menyimpan sebuah rahasia.", "Buat halaman lanjutan");
                tip(sender, cmd + "option start cerita Aku ingin mendengarkan.", "Pilihan menuju halaman cerita");
                tip(sender, cmd + "option start END Keluar...", "Pilihan mengakhiri percakapan");
                tip(sender, cmd + "option cerita END Terima kasih.", "Akhiri halaman lanjutan");
                tip(sender, cmd + "root start", "Pilih halaman pertama");
                tip(sender, cmd + "speed 1", "Kecepatan mengetik 1–8 karakter per pembaruan");
                tip(sender, cmd + "clearoptions start", "Hapus pilihan halaman start sebelum menyusun ulang");
                note(sender, "page memperbarui teks, option menambah pilihan. Halaman tanpa pilihan otomatis punya Keluar...");
                note(sender, "Dungeon bersama: target DUNGEON_GLASS, DUNGEON_MINION, atau DUNGEON_BOSS. Hubungkan NPC melalui /vdungeon <id> npc <tahap> <id-npc>.");
                note(sender, "Aksi teleport/spawn terpisah: target ACTION_<id>. Setup melalui /vdungeon help aksi.");
            }
            case "hadiah" -> {
                tip(sender, cmd + "page start Terima kasih sudah mendengarkan ceritaku.", "Tulis dialog sebelum hadiah");
                tip(sender, cmd + "reward dialog viti 1000", "Hadiah Viti sekali per pemain");
                tip(sender, cmd + "reward dialog items", "Buka chest; letakkan item contoh lalu tutup");
                tip(sender, cmd + "reward dialog none", "Matikan hadiah dialog");
                tip(sender, cmd + "repeat Hadiahku sudah kuberikan. Hus hus!", "Dialog setelah hadiah sudah diterima");
                note(sender, "Pilih Viti atau item. Item contoh admin dikembalikan setelah GUI ditutup.");
                note(sender, "Hadiah dikirim saat pemain menyelesaikan dialog melalui END, bukan saat membatalkan.");
            }
            case "ladang" -> {
                note(sender, "Siapkan tanaman di atas farmland. Tandai area, lalu berdiri di titik awal player dalam area.");
                tip(sender, cmd + "wand quest", "Ambil alat untuk memilih area ladang dengan dua klik");
                tip(sender, cmd + "pos2", "Lihat blok sudut B yang mencakup tanaman");
                tip(sender, cmd + "quest farm 5", "Baca tanaman bertahap; lima putaran panen; tunggu pesan Ladang tersimpan");
                tip(sender, cmd + "quest cancel", "Batalkan pemindaian; konfigurasi lama tetap tersimpan");
                tip(sender, cmd + "queststart", "Simpan tempat awal player dalam area");
                tip(sender, cmd + "reward quest viti 1000", "Hadiah ladang wajib Viti");
                tip(sender, cmd + "page start Bantu aku memanen ladang ini.", "Tulis tawaran quest");
                tip(sender, cmd + "option start QUEST Terima quest.", "Pilihan menerima pekerjaan");
                tip(sender, cmd + "option start END Keluar...", "Pilihan menolak");
                note(sender, "Pemain wajib punya iron hoe. Pos1/pos2 adalah blok yang dilihat, maksimal enam blok.");
                note(sender, "Tidak ada batas 512 tanaman atau volume 8192 blok. Tetap pakai area A–B yang valid.");
                note(sender, "Rescan farm mempertahankan hadiah Viti lama. Sesudah Ladang tersimpan, atur hadiah lalu on.");
            }
            case "mob" -> {
                tip(sender, cmd + "wand quest", "Pilih area quest dengan dua klik");
                note(sender, "Tandai area A–B yang aman, lalu berdiri di titik awal player dalam area.");
                tip(sender, cmd + "pos1", "Tandai sudut A");
                tip(sender, cmd + "pos2", "Tandai sudut B");
                tip(sender, cmd + "quest mob 5 COW", "Lima mob; ganti COW dengan jenis mob lain");
                tip(sender, cmd + "queststart", "Simpan tempat awal player dalam area");
                tip(sender, cmd + "reward quest viti 1000", "Atur hadiah quest; items juga tersedia");
                tip(sender, cmd + "page start Bantu aku mengalahkan lima mob.", "Tulis tawaran quest");
                tip(sender, cmd + "option start QUEST Terima quest.", "Terima pekerjaan");
                tip(sender, cmd + "option start END Keluar...", "Tolak pekerjaan");
                note(sender, "Pemain wajib punya iron sword dan memakainya di tangan utama saat membunuh mob quest.");
            }
            case "pemicu" -> {
                tip(sender, cmd + "wand trigger", "Klik dua blok lantai; pemicu dialog otomatis tersimpan");
                note(sender, "Pilih dua sudut lorong yang mencakup ketinggian kaki player, dalam dunia NPC.");
                tip(sender, cmd + "pos1", "Tandai sudut A; mengambil blok yang dilihat");
                tip(sender, cmd + "pos2", "Tandai sudut B");
                tip(sender, cmd + "trigger set", "Jadikan seleksi sebagai pemicu dialog wajib");
                tip(sender, cmd + "trigger off", "Hapus pemicu area");
                tip(sender, cmd + "stand", "Berdiri di depan NPC untuk menyimpan posisi bicara");
                note(sender, "Letakkan stand di luar pemicu. Shift membatalkan; percakapan dipicu lagi sampai selesai.");
            }
            case "gerbang" -> {
                note(sender, "Bangun dinding solid biasa; NPC dan posisi bicara berada di luarnya. Gerbang terbuka global untuk semua pemain.");
                tip(sender, cmd + "wand gate", "Dua klik memilih dinding; pemindaian/simpan otomatis");
                tip(sender, cmd + "gate pos1", "Aktifkan pemilihan A, lalu pukul block sudut dinding; block tidak pecah");
                tip(sender, cmd + "gate pos2", "Aktifkan pemilihan B, lalu pukul sudut diagonal seberangnya");
                tip(sender, cmd + "gate set", "Simpan dinding tanpa batas jumlah blok; pemindaian berjalan bertahap");
                tip(sender, cmd + "page start Kamu boleh masuk. Akan kubukakan gerbangnya.", "Tulis dialog penjaga");
                tip(sender, cmd + "clearoptions start", "Hapus pilihan lama sebelum menyusun ulang");
                tip(sender, cmd + "option start OPEN Bukakan gerbangnya.", "Pilihan membuka gerbang dan menyelesaikan dialog");
                tip(sender, cmd + "option start END Nanti saja.", "Menutup dialog tanpa membuka gerbang");
                tip(sender, cmd + "repeat Gerbang sudah terbuka. Silakan lewat.", "Dialog saat pemain yang sama datang lagi");
                tip(sender, cmd + "gate autoclose 30", "Tutup otomatis 30 detik setelah terbuka; 1–3600 detik");
                tip(sender, cmd + "gate autoclose off", "Biarkan terbuka sampai reset manual");
                tip(sender, cmd + "gate status", "Lihat CLOSED/OPENING/OPEN/CLOSING dan durasi");
                note(sender, "Peringatan lima detik terakhir. Pemain di posisi dinding menunda penutupan. Klik lain tidak mengulang timer.");
                note(sender, "Saat tertutup, pemain lama boleh membuka lagi; hadiah dialog tetap sekali per pemain.");
                tip(sender, cmd + "gate reset", "Pulihkan dinding dari snapshot untuk tes ulang; NPC menjadi OFF");
                tip(sender, cmd + "gate cancel", "Batalkan mode pukul untuk memilih titik");
                note(sender, "Klik pertama mengklaim gerbang; klik bersamaan tidak memulai animasi tambahan. Preview tidak menghancurkan dinding.");
                note(sender, "Jauhkan pemain dari dinding sebelum reset. gate off melepas pengaturan tanpa membangun kembali dinding.");
            }
            case "tampilan" -> {
                tip(sender, cmd + "name &6Penjaga &eGerbang", "Ganti nama dan warna; nama biasa juga boleh");
                tip(sender, cmd + "nametag off", "Sembunyikan nama di atas kepala; on menampilkannya lagi");
                tip(sender, cmd + "track on", "NPC menoleh saat dialog; off mempertahankan arah awal");
                note(sender, "Kode warna &0–&f; &l tebal, &o miring, &r reset. Nama di dialog memakai warna yang sama.");
                tip(sender, cmd + "skin NamaPemain", "Pakai skin akun premium atau pemain online");
                tip(sender, cmd + "model id_model", "Pakai model custom; perlu ModelEngine 4");
                tip(sender, cmd + "height 1.62", "Tinggi kepala untuk arah kamera; rentang 0.2–8");
                tip(sender, cmd + "move", "Simpan lokasi NPC di tempat kamu berdiri");
                tip(sender, cmd + "stand", "Simpan posisi bicara player di tempat kamu berdiri");
                tip(sender, cmd + "look", "Salin arah pandangmu tanpa memindahkan NPC; offset direset");
                tip(sender, cmd + "offset 90 0", "Geser arah 90 derajat dari arah dasar; berlaku untuk skin/model");
                tip(sender, cmd + "offset 0 0", "Reset offset arah");
                note(sender, "Yaw memutar kiri/kanan (-360–360). Pitch mengatur kepala atas/bawah (-90–90); positif melihat ke bawah.");
                note(sender, "Pilih skin manusia atau model. Hindari posisi berdiri di dalam blok.");
            }
            case "ingatan" -> {
                tip(sender, cmd + "page start Maukah kamu membantuku?", "Buat pertanyaan awal");
                tip(sender, cmd + "clearoptions start", "Susun pilihan dari awal");
                tip(sender, cmd + "option start END Aku akan membantu.", "Pilihan pertama");
                tip(sender, cmd + "option start END Aku menolak.", "Pilihan kedua");
                tip(sender, cmd + "remember start 1 sikap membantu", "Pilihan pertama menyimpan sikap=membantu");
                tip(sender, cmd + "remember start 2 sikap menolak", "Pilihan kedua menyimpan sikap=menolak");
                tip(sender, cmd + "page ramah Terima kasih sudah bersedia membantuku.", "Dialog untuk pemain yang membantu");
                tip(sender, cmd + "page dingin Kamu pernah menolakku. Ada perlu apa lagi?", "Dialog untuk pemain yang menolak");
                tip(sender, cmd + "memory sikap membantu ramah", "Pertemuan berikutnya dimulai di halaman ramah");
                tip(sender, cmd + "memory sikap menolak dingin", "Pertemuan berikutnya dimulai di halaman dingin");
                tip(sender, cmd + "memory status", "Cek aturan dan pilihan yang menyimpan ingatan");
                note(sender, "Nomor pilihan mulai dari 1. Ingatan tersimpan setelah konfirmasi pilihan; preview tidak menyimpan. Hadiah tetap sekali.");
                note(sender, "memory clear menghapus aturan, bukan ingatan pemain. remember start 1 off melepas penyimpanan pilihan.");
            }
            case "kelola" -> {
                tip(sender, "/vnpc list", "Lihat seluruh ID NPC");
                tip(sender, "/vnpc status", "Lihat apakah modul NPC aktif atau penyebab startup gagal");
                tip(sender, "/vnpc reload", "Muat ulang NPC setelah masalah setup diperbaiki");
                tip(sender, cmd + "on", "Aktifkan setelah semua pengaturan valid");
                tip(sender, cmd + "off", "Nonaktifkan sementara");
                tip(sender, cmd + "quest off", "Hapus quest NPC");
                tip(sender, cmd + "reset NamaPemain", "Reset progres NPC dan kuota quest global pemain");
                tip(sender, cmd + "delete", "Hapus NPC ini");
                note(sender, "Player: /quest status untuk progres, /quest quit untuk membatalkan pekerjaan.");
            }
            default -> {
                tip(sender, "/vnpc create <id> <nama>", "Buat NPC baru; contoh /vnpc create mira Mira");
                purposes(sender, id);
                tip(sender, id.equals("<id>") ? "/vnpc help tampilan" : cmd + "help tampilan", "Skin/model dan posisi NPC");
                tip(sender, id.equals("<id>") ? "/vnpc help pemicu" : cmd + "help pemicu", "Area pemicu dialog wajib");
                tip(sender, id.equals("<id>") ? "/vnpc help gerbang" : cmd + "help gerbang", "Dialog yang membuka dinding dengan animasi");
                tip(sender, id.equals("<id>") ? "/vnpc help kelola" : cmd + "help kelola", "Aktif/nonaktif, reset, hapus, dan daftar NPC");
                note(sender, "Bantuan untuk NPC tertentu: /vnpc <id> help <topik>. Ganti <id> dengan ID NPC milikmu.");
            }
        }
        if (!topic.isEmpty() && !topic.equals("kelola")) {
            sender.sendMessage(Component.empty());
            tip(sender, cmd + "preview", "Coba dialog sendiri; hadiah/quest tidak dijalankan");
            tip(sender, cmd + "on", "Aktifkan setelah seluruh setup selesai");
            note(sender, "Perubahan setup membuat NPC menjadi draft OFF. Klik kiri/kanan membuka dialog; scroll memilih; Shift batal.");
        }
        footer(sender);
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command c, String alias, String[] a) {
        List<String> values = new ArrayList<>();
        if (c.getName().equalsIgnoreCase("quest")) values.addAll(List.of("quit", "status"));
        else if (!sender.hasPermission("vitae.admin.npc")) return List.of();
        else if (a.length > 1 && !Set.of("help", "create", "list").contains(a[0].toLowerCase(Locale.ROOT))
                && !service.current().npcs().containsKey(a[0])) return List.of();
        else if (a.length == 1) { values.addAll(List.of("help", "create", "list", "status", "reload")); values.addAll(service.current().npcs().keySet()); }
        else if (a.length == 2 && service.current().npcs().containsKey(a[0])) values.addAll(ACTIONS);
        else if (a.length == 2 && a[0].equalsIgnoreCase("help")) values.addAll(TOPICS);
        else if (a.length == 3) switch (a[1].toLowerCase(Locale.ROOT)) {
            case "help" -> values.addAll(TOPICS);
            case "trigger" -> values.addAll(List.of("set", "off"));
            case "nametag", "track" -> values.addAll(List.of("on", "off"));
            case "memory" -> values.addAll(List.of("clear", "status"));
            case "remember" -> values.addAll(service.get(a[0]).nodes().keySet());
            case "wand" -> values.addAll(List.of("quest", "trigger", "gate"));
            case "gate" -> values.addAll(List.of("pos1", "pos2", "set", "status", "reset", "off", "cancel", "autoclose"));
            case "quest" -> values.addAll(List.of("farm", "mob", "off", "cancel"));
            case "reward" -> values.addAll(List.of("dialog", "quest"));
            case "page", "option", "root", "clearoptions" -> values.addAll(service.get(a[0]).nodes().keySet());
            default -> { }
        }
        else if (a.length == 4 && a[1].equalsIgnoreCase("gate") && a[2].equalsIgnoreCase("autoclose")) values.addAll(List.of("10", "30", "60", "off"));
        else if (a.length == 4 && a[1].equalsIgnoreCase("remember")) values.addAll(List.of("1", "2", "3", "4"));
        else if (a.length == 4 && a[1].equalsIgnoreCase("reward")) values.addAll(List.of("viti", "items", "none"));
        else if (a.length == 4 && a[1].equalsIgnoreCase("option")) { values.addAll(List.of("END", "QUEST", "OPEN", "DUNGEON_GLASS", "DUNGEON_MINION", "DUNGEON_BOSS", "DUNGEON_REWARD", "ACTION_")); values.addAll(service.get(a[0]).nodes().keySet()); }
        else if (a.length == 5 && a[1].equalsIgnoreCase("quest") && a[2].equalsIgnoreCase("mob")) values.addAll(List.of("COW", "PIG", "SHEEP", "CHICKEN", "ZOMBIE", "SKELETON"));
        String last = a.length == 0 ? "" : a[a.length - 1].toLowerCase(Locale.ROOT);
        return values.stream().filter(v -> v.toLowerCase(Locale.ROOT).startsWith(last)).sorted().toList();
    }
}
