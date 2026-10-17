# Vitaemanager — update quest farm besar

Update ini dipasang setelah `update-npc-cerita-evo.md`. Semua file Java yang berubah tersedia lengkap di dalam installer pada bagian bawah dokumen ini.

## Perubahan

- Batas 512 tanaman dihapus. Ladang wajib memiliki minimal satu tanaman; tidak ada kuota jumlah tanaman.
- Batas volume seleksi farm 8.192 blok dihapus. Area Revenant A `-6323, 67, -4180` dan B `-6364, 72, -4235` memiliki volume 14.112 blok dan dapat dipindai.
- Tanaman yang didukung tetap carrot, potato, wheat, dan beetroot di atas farmland. Snapshot otomatis menggunakan usia matang.
- Pembacaan area dan pemasangan ulang tanaman diproses bertahap di thread server, dengan anggaran pekerjaan yang dibagi seluruh ladang.
- Admin mendapat progres pemindaian di actionbar. Data quest baru disimpan setelah semua blok selesai dibaca. Pemindaian gagal atau dibatalkan mempertahankan konfigurasi lama.
- Hadiah Viti lama dipertahankan ketika menjalankan ulang `quest farm`. Hadiah boleh diganti melalui command biasa.
- Panen memakai indeks posisi tanaman. Seluruh tanaman harus dipanen untuk menyelesaikan satu putaran; hadiah diberikan setelah semua putaran selesai.
- Ladang tetap terkunci selama disiapkan, dipakai, atau dipulihkan setelah batal/selesai. Pemain lain baru boleh masuk setelah seluruh snapshot siap.
- Tanaman dipulihkan lagi ketika NPC aktif dimuat setelah restart. Quest pemain yang terputus tidak dianggap selesai dan tidak memberikan hadiah.
- Batas file NPC 16 MB dihapus supaya snapshot besar yang sudah tersimpan juga dapat dibaca kembali setelah restart. Penulisan tetap melalui file sementara dan pergantian atomik; data rusak tetap ditolak.

Ini menghapus batas jumlah tanaman dan batas volume 8.192 khusus farm. Aturan area bersama yang sudah ada, maksimal **256 × 512 × 256 blok**, tetap berlaku untuk NPC/bisikan. Jumlah putaran tetap 1–50; bukan batas jumlah tanaman. Semakin banyak tanaman, semakin lama proses pembacaan, persiapan, dan pekerjaan pemain.

Main plugin, nama folder data, dependency, dan integrasi modul lain tidak diganti. Installer hanya mengubah source yang tercantum di bawah. Jangan menghapus folder data server.

## Pasang update di IntelliJ

1. Unduh `update-npc-farm-besar.md` dan letakkan sejajar dengan `gradlew.bat` dalam folder project Vitaemanager.
2. Buka PowerShell di folder tersebut.
3. Jalankan:

```powershell
$m=Get-Content -LiteralPath .\update-npc-farm-besar.md -Raw -Encoding UTF8; $i=[regex]::Match($m,'(?s)<!-- VITAE_INSTALLER_BEGIN -->\r?\n```powershell\r?\n(.*?)\r?\n```\r?\n<!-- VITAE_INSTALLER_END -->'); if(!$i.Success){throw 'Blok installer tidak ditemukan'}; & ([scriptblock]::Create($i.Groups[1].Value)) -ProjectPath (Get-Location).Path
```

Installer memeriksa seluruh source terlebih dahulu. Jika menemukan file berbeda dari versi sebelumnya, ia berhenti sebelum menulis agar perubahan manualmu tidak tertimpa. File lama dicadangkan dalam `vitae-npc-farm-backup-...`. Pengulangan installer pada versi yang sudah terpasang tidak membuat perubahan baru.

Untuk pemeriksaan tanpa memasang, tambahkan `-CheckOnly` pada akhir command di atas.

4. Build:

```powershell
.\gradlew.bat clean build
```

5. Setelah build berhasil, matikan server, ganti JAR lama dengan JAR baru dari `build/libs`, biarkan folder data `plugins/vitaemanager` tetap ada, lalu nyalakan server. Jangan menjalankan `/reload` server.

## Lanjutkan setup petani Revenant yang sudah dibuat

Gunakan ID `petani_revenant`. Kalau ID NPC-mu berbeda, ganti ID di semua command.

1. Pastikan tidak ada pemain sedang menggunakan NPC atau ladangnya. Jika ada quest berjalan, pemain tersebut bisa membatalkan dengan `/quest quit`, lalu tunggu ladang selesai dipulihkan.
2. Nonaktifkan NPC sementara:

```text
/vnpc petani_revenant off
```

3. Tandai sudut A dan B seperti biasa. Lihat blok sudut dalam jarak enam blok, lalu jalankan:

```text
/vnpc petani_revenant pos1
```

Sudut A yang kamu rencanakan: `-6323, 67, -4180`.

Kemudian lihat blok sudut B:

```text
/vnpc petani_revenant pos2
```

Sudut B: `-6364, 72, -4235`. Perhatikan koordinat yang ditampilkan chat; semua tanaman harus masuk area ini. Pos1/pos2 tidak dipukul. Kedua titik harus berada di dunia yang sama.

4. Berdiri di tempat kosong di dalam area ladang, sebagai posisi awal pemain. Simpan lima putaran:

```text
/vnpc petani_revenant quest farm 5
```

**Tunggu pesan `Ladang tersimpan: ... tanaman, 5 putaran` sebelum menjalankan command berikutnya.** Saat pemindaian berlangsung, konfigurasi lama masih dipertahankan dan perubahan NPC sementara diblokir.

Jika salah seleksi, batalkan pemindaian yang masih berjalan:

```text
/vnpc petani_revenant quest cancel
```

5. Sesudah pemindaian berhasil, atur hadiah seluruh quest:

```text
/vnpc petani_revenant reward quest viti 10000
```

Posisi awal tadi sudah tersimpan. Jika ingin menggantinya, berdiri di posisi baru di dalam area, lalu jalankan `/vnpc petani_revenant queststart`.

6. Jika dialog belum dibuat:

```text
/vnpc petani_revenant page start Ladang Revenant membutuhkan bantuanmu. Panen seluruh tanaman sebanyak lima putaran. Setelah selesai, kamu mendapat 10.000 Viti. Bawa iron hoe terlebih dahulu.
/vnpc petani_revenant clearoptions start
/vnpc petani_revenant option start QUEST Aku siap membantu Revenant.
/vnpc petani_revenant option start END Aku kembali nanti.
/vnpc petani_revenant root start
```

`clearoptions start` mengganti pilihan lama pada halaman start. Jika dialogmu sudah benar, langkah dialog ini boleh dilewati.

7. Aktifkan:

```text
/vnpc petani_revenant on
```

Saat pertama kali diaktifkan, tanaman disiapkan bertahap. Jika NPC mengatakan ladang sedang disiapkan, tunggu sebentar lalu coba lagi.

## Cara kerja untuk pemain

- Iron hoe harus ada di inventory atau offhand. Enchant dan sisa durability bebas.
- Klik NPC, tunggu tulisan selesai, pilih tawaran QUEST dengan scroll dan konfirmasi klik.
- Pemain dipindahkan ke posisi awal; tunggu pesan `Ladang siap` sebelum mulai memanen.
- Semua tanaman yang terdaftar wajib dipanen untuk satu putaran. Tanaman baru dipasang ulang setelah jeda, lalu putaran berikutnya dimulai ketika seluruh ladang siap.
- Jika ada 2.352 tanaman dan lima putaran, pemain harus memanen 2.352 tanaman di setiap putaran, total 11.760 panen. Selesaikan dalam batas waktu quest 30 menit.
- Setelah 5/5, pemain kembali ke NPC dan menerima **10.000 Viti sekali untuk penyelesaian quest itu**.
- Panen tidak menghasilkan item di inventory, ground item, atau XP. Farmland tetap dilindungi dari injakan.
- Batal dengan `/quest quit`, keluar area, mati, kehilangan alat, atau disconnect menggagalkan quest. Pemain harus mengulang dari awal. Ladang dipulihkan sebelum digunakan lagi.
- Batas tiga quest sukses dan cooldown 20 menit tetap berlaku global untuk seluruh jenis/NPC quest. Nama fraksi merupakan identitas NPC, belum membatasi pemain berdasarkan keanggotaan fraksi.

## Verifikasi

Build Java 21/Paper API 1.21.4 dan seluruh test project berhasil. Pemeriksaan tambahan mencakup area Revenant 14.112 blok, 2.352 tanaman, snapshot 90.000 tanaman, penyimpanan lebih dari 16 MB, pergantian putaran, larangan panen ketika persiapan belum selesai, pemulihan setelah batal/disconnect, penguncian area bertumpuk, kegagalan simpan, serta pelestarian hadiah dan progres.

Tes server nyata tetap diperlukan: terima quest dengan iron hoe, coba panen sebelum/ sesudah pesan Ladang siap, selesaikan seluruh putaran, cek saldo Viti, lalu uji pembatalan dan restart. Pengujian otomatis tidak mengukur TPS servermu atau interaksi semua plugin yang terpasang.

## Installer dan file Java lengkap

