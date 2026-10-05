package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.economy.VitiAmount;
import com.bangzachery.vitae.vitaemanager.whisper.WhisperArea;
import com.google.gson.*;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public final class NpcCommand implements CommandExecutor, TabCompleter {
    private record Corners(Location a, Location b) {}
    private final NpcService service;
    private final Map<UUID, Corners> selections = new HashMap<>();
    private static final List<String> ACTIONS = List.of("move", "stand", "height", "skin", "model", "page", "option",
            "clearoptions", "root", "repeat", "speed", "pos1", "pos2", "trigger", "quest", "queststart", "reward", "on", "off", "preview", "reset", "delete");
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
        if (!sender.hasPermission("vitae.admin.npc")) { sender.sendMessage(Component.text("Perlu izin vitae.admin.npc.")); return true; }
        try {
            if (args.length == 0 || args[0].equalsIgnoreCase("help")) { help(sender); return true; }
            if (args[0].equalsIgnoreCase("list") && args.length == 1) {
                sender.sendMessage(Component.text("NPC: " + (service.current().npcs().isEmpty() ? "belum ada" : String.join(", ", new TreeSet<>(service.current().npcs().keySet()))))); return true;
            }
            if (!(sender instanceof Player p)) throw new IllegalArgumentException("Setup lokasi dilakukan sebagai pemain di dalam server.");
            if (args[0].equalsIgnoreCase("create")) {
                require(args.length >= 3, "/vnpc create <id> <nama>"); service.create(p, args[1], join(args, 2));
                NpcService.message(p, "NPC draft dibuat; NPC sebelumnya tetap tersimpan. /vnpc " + args[1] + " on setelah setup."); return true;
            }
            require(args.length >= 2, "/vnpc <id> <pengaturan> atau /vnpc help");
            String id = args[0], action = args[1].toLowerCase(Locale.ROOT); var d = service.get(id);
            switch (action) {
                case "delete" -> { require(args.length == 2, "/vnpc <id> delete"); service.delete(id); }
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
                    require(args.length >= 5, "/vnpc <id> option <halaman> <tujuan|END|QUEST> <label>");
                    NpcData.Node old = d.nodes().get(args[2]); require(old != null, "Halaman belum ada; gunakan page dahulu.");
                    String target = Set.of("END", "QUEST").contains(args[3].toUpperCase(Locale.ROOT)) ? args[3].toUpperCase(Locale.ROOT) : args[3];
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
                case "quest" -> {
                    require(args.length >= 3, "/vnpc <id> quest farm <gelombang> | mob <jumlah> <COW> | off");
                    NpcData.Quest q;
                    if (args[2].equalsIgnoreCase("off") && args.length == 3) q = NpcData.Quest.none();
                    else {
                        require(args.length >= 4, "Masukkan jumlah target quest."); int goal = Integer.parseInt(args[3]); WhisperArea selected = area(p);
                        if (args[2].equalsIgnoreCase("farm") && args.length == 4) q = new NpcData.Quest(NpcData.QuestKind.FARM, selected,
                                NpcRenderer.point(p.getLocation()), goal, "COW", NpcQuest.scan(selected), NpcData.Reward.none());
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
            NpcService.message(p, "Tersimpan. " + (Set.of("on", "off", "reset", "delete").contains(action) ? "" : "Perubahan setup menonaktifkan draft; jalankan /vnpc " + id + " on setelah selesai."));
        } catch (Exception e) {
            Throwable cause = e; while (cause.getCause() != null) cause = cause.getCause();
            sender.sendMessage(Component.text("[Vitae NPC] " + Objects.requireNonNullElse(cause.getMessage(), "Setup tidak valid.") + " Bantuan: /vnpc help"));
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
    private static void help(CommandSender sender) {
        for (String text : List.of("/vnpc create <id> <nama> • /vnpc list",
                "/vnpc <id> move|stand|on|off|preview|delete",
                "/vnpc <id> skin <nama> | model <model-id> | height <0.2–8> | speed <1–8>",
                "/vnpc <id> page <halaman> <teks> | root <halaman> | repeat <teks>",
                "/vnpc <id> option <halaman> <tujuan|END|QUEST> <label>",
                "/vnpc <id> clearoptions <halaman>",
                "/vnpc <id> pos1|pos2 • trigger set|off",
                "/vnpc <id> quest farm <gelombang> | quest mob <jumlah> <COW> | quest off",
                "/vnpc <id> queststart (berdiri dalam seleksi terlebih dahulu)",
                "/vnpc <id> reward dialog|quest viti <nominal> | items | none",
                "/vnpc <id> reset <nama|UUID> (progres NPC dan kuota quest global)",
                "Player: /quest status • /quest quit. Klik kiri NPC untuk mulai; scroll dan klik untuk memilih.",
                "Siapkan tanaman dahulu untuk quest farm. Aktifkan dengan on setelah seluruh setup selesai.")) sender.sendMessage(Component.text(text));
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command c, String alias, String[] a) {
        List<String> values = new ArrayList<>();
        if (c.getName().equalsIgnoreCase("quest")) values.addAll(List.of("quit", "status"));
        else if (!sender.hasPermission("vitae.admin.npc")) return List.of();
        else if (a.length == 1) { values.addAll(List.of("help", "create", "list")); values.addAll(service.current().npcs().keySet()); }
        else if (a.length == 2 && service.current().npcs().containsKey(a[0])) values.addAll(ACTIONS);
        else if (a.length == 3) switch (a[1].toLowerCase(Locale.ROOT)) {
            case "trigger" -> values.addAll(List.of("set", "off"));
            case "quest" -> values.addAll(List.of("farm", "mob", "off"));
            case "reward" -> values.addAll(List.of("dialog", "quest"));
            case "page", "option", "root", "clearoptions" -> values.addAll(service.get(a[0]).nodes().keySet());
            default -> { }
        }
        else if (a.length == 4 && a[1].equalsIgnoreCase("reward")) values.addAll(List.of("viti", "items", "none"));
        else if (a.length == 4 && a[1].equalsIgnoreCase("option")) { values.addAll(List.of("END", "QUEST")); values.addAll(service.get(a[0]).nodes().keySet()); }
        else if (a.length == 5 && a[1].equalsIgnoreCase("quest") && a[2].equalsIgnoreCase("mob")) values.addAll(List.of("COW", "PIG", "SHEEP", "CHICKEN", "ZOMBIE", "SKELETON"));
        String last = a.length == 0 ? "" : a[a.length - 1].toLowerCase(Locale.ROOT);
        return values.stream().filter(v -> v.toLowerCase(Locale.ROOT).startsWith(last)).sorted().toList();
    }
}