<!-- VITAE_INSTALLER_BEGIN -->
```powershell
[CmdletBinding()]
param([string]$ProjectPath = (Get-Location).Path, [switch]$CheckOnly)
$ErrorActionPreference = 'Stop'
$utf8Vitae = New-Object System.Text.UTF8Encoding($false)
$rootVitae = [IO.Path]::GetFullPath($ProjectPath)
$lfVitae = [string][char]10
$crVitae = [string][char]13
function Get-VitaeHash([byte[]]$Bytes) {
    $shaVitae = [Security.Cryptography.SHA256]::Create()
    try { return ([BitConverter]::ToString($shaVitae.ComputeHash($Bytes))).Replace('-', '').ToLowerInvariant() }
    finally { $shaVitae.Dispose() }
}
function Normalize-Vitae([string]$Source) {
    return $Source.TrimStart([char]0xfeff).Replace($crVitae + $lfVitae, $lfVitae).Replace($crVitae, $lfVitae).TrimEnd([char[]]$lfVitae) + $lfVitae
}
$mainVitae = Join-Path $rootVitae 'src/main/java/com/bangzachery/vitae/vitaemanager/Vitaemanager.java'
if (-not (Test-Path -LiteralPath $mainVitae -PathType Leaf)) { throw 'Vitaemanager.java tidak ditemukan. Jalankan dari folder proyek, sejajar gradlew.bat.' }
$mainTextVitae = [IO.File]::ReadAllText($mainVitae, $utf8Vitae)
if ($mainTextVitae -notmatch 'new\s+(?:com\.bangzachery\.vitae\.vitaemanager\.evolution\.)?EvolutionService\s*\(' -or
    $mainTextVitae -notmatch 'registerEvents\s*\(\s*evolution\s*,\s*this\s*\)' -or
    $mainTextVitae -notmatch 'evolution\.close\s*\(') {
    throw 'Integrasi evo pada main belum lengkap. Tidak ada file yang diubah.'
}
if ($mainTextVitae -notmatch 'NpcModule\.start\s*\(') { throw 'Integrasi NPC belum ada pada main. Tidak ada file yang diubah.' }
$filesVitae = @(
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcCommand.java'
        OriginalHash = 'e0290f06a2ee5e692839e05955a727eb6e256c5301d33b74fb1aa7e4b3495fdb'
        Hash = '68d8df96b476e686e51bd98d9fb7ace4f2808b31ba0c49ff40d664f050c06a5b'
        Source = @'
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
            "clearoptions", "root", "repeat", "speed", "pos1", "pos2", "trigger", "gate", "quest", "queststart", "reward", "on", "off", "preview", "reset", "delete");
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
                    require(args.length >= 5, "/vnpc <id> option <halaman> <tujuan|END|QUEST|OPEN> <label>");
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
            case "pos1", "pos2", "trigger" -> "pemicu";
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
                tip(sender, cmd + "pos1", "Lihat blok sudut A, lalu jalankan command");
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
                tip(sender, cmd + "gate pos1", "Aktifkan pemilihan A, lalu pukul block sudut dinding; block tidak pecah");
                tip(sender, cmd + "gate pos2", "Aktifkan pemilihan B, lalu pukul sudut diagonal seberangnya");
                tip(sender, cmd + "gate set", "Simpan 1–256 block dinding; trigger area lama NPC ini dimatikan");
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
            case "gate" -> values.addAll(List.of("pos1", "pos2", "set", "status", "reset", "off", "cancel", "autoclose"));
            case "quest" -> values.addAll(List.of("farm", "mob", "off", "cancel"));
            case "reward" -> values.addAll(List.of("dialog", "quest"));
            case "page", "option", "root", "clearoptions" -> values.addAll(service.get(a[0]).nodes().keySet());
            default -> { }
        }
        else if (a.length == 4 && a[1].equalsIgnoreCase("gate") && a[2].equalsIgnoreCase("autoclose")) values.addAll(List.of("10", "30", "60", "off"));
        else if (a.length == 4 && a[1].equalsIgnoreCase("remember")) values.addAll(List.of("1", "2", "3", "4"));
        else if (a.length == 4 && a[1].equalsIgnoreCase("reward")) values.addAll(List.of("viti", "items", "none"));
        else if (a.length == 4 && a[1].equalsIgnoreCase("option")) { values.addAll(List.of("END", "QUEST", "OPEN")); values.addAll(service.get(a[0]).nodes().keySet()); }
        else if (a.length == 5 && a[1].equalsIgnoreCase("quest") && a[2].equalsIgnoreCase("mob")) values.addAll(List.of("COW", "PIG", "SHEEP", "CHICKEN", "ZOMBIE", "SKELETON"));
        String last = a.length == 0 ? "" : a[a.length - 1].toLowerCase(Locale.ROOT);
        return values.stream().filter(v -> v.toLowerCase(Locale.ROOT).startsWith(last)).sorted().toList();
    }
}
'@
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcData.java'
        OriginalHash = '594ef5662c976c940e2e3e78546c4254807622c2320d35d691a3bc589187fb86'
        Hash = 'ec5a3242168e59cb7d12474143efd76400fe0623172e9500fb29ccb6faa363c0'
        Source = @'
package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.economy.VitiAmount;
import com.bangzachery.vitae.vitaemanager.whisper.WhisperArea;
import java.math.BigDecimal;
import java.util.*;

/** Immutable definitions and durable progress. No Bukkit calls in this file. */
public final class NpcData {
    private NpcData() {}
    public enum Kind { HUMAN, MODEL }
    public enum QuestKind { NONE, FARM, MOB }
    public enum GateStatus { CLOSED, OPENING, OPEN, CLOSING }
    public record GateBlock(int x, int y, int z, String data) {
        public GateBlock {
            if (Math.abs((long) x) > 30_000_000 || Math.abs((long) z) > 30_000_000 || Math.abs((long) y) > 4096)
                throw new IllegalArgumentException("Koordinat gerbang di luar batas.");
            data = text(data, 512, "Data block gerbang");
        }
        public String key() { return x + ":" + y + ":" + z; }
    }
    public record GateTiming(int seconds, long closeAt) {
        public GateTiming {
            if (seconds < 0 || seconds > 3600 || closeAt < 0 || (seconds == 0 && closeAt != 0))
                throw new IllegalArgumentException("Tutup otomatis: off atau 1–3600 detik.");
        }
        public static GateTiming defaults() { return new GateTiming(30, 0); }
    }
    public record Gate(WhisperArea area, List<GateBlock> blocks, GateStatus status, GateTiming timing) {
        public Gate {
            Objects.requireNonNull(area); Objects.requireNonNull(status); blocks = List.copyOf(blocks); timing = Objects.requireNonNullElse(timing, GateTiming.defaults());
            long volume = (long) (area.maxX() - area.minX() + 1) * (area.maxY() - area.minY() + 1) * (area.maxZ() - area.minZ() + 1);
            if (volume > 256 || blocks.isEmpty() || blocks.size() > 256) throw new IllegalArgumentException("Gerbang perlu 1–256 block; seleksi maksimal 256 block.");
            Set<String> positions = new HashSet<>();
            for (GateBlock block : blocks) if (!positions.add(block.key()) || !area.contains(area.world(), block.x() + .5, block.y() + .5, block.z() + .5))
                throw new IllegalArgumentException("Block gerbang duplikat atau di luar seleksi.");
        }
        public Gate(WhisperArea area, List<GateBlock> blocks, GateStatus status) { this(area, blocks, status, GateTiming.defaults()); }
        public Gate withStatus(GateStatus next) { return new Gate(area, blocks, next,
                new GateTiming(timing.seconds(), next == GateStatus.OPEN ? timing.closeAt() : 0)); }
        public Gate opened(long now) { return new Gate(area, blocks, GateStatus.OPEN,
                new GateTiming(timing.seconds(), timing.seconds() == 0 ? 0 : Math.addExact(now, timing.seconds() * 1000L))); }
        public Gate withDelay(int seconds, long now) { return new Gate(area, blocks, status,
                new GateTiming(seconds, status == GateStatus.OPEN && seconds > 0 ? Math.addExact(now, seconds * 1000L) : 0)); }
        public boolean due(long now) { return status == GateStatus.OPEN && timing.seconds() > 0 && timing.closeAt() > 0 && now >= timing.closeAt(); }
        public boolean overlaps(Gate other) {
            WhisperArea b = other.area();
            return area.world().equals(b.world()) && area.minX() <= b.maxX() && b.minX() <= area.maxX()
                    && area.minY() <= b.maxY() && b.minY() <= area.maxY() && area.minZ() <= b.maxZ() && b.minZ() <= area.maxZ();
        }
    }
    public record Facing(float yaw, float pitch) {
        public Facing {
            if (!Float.isFinite(yaw) || Math.abs(yaw) > 360 || !Float.isFinite(pitch) || Math.abs(pitch) > 90)
                throw new IllegalArgumentException("Offset arah: yaw -360 sampai 360, pitch -90 sampai 90.");
        }
        public static Facing zero() { return new Facing(0, 0); }
    }
    public record Point(UUID world, double x, double y, double z, float yaw, float pitch) {
        public Point {
            Objects.requireNonNull(world);
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || Math.abs(x) > 30_000_000 || Math.abs(z) > 30_000_000 || Math.abs(y) > 4096
                    || !Float.isFinite(yaw) || !Float.isFinite(pitch) || Math.abs(pitch) > 90)
                throw new IllegalArgumentException("Lokasi NPC tidak valid.");
        }
    }
    public record Reward(BigDecimal viti, List<String> items) {
        public Reward {
            viti = VitiAmount.checked(viti);
            items = List.copyOf(items);
            if (items.size() > 27 || (viti.signum() > 0 && !items.isEmpty()))
                throw new IllegalArgumentException("Pilih hadiah Viti atau item, bukan keduanya.");
            for (String item : items) if (item.isBlank() || item.length() > 2_000_000)
                throw new IllegalArgumentException("Data item terlalu besar atau kosong.");
        }
        public static Reward none() { return new Reward(BigDecimal.ZERO, List.of()); }
        public boolean empty() { return viti.signum() == 0 && items.isEmpty(); }
    }
    public record Choice(String label, String target, String memoryKey, String memoryValue) {
        public Choice {
            label = text(label, 80, "Label pilihan");
            target = id(target);
            memoryKey = Objects.requireNonNullElse(memoryKey, ""); memoryValue = Objects.requireNonNullElse(memoryValue, "");
            if (!memoryKey.isEmpty()) { id(memoryKey); id(memoryValue); }
            else if (!memoryValue.isEmpty()) throw new IllegalArgumentException("Kunci ingatan belum diisi.");
        }
        public Choice(String label, String target) { this(label, target, "", ""); }
    }
    public record MemoryRule(String key, String value, String page) {
        public MemoryRule { key = id(key); value = id(value); page = id(page); }
    }
    public record Appearance(boolean nametag, boolean track) {
        public static Appearance defaults() { return new Appearance(true, true); }
    }
    public record Node(String text, List<Choice> choices) {
        public Node { text = NpcData.text(text, 512, "Dialog"); choices = List.copyOf(choices);
            if (choices.size() > 8) throw new IllegalArgumentException("Maksimum delapan pilihan per halaman."); }
        public List<Choice> available() {
            return choices.isEmpty() ? List.of(new Choice("Keluar...", "END")) : choices;
        }
    }
    public record Crop(int x, int y, int z, String plant, String soil) {
        public Crop { plant = text(plant, 200, "Tanaman"); soil = text(soil, 200, "Tanah"); }
        public String key() { return x + ":" + y + ":" + z; }
    }
    public record Quest(QuestKind kind, WhisperArea area, Point start, int goal,
                        String mob, List<Crop> crops, Reward reward) {
        public Quest {
            Objects.requireNonNull(kind); Objects.requireNonNull(reward);
            crops = List.copyOf(crops);
            mob = text(mob, 64, "Jenis mob");
            if (goal < 1 || goal > 50) throw new IllegalArgumentException("Target quest harus 1 sampai 50.");
            if (kind != QuestKind.NONE) {
                Objects.requireNonNull(area); Objects.requireNonNull(start);
                if (!inside(area, start)) throw new IllegalArgumentException("Posisi awal harus di dalam area quest.");
            }
            if (kind == QuestKind.FARM) {
                if (crops.isEmpty() || !reward.items().isEmpty())
                    throw new IllegalArgumentException("Ladang perlu minimal satu tanaman dan hadiah berupa Viti.");
                Set<String> keys = new HashSet<>();
                for (Crop crop : crops) if (!keys.add(crop.key())
                        || !area.contains(area.world(), crop.x() + .5, crop.y() + .5, crop.z() + .5))
                    throw new IllegalArgumentException("Tanaman duplikat atau di luar seleksi.");
            }
        }
        public static Quest none() { return new Quest(QuestKind.NONE, null, null, 1, "COW", List.of(), Reward.none()); }
    }
    public record Definition(String id, UUID token, String name, Kind kind, String model,
                             String texture, String signature, Point location, Point stand,
                             double eyeHeight, WhisperArea trigger, String root, Map<String, Node> nodes,
                             Reward gift, Quest quest, String repeat, int speed, boolean enabled, Facing facing, Gate gate, Appearance appearance, List<MemoryRule> memory) {
        public Definition {
            facing = Objects.requireNonNullElse(facing, Facing.zero());
            appearance = Objects.requireNonNullElse(appearance, Appearance.defaults());
            memory = memory == null ? List.of() : List.copyOf(memory);
            if (memory.size() > 64) throw new IllegalArgumentException("Maksimal 64 aturan ingatan.");
            id = NpcData.id(id); Objects.requireNonNull(token); name = text(name, 64, "Nama NPC");
            Objects.requireNonNull(kind); Objects.requireNonNull(location); Objects.requireNonNull(stand);
            Objects.requireNonNull(gift); Objects.requireNonNull(quest);
            model = Objects.requireNonNull(model); texture = Objects.requireNonNull(texture);
            signature = Objects.requireNonNull(signature); root = NpcData.id(root);
            nodes = Map.copyOf(nodes); repeat = text(repeat, 512, "Dialog pengulangan");
            if (model.length() > 128 || texture.length() > 16_384 || signature.length() > 16_384)
                throw new IllegalArgumentException("Model atau data skin terlalu besar.");
            if (nodes.isEmpty() || nodes.size() > 128 || !nodes.containsKey(root))
                throw new IllegalArgumentException("Halaman awal tidak ditemukan; maksimum 128 halaman.");
            for (String key : nodes.keySet()) NpcData.id(key);
            if (!Double.isFinite(eyeHeight) || eyeHeight < .2 || eyeHeight > 8 || speed < 1 || speed > 8)
                throw new IllegalArgumentException("Tinggi kepala 0,2–8; kecepatan teks 1–8.");
            if (!location.world().equals(stand.world()) || (trigger != null && !trigger.world().equals(location.world())))
                throw new IllegalArgumentException("NPC, posisi berdiri, dan pemicu harus di dunia yang sama.");
            if (gate != null && !gate.area().world().equals(location.world())) throw new IllegalArgumentException("Gerbang dan NPC harus di dunia yang sama.");
            if (gate != null && gate.area().contains(stand.world(), stand.x(), stand.y(), stand.z()))
                throw new IllegalArgumentException("Posisi bicara harus berada di luar dinding gerbang.");
            Set<String> rules = new HashSet<>();
            for (MemoryRule rule : memory) {
                if (!rules.add(rule.key() + ":" + rule.value())) throw new IllegalArgumentException("Aturan ingatan duplikat.");
                if (enabled && !nodes.containsKey(rule.page())) throw new IllegalArgumentException("Halaman ingatan belum ada: " + rule.page());
            }
            if (enabled) validate(nodes, root, kind, model, quest, gate);
        }
        public Definition(String id, UUID token, String name, Kind kind, String model, String texture, String signature,
                          Point location, Point stand, double eyeHeight, WhisperArea trigger, String root, Map<String, Node> nodes,
                          Reward gift, Quest quest, String repeat, int speed, boolean enabled, Facing facing, Gate gate) {
            this(id, token, name, kind, model, texture, signature, location, stand, eyeHeight, trigger, root,
                    nodes, gift, quest, repeat, speed, enabled, facing, gate, Appearance.defaults(), List.of());
        }
        /** Retains source compatibility with existing NPC setup/tests. */
        public Definition(String id, UUID token, String name, Kind kind, String model, String texture, String signature,
                          Point location, Point stand, double eyeHeight, WhisperArea trigger, String root, Map<String, Node> nodes,
                          Reward gift, Quest quest, String repeat, int speed, boolean enabled, Facing facing) {
            this(id, token, name, kind, model, texture, signature, location, stand, eyeHeight, trigger, root,
                    nodes, gift, quest, repeat, speed, enabled, facing, null);
        }
        public Definition(String id, UUID token, String name, Kind kind, String model, String texture, String signature,
                          Point location, Point stand, double eyeHeight, WhisperArea trigger, String root, Map<String, Node> nodes,
                          Reward gift, Quest quest, String repeat, int speed, boolean enabled) {
            this(id, token, name, kind, model, texture, signature, location, stand, eyeHeight, trigger, root,
                    nodes, gift, quest, repeat, speed, enabled, Facing.zero());
        }
        public Definition withGate(Gate next) {
            return new Definition(id, token, name, kind, model, texture, signature, location, stand,
                    eyeHeight, trigger, root, nodes, gift, quest, repeat, speed, enabled, facing, next, appearance, memory);
        }
        public Point facingLocation() {
            float yaw = ((location.yaw() + facing.yaw()) % 360 + 540) % 360 - 180;
            float pitch = Math.max(-90, Math.min(90, location.pitch() + facing.pitch()));
            return new Point(location.world(), location.x(), location.y(), location.z(), yaw, pitch);
        }
        public static Definition create(String id, String name, Point location, Point stand) {
            return new Definition(id, UUID.randomUUID(), name, Kind.HUMAN, "", "", "", location, stand,
                    1.62, null, "start", Map.of("start", new Node("Halo, ada yang bisa kubantu?", List.of())),
                    Reward.none(), Quest.none(), "Aku sudah memberikan hadiah kepadamu. Pergi sana, kamu mencari bansos lagi?", 1, false);
        }
    }
    private static void validate(Map<String, Node> nodes, String root, Kind kind, String model, Quest quest, Gate gate) {
        if (kind == Kind.MODEL && model.isBlank()) throw new IllegalArgumentException("ID model belum diatur.");
        for (Node node : nodes.values()) for (Choice c : node.available()) {
            if (c.target().equals("QUEST")) {
                if (quest.kind() == QuestKind.NONE || quest.reward().empty())
                    throw new IllegalArgumentException("Pilihan QUEST memerlukan quest dan hadiah.");
            } else if (c.target().equals("OPEN")) {
                if (gate == null) throw new IllegalArgumentException("Pilihan OPEN memerlukan gerbang; /vnpc <id> gate set.");
            } else if (!c.target().equals("END") && !nodes.containsKey(c.target()))
                throw new IllegalArgumentException("Tujuan pilihan tidak ditemukan: " + c.target());
        }
        Set<String> exits = new HashSet<>();
        boolean changed;
        do {
            changed = false;
            for (var entry : nodes.entrySet()) if (!exits.contains(entry.getKey())
                    && entry.getValue().available().stream().anyMatch(c -> c.target().equals("END") || c.target().equals("OPEN")
                    || c.target().equals("QUEST") || exits.contains(c.target()))) changed |= exits.add(entry.getKey());
        } while (changed);
        if (exits.size() != nodes.size() || !exits.contains(root))
            throw new IllegalArgumentException("Setiap halaman harus memiliki jalan menuju END, QUEST, atau OPEN.");
    }
    public record Quota(int count, long until) {
        public Quota {
            if (count < 0 || count > 3 || until < 0 || (count < 3 && until != 0) || (count == 3 && until == 0))
                throw new IllegalArgumentException("Data cooldown tidak valid.");
        }
        public Quota effective(long now) { return count == 3 && now >= until ? new Quota(0, 0) : this; }
        public Quota complete(long now) {
            Quota old = effective(now);
            if (old.count == 3) throw new IllegalStateException("Masih cooldown.");
            return old.count == 2 ? new Quota(3, Math.addExact(now, 1_200_000)) : new Quota(old.count + 1, 0);
        }
    }
    public record Award(UUID receipt, UUID owner, String npcName, Reward reward) {
        public Award { Objects.requireNonNull(receipt); Objects.requireNonNull(owner);
            npcName = text(npcName, 64, "Nama NPC"); Objects.requireNonNull(reward); }
    }
    public record State(int version, Map<String, Definition> npcs, Map<UUID, Set<UUID>> completed,
                        Map<UUID, Quota> quotas, Map<UUID, Award> pending, Map<UUID, Map<UUID, Map<String, String>>> memories) {
        public State {
            if (version != 1 || npcs.size() > 128) throw new IllegalArgumentException("Versi data atau jumlah NPC tidak valid.");
            npcs = Map.copyOf(npcs); quotas = Map.copyOf(quotas); pending = Map.copyOf(pending);
            Map<UUID, Map<UUID, Map<String, String>>> frozen = new HashMap<>();
            if (memories != null) memories.forEach((player, byNpc) -> {
                Map<UUID, Map<String, String>> copy = new HashMap<>();
                byNpc.forEach((token, values) -> {
                    if (values.size() > 64) throw new IllegalArgumentException("Maksimal 64 kunci ingatan per NPC/pemain.");
                    values.forEach((key, value) -> { id(key); id(value); }); copy.put(Objects.requireNonNull(token), Map.copyOf(values));
                }); frozen.put(Objects.requireNonNull(player), Map.copyOf(copy));
            }); memories = Map.copyOf(frozen);
            var progress = new HashMap<UUID, Set<UUID>>();
            completed.forEach((player, tokens) -> progress.put(Objects.requireNonNull(player), Set.copyOf(tokens)));
            completed = Map.copyOf(progress);
            npcs.forEach((key, value) -> { if (!key.equals(value.id())) throw new IllegalArgumentException("ID NPC berbeda."); });
            var gated = npcs.values().stream().filter(d -> d.gate() != null).toList();
            for (int i = 0; i < gated.size(); i++) for (int j = i + 1; j < gated.size(); j++)
                if (gated.get(i).gate().overlaps(gated.get(j).gate())) throw new IllegalArgumentException("Seleksi gerbang tidak boleh bertumpuk antar NPC.");
            for (var d : gated) for (var q : npcs.values()) if (q.quest().kind() != QuestKind.NONE && overlaps(d.gate().area(), q.quest().area()))
                throw new IllegalArgumentException("Gerbang tidak boleh bertumpuk dengan area quest.");
            pending.forEach((key, value) -> { if (!key.equals(value.owner())) throw new IllegalArgumentException("Pemilik hadiah berbeda."); });
        }
        public State(int version, Map<String, Definition> npcs, Map<UUID, Set<UUID>> completed,
                     Map<UUID, Quota> quotas, Map<UUID, Award> pending) { this(version, npcs, completed, quotas, pending, Map.of()); }
        public Map<String, String> memory(UUID player, Definition npc) { return memories.getOrDefault(player, Map.of()).getOrDefault(npc.token(), Map.of()); }
        public String recalledRoot(UUID player, Definition npc) {
            Map<String, String> values = memory(player, npc);
            for (MemoryRule rule : npc.memory()) if (rule.value().equals(values.get(rule.key()))) return rule.page();
            return npc.root();
        }
        public boolean recalls(UUID player, Definition npc) {
            Map<String, String> values = memory(player, npc);
            return npc.memory().stream().anyMatch(rule -> rule.value().equals(values.get(rule.key())));
        }
        public State remember(UUID player, Definition npc, Choice choice) {
            if (choice.memoryKey().isEmpty()) return this;
            var all = new HashMap<>(memories); var byNpc = new HashMap<>(all.getOrDefault(player, Map.of()));
            var values = new HashMap<>(byNpc.getOrDefault(npc.token(), Map.of())); values.put(choice.memoryKey(), choice.memoryValue());
            byNpc.put(npc.token(), values); all.put(player, byNpc);
            return new State(1, npcs, completed, quotas, pending, all);
        }
        public static State empty() { return new State(1, Map.of(), Map.of(), Map.of(), Map.of()); }
        public Quota quota(UUID player, long now) { return quotas.getOrDefault(player, new Quota(0, 0)).effective(now); }
        public boolean done(UUID player, Definition npc) { return completed.getOrDefault(player, Set.of()).contains(npc.token()); }
        public State put(Definition d) { var copy = new HashMap<>(npcs); copy.put(d.id(), d); return new State(1, copy, completed, quotas, pending, memories); }
        public State remove(String id) { var copy = new HashMap<>(npcs); copy.remove(id); return new State(1, copy, completed, quotas, pending, memories); }
        public State heard(UUID player, Definition npc) {
            var copy = new HashMap<>(completed); var tokens = new HashSet<>(copy.getOrDefault(player, Set.of()));
            tokens.add(npc.token()); copy.put(player, tokens); return new State(1, npcs, copy, quotas, pending, memories);
        }
        public State finish(UUID player, Definition npc, boolean quest, UUID receipt, long now) {
            if (pending.containsKey(player)) throw new IllegalStateException("Hadiah sebelumnya belum dikirim.");
            if (!quest && done(player, npc)) return this;
            State heard = heard(player, npc);
            var q = new HashMap<>(quotas); if (quest) q.put(player, quota(player, now).complete(now));
            var awards = new HashMap<>(pending);
            Reward reward = quest ? npc.quest().reward() : npc.gift();
            if (!reward.empty()) awards.put(player, new Award(receipt, player, npc.name(), reward));
            return new State(1, npcs, heard.completed, q, awards, memories);
        }
        public State acknowledge(UUID player, UUID receipt) {
            Award a = pending.get(player); if (a == null || !a.receipt().equals(receipt)) return this;
            var copy = new HashMap<>(pending); copy.remove(player); return new State(1, npcs, completed, quotas, copy, memories);
        }
        public State reset(UUID player, Definition npc) {
            if (pending.containsKey(player)) throw new IllegalStateException("Tunggu hadiah terkirim sebelum reset.");
            var c = new HashMap<>(completed); var tokens = new HashSet<>(c.getOrDefault(player, Set.of()));
            tokens.remove(npc.token()); c.put(player, tokens);
            var q = new HashMap<>(quotas); q.remove(player);
            var all = new HashMap<>(memories); var byNpc = new HashMap<>(all.getOrDefault(player, Map.of()));
            byNpc.remove(npc.token()); if (byNpc.isEmpty()) all.remove(player); else all.put(player, byNpc);
            return new State(1, npcs, c, q, pending, all);
        }
    }
    public static String id(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]{1,48}")) throw new IllegalArgumentException("ID harus 1–48 huruf, angka, _ atau -.");
        return value;
    }
    public static String text(String value, int limit, String field) {
        if (value == null || value.isBlank() || value.length() > limit) throw new IllegalArgumentException(field + " kosong atau terlalu panjang.");
        return value;
    }
    public static boolean inside(WhisperArea a, Point p) {
        return a.world().equals(p.world()) && p.x() >= a.minX() && p.x() < a.maxX() + 1.0
                && p.z() >= a.minZ() && p.z() < a.maxZ() + 1.0 && p.y() >= a.minY() - 1 && p.y() < a.maxY() + 3.0;
    }
    public static boolean overlaps(WhisperArea a, WhisperArea b) {
        return a.world().equals(b.world()) && a.minX() <= b.maxX() && b.minX() <= a.maxX()
                && a.minZ() <= b.maxZ() && b.minZ() <= a.maxZ() && a.minY() - 1 <= b.maxY() + 3 && b.minY() - 1 <= a.maxY() + 3;
    }
}
'@
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcFarm.java'
        OriginalHash = ''
        Hash = '2cc96c14192a40191772cde7aded02ddb4fbab27dbad828452e880dd413a17b8'
        Source = @'
package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.whisper.WhisperArea;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;

/** Farm world access stays on the server thread, with a shared budget for all NPCs. */
final class NpcFarm implements AutoCloseable {
    static final int BLOCKS_PER_TICK = 512;
    private static final int SLICE = 32;
    private static final long NANOS_PER_TICK = 3_000_000;
    private static final Set<Material> PLANTS = Set.of(Material.CARROTS, Material.POTATOES, Material.WHEAT, Material.BEETROOTS);
    private final Deque<Job> jobs = new ArrayDeque<>();
    private boolean closed;

    final class Handle {
        private final Job job;
        private Handle(Job job) { this.job = job; }
        void cancel() { job.cancelled = true; jobs.remove(job); }
    }
    private abstract static class Job {
        final WhisperArea area;
        final BooleanSupplier active;
        final Consumer<String> failure;
        final Map<String, BlockData> data = new HashMap<>();
        boolean cancelled, done;
        CompletableFuture<Chunk> loading;
        int loadingX, loadingZ;
        long waitingSince;
        Job(WhisperArea area, BooleanSupplier active, Consumer<String> failure) {
            this.area = area; this.active = active; this.failure = failure;
        }
        BlockData data(String value) { return data.computeIfAbsent(value, Bukkit::createBlockData); }
        boolean loaded(World world, int x, int z) {
            int cx = x >> 4, cz = z >> 4;
            if (world.isChunkLoaded(cx, cz)) { loading = null; waitingSince = 0; return true; }
            if (loading == null || cx != loadingX || cz != loadingZ) {
                loadingX = cx; loadingZ = cz; waitingSince = System.currentTimeMillis();
                loading = world.getChunkAtAsync(cx, cz, false); // Existing terrain only; never generate new chunks.
                return false;
            }
            if (loading.isDone()) {
                if (loading.join() == null) throw new IllegalStateException("Chunk ladang belum tersedia. Bangun/muat ladangnya dahulu.");
                loading = null;
            }
            if (waitingSince > 0 && System.currentTimeMillis() - waitingSince > 60_000)
                throw new IllegalStateException("Chunk ladang belum bisa dimuat; coba kembali setelah dunia siap.");
            return false;
        }
        abstract int step(World world, int allowance);
        abstract void success();
    }
    Handle scan(WhisperArea area, BooleanSupplier active, BiConsumer<Long, Long> progress,
                Consumer<List<NpcData.Crop>> success, Consumer<String> failure) {
        if (closed) throw new IllegalStateException("Modul ladang sedang ditutup.");
        World world = Objects.requireNonNull(Bukkit.getWorld(area.world()), "Dunia ladang belum dimuat.");
        if (area.minY() <= world.getMinHeight() || area.maxY() >= world.getMaxHeight())
            throw new IllegalArgumentException("Pilih tanaman di dalam ketinggian dunia; tanah di bawahnya juga harus tersedia.");
        Job job = new Job(area, active, failure) {
            final NpcFarmCursor cursor = new NpcFarmCursor(area);
            final List<NpcData.Crop> crops = new ArrayList<>();
            long lastProgress;
            @Override int step(World current, int allowance) {
                int used = 0;
                while (!cursor.done() && used < allowance) {
                    if (!loaded(current, cursor.x(), cursor.z())) break;
                    Block b = current.getBlockAt(cursor.x(), cursor.y(), cursor.z());
                    if (PLANTS.contains(b.getType())) {
                        Block soil = b.getRelative(BlockFace.DOWN);
                        if (soil.getType() != Material.FARMLAND)
                            throw new IllegalArgumentException("Tanaman pada " + cursor.x() + ", " + cursor.y() + ", " + cursor.z() + " harus di atas farmland.");
                        Ageable plant = (Ageable) b.getBlockData(); plant.setAge(plant.getMaximumAge());
                        crops.add(new NpcData.Crop(cursor.x(), cursor.y(), cursor.z(), plant.getAsString(), soil.getBlockData().getAsString()));
                    }
                    cursor.next(); used++;
                }
                long now = System.currentTimeMillis();
                if (now - lastProgress >= 1000 || cursor.done()) { progress.accept(cursor.visited(), cursor.volume()); lastProgress = now; }
                done = cursor.done();
                return used;
            }
            @Override void success() {
                if (crops.isEmpty()) throw new IllegalArgumentException("Siapkan minimal satu wortel/kentang/gandum/bit di dalam seleksi.");
                success.accept(List.copyOf(crops));
            }
        };
        jobs.addLast(job); return new Handle(job);
    }
    Handle restore(NpcData.Quest quest, BooleanSupplier active, Consumer<NpcData.Crop> planted,
                   Runnable success, Consumer<String> failure) {
        if (closed) throw new IllegalStateException("Modul ladang sedang ditutup.");
        Job job = new Job(quest.area(), active, failure) {
            int index;
            @Override int step(World world, int allowance) {
                int used = 0;
                while (index < quest.crops().size() && used < allowance) {
                    NpcData.Crop crop = quest.crops().get(index);
                    if (!loaded(world, crop.x(), crop.z())) break;
                    if (crop.y() <= world.getMinHeight() || crop.y() >= world.getMaxHeight())
                        throw new IllegalArgumentException("Ketinggian snapshot ladang di luar dunia.");
                    world.getBlockAt(crop.x(), crop.y() - 1, crop.z()).setBlockData(data(crop.soil()), false);
                    world.getBlockAt(crop.x(), crop.y(), crop.z()).setBlockData(data(crop.plant()), false);
                    planted.accept(crop); index++; used++;
                }
                done = index == quest.crops().size(); return used;
            }
            @Override void success() { success.run(); }
        };
        jobs.addLast(job); return new Handle(job);
    }
    boolean busy(WhisperArea area) { return jobs.stream().anyMatch(j -> !j.cancelled && NpcData.overlaps(j.area, area)); }
    boolean contains(NpcData.Point point) { return jobs.stream().anyMatch(j -> !j.cancelled && NpcData.inside(j.area, point)); }
    void tick() {
        long until = System.nanoTime() + NANOS_PER_TICK;
        int used = 0, idle = 0;
        while (!jobs.isEmpty() && used < BLOCKS_PER_TICK && System.nanoTime() < until) {
            Job job = jobs.removeFirst();
            if (job.cancelled) continue;
            try {
                if (!job.active.getAsBoolean()) throw new IllegalStateException("Pemrosesan ladang dibatalkan; setup lama dipertahankan.");
                World world = Bukkit.getWorld(job.area.world());
                int amount = world == null ? 0 : job.step(world, Math.min(SLICE, BLOCKS_PER_TICK - used));
                used += amount;
                if (job.done) { job.success(); idle = 0; }
                else { jobs.addLast(job); idle = amount == 0 ? idle + 1 : 0; }
                if (idle >= jobs.size()) break; // Every queued job is waiting; retry on the next tick.
            } catch (RuntimeException e) {
                job.cancelled = true;
                Throwable cause = e; while (cause.getCause() != null) cause = cause.getCause();
                job.failure.accept(Objects.requireNonNullElse(cause.getMessage(), "Pemrosesan ladang gagal."));
            }
        }
    }
    @Override public void close() { closed = true; for (Job job : jobs) job.cancelled = true; jobs.clear(); }
}
'@
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcFarmCursor.java'
        OriginalHash = ''
        Hash = '8ddc81b0bca051332853d80d00e82c3a0dc22cc073b2e2f4eae1613581c2b4fc'
        Source = @'
package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.whisper.WhisperArea;

/** Inclusive, chunk-ordered traversal without allocating a list of every selected block. */
final class NpcFarmCursor {
    private final WhisperArea area;
    private int chunkX, chunkZ, x, y, z;
    private long visited;
    private boolean done;

    NpcFarmCursor(WhisperArea area) {
        this.area = area; chunkX = area.minX() >> 4; chunkZ = area.minZ() >> 4;
        firstInChunk();
    }
    int x() { return x; }
    int y() { return y; }
    int z() { return z; }
    int chunkX() { return chunkX; }
    int chunkZ() { return chunkZ; }
    long visited() { return visited; }
    long volume() {
        return ((long) area.maxX() - area.minX() + 1) * ((long) area.maxY() - area.minY() + 1)
                * ((long) area.maxZ() - area.minZ() + 1);
    }
    boolean done() { return done; }
    void next() {
        if (done) return;
        visited++;
        if (++y <= area.maxY()) return;
        y = area.minY();
        if (++x <= Math.min(area.maxX(), (chunkX << 4) + 15)) return;
        x = Math.max(area.minX(), chunkX << 4);
        if (++z <= Math.min(area.maxZ(), (chunkZ << 4) + 15)) return;
        nextChunk();
    }
    private void nextChunk() {
        if (++chunkX > (area.maxX() >> 4)) { chunkX = area.minX() >> 4; chunkZ++; }
        if (chunkZ > (area.maxZ() >> 4)) { done = true; return; }
        firstInChunk();
    }
    private void firstInChunk() {
        x = Math.max(area.minX(), chunkX << 4); y = area.minY(); z = Math.max(area.minZ(), chunkZ << 4);
    }
}
'@
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcQuest.java'
        OriginalHash = '370aa44f067787f13fd8c0ca88b2877c37274b50cc0635536326d33d329890e2'
        Hash = 'a90eda18fa3dab8a9517f3147e782a1082c8ce1e19e77a840a3e3a489b0d6071'
        Source = @'
package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.whisper.WhisperArea;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
import java.util.function.*;

/** One occupant per overlapping quest area; world restoration runs on the server thread. */
public final class NpcQuest implements Listener, AutoCloseable {
    public static final class Run {
        public final UUID player;
        public final NpcData.Definition npc;
        public final long deadline = System.currentTimeMillis() + 1_800_000;
        public final BossBar bar = BossBar.bossBar(Component.text("Quest • 30:00"), 1, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
        public final Set<String> remaining = new HashSet<>();
        final Map<String, NpcData.Crop> crops = new HashMap<>();
        boolean preparing;
        NpcFarm.Handle work;
        public int count;
        public long nextWave;
        public Mob mob;
        public Location spawn;
        Run(UUID player, NpcData.Definition npc) { this.player = player; this.npc = npc; }
    }
    private final JavaPlugin plugin;
    private final Supplier<Collection<NpcData.Definition>> definitions;
    private final Consumer<Run> completed;
    private final BiPredicate<Player, Location> teleport;
    private final NamespacedKey key;
    private final Map<UUID, Run> runs = new HashMap<>();
    private final NpcFarm farms = new NpcFarm();
    private static final class Repair {
        final NpcData.Definition npc;
        NpcFarm.Handle work;
        long retryAt;
        boolean warned;
        Repair(NpcData.Definition npc) { this.npc = npc; }
    }
    private final Map<UUID, Repair> repairs = new LinkedHashMap<>();
    private long ticks;
    public NpcQuest(JavaPlugin plugin, Supplier<Collection<NpcData.Definition>> definitions,
                    Consumer<Run> completed, BiPredicate<Player, Location> teleport) {
        this.plugin = plugin; this.definitions = definitions; this.completed = completed; this.teleport = teleport;
        key = new NamespacedKey(plugin, "npc_quest_mob");
    }
    public Run run(UUID player) { return runs.get(player); }
    public boolean uses(String npc) {
        return runs.values().stream().anyMatch(r -> r.npc.id().equals(npc))
                || repairs.values().stream().anyMatch(r -> r.npc.id().equals(npc));
    }
    public boolean farmBusy(WhisperArea area) {
        return farms.busy(area) || repairs.values().stream().anyMatch(r -> NpcData.overlaps(r.npc.quest().area(), area))
                || runs.values().stream().anyMatch(r -> NpcData.overlaps(r.npc.quest().area(), area));
    }
    NpcFarm.Handle scan(WhisperArea area, BooleanSupplier active, BiConsumer<Long, Long> progress,
                        Consumer<List<NpcData.Crop>> success, Consumer<String> failure) {
        return farms.scan(area, active, progress, success, failure);
    }
    /** Keep an idle field reserved until its complete snapshot has been restored. */
    public void restore(NpcData.Definition npc) {
        if (npc.quest().kind() != NpcData.QuestKind.FARM || repairs.containsKey(npc.token())) return;
        if (runs.values().stream().anyMatch(r -> NpcData.overlaps(r.npc.quest().area(), npc.quest().area()))) return;
        Repair repair = new Repair(npc); repairs.put(npc.token(), repair); repair(repair);
    }
    private void repair(Repair repair) {
        repair.work = farms.restore(repair.npc.quest(), () -> repairs.get(repair.npc.token()) == repair,
                crop -> {}, () -> repairs.remove(repair.npc.token()), reason -> {
                    repair.work = null; repair.retryAt = ticks + 200;
                    if (!repair.warned) { repair.warned = true; plugin.getLogger().warning("Ladang " + repair.npc.id() + " belum pulih: " + reason); }
                });
    }
    public boolean tool(Player p, NpcData.Quest q) {
        Material type = q.kind() == NpcData.QuestKind.FARM ? Material.IRON_HOE : Material.IRON_SWORD;
        for (ItemStack item : p.getInventory().getStorageContents()) if (item != null && item.getType() == type) return true;
        return p.getInventory().getItemInOffHand().getType() == type;
    }
    public String refusal(Player p, NpcData.Definition npc) {
        if (!tool(p, npc.quest())) return npc.quest().kind() == NpcData.QuestKind.FARM
                ? "Kamu perlu iron hoe di inventory untuk meladang." : "Bawalah iron sword untuk mengerjakan quest ini.";
        if (runs.values().stream().anyMatch(r -> NpcData.overlaps(r.npc.quest().area(), npc.quest().area())))
            return "Tempat quest sedang dipakai. Tunggu pemain sebelumnya selesai, ya.";
        if (farmBusy(npc.quest().area())) return "Ladang sedang disiapkan atau dipulihkan. Coba kembali sebentar lagi.";
        return null;
    }
    public boolean start(Player p, NpcData.Definition npc) {
        if (refusal(p, npc) != null || runs.containsKey(p.getUniqueId())) return false;
        Run run = new Run(p.getUniqueId(), npc); runs.put(run.player, run);
        if (!teleport.test(p, NpcRenderer.location(npc.quest().start()))) { runs.remove(run.player); return false; }
        p.showBossBar(run.bar);
        try { wave(run); tell(p, npc, "Quest dimulai: " + objective(npc.quest()) + ". Target " + npc.quest().goal() + "; batas waktu 30 menit. /quest quit untuk batal."); return true; }
        catch (RuntimeException e) { cancel(p.getUniqueId(), "Lokasi spawn tidak aman. Hubungi admin.", true); return false; }
    }
    private static String objective(NpcData.Quest q) {
        if (q.kind() == NpcData.QuestKind.MOB) return "kalahkan " + q.mob().toLowerCase(Locale.ROOT);
        Set<String> names = new LinkedHashSet<>();
        Set<String> plants = new LinkedHashSet<>(); for (var crop : q.crops()) plants.add(crop.plant());
        for (String plant : plants) names.add(switch (Bukkit.createBlockData(plant).getMaterial()) {
            case CARROTS -> "wortel"; case POTATOES -> "kentang"; case WHEAT -> "gandum"; case BEETROOTS -> "bit";
            default -> "tanaman";
        });
        return "panen seluruh " + String.join(", ", names) + " setiap gelombang";
    }
    private void wave(Run run) {
        run.nextWave = 0;
        if (run.npc.quest().kind() == NpcData.QuestKind.FARM) {
            run.preparing = true; run.remaining.clear(); run.crops.clear();
            Player player = Bukkit.getPlayer(run.player);
            if (player != null) tell(player, run.npc, "Menyiapkan " + run.npc.quest().crops().size() + " tanaman untuk putaran " + (run.count + 1) + ". Tunggu sampai ladang siap.");
            run.work = farms.restore(run.npc.quest(), () -> runs.get(run.player) == run,
                    crop -> { run.remaining.add(crop.key()); run.crops.put(crop.key(), crop); }, () -> {
                        if (runs.get(run.player) != run) return;
                        run.work = null; run.preparing = false;
                        Player p = Bukkit.getPlayer(run.player);
                        if (p != null) tell(p, run.npc, "Ladang siap. Panen seluruh " + run.remaining.size() + " tanaman!");
                    }, reason -> {
                        plugin.getLogger().warning("Putaran ladang " + run.npc.id() + " gagal: " + reason);
                        cancel(run.player, "Ladang belum bisa disiapkan. Hubungi admin; quest dibatalkan.", true);
                    });
        } else spawn(run);
    }
    private void spawn(Run run) {
        WhisperArea area = run.npc.quest().area(); World world = Bukkit.getWorld(area.world());
        if (world == null) throw new IllegalStateException("Dunia belum dimuat.");
        Location chosen = null; Random random = new Random();
        for (int attempt = 0; attempt < 48 && chosen == null; attempt++) {
            int x = area.minX() + random.nextInt(area.maxX() - area.minX() + 1);
            int z = area.minZ() + random.nextInt(area.maxZ() - area.minZ() + 1);
            int low = Math.max(world.getMinHeight() + 1, area.minY() - 1);
            int high = Math.min(world.getMaxHeight() - 2, area.maxY() + 2);
            for (int y = high; y >= low; y--) {
                Material floor = world.getBlockAt(x, y - 1, z).getType();
                if (world.getBlockAt(x, y, z).isPassable() && !world.getBlockAt(x, y, z).isLiquid()
                        && world.getBlockAt(x, y + 1, z).isPassable() && !world.getBlockAt(x, y + 1, z).isLiquid()
                        && floor.isSolid() && floor != Material.MAGMA_BLOCK && floor != Material.CAMPFIRE
                        && floor != Material.SOUL_CAMPFIRE && floor != Material.CACTUS) {
                    chosen = new Location(world, x + .5, y, z + .5); break;
                }
            }
        }
        if (chosen == null) throw new IllegalStateException("Tidak ada titik mob aman di area quest.");
        EntityType type = mobType(run.npc.quest().mob());
        Entity entity = world.spawnEntity(chosen, type); if (!(entity instanceof Mob mob)) { entity.remove(); throw new IllegalArgumentException("Mob tidak didukung."); }
        run.spawn = chosen; run.mob = mob;
        mob.setPersistent(false); mob.setRemoveWhenFarAway(false); mob.setCanPickupItems(false);
        mob.getPersistentDataContainer().set(key, PersistentDataType.STRING, run.player.toString());
        if (mob instanceof Enemy) { mob.setAI(false); mob.setTarget(null); }
    }
    public static EntityType mobType(String name) {
        EntityType type = EntityType.valueOf(name.toUpperCase(Locale.ROOT));
        if (type.getEntityClass() == null || !Mob.class.isAssignableFrom(type.getEntityClass()) || type == EntityType.ENDER_DRAGON)
            throw new IllegalArgumentException("Jenis mob vanilla tidak didukung.");
        return type;
    }
    private void advance(Run run) {
        run.count++;
        Player p = Bukkit.getPlayer(run.player);
        if (p != null) tell(p, run.npc, "Progres quest: " + run.count + "/" + run.npc.quest().goal());
        if (run.count >= run.npc.quest().goal()) completed.accept(run); else run.nextWave = ticks + 40;
    }
    public void retry(Run run) { run.count = Math.max(0, run.npc.quest().goal() - 1); run.nextWave = ticks + 40; }
    public void tick() {
        ticks++;
        for (Run run : List.copyOf(runs.values())) {
            Player p = Bukkit.getPlayer(run.player);
            if (p == null || p.isDead() || !p.isOnline()) { cancel(run.player, "Quest dibatalkan.", false); continue; }
            long left = run.deadline - System.currentTimeMillis();
            if (left <= 0) { cancel(run.player, "Waktu quest habis.", true); continue; }
            if (!NpcData.inside(run.npc.quest().area(), NpcRenderer.point(p.getLocation()))) {
                cancel(run.player, "Kamu keluar dari area. Quest dibatalkan.", true); continue;
            }
            if (!tool(p, run.npc.quest())) { cancel(run.player, "Alat wajib tidak ada lagi di inventory.", true); continue; }
            if (ticks % 20 == 0) {
                long seconds = (left + 999) / 1000;
                run.bar.name(Component.text("Quest " + NpcText.plain(run.npc.name()) + " • " + String.format("%02d:%02d", seconds / 60, seconds % 60)));
                run.bar.progress(Math.max(0, Math.min(1, left / 1_800_000f)));
            }
            if (run.nextWave > 0 && ticks >= run.nextWave) {
                try { wave(run); } catch (RuntimeException e) { cancel(run.player, "Spawn quest gagal; hubungi admin.", true); }
            }
            if (run.mob != null && run.mob.isValid() && !NpcData.inside(run.npc.quest().area(), NpcRenderer.point(run.mob.getLocation()))) run.mob.teleport(run.spawn);
        }
        for (Repair repair : List.copyOf(repairs.values())) if (repair.work == null && ticks >= repair.retryAt) repair(repair);
        farms.tick();
    }
    public void finish(Run run) { cleanup(run); }
    public void cancel(UUID player, String reason, boolean back) {
        Run run = runs.get(player); if (run == null) return; cleanup(run);
        Player p = Bukkit.getPlayer(player); if (p != null) {
            if (!reason.isBlank()) tell(p, run.npc, reason);
            if (back && p.isOnline() && !p.isDead()) teleport.test(p, NpcRenderer.location(run.npc.stand()));
        }
    }
    private void cleanup(Run run) {
        runs.remove(run.player); if (run.work != null) run.work.cancel(); if (run.mob != null) run.mob.remove();
        Player p = Bukkit.getPlayer(run.player); if (p != null) p.hideBossBar(run.bar);
        restore(run.npc);
    }
    private boolean protectedAt(Block block) {
        NpcData.Point p = NpcRenderer.point(block.getLocation());
        return definitions.get().stream().anyMatch(d -> d.enabled() && d.quest().kind() == NpcData.QuestKind.FARM && NpcData.inside(d.quest().area(), p))
                || runs.values().stream().anyMatch(r -> r.npc.quest().kind() == NpcData.QuestKind.FARM && NpcData.inside(r.npc.quest().area(), p))
                || repairs.values().stream().anyMatch(r -> NpcData.inside(r.npc.quest().area(), p)) || farms.contains(p);
    }
    private Run owner(Entity entity) {
        String id = entity.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (id == null) return null;
        try { return runs.get(UUID.fromString(id)); } catch (IllegalArgumentException e) { return null; }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void harvest(BlockBreakEvent e) {
        if (!protectedAt(e.getBlock())) return; e.setCancelled(true); e.setDropItems(false); e.setExpToDrop(0);
        Run r = runs.get(e.getPlayer().getUniqueId()); if (r == null || r.npc.quest().kind() != NpcData.QuestKind.FARM || r.nextWave > 0 || r.preparing) return;
        Block b = e.getBlock(); String position = b.getX() + ":" + b.getY() + ":" + b.getZ();
        NpcData.Crop crop = r.crops.get(position);
        if (!r.remaining.contains(position) || crop == null || b.getType() != Bukkit.createBlockData(crop.plant()).getMaterial()
                || !(b.getBlockData() instanceof Ageable age) || age.getAge() != age.getMaximumAge() || !tool(e.getPlayer(), r.npc.quest())) return;
        b.setType(Material.AIR, false); r.remaining.remove(position); if (r.remaining.isEmpty()) advance(r);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void damage(EntityDamageEvent e) {
        Run r = owner(e.getEntity()); if (r == null) return;
        if (!(e instanceof EntityDamageByEntityEvent hit) || !(hit.getDamager() instanceof Player p)
                || !p.getUniqueId().equals(r.player) || p.getInventory().getItemInMainHand().getType() != Material.IRON_SWORD) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void outgoing(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        if (owner(damager) != null || (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity source && owner(source) != null)) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void dead(EntityDeathEvent e) {
        Run r = owner(e.getEntity()); if (r == null) return;
        e.getDrops().clear(); e.setDroppedExp(0); r.mob = null;
        if (e.getEntity().getKiller() != null && e.getEntity().getKiller().getUniqueId().equals(r.player)) advance(r); else r.nextWave = ticks + 40;
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void interactMob(org.bukkit.event.player.PlayerInteractEntityEvent e) {
        if (e.getRightClicked().getPersistentDataContainer().has(key, PersistentDataType.STRING)) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void shear(org.bukkit.event.player.PlayerShearEntityEvent e) {
        if (e.getEntity().getPersistentDataContainer().has(key, PersistentDataType.STRING)) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void target(EntityTargetEvent e) { if (owner(e.getEntity()) != null) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void split(SlimeSplitEvent e) { if (e.getEntity().getPersistentDataContainer().has(key, PersistentDataType.STRING)) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void breed(EntityBreedEvent e) { if (owner(e.getMother()) != null || owner(e.getFather()) != null) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void place(BlockPlaceEvent e) { if (protectedAt(e.getBlock())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void physical(org.bukkit.event.player.PlayerInteractEvent e) {
        if (e.getAction() == Action.PHYSICAL && e.getClickedBlock() != null && protectedAt(e.getClickedBlock())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void trample(EntityInteractEvent e) { if (protectedAt(e.getBlock())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void change(EntityChangeBlockEvent e) { if (protectedAt(e.getBlock()) || owner(e.getEntity()) != null) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void grow(BlockGrowEvent e) { if (protectedAt(e.getBlock())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void fade(BlockFadeEvent e) { if (protectedAt(e.getBlock())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void flow(BlockFromToEvent e) { if (protectedAt(e.getToBlock())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void piston(BlockPistonExtendEvent e) { if (e.getBlocks().stream().anyMatch(b -> protectedAt(b) || protectedAt(b.getRelative(e.getDirection())))) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void retract(BlockPistonRetractEvent e) { if (e.getBlocks().stream().anyMatch(b -> protectedAt(b) || protectedAt(b.getRelative(e.getDirection())))) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void explosion(EntityExplodeEvent e) { if (owner(e.getEntity()) != null) e.setCancelled(true); else e.blockList().removeIf(this::protectedAt); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void explosion(BlockExplodeEvent e) { e.blockList().removeIf(this::protectedAt); }
    private static void tell(Player p, NpcData.Definition d, String message) { p.sendMessage(NpcText.name(d.name()).append(Component.text(": " + message))); }
    @Override public void close() {
        for (UUID player : List.copyOf(runs.keySet())) cancel(player, "", false);
        farms.close(); repairs.clear(); HandlerList.unregisterAll(this);
    }
}
'@
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcService.java'
        OriginalHash = '0f1712e9b1e682079a4d62c87a0d452aa78d03d5a71f61590bab7080bebb633c'
        Hash = 'd79f24718a388fa9a81d5691ac5ea87d3be1bede60a4cf242735d0dd553470e5'
        Source = @'
package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.core.gui.VitaeMenu;
import com.bangzachery.vitae.vitaemanager.economy.*;
import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import com.google.gson.JsonObject;
import com.google.gson.JsonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.joml.Vector3f;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;

public final class NpcService implements Listener, AutoCloseable {
    static final class Session {
        final Player player; final NpcData.Definition npc; final boolean preview, special, trigger;
        final long deadline = System.currentTimeMillis() + 300_000;
        Location anchor; NpcData.Node node; TextDisplay display, blackout;
        int phase, age, shown, selected; float fromYaw, fromPitch, goalYaw, goalPitch;
        boolean blind;
        Session(Player player, NpcData.Definition npc, boolean preview, boolean special, boolean trigger) {
            this.player = player; this.npc = npc; this.preview = preview; this.special = special; this.trigger = trigger;
            anchor = player.getLocation().clone(); phase = trigger ? 0 : 1;
        }
    }
    private final JavaPlugin plugin;
    private final VitiService viti;
    private final NpcStore store;
    private NpcData.State state;
    private final NpcRenderer renderer;
    private final NpcQuest quests;
    private final NpcGates gates;
    private record FarmSetup(UUID admin, NpcFarm.Handle work) {}
    private final Map<String, FarmSetup> farmSetups = new HashMap<>();
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, Long> clicks = new HashMap<>(), warned = new HashMap<>();
    private final Map<UUID, BukkitTask> pushes = new HashMap<>();
    private final Set<UUID> internalTeleport = new HashSet<>(), paying = new HashSet<>();
    private final Set<UUID> queuedTriggers = new HashSet<>();
    private final Map<UUID, Long> triggerRetry = new HashMap<>();
    private final NamespacedKey awardKey;
    private BukkitTask task;
    private boolean closed;
    private long ticks;
    public NpcService(JavaPlugin plugin, VitiService viti) throws IOException {
        this.plugin = plugin; this.viti = viti; awardKey = new NamespacedKey(plugin, "npc_last_award");
        store = new NpcStore(plugin.getDataFolder().toPath().resolve("npc.json")); state = store.initialize();
        for (var d : state.npcs().values()) validateRuntime(d, false);
        for (var a : state.pending().values()) for (String bytes : a.reward().items()) decode(bytes);
        renderer = new NpcRenderer(plugin, this::modelClick);
        quests = new NpcQuest(plugin, () -> state.npcs().values(), this::questComplete, this::teleport);
        gates = new NpcGates(plugin, () -> state.npcs(), (id, gate) -> commit(state.put(get(id).withGate(gate))), this::log);
        Bukkit.getPluginManager().registerEvents(quests, plugin);
    }
    public void start() {
        try {
            gates.start();
            renderer.refresh(state.npcs());
            for (var d : state.npcs().values()) if (d.enabled()) quests.restore(d);
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1, 1);
        } catch (IOException e) { close(); throw new IllegalStateException("Status gerbang gagal dipulihkan; data lama dipertahankan.", e); }
        catch (RuntimeException e) { close(); throw e; }
    }
    public NpcData.State current() { return state; }
    public NpcData.Definition get(String id) {
        NpcData.Definition d = state.npcs().get(id);
        if (d == null) throw new IllegalArgumentException("NPC tidak ditemukan: " + id + ". /vnpc list"); return d;
    }
    public boolean inUse(String id) {
        return farmSetups.containsKey(id) || gates.busy(id) || quests.uses(id) || sessions.values().stream().anyMatch(s -> s.npc.id().equals(id));
    }
    private void commit(NpcData.State next) throws IOException { store.save(next); state = next; }
    public void create(Player p, String id, String name) throws IOException {
        if (state.npcs().containsKey(id)) throw new IllegalArgumentException("ID NPC sudah digunakan.");
        Location stand = p.getLocation(); Vector forward = stand.getDirection().setY(0);
        if (forward.lengthSquared() < .001) forward = new Vector(0, 0, 1); else forward.normalize();
        Location at = stand.clone().add(forward.multiply(2)); at.setYaw(stand.getYaw() + 180); at.setPitch(0);
        commit(state.put(NpcData.Definition.create(id, name, NpcRenderer.point(at), NpcRenderer.point(stand))));
    }
    public void edit(String id, Consumer<JsonObject> change, boolean enable) throws IOException {
        NpcData.Definition old = get(id);
        if (inUse(id)) throw new IllegalStateException("NPC sedang dialog, quest, pemindaian, atau pemulihan ladang. Tunggu selesai sebelum mengubahnya.");
        JsonObject json = NpcStore.JSON.toJsonTree(old).getAsJsonObject(); change.accept(json);
        json.addProperty("enabled", enable); NpcData.Definition next = NpcStore.JSON.fromJson(json, NpcData.Definition.class);
        if (!next.id().equals(old.id()) || !next.token().equals(old.token())) throw new IllegalArgumentException("Identitas NPC tidak boleh berubah.");
        validateRuntime(next);
        NpcData.State previous = state; commit(state.put(next));
        try { gates.refresh(); renderer.refresh(state.npcs()); if (next.enabled()) quests.restore(next); }
        catch (RuntimeException e) { commit(previous); gates.refresh(); renderer.refresh(previous.npcs()); throw e; }
    }
    private void validateRuntime(NpcData.Definition d) {
        validateRuntime(d, true);
    }
    private void validateRuntime(NpcData.Definition d, boolean requireWorld) {
        for (String item : d.gift().items()) decode(item);
        for (String item : d.quest().reward().items()) decode(item);
        if (d.quest().kind() == NpcData.QuestKind.MOB) NpcQuest.mobType(d.quest().mob());
        Set<String> plants = new HashSet<>(), soils = new HashSet<>();
        for (var crop : d.quest().crops()) {
            if (plants.add(crop.plant())) {
                var plant = Bukkit.createBlockData(crop.plant());
                if (!(plant instanceof org.bukkit.block.data.Ageable)
                        || !Set.of(Material.CARROTS, Material.POTATOES, Material.WHEAT, Material.BEETROOTS).contains(plant.getMaterial()))
                    throw new IllegalArgumentException("Snapshot ladang tidak valid.");
            }
            if (soils.add(crop.soil()) && Bukkit.createBlockData(crop.soil()).getMaterial() != Material.FARMLAND)
                throw new IllegalArgumentException("Snapshot ladang tidak valid.");
        }
        if (d.gate() != null) for (var cell : d.gate().blocks()) {
            Material material = Bukkit.createBlockData(cell.data()).getMaterial();
            if (!material.isSolid() || material.hasGravity() || material.isInteractable()
                    || Set.of(Material.BEDROCK, Material.BARRIER, Material.END_PORTAL_FRAME, Material.REINFORCED_DEEPSLATE).contains(material))
                throw new IllegalArgumentException("Snapshot gerbang harus berupa dinding solid biasa.");
        }
        if (requireWorld && d.enabled() && (Bukkit.getWorld(d.location().world()) == null || Bukkit.getWorld(d.stand().world()) == null))
            throw new IllegalArgumentException("Dunia NPC belum dimuat.");
    }
    public void delete(String id) throws IOException {
        get(id); if (inUse(id)) throw new IllegalStateException("NPC sedang dipakai."); commit(state.remove(id)); gates.refresh(); renderer.refresh(state.npcs());
    }
    public void setupFarm(Player admin, String id, com.bangzachery.vitae.vitaemanager.whisper.WhisperArea area, int goal) {
        NpcData.Definition before = get(id);
        if (inUse(id) || quests.farmBusy(area)) throw new IllegalStateException("NPC/ladang sedang dipakai atau diproses. Tunggu sampai selesai.");
        if (goal < 1 || goal > 50) throw new IllegalArgumentException("Jumlah putaran harus 1–50.");
        NpcData.Point start = NpcRenderer.point(admin.getLocation());
        if (!NpcData.inside(area, start)) throw new IllegalArgumentException("Berdiri di titik awal pemain di dalam area ladang sebelum menyimpan quest.");
        BooleanSupplier active = () -> !closed && admin.isOnline() && admin.hasPermission("vitae.admin.npc") && state.npcs().get(id) == before;
        NpcFarm.Handle work = quests.scan(area, active, (read, total) -> {
            if (admin.isOnline()) admin.sendActionBar(Component.text("Membaca ladang " + id + ": " + read + "/" + total + " blok", NamedTextColor.AQUA));
        }, crops -> {
            farmSetups.remove(id);
            if (!active.getAsBoolean()) return;
            try {
                NpcData.Reward reward = before.quest().kind() == NpcData.QuestKind.FARM ? before.quest().reward() : NpcData.Reward.none();
                NpcData.Quest quest = new NpcData.Quest(NpcData.QuestKind.FARM, area, start, goal, "COW", crops, reward);
                edit(id, json -> json.add("quest", NpcStore.JSON.toJsonTree(quest)), false);
                admin.sendActionBar(Component.empty());
                message(admin, "Ladang tersimpan: " + crops.size() + " tanaman, " + goal + " putaran. NPC sekarang OFF.");
                message(admin, "Contoh hadiah: /vnpc " + id + " reward quest viti 10000. Setelah setup selesai: /vnpc " + id + " on.");
            } catch (IOException | RuntimeException e) { message(admin, e.getMessage()); log(e); }
        }, reason -> {
            farmSetups.remove(id);
            if (admin.isOnline()) { admin.sendActionBar(Component.empty()); message(admin, reason + " Setup lama dipertahankan."); }
        });
        farmSetups.put(id, new FarmSetup(admin.getUniqueId(), work));
        message(admin, "Pemindaian ladang dimulai. Tunggu pesan 'Ladang tersimpan' sebelum menjalankan command hadiah/on. /vnpc " + id + " quest cancel untuk batal.");
    }
    public void cancelFarmSetup(String id) {
        get(id); FarmSetup setup = farmSetups.remove(id);
        if (setup == null) throw new IllegalArgumentException("NPC ini tidak sedang membaca ladang.");
        setup.work().cancel();
        Player admin = Bukkit.getPlayer(setup.admin());
        if (admin != null) { admin.sendActionBar(Component.empty()); message(admin, "Pemindaian dibatalkan. Setup ladang sebelumnya tetap tersimpan."); }
    }
    public void reset(UUID player, String id) throws IOException {
        if (quests.run(player) != null || sessions.containsKey(player)) throw new IllegalStateException("Pemain sedang dialog/quest.");
        commit(state.reset(player, get(id))); warned.remove(player); triggerRetry.remove(player);
    }
    public void preview(Player p, String id) {
        JsonObject json = NpcStore.JSON.toJsonTree(get(id)).getAsJsonObject(); json.addProperty("enabled", true);
        NpcData.Definition checked = NpcStore.JSON.fromJson(json, NpcData.Definition.class);
        validateRuntime(checked); begin(p, checked, false, true, null);
    }
    public void skin(Player admin, String id, String name) {
        NpcData.Definition before = get(id);
        Player online = Bukkit.getPlayerExact(name);
        com.destroystokyo.paper.profile.PlayerProfile profile = online == null ? Bukkit.createProfile(name) : online.getPlayerProfile();
        Consumer<com.destroystokyo.paper.profile.PlayerProfile> apply = loaded -> {
            if (closed || !admin.isOnline() || !admin.hasPermission("vitae.admin.npc")) return;
            try {
                if (!get(id).equals(before)) throw new IllegalStateException("NPC berubah saat mengambil skin; ulangi perintah.");
                var texture = loaded.getProperties().stream().filter(v -> v.getName().equals("textures")).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Skin tidak ditemukan. Coba nama akun premium atau pemain online."));
                edit(id, json -> { json.addProperty("kind", "HUMAN"); json.addProperty("texture", texture.getValue());
                    json.addProperty("signature", Objects.requireNonNullElse(texture.getSignature(), "")); }, false);
                message(admin, "Skin tersimpan. Jalankan /vnpc " + id + " on setelah setup selesai.");
            } catch (Exception e) { message(admin, e.getMessage()); }
        };
        if (profile.hasTextures()) apply.accept(profile);
        else profile.update().whenComplete((loaded, error) -> {
            if (!closed) Bukkit.getScheduler().runTask(plugin, () -> {
                if (error != null || !(loaded instanceof com.destroystokyo.paper.profile.PlayerProfile paper)) message(admin, "Pengambilan skin gagal; coba lagi nanti.");
                else apply.accept(paper);
            });
        });
    }
    public void quitQuest(Player p) { quests.cancel(p.getUniqueId(), "Quest dibatalkan. Percobaan gagal tidak mengurangi kuota.", true); }
    public void gateSelect(Player admin, String id, boolean first) { get(id); gates.arm(admin, id, first); }
    public void gateCancel(Player admin) { gates.cancelSelection(admin); }
    public void gateSet(Player admin, String id) throws IOException {
        if (get(id).gate() != null) throw new IllegalArgumentException("Gerbang sudah ada. Gunakan gate off sebelum mengganti seleksi.");
        NpcData.Gate gate = gates.capture(admin, id);
        edit(id, json -> { json.add("gate", NpcStore.JSON.toJsonTree(gate)); json.add("trigger", JsonNull.INSTANCE); }, false);
    }
    public void gateReset(String id) throws IOException {
        if (inUse(id)) throw new IllegalStateException("NPC sedang dialog, quest, atau animasi. Tunggu selesai sebelum reset gerbang.");
        NpcData.Gate gate = get(id).gate();
        if (gate == null) throw new IllegalArgumentException("Gerbang belum diatur.");
        gates.reset(id, () -> edit(id, json -> json.add("gate", NpcStore.JSON.toJsonTree(gate.withStatus(NpcData.GateStatus.CLOSED))), false));
    }
    public void reloadModule() throws IOException {
        if (state.npcs().keySet().stream().anyMatch(this::inUse)) throw new IllegalStateException("Selesaikan dialog/quest aktif sebelum reload NPC.");
        NpcModule.stop(plugin); NpcModule.start(plugin, viti);
    }
    public void status(Player p) {
        NpcQuest.Run r = quests.run(p.getUniqueId()); var quota = state.quota(p.getUniqueId(), System.currentTimeMillis());
        message(p, r == null ? "Tidak ada quest aktif. Selesai: " + quota.count() + "/3."
                : "Quest " + NpcText.plain(r.npc.name()) + ": " + r.count + "/" + r.npc.quest().goal());
        if (quota.count() == 3) message(p, "Cooldown tersisa " + Math.max(1, (quota.until() - System.currentTimeMillis() + 999) / 1000) + " detik.");
    }
    private void tick() {
        if (closed) return; ticks++;
        if (ticks % 10 == 0) renderer.tick(); quests.tick(); gates.tick();
        for (Session s : List.copyOf(sessions.values())) {
            try {
            if (!s.player.isOnline() || s.player.isDead() || System.currentTimeMillis() >= s.deadline) { end(s, false); continue; }
            if (ticks % 2 == 0 && s.npc.appearance().track()) renderer.lookAt(s.npc.id(), s.player);
            if (s.phase == 0) {
                if (++s.age < 12) continue;
                Location target = NpcRenderer.location(s.npc.stand());
                if (!target.getBlock().isPassable() || !target.clone().add(0, 1, 0).getBlock().isPassable()
                        || !teleport(s.player, target)) {
                    triggerRetry.put(s.player.getUniqueId(), ticks + 40);
                    message(s.player, "Teleport dialog gagal. Admin: cek /vnpc " + s.npc.id() + " stand dan izin teleport plugin proteksi.");
                    end(s, true); continue;
                }
                removeBlindness(s); if (s.blackout != null) { s.blackout.remove(); s.blackout = null; }
                s.anchor = s.player.getLocation().clone(); s.phase = 1; s.age = 0; aim(s);
            } else if (s.phase == 1) {
                float t = Math.min(1, ++s.age / 12f);
                s.anchor.setYaw(s.fromYaw + shortest(s.goalYaw - s.fromYaw) * t);
                s.anchor.setPitch(s.fromPitch + (s.goalPitch - s.fromPitch) * t);
                if (!teleport(s.player, s.anchor)) { end(s, false); continue; }
                if (s.age >= 12) { s.phase = 2; s.age = 0; display(s); }
            } else if (ticks % 2 == 0) {
                int length = s.node.text().codePointCount(0, s.node.text().length());
                if (s.shown < length) { s.shown = Math.min(length, s.shown + s.npc.speed()); sound(s.player, "minecraft:block.note_block.hat", .15f, 1.4f); }
                render(s);
            }
            } catch (RuntimeException e) {
                triggerRetry.put(s.player.getUniqueId(), ticks + 40);
                end(s, false); message(s.player, "Dialog dihentikan karena error. Kamu sudah dilepas; minta admin memeriksa log."); log(e);
            }
        }
        if (ticks % 20 == 0) for (Player p : Bukkit.getOnlinePlayers()) pay(p);
    }
    private static float shortest(float angle) { return (angle % 360 + 540) % 360 - 180; }
    private void aim(Session s) {
        s.fromYaw = s.anchor.getYaw(); s.fromPitch = s.anchor.getPitch();
        Vector delta = NpcRenderer.location(s.npc.location()).add(0, s.npc.eyeHeight(), 0).toVector()
                .subtract(s.anchor.clone().add(0, s.player.getEyeHeight(), 0).toVector());
        Location look = s.anchor.clone().setDirection(delta); s.goalYaw = look.getYaw(); s.goalPitch = look.getPitch();
    }
    private void display(Session s) {
        Location at = s.anchor.clone().add(0, s.player.getEyeHeight(), 0).add(s.anchor.getDirection().multiply(1.5)).add(0, -.4, 0);
        s.display = at.getWorld().spawn(at, TextDisplay.class, e -> {
            e.setPersistent(false); e.setVisibleByDefault(false); e.setGravity(false); e.setInvulnerable(true);
            e.setBillboard(Display.Billboard.CENTER); e.setSeeThrough(true); e.setShadowed(true); e.setLineWidth(230);
            e.setBackgroundColor(Color.fromARGB(210, 10, 10, 15));
            var transform = e.getTransformation(); transform.getScale().set(new Vector3f(.30f)); e.setTransformation(transform);
        });
        s.player.showEntity(plugin, s.display); render(s);
    }
    private void render(Session s) {
        if (s.display == null) return;
        int total = s.node.text().codePointCount(0, s.node.text().length()); int end = s.node.text().offsetByCodePoints(0, s.shown);
        Component text = NpcText.dialogName(s.npc.name()).decorate(TextDecoration.BOLD).append(Component.newline())
                .append(Component.text(s.node.text().substring(0, end), NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false));
        if (s.shown >= total) {
            List<NpcData.Choice> choices = s.node.available(); int start = Math.max(0, Math.min(s.selected - 1, choices.size() - 3));
            for (int i = start; i < Math.min(choices.size(), start + 3); i++) text = text.append(Component.text("\n" + (i == s.selected ? "▶ " : "  ") + choices.get(i).label(), i == s.selected ? NamedTextColor.YELLOW : NamedTextColor.GRAY));
            s.player.sendActionBar(Component.text("Scroll: pilih • Klik kiri/kanan: pilih • Shift: batal", NamedTextColor.GRAY));
        } else s.player.sendActionBar(Component.text("Dengarkan dialog... • Shift: batal", NamedTextColor.GRAY));
        s.display.text(text);
    }
    private boolean begin(Player p, NpcData.Definition d, boolean trigger, boolean preview, String specialText) {
        if (closed || pushes.containsKey(p.getUniqueId()) || (!preview && !d.enabled()) || (!preview && !p.hasPermission("vitae.npc"))
                || p.isDead() || p.getGameMode() == GameMode.SPECTATOR || p.getVehicle() != null || !p.getPassengers().isEmpty()
                || sessions.containsKey(p.getUniqueId()) || quests.run(p.getUniqueId()) != null
                || p.getOpenInventory().getTopInventory().getHolder() instanceof RewardMenu) return false;
        if (!p.getWorld().getUID().equals(d.location().world())) return false;
        if (!trigger && !preview && p.getLocation().distanceSquared(NpcRenderer.location(d.location())) > 25) return false;
        if (!preview && state.pending().containsKey(p.getUniqueId())) { message(p, "Hadiah sebelumnya sedang dikirim. Tunggu sebentar."); return false; }
        if (!preview && specialText == null) {
            var quota = state.quota(p.getUniqueId(), System.currentTimeMillis());
            if (d.quest().kind() != NpcData.QuestKind.NONE && quota.count() == 3) {
                if (Objects.equals(warned.get(p.getUniqueId()), quota.until())) { push(p, d); return false; }
                warned.put(p.getUniqueId(), quota.until()); specialText = "Kamu sudah menyelesaikan tiga quest. Istirahat dulu; kembali dalam "
                        + Math.max(1, (quota.until() - System.currentTimeMillis() + 999) / 1000) + " detik.";
            } else if (d.gate() != null && d.gate().status() != NpcData.GateStatus.CLOSED) {
                specialText = switch (d.gate().status()) {
                    case OPEN -> d.repeat();
                    case OPENING -> "Gerbang sedang dibuka. Tunggu sebentar.";
                    case CLOSING -> "Gerbang sedang ditutup. Tunggu sampai selesai.";
                    default -> null;
                };
            } else if (d.gate() == null && d.quest().kind() == NpcData.QuestKind.NONE && state.done(p.getUniqueId(), d)
                    && d.memory().isEmpty()) specialText = d.repeat();
        }
        Session s = new Session(p, d, preview, specialText != null, trigger);
        s.node = specialText == null ? d.nodes().get(state.recalledRoot(p.getUniqueId(), d)) : new NpcData.Node(specialText, List.of());
        sessions.put(p.getUniqueId(), s);
        if (d.appearance().track()) renderer.lookAt(d.id(), p);
        try { if (trigger) {
            blackout(s);
        } else aim(s); }
        catch (RuntimeException e) { end(s, false); throw e; }
        return true;
    }
    void blackout(Session s) {
            Location blackAt = s.player.getEyeLocation().add(s.player.getEyeLocation().getDirection());
            s.blackout = blackAt.getWorld().spawn(blackAt, TextDisplay.class, e -> {
                e.setPersistent(false); e.setVisibleByDefault(false); e.setBillboard(Display.Billboard.CENTER);
                e.setSeeThrough(true); e.setBackgroundColor(Color.fromARGB(255, 0, 0, 0));
                e.text(Component.text("                                        \n                                        \n                                        "));
                var t = e.getTransformation(); t.getScale().set(new Vector3f(100)); e.setTransformation(t);
            });
            s.player.showEntity(plugin, s.blackout);
            if (!s.player.hasPotionEffect(PotionEffectType.BLINDNESS)) { s.player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0, false, false)); s.blind = true; }
    }
    private void modelClick(Player p, String id, boolean attack) {
        if (sessions.containsKey(p.getUniqueId())) confirm(p); else click(p, id);
    }
    private void click(Player p, String id) {
        if (gates.selecting(p.getUniqueId())) return;
        if (ticks - clicks.getOrDefault(p.getUniqueId(), -100L) < 6) return; clicks.put(p.getUniqueId(), ticks);
        begin(p, get(id), false, false, null);
    }
    private void confirm(Player p) {
        Session s = sessions.get(p.getUniqueId());
        if (s == null || s.phase != 2 || s.shown < s.node.text().codePointCount(0, s.node.text().length())
                || ticks - clicks.getOrDefault(p.getUniqueId(), -100L) < 4) return;
        clicks.put(p.getUniqueId(), ticks); NpcData.Choice c = s.node.available().get(s.selected);
        sound(p, "minecraft:entity.experience_orb.pickup", .4f, 1.3f);
        if (c.target().equals("END")) {
            try {
                if (!s.preview && !s.special) commit(state.remember(p.getUniqueId(), s.npc, c).finish(p.getUniqueId(), s.npc, false, UUID.randomUUID(), System.currentTimeMillis()));
                end(s, false); pay(p);
            } catch (IOException e) { message(p, "Progres belum tersimpan; coba pilih kembali."); log(e); }
        } else if (c.target().equals("OPEN")) {
            if (s.preview) { message(p, "Preview selesai. Gerbang dan hadiah tidak dijalankan saat preview."); end(s, false); return; }
            try {
                boolean first = gates.open(s.npc.id(), opening -> commit(state.remember(p.getUniqueId(), s.npc, c).finish(p.getUniqueId(), s.npc, false, UUID.randomUUID(), System.currentTimeMillis())
                        .put(get(s.npc.id()).withGate(opening))));
                if (!first) commit(state.remember(p.getUniqueId(), s.npc, c).finish(p.getUniqueId(), s.npc, false, UUID.randomUUID(), System.currentTimeMillis()));
                end(s, false); pay(p);
                message(p, first ? "Gerbang mulai bergetar. Tunggu sampai terbuka!" : "Gerbang sudah dibuka atau sedang dibuka pemain lain.");
            } catch (IOException | RuntimeException e) {
                page(s, new NpcData.Node("Gerbang belum bisa dibuka. Minta admin memeriksa setup; kamu boleh mencoba lagi.", List.of())); log(e);
            }
        } else if (c.target().equals("QUEST")) {
            if (s.preview) { message(p, "Preview selesai. Quest dan hadiah tidak dijalankan saat preview."); end(s, false); return; }
            String refusal = p.hasPermission("vitae.quest") ? quests.refusal(p, s.npc) : "Kamu tidak memiliki izin quest.";
            var quota = state.quota(p.getUniqueId(), System.currentTimeMillis()); if (quota.count() == 3) refusal = "Kamu masih cooldown.";
            if (refusal != null) { page(s, new NpcData.Node(refusal, List.of())); return; }
            end(s, false);
            if (quests.start(p, s.npc)) {
                try { commit(state.remember(p.getUniqueId(), s.npc, c).heard(p.getUniqueId(), s.npc)); }
                catch (IOException e) { quests.cancel(p.getUniqueId(), "Progres gagal disimpan; coba kembali.", true); log(e); }
            }
        } else {
            try {
                if (!s.preview && !s.special && !c.memoryKey().isEmpty()) commit(state.remember(p.getUniqueId(), s.npc, c));
                page(s, s.npc.nodes().get(c.target()));
            } catch (IOException e) { message(p, "Pilihan belum tersimpan; coba kembali."); log(e); }
        }
    }
    private void page(Session s, NpcData.Node node) { s.node = node; s.shown = 0; s.selected = 0; render(s); }
    private void end(Session s, boolean sound) {
        sessions.remove(s.player.getUniqueId()); renderer.endLook(s.npc.id(), s.player); removeBlindness(s); if (s.blackout != null) s.blackout.remove(); if (s.display != null) s.display.remove();
        s.player.sendActionBar(Component.empty()); if (sound) sound(s.player, "minecraft:entity.villager.no", .45f, 1);
    }
    private void removeBlindness(Session s) {
        if (s.blind) { var effect = s.player.getPotionEffect(PotionEffectType.BLINDNESS);
            if (effect != null && effect.getAmplifier() == 0 && effect.getDuration() <= 30) s.player.removePotionEffect(PotionEffectType.BLINDNESS); s.blind = false; }
    }
    private void questComplete(NpcQuest.Run run) {
        Player p = Bukkit.getPlayer(run.player); if (p == null) { quests.cancel(run.player, "", false); return; }
        try {
            commit(state.finish(run.player, run.npc, true, UUID.randomUUID(), System.currentTimeMillis()));
            quests.finish(run); teleport(p, NpcRenderer.location(run.npc.stand()));
            begin(p, run.npc, false, true, "Kerja bagus! Kamu telah menyelesaikan questku."); pay(p);
        } catch (IOException | RuntimeException e) { log(e); quests.retry(run); message(p, "Penyimpanan hadiah gagal; gelombang terakhir akan diulang."); }
    }
    private boolean teleport(Player p, Location location) {
        internalTeleport.add(p.getUniqueId());
        try { return p.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN); } finally { internalTeleport.remove(p.getUniqueId()); }
    }
    private void push(Player p, NpcData.Definition d) {
        message(p, NpcText.plain(d.name()) + ": Sudah kubilang istirahat dulu! Hus, hus!" );
        Location from = p.getLocation(); Vector dir = from.toVector().subtract(NpcRenderer.location(d.location()).toVector()).setY(0);
        if (dir.lengthSquared() < .001) dir = from.getDirection().setY(0);
        if (dir.lengthSquared() < .001) dir = new Vector(0, 0, 1); dir.normalize(); final Vector away = dir;
        final int[] count = {0}; final BukkitTask[] animation = new BukkitTask[1];
        animation[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (closed || !p.isOnline() || p.isDead() || ++count[0] > 12 || sessions.containsKey(p.getUniqueId()) || quests.run(p.getUniqueId()) != null) { animation[0].cancel(); pushes.remove(p.getUniqueId()); return; }
            double t = count[0] / 12.0; Location to = from.clone().add(away.clone().multiply(3 * t)).add(0, .8 * Math.sin(Math.PI * t), 0);
            if (!to.getBlock().isPassable() || !to.clone().add(0, 1, 0).getBlock().isPassable() || !teleport(p, to)) { animation[0].cancel(); pushes.remove(p.getUniqueId()); return; }
            p.setFallDistance(0);
        }, 1, 1);
        pushes.put(p.getUniqueId(), animation[0]);
    }
    private void pay(Player p) {
        var award = state.pending().get(p.getUniqueId()); if (award == null || !paying.add(p.getUniqueId())) return;
        if (award.reward().viti().signum() > 0) {
            viti.reward(p, award.receipt(), award.reward().viti(), key -> {
                try { if (!closed && key.equals("viti-success")) {
                    if (p.isOnline()) notifyAward(p, award); commit(state.acknowledge(p.getUniqueId(), award.receipt()));
                } } catch (Exception e) { log(e); } finally { paying.remove(p.getUniqueId()); }
            });
        } else {
            ItemStack[] before = Arrays.stream(p.getInventory().getStorageContents()).map(i -> i == null ? null : i.clone()).toArray(ItemStack[]::new);
            var dropped = new ArrayList<Item>();
            String marker = p.getPersistentDataContainer().get(awardKey, PersistentDataType.STRING);
            try {
                if (!award.receipt().toString().equals(marker)) {
                    ItemStack[] items = award.reward().items().stream().map(NpcService::decode).toArray(ItemStack[]::new);
                    for (ItemStack item : p.getInventory().addItem(items).values()) dropped.add(dropAward(p, item, award.receipt()));
                    notifyAward(p, award);
                }
                commit(state.acknowledge(p.getUniqueId(), award.receipt()));
            } catch (RuntimeException e) {
                p.getInventory().setStorageContents(before); for (Item item : dropped) item.remove();
                if (marker == null) p.getPersistentDataContainer().remove(awardKey); else p.getPersistentDataContainer().set(awardKey, PersistentDataType.STRING, marker);
                log(e);
            } catch (IOException e) { log(e); } finally { paying.remove(p.getUniqueId()); }
        }
    }
    private Item dropAward(Player player, ItemStack stack, UUID receipt) {
        Item item = player.getWorld().dropItem(player.getLocation(), stack.clone());
        item.setOwner(player.getUniqueId()); item.setPickupDelay(20);
        item.setUnlimitedLifetime(true); item.setInvulnerable(true);
        item.getPersistentDataContainer().set(awardKey, PersistentDataType.STRING, receipt.toString()); return item;
    }
    private void notifyAward(Player p, NpcData.Award award) {
        if (award.receipt().toString().equals(p.getPersistentDataContainer().get(awardKey, PersistentDataType.STRING))) return;
        p.getPersistentDataContainer().set(awardKey, PersistentDataType.STRING, award.receipt().toString()); p.saveData();
        p.sendMessage(NpcText.name(award.npcName()).append(Component.text(": " + (award.reward().viti().signum() > 0
                ? "Aku memberikan " + VitiAmount.format(award.reward().viti()) + " Viti kepadamu." : "Hadiahku sudah diberikan. Item yang tidak muat jatuh di dekatmu."), NamedTextColor.WHITE)));
        sound(p, "minecraft:entity.player.levelup", .4f, 1);
    }
    public static ItemStack decode(String bytes) {
        byte[] data = Base64.getDecoder().decode(bytes);
        if (data.length > 1_500_000) throw new IllegalArgumentException("Item terlalu besar.");
        ItemStack result = ItemStack.deserializeBytes(data); if (result.getType().isAir() || result.getAmount() <= 0) throw new IllegalArgumentException("Item hadiah kosong."); return result;
    }
    public void items(Player p, String id, boolean quest) {
        NpcData.Definition d = get(id);
        if (inUse(id)) throw new IllegalStateException("NPC sedang dipakai.");
        if (quest && (d.quest().kind() == NpcData.QuestKind.NONE || d.quest().kind() == NpcData.QuestKind.FARM)) throw new IllegalArgumentException("Hadiah ladang wajib Viti; konfigurasi quest mob dahulu.");
        p.openInventory(new RewardMenu(p.getUniqueId(), d, quest).inventory);
    }
    private static final class RewardMenu implements VitaeMenu {
        final UUID admin; final NpcData.Definition before; final boolean quest; final Inventory inventory;
        RewardMenu(UUID admin, NpcData.Definition before, boolean quest) {
            this.admin = admin; this.before = before; this.quest = quest;
            inventory = Bukkit.createInventory(this, 27, Component.text("Letakkan item hadiah • tutup: simpan"));
        }
        @Override public Inventory getInventory() { return inventory; }
    }
    @EventHandler public void menuClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof RewardMenu menu) || !(e.getPlayer() instanceof Player p)) return;
        var templates = new ArrayList<String>();
        for (ItemStack item : e.getInventory().getContents()) if (item != null && !item.getType().isAir()) {
            ItemStack copy = item.clone(); templates.add(Base64.getEncoder().encodeToString(copy.serializeAsBytes()));
            for (ItemStack overflow : p.getInventory().addItem(copy).values()) p.getWorld().dropItem(p.getLocation(), overflow);
        }
        e.getInventory().clear();
        if (closed) return;
        try {
            if (!p.getUniqueId().equals(menu.admin) || !p.hasPermission("vitae.admin.npc") || !get(menu.before.id()).equals(menu.before))
                throw new IllegalStateException("NPC atau izin berubah. Setup hadiah tidak disimpan.");
            NpcData.Reward reward = new NpcData.Reward(java.math.BigDecimal.ZERO, templates);
            edit(menu.before.id(), json -> { if (menu.quest) json.getAsJsonObject("quest").add("reward", NpcStore.JSON.toJsonTree(reward)); else json.add("gift", NpcStore.JSON.toJsonTree(reward)); }, false);
            message(p, "Template hadiah disimpan. Item admin dikembalikan. Jalankan /vnpc " + menu.before.id() + " on.");
        } catch (Exception exception) { message(p, exception.getMessage()); }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void move(PlayerMoveEvent e) {
        if (internalTeleport.contains(e.getPlayer().getUniqueId())) return;
        Session s = sessions.get(e.getPlayer().getUniqueId());
        if (s != null && !(e instanceof PlayerTeleportEvent)) e.setTo(s.anchor.clone());
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void trigger(PlayerMoveEvent e) {
        Player player = e.getPlayer(); UUID owner = player.getUniqueId();
        if (e.isCancelled() || e instanceof PlayerTeleportEvent || internalTeleport.contains(owner) || e.getTo() == null || !e.hasChangedPosition()
                || sessions.containsKey(owner) || queuedTriggers.contains(owner) || quests.run(owner) != null
                || ticks < triggerRetry.getOrDefault(owner, 0L)) return;
        for (var d : state.npcs().values()) if (d.enabled() && d.trigger() != null && !state.done(e.getPlayer().getUniqueId(), d)
                && (d.trigger().contains(e.getTo().getWorld().getUID(), e.getTo().getX(), e.getTo().getY(), e.getTo().getZ())
                || d.trigger().crossed(e.getFrom().getWorld().getUID(), e.getFrom().getX(), e.getFrom().getY(), e.getFrom().getZ(), e.getTo().getX(), e.getTo().getY(), e.getTo().getZ()))) {
            queuedTriggers.add(owner);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!queuedTriggers.remove(owner) || closed || !player.isOnline() || player.isDead()) return;
                NpcData.Definition live = state.npcs().get(d.id());
                if (live == null || !live.enabled() || !live.token().equals(d.token()) || live.trigger() == null || state.done(owner, live)) return;
                try { begin(player, live, true, false, null); }
                catch (RuntimeException error) {
                    Session session = sessions.get(owner); if (session != null) end(session, false);
                    triggerRetry.put(owner, ticks + 40); message(player, "Trigger dialog gagal; pemain sudah dilepas. Minta admin memeriksa log."); log(error);
                }
            });
            break;
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void externalTeleport(PlayerTeleportEvent e) {
        if (internalTeleport.contains(e.getPlayer().getUniqueId())) return;
        queuedTriggers.remove(e.getPlayer().getUniqueId());
        Session s = sessions.get(e.getPlayer().getUniqueId()); if (s != null) end(s, false);
        quests.cancel(e.getPlayer().getUniqueId(), "Teleport membatalkan quest.", false); renderer.forget(e.getPlayer());
    }
    @EventHandler(priority = EventPriority.LOWEST) public void held(PlayerItemHeldEvent e) {
        Session s = sessions.get(e.getPlayer().getUniqueId()); if (s == null) return; e.setCancelled(true);
        if (s.phase != 2 || s.shown < s.node.text().codePointCount(0, s.node.text().length())) return;
        int delta = Math.floorMod(e.getNewSlot() - e.getPreviousSlot() + 4, 9) - 4;
        if (delta == 0) return; s.selected = Math.floorMod(s.selected + (delta > 0 ? 1 : -1), s.node.available().size());
        sound(s.player, "minecraft:ui.button.click", .3f, 1.2f); render(s);
    }
    @EventHandler(priority = EventPriority.LOWEST) public void sneak(PlayerToggleSneakEvent e) {
        Session s = sessions.get(e.getPlayer().getUniqueId()); if (s != null && e.isSneaking()) { e.setCancelled(true); end(s, true); }
    }
    @EventHandler(priority = EventPriority.LOWEST) public void swing(PlayerAnimationEvent e) {
        if (e.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        if (gates.selecting(e.getPlayer().getUniqueId())) return;
        Player p = e.getPlayer(); if (sessions.containsKey(p.getUniqueId())) { e.setCancelled(true); confirm(p); return; }
        var hit = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getEyeLocation().getDirection(), 4.5, .1,
                entity -> entity != p && renderer.id(entity) != null);
        if (hit != null && hit.getHitEntity() != null) click(p, renderer.id(hit.getHitEntity()));
    }
    @EventHandler(priority = EventPriority.LOWEST) public void interact(PlayerInteractEvent e) {
        if (!sessions.containsKey(e.getPlayer().getUniqueId())) return; e.setCancelled(true);
        if (e.getHand() == EquipmentSlot.HAND && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) confirm(e.getPlayer());
    }
    @EventHandler(priority = EventPriority.LOWEST) public void entityInteract(PlayerInteractEntityEvent e) {
        String id = renderer.id(e.getRightClicked());
        boolean dialog = sessions.containsKey(e.getPlayer().getUniqueId());
        if (id == null && !dialog) return;
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (dialog) confirm(e.getPlayer()); else click(e.getPlayer(), id);
    }
    @EventHandler(priority = EventPriority.LOWEST) public void entityInteractAt(PlayerInteractAtEntityEvent e) { entityInteract(e); }
    @EventHandler(priority = EventPriority.LOWEST) public void virtualInteract(PlayerUseUnknownEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        String id = renderer.id(e.getPlayer(), e.getEntityId());
        if (id != null) modelClick(e.getPlayer(), id, e.isAttack());
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void damage(EntityDamageEvent e) {
        if (renderer.id(e.getEntity()) != null || sessions.containsKey(e.getEntity().getUniqueId())) e.setCancelled(true);
        if (e instanceof EntityDamageByEntityEvent hit && hit.getDamager() instanceof Player p) {
            String id = renderer.id(e.getEntity()); if (id != null) { e.setCancelled(true); modelClick(p, id, true); }
            if (sessions.containsKey(p.getUniqueId())) e.setCancelled(true);
        }
    }
    @EventHandler(priority = EventPriority.LOWEST) public void inventory(InventoryClickEvent e) { if (sessions.containsKey(e.getWhoClicked().getUniqueId())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.LOWEST) public void drag(InventoryDragEvent e) { if (sessions.containsKey(e.getWhoClicked().getUniqueId())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.LOWEST) public void drop(PlayerDropItemEvent e) { if (sessions.containsKey(e.getPlayer().getUniqueId())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.LOWEST) public void swap(PlayerSwapHandItemsEvent e) { if (sessions.containsKey(e.getPlayer().getUniqueId())) e.setCancelled(true); }
    @EventHandler public void death(PlayerDeathEvent e) { disconnect(e.getEntity()); }
    @EventHandler public void quit(PlayerQuitEvent e) { disconnect(e.getPlayer()); clicks.remove(e.getPlayer().getUniqueId()); warned.remove(e.getPlayer().getUniqueId()); triggerRetry.remove(e.getPlayer().getUniqueId()); }
    @EventHandler public void respawn(PlayerRespawnEvent e) { renderer.forget(e.getPlayer()); }
    @EventHandler public void world(PlayerChangedWorldEvent e) { renderer.forget(e.getPlayer()); }
    @EventHandler public void worldLoad(WorldLoadEvent e) { renderer.refresh(state.npcs()); for (var d : state.npcs().values()) if (d.enabled() && !quests.uses(d.id())) quests.restore(d); }
    private void disconnect(Player p) { queuedTriggers.remove(p.getUniqueId()); BukkitTask push = pushes.remove(p.getUniqueId()); if (push != null) push.cancel(); Session s = sessions.get(p.getUniqueId()); if (s != null) end(s, false); quests.cancel(p.getUniqueId(), "", false); renderer.forget(p); }
    private static void sound(Player p, String sound, float volume, float pitch) { p.playSound(p.getLocation(), sound, volume, pitch); }
    public static void message(Player p, String text) { p.sendMessage(Component.text("[Vitae NPC] " + Objects.requireNonNullElse(text, "Setup gagal."), NamedTextColor.YELLOW)); }
    private void log(Exception e) { plugin.getLogger().log(Level.SEVERE, "Operasi NPC gagal; progres/hadiah belum dihapus.", e); }
    @Override public void close() {
        if (closed) return; closed = true; if (task != null) task.cancel();
        for (Player p : Bukkit.getOnlinePlayers()) if (p.getOpenInventory().getTopInventory().getHolder() instanceof RewardMenu) p.closeInventory();
        for (Session s : List.copyOf(sessions.values())) end(s, false);
        for (BukkitTask push : pushes.values()) push.cancel(); pushes.clear();
        queuedTriggers.clear(); triggerRetry.clear(); gates.close(); quests.close(); renderer.close(); HandlerList.unregisterAll(this);
        farmSetups.clear();
    }
}
'@
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcStore.java'
        OriginalHash = 'f52a700aa8996c66af6e4a557430f83573c629a553efe15b2f9f05bc7931d524'
        Hash = '534da37797d029896c4e485ab668184de664cb97858ace08e310e030defadb4d'
        Source = @'
package com.bangzachery.vitae.vitaemanager.npc;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import java.io.*;
import java.lang.reflect.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Invalid files are rejected, never replaced with empty state. */
public final class NpcStore {
    public static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Path file;
    public NpcStore(Path file) { this.file = file; }
    public NpcData.State initialize() throws IOException {
        if (!Files.exists(file)) { var state = NpcData.State.empty(); save(state); return state; }
        JsonElement json;
        NpcData.State state;
        try (var reader = new JsonReader(Files.newBufferedReader(file, StandardCharsets.UTF_8))) {
            reader.setStrictness(Strictness.STRICT);
            json = JsonParser.parseReader(reader);
            if (reader.peek() != com.google.gson.stream.JsonToken.END_DOCUMENT) throw new IllegalArgumentException("Data tambahan setelah JSON.");
            validate(json, NpcData.State.class);
            state = Objects.requireNonNull(JSON.fromJson(json, NpcData.State.class));
        } catch (RuntimeException e) { throw new IOException("npc.json tidak valid. File lama dipertahankan.", e); }
        if (needsUpgrade(json)) {
            Path backup = file.resolveSibling("npc-before-story-update-" + UUID.randomUUID() + ".json.bak");
            Files.copy(file, backup); // Close the reader before replacing a file on Windows.
            save(state);
        }
        return state;
    }
    public void save(NpcData.State state) throws IOException {
        Path parent = file.toAbsolutePath().getParent(); Files.createDirectories(parent);
        Path temp = Files.createTempFile(parent, "npc-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) { JSON.toJson(state, writer); }
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE)) { channel.force(true); }
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }
    private static boolean needsUpgrade(JsonElement value) {
        if (value.isJsonArray()) { for (JsonElement child : value.getAsJsonArray()) if (needsUpgrade(child)) return true; }
        if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            if (object.has("npcs") && !object.has("memories") || object.has("token") && (!object.has("appearance") || !object.has("memory"))
                    || object.has("blocks") && !object.has("timing") || object.has("label") && !object.has("memoryKey")) return true;
            for (var entry : object.entrySet()) if (needsUpgrade(entry.getValue())) return true;
        }
        return false;
    }
    private static void validate(JsonElement value, Type type) {
        if (value == null || value.isJsonNull()) {
            if (type == NpcData.Point.class || type == NpcData.Gate.class || type == com.bangzachery.vitae.vitaemanager.whisper.WhisperArea.class) return;
            throw new IllegalArgumentException("Nilai wajib tidak boleh kosong: " + type);
        }
        if (type instanceof ParameterizedType p) {
            Type[] args = p.getActualTypeArguments();
            if (p.getRawType() == Map.class) {
                if (!value.isJsonObject()) throw new IllegalArgumentException("Map harus berupa object.");
                for (var entry : value.getAsJsonObject().entrySet()) {
                    if (args[0] == UUID.class) UUID.fromString(entry.getKey());
                    validate(entry.getValue(), args[1]);
                }
            } else {
                if (!value.isJsonArray()) throw new IllegalArgumentException("List harus berupa array.");
                for (JsonElement child : value.getAsJsonArray()) validate(child, args[0]);
            }
            return;
        }
        Class<?> c = (Class<?>) type;
        if (c.isRecord()) {
            if (!value.isJsonObject()) throw new IllegalArgumentException("Record harus berupa object.");
            for (RecordComponent field : c.getRecordComponents()) {
                // Older npc.json versions have no orientation offset; their default is zero.
                if (c == NpcData.Definition.class && Set.of("facing", "gate", "appearance", "memory").contains(field.getName()) && !value.getAsJsonObject().has(field.getName())) continue;
                if (c == NpcData.State.class && field.getName().equals("memories") && !value.getAsJsonObject().has(field.getName())) continue;
                if (c == NpcData.Gate.class && field.getName().equals("timing") && !value.getAsJsonObject().has(field.getName())) continue;
                if (c == NpcData.Choice.class && Set.of("memoryKey", "memoryValue").contains(field.getName()) && !value.getAsJsonObject().has(field.getName())) continue;
                validate(value.getAsJsonObject().get(field.getName()), field.getGenericType());
            }
        } else if (c == boolean.class) {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("Harus boolean.");
        } else if (c == int.class || c == long.class || c == float.class || c == double.class || c == java.math.BigDecimal.class) {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Harus angka.");
            var number = value.getAsBigDecimal();
            if (c == int.class) number.intValueExact(); if (c == long.class) number.longValueExact();
        } else {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("Harus teks.");
            if (c == UUID.class) UUID.fromString(value.getAsString());
        }
    }
}
'@
    },
    [PSCustomObject]@{
        Name = 'src/test/java/com/bangzachery/vitae/vitaemanager/npc/NpcFarmDataTest.java'
        OriginalHash = ''
        Hash = 'd7fa7558381057ce95d6b25e4c44317f4b9486f1f0a3c76b955c49b7e31b491b'
        Source = @'
package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.whisper.WhisperArea;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import static com.bangzachery.vitae.vitaemanager.npc.NpcData.*;
import static org.junit.jupiter.api.Assertions.*;

class NpcFarmDataTest {
    @TempDir Path directory;
    static final UUID WORLD = UUID.fromString("22222222-2222-2222-2222-222222222222");
    static final WhisperArea AREA = WhisperArea.between(WORLD, -6323, 67, -4180, -6364, 72, -4235);
    static final Point START = new Point(WORLD, -6340.5, 68, -4210.5, 0, 0);
    static List<Crop> crops(int count) {
        List<Crop> crops = new ArrayList<>();
        for (int i = 0; i < count; i++) crops.add(new Crop(-6364 + i % 42, 68, -4235 + i / 42,
                "minecraft:carrots[age=7]", "minecraft:farmland[moisture=7]"));
        return crops;
    }
    static Quest farm(int count) { return new Quest(QuestKind.FARM, AREA, START, 5, "COW", crops(count), new Reward(new BigDecimal("10000"), List.of())); }
    static Definition npc(Quest quest) {
        var json = NpcStore.JSON.toJsonTree(Definition.create("petani_revenant", "Petani Revenant", START, START)).getAsJsonObject();
        json.add("quest", NpcStore.JSON.toJsonTree(quest));
        return NpcStore.JSON.fromJson(json, Definition.class);
    }
    @Test void fullRevenantFieldExceedsOldLimitsAndKeepsAllPlants() {
        Quest quest = farm(2352);
        assertEquals(2352, quest.crops().size()); assertEquals(5, quest.goal()); assertEquals(0, new BigDecimal("10000").compareTo(quest.reward().viti()));
        assertEquals(14112, new NpcFarmCursor(AREA).volume());
    }
    @Test void largeFarmSurvivesRestartWithRewardIdentityMemoriesAndGlobalQuota() throws Exception {
        Definition npc = npc(farm(2352)); UUID player = UUID.randomUUID(), receipt = UUID.randomUUID();
        State state = State.empty().put(npc).remember(player, npc, new Choice("Ya", "QUEST", "ladang", "revenant"))
                .finish(player, npc, true, receipt, 1000);
        NpcStore store = new NpcStore(directory.resolve("npc.json")); store.save(state);
        State reloaded = store.initialize();
        assertEquals(state, reloaded); assertEquals(npc.token(), reloaded.npcs().get(npc.id()).token());
        assertEquals(1, reloaded.quota(player, 1001).count()); assertEquals(receipt, reloaded.pending().get(player).receipt());
        assertEquals("revenant", reloaded.memory(player, npc).get("ladang"));
    }
    @Test void threeCompletedLargeQuestsStillStartTwentyMinuteCooldown() {
        Definition npc = npc(farm(600)); UUID player = UUID.randomUUID(); State state = State.empty().put(npc);
        for (int i = 0; i < 3; i++) {
            UUID receipt = UUID.randomUUID(); state = state.finish(player, npc, true, receipt, 1000);
            assertEquals(0, new BigDecimal("10000").compareTo(state.pending().get(player).reward().viti()));
            state = state.acknowledge(player, receipt);
        }
        assertEquals(3, state.quota(player, 1001).count()); assertEquals(0, state.quota(player, 1_201_000).count());
    }
    @Test void emptyDuplicateOutsideAndItemRewardFarmDefinitionsRemainInvalid() {
        assertThrows(IllegalArgumentException.class, () -> farm(0)); Crop first = crops(1).getFirst();
        assertThrows(IllegalArgumentException.class, () -> new Quest(QuestKind.FARM, AREA, START, 5, "COW", List.of(first, first), Reward.none()));
        assertThrows(IllegalArgumentException.class, () -> new Quest(QuestKind.FARM, AREA, START, 5, "COW",
                List.of(new Crop(0, 68, 0, first.plant(), first.soil())), Reward.none()));
        assertThrows(IllegalArgumentException.class, () -> new Quest(QuestKind.FARM, AREA, START, 5, "COW", List.of(first), new Reward(BigDecimal.ZERO, List.of("item"))));
    }
    @Test void cursorVisitsAllNegativeCoordinatesOnceIncludingBothCorners() {
        NpcFarmCursor cursor = new NpcFarmCursor(AREA); Set<String> positions = new HashSet<>(); Set<String> finishedChunks = new HashSet<>(); String chunk = "";
        while (!cursor.done()) {
            String current = cursor.chunkX() + ":" + cursor.chunkZ();
            if (!chunk.equals(current)) { if (!chunk.isEmpty()) finishedChunks.add(chunk); assertFalse(finishedChunks.contains(current)); chunk = current; }
            assertTrue(positions.add(cursor.x() + ":" + cursor.y() + ":" + cursor.z())); cursor.next();
        }
        assertEquals(14112, positions.size()); assertEquals(14112, cursor.visited());
        assertTrue(positions.contains("-6323:67:-4180")); assertTrue(positions.contains("-6364:72:-4235"));
    }
    @Test void singleBlockCursorAndChunkBoundaryFinishWithoutExtraCells() {
        for (int x : List.of(-17, -16, -1, 0, 15, 16)) {
            NpcFarmCursor cursor = new NpcFarmCursor(new WhisperArea(WORLD, x, 68, x, x, 68, x));
            assertEquals(x >> 4, cursor.chunkX()); assertEquals(1, cursor.volume()); cursor.next(); cursor.next();
            assertTrue(cursor.done()); assertEquals(1, cursor.visited());
        }
    }
    @Test void legacySmallFarmDoesNotNeedSchemaChangeOrLoseData() throws Exception {
        State state = State.empty().put(npc(farm(10))); Path file = directory.resolve("npc.json");
        Files.writeString(file, NpcStore.JSON.toJson(state)); byte[] old = Files.readAllBytes(file);
        assertEquals(state, new NpcStore(file).initialize()); assertArrayEquals(old, Files.readAllBytes(file));
    }
    @Test void validNpcFileAboveSixteenMegabytesStillLoads() throws Exception {
        State state = State.empty().put(npc(farm(600))); Path file = directory.resolve("npc.json");
        Files.writeString(file, NpcStore.JSON.toJson(state));
        try (var writer = Files.newBufferedWriter(file, StandardOpenOption.APPEND)) {
            String whitespace = " ".repeat(65536); for (int i = 0; i < 250; i++) writer.write(whitespace);
        }
        assertTrue(Files.size(file) > 16_000_000); assertEquals(state, new NpcStore(file).initialize());
    }
    @Test void farmSnapshotLargerThanOldFileCapCanBeSavedAndReopened() throws Exception {
        WhisperArea area = new WhisperArea(WORLD, 0, 64, 0, 255, 68, 255); List<Crop> crops = new ArrayList<>();
        for (int i = 0; i < 90_000; i++) crops.add(new Crop(i % 256, 65 + 2 * (i / 65536), i / 256 % 256,
                "minecraft:wheat[age=7]", "minecraft:farmland[moisture=7]"));
        Quest quest = new Quest(QuestKind.FARM, area, new Point(WORLD, .5, 65, .5, 0, 0), 5, "COW", crops, Reward.none());
        State state = State.empty().put(npc(quest)); NpcStore store = new NpcStore(directory.resolve("npc.json")); store.save(state);
        assertTrue(Files.size(directory.resolve("npc.json")) > 16_000_000); assertEquals(state, store.initialize());
    }
}
'@
    }
)
$plansVitae = @()
foreach ($fileVitae in $filesVitae) {
    if ($fileVitae.Name -notmatch '^src/(main|test)/java/com/bangzachery/vitae/vitaemanager/[A-Za-z0-9_/]+\.java$') { throw 'Path installer tidak valid.' }
    $targetVitae = Join-Path $rootVitae $fileVitae.Name
    $newTextVitae = Normalize-Vitae $fileVitae.Source
    $bytesVitae = $utf8Vitae.GetBytes($newTextVitae)
    if ((Get-VitaeHash $bytesVitae) -ne $fileVitae.Hash) { throw ('Isi installer tidak utuh: ' + $fileVitae.Name + '. Unduh ulang. Tidak ada file yang diubah.') }
    $existsVitae = Test-Path -LiteralPath $targetVitae -PathType Leaf
    $oldRawHashVitae = ''
    if ($existsVitae) {
        $rawVitae = [IO.File]::ReadAllBytes($targetVitae)
        $oldRawHashVitae = Get-VitaeHash $rawVitae
        $oldHashVitae = Get-VitaeHash ($utf8Vitae.GetBytes((Normalize-Vitae ([IO.File]::ReadAllText($targetVitae, $utf8Vitae)))))
        if ($oldHashVitae -eq $fileVitae.Hash) { continue }
        if ($fileVitae.OriginalHash -eq '' -or $oldHashVitae -ne $fileVitae.OriginalHash) {
            throw ($fileVitae.Name + ' berbeda dari commit sumber pembaruan. Kirim versi terbarunya untuk penyesuaian. Tidak ada file yang diubah.')
        }
        if ((Get-Item -LiteralPath $targetVitae).IsReadOnly) { throw ($fileVitae.Name + ' read-only. Tidak ada file yang diubah.') }
    } elseif ($fileVitae.OriginalHash -ne '') {
        throw ($fileVitae.Name + ' belum ada; modul lama belum lengkap. Tidak ada file yang diubah.')
    } elseif (Test-Path -LiteralPath $targetVitae) {
        throw ($fileVitae.Name + ' adalah direktori, bukan file. Tidak ada file yang diubah.')
    }
    $plansVitae += [PSCustomObject]@{ Name=$fileVitae.Name; Path=$targetVitae; Bytes=$bytesVitae; Existed=$existsVitae; OldRawHash=$oldRawHashVitae }
}
if ($plansVitae.Count -eq 0) { Write-Host 'Update NPC farm besar ini sudah terpasang; tidak ditulis ulang.' -ForegroundColor Green; return }
Write-Host ('Pemeriksaan lolos: ' + $plansVitae.Count + ' file siap dipasang.') -ForegroundColor Green
Write-Host 'Main, command lain, dan konfigurasi server tetap utuh.' -ForegroundColor Cyan
if ($CheckOnly) { Write-Host 'CheckOnly selesai. Belum ada file yang diubah.' -ForegroundColor Yellow; return }
$backupVitae = Join-Path $rootVitae ('vitae-npc-farm-backup-' + [DateTime]::Now.ToString('yyyyMMdd-HHmmss-fff') + '-' + [Guid]::NewGuid().ToString('N').Substring(0,6))
$temporaryVitae = @()
$attemptedVitae = @()
$createdDirsVitae = @()
try {
    # Pastikan IntelliJ tidak mengubah source di antara pemeriksaan dan pemasangan.
    foreach ($planVitae in $plansVitae) {
        if ($planVitae.Existed) {
            if (-not (Test-Path -LiteralPath $planVitae.Path -PathType Leaf) -or
                (Get-VitaeHash ([IO.File]::ReadAllBytes($planVitae.Path))) -ne $planVitae.OldRawHash) { throw ($planVitae.Name + ' berubah saat pemasangan; hentikan dan ulangi.') }
        } elseif (Test-Path -LiteralPath $planVitae.Path) { throw ($planVitae.Name + ' muncul saat pemasangan; hentikan dan ulangi.') }
    }
    [IO.Directory]::CreateDirectory($backupVitae) | Out-Null
    foreach ($planVitae in $plansVitae) {
        if ($planVitae.Existed) {
            $savedVitae = Join-Path $backupVitae $planVitae.Name
            [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($savedVitae)) | Out-Null
            Copy-Item -LiteralPath $planVitae.Path -Destination $savedVitae -ErrorAction Stop
        }
    }
    foreach ($planVitae in $plansVitae) {
        $parentVitae = [IO.Path]::GetDirectoryName($planVitae.Path)
        if (-not (Test-Path -LiteralPath $parentVitae -PathType Container)) {
            $createdDirsVitae += $parentVitae
            [IO.Directory]::CreateDirectory($parentVitae) | Out-Null
        }
        $tempVitae = $planVitae.Path + '.vitae-evo-' + [Guid]::NewGuid().ToString('N')
        $temporaryVitae += $tempVitae
        [IO.File]::WriteAllBytes($tempVitae, $planVitae.Bytes)
        $planVitae | Add-Member -NotePropertyName Temporary -NotePropertyValue $tempVitae
    }
    foreach ($planVitae in $plansVitae) {
        $attemptedVitae += $planVitae
        Move-Item -LiteralPath $planVitae.Temporary -Destination $planVitae.Path -Force -ErrorAction Stop
    }
} catch {
    $causeVitae = $_.Exception.Message
    $rollbackVitae = @()
    foreach ($planVitae in $attemptedVitae) {
        try {
            if ($planVitae.Existed) { Copy-Item -LiteralPath (Join-Path $backupVitae $planVitae.Name) -Destination $planVitae.Path -Force -ErrorAction Stop }
            elseif (Test-Path -LiteralPath $planVitae.Path -PathType Leaf) { Remove-Item -LiteralPath $planVitae.Path -Force -ErrorAction Stop }
        } catch { $rollbackVitae += ($planVitae.Name + ': ' + $_.Exception.Message) }
    }
    if ($rollbackVitae.Count -gt 0) { throw ('Pemasangan gagal dan sebagian rollback gagal. Backup: ' + $backupVitae + '. Detail: ' + ($rollbackVitae -join '; ')) }
    throw ('Pemasangan dibatalkan; source yang sudah ditulis dipulihkan. Penyebab: ' + $causeVitae + '. Backup: ' + $backupVitae)
} finally {
    foreach ($tempVitae in $temporaryVitae) { if (Test-Path -LiteralPath $tempVitae -PathType Leaf) { Remove-Item -LiteralPath $tempVitae -Force -ErrorAction SilentlyContinue } }
    foreach ($dirVitae in $createdDirsVitae) { if ((Test-Path -LiteralPath $dirVitae -PathType Container) -and @(Get-ChildItem -LiteralPath $dirVitae -Force).Count -eq 0) { Remove-Item -LiteralPath $dirVitae -ErrorAction SilentlyContinue } }
}
Write-Host 'Update NPC farm besar terpasang. Backup source lama:' -ForegroundColor Green
Write-Host $backupVitae
Write-Host 'Lanjutkan: .\gradlew.bat clean build' -ForegroundColor Cyan
Write-Host 'Matikan server, ganti JAR hasil build, biarkan folder data lama, lalu nyalakan server.' -ForegroundColor Yellow
Write-Host 'Bantuan: /vnpc help ladang. Tunggu Ladang tersimpan setelah quest farm.' -ForegroundColor Cyan
```
<!-- VITAE_INSTALLER_END -->
