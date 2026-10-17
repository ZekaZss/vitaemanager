# Update NPC Cerita dan Adegan Penutup Evo

Untuk Vitaemanager, Paper **1.21.4**, Java **21**. Update ini melanjutkan **update-evo-gerbang.md** yang sudah dipasang sebelumnya.

## Isi update

- Nama NPC skin/model memakai nama tampilan, tanpa tulisan internal `VN_`. Nama bisa biasa, diberi warna, atau disembunyikan di atas kepala.
- NPC menoleh ke pemain selama dialog dan kembali ke arah dasar beserta offset saat selesai, batal, mati, disconnect, atau dialog terputus.
- Gerbang bisa dipakai berulang dan otomatis menutup setelah **30 detik** sejak dinding selesai dibuka. Durasi dapat diubah menjadi 1–3600 detik atau dimatikan.
- Peringatan actionbar dan suara vanilla untuk pemain sekitar pada lima detik terakhir. Jika ada badan pemain di posisi block dinding, penutupan menunggu hingga kosong. Tidak ada teleport paksa.
- Klik berikutnya tidak mengulang timer. Dialog mengikuti status gerbang; setelah tertutup pemain lama boleh membuka kembali. Hadiah dialog tetap sekali per pemain.
- Pilihan dialog dapat menyimpan ingatan per pemain/NPC. Admin menentukan halaman yang muncul pada pertemuan berikutnya berdasarkan ingatan itu.
- Evo memiliki adegan setelah ledakan: hening singkat, hasil melayang dengan particle, title akhir, kemudian senjata sungguhan jatuh di atas altar. Default **5 detik**, dapat diatur 2–15 detik atau dimatikan.
- Folder data lama tetap dipakai. Tidak perlu membuat ulang NPC, altar, quest, bisikan atau saldo Viti.

## Cara memasang di proyek IntelliJ

Letakkan **update-npc-cerita-evo.md** sejajar dengan **gradlew.bat** di folder proyek `vitaemanager`. Jalankan PowerShell dari folder itu:

```powershell
$m=Get-Content -LiteralPath .\update-npc-cerita-evo.md -Raw -Encoding UTF8; $i=[regex]::Match($m,'(?s)<!-- VITAE_INSTALLER_BEGIN -->\r?\n```powershell\r?\n(.*?)\r?\n```\r?\n<!-- VITAE_INSTALLER_END -->'); if(!$i.Success){throw 'Blok installer tidak ditemukan'}; & ([scriptblock]::Create($i.Groups[1].Value)) -ProjectPath (Get-Location).Path
```

Untuk pemeriksaan tanpa mengubah source, tambahkan `-CheckOnly` di ujung command tersebut. Installer utama di bawah ini dipanggil oleh command itu; tidak perlu membuat file `.ps1` terpisah atau menyalin Java satu per satu.

Setelah installer berhasil:

```powershell
.\gradlew.bat clean build
```

Installer memeriksa hash source seluruh file sebelum menulis, menyimpan backup source lama, dan memulihkan file yang sudah ditulis jika pemasangan gagal. Menjalankan installer dua kali tidak menulis ulang update yang sama. Windows BOM/CRLF tetap didukung.

`Vitaemanager.java`, `plugin.yml`, Gradle, resource config, dan modul lain tidak diganti. Jika source lokal berbeda dari versi sebelumnya, installer berhenti sebelum mengubah file. Jangan paksa mengganti source yang berbeda; versi itu perlu disesuaikan.

## Memasang JAR ke server tanpa menghapus data

1. Matikan server dengan `stop`.
2. Ganti JAR Vitaemanager lama di `plugins` dengan JAR hasil build dari `build/libs`. Jangan sisakan dua versi JAR Vitaemanager.
3. **Biarkan folder data Vitaemanager yang lama beserta seluruh isinya. Jangan hapus atau ganti nama folder itu.**
4. Nyalakan server kembali. Jangan gunakan `/reload` server untuk mengganti JAR.

Data Viti, profil, giveaway, NPC, dialog, quest, gerbang, progres, bisikan, altar, item custom, setoran dan hadiah tertunda tetap menggunakan data lama. ID dan token NPC tidak dibuat ulang saat nama diganti.

Ketika format lama perlu ditambahkan field baru, file NPC/evo dibaca dan divalidasi dahulu, kemudian dibuat backup dengan nama `npc-before-story-update-....json.bak` atau `evolution-before-story-update-....yml.bak`. Data yang tidak valid tidak diganti dengan data kosong. Backup source installer berbeda dari backup data server ini.

NPC lama otomatis memakai nama yang tampil, pelacakan arah ON, dan ingatan kosong. Gerbang lama mendapat durasi 30 detik; gerbang yang sebelumnya sudah OPEN tanpa timer mendapat timer pertama saat modul aktif. Altar lama otomatis mendapat penutup 5 detik. Semua lokasi, penerima, syarat bahan, musik, radius damage, status lethal, hadiah dan cooldown yang sudah diatur tetap dipertahankan.

## Nama dan arah NPC

Ganti `penjaga` dengan ID NPC yang sudah ada. Jalankan satu per satu:

```text
/vnpc penjaga name Penjaga Gerbang
/vnpc penjaga on
```

Nama berwarna:

```text
/vnpc penjaga name &6Penjaga &eGerbang
/vnpc penjaga on
```

Kode `&0`–`&f` untuk warna, `&l` tebal, `&o` miring, `&r` reset. Nama dengan warna yang sama juga muncul di dialog. Tidak perlu mengubah ID NPC.

Sembunyikan hanya nama di atas kepala:

```text
/vnpc penjaga nametag off
/vnpc penjaga on
```

Untuk menampilkan lagi, gunakan `nametag on`. Nama di dialog tetap ada ketika nametag OFF.

Pelacakan arah saat dialog aktif otomatis. Untuk pengaturan:

```text
/vnpc penjaga track on
/vnpc penjaga on
```

`track off` mempertahankan arah NPC selama dialog. Arah diam tetap diatur dengan command yang sudah ada:

```text
/vnpc penjaga look
/vnpc penjaga offset 0 0
/vnpc penjaga on
```

`look` menyalin arah pandang admin tanpa memindahkan NPC dan mereset offset. Untuk model yang arah depannya berbeda, gunakan offset yang sesuai, misalnya `offset 90 0`.

NPC skin menoleh khusus di layar pemain yang sedang berbicara, sehingga dua percakapan tidak berebut arah. NPC ModelEngine memiliki satu arah model bersama: mengikuti pemain pertama yang sedang dialog; setelah pemain itu selesai, mengikuti pembicara berikutnya, lalu kembali ke arah awal ketika tidak ada dialog lagi. Offset model ikut dipakai saat mengarah kepada pemain.

**Perubahan setup membuat NPC menjadi OFF. Jalankan `/vnpc <id> on` setelah selesai mengatur.**

## Gerbang yang sudah ada

Tidak perlu menandai dinding ulang. Pilihan `OPEN` dan snapshot gerbang lama tetap dipakai.

```text
/vnpc penjaga gate autoclose 30
/vnpc penjaga repeat Cepat masuk sebelum gerbang ditutup.
/vnpc penjaga on
```

Durasi 60 detik:

```text
/vnpc penjaga gate autoclose 60
/vnpc penjaga on
```

Biarkan terbuka sampai reset manual:

```text
/vnpc penjaga gate autoclose off
/vnpc penjaga on
```

Cek status:

```text
/vnpc penjaga gate status
```

- `CLOSED`: pilihan dialog membuka gerbang boleh dipakai lagi, termasuk pemain yang pernah datang.
- `OPENING`: pemain diminta menunggu. Klik bersamaan tetap satu animasi.
- `OPEN`: NPC memakai teks `repeat`. Interaksi tidak menambah waktu buka.
- `CLOSING`: NPC meminta menunggu sampai penutupan selesai.

Lima detik terakhir, pemain dalam radius 12 block dari tengah gerbang mendapat peringatan dan suara vanilla. Jika badan pemain menyentuh posisi block dinding saat timer habis, dinding tetap terbuka hingga pemain keluar. Saat menutup, block muncul kembali dengan particle dan suara batu, tanpa ledakan kedua.

Timer disimpan sebagai waktu tutup, sehingga restart/reload tidak memberikan 30 detik baru untuk gerbang yang sudah memiliki timer. Jika waktu sudah habis ketika server hidup kembali, penutupan tetap menunggu dunia/chunk siap dan tempat dinding kosong. Plugin tidak memaksa memuat chunk dan tidak menimpa block asing yang berbeda dari snapshot.

## Membuat NPC gerbang baru

```text
/vnpc create penjaga Penjaga Gerbang
```

Berdiri di tempat pemain akan bicara, di luar dinding dan tidak terhalang block:

```text
/vnpc penjaga stand
```

Untuk menandai dinding:

```text
/vnpc penjaga gate pos1
```

Pukul block sudut pertama. Kemudian:

```text
/vnpc penjaga gate pos2
```

Pukul sudut diagonal seberangnya. Pilih dinding biasa, maksimal 256 block; jangan sertakan chest atau block mesin.

```text
/vnpc penjaga gate set
/vnpc penjaga page start Kamu boleh masuk. Akan kubukakan gerbangnya.
/vnpc penjaga clearoptions start
/vnpc penjaga option start OPEN Bukakan gerbangnya.
/vnpc penjaga option start END Nanti saja.
/vnpc penjaga root start
/vnpc penjaga repeat Cepat masuk sebelum gerbang ditutup.
/vnpc penjaga gate autoclose 30
/vnpc penjaga on
```

Player klik kiri/kanan NPC, tunggu teks selesai, scroll memilih, lalu klik kiri/kanan untuk konfirmasi. `gate set` menghapus trigger area lama hanya untuk NPC ini. Preview tidak membuka gerbang atau memberikan hadiah.

Reset manual masih ada:

```text
/vnpc penjaga gate reset
/vnpc penjaga on
```

Pindahkan pemain dari tempat dinding sebelum reset. Reset gerbang tidak mereset hadiah, ingatan dialog, atau kuota quest.

## Dialog mengingat pilihan pemain

Contoh NPC baru bernama `mira`. Jika ID ini sudah ada, jangan jalankan create lagi.

```text
/vnpc create mira Mira
/vnpc mira page start Maukah kamu membantuku?
/vnpc mira clearoptions start
/vnpc mira option start END Aku akan membantu.
/vnpc mira option start END Aku menolak.
/vnpc mira remember start 1 sikap membantu
/vnpc mira remember start 2 sikap menolak
/vnpc mira page ramah Terima kasih sudah bersedia membantuku. Aku mengingat kebaikanmu.
/vnpc mira page dingin Kamu pernah menolakku. Sekarang ada urusan apa?
/vnpc mira memory sikap membantu ramah
/vnpc mira memory sikap menolak dingin
/vnpc mira root start
/vnpc mira on
```

`remember start 1 sikap membantu` berarti **pilihan nomor 1** pada halaman `start` menyimpan `sikap=membantu` untuk pemain itu saja.

`memory sikap membantu ramah` berarti pada percakapan berikutnya, pemain yang memiliki ingatan tersebut mulai di halaman `ramah`. Halaman tanpa pilihan otomatis memiliki `Keluar...`.

Ingatan disimpan saat pemain mengonfirmasi pilihan yang diberi pengaturan `remember`. Jika pilihan menuju halaman lain sudah dikonfirmasi lalu percakapan dibatalkan, pilihan yang tadi tetap diingat. Membatalkan sebelum konfirmasi tidak menyimpan pilihan. Pilihan QUEST hanya disimpan bila quest berhasil diterima. Preview tidak menyimpan ingatan.

Aturan berlaku per NPC dan per pemain. Jika beberapa aturan cocok, aturan pertama dalam daftar dipakai. Tanpa aturan cocok, halaman root dipakai. NPC lama yang sudah pernah diselesaikan dapat memiliki pilihan cerita baru tanpa mereset catatan hadiah.

Hadiah dialog tetap sekali per pemain meskipun dialog berulang atau ingatan berubah. Tidak perlu reset progres untuk mencoba percakapan lanjutan.

Pengelolaan:

```text
/vnpc mira memory status
/vnpc mira remember start 1 off
/vnpc mira memory clear
```

`remember ... off` hanya melepas penyimpanan pada pilihan itu. `memory clear` hanya menghapus aturan halaman, bukan ingatan yang sudah tersimpan pada pemain. Keduanya membuat NPC draft OFF; aktifkan lagi sesudah setup.

Command lama `/vnpc <id> reset <player>` tetap mereset progres NPC, ingatan NPC tersebut, dan kuota quest global pemain. Gunakan hanya ketika memang ingin reset, karena hadiah sekali-per-player bisa menjadi tersedia kembali setelah reset eksplisit.

## Adegan evo setelah ledakan

Untuk altar lama, cukup jalankan:

```text
/evo aftermath altarapi on 5
/evo info altarapi
/evo preview altarapi
```

Tunggu pesan tersimpan sebelum command berikutnya. Tidak perlu menyusun bahan/pedestal/source/result lagi.

Alurnya: animasi lama dan musik berjalan → ledakan pada durasi ritual yang sudah diatur → musik berhenti → hening satu detik → hasil evo melayang dengan particle dan title akhir → setelah total lima detik penutup, item sungguhan diberikan di atas altar.

Contoh durasi ritual 60 detik dan penutup 8 detik:

```text
/evo music altarapi public minecraft:music.dragon 60 1.0
/evo aftermath altarapi on 8
```

Ledakan tetap pada akhir ritual 60 detik; senjata diberikan sekitar detik 68. Jika penyimpanan disk sedang lambat, plugin menunggu checkpoint hasil sebelum ledakan. Damage hanya sekali saat ledakan, bukan ketika item diberikan. Preview tetap tanpa damage, konsumsi atau hadiah.

Untuk kembali ke penyelesaian langsung seperti sebelumnya:

```text
/evo aftermath altarapi off
```

Title penutup memakai tahap `finish` yang sudah tersedia:

```text
/evo text altarapi finish Kekuatan baru telah lahir | Senjatamu telah berevolusi
```

Setelah checkpoint ledakan, input tidak direfund otomatis. Kalau pemain disconnect, mati tanpa totem, atau plugin berhenti saat penutup, hasil tetap dicatat sebagai tertunda. Setelah pemain kembali dan admin memeriksa item yang sudah ada:

```text
/evo pending
/evo recover NamaPlayer result confirm
```

Phase `REVEALING` hanya menerima pemulihan hasil atau discard; refund senjata lama ditolak. Ini mencegah hasil dan senjata lama sama-sama diberikan. Item hasil tetap memakai data custom yang disimpan admin dan drop bertanda pemilik/receipt seperti versi sebelumnya.

## Bantuan dan uji singkat

```text
/vnpc help tampilan
/vnpc help gerbang
/vnpc help ingatan
/evo help aftermath
```

Uji di server: warna/nama tersembunyi, dialog dari beberapa arah dan Shift cancel, dua pemain membuka bersamaan, gerbang menutup lalu bisa dibuka lagi, pemain berdiri di dinding saat timer habis, dialog lanjutan berdasarkan pilihan, dan preview evo sebelum uji lethal.

Build dan tes otomatis memeriksa data lama serta lifecycle. Tampilan client, adapter ModelEngine yang terpasang, dan interaksi proteksi server tetap perlu diuji di server Paper milikmu.

## Installer lengkap

Bagian ini berisi source Java lengkap dan dipasang otomatis oleh command PowerShell di atas.

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
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/command/EvolutionCommand.java'
        OriginalHash = '54ccdc0a5715a3002a5b666d576d7e3b847e415f4e5363f19ad8d62797efa6e7'
        Hash = '9c5847ba752add6cae12d05d6d618598641756b8b79c9d71e07e339fd48aef98'
        Source = @'
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
        usage.put("layout", "layout <id> — susun contoh bahan di GUI crafting admin, tutup untuk simpan");
        usage.put("pedestal", "pedestal <id> <slot> — lihat block solid, slot 1/2/3/4/6/7/8/9, radius 16 block");
        usage.put("ingredient",
                "ingredient <id> add <jumlah> | remove <nomor> — pegang bahan, lihat pedestal");
        usage.put("target", "target <id> add|remove <player|uuid>");
        usage.put("preset", "preset <id> inferno|stormforge|void|astral|frost|phoenix");
        usage.put("settings",
                "settings <id> duration|scale|density|soundradius|damageradius|damage <nilai>");
        usage.put("text", "text <id> <tahap> title | subtitle");
        usage.put("sound",
                "sound <id> <tahap> private|public <namespace:sound|off> [volume] [pitch]");

        usage.put("music", "music <id> public|private <namespace:sound> <detik> [volume] | music <id> off");
        usage.put("aftermath", "aftermath <id> on [detik 2–15] | aftermath <id> off — adegan setelah ledakan");
        usage.put("blast", "blast <id> lethal|off | blast <id> damage <jumlah>");
        usage.put("effects", "effects <id> lightning on|off [jeda-detik] [radius] | effects <id> shockwave on|off");
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
                case "layout" -> { exact(args, 2); service.layout(player(sender), id(args[1])); }
                case "pedestal" -> {
                    exact(args, 3); String altarId = id(args[1]); int slot = Integer.parseInt(args[2]);
                    require(EvolutionGrid.ingredientSlot(slot), "Slot 1,2,3,4,6,7,8,9; slot 5 khusus senjata.");
                    var altar = service.altar(altarId); var next = new ArrayList<>(altar.ingredients());
                    int index = EvolutionGrid.index(altar, slot); require(index >= 0, "Slot ini belum punya bahan. Gunakan /evo layout " + altarId + ".");
                    Block block = looked(player(sender)); require(block.getType().isSolid(), "Pedestal harus berupa block solid, misalnya lodestone.");
                    Ingredient old = next.get(index); next.set(index, new Ingredient(old.item(), old.count(), EvolutionService.point(block), slot));
                    edit(sender, altarId, section -> {
                        section.set("ingredients", null); var root = section.createSection("ingredients");
                        for (int i = 0; i < next.size(); i++) EvolutionStore.writeIngredient(root.createSection(Integer.toString(i + 1)), next.get(i));
                    });
                }
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
                    edit(sender, args[1], section -> { section.set(key, value); if (key.equals("damage")) section.set("lethal", false); });
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
                case "music" -> {
                    if (args.length == 3 && args[2].equalsIgnoreCase("off")) {
                        edit(sender, args[1], section -> section.set("music.sound", ""));
                    } else {
                        require(args.length == 5 || args.length == 6, "Gunakan /evo " + usage.get("music"));
                        require(args[2].equalsIgnoreCase("private") || args[2].equalsIgnoreCase("public"), "Musik: private atau public.");
                        double seconds = Double.parseDouble(args[4]); range(seconds, 5, 120, "Durasi musik");
                        Music music = new Music(args[3], args[2].equalsIgnoreCase("private"),
                                args.length == 6 ? Float.parseFloat(args[5]) : .8f, (int) Math.round(seconds * 20));
                        edit(sender, args[1], section -> {
                            section.set("duration", music.ticks() / 20.0); section.set("music.sound", music.sound());
                            section.set("music.private", music.privateSound()); section.set("music.volume", music.volume());
                        });
                    }
                }
                case "aftermath" -> {
                    require(args.length == 3 || args.length == 4, "Gunakan /evo " + usage.get("aftermath"));
                    require(args[2].equalsIgnoreCase("on") || args[2].equalsIgnoreCase("off"), "Adegan: on atau off.");
                    require(args[2].equalsIgnoreCase("on") || args.length == 3, "Off tidak memerlukan durasi.");
                    double seconds = args.length == 4 ? Double.parseDouble(args[3]) : service.altar(id(args[1])).aftermath().ticks() / 20.0;
                    range(seconds, 2, 15, "Adegan penutup");
                    edit(sender, args[1], section -> { section.set("aftermath.enabled", args[2].equalsIgnoreCase("on")); section.set("aftermath.duration", seconds); });
                }
                case "blast" -> {
                    require(args.length == 3 || args.length == 4, "Gunakan /evo " + usage.get("blast"));
                    String mode = args[2].toLowerCase(Locale.ROOT);
                    require(List.of("lethal", "damage", "off").contains(mode), "Ledakan: lethal, damage, atau off.");
                    require(args.length == (mode.equals("damage") ? 4 : 3), "Gunakan /evo " + usage.get("blast"));
                    double damage = mode.equals("damage") ? Double.parseDouble(args[3]) : 0; range(damage, 0, 1000, "Damage");
                    edit(sender, args[1], section -> { section.set("lethal", mode.equals("lethal")); section.set("damage", damage); });
                }
                case "effects" -> {
                    require(args.length >= 4 && args.length <= 6, "Gunakan /evo " + usage.get("effects"));
                    String effect = args[2].toLowerCase(Locale.ROOT);
                    require(List.of("lightning", "shockwave").contains(effect), "Efek: lightning atau shockwave.");
                    require(args[3].equalsIgnoreCase("on") || args[3].equalsIgnoreCase("off"), "Efek: on atau off.");
                    require(effect.equals("lightning") || args.length == 4, "Shockwave cukup on/off.");
                    double interval = args.length >= 5 ? Double.parseDouble(args[4]) : 1.5;
                    double radius = args.length == 6 ? Double.parseDouble(args[5]) : 3;
                    range(interval, .5, 10, "Jeda petir"); range(radius, 0, 8, "Radius petir");
                    edit(sender, args[1], section -> {
                        section.set("effects." + effect, args[3].equalsIgnoreCase("on"));
                        if (effect.equals("lightning")) { section.set("effects.interval-ticks", (int) Math.round(interval * 20)); section.set("effects.radius", radius); }
                    });
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
                            + " | ledakan " + (altar.lethal() ? "LETHAL (memicu totem jika tersedia)" : altar.damage())
                            + " | radius damage " + altar.damageRadius());

                    service.say(sender, "Musik " + (altar.music().sound().isEmpty() ? "off" : altar.music().sound())
                            + " | volume " + altar.music().volume() + " | petir " + altar.effects().lightning());
                    service.say(sender, "Adegan setelah ledakan " + (altar.aftermath().enabled() ? altar.aftermath().ticks() / 20.0 + "s" : "OFF"));
                    for (int i = 0; i < altar.ingredients().size(); i++) {
                        service.say(sender, "Bahan " + (i + 1) + " [slot " + altar.ingredients().get(i).slot() + "]: x"
                                + altar.ingredients().get(i).count()
                                + " di " + (altar.ingredients().get(i).point() == null ? "BELUM DISET — /evo pedestal " + altar.id() + " " + altar.ingredients().get(i).slot() : altar.ingredients().get(i).point().encode()));
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
                    var deposits = service.pendingDeposits();
                    deposits.forEach(d -> service.say(sender, d.owner() + " | " + d.altar() + " | SETORAN " + d.items().size() + "/9 slot"
                            + (d.transfer() == null ? "" : " | transaksi tertunda")));
                    if (pending.isEmpty() && deposits.isEmpty()) {
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
                    EvolutionService.point(looked(player)),
                    java.util.stream.IntStream.range(0, 8).map(EvolutionGrid::rawSlot)
                            .filter(slot -> EvolutionGrid.index(altar, slot) < 0).findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Delapan slot bahan sudah terisi."))
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

        sender.sendMessage(net.kyori.adventure.text.Component.empty());
        sender.sendMessage(net.kyori.adventure.text.Component.text("━━ Bantuan Evolusi ━━", net.kyori.adventure.text.format.NamedTextColor.GOLD));
        topic = topic.toLowerCase(Locale.ROOT);

        if (topic.equals("setup")) {
            for (String key : List.of(
                    "create", "source", "result", "layout", "pedestal", "ingredient", "target", "enable")) {
                service.say(sender, "/evo " + usage.get(key));
            }
            service.say(sender,
                    "Pedestal berbeda, solid, maksimal 16 block dari altar. Susunan GUI: baris atas 1/2/3, tengah 4/5/6, bawah 7/8/9; slot 5 senjata. Tunggu pesan tersimpan sebelum command berikutnya.");
        } else if (usage.containsKey(topic)) {
            service.say(sender, "/evo " + usage.get(topic));
        } else {
            service.say(sender,
                    "/evo help setup. Bantuan command: /evo help <command>.");
            usage.values().forEach(line -> service.say(sender, "/evo " + line));
        }

        sender.sendMessage(net.kyori.adventure.text.Component.empty());
        if (topic.equals("music")) service.say(sender, "Durasi musik menentukan waktu ledakan. Cocokkan panjang rekaman custom; pitch tetap 1. Volume 0–1; radius melalui soundradius. ID custom harus ada di resource pack ItemsAdder.");
        if (topic.equals("aftermath")) service.say(sender, "Default ON 5 detik: hening 1 detik, hasil melayang, title, lalu item diberikan. Durasi musik tetap sampai ledakan; adegan ini tambahan. Setelah ledakan tidak ada refund otomatis; hasil tertunda dapat dipulihkan admin.");
        if (topic.equals("blast")) service.say(sender, "Lethal lewat sistem totem; tanpa totem pemain mati. Radius dari crafting table. Preview tanpa damage.");
        if (topic.equals("effects")) service.say(sender, "Petir visual tanpa api/damage: jeda 0.5–10s, radius 0–8. Shockwave berupa particle.");
        if (topic.equals("layout")) service.say(sender, "Contoh disalin dari cursor; item admin tetap milik admin. Klik kosong menghapus slot. Tutup menyimpan draft OFF; lengkapi pedestal, lalu enable.");
        if (topic.equals("pedestal")) service.say(sender, "Lihat alas display dalam jarak 6 block dari admin. Jarak pedestal dari altar maksimal 16 block; di world yang sama dan tidak duplikat.");
        if (topic.equals("settings")) {
            service.say(sender,
                    "Duration 5–120 detik; scale 0.5–2; density 8–64; soundradius 1–64; damageradius 0–16; damage 0–1000. Altar lama mempertahankan damage lama; aktifkan lethal melalui /evo blast.");
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
                         "info", "preview", "delete", "music", "blast", "effects", "layout", "pedestal", "aftermath" ->
                            service.ids().stream().toList();
                    default -> List.of();
                };
            } else if (args.length == 3) {
                choices = switch (action) {
                    case "pedestal" -> List.of("1", "2", "3", "4", "6", "7", "8", "9");
                    case "ingredient", "target" -> List.of("add", "remove");
                    case "preset" -> Arrays.stream(Preset.values()).map(p -> p.name().toLowerCase(Locale.ROOT)).toList();
                    case "settings" -> List.of(
                            "duration", "scale", "density",
                            "soundradius", "damageradius", "damage");
                    case "text", "sound" -> List.of(
                            "awaken", "lift", "orbit", "absorb", "finish");
                    case "music" -> List.of("public", "private", "off");
                    case "blast" -> List.of("lethal", "damage", "off");
                    case "effects" -> List.of("lightning", "shockwave");
                    case "aftermath" -> List.of("on", "off");
                    case "recover" -> List.of("refund", "result", "discard");
                    case "delete" -> List.of("confirm");
                    default -> List.of();
                };
            } else if (args.length == 4) {
                choices = switch (action) {
                    case "target" -> Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName).toList();
                    case "sound" -> List.of("private", "public");
                    case "effects" -> List.of("on", "off");
                    case "music" -> Registry.SOUNDS.stream().map(sound -> Registry.SOUNDS.getKeyOrThrow(sound).toString()).toList();
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
            } else if (args.length == 5 && action.equals("music")) {
                choices = List.of("9", "15", "30", "60");
            } else if (args.length == 6 && action.equals("music")) {
                choices = List.of("0.5", "0.8", "1.0");
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
'@
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionAltar.java'
        OriginalHash = '3b2ebd6d55376364e2498c4205662304e9e78afbd29beb2bf67b3b3399cbede7'
        Hash = 'b0b470f498df2fbea131a7ec4ca3b7f749007710edaf7688f66a14bcfe4b15e3'
        Source = @'
package com.bangzachery.vitae.vitaemanager.evolution;

import java.util.*;

public record EvolutionAltar(
        String id,
        Point table,
        String source,
        String result,
        List<Ingredient> ingredients,
        Set<UUID> targets,
        Preset preset,
        int ticks,
        double scale,
        int density,
        double soundRadius,
        double damageRadius,
        double damage,
        boolean enabled,
        Map<Stage, Cue> cues, Music music, Effects effects, boolean lethal, Aftermath aftermath
) {
    public EvolutionAltar {
        require(id != null && id.matches("[a-z0-9_-]{1,32}"),
                "ID: 1–32 huruf kecil, angka, _ atau -.");

        Objects.requireNonNull(table);
        Objects.requireNonNull(preset);
        Objects.requireNonNull(music); Objects.requireNonNull(effects); aftermath = Objects.requireNonNullElse(aftermath, Aftermath.defaults());
        encoded(source, true);
        encoded(result, true);

        List<Ingredient> positioned = new ArrayList<>();
        Set<Integer> slots = new HashSet<>();
        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ingredient = ingredients.get(i);
            int slot = ingredient.slot() == 0 ? EvolutionGrid.rawSlot(i) : ingredient.slot();
            require(slots.add(slot), "Slot bahan tidak boleh duplikat.");
            positioned.add(new Ingredient(ingredient.item(), ingredient.count(), ingredient.point(), slot));
        }
        ingredients = List.copyOf(positioned);
        targets = Set.copyOf(targets);
        cues = Map.copyOf(cues);

        require(ingredients.size() <= 8 && targets.size() <= 256,
                "Maksimal 8 bahan dan 256 penerima.");
        require(ticks >= 100 && ticks <= 2400,
                "Durasi harus 5–120 detik.");
        range(scale, .5, 2, "Scale");
        require(density >= 8 && density <= 64, "Density harus 8–64.");
        range(soundRadius, 1, 64, "Radius suara");
        range(damageRadius, 0, 16, "Radius damage");
        range(damage, 0, 1000, "Damage");
        require(music.sound().isEmpty() || music.ticks() == ticks, "Durasi musik harus sama dengan ritual.");

        Set<Point> places = new HashSet<>();
        for (Ingredient ingredient : ingredients) {
            if (ingredient.point() == null) continue;
            require(ingredient.point().distanceSquared(table) <= 256
                            && !ingredient.point().equals(table),
                    "Pedestal harus berbeda dari altar dan maksimal 16 block di world yang sama.");
            require(places.add(ingredient.point()),
                    "Pedestal bahan tidak boleh duplikat.");
        }

        // Validasi parameter: field record belum diisi pada compact constructor.
        require(!enabled || (!source.isEmpty() && !result.isEmpty()
                        && !ingredients.isEmpty() && !targets.isEmpty()
                        && ingredients.stream().allMatch(i -> i.point() != null)),
                "Lengkapi source, result, bahan, pedestal tiap slot, dan target sebelum enable.");
    }

    public record Aftermath(boolean enabled, int ticks) {
        public Aftermath { require(ticks >= 40 && ticks <= 300, "Adegan penutup harus 2–15 detik."); }
        public static Aftermath defaults() { return new Aftermath(true, 100); }
        public int quietTicks() { return 20; }
    }
    public EvolutionAltar(String id, Point table, String source, String result,
            List<Ingredient> ingredients, Set<UUID> targets, Preset preset,
            int ticks, double scale, int density, double soundRadius,
            double damageRadius, double damage, boolean enabled, Map<Stage, Cue> cues, Music music, Effects effects, boolean lethal) {
        this(id, table, source, result, ingredients, targets, preset, ticks, scale, density, soundRadius, damageRadius,
                damage, enabled, cues, music, effects, lethal, Aftermath.defaults());
    }
    public EvolutionAltar(String id, Point table, String source, String result,
            List<Ingredient> ingredients, Set<UUID> targets, Preset preset,
            int ticks, double scale, int density, double soundRadius,
            double damageRadius, double damage, boolean enabled, Map<Stage, Cue> cues) {
        this(id, table, source, result, ingredients, targets, preset, ticks, scale,
                density, soundRadius, damageRadius, damage, enabled, cues,
                Music.off(ticks), Effects.off(), false);
    }

    public record Music(String sound, boolean privateSound, float volume, int ticks) {
        public Music {
            require(sound != null && (sound.isEmpty() || sound.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")),
                    "Musik: namespace:nama atau off.");
            range(volume, 0, 1, "Volume musik");
            require(ticks >= 100 && ticks <= 2400, "Durasi musik 5–120 detik.");
        }
        public static Music off(int ticks) { return new Music("", false, .8f, ticks); }
    }
    public record Effects(boolean lightning, int interval, double radius, boolean shockwave) {
        public Effects {
            require(interval >= 10 && interval <= 200, "Jeda petir 0.5–10 detik.");
            range(radius, 0, 8, "Radius petir");
        }
        public static Effects off() { return new Effects(false, 30, 3, true); }
    }

    public static EvolutionAltar draft(String id, Point point) {
        return new EvolutionAltar(
                id, point, "", "", List.of(), Set.of(),
                Preset.INFERNO, 300, 1, 24, 24, 9, 0, false, Map.of(), Music.off(300), Effects.off(), false
        );
    }

    public boolean complete() {
        return !source.isEmpty() && !result.isEmpty()
                && !ingredients.isEmpty() && !targets.isEmpty()
                && ingredients.stream().allMatch(i -> i.point() != null);
    }

    public enum Preset {
        INFERNO, STORMFORGE, VOID, ASTRAL, FROST, PHOENIX;

        public static Preset parse(String text) {
            try {
                return valueOf(text.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Preset: inferno, stormforge, void, astral, frost, phoenix.");
            }
        }
    }

    public enum Stage {
        AWAKEN, LIFT, ORBIT, ABSORB, FINISH;

        public static Stage parse(String text) {
            try {
                return valueOf(text.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Tahap: awaken, lift, orbit, absorb, finish.");
            }
        }
    }

    public record Point(UUID world, int x, int y, int z) {
        public Point {
            Objects.requireNonNull(world);
            require(Math.abs((long) x) <= 30_000_000
                            && Math.abs((long) z) <= 30_000_000
                            && y >= -4096 && y <= 4096,
                    "Koordinat di luar batas.");
        }

        public double distanceSquared(Point other) {
            if (!world.equals(other.world)) return Double.POSITIVE_INFINITY;
            double dx = (double) x - other.x;
            double dy = (double) y - other.y;
            double dz = (double) z - other.z;
            return dx * dx + dy * dy + dz * dz;
        }

        public String encode() {
            return world + " " + x + " " + y + " " + z;
        }

        public static Point parse(String text) {
            String[] a = text.split(" ");
            require(a.length == 4, "Koordinat tidak valid.");
            return new Point(
                    UUID.fromString(a[0]),
                    Integer.parseInt(a[1]),
                    Integer.parseInt(a[2]),
                    Integer.parseInt(a[3])
            );
        }
    }

    public record Ingredient(String item, int count, Point point, int slot) {
        public Ingredient {
            encoded(item, false);
            require(slot == 0 || EvolutionGrid.ingredientSlot(slot), "Slot bahan: 1, 2, 3, 4, 6, 7, 8, 9; slot 5 khusus senjata.");
            require(count >= 1 && count <= 64,
                    "Jumlah bahan harus 1–64.");
        }
        public Ingredient(String item, int count, Point point) { this(item, count, point, 0); }
    }

    public record Cue(
            String title,
            String subtitle,
            String sound,
            boolean privateSound,
            float volume,
            float pitch
    ) {
        public Cue {
            require(title != null && subtitle != null
                            && title.length() <= 120 && subtitle.length() <= 240,
                    "Title maksimal 120, subtitle 240 karakter.");
            require(!title.matches("(?s).*[\\r\\n\\p{Cntrl}].*")
                            && !subtitle.matches("(?s).*[\\r\\n\\p{Cntrl}].*"),
                    "Teks harus satu baris.");
            require(sound != null && (sound.isEmpty()
                            || sound.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")),
                    "Sound: namespace:nama atau off.");
            range(volume, 0, 1, "Volume");
            range(pitch, .5, 2, "Pitch");
        }
    }

    public Cue cue(Stage stage) {
        if (cues.containsKey(stage)) return cues.get(stage);

        String[] sounds = switch (preset) {
            case INFERNO, PHOENIX -> new String[]{"block.fire.ambient", "entity.blaze.ambient",
                    "block.respawn_anchor.charge", "entity.blaze.shoot", "entity.generic.explode"};
            case STORMFORGE -> new String[]{"block.beacon.activate", "block.anvil.use",
                    "block.amethyst_block.resonate", "entity.lightning_bolt.thunder", "entity.generic.explode"};
            case VOID -> new String[]{"block.portal.ambient", "block.enchantment_table.use",
                    "block.respawn_anchor.ambient", "entity.enderman.teleport", "entity.generic.explode"};
            case ASTRAL -> new String[]{"block.beacon.activate", "block.amethyst_block.chime",
                    "block.amethyst_block.resonate", "block.respawn_anchor.charge", "entity.generic.explode"};
            case FROST -> new String[]{"block.glass.place", "block.amethyst_block.chime",
                    "block.beacon.ambient", "block.glass.break", "entity.generic.explode"};
        };

        String[] titles = {
                "Ritual dimulai", "Kekuatan terbangun",
                "Penyatuan", "Evolusi", "Evolusi selesai"
        };
        String[] subtitles = {
                "Bahan telah diterima", "Senjata menjawab panggilan",
                "Energi mengelilingi senjata", "Kekuatan baru terbentuk",
                "Ambil senjatamu"
        };

        return new Cue(
                titles[stage.ordinal()],
                subtitles[stage.ordinal()],
                "minecraft:" + sounds[stage.ordinal()],
                false, .5f, 1
        );
    }

    public static void encoded(String text, boolean emptyAllowed) {
        require(text != null && text.length() <= 180_000,
                "Data item tidak valid atau terlalu besar.");
        if (emptyAllowed && text.isEmpty()) return;
        byte[] bytes = Base64.getDecoder().decode(text);
        require(bytes.length > 0, "Data item kosong.");
    }

    public static void range(
            double value, double min, double max, String name
    ) {
        require(Double.isFinite(value) && value >= min && value <= max,
                name + " harus " + min + "–" + max + ".");
    }

    public static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
'@
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionService.java'
        OriginalHash = '63b142a5e3e9de801e60597402f5e7dde822a92a9ca154007b897d3d69fdfa35'
        Hash = '68ce8f6aff0fce2e86727f4fbe788d215cc9cb556d4ac5f56da292340f53ecb2'
        Source = @'
package com.bangzachery.vitae.vitaemanager.evolution;

import com.bangzachery.vitae.vitaemanager.core.message.MessageService;
import com.bangzachery.vitae.vitaemanager.items.ItemRulesService;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.Level;

import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionAltar.*;
import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionStore.*;

public final class EvolutionService implements Listener, AutoCloseable {
    private final JavaPlugin plugin;
    private final ItemRulesService rules;
    private final MessageService messages;
    private final EvolutionStore store;
    private final NamespacedKey visualKey, receiptKey, transferKey, refundKey, deliveryKey;
    private final EvolutionBlast blast = new EvolutionBlast();
    private final EvolutionMusic music = new EvolutionMusic();
    private final EvolutionLayout layout;
    private final Map<UUID, Map<Integer, ItemDisplay>> depositVisuals = new HashMap<>();
    private final ExecutorService writer;

    private State current, persisted;

    private final Map<UUID, Menu> menus = new HashMap<>();
    private final Map<UUID, Run> runs = new HashMap<>();
    private final Map<UUID, Run> finalAudio = new HashMap<>();
    private final Map<String, UUID> leases = new HashMap<>();
    private final Set<UUID> recovering = new HashSet<>();
    private final Map<UUID, Map<String, UUID>> soundOwners = new HashMap<>();

    private BukkitTask task;
    private int pendingWrites;
    private long clock;
    private boolean reloading;
    private volatile boolean closed;

    public EvolutionService(
            JavaPlugin plugin,
            ItemRulesService rules,
            MessageService messages,
            Set<String> vanillaSounds
    ) throws IOException, InvalidConfigurationException {
        this.plugin = plugin;
        this.rules = rules;
        this.messages = messages;

        store = new EvolutionStore(
                plugin.getDataFolder().toPath().resolve("evolution.yml"),
                vanillaSounds
        );

        current = store.initialize();
        validateItems(current);
        persisted = current;

        visualKey = new NamespacedKey(plugin, "evolution_visual");
        receiptKey = new NamespacedKey(plugin, "evolution_receipt");
        transferKey = new NamespacedKey(plugin, "evolution_transfer");
        refundKey = new NamespacedKey(plugin, "evolution_refund");
        deliveryKey = new NamespacedKey(plugin, "evolution_delivery");
        writer = Executors.newSingleThreadExecutor(
                action -> new Thread(action, "Vitae-Evolution-Writer"));
        layout = new EvolutionLayout(plugin, this);
    }

    public void start() {
        require(task == null && !closed,
                "Evolution sudah berjalan atau ditutup.");

        Bukkit.getWorlds().forEach(world ->
                world.getEntities().forEach(this::removeStale));

        current.deposits().values().forEach(d -> leases.put(d.altar(), d.owner()));
        Bukkit.getPluginManager().registerEvents(layout, plugin);
        task = Bukkit.getScheduler().runTaskTimer(
                plugin, this::advance, 2, 2);

        if (!pending().isEmpty()) {
            plugin.getLogger().warning(
                    "Evolution: periksa ritual tertunda dengan /evo pending.");
        }
    }

    public Set<String> ids() {
        return current.altars().keySet();
    }

    public EvolutionAltar altar(String id) {
        var altar = current.altars().get(id);
        require(altar != null, "Altar tidak ditemukan: " + id);
        return altar;
    }

    public List<Ticket> pending() {
        return current.tickets().values().stream()
                .filter(ticket -> !ticket.phase().terminal()).toList();
    }

    public List<Deposit> pendingDeposits() { return List.copyOf(current.deposits().values()); }

    public void checkSetup(String id) {
        editable(); altar(id);
        require(!layout.inUse(id), "Editor susunan altar sedang dibuka; tutup dahulu.");
        require(!leases.containsKey(id) && current.deposits().values().stream().noneMatch(d -> d.altar().equals(id)),
                "Altar masih memiliki ritual atau setoran.");
    }
    void templateAllowed(ItemStack item) {
        require(usable(item) && item.getAmount() <= 64 && !rules.blocked(item), "Contoh bahan kosong, terlalu banyak, atau diblokir.");
        template(encode(item.asOne()));
    }
    public void layout(Player admin, String id) { layout.open(admin, id); }

    public void say(CommandSender sender, String text) {
        sender.sendMessage(messages.component("prefix")
                .append(Component.text(text, NamedTextColor.GRAY)));
    }

    public void create(String id, Block block, CommandSender sender) {
        editable();
        require(!ids().contains(id), "ID altar sudah ada.");
        require(block.getType() == Material.CRAFTING_TABLE,
                "Lihat crafting table untuk create.");

        EvolutionAltar draft = EvolutionAltar.draft(id, point(block));
        current.put(draft);

        submit(
                state -> state.put(draft),
                () -> say(sender, "Altar dibuat. Lanjutkan /evo help setup."),
                () -> saveFailed(sender)
        );
    }

    public void edit(
            String id,
            Consumer<ConfigurationSection> change,
            CommandSender sender
    ) {
        checkSetup(id);

        var yaml = store.document(current);
        change.accept(yaml.getConfigurationSection("altars." + id));

        EvolutionAltar next = store.parse(yaml).altars().get(id);
        validateItems(next);
        if (next.enabled()) ready(next);

        submit(
                state -> state.put(next),
                () -> say(sender, "Setup altar tersimpan."),
                () -> saveFailed(sender)
        );
    }

    public void delete(String id, CommandSender sender) {
        checkSetup(id);

        submit(
                state -> state.remove(id),
                () -> say(sender, "Altar dihapus."),
                () -> saveFailed(sender)
        );
    }

    public void reload(CommandSender sender) {
        editable();
        require(layout.empty() && menus.isEmpty() && runs.isEmpty() && recovering.isEmpty(),
                "Tutup GUI dan ritual sebelum reload.");

        reloading = true;
        pendingWrites++;

        writer.execute(() -> {
            try {
                State loaded = store.load();
                post(() -> {
                    try {
                        validateItems(loaded);
                    } catch (RuntimeException exception) {
                        pendingWrites--;
                        reloading = false;
                        failure(exception);
                        saveFailed(sender);
                        return;
                    }

                    writer.execute(() -> {
                        persisted = loaded;
                        post(() -> {
                            current = loaded;
                            pendingWrites--;
                            reloading = false;
                            clearDepositVisuals(); leases.clear();
                            current.deposits().values().forEach(d -> leases.put(d.altar(), d.owner()));
                            say(sender, "Evolution dimuat ulang.");
                        });
                    });
                });
            } catch (Exception exception) {
                failure(exception);
                post(() -> {
                    pendingWrites--;
                    reloading = false;
                    saveFailed(sender);
                });
            }
        });
    }

    public String sample(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        require(usable(item), "Pegang contoh item di tangan utama.");
        require(!rules.blocked(item), "Item diblokir aturan item.");
        return encode(item.asOne());
    }

    private void editable() {
        require(!closed && !reloading && pendingWrites == 0,
                "Masih menyimpan data. Tunggu sebentar lalu ulangi.");
    }

    private void submit(
            UnaryOperator<State> operation,
            Runnable success,
            Runnable failed
    ) {
        pendingWrites++;
        writer.execute(() -> {
            try {
                State next = operation.apply(persisted);
                store.save(next);
                persisted = next;

                post(() -> {
                    current = next;
                    pendingWrites--;
                    success.run();
                });
            } catch (Exception exception) {
                failure(exception);
                post(() -> {
                    pendingWrites--;
                    failed.run();
                });
            }
        });
    }

    private void post(Runnable callback) {
        if (closed) return;
        try {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!closed) callback.run();
            });
        } catch (IllegalPluginAccessException ignored) {
        }
    }

    private void failure(Exception exception) {
        plugin.getLogger().log(
                Level.WARNING,
                "Evolution: operasi gagal; periksa log dan /evo pending.",
                exception
        );
    }

    private void saveFailed(CommandSender sender) {
        say(sender,
                "Gagal menyimpan/memuat data; snapshot terakhir dipertahankan. Periksa console.");
    }

    private void validateItems(State state) {
        state.altars().values().forEach(EvolutionService::validateItems);
        state.deposits().values().forEach(deposit -> {
            EvolutionAltar altar = state.altars().get(deposit.altar()); validateDeposit(altar, deposit.items());
            if (deposit.transfer() != null) { decode(deposit.transfer().cursor()); validateDeposit(altar, deposit.transfer().next()); }
        });
        state.tickets().values().forEach(ticket -> {
            ticket.inputs().forEach(EvolutionService::decode);
            template(ticket.result());
        });
    }

    private static void validateItems(EvolutionAltar altar) {
        if (!altar.source().isEmpty()) template(altar.source());
        if (!altar.result().isEmpty()) template(altar.result());
        altar.ingredients().forEach(ingredient -> template(ingredient.item()));
    }

    private void ready(EvolutionAltar altar) {
        require(altar.complete(),
                "Lengkapi source, result, bahan, pedestal tiap slot dan target.");
        require(block(altar.table()).getType() == Material.CRAFTING_TABLE,
                "Crafting table altar hilang.");
        require(!rules.blocked(decode(altar.source()))
                        && !rules.blocked(decode(altar.result())),
                "Senjata diblokir aturan item.");

        for (Ingredient ingredient : altar.ingredients()) {
            require(block(ingredient.point()).getType().isSolid(),
                    "Pedestal harus berupa block solid.");
            require(!rules.blocked(decode(ingredient.item())),
                    "Bahan diblokir aturan item.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void interact(PlayerInteractEvent event) {
        if (closed || event.getHand() != EquipmentSlot.HAND
                || event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getClickedBlock() == null) return;

        EvolutionAltar altar = current.altars().values().stream()
                .filter(a -> a.table().equals(point(event.getClickedBlock())))
                .findFirst().orElse(null);

        if (altar == null) return;
        event.setCancelled(true);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (closed || !event.getPlayer().isOnline()) return;
            try {
                open(event.getPlayer(), altar.id());
            } catch (RuntimeException exception) {
                say(event.getPlayer(), exception.getMessage());
            }
        });
    }

    private void available(Player player, EvolutionAltar altar) {
        require(!layout.inUse(altar.id()), "Admin sedang mengatur susunan altar ini.");
        require(!closed && !reloading && pendingWrites == 0 && !player.isDead(),
                "Evolution sedang menyimpan data; tunggu sebentar.");
        UUID owner = player.getUniqueId();
        require(!menus.containsKey(owner) && !runs.containsKey(owner) && !recovering.contains(owner),
                "Kamu sedang menjalankan ritual.");
        require(!leases.containsKey(altar.id()) || leases.get(altar.id()).equals(owner),
                "Altar sedang digunakan atau menyimpan bahan pemain lain.");
        Deposit deposit = current.deposits().get(owner);
        require(deposit == null || deposit.altar().equals(altar.id()), "Selesaikan setoran di altar lain, atau /evo cancel dahulu.");
        require(deposit == null || !hasReceipt(player, refundKey, deposit.id()), "Setoran telah direfund; admin perlu menutup catatannya.");
        require(deposit == null || deposit.transfer() == null, "Transaksi tertunda; minta admin memeriksa /evo pending.");
        require(menus.size() + runs.size() < 32, "Terlalu banyak ritual berjalan.");
        Ticket ticket = current.tickets().get(owner);
        require(ticket == null || ticket.phase().terminal(), "Ritual sebelumnya perlu diperiksa admin.");
    }

    private void open(Player player, String id) {
        EvolutionAltar altar = altar(id);
        require(player.hasPermission("vitae.evolution"), "Kamu tidak memiliki izin ritual.");
        require(altar.enabled() && altar.targets().contains(player.getUniqueId()), "Altar belum aktif atau belum ditujukan untukmu.");
        require(survival(player) && !usable(player.getItemOnCursor()), "Gunakan survival/adventure dan kosongkan cursor.");
        available(player, altar); ready(altar);
        require(near(player, altar.table(), 8), "Kamu terlalu jauh dari altar.");
        stopFinal(player.getUniqueId());
        InventoryView view = player.openWorkbench(block(altar.table()).getLocation(), true);
        require(view != null && view.getTopInventory() instanceof CraftingInventory, "Crafting table tidak dapat dibuka.");
        Menu menu = new Menu(player.getUniqueId(), altar, (CraftingInventory) view.getTopInventory());
        menus.put(menu.owner, menu); leases.put(id, menu.owner);
        try {
            render(menu);
            say(player, "Ambil bahan ke cursor, lalu taruh/drag ke slot bahan yang cocok. Senjata terakhir di tengah. Hasil kanan hanya preview.");
            say(player, "Setoran tersimpan saat GUI ditutup. /evo cancel untuk refund bahan.");
            Deposit d = current.deposits().get(menu.owner);
            if (d != null && complete(menu.altar, d.items())) begin(player, menu);
        } catch (RuntimeException exception) { removeMenu(menu); player.closeInventory(); throw exception; }
    }
    private Menu menu(InventoryView view) {
        Menu menu = menus.get(view.getPlayer().getUniqueId());
        return menu != null && view.getTopInventory() == menu.inventory ? menu : null;
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void craft(PrepareItemCraftEvent event) {
        Menu menu = menu(event.getView());
        if (menu != null && !menu.rendering) menu.inventory.setResult(decode(menu.altar.result()).asOne());
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void recipeBook(com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent event) {
        // Autofill bisa mengembalikan matrix preview ke storage tanpa InventoryClickEvent.
        if (menu(event.getPlayer().getOpenInventory()) != null) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void click(InventoryClickEvent event) {
        Menu menu = menu(event.getView()); if (menu == null) return;
        boolean top = event.getRawSlot() >= 0 && event.getRawSlot() < 10;
        boolean simple = event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT;
        boolean denied = event.isCancelled();
        // Shift, number-key, offhand dan collect tidak boleh mengambil preview.
        if (!top && simple && !menu.pending && !denied && !(event instanceof InventoryCreativeEvent)) return;
        event.setCancelled(true);
        if (denied || event instanceof InventoryCreativeEvent || menu.pending || !simple || !top
                || !(event.getWhoClicked() instanceof Player player)) return;
        int index = EvolutionGrid.index(menu.altar, event.getRawSlot()); if (index < 0) return;
        ItemStack cursor = event.getCursor();
        if (!usable(cursor)) { describe(player, menu, index); return; }
        if (rules.blocked(cursor)) return;
        try {
            int count = amountFor(menu, index, cursor, event.getClick() == ClickType.RIGHT);
            require(count > 0, "Slot ini sudah selesai atau tidak dipakai.");
            acceptLater(player, menu, cursor.clone(), Map.of(index, count));
        } catch (RuntimeException exception) { say(player, exception.getMessage()); }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void drag(InventoryDragEvent event) {
        Menu menu = menu(event.getView()); if (menu == null) return;
        boolean top = event.getRawSlots().stream().anyMatch(slot -> slot < 10);
        if (!top && !menu.pending) return;
        boolean denied = event.isCancelled(); event.setCancelled(true);
        if (denied || menu.pending || !top || !(event.getWhoClicked() instanceof Player player)) return;
        try {
            require(event.getRawSlots().stream().allMatch(slot -> slot >= 1 && slot < 10), "Drag bahan di slot crafting saja.");
            ItemStack cursor = event.getOldCursor(); require(usable(cursor) && !rules.blocked(cursor), "Bahan tidak tersedia.");
            Map<Integer, Integer> additions = new HashMap<>();
            for (var entry : event.getNewItems().entrySet()) {
                int index = EvolutionGrid.index(menu.altar, entry.getKey()); ItemStack old = menu.inventory.getItem(entry.getKey());
                int requested = entry.getValue().getAmount() - (usable(old) ? old.getAmount() : 0);
                int count = Math.min(requested, amountFor(menu, index, cursor, false));
                require(count > 0, "Drag hanya ke slot bahan yang belum lengkap."); additions.put(index, count);
            }
            require(additions.values().stream().mapToInt(Integer::intValue).sum() <= cursor.getAmount(), "Bahan pada cursor tidak cukup.");
            acceptLater(player, menu, cursor.clone(), additions);
        } catch (RuntimeException exception) { say(player, exception.getMessage()); }
    }
    private void acceptLater(Player player, Menu menu, ItemStack cursor, Map<Integer, Integer> additions) {
        menu.pending = true;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (closed || menus.get(menu.owner) != menu || !player.isOnline()
                    || !cursor.equals(player.getItemOnCursor())) { menu.pending = false; return; }
            try { deposit(player, menu, cursor, additions); }
            catch (RuntimeException exception) { menu.pending = false; say(player, exception.getMessage()); }
        });
    }
    @EventHandler(priority = EventPriority.LOWEST)
    public void closeMenu(InventoryCloseEvent event) {
        Menu menu = menu(event.getView()); if (menu != null) removeMenu(menu);
    }
    private void describe(Player player, Menu menu, int index) {
        if (index == 8) say(player, "Senjata wajib ditaruh terakhir, setelah semua slot bahan hijau.");
        else if (index < menu.altar.ingredients().size()) say(player, "Bahan " + (index + 1) + ": "
                + amounts(items(menu.owner)).getOrDefault(index, 0) + "/" + menu.altar.ingredients().get(index).count());
    }
    private Map<Integer, String> items(UUID owner) {
        Deposit d = current.deposits().get(owner); return d == null ? Map.of() : d.items();
    }
    private static Map<Integer, Integer> amounts(Map<Integer, String> items) {
        Map<Integer, Integer> amounts = new HashMap<>();
        items.forEach((slot, bytes) -> amounts.put(slot, decode(bytes).getAmount())); return amounts;
    }
    private static boolean complete(EvolutionAltar altar, Map<Integer, String> items) {
        Map<Integer, Integer> amounts = amounts(items);
        return EvolutionGrid.materialsComplete(altar, amounts) && amounts.getOrDefault(8, 0) == 1;
    }
    private void validateDeposit(EvolutionAltar altar, Map<Integer, String> items) {
        for (var e : items.entrySet()) {
            int index = e.getKey(); ItemStack actual = decode(e.getValue());
            require(index == 8 || index < altar.ingredients().size(), "Slot setoran tidak sesuai altar.");
            int max = index == 8 ? 1 : altar.ingredients().get(index).count();
            require(actual.getAmount() >= 1 && actual.getAmount() <= max, "Jumlah setoran melebihi kebutuhan.");
            require(matches(actual, index == 8 ? altar.source() : altar.ingredients().get(index).item(), index == 8),
                    "Item setoran tidak sesuai konfigurasi altar.");
        }
        require(!items.containsKey(8) || complete(altar, items), "Senjata disetor sebelum bahan lengkap.");
    }
    private int amountFor(Menu menu, int index, ItemStack cursor, boolean single) {
        require(index >= 0 && (index == 8 || index < menu.altar.ingredients().size()), "Slot ini tidak dipakai.");
        Map<Integer, Integer> amounts = amounts(items(menu.owner));
        if (index == 8) require(EvolutionGrid.materialsComplete(menu.altar, amounts), "Isi seluruh bahan di pinggir sampai hijau, baru senjata di tengah.");
        require(matches(cursor, index == 8 ? menu.altar.source() : menu.altar.ingredients().get(index).item(), index == 8), "Item tidak cocok untuk slot ini.");
        return EvolutionGrid.accepted(index == 8 ? 1 : menu.altar.ingredients().get(index).count(),
                amounts.getOrDefault(index, 0), cursor.getAmount(), single);
    }
    private void deposit(Player player, Menu menu, ItemStack cursor, Map<Integer, Integer> additions) {
        ready(menu.altar);
        require(near(player, menu.altar.table(), 8) && survival(player) && !player.isDead(), "Pemain meninggalkan altar.");
        Deposit old = current.deposits().get(menu.owner); require(old == null || old.transfer() == null, "Masih menyimpan setoran.");
        Map<Integer, String> next = new HashMap<>(items(menu.owner)); int used = 0;
        for (var e : additions.entrySet()) {
            require(e.getValue() > 0 && e.getValue() <= amountFor(menu, e.getKey(), cursor, false), "Setoran berubah.");
            int before = next.containsKey(e.getKey()) ? decode(next.get(e.getKey())).getAmount() : 0;
            next.put(e.getKey(), encode(cursor.asQuantity(before + e.getValue()))); used += e.getValue();
        }
        require(used > 0 && used <= cursor.getAmount(), "Bahan tidak cukup.");
        UUID id = old == null ? UUID.randomUUID() : old.id();
        Transfer transfer = new Transfer(UUID.randomUUID(), encode(cursor), next);
        Deposit prepared = old == null ? new Deposit(id, menu.owner, menu.altar.id(), menu.altar.table(), Map.of(), transfer) : old.pending(transfer);
        int consumed = used;
        submit(state -> state.deposit(prepared), () -> {
            if (!player.isOnline() || player.isDead() || menus.get(menu.owner) != menu
                    || !cursor.equals(player.getItemOnCursor())) { settleDeposit(prepared, false, menu, player); return; }
            try {
                // Cursor tidak disimpan di player.dat. Sisa stack dipindah ke storage sebelum save.
                player.setItemOnCursor(null);
                if (cursor.getAmount() > consumed) give(player, List.of(cursor.asQuantity(cursor.getAmount() - consumed)), transfer.id());
                player.getPersistentDataContainer().set(transferKey, PersistentDataType.STRING, transfer.id().toString()); player.saveData();
            } catch (RuntimeException exception) {
                menu.pending = false; player.closeInventory(); pendingMessage(player); failure(exception); return;
            }
            settleDeposit(prepared, true, menu, player);
        }, () -> { menu.pending = false; releaseIfEmpty(menu); saveFailed(player); });
    }
    private void settleDeposit(Deposit prepared, boolean consumed, Menu menu, Player player) {
        submit(state -> state.settle(prepared.owner(), prepared.id(), prepared.transfer().id(), consumed), () -> {
            menu.pending = false; Deposit d = current.deposits().get(prepared.owner());
            if (d != null) {
                paintDeposit(d);
                if (consumed) for (int index : EvolutionGrid.newlyComplete(menu.altar, amounts(prepared.items()), amounts(d.items()))) {
                    Point point = menu.altar.ingredients().get(index).point(); World world = Bukkit.getWorld(point.world());
                    if (world != null && world.isChunkLoaded(point.x() >> 4, point.z() >> 4)) world.strikeLightningEffect(location(point));
                }
            } else releaseIfEmpty(menu);
            if (menus.get(menu.owner) == menu && player.isOnline()) render(menu);
            if (d != null && complete(menu.altar, d.items()) && player.isOnline() && !player.isDead()
                    && survival(player) && near(player, menu.altar.table(), 8)) begin(player, menu);
        }, () -> { menu.pending = false; if (player.isOnline()) player.closeInventory(); pendingMessage(player); });
    }
    private void releaseIfEmpty(Menu menu) {
        if (!current.deposits().containsKey(menu.owner) && !menus.containsKey(menu.owner) && !runs.containsKey(menu.owner))
            releaseDeposit(menu.owner, menu.altar.id());
    }
    private void render(Menu menu) {
        menu.rendering = true;
        try {
            menu.inventory.clear(); Map<Integer, Integer> amounts = amounts(items(menu.owner));
            for (int i = 0; i < 8; i++) menu.inventory.setItem(EvolutionGrid.rawSlot(i), icon(Material.GRAY_STAINED_GLASS_PANE, "Tidak dipakai", false));
            for (int i = 0; i < menu.altar.ingredients().size(); i++) {
                Ingredient ingredient = menu.altar.ingredients().get(i);
                ItemStack item = amounts.getOrDefault(i, 0) == ingredient.count()
                        ? icon(Material.LIME_STAINED_GLASS_PANE, "Slot " + ingredient.slot() + " selesai", true)
                        : decode(ingredient.item()).asOne();
                menu.inventory.setItem(ingredient.slot(), item);
            }
            menu.inventory.setItem(5, decode(menu.altar.source()).asOne()); menu.inventory.setResult(decode(menu.altar.result()).asOne());
            Deposit d = current.deposits().get(menu.owner); if (d != null) paintDeposit(d);
        } finally { menu.rendering = false; }
    }
    private void paintDeposit(Deposit deposit) {
        EvolutionAltar altar = current.altars().get(deposit.altar());
        if (altar == null || Bukkit.getWorld(altar.table().world()) == null) return;
        var visuals = depositVisuals.computeIfAbsent(deposit.owner(), owner -> new HashMap<>());
        deposit.items().forEach((index, bytes) -> {
            ItemDisplay old = visuals.get(index); if (old != null && old.isValid()) return;
            Point point = index == 8 ? altar.table() : altar.ingredients().get(index).point(); World world = Bukkit.getWorld(point.world());
            if (world != null && world.isChunkLoaded(point.x() >> 4, point.z() >> 4))
                visuals.put(index, display(point, decode(bytes), altar.scale() * (index == 8 ? 1 : .65)));
        });
    }
    private void begin(Player player, Menu menu) {
        Deposit d = current.deposits().get(menu.owner);
        require(d != null && d.transfer() == null && complete(menu.altar, d.items()), "Setoran belum lengkap."); ready(menu.altar);
        Ticket ticket = new Ticket(UUID.randomUUID(), menu.owner, menu.altar.id(), menu.altar.table(), Phase.PREPARED,
                new TreeMap<>(d.items()).values().stream().toList(), menu.altar.result());
        menu.pending = true;
        submit(state -> state.beginDeposit(ticket, d.id()), () -> {
            Map<Integer, ItemDisplay> visuals = depositVisuals.remove(menu.owner); if (visuals == null) visuals = new HashMap<>();
            Run run = new Run(menu.owner, menu.altar, false, visuals, ticket); run.consumed = true; runs.put(run.owner, run);
            removeMenu(menu); leases.put(menu.altar.id(), menu.owner);
            if (player.isOnline()) player.closeInventory();
            try {
                require(player.isOnline() && !player.isDead() && survival(player) && near(player, menu.altar.table(), 8), "Pemilik meninggalkan altar.");
                for (var e : d.items().entrySet()) if (!run.visuals.containsKey(e.getKey()) || !run.visuals.get(e.getKey()).isValid()) {
                    Point point = e.getKey() == 8 ? menu.altar.table() : menu.altar.ingredients().get(e.getKey()).point();
                    run.visuals.put(e.getKey(), display(point, decode(e.getValue()), menu.altar.scale() * (e.getKey() == 8 ? 1 : .65)));
                }
                checkpoint(run, Phase.ACTIVE, () -> {
                    if (live(run)) { run.active = true; say(player, "Ritual dimulai. Saksikan evolusinya!"); }
                }, () -> abort(run, player, null, "Gagal menyimpan ritual."));
            } catch (RuntimeException e) { abort(run, player, null, e.getMessage()); }
        }, () -> { menu.pending = false; saveFailed(player); });
    }
    private void give(Player player, List<ItemStack> items, UUID receipt) {
        for (ItemStack item : items) player.getInventory().addItem(item.clone()).values()
                .forEach(left -> drop(player.getLocation(), left, player.getUniqueId(), receipt));
    }
    private void releaseDeposit(UUID owner, String altar) {
        var visuals = depositVisuals.remove(owner); if (visuals != null) removeVisuals(visuals); leases.remove(altar, owner);
    }
    private void clearDepositVisuals() { depositVisuals.values().forEach(EvolutionService::removeVisuals); depositVisuals.clear(); }

    private void checkpoint(
            Run run, Phase phase, Runnable success, Runnable failed
    ) {
        submit(
                state -> state.phase(run.owner, run.id, phase),
                success, failed
        );
    }

    public void preview(Player player, String id) {
        EvolutionAltar altar = altar(id);
        available(player, altar);
        ready(altar);
        require(!current.deposits().containsKey(player.getUniqueId()), "Batalkan setoran sebelum preview.");
        require(near(player, altar.table(), 10),
                "Datang ke altar sebelum preview.");
        stopFinal(player.getUniqueId());

        Map<Integer, ItemDisplay> visuals = new HashMap<>();
        try {
            for (int i = 0; i < altar.ingredients().size(); i++) {
                var ingredient = altar.ingredients().get(i);
                visuals.put(i, display(
                        ingredient.point(), decode(ingredient.item()),
                        altar.scale() * .65));
            }
            visuals.put(8, display(
                    altar.table(), decode(altar.source()), altar.scale()));

            Run run = new Run(
                    player.getUniqueId(), altar, true, visuals, null);
            run.active = true;
            runs.put(run.owner, run);
            leases.put(id, run.owner);
            say(player,
                    "Preview: tidak mengonsumsi item, membuat hasil, atau memberikan damage.");
        } catch (RuntimeException exception) {
            removeVisuals(visuals);
            throw exception;
        }
    }

    public void control(Player player, String action) {
        Menu menu = menus.get(player.getUniqueId());
        if (action.equals("cancel") && current.deposits().containsKey(player.getUniqueId())) {
            require(menu == null || !menu.pending, "Tunggu setoran selesai disimpan.");
            if (menu != null) player.closeInventory(); recover(player.getUniqueId(), "refund", player); return;
        }
        if (action.equals("cancel") && menu != null) {
            require(!menu.pending, "Tunggu setoran selesai disimpan.");
            player.closeInventory(); say(player, "Persiapan dibatalkan."); return;
        }

        Run run = runs.get(player.getUniqueId());
        require(run != null, "Pemain tidak sedang menjalankan ritual.");

        switch (action) {
            case "cancel" -> abort(
                    run, player, null, "Ritual dibatalkan.");
            case "pause" -> {
                require(run.active && !run.paused && !run.finishing && !run.exploded,
                        "Ritual belum dapat dijeda.");
                run.paused = true;
                stopAudio(run); music.stop(run.id);
                player.clearTitle();
                say(player, "Ritual dijeda.");
            }
            case "resume" -> {
                require(run.active && run.paused,
                        "Ritual tidak sedang dijeda.");
                run.paused = false;
                run.stage = null;
                if (!run.altar.music().sound().isEmpty()) run.elapsed = 0;
                run.musicStarted = false;
                say(player, run.altar.music().sound().isEmpty() ? "Ritual dilanjutkan."
                        : "Animasi diulang dari awal agar musik tetap sinkron.");
            }
            default -> throw new IllegalArgumentException(
                    "Kontrol tidak dikenal.");
        }
    }

    private void advance() {
        if (closed) return;
        clock += 2;

        for (Menu menu : List.copyOf(menus.values())) {
            Player player = Bukkit.getPlayer(menu.owner);
            try {
                require(player != null && !player.isDead()
                                && near(player, menu.altar.table(), 8),
                        "GUI ditutup karena meninggalkan altar.");
            } catch (RuntimeException exception) {
                if (player != null) player.closeInventory();
                removeMenu(menu);
            }
        }

        for (Run run : List.copyOf(runs.values())) {
            Player player = Bukkit.getPlayer(run.owner);
            try {
                if (run.exploded) { advanceReveal(run, player); continue; }
                require(player != null && !player.isDead()
                                && near(player, run.altar.table(), 12),
                        "Pemain meninggalkan altar.");
                require(block(run.altar.table()).getType()
                                == Material.CRAFTING_TABLE,
                        "Crafting table hilang.");
                require(run.altar.ingredients().stream()
                                .allMatch(i -> block(i.point()).getType().isSolid()),
                        "Pedestal hilang.");
                require(run.visuals.values().stream().allMatch(Entity::isValid),
                        "Visual ritual terputus.");

                if (!run.active || run.paused || run.finishing) continue;

                // Checkpoint hasil disiapkan satu detik sebelum ledakan, tanpa memperpanjang timeline normal.
                if (!run.preview && !run.commitRequested && run.elapsed >= run.altar.ticks() - 20) {
                    run.commitRequested = true;
                    checkpoint(run, Phase.COMMITTING, () -> run.commitReady = true,
                            () -> abort(run, player, null, "Hasil gagal disiapkan."));
                }
                if (!run.musicStarted) { music.start(run.id, run.owner, run.altar); run.musicStarted = true; }
                music.trim(run.id);
                if (run.elapsed >= run.altar.ticks()) {
                    if (run.preview || run.commitReady) finish(run, player);
                    continue;
                }

                var frame = EvolutionAnimation.frame(
                        run.altar.preset(),
                        run.elapsed / (double) run.altar.ticks(),
                        run.elapsed,
                        run.origins
                );

                if (frame.stage() != run.stage) {
                    run.stage = frame.stage();
                    cue(run, run.stage);
                }

                animate(run, frame);
                run.elapsed += 2;
            } catch (RuntimeException exception) {
                abort(run, player, null, exception.getMessage());
                failure(exception);
            }
        }

        if (clock % 40 == 0) current.deposits().values().forEach(this::paintDeposit);
        for (Run run : List.copyOf(finalAudio.values())) {
            if (clock >= run.audioUntil) stopFinal(run.owner);
        }
    }

    private void animate(Run run, EvolutionAnimation.Frame frame) {
        Location base = location(run.altar.table());
        Location weapon = offset(base, frame.weapon());
        Particle particle = particle(run.altar.preset());

        move(run.visuals.get(8), weapon, frame.rotation(),
                frame.tilt(), run.altar.scale());

        for (int i = 0; i < frame.ingredients().size(); i++) {
            Location position = offset(base, frame.ingredients().get(i));
            move(run.visuals.get(i), position,
                    -frame.rotation(), 0, run.altar.scale() * .65);

            if (run.elapsed >= run.altar.ticks() * .15) {
                position.getWorld().spawnParticle(
                        particle, position, 1, .03, .03, .03, .01);
            }
        }

        weapon.getWorld().spawnParticle(
                particle, weapon, Math.max(1, run.altar.density() / 8),
                .12, .15, .12, .01);

        if (run.altar.effects().lightning() && run.elapsed > 0
                && run.elapsed % run.altar.effects().interval() < 2) {
            double angle = run.elapsed * .71;
            Location strike = altarCenter(run.altar.table()).add(Math.cos(angle) * run.altar.effects().radius(),
                    0, Math.sin(angle) * run.altar.effects().radius());
            strike.getWorld().strikeLightningEffect(strike);
        }
        if (run.altar.preset() == Preset.INFERNO) {
            for (int i = 0; i < 3; i++) {
                double angle = frame.rotation() + i * Math.PI * 2 / 3;
                base.getWorld().spawnParticle(
                        Particle.FLAME,
                        base.clone().add(
                                Math.cos(angle) * .7,
                                frame.weapon().y() * i / 3,
                                Math.sin(angle) * .7),
                        1, 0, .05, 0, 0
                );
            }
        } else if (run.altar.preset() == Preset.STORMFORGE
                && run.elapsed % 12 == 0) {
            weapon.getWorld().spawnParticle(
                    Particle.ELECTRIC_SPARK, weapon,
                    run.altar.density() / 2, .8, .15, .8, .1);
        } else if (run.altar.preset() == Preset.VOID) {
            base.getWorld().spawnParticle(
                    Particle.PORTAL, base,
                    Math.max(1, run.altar.density() / 8),
                    .6, .05, .6, .05);
        } else if (run.altar.preset() == Preset.ASTRAL) {
            for (int i = 0; i < run.altar.density(); i++) {
                double a = i * Math.PI * 2 / run.altar.density() + frame.rotation();
                double r = 1.4 * (1 - Math.max(0, (run.elapsed / (double) run.altar.ticks() - .7) / .3));
                weapon.getWorld().spawnParticle(Particle.END_ROD,
                        weapon.clone().add(Math.cos(a) * r, Math.sin(a) * r * .7, Math.sin(a) * r * .6), 1, 0, 0, 0, 0);
            }
        } else if (run.altar.preset() == Preset.FROST) {
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3 + frame.rotation();
                for (int arm = 1; arm <= 3; arm++) base.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        base.clone().add(Math.cos(a) * arm * .6, .03, Math.sin(a) * arm * .6), 1, 0, 0, 0, 0);
            }
        } else if (run.altar.preset() == Preset.PHOENIX) {
            for (int wing : new int[]{-1, 1}) for (int i = 1; i <= 8; i++) {
                double flap = Math.sin(run.elapsed * .12) * .5;
                weapon.getWorld().spawnParticle(Particle.FLAME,
                        weapon.clone().add(wing * i * .22, Math.sin(i * Math.PI / 8) * (1 + flap), -.15 * i), 1, .02, .02, .02, 0);
            }
        }
    }

    private void finish(Run run, Player player) {
        if (run.altar.aftermath().enabled()) { startReveal(run, player); return; }
        if (run.preview) {
            completeVisual(run, player);
            return;
        }

        require(!rules.blocked(decode(run.ticket.result())),
                "Senjata hasil diblokir aturan item.");
        run.finishing = true;

        require(run.commitReady, "Hasil belum siap.");
        if (!live(run)) return;
        try {
                require(player.isOnline() && !player.isDead()
                                && near(player, run.altar.table(), 12),
                        "Pemilik tidak tersedia.");

                // Lepas run sebelum damage agar kematian tidak me-refund input.
                removeRun(run);
                deliver(player, run.ticket, location(run.altar.table()));

                checkpoint(
                        run,
                        Phase.DELIVERED,
                        () -> say(player,
                                "Evolusi selesai. Senjata berada di atas altar."),
                        () -> pendingMessage(player)
                );

                completeVisual(run, player);

                if ((run.altar.lethal() || run.altar.damage() > 0) && run.altar.damageRadius() > 0) {
                    Location center = altarCenter(run.altar.table());
                    for (Player nearby : List.copyOf(center.getWorld().getPlayers()))
                        if (nearby.getLocation().distanceSquared(center) <= run.altar.damageRadius() * run.altar.damageRadius())
                            blast.hit(nearby, run.altar.lethal(), run.altar.damage());
                }
            } catch (RuntimeException exception) {
                if (live(run)) {
                    abort(run, player, null, exception.getMessage());
                } else {
                    pendingMessage(player);
                    failure(exception);
                }
            }
    }

    private void startReveal(Run run, Player player) {
        if (run.finishing) return;
        run.finishing = true;
        if (run.preview) { explodeReveal(run); return; }
        require(run.commitReady, "Hasil belum siap.");
        require(!rules.blocked(decode(run.ticket.result())), "Senjata hasil diblokir aturan item.");
        // This checkpoint makes death/disconnect during the blast preserve the result, never refund the old weapon.
        run.uncertain = true;
        checkpoint(run, Phase.REVEALING, () -> {
            run.uncertain = false;
            if (live(run)) try { explodeReveal(run); }
            catch (RuntimeException error) { abort(run, Bukkit.getPlayer(run.owner), null, "Adegan hasil terputus."); failure(error); }
        }, () -> { run.uncertain = false; abort(run, Bukkit.getPlayer(run.owner), null, "Adegan hasil gagal disimpan."); });
    }
    private void explodeReveal(Run run) {
        run.exploded = true; run.finishing = false; run.revealAge = 0;
        music.stop(run.id); stopAudio(run);
        Player owner = Bukkit.getPlayer(run.owner); if (owner != null) owner.clearTitle();
        for (int key : List.copyOf(run.visuals.keySet())) if (key != 8) removeVisual(run.visuals, key);
        run.visuals.get(8).setItemStack(new ItemStack(Material.AIR));
        explosionParticles(run); cueSound(run, run.altar.cue(Stage.FINISH));
        if (!run.preview && (run.altar.lethal() || run.altar.damage() > 0) && run.altar.damageRadius() > 0) {
            Location center = altarCenter(run.altar.table());
            for (Player nearby : List.copyOf(center.getWorld().getPlayers()))
                if (nearby.getLocation().distanceSquared(center) <= run.altar.damageRadius() * run.altar.damageRadius())
                    blast.hit(nearby, run.altar.lethal(), run.altar.damage());
        }
    }
    private void advanceReveal(Run run, Player player) {
        if (run.finishing) return;
        run.revealAge += 2;
        ItemDisplay weapon = run.visuals.get(8);
        require(weapon != null && weapon.isValid(), "Visual hasil terputus.");
        if (run.revealAge >= run.altar.aftermath().quietTicks()) {
            if (!run.revealed) {
                weapon.setItemStack(decode(run.altar.result())); run.revealed = true;
                Cue cue = run.altar.cue(Stage.FINISH);
                if (player != null && player.isOnline() && !player.isDead()) player.showTitle(Title.title(
                        Component.text(cue.title(), NamedTextColor.GOLD), Component.text(cue.subtitle(), NamedTextColor.GRAY),
                        Title.Times.times(Duration.ofMillis(250), Duration.ofMillis((run.altar.aftermath().ticks() - run.altar.aftermath().quietTicks()) * 50L), Duration.ofMillis(250))));
            }
            Location at = location(run.altar.table()).add(0, 1.1 + Math.sin(run.revealAge * .06) * .12, 0);
            move(weapon, at, run.revealAge * .035, 0, run.altar.scale());
            if (run.revealAge % 6 == 0) at.getWorld().spawnParticle(particle(run.altar.preset()), at, 3, .2, .2, .2, .01);
        }
        if (run.revealAge < run.altar.aftermath().ticks()) return;
        run.finishing = true;
        if (run.preview) { removeRun(run); if (player != null) say(player, "Preview selesai."); return; }
        if (player == null || !player.isOnline() || player.isDead()) { abort(run, player, null, "Hasil menunggu pemulihan admin setelah pemain kembali."); return; }
        require(!rules.blocked(decode(run.ticket.result())), "Senjata hasil diblokir aturan item.");
        removeRun(run);
        try {
            deliver(player, run.ticket, location(run.altar.table()));
            checkpoint(run, Phase.DELIVERED, () -> say(player, "Evolusi selesai. Senjata berada di atas altar."), () -> pendingMessage(player));
        } catch (RuntimeException error) { pendingMessage(player); failure(error); }
    }
    private void explosionParticles(Run run) {
        Location center = location(run.altar.table()).add(0, 2.4, 0);
        center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
        center.getWorld().spawnParticle(particle(run.altar.preset()), center, run.altar.density() * 2, .7, .7, .7, .15);
        if (run.altar.effects().shockwave()) for (int i = 0; i < run.altar.density() * 2; i++) {
            double angle = Math.PI * 2 * i / (run.altar.density() * 2);
            center.getWorld().spawnParticle(Particle.GUST, altarCenter(run.altar.table()).add(Math.cos(angle) * 2.5, .15, Math.sin(angle) * 2.5), 1, 0, 0, 0, 0);
        }
    }
    private void completeVisual(Run run, Player player) {
        removeRun(run);

        Location center = location(run.altar.table()).add(0, 2.4, 0);
        center.getWorld().spawnParticle(
                Particle.EXPLOSION_EMITTER, center, 1);
        center.getWorld().spawnParticle(
                particle(run.altar.preset()), center,
                run.altar.density() * 2, .7, .7, .7, .15);

        if (run.altar.effects().shockwave()) {
            for (int i = 0; i < run.altar.density() * 2; i++) {
                double angle = Math.PI * 2 * i / (run.altar.density() * 2);
                center.getWorld().spawnParticle(Particle.GUST,
                        altarCenter(run.altar.table()).add(Math.cos(angle) * 2.5, .15, Math.sin(angle) * 2.5), 1, 0, 0, 0, 0);
            }
        }
        cue(run, Stage.FINISH);
        run.audioUntil = clock + 2400;
        finalAudio.put(run.owner, run);

        if (run.preview) say(player, "Preview selesai.");
    }

    private void abort(
            Run run, Player player, PlayerDeathEvent death, String reason
    ) {
        if (!live(run)) return;
        removeRun(run);

        if (player != null) {
            say(player, reason == null ? "Ritual dibatalkan." : reason);
        }
        if (run.preview) return;
        if (run.exploded) {
            if (player != null) pendingMessage(player);
            return;
        }
        if (run.uncertain) {
            if (player != null) pendingMessage(player);
            return;
        }

        boolean returned = false;
        try {
            if (run.consumed) {
                require(player != null, "Pemain offline.");
                refund(player, run.ticket, death);
                returned = true;
            }

            Phase terminal = returned ? Phase.RETURNED : Phase.DISCARDED;

            submit(state -> {
                Ticket old = state.tickets().get(run.owner);
                return old == null || !old.id().equals(run.id)
                        || old.phase().terminal()
                        ? state : state.phase(run.owner, run.id, terminal);
            }, () -> {
                if (player != null) {
                    say(player, "Pembatalan tersimpan; "
                            + (run.consumed
                            ? "input dikembalikan." : "item belum dikonsumsi."));
                }
            }, () -> {
                if (player != null) pendingMessage(player);
            });
        } catch (RuntimeException exception) {
            if (player != null) pendingMessage(player);
            failure(exception);
        }
    }

    private void refund(
            Player player, Ticket ticket, PlayerDeathEvent death
    ) {
        require(!hasReceipt(player, deliveryKey, ticket.id()), "Hasil sudah diberikan; refund ditolak.");
        if (hasReceipt(player, refundKey, ticket.id())) return;
        List<ItemStack> inputs = ticket.inputs().stream()
                .map(EvolutionService::decode).toList();

        if (death != null && !death.getKeepInventory()) {
            inputs.forEach(item -> death.getDrops().add(item.clone()));
            player.getPersistentDataContainer().set(refundKey, PersistentDataType.STRING, ticket.id().toString()); return;
        }

        require(player.isOnline(),
                "Pemain harus online untuk menerima refund.");

        for (ItemStack item : inputs) {
            player.getInventory().addItem(item.clone()).values()
                    .forEach(left -> drop(
                            player.getLocation(), left,
                            ticket.owner(), ticket.id()));
        }

        player.getPersistentDataContainer().set(refundKey, PersistentDataType.STRING, ticket.id().toString());
        if (death == null) player.saveData();
    }

    public void recover(UUID owner, String action, CommandSender sender) {
        editable();
        if (current.deposits().containsKey(owner)) { recoverDeposit(owner, action, sender); return; }
        require(!closed && !reloading && !runs.containsKey(owner)
                        && !menus.containsKey(owner) && recovering.add(owner),
                "Pemain sedang diproses.");

        Ticket ticket = current.tickets().get(owner);
        if (ticket == null || ticket.phase().terminal()) {
            recovering.remove(owner);
            throw new IllegalArgumentException("Tidak ada ritual tertunda.");
        }

        if (action.equals("discard")) {
            submit(
                    state -> state.phase(owner, ticket.id(), Phase.DISCARDED),
                    () -> {
                        recovering.remove(owner);
                        say(sender, "Ticket ditutup tanpa memberi item.");
                    },
                    () -> {
                        recovering.remove(owner);
                        saveFailed(sender);
                    }
            );
            return;
        }

        Player player = Bukkit.getPlayer(owner);
        if (player == null || player.isDead()
                || !List.of("refund", "result").contains(action)) {
            recovering.remove(owner);
            throw new IllegalArgumentException(
                    "Pilihan refund/result memerlukan pemain online dan hidup.");
        }

        if (ticket.phase() == Phase.REVEALING && action.equals("refund")) {
            recovering.remove(owner); throw new IllegalArgumentException("Ledakan sudah dipastikan. Pilih result atau discard setelah memeriksa item.");
        }
        Phase preparing = action.equals("refund") ? Phase.REFUNDING
                : ticket.phase() == Phase.REVEALING ? Phase.REVEALING : Phase.COMMITTING;

        submit(state -> state.phase(owner, ticket.id(), preparing), () -> {
            try {
                require(player.isOnline() && !player.isDead(),
                        "Pemain tidak tersedia; periksa ticket lagi.");

                if (action.equals("refund")) {
                    refund(player, ticket, null);
                } else {
                    require(!rules.blocked(decode(ticket.result())),
                            "Hasil diblokir aturan item.");
                    deliver(player, ticket, player.getLocation());
                }

                submit(
                        state -> state.phase(
                                owner, ticket.id(),
                                action.equals("refund")
                                        ? Phase.RETURNED : Phase.DELIVERED),
                        () -> {
                            recovering.remove(owner);
                            say(sender, "Pemulihan selesai.");
                            say(player, "Admin memulihkan item ritualmu.");
                        },
                        () -> {
                            recovering.remove(owner);
                            saveFailed(sender);
                        }
                );
            } catch (RuntimeException exception) {
                recovering.remove(owner);
                say(sender, exception.getMessage());
            }
        }, () -> {
            recovering.remove(owner);
            saveFailed(sender);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void blastDamage(EntityDamageEvent event) { blast.adjust(event); }
    private static boolean hasReceipt(Player player, NamespacedKey key, UUID id) {
        return id.toString().equals(player.getPersistentDataContainer().get(key, PersistentDataType.STRING));
    }
    private void deliver(Player player, Ticket ticket, Location position) {
        require(!hasReceipt(player, refundKey, ticket.id()), "Input sudah direfund; hasil ditolak.");
        if (hasReceipt(player, deliveryKey, ticket.id())) return;
        boolean groundExists = Bukkit.getWorlds().stream().flatMap(w -> w.getEntitiesByClass(Item.class).stream())
                .anyMatch(item -> ticket.id().toString().equals(item.getPersistentDataContainer().get(receiptKey, PersistentDataType.STRING))
                        && item.getPersistentDataContainer().has(deliveryKey, PersistentDataType.BYTE)
                        && ticket.owner().equals(item.getOwner()));
        if (!groundExists) drop(position, decode(ticket.result()), ticket.owner(), ticket.id())
                .getPersistentDataContainer().set(deliveryKey, PersistentDataType.BYTE, (byte) 1);
        player.getPersistentDataContainer().set(deliveryKey, PersistentDataType.STRING, ticket.id().toString()); player.saveData();
    }
    private void recoverDeposit(UUID owner, String action, CommandSender sender) {
        require(!menus.containsKey(owner) && !runs.containsKey(owner) && recovering.add(owner), "Pemain sedang diproses.");
        Deposit d = current.deposits().get(owner);
        if (action.equals("discard")) {
            submit(state -> state.removeDeposit(owner, d.id()), () -> {
                recovering.remove(owner); releaseDeposit(owner, d.altar()); say(sender, "Setoran ditutup tanpa memberi item.");
            }, () -> { recovering.remove(owner); saveFailed(sender); }); return;
        }
        Player player = Bukkit.getPlayer(owner);
        if (!action.equals("refund") || player == null || player.isDead()) {
            recovering.remove(owner); throw new IllegalArgumentException("Setoran belum berevolusi: pilih refund dengan pemain online; atau discard.");
        }
        if (d.transfer() != null) {
            boolean consumed = hasReceipt(player, transferKey, d.transfer().id());
            submit(state -> state.settle(owner, d.id(), d.transfer().id(), consumed), () -> {
                recovering.remove(owner);
                if (current.deposits().containsKey(owner)) recoverDeposit(owner, action, sender);
                else { releaseDeposit(owner, d.altar()); say(sender, "Transaksi yang belum dipotong dibatalkan."); }
            }, () -> { recovering.remove(owner); saveFailed(sender); }); return;
        }
        try {
            require(player.isOnline(), "Pemain offline.");
            if (!hasReceipt(player, refundKey, d.id())) {
                give(player, d.items().values().stream().map(EvolutionService::decode).toList(), d.id());
                player.getPersistentDataContainer().set(refundKey, PersistentDataType.STRING, d.id().toString()); player.saveData();
            }
            submit(state -> state.removeDeposit(owner, d.id()), () -> {
                recovering.remove(owner); releaseDeposit(owner, d.altar()); say(sender, "Setoran dikembalikan; altar tersedia lagi.");
            }, () -> { recovering.remove(owner); saveFailed(sender); });
        } catch (RuntimeException exception) { recovering.remove(owner); pendingMessage(player); failure(exception); }
    }

    private void pendingMessage(Player player) {
        say(player,
                "Item ritual perlu diperiksa admin: /evo pending. Jangan mengulangi payout tanpa pemeriksaan.");
    }

    private void cue(Run run, Stage stage) {
        stopAudio(run);
        Cue cue = run.altar.cue(stage);
        Player owner = Bukkit.getPlayer(run.owner);

        if (owner != null
                && (!cue.title().isEmpty() || !cue.subtitle().isEmpty())) {
            int duration = EvolutionAnimation.remaining(
                    stage, run.altar.ticks(), run.elapsed);
            owner.showTitle(Title.title(
                    Component.text(cue.title(), NamedTextColor.GOLD),
                    Component.text(cue.subtitle(), NamedTextColor.GRAY),
                    Title.Times.times(
                            Duration.ofMillis(250),
                            Duration.ofMillis((duration - 10L) * 50),
                            Duration.ofMillis(250))
            ));
        }

        cueSound(run, cue);
    }
    private void cueSound(Run run, Cue cue) {
        if (cue.sound().isEmpty() || cue.volume() == 0) return;
        Player owner = Bukkit.getPlayer(run.owner);
        Location center = location(run.altar.table());
        List<Player> audience = cue.privateSound()
                ? owner == null ? List.of() : List.of(owner)
                : center.getWorld().getPlayers().stream()
                .filter(p -> near(p, run.altar.table(), run.altar.soundRadius()))
                .toList();

        for (Player listener : audience) {
            listener.playSound(
                    listener.getLocation(), cue.sound(), SoundCategory.VOICE,
                    cue.volume(), cue.pitch());

            run.audio.put(listener.getUniqueId(), cue.sound());
            soundOwners.computeIfAbsent(
                            listener.getUniqueId(), id -> new HashMap<>())
                    .put(cue.sound(), run.id);
        }
    }

    private void stopAudio(Run run) {
        run.audio.forEach((id, key) -> {
            Map<String, UUID> ownership = soundOwners.get(id);

            if (ownership != null && ownership.remove(key, run.id)) {
                Player player = Bukkit.getPlayer(id);
                if (player != null) {
                    player.stopSound(key, SoundCategory.VOICE);
                }
                if (ownership.isEmpty()) soundOwners.remove(id);
            }
        });
        run.audio.clear();
    }

    private void stopFinal(UUID owner) {
        Run old = finalAudio.remove(owner);
        if (old != null) stopAudio(old);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void quit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Menu menu = menus.get(player.getUniqueId());
        if (menu != null) removeMenu(menu);
        Map<Integer, ItemDisplay> dormant = depositVisuals.remove(player.getUniqueId());
        if (dormant != null) removeVisuals(dormant); music.quit(player.getUniqueId());

        Run run = runs.get(player.getUniqueId());
        if (run != null) {
            abort(run, player, null,
                    "Ritual dibatalkan karena disconnect.");
        }

        stopFinal(player.getUniqueId());
        soundOwners.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void death(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Menu menu = menus.get(player.getUniqueId());
        if (menu != null) removeMenu(menu);

        Run run = runs.get(player.getUniqueId());
        if (run != null) {
            abort(run, player, event, "Ritual dibatalkan karena mati.");
        }

        stopFinal(player.getUniqueId());
    }

    @EventHandler
    public void join(PlayerJoinEvent event) {
        Ticket ticket = current.tickets().get(event.getPlayer().getUniqueId());
        if (ticket != null && !ticket.phase().terminal()) pendingMessage(event.getPlayer());
        Deposit d = current.deposits().get(event.getPlayer().getUniqueId());
        if (d != null) say(event.getPlayer(), "Setoran evo di altar " + d.altar() + " tersimpan. Buka altar untuk melanjutkan, atau /evo cancel.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void breakBlock(BlockBreakEvent event) {
        if (protectedBlock(event.getBlock())) {
            event.setCancelled(true);
            say(event.getPlayer(), "Block ritual sedang digunakan.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void explode(EntityExplodeEvent event) {
        event.blockList().removeIf(this::protectedBlock);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void explode(BlockExplodeEvent event) {
        event.blockList().removeIf(this::protectedBlock);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void piston(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(block ->
                protectedBlock(block)
                        || protectedBlock(block.getRelative(event.getDirection())))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void piston(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(block ->
                protectedBlock(block)
                        || protectedBlock(block.getRelative(event.getDirection())))) {
            event.setCancelled(true);
        }
    }

    private boolean protectedBlock(Block block) {
        Point point = point(block);
        return leases.keySet().stream()
                .map(current.altars()::get)
                .filter(Objects::nonNull)
                .anyMatch(altar -> altar.table().equals(point)
                        || altar.ingredients().stream()
                        .anyMatch(ingredient -> Objects.equals(ingredient.point(), point)));
    }

    @EventHandler
    public void chunk(ChunkLoadEvent event) {
        Arrays.stream(event.getChunk().getEntities()).forEach(this::removeStale);
    }

    private void removeStale(Entity entity) {
        if (!(entity instanceof ItemDisplay)
                || !entity.getPersistentDataContainer().has(
                visualKey, PersistentDataType.BYTE)) return;

        boolean owned = runs.values().stream().anyMatch(run -> run.visuals.containsValue(entity))
                || depositVisuals.values().stream().anyMatch(visuals -> visuals.containsValue(entity));

        if (!owned) entity.remove();
    }

    private ItemDisplay display(Point point, ItemStack item, double scale) {
        return location(point).getWorld().spawn(
                location(point), ItemDisplay.class, entity -> {
                    entity.setPersistent(false);
                    entity.setInvulnerable(true);
                    entity.setGravity(false);
                    entity.setItemStack(item.asOne());
                    entity.setItemDisplayTransform(
                            ItemDisplay.ItemDisplayTransform.FIXED);
                    entity.setTeleportDuration(2);
                    entity.setInterpolationDuration(2);
                    entity.getPersistentDataContainer().set(
                            visualKey, PersistentDataType.BYTE, (byte) 1);
                    entity.setTransformation(transform(0, 0, scale));
                });
    }

    private static void move(
            ItemDisplay entity, Location position,
            double rotation, double tilt, double scale
    ) {
        require(entity != null && entity.isValid() && entity.teleport(position),
                "Visual gagal dipindahkan.");
        entity.setTransformation(transform(rotation, tilt, scale));
    }

    private static Transformation transform(
            double rotation, double tilt, double scale
    ) {
        return new Transformation(
                new Vector3f(),
                new Quaternionf().rotationY((float) rotation),
                new Vector3f((float) scale),
                new Quaternionf().rotationZ((float) tilt)
        );
    }

    private Item drop(
            Location position, ItemStack stack, UUID owner, UUID receipt
    ) {
        Item item = position.getWorld().dropItem(position, stack.clone());
        item.setOwner(owner);
        item.setPickupDelay(20);
        item.setUnlimitedLifetime(true);
        item.setInvulnerable(true);
        item.setVelocity(new Vector(0, .08, 0));
        item.getPersistentDataContainer().set(
                receiptKey, PersistentDataType.STRING, receipt.toString());
        return item;
    }

    private boolean matches(
            ItemStack actual, String encoded, boolean ignoreDamage
    ) {
        if (!usable(actual)) return false;

        ItemStack first = rules.stacks().prepared(actual.asOne());
        ItemStack second = rules.stacks().prepared(decode(encoded).asOne());

        if (ignoreDamage) {
            first.editMeta(meta -> {
                if (meta instanceof Damageable damage) damage.setDamage(0);
            });
            second.editMeta(meta -> {
                if (meta instanceof Damageable damage) damage.setDamage(0);
            });
        }
        return first.isSimilar(second);
    }

    static String encode(ItemStack item) {
        String result = Base64.getEncoder()
                .encodeToString(item.serializeAsBytes());
        encoded(result, false);
        return result;
    }

    static ItemStack decode(String encoded) {
        ItemStack item = ItemStack.deserializeBytes(
                Base64.getDecoder().decode(encoded));
        require(usable(item), "Data item kosong/tidak valid.");
        return item;
    }

    private static void template(String encoded) {
        require(decode(encoded).getAmount() == 1,
                "Contoh item harus berjumlah satu.");
    }

    private static boolean usable(ItemStack item) {
        return item != null && !item.getType().isAir() && item.getAmount() > 0;
    }

    private static boolean survival(Player player) {
        return player.getGameMode() == GameMode.SURVIVAL
                || player.getGameMode() == GameMode.ADVENTURE;
    }

    public static Point point(Block block) {
        return new Point(
                block.getWorld().getUID(),
                block.getX(), block.getY(), block.getZ());
    }

    private static Location location(Point point) {
        World world = Bukkit.getWorld(point.world());
        require(world != null, "World altar belum tersedia.");
        return new Location(
                world, point.x() + .5, point.y() + 1.2, point.z() + .5);
    }

    private static Location altarCenter(Point point) { return location(point).subtract(0, .7, 0); }

    private static Block block(Point point) {
        return location(point).getWorld()
                .getBlockAt(point.x(), point.y(), point.z());
    }

    private static boolean near(
            Player player, Point point, double radius
    ) {
        return player.getWorld().getUID().equals(point.world())
                && player.getLocation().distanceSquared(location(point))
                <= radius * radius;
    }

    private static Location offset(
            Location base, EvolutionAnimation.Vec vector
    ) {
        return base.clone().add(vector.x(), vector.y(), vector.z());
    }

    private static Particle particle(Preset preset) {
        return switch (preset) {
            case INFERNO, PHOENIX -> Particle.FLAME;
            case STORMFORGE -> Particle.ELECTRIC_SPARK;
            case VOID -> Particle.PORTAL;
            case ASTRAL -> Particle.END_ROD;
            case FROST -> Particle.SNOWFLAKE;
        };
    }

    private static ItemStack icon(
            Material material, String name, boolean green
    ) {
        var item = new ItemStack(material);
        item.editMeta(meta -> meta.displayName(Component.text(
                name, green ? NamedTextColor.GREEN : NamedTextColor.GRAY)));
        return item;
    }

    private boolean live(Run run) {
        return runs.get(run.owner) == run;
    }

    private static void removeVisual(
            Map<Integer, ItemDisplay> visuals, int key
    ) {
        ItemDisplay old = visuals.remove(key);
        if (old != null) old.remove();
    }

    private static void removeVisuals(Map<Integer, ItemDisplay> visuals) {
        visuals.values().forEach(Entity::remove);
        visuals.clear();
    }

    private void removeMenu(Menu menu) {
        // Preview dikosongkan sebelum native close mengembalikan matrix sebagai item nyata.
        menu.rendering = true; menu.inventory.clear(); menus.remove(menu.owner, menu);
        if (!current.deposits().containsKey(menu.owner) && !menu.pending && !runs.containsKey(menu.owner))
            leases.remove(menu.altar.id(), menu.owner);
    }

    private void removeRun(Run run) {
        if (runs.remove(run.owner, run)) {
            leases.remove(run.altar.id(), run.owner);
        }
        removeVisuals(run.visuals);
        stopAudio(run); music.stop(run.id);

        Player player = Bukkit.getPlayer(run.owner);
        if (player != null) player.clearTitle();
    }

    @Override
    public void close() {
        if (closed) return;
        layout.close();
        if (task != null) task.cancel();

        for (Menu menu : List.copyOf(menus.values())) {
            Player player = Bukkit.getPlayer(menu.owner);
            if (player != null
                    && player.getOpenInventory().getTopInventory()
                    == menu.inventory) {
                player.closeInventory();
            }
            removeMenu(menu);
        }

        for (Run run : List.copyOf(runs.values())) {
            abort(run, Bukkit.getPlayer(run.owner), null,
                    "Ritual dihentikan karena plugin ditutup.");
        }

        List.copyOf(finalAudio.keySet()).forEach(this::stopFinal);
        clearDepositVisuals(); music.close();
        closed = true;
        HandlerList.unregisterAll(this);
        writer.shutdown();

        try {
            if (!writer.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning(
                        "Writer evolution belum selesai setelah 10 detik.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class Menu {
        final UUID owner;
        final EvolutionAltar altar;
        final CraftingInventory inventory;
        boolean pending, rendering;
        Menu(UUID owner, EvolutionAltar altar, CraftingInventory inventory) {
            this.owner = owner; this.altar = altar; this.inventory = inventory;
        }
    }

    private static final class Run {
        final UUID owner, id;
        final EvolutionAltar altar;
        final boolean preview;
        final Ticket ticket;
        final Map<Integer, ItemDisplay> visuals;
        final Map<UUID, String> audio = new HashMap<>();
        final List<EvolutionAnimation.Vec> origins;

        boolean consumed, active, paused, finishing, uncertain, musicStarted, commitRequested, commitReady, exploded, revealed;
        int elapsed, revealAge;
        Stage stage;
        long audioUntil;

        Run(
                UUID owner,
                EvolutionAltar altar,
                boolean preview,
                Map<Integer, ItemDisplay> visuals,
                Ticket ticket
        ) {
            this.owner = owner;
            this.altar = altar;
            this.preview = preview;
            this.visuals = visuals;
            this.ticket = ticket;
            id = preview ? UUID.randomUUID() : ticket.id();

            origins = altar.ingredients().stream()
                    .map(ingredient -> new EvolutionAnimation.Vec(
                            ingredient.point().x() - altar.table().x(),
                            ingredient.point().y() - altar.table().y(),
                            ingredient.point().z() - altar.table().z()))
                    .toList();
        }
    }
}
'@
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionStore.java'
        OriginalHash = '53598e287e3a5fc445e0f580943f65440d16bbe647063b30286a790c950f417b'
        Hash = '7c54a9774c5f10b1b64699d5395a8edab66304aeff8994cd3e48c1cf7addc0ba'
        Source = @'
package com.bangzachery.vitae.vitaemanager.evolution;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionAltar.*;

/**
 * Tidak mengakses player, world, registry, atau ItemStack pada worker.
 */
public final class EvolutionStore {
    private final Path file;
    private final Set<String> vanillaSounds;

    public EvolutionStore(Path file, Set<String> vanillaSounds) {
        this.file = file;
        this.vanillaSounds = Set.copyOf(vanillaSounds);
    }

    public enum Phase {
        PREPARED, ACTIVE, REFUNDING, COMMITTING, REVEALING,
        RETURNED, DELIVERED, DISCARDED;

        public boolean terminal() {
            return this == RETURNED || this == DELIVERED || this == DISCARDED;
        }
    }

    public record Ticket(
            UUID id,
            UUID owner,
            String altar,
            Point origin,
            Phase phase,
            List<String> inputs,
            String result
    ) {
        public Ticket {
            Objects.requireNonNull(id);
            Objects.requireNonNull(owner);
            Objects.requireNonNull(origin);
            Objects.requireNonNull(phase);
            require(altar != null && altar.matches("[a-z0-9_-]{1,32}"),
                    "ID recovery tidak valid.");

            inputs = List.copyOf(inputs);
            require(!inputs.isEmpty() && inputs.size() <= 36,
                    "Input recovery tidak valid.");
            inputs.forEach(input -> encoded(input, false));
            encoded(result, false);
        }

        public Ticket phase(Phase next) {
            return new Ticket(id, owner, altar, origin, next, inputs, result);
        }
    }

    public record Deposit(UUID id, UUID owner, String altar, Point origin,
                          Map<Integer, String> items, Transfer transfer) {
        public Deposit {
            Objects.requireNonNull(id); Objects.requireNonNull(owner); Objects.requireNonNull(origin);
            require(altar != null && altar.matches("[a-z0-9_-]{1,32}"), "ID setoran tidak valid.");
            items = checkedSlots(items);
        }
        public Deposit pending(Transfer next) {
            require(transfer == null, "Setoran sebelumnya masih diproses.");
            return new Deposit(id, owner, altar, origin, items, next);
        }
        public Deposit settle(boolean consumed) {
            require(transfer != null, "Tidak ada transaksi setoran.");
            return new Deposit(id, owner, altar, origin, consumed ? transfer.next() : items, null);
        }
    }
    /** Journal ditulis sebelum cursor dipotong; receipt disimpan di data pemain. */
    public record Transfer(UUID id, String cursor, Map<Integer, String> next) {
        public Transfer {
            Objects.requireNonNull(id); encoded(cursor, false); next = checkedSlots(next);
            require(!next.isEmpty(), "Setoran kosong.");
        }
    }
    private static Map<Integer, String> checkedSlots(Map<Integer, String> source) {
        var copy = Map.copyOf(source); require(copy.size() <= 9, "Maksimal 9 slot setoran.");
        copy.forEach((slot, item) -> {
            require(slot >= 0 && slot <= 8, "Slot setoran tidak valid."); encoded(item, false);
        });
        return copy;
    }

    public record State(
            Map<String, EvolutionAltar> altars,
            Map<UUID, Ticket> tickets, Map<UUID, Deposit> deposits
    ) {
        public State {
            altars = Map.copyOf(altars);
            tickets = Map.copyOf(tickets);
            deposits = Map.copyOf(deposits);
            require(altars.size() <= 128, "Maksimal 128 altar.");

            Set<Point> points = new HashSet<>();
            altars.forEach((id, a) -> {
                require(id.equals(a.id()), "ID altar tidak sesuai.");
                require(points.add(a.table()),
                        "Crafting table sudah digunakan altar lain.");
            });
            tickets.forEach((owner, t) ->
                    require(owner.equals(t.owner()),
                            "Pemilik recovery tidak sesuai."));
            var definitions = altars; var receipts = tickets;
            Set<String> reserved = new HashSet<>();
            require(deposits.size() <= 128, "Maksimal 128 setoran.");
            deposits.forEach((owner, d) -> {
                require(owner.equals(d.owner()), "Pemilik setoran tidak sesuai.");
                EvolutionAltar a = definitions.get(d.altar());
                require(a != null && a.table().equals(d.origin()), "Altar setoran hilang atau dipindahkan.");
                require(a.ingredients().stream().allMatch(i -> i.point() != null), "Setoran memiliki pedestal yang belum dipetakan.");
                require(reserved.add(d.altar()), "Altar memiliki dua pemilik setoran.");
                Ticket t = receipts.get(owner);
                require(t == null || t.phase().terminal(), "Pemilik punya ritual dan setoran bersamaan.");
            });
        }

        public State(Map<String, EvolutionAltar> altars, Map<UUID, Ticket> tickets) {
            this(altars, tickets, Map.of());
        }
        public State deposit(Deposit next) {
            Deposit old = deposits.get(next.owner());
            require(old == null || old.id().equals(next.id()), "Setoran telah berubah.");
            var copy = new HashMap<>(deposits); copy.put(next.owner(), next);
            return new State(altars, tickets, copy);
        }
        public State removeDeposit(UUID owner, UUID id) {
            Deposit old = deposits.get(owner);
            require(old != null && old.id().equals(id), "Setoran telah berubah.");
            var copy = new HashMap<>(deposits); copy.remove(owner);
            return new State(altars, tickets, copy);
        }
        public State settle(UUID owner, UUID id, UUID receipt, boolean consumed) {
            Deposit old = deposits.get(owner);
            require(old != null && old.id().equals(id) && old.transfer() != null
                    && old.transfer().id().equals(receipt), "Transaksi setoran telah berubah.");
            Deposit next = old.settle(consumed);
            return next.items().isEmpty() ? removeDeposit(owner, id) : deposit(next);
        }
        public State beginDeposit(Ticket ticket, UUID depositId) {
            Deposit old = deposits.get(ticket.owner());
            require(old != null && old.id().equals(depositId) && old.transfer() == null, "Setoran belum siap.");
            require(ticket.inputs().equals(new TreeMap<>(old.items()).values().stream().toList()),
                    "Input ritual tidak sesuai setoran.");
            return removeDeposit(old.owner(), old.id()).prepare(ticket);
        }

        public State put(EvolutionAltar altar) {
            var copy = new HashMap<>(altars);
            copy.put(altar.id(), altar);
            return new State(copy, tickets, deposits);
        }

        public State remove(String id) {
            var copy = new HashMap<>(altars);
            require(copy.remove(id) != null, "Altar tidak ditemukan.");
            return new State(copy, tickets, deposits);
        }

        public State prepare(Ticket ticket) {
            Ticket previous = tickets.get(ticket.owner());
            require(previous == null || previous.phase().terminal(),
                    "Ritual sebelumnya belum diselesaikan.");

            var copy = new HashMap<>(tickets);
            copy.put(ticket.owner(), ticket);
            return new State(altars, copy, deposits);
        }

        public State phase(UUID owner, UUID id, Phase phase) {
            Ticket old = tickets.get(owner);
            require(old != null && old.id().equals(id),
                    "Ticket ritual telah berubah.");
            require(!old.phase().terminal(),
                    "Ticket ritual sudah ditutup.");
            require(phase != Phase.PREPARED,
                    "Tidak boleh kembali ke PREPARED.");
            require(phase != Phase.ACTIVE || old.phase() == Phase.PREPARED,
                    "Transisi ACTIVE tidak valid.");
            require(phase != Phase.REVEALING || old.phase() == Phase.COMMITTING, "Adegan hasil harus melalui COMMITTING.");
            require(old.phase() != Phase.REVEALING || phase == Phase.REVEALING || phase == Phase.DELIVERED || phase == Phase.DISCARDED,
                    "Ledakan sudah dipastikan; hanya hasil atau discard, bukan refund.");
            require(phase != Phase.DELIVERED || old.phase() == Phase.COMMITTING || old.phase() == Phase.REVEALING,
                    "Hasil harus melalui COMMITTING.");

            var copy = new HashMap<>(tickets);
            copy.put(owner, old.phase(phase));
            return new State(altars, copy, deposits);
        }
    }

    public State initialize()
            throws IOException, InvalidConfigurationException {
        if (!Files.exists(file)) {
            State empty = new State(Map.of(), Map.of());
            save(empty);
            return empty;
        }
        State loaded = load();
        YamlConfiguration old = new YamlConfiguration(); old.load(file.toFile());
        boolean upgrade = loaded.altars().keySet().stream().anyMatch(id -> !old.contains("altars." + id + ".aftermath"));
        if (upgrade) {
            Files.copy(file, file.resolveSibling("evolution-before-story-update-" + UUID.randomUUID() + ".yml.bak"));
            save(loaded);
        }
        return loaded;
    }

    public State load() throws IOException, InvalidConfigurationException {
        if (!Files.isRegularFile(file)) {
            throw new IOException(
                    "File evolution hilang/tidak valid; reload tidak membuat data kosong.");
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(file.toFile());
        return parse(yaml);
    }

    public void validateAudio(Cue cue) {
        require(!cue.sound().startsWith("minecraft:")
                        || vanillaSounds.contains(cue.sound()),
                "Sound vanilla tidak tersedia pada versi server.");
    }

    public State parse(YamlConfiguration yaml) {
        int version = integer(yaml, "state-version");
        require(version == 1 || version == 2,
                "Versi evolution tidak didukung.");

        Map<String, EvolutionAltar> altars = new HashMap<>();
        ConfigurationSection root = section(yaml, "altars");

        for (String id : root.getKeys(false)) {
            ConfigurationSection a = section(root, id);
            ConfigurationSection ingredients = section(a, "ingredients");
            ConfigurationSection lines = section(a, "cues");

            List<Ingredient> materials = new ArrayList<>();
            for (int i = 1; i <= ingredients.getKeys(false).size(); i++) {
                ConfigurationSection entry =
                        section(ingredients, Integer.toString(i));
                int slot = entry.contains("slot") ? integer(entry, "slot") : EvolutionGrid.rawSlot(i - 1);
                require(EvolutionGrid.ingredientSlot(slot), "Slot bahan tersimpan tidak valid.");
                materials.add(new Ingredient(
                        text(entry, "item"),
                        integer(entry, "count"),
                        text(entry, "point").isEmpty() ? null : Point.parse(text(entry, "point")),
                        slot
                ));
            }

            Set<UUID> targets = new HashSet<>();
            for (String target : strings(a, "targets")) {
                require(targets.add(UUID.fromString(target)),
                        "Target duplikat.");
            }

            Map<Stage, Cue> cues = new HashMap<>();
            for (String key : lines.getKeys(false)) {
                ConfigurationSection line = section(lines, key);
                Stage stage = Stage.parse(key);
                require(!cues.containsKey(stage), "Tahap duplikat.");

                Cue cue = new Cue(
                        text(line, "title"),
                        text(line, "subtitle"),
                        text(line, "sound"),
                        bool(line, "private"),
                        (float) number(line, "volume"),
                        (float) number(line, "pitch")
                );
                validateAudio(cue);
                cues.put(stage, cue);
            }

            double duration = number(a, "duration");
            range(duration, 5, 120, "Durasi");
            int ticks = (int) Math.round(duration * 20);
            Music music = Music.off(ticks); Effects effects = Effects.off();
            if (version == 2 || a.contains("music")) {
                ConfigurationSection m = section(a, "music");
                music = new Music(text(m, "sound"), bool(m, "private"), (float) number(m, "volume"), ticks);
                validateAudio(new Cue("", "", music.sound(), music.privateSound(), music.volume(), 1));
            }
            if (version == 2 || a.contains("effects")) {
                ConfigurationSection e = section(a, "effects");
                effects = new Effects(bool(e, "lightning"), integer(e, "interval-ticks"),
                        number(e, "radius"), bool(e, "shockwave"));
            }


            Aftermath aftermath = Aftermath.defaults();
            if (a.contains("aftermath")) {
                ConfigurationSection ending = section(a, "aftermath");
                double seconds = number(ending, "duration"); range(seconds, 2, 15, "Adegan penutup");
                aftermath = new Aftermath(bool(ending, "enabled"), (int) Math.round(seconds * 20));
            }
            EvolutionAltar altar = new EvolutionAltar(
                    id,
                    Point.parse(text(a, "table")),
                    text(a, "source"),
                    text(a, "result"),
                    materials,
                    targets,
                    Preset.parse(text(a, "preset")),
                    (int) Math.round(duration * 20),
                    number(a, "scale"),
                    integer(a, "density"),
                    number(a, "sound-radius"),
                    number(a, "damage-radius"),
                    number(a, "damage"),
                    bool(a, "enabled"),
                    cues, music, effects, (version == 2 || a.contains("lethal")) && bool(a, "lethal"), aftermath
            );

            for (Stage stage : Stage.values()) validateAudio(altar.cue(stage));
            altars.put(id, altar);
        }

        Map<UUID, Ticket> tickets = new HashMap<>();
        ConfigurationSection recovery = section(yaml, "recovery");

        for (String key : recovery.getKeys(false)) {
            UUID owner = UUID.fromString(key);
            ConfigurationSection t = section(recovery, key);

            tickets.put(owner, new Ticket(
                    UUID.fromString(text(t, "ticket")),
                    owner,
                    text(t, "altar"),
                    Point.parse(text(t, "origin")),
                    Phase.valueOf(text(t, "phase")),
                    strings(t, "inputs"),
                    text(t, "result")
            ));
        }

        Map<UUID, Deposit> deposits = new HashMap<>();
        if (version == 2 || yaml.contains("deposits")) {
            ConfigurationSection ds = section(yaml, "deposits");
            for (String key : ds.getKeys(false)) {
                UUID owner = UUID.fromString(key); ConfigurationSection d = section(ds, key);
                Transfer transfer = null;
                if (d.contains("transfer")) {
                    ConfigurationSection t = section(d, "transfer");
                    transfer = new Transfer(UUID.fromString(text(t, "id")), text(t, "cursor"), readSlots(section(t, "next")));
                }
                deposits.put(owner, new Deposit(UUID.fromString(text(d, "id")), owner, text(d, "altar"),
                        Point.parse(text(d, "origin")), readSlots(section(d, "items")), transfer));
            }
        }
        return new State(altars, tickets, deposits);
    }

    public YamlConfiguration document(State state) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("state-version", 2);
        ConfigurationSection root = yaml.createSection("altars");

        for (EvolutionAltar a : new TreeMap<>(state.altars()).values()) {
            ConfigurationSection entry = root.createSection(a.id());
            entry.set("table", a.table().encode());
            entry.set("source", a.source());
            entry.set("result", a.result());
            entry.set("preset", a.preset().name());
            entry.set("duration", a.ticks() / 20.0);
            entry.set("scale", a.scale());
            entry.set("density", a.density());
            entry.set("sound-radius", a.soundRadius());
            entry.set("damage-radius", a.damageRadius());
            entry.set("damage", a.damage());
            entry.set("lethal", a.lethal());
            entry.set("music.sound", a.music().sound());
            entry.set("music.private", a.music().privateSound());
            entry.set("music.volume", a.music().volume());
            entry.set("effects.lightning", a.effects().lightning());
            entry.set("effects.interval-ticks", a.effects().interval());
            entry.set("effects.radius", a.effects().radius());
            entry.set("effects.shockwave", a.effects().shockwave());
            entry.set("aftermath.enabled", a.aftermath().enabled());
            entry.set("aftermath.duration", a.aftermath().ticks() / 20.0);
            entry.set("enabled", a.enabled());
            entry.set("targets", a.targets().stream()
                    .map(UUID::toString).sorted().toList());

            ConfigurationSection ingredients =
                    entry.createSection("ingredients");
            for (int i = 0; i < a.ingredients().size(); i++) {
                writeIngredient(
                        ingredients.createSection(Integer.toString(i + 1)),
                        a.ingredients().get(i)
                );
            }

            ConfigurationSection cues = entry.createSection("cues");
            a.cues().forEach((stage, cue) -> writeCue(
                    cues.createSection(stage.name().toLowerCase(Locale.ROOT)),
                    cue
            ));
        }

        ConfigurationSection recovery = yaml.createSection("recovery");
        state.tickets().forEach((owner, ticket) -> {
            ConfigurationSection t = recovery.createSection(owner.toString());
            t.set("ticket", ticket.id().toString());
            t.set("altar", ticket.altar());
            t.set("origin", ticket.origin().encode());
            t.set("phase", ticket.phase().name());
            t.set("inputs", ticket.inputs());
            t.set("result", ticket.result());
        });
        ConfigurationSection deposits = yaml.createSection("deposits");
        state.deposits().forEach((owner, deposit) -> {
            ConfigurationSection d = deposits.createSection(owner.toString());
            d.set("id", deposit.id().toString()); d.set("altar", deposit.altar()); d.set("origin", deposit.origin().encode());
            writeSlots(d.createSection("items"), deposit.items());
            if (deposit.transfer() != null) {
                Transfer transfer = deposit.transfer(); ConfigurationSection t = d.createSection("transfer");
                t.set("id", transfer.id().toString()); t.set("cursor", transfer.cursor());
                writeSlots(t.createSection("next"), transfer.next());
            }
        });
        return yaml;
    }
    private static Map<Integer, String> readSlots(ConfigurationSection section) {
        Map<Integer, String> slots = new HashMap<>();
        for (String key : section.getKeys(false)) {
            int index = Integer.parseInt(key);
            require(Integer.toString(index).equals(key), "Nomor slot harus kanonik."); slots.put(index, text(section, key));
        }
        return checkedSlots(slots);
    }
    private static void writeSlots(ConfigurationSection section, Map<Integer, String> slots) {
        new TreeMap<>(slots).forEach((slot, item) -> section.set(slot.toString(), item));
    }

    public static void writeIngredient(
            ConfigurationSection section, Ingredient ingredient
    ) {
        section.set("item", ingredient.item());
        section.set("count", ingredient.count());
        section.set("point", ingredient.point() == null ? "" : ingredient.point().encode());
        section.set("slot", ingredient.slot());
    }

    public static void writeCue(ConfigurationSection section, Cue cue) {
        section.set("title", cue.title());
        section.set("subtitle", cue.subtitle());
        section.set("sound", cue.sound());
        section.set("private", cue.privateSound());
        section.set("volume", cue.volume());
        section.set("pitch", cue.pitch());
    }

    public void save(State state) throws IOException {
        YamlConfiguration yaml = document(state);
        parse(yaml);
        Files.createDirectories(file.toAbsolutePath().getParent());

        Path temp = Files.createTempFile(
                file.toAbsolutePath().getParent(), "evolution-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(
                    temp, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
            )) {
                ByteBuffer bytes = ByteBuffer.wrap(
                        yaml.saveToString().getBytes(StandardCharsets.UTF_8));
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            Files.move(temp, file,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static ConfigurationSection section(
            ConfigurationSection parent, String key
    ) {
        var found = parent.getConfigurationSection(key);
        require(found != null, "Section tidak valid: " + key);
        return found;
    }

    private static String text(ConfigurationSection section, String key) {
        Object value = section.get(key);
        require(value instanceof String, "Harus teks: " + key);
        return (String) value;
    }

    private static boolean bool(ConfigurationSection section, String key) {
        Object value = section.get(key);
        require(value instanceof Boolean, "Harus true/false: " + key);
        return (Boolean) value;
    }

    private static double number(ConfigurationSection section, String key) {
        Object value = section.get(key);
        require(value instanceof Number, "Harus angka: " + key);
        double number = ((Number) value).doubleValue();
        require(Double.isFinite(number), "Angka tidak valid: " + key);
        return number;
    }

    private static int integer(ConfigurationSection section, String key) {
        double number = number(section, key);
        require(number == Math.rint(number)
                        && number >= Integer.MIN_VALUE
                        && number <= Integer.MAX_VALUE,
                "Harus bilangan bulat: " + key);
        return (int) number;
    }

    private static List<String> strings(
            ConfigurationSection section, String key
    ) {
        Object value = section.get(key);
        require(value instanceof List<?>, "Harus daftar: " + key);
        List<String> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            require(item instanceof String, "Isi daftar harus teks: " + key);
            result.add((String) item);
        }
        return List.copyOf(result);
    }
}
'@
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcCommand.java'
        OriginalHash = 'bf1fa613a382ee098c23d854215da6c8480d78ad73036d387975fa2222e1f0e1'
        Hash = 'e0290f06a2ee5e692839e05955a727eb6e256c5301d33b74fb1aa7e4b3495fdb'
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
                tip(sender, cmd + "quest farm 5", "Baca tanaman; lima putaran panen");
                tip(sender, cmd + "queststart", "Simpan tempat awal player dalam area");
                tip(sender, cmd + "reward quest viti 1000", "Hadiah ladang wajib Viti");
                tip(sender, cmd + "page start Bantu aku memanen ladang ini.", "Tulis tawaran quest");
                tip(sender, cmd + "option start QUEST Terima quest.", "Pilihan menerima pekerjaan");
                tip(sender, cmd + "option start END Keluar...", "Pilihan menolak");
                note(sender, "Pemain wajib punya iron hoe. Pos1/pos2 adalah blok yang dilihat, maksimal enam blok.");
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
            case "quest" -> values.addAll(List.of("farm", "mob", "off"));
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
        OriginalHash = '569a4b30150cb1fa63e7dc59d02e37d5175f32b1693ee309c637a86a0d9aaaee'
        Hash = '594ef5662c976c940e2e3e78546c4254807622c2320d35d691a3bc589187fb86'
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
                if (crops.isEmpty() || crops.size() > 512 || !reward.items().isEmpty())
                    throw new IllegalArgumentException("Ladang perlu 1–512 tanaman dan hadiah berupa Viti.");
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
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcFacing.java'
        OriginalHash = ''
        Hash = 'de684821d755ed3267d9711de50662314fecd994a1877d5b22b969ff8f410fd8'
        Source = @'
package com.bangzachery.vitae.vitaemanager.npc;

/** Rotation only: never moves the NPC or changes its saved idle direction. */
public final class NpcFacing {
    private NpcFacing() { }
    public static NpcData.Point towards(NpcData.Definition npc, NpcData.Point eyes, NpcData.Point current) {
        var at = npc.location();
        if (!at.world().equals(eyes.world())) return npc.facingLocation();
        double dx = eyes.x() - at.x(), dy = eyes.y() - at.y() - npc.eyeHeight(), dz = eyes.z() - at.z();
        if (dx * dx + dy * dy + dz * dz < .000001) return current;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        if (npc.kind() == NpcData.Kind.MODEL) { yaw += npc.facing().yaw(); pitch += npc.facing().pitch(); }
        pitch = Math.max(-90, Math.min(90, pitch));
        return new NpcData.Point(at.world(), at.x(), at.y(), at.z(), current.yaw() + step(shortest(yaw - current.yaw())),
                current.pitch() + step(pitch - current.pitch()));
    }
    private static float step(float delta) { return Math.max(-15, Math.min(15, delta)); }
    private static float shortest(float angle) { return (angle % 360 + 540) % 360 - 180; }
}
'@
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcGates.java'
        OriginalHash = '1e9914c5acb9591c78d043f8c7790322ece96f20fa0bc7fe7d1b5a15f05b0335'
        Hash = '921104a726360c8689e22810558bbeac5eb6076b2238fe66b38e6b41f50fd673'
        Source = @'
package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.whisper.WhisperArea;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.world.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.IOException;
import java.util.*;
import java.util.function.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import static com.bangzachery.vitae.vitaemanager.npc.NpcData.*;

/** Global, durable gate claims; every operation runs on the server thread. */
public final class NpcGates implements Listener, AutoCloseable {
    @FunctionalInterface public interface StatusWriter { void write(String id, Gate next) throws IOException; }
    @FunctionalInterface public interface Claim { void save(Gate opening) throws IOException; }
    @FunctionalInterface public interface ResetSave { void save() throws IOException; }
    private record Cell(UUID world, int x, int y, int z) { }
    private record Pick(String id, boolean first) { }
    private record Corners(String id, Cell first, Cell second) { }
    private record Suppressed(Cell cell, long until) { }
    private static final class Run {
        final String id; final Gate gate; final List<BlockDisplay> visuals = new ArrayList<>();
        int age; boolean opened;
        Run(String id, Gate gate) { this.id = id; this.gate = gate; }
    }
    private final JavaPlugin plugin;
    private final Supplier<Map<String, Definition>> definitions;
    private final StatusWriter writer;
    private final Consumer<Exception> failure;
    private final Map<String, Run> runs = new HashMap<>();
    private final Map<UUID, Pick> picks = new HashMap<>();
    private final Map<UUID, Corners> corners = new HashMap<>();
    private final Map<UUID, Suppressed> suppressed = new HashMap<>();
    private final Set<Cell> protectedCells = new HashSet<>();
    private final Set<String> reported = new HashSet<>();
    private final Set<String> repairs = new HashSet<>();
    private long ticks;
    private boolean closed;
    NpcGates(JavaPlugin plugin, Supplier<Map<String, Definition>> definitions, StatusWriter writer, Consumer<Exception> failure) {
        this.plugin = plugin; this.definitions = definitions; this.writer = writer; this.failure = failure;
    }
    public boolean busy(String id) { return runs.containsKey(id); }
    public boolean selecting(UUID player) {
        Suppressed point = suppressed.get(player);
        return picks.containsKey(player) || point != null && point.until() >= ticks;
    }
    public void start() throws IOException {
        for (Definition definition : List.copyOf(definitions.get().values())) if (definition.gate() != null
                && definition.gate().status() == GateStatus.OPENING) writer.write(definition.id(), definition.gate().withStatus(GateStatus.CLOSED));
        refresh(); Bukkit.getPluginManager().registerEvents(this, plugin);
        for (Definition definition : List.copyOf(definitions.get().values())) if (definition.gate() != null) {
            Gate gate = definition.gate();
            if (gate.status() == GateStatus.OPEN) {
                clearLoaded(gate);
                if (gate.timing().seconds() > 0 && gate.timing().closeAt() == 0) writer.write(definition.id(), gate.opened(System.currentTimeMillis()));
            } else if (gate.status() == GateStatus.CLOSED) repairs.add(definition.id());
        }
    }
    public void refresh() {
        protectedCells.clear();
        repairs.removeIf(id -> !definitions.get().containsKey(id) || definitions.get().get(id).gate() == null);
        for (Definition definition : definitions.get().values()) if (definition.gate() != null)
            for (GateBlock block : definition.gate().blocks()) protectedCells.add(cell(definition.gate().area().world(), block));
    }
    public void arm(Player admin, String id, boolean first) {
        requireAdmin(admin); picks.put(admin.getUniqueId(), new Pick(id, first));
        NpcService.message(admin, "Pukul block sudut " + (first ? "A" : "B") + " dinding. Block tidak dihancurkan. /vnpc " + id + " gate cancel untuk batal seleksi.");
    }
    public void cancelSelection(Player admin) { picks.remove(admin.getUniqueId()); }
    public Gate capture(Player admin, String id) {
        requireAdmin(admin); Corners selection = corners.get(admin.getUniqueId());
        if (selection == null || !selection.id().equals(id) || selection.first() == null || selection.second() == null)
            throw new IllegalArgumentException("Pilih gate pos1 dan gate pos2 untuk NPC ini, lalu pukul kedua sudut dinding.");
        Cell a = selection.first(), b = selection.second();
        if (!a.world().equals(b.world())) throw new IllegalArgumentException("Sudut gerbang harus dalam dunia yang sama.");
        WhisperArea area = WhisperArea.between(a.world(), a.x(), a.y(), a.z(), b.x(), b.y(), b.z());
        long volume = (long) (area.maxX() - area.minX() + 1) * (area.maxY() - area.minY() + 1) * (area.maxZ() - area.minZ() + 1);
        if (volume > 256) throw new IllegalArgumentException("Seleksi gerbang maksimal 256 block. Pilih dindingnya saja.");
        World world = Objects.requireNonNull(Bukkit.getWorld(area.world()), "Dunia belum dimuat.");
        List<GateBlock> blocks = new ArrayList<>();
        for (int x = area.minX(); x <= area.maxX(); x++) for (int y = area.minY(); y <= area.maxY(); y++) for (int z = area.minZ(); z <= area.maxZ(); z++) {
            if (!world.isChunkLoaded(x >> 4, z >> 4)) throw new IllegalArgumentException("Seluruh chunk dinding harus dimuat saat setup.");
            Block block = world.getBlockAt(x, y, z); if (block.getType().isAir()) continue;
            if (!safe(block)) throw new IllegalArgumentException("Gunakan dinding solid biasa. Chest, block mesin, cairan, tanaman, dan block gravitasi tidak didukung: " + block.getType());
            blocks.add(new GateBlock(x, y, z, block.getBlockData().getAsString()));
        }
        return new Gate(area, blocks, GateStatus.CLOSED);
    }
    static boolean safe(Block block) {
        Material type = block.getType();
        return type.isSolid() && !type.hasGravity() && !type.isInteractable() && !block.isLiquid() && !block.isPassable() && !(block.getState() instanceof TileState)
                && !Set.of(Material.BEDROCK, Material.BARRIER, Material.END_PORTAL_FRAME, Material.STRUCTURE_VOID, Material.REINFORCED_DEEPSLATE).contains(type);
    }
    public boolean open(String id, Claim claim) throws IOException {
        Definition definition = definitions.get().get(id); Gate gate = definition == null ? null : definition.gate();
        if (closed || gate == null) throw new IllegalStateException("Gerbang belum disiapkan.");
        if (runs.containsKey(id) || gate.status() != GateStatus.CLOSED) return false;
        World world = ready(gate, false);
        Gate opening = gate.withStatus(GateStatus.OPENING); claim.save(opening); // Durable claim before any animation.
        Run run = new Run(id, opening); runs.put(id, run);
        try {
            for (GateBlock block : gate.blocks()) {
                BlockData data = Bukkit.createBlockData(block.data());
                BlockDisplay visual = world.spawn(new Location(world, block.x(), block.y(), block.z()), BlockDisplay.class);
                run.visuals.add(visual); visual.setBlock(data); visual.setPersistent(false); visual.setInvulnerable(true);
                visual.setGravity(false); visual.setTeleportDuration(1); visual.setInterpolationDuration(1);
            }
            world.playSound(center(gate), "minecraft:block.stone.hit", .7f, .6f);
        } catch (RuntimeException e) { abort(run); failure.accept(e); throw e; }
        return true;
    }
    public void tick() {
        if (closed) return; ticks++;
        suppressed.entrySet().removeIf(e -> e.getValue().until() < ticks);
        for (Run run : List.copyOf(runs.values())) try {
            run.age++;
            if (!run.opened) {
                ready(run.gate, false);
                double amplitude = .025 + .08 * run.age / 40.0;
                for (int i = 0; i < run.visuals.size(); i++) {
                    GateBlock block = run.gate.blocks().get(i);
                    Location location = new Location(Bukkit.getWorld(run.gate.area().world()), block.x() + Math.sin(run.age * 2.4 + i) * amplitude,
                            block.y() + Math.cos(run.age * 2 + i) * amplitude / 3, block.z() + Math.cos(run.age * 2.2 + i) * amplitude);
                    if (!run.visuals.get(i).isValid() || !run.visuals.get(i).teleport(location)) throw new IllegalStateException("Visual gerbang terputus.");
                }
                if (run.age % 10 == 0) center(run.gate).getWorld().playSound(center(run.gate), "minecraft:block.stone.hit", .5f, .5f);
                if (run.age >= 40) {
                    writer.write(run.id, run.gate.opened(System.currentTimeMillis())); // Timer starts after the windup; persisted across restarts.
                    run.opened = true; clearLoaded(run.gate);
                    World world = center(run.gate).getWorld(); world.playSound(center(run.gate), "minecraft:entity.generic.explode", .9f, .8f);
                    world.spawnParticle(Particle.EXPLOSION_EMITTER, center(run.gate), 1);
                    for (GateBlock block : run.gate.blocks()) world.spawnParticle(Particle.BLOCK,
                            new Location(world, block.x() + .5, block.y() + .5, block.z() + .5), 3, .2, .2, .2, Bukkit.createBlockData(block.data()));
                }
            } else {
                double t = (run.age - 40) / 12.0;
                for (int i = 0; i < run.visuals.size(); i++) {
                    GateBlock block = run.gate.blocks().get(i); BlockDisplay visual = run.visuals.get(i);
                    if (visual.isValid()) visual.teleport(new Location(Bukkit.getWorld(run.gate.area().world()), block.x() + Math.sin(i * 2.4) * t,
                            block.y() + 1.5 * t - 2 * t * t, block.z() + Math.cos(i * 2.4) * t));
                }
                if (run.age >= 52) remove(run);
            }
        } catch (IOException | RuntimeException e) { abort(run); failure.accept(e); }
        if (ticks % 20 == 0) autoClose(System.currentTimeMillis());
    }
    void autoClose(long now) {
        for (Definition definition : List.copyOf(definitions.get().values())) {
            Gate gate = definition.gate();
            if (gate == null || runs.containsKey(definition.id())) continue;
            try {
                if (gate.status() == GateStatus.OPEN && gate.timing().seconds() > 0) {
                    long remaining = gate.timing().closeAt() - now;
                    if (remaining > 0 && remaining <= 5000) warnNearby(gate, "Gerbang akan ditutup dalam " + ((remaining + 999) / 1000) + " detik", true);
                    if (gate.due(now)) {
                        if (!loaded(gate)) continue;
                        ready(gate, true);
                        if (occupied(gate)) { warnNearby(gate, "Gerbang menunggu pemain keluar dari posisi dinding", false); continue; }
                        writer.write(definition.id(), gate.withStatus(GateStatus.CLOSING));
                        restoreLoaded(gate, true);
                        writer.write(definition.id(), gate.withStatus(GateStatus.CLOSED));
                    }
                } else if (gate.status() == GateStatus.CLOSING) {
                    if (!loaded(gate) || occupied(gate)) continue;
                    ready(gate, true); restoreLoaded(gate, false);
                    writer.write(definition.id(), gate.withStatus(GateStatus.CLOSED));
                } else if (gate.status() == GateStatus.CLOSED && repairs.contains(definition.id()) && loaded(gate) && !occupied(gate)) {
                    restoreLoaded(gate, false); repairs.remove(definition.id());
                }
                reported.remove(definition.id());
            } catch (IOException | RuntimeException e) {
                if (reported.add(definition.id())) failure.accept(e); // One report until the condition is resolved.
            }
        }
    }
    private static boolean loaded(Gate gate) {
        World world = Bukkit.getWorld(gate.area().world());
        return world != null && gate.blocks().stream().allMatch(cell -> world.isChunkLoaded(cell.x() >> 4, cell.z() >> 4));
    }
    static boolean occupied(Gate gate) {
        World world = Bukkit.getWorld(gate.area().world());
        if (world == null) return true;
        for (Player player : world.getPlayers()) for (GateBlock block : gate.blocks())
            if (player.getBoundingBox().overlaps(new org.bukkit.util.BoundingBox(block.x(), block.y(), block.z(), block.x() + 1, block.y() + 1, block.z() + 1))) return true;
        return false;
    }
    private static void warnNearby(Gate gate, String text, boolean sound) {
        Location center = center(gate); if (center.getWorld() == null) return;
        for (Player player : center.getWorld().getPlayers()) if (player.getLocation().distanceSquared(center) <= 144) {
            player.sendActionBar(Component.text(text, NamedTextColor.YELLOW));
            if (sound) player.playSound(player.getLocation(), "minecraft:block.note_block.hat", .35f, 1.2f);
        }
    }
    /** Only fills air. A foreign block is never overwritten; occupants postpone restoration. */
    private static void restoreLoaded(Gate gate, boolean effects) {
        World world = Bukkit.getWorld(gate.area().world()); if (world == null || occupied(gate)) return;
        boolean restored = false;
        for (GateBlock cell : gate.blocks()) if (world.isChunkLoaded(cell.x() >> 4, cell.z() >> 4)) {
            Block block = world.getBlockAt(cell.x(), cell.y(), cell.z());
            if (!block.getType().isAir()) continue;
            BlockData data = Bukkit.createBlockData(cell.data());
            block.setBlockData(data, false); restored = true;
            if (effects) world.spawnParticle(Particle.BLOCK, new Location(world, cell.x() + .5, cell.y() + .5, cell.z() + .5), 2, .15, .15, .15, data);
        }
        if (effects && restored) world.playSound(center(gate), "minecraft:block.stone.place", .8f, .7f);
    }
    private void abort(Run run) {
        remove(run);
        if (!run.opened) try { writer.write(run.id, run.gate.withStatus(GateStatus.CLOSED)); }
        catch (IOException | RuntimeException e) { failure.accept(e); }
    }
    private void remove(Run run) { runs.remove(run.id); for (BlockDisplay visual : run.visuals) if (visual.isValid()) visual.remove(); }
    public void reset(String id, ResetSave saveClosed) throws IOException {
        Definition definition = definitions.get().get(id); Gate gate = definition == null ? null : definition.gate();
        if (gate == null || busy(id)) throw new IllegalArgumentException("Gerbang belum ada atau masih beranimasi.");
        World world = ready(gate, true);
        for (Player player : world.getPlayers()) {
            var box = player.getBoundingBox(); WhisperArea area = gate.area();
            if (box.overlaps(new org.bukkit.util.BoundingBox(area.minX(), area.minY(), area.minZ(), area.maxX() + 1, area.maxY() + 1, area.maxZ() + 1)))
                throw new IllegalStateException("Pindahkan pemain dari area gerbang sebelum reset.");
        }
        writer.write(id, gate.withStatus(GateStatus.CLOSING));
        restoreLoaded(gate, true);
        saveClosed.save();
        refresh();
    }
    private static World ready(Gate gate, boolean allowAir) {
        World world = Objects.requireNonNull(Bukkit.getWorld(gate.area().world()), "Dunia gerbang belum dimuat.");
        for (GateBlock cell : gate.blocks()) {
            if (!world.isChunkLoaded(cell.x() >> 4, cell.z() >> 4)) throw new IllegalStateException("Chunk gerbang belum dimuat.");
            Block block = world.getBlockAt(cell.x(), cell.y(), cell.z());
            if (block.getBlockData().getAsString().equals(cell.data()) && !safe(block)) throw new IllegalStateException("Block gerbang bukan dinding biasa yang aman.");
            if (!block.getBlockData().getAsString().equals(cell.data()) && !(allowAir && block.getType().isAir()))
                throw new IllegalStateException("Dinding berubah dari snapshot. Pulihkan block atau setup ulang; tidak ditimpa otomatis.");
        }
        return world;
    }
    private static void clearLoaded(Gate gate) {
        World world = Bukkit.getWorld(gate.area().world()); if (world == null) return;
        for (GateBlock cell : gate.blocks()) if (world.isChunkLoaded(cell.x() >> 4, cell.z() >> 4)) {
            Block block = world.getBlockAt(cell.x(), cell.y(), cell.z());
            if (block.getBlockData().getAsString().equals(cell.data()) && safe(block)) block.setType(Material.AIR, false);
        }
    }
    private static Location center(Gate gate) {
        WhisperArea area = gate.area(); return new Location(Bukkit.getWorld(area.world()), (area.minX() + area.maxX()) / 2.0 + .5,
                (area.minY() + area.maxY()) / 2.0 + .5, (area.minZ() + area.maxZ()) / 2.0 + .5);
    }
    private static Cell cell(UUID world, GateBlock block) { return new Cell(world, block.x(), block.y(), block.z()); }
    private static Cell cell(Block block) { return new Cell(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ()); }
    private boolean protectedAt(Block block) { return protectedCells.contains(cell(block)); }
    private static void requireAdmin(Player player) { if (!player.hasPermission("vitae.admin.npc")) throw new IllegalArgumentException("Perlu izin vitae.admin.npc."); }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void select(PlayerInteractEvent event) {
        Pick pick = picks.get(event.getPlayer().getUniqueId());
        if (pick == null || event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.LEFT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        event.setCancelled(true); UUID player = event.getPlayer().getUniqueId(); picks.remove(player);
        if (!event.getPlayer().hasPermission("vitae.admin.npc")) return;
        Cell point = cell(event.getClickedBlock()); Corners previous = corners.get(player);
        if (previous == null || !previous.id().equals(pick.id())) previous = new Corners(pick.id(), null, null);
        corners.put(player, pick.first() ? new Corners(pick.id(), point, previous.second()) : new Corners(pick.id(), previous.first(), point));
        suppressed.put(player, new Suppressed(point, ticks + 3));
        NpcService.message(event.getPlayer(), "Gerbang " + pick.id() + ": titik " + (pick.first() ? "A" : "B") + " = " + point.x() + ", " + point.y() + ", " + point.z() + ".");
    }
    private boolean suppressed(Player player, Block block) {
        Suppressed value = suppressed.get(player.getUniqueId());
        return picks.containsKey(player.getUniqueId()) || value != null && value.until() >= ticks && value.cell().equals(cell(block));
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void breakBlock(BlockBreakEvent event) { if (protectedAt(event.getBlock()) || suppressed(event.getPlayer(), event.getBlock())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void damageBlock(BlockDamageEvent event) { if (protectedAt(event.getBlock()) || suppressed(event.getPlayer(), event.getBlock())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void place(BlockPlaceEvent event) { if (protectedAt(event.getBlock())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void piston(BlockPistonExtendEvent event) { if (event.getBlocks().stream().anyMatch(b -> protectedAt(b) || protectedAt(b.getRelative(event.getDirection())))) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void retract(BlockPistonRetractEvent event) { if (event.getBlocks().stream().anyMatch(b -> protectedAt(b) || protectedAt(b.getRelative(event.getDirection())))) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void explosion(EntityExplodeEvent event) { event.blockList().removeIf(this::protectedAt); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void explosion(BlockExplodeEvent event) { event.blockList().removeIf(this::protectedAt); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void change(EntityChangeBlockEvent event) { if (protectedAt(event.getBlock())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void flow(BlockFromToEvent event) { if (protectedAt(event.getToBlock())) event.setCancelled(true); }
    @EventHandler public void chunk(ChunkLoadEvent event) {
        for (Definition definition : definitions.get().values()) if (definition.gate() != null
                && definition.gate().area().world().equals(event.getWorld().getUID())
                && event.getChunk().getX() >= (definition.gate().area().minX() >> 4) && event.getChunk().getX() <= (definition.gate().area().maxX() >> 4)
                && event.getChunk().getZ() >= (definition.gate().area().minZ() >> 4) && event.getChunk().getZ() <= (definition.gate().area().maxZ() >> 4)) {
            if (definition.gate().status() == GateStatus.OPEN) clearLoaded(definition.gate());
            else if (definition.gate().status() == GateStatus.CLOSED) repairs.add(definition.id());
        }
    }
    @EventHandler public void quit(PlayerQuitEvent event) { picks.remove(event.getPlayer().getUniqueId()); corners.remove(event.getPlayer().getUniqueId()); suppressed.remove(event.getPlayer().getUniqueId()); }
    @Override public void close() {
        closed = true; for (Run run : List.copyOf(runs.values())) abort(run);
        picks.clear(); corners.clear(); suppressed.clear(); protectedCells.clear(); repairs.clear(); reported.clear(); HandlerList.unregisterAll(this);
    }
}
'@
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcQuest.java'
        OriginalHash = '32cdfb12c381e999abd91f7bbf3b407255c9fc1004521337e9827f6b2b3b1484'
        Hash = '370aa44f067787f13fd8c0ca88b2877c37274b50cc0635536326d33d329890e2'
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
    private long ticks;
    public NpcQuest(JavaPlugin plugin, Supplier<Collection<NpcData.Definition>> definitions,
                    Consumer<Run> completed, BiPredicate<Player, Location> teleport) {
        this.plugin = plugin; this.definitions = definitions; this.completed = completed; this.teleport = teleport;
        key = new NamespacedKey(plugin, "npc_quest_mob");
    }
    public Run run(UUID player) { return runs.get(player); }
    public boolean uses(String npc) { return runs.values().stream().anyMatch(r -> r.npc.id().equals(npc)); }
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
        for (var crop : q.crops()) names.add(switch (Bukkit.createBlockData(crop.plant()).getMaterial()) {
            case CARROTS -> "wortel"; case POTATOES -> "kentang"; case WHEAT -> "gandum"; case BEETROOTS -> "bit";
            default -> "tanaman";
        });
        return "panen seluruh " + String.join(", ", names) + " setiap gelombang";
    }
    private void wave(Run run) {
        run.nextWave = 0;
        if (run.npc.quest().kind() == NpcData.QuestKind.FARM) {
            restore(run.npc.quest()); run.remaining.clear();
            for (var crop : run.npc.quest().crops()) run.remaining.add(crop.key());
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
        runs.remove(run.player); if (run.mob != null) run.mob.remove();
        Player p = Bukkit.getPlayer(run.player); if (p != null) p.hideBossBar(run.bar);
        if (run.npc.quest().kind() == NpcData.QuestKind.FARM) restore(run.npc.quest());
    }
    public static void restore(NpcData.Quest quest) {
        if (quest.kind() != NpcData.QuestKind.FARM) return;
        World world = Bukkit.getWorld(quest.area().world()); if (world == null) return;
        for (var crop : quest.crops()) {
            world.getBlockAt(crop.x(), crop.y() - 1, crop.z()).setBlockData(Bukkit.createBlockData(crop.soil()), false);
            world.getBlockAt(crop.x(), crop.y(), crop.z()).setBlockData(Bukkit.createBlockData(crop.plant()), false);
        }
    }
    public static List<NpcData.Crop> scan(WhisperArea area) {
        long volume = (long) (area.maxX() - area.minX() + 1) * (area.maxY() - area.minY() + 1) * (area.maxZ() - area.minZ() + 1);
        if (volume > 8192) throw new IllegalArgumentException("Seleksi ladang maksimum 8192 blok.");
        World world = Objects.requireNonNull(Bukkit.getWorld(area.world()), "Dunia belum dimuat.");
        var crops = new ArrayList<NpcData.Crop>();
        for (int x = area.minX(); x <= area.maxX(); x++) for (int y = area.minY(); y <= area.maxY(); y++) for (int z = area.minZ(); z <= area.maxZ(); z++) {
            Block b = world.getBlockAt(x, y, z);
            if (Set.of(Material.CARROTS, Material.POTATOES, Material.WHEAT, Material.BEETROOTS).contains(b.getType())) {
                Block soil = b.getRelative(BlockFace.DOWN); if (soil.getType() != Material.FARMLAND) throw new IllegalArgumentException("Tanaman harus berada di atas farmland.");
                Ageable plant = (Ageable) b.getBlockData(); plant.setAge(plant.getMaximumAge());
                crops.add(new NpcData.Crop(x, y, z, plant.getAsString(), soil.getBlockData().getAsString()));
            }
        }
        if (crops.isEmpty() || crops.size() > 512) throw new IllegalArgumentException("Siapkan 1–512 wortel/kentang/gandum/bit dalam seleksi.");
        return List.copyOf(crops);
    }
    private boolean protectedAt(Block block) {
        NpcData.Point p = NpcRenderer.point(block.getLocation());
        return definitions.get().stream().anyMatch(d -> d.enabled() && d.quest().kind() == NpcData.QuestKind.FARM && NpcData.inside(d.quest().area(), p))
                || runs.values().stream().anyMatch(r -> r.npc.quest().kind() == NpcData.QuestKind.FARM && NpcData.inside(r.npc.quest().area(), p));
    }
    private Run owner(Entity entity) {
        String id = entity.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (id == null) return null;
        try { return runs.get(UUID.fromString(id)); } catch (IllegalArgumentException e) { return null; }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void harvest(BlockBreakEvent e) {
        if (!protectedAt(e.getBlock())) return; e.setCancelled(true); e.setDropItems(false); e.setExpToDrop(0);
        Run r = runs.get(e.getPlayer().getUniqueId()); if (r == null || r.npc.quest().kind() != NpcData.QuestKind.FARM || r.nextWave > 0) return;
        Block b = e.getBlock(); String position = b.getX() + ":" + b.getY() + ":" + b.getZ();
        NpcData.Crop crop = r.npc.quest().crops().stream().filter(c -> c.key().equals(position)).findFirst().orElse(null);
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
    @Override public void close() { for (UUID player : List.copyOf(runs.keySet())) cancel(player, "", false); HandlerList.unregisterAll(this); }
}
'@
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcRenderer.java'
        OriginalHash = '27e727bb4fbcefd077b4cd3bd8a63a503d276dcb5822d1872f423246c992bafb'
        Hash = '1605b43a16da0f9513f298e540a9b5195cae8d2e3b713194fc71b9ad154a131d'
        Source = @'
package com.bangzachery.vitae.vitaemanager.npc;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Level;

/** Native humanoid packets for Paper 1.21.4; ModelEngine is an optional adapter. */
public final class NpcRenderer implements AutoCloseable {
    @FunctionalInterface public interface Input { void click(Player player, String npc, boolean attack); }
    private record Visible(NpcData.Definition definition, Interaction hitbox, int avatarId, UUID avatarUuid, ArmorStand base,
                           Object modeled, TextDisplay nametag, Set<UUID> viewers, Set<UUID> failed) {}
    private final JavaPlugin plugin;
    private final NamespacedKey key;
    private final Input input;
    private final Map<String, Visible> entities = new HashMap<>();
    private final Map<Integer, Visible> avatars = new HashMap<>();
    private final Listener modelListener = new Listener() {};
    private final Set<Chunk> tickets = new HashSet<>();
    private final Map<String, LinkedHashMap<UUID, NpcData.Point>> looking = new HashMap<>();
    private final Map<String, Map<UUID, NpcData.Point>> rotations = new HashMap<>();
    private final Map<String, NpcData.Point> modelRotations = new HashMap<>();
    private final Set<String> rotationErrors = new HashSet<>();
    public NpcRenderer(JavaPlugin plugin, Input input) {
        this.plugin = plugin; this.input = input; key = new NamespacedKey(plugin, "npc_id");
        hookModel();
    }
    public static Location location(NpcData.Point p) {
        World world = Bukkit.getWorld(p.world());
        if (world == null) throw new IllegalArgumentException("Dunia NPC belum dimuat.");
        return new Location(world, p.x(), p.y(), p.z(), p.yaw(), p.pitch());
    }
    public static NpcData.Point point(Location p) {
        return new NpcData.Point(p.getWorld().getUID(), p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch());
    }
    public String id(Entity entity) { return entity.getPersistentDataContainer().get(key, PersistentDataType.STRING); }
    /** Only accept packets for a humanoid avatar actually shown to this player. */
    public String id(Player player, int entityId) {
        Visible visible = avatars.get(entityId);
        return visible != null && visible.viewers().contains(player.getUniqueId())
                && player.getWorld().getUID().equals(visible.definition().location().world()) ? visible.definition().id() : null;
    }
    @SuppressWarnings("deprecation")
    private static int nextAvatarId() { return Bukkit.getUnsafe().nextEntityId(); }
    public void refresh(Map<String, NpcData.Definition> definitions) {
        for (String id : List.copyOf(entities.keySet())) {
            NpcData.Definition next = definitions.get(id);
            if (next == null || !next.enabled() || !next.equals(entities.get(id).definition())) remove(id);
        }
        for (var d : definitions.values()) if (d.enabled() && !entities.containsKey(d.id()) && Bukkit.getWorld(d.location().world()) != null) {
            Interaction box = null; ArmorStand base = null; Object modeled = null; TextDisplay nametag = null;
            try {
                Location at = location(d.facingLocation());
                Chunk chunk = at.getChunk(); chunk.addPluginChunkTicket(plugin); tickets.add(chunk);
                box = at.getWorld().spawn(at, Interaction.class, e -> {
                    e.setPersistent(false); e.setInteractionWidth(d.kind() == NpcData.Kind.HUMAN ? .8f : 1.5f);
                    e.setInteractionHeight((float) Math.max(1.8, d.eyeHeight() + .2));
                    e.getPersistentDataContainer().set(key, PersistentDataType.STRING, d.id());
                });
                if (d.kind() == NpcData.Kind.MODEL) {
                    if (!Bukkit.getPluginManager().isPluginEnabled("ModelEngine")) throw new IllegalArgumentException("ModelEngine belum aktif.");
                    base = at.getWorld().spawn(at, ArmorStand.class, e -> {
                        e.setVisible(false); e.setMarker(true); e.setGravity(false); e.setInvulnerable(true);
                        e.setPersistent(false); e.getPersistentDataContainer().set(key, PersistentDataType.STRING, d.id());
                    });
                    Class<?> api = Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");
                    Class<?> modelType = Class.forName("com.ticxo.modelengine.api.model.ModeledEntity");
                    Class<?> activeType = Class.forName("com.ticxo.modelengine.api.model.ActiveModel");
                    modeled = api.getMethod("createModeledEntity", Entity.class).invoke(null, base);
                    modelType.getMethod("setSaved", boolean.class).invoke(modeled, false);
                    modelType.getMethod("setBaseEntityVisible", boolean.class).invoke(modeled, false);
                    modelType.getMethod("setModelRotationLocked", boolean.class).invoke(modeled, true);
                    Object active = api.getMethod("createActiveModel", String.class).invoke(null, d.model());
                    if (active == null) throw new IllegalArgumentException("ID ModelEngine tidak ditemukan: " + d.model());
                    modelType.getMethod("addModel", activeType, boolean.class).invoke(modeled, active, false);
                    modelRotation(modelType, modeled, "setYBodyRot", at.getYaw());
                    modelRotation(modelType, modeled, "setYHeadRot", at.getYaw());
                    modelRotation(modelType, modeled, "setXHeadRot", at.getPitch());
                }
                // Never disguise the real Interaction entity under the same network ID.
                // Cancelling a use packet can resend its metadata, which is not Player metadata.
                int avatarId = d.kind() == NpcData.Kind.HUMAN ? nextAvatarId() : -1;
                if (d.appearance().nametag()) nametag = at.getWorld().spawn(at.clone().add(0, d.eyeHeight() + .45, 0), TextDisplay.class, e -> {
                    e.setPersistent(false); e.setGravity(false); e.setInvulnerable(true);
                    e.setBillboard(Display.Billboard.CENTER); e.setShadowed(true); e.setSeeThrough(false);
                    e.setDefaultBackground(false); e.setBackgroundColor(Color.fromARGB(0)); e.setLineWidth(240);
                    e.text(NpcText.name(d.name())); e.getPersistentDataContainer().set(key, PersistentDataType.STRING, d.id());
                });
                Visible visible = new Visible(d, box, avatarId, UUID.randomUUID(), base, modeled, nametag, new HashSet<>(), new HashSet<>());
                entities.put(d.id(), visible);
                if (d.kind() == NpcData.Kind.HUMAN) avatars.put(avatarId, visible);
            } catch (Exception e) {
                if (modeled != null) destroyModel(modeled); if (base != null) base.remove(); if (box != null) box.remove(); if (nametag != null) nametag.remove();
                releaseUnusedTickets();
                throw new IllegalStateException("NPC " + d.id() + " gagal ditampilkan. Periksa versi server/model.", e);
            }
        }
        releaseUnusedTickets();
    }
    private void releaseUnusedTickets() {
        Set<Chunk> used = new HashSet<>();
        for (Visible v : entities.values()) used.add(v.hitbox().getLocation().getChunk());
        for (Chunk chunk : Set.copyOf(tickets)) if (!used.contains(chunk)) {
            chunk.removePluginChunkTicket(plugin); tickets.remove(chunk);
        }
    }
    public void tick() {
        for (Visible v : entities.values()) {
            if (v.definition().kind() != NpcData.Kind.HUMAN) continue;
            Set<UUID> tracked = new HashSet<>();
            for (Player player : v.hitbox().getTrackedBy()) {
                tracked.add(player.getUniqueId());
                if (v.viewers().contains(player.getUniqueId()) || v.failed().contains(player.getUniqueId())) continue;
                try { show(player, v); v.viewers().add(player.getUniqueId()); }
                catch (Exception e) {
                    hide(player, v);
                    v.failed().add(player.getUniqueId());
                    plugin.getLogger().log(Level.SEVERE, "Paket NPC manusia gagal pada Paper 1.21.4: " + v.definition().id(), e);
                }
            }
            for (UUID old : Set.copyOf(v.viewers())) if (!tracked.contains(old)) {
                Player player = Bukkit.getPlayer(old); if (player != null) hide(player, v);
                v.viewers().remove(old);
            }
            v.failed().retainAll(tracked);
        }
    }
    public void forget(Player player) {
        for (Visible v : entities.values()) {
            endLook(v.definition().id(), player);
            if (v.viewers().remove(player.getUniqueId()) && player.isOnline()) hide(player, v);
            v.failed().remove(player.getUniqueId());
        }
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void show(Player player, Visible v) throws ReflectiveOperationException {
        var d = v.definition(); UUID uuid = v.avatarUuid(); int entityId = v.avatarId();
        Class<?> profileType = Class.forName("com.mojang.authlib.GameProfile");
        hideInternalName(player, v);
        Object profile = profileType.getConstructor(UUID.class, String.class).newInstance(uuid, profileName(v));
        if (!d.texture().isBlank()) {
            Class<?> property = Class.forName("com.mojang.authlib.properties.Property");
            Object texture = property.getConstructor(String.class, String.class, String.class)
                    .newInstance("textures", d.texture(), d.signature().isBlank() ? null : d.signature());
            Object properties = profileType.getMethod("getProperties").invoke(profile);
            Class.forName("com.google.common.collect.Multimap").getMethod("put", Object.class, Object.class).invoke(properties, "textures", texture);
        }
        String prefix = "net.minecraft.network.protocol.game.";
        Class<? extends Enum> action = (Class<? extends Enum>) Class.forName(prefix + "ClientboundPlayerInfoUpdatePacket$Action");
        EnumSet actions = EnumSet.noneOf(action);
        for (String name : List.of("ADD_PLAYER", "UPDATE_LISTED", "UPDATE_GAME_MODE", "UPDATE_HAT")) actions.add(Enum.valueOf(action, name));
        Class<?> game = Class.forName("net.minecraft.world.level.GameType");
        Class<?> component = Class.forName("net.minecraft.network.chat.Component");
        Class<?> chat = Class.forName("net.minecraft.network.chat.RemoteChatSession$Data");
        Class<?> entryType = Class.forName(prefix + "ClientboundPlayerInfoUpdatePacket$Entry");
        Object entry = entryType.getConstructor(UUID.class, profileType, boolean.class, int.class, game, component, boolean.class, int.class, chat)
                .newInstance(uuid, profile, false, 0, game.getField("SURVIVAL").get(null), null, true, 0, null);
        Class<?> infoType = Class.forName(prefix + "ClientboundPlayerInfoUpdatePacket"); Object info;
        try { info = infoType.getConstructor(EnumSet.class, entryType).newInstance(actions, entry); }
        catch (NoSuchMethodException e) {
            info = infoType.getConstructor(EnumSet.class, Collection.class).newInstance(actions, List.of());
            Field entries = infoType.getDeclaredField("entries"); entries.setAccessible(true); entries.set(info, List.of(entry));
        }
        send(player, info);
        Class<?> entityType = Class.forName("net.minecraft.world.entity.EntityType");
        Class<?> vector = Class.forName("net.minecraft.world.phys.Vec3");
        NpcData.Point pose = d.facingLocation();
        Object spawn = Class.forName(prefix + "ClientboundAddEntityPacket")
                .getConstructor(int.class, UUID.class, double.class, double.class, double.class, float.class, float.class, entityType, int.class, vector, double.class)
                .newInstance(entityId, uuid, pose.x(), pose.y(), pose.z(), pose.pitch(), pose.yaw(),
                        entityType.getField("PLAYER").get(null), 0, vector.getField("ZERO").get(null), (double) pose.yaw());
        send(player, spawn);
        Class<?> accessor = Class.forName("net.minecraft.network.syncher.EntityDataAccessor");
        Field skin = Class.forName("net.minecraft.world.entity.player.Player").getDeclaredField("DATA_PLAYER_MODE_CUSTOMISATION"); skin.setAccessible(true);
        Object value = Class.forName("net.minecraft.network.syncher.SynchedEntityData$DataValue")
                .getMethod("create", accessor, Object.class).invoke(null, skin.get(null), (byte) 127);
        send(player, Class.forName(prefix + "ClientboundSetEntityDataPacket").getConstructor(int.class, List.class).newInstance(entityId, List.of(value)));
    }
    private static void send(Player player, Object packet) throws ReflectiveOperationException {
        Object handle = player.getClass().getMethod("getHandle").invoke(player);
        Object connection = handle.getClass().getField("connection").get(handle);
        connection.getClass().getMethod("send", Class.forName("net.minecraft.network.protocol.Packet")).invoke(connection, packet);
    }
    private static String profileName(Visible v) { return "VN_" + v.avatarUuid().toString().replace("-", "").substring(0, 13); }
    private static String teamName(Visible v) { return "VT" + v.avatarUuid().toString().replace("-", "").substring(0, 14); }
    /** Client-only team containing this fake profile alone; real player scoreboards are untouched. */
    @SuppressWarnings("unchecked")
    private static Object nameTeam(Visible v) throws ReflectiveOperationException {
        Class<?> board = Class.forName("net.minecraft.world.scores.Scoreboard");
        Class<?> team = Class.forName("net.minecraft.world.scores.PlayerTeam");
        Object value = team.getConstructor(board, String.class).newInstance(board.getConstructor().newInstance(), teamName(v));
        Class<?> visibility = Class.forName("net.minecraft.world.scores.Team$Visibility");
        team.getMethod("setNameTagVisibility", visibility).invoke(value, visibility.getField("NEVER").get(null));
        ((Collection<String>) team.getMethod("getPlayers").invoke(value)).add(profileName(v));
        return value;
    }
    private static void hideInternalName(Player player, Visible v) throws ReflectiveOperationException {
        Object team = nameTeam(v);
        send(player, Class.forName("net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket")
                .getMethod("createAddOrModifyPacket", team.getClass(), boolean.class).invoke(null, team, true));
    }
    public void lookAt(String id, Player player) {
        Visible v = entities.get(id); if (v == null || !v.definition().appearance().track()) return;
        var targets = looking.computeIfAbsent(id, ignored -> new LinkedHashMap<>());
        targets.put(player.getUniqueId(), point(player.getEyeLocation()));
        if (v.definition().kind() == NpcData.Kind.HUMAN) {
            if (!v.viewers().contains(player.getUniqueId())) return;
            var map = rotations.computeIfAbsent(id, ignored -> new HashMap<>());
            NpcData.Point next = NpcFacing.towards(v.definition(), targets.get(player.getUniqueId()), map.getOrDefault(player.getUniqueId(), v.definition().facingLocation()));
            rotateHuman(player, v, next); map.put(player.getUniqueId(), next);
        } else rotateModel(v, NpcFacing.towards(v.definition(), targets.values().iterator().next(), modelRotations.getOrDefault(id, v.definition().facingLocation())));
    }
    public void endLook(String id, Player player) {
        var targets = looking.get(id); if (targets != null) { targets.remove(player.getUniqueId()); if (targets.isEmpty()) looking.remove(id); }
        var map = rotations.get(id); if (map != null) map.remove(player.getUniqueId());
        Visible v = entities.get(id); if (v == null) return;
        if (v.definition().kind() == NpcData.Kind.HUMAN) {
            if (player.isOnline() && v.viewers().contains(player.getUniqueId())) rotateHuman(player, v, v.definition().facingLocation());
        } else if (targets == null || targets.isEmpty()) rotateModel(v, v.definition().facingLocation());
        else rotateModel(v, NpcFacing.towards(v.definition(), targets.values().iterator().next(), modelRotations.getOrDefault(id, v.definition().facingLocation())));
    }
    private void rotateModel(Visible v, NpcData.Point pose) {
        try {
            v.base().setRotation(pose.yaw(), pose.pitch());
            Class<?> type = Class.forName("com.ticxo.modelengine.api.model.ModeledEntity");
            modelRotation(type, v.modeled(), "setYBodyRot", pose.yaw()); modelRotation(type, v.modeled(), "setYHeadRot", pose.yaw());
            modelRotation(type, v.modeled(), "setXHeadRot", pose.pitch()); modelRotations.put(v.definition().id(), pose);
        } catch (ReflectiveOperationException | RuntimeException e) { rotationFailure(v, e); }
    }
    private void rotateHuman(Player player, Visible v, NpcData.Point pose) {
        Object buffer = null;
        try {
            byte yaw = (byte) Math.floor(pose.yaw() * 256 / 360), pitch = (byte) Math.floor(pose.pitch() * 256 / 360);
            send(player, Class.forName("net.minecraft.network.protocol.game.ClientboundMoveEntityPacket$Rot")
                    .getConstructor(int.class, byte.class, byte.class, boolean.class).newInstance(v.avatarId(), yaw, pitch, true));
            Class<?> byteBuf = Class.forName("io.netty.buffer.ByteBuf"), friendly = Class.forName("net.minecraft.network.FriendlyByteBuf");
            buffer = Class.forName("io.netty.buffer.Unpooled").getMethod("buffer").invoke(null);
            Object data = friendly.getConstructor(byteBuf).newInstance(buffer);
            friendly.getMethod("writeVarInt", int.class).invoke(data, v.avatarId()); friendly.getMethod("writeByte", int.class).invoke(data, (int) yaw);
            Object codec = Class.forName("net.minecraft.network.protocol.game.ClientboundRotateHeadPacket").getField("STREAM_CODEC").get(null);
            Object packet = Class.forName("net.minecraft.network.codec.StreamDecoder").getMethod("decode", Object.class).invoke(codec, data);
            send(player, packet);
        } catch (ReflectiveOperationException | RuntimeException e) { rotationFailure(v, e); }
        finally { if (buffer != null) try { Class.forName("io.netty.buffer.ByteBuf").getMethod("release").invoke(buffer); } catch (ReflectiveOperationException ignored) { } }
    }
    private void rotationFailure(Visible v, Exception error) {
        if (rotationErrors.add(v.definition().id())) plugin.getLogger().log(Level.WARNING, "Arah NPC gagal diperbarui: " + v.definition().id(), error);
    }
    private static void modelRotation(Class<?> type, Object model, String setter, float angle) throws ReflectiveOperationException {
        Method method;
        try { method = type.getMethod(setter + "Immediately", float.class); }
        catch (NoSuchMethodException e) { method = type.getMethod(setter, float.class); }
        method.invoke(model, angle);
    }
    private void hide(Player p, Visible v) {
        try {
            send(p, Class.forName("net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket")
                    .getConstructor(int[].class).newInstance((Object) new int[]{v.avatarId()}));
            send(p, Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket")
                    .getConstructor(List.class).newInstance(List.of(v.avatarUuid())));
            Object team = nameTeam(v);
            send(p, Class.forName("net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket")
                    .getMethod("createRemovePacket", team.getClass()).invoke(null, team));
        }
        catch (ReflectiveOperationException e) { plugin.getLogger().fine("Profil NPC sudah tidak tersedia bagi " + p.getName()); }
    }
    @SuppressWarnings("unchecked") private void hookModel() {
        if (!Bukkit.getPluginManager().isPluginEnabled("ModelEngine")) return;
        try {
            Class<?> eventType = Class.forName("com.ticxo.modelengine.api.events.BaseEntityInteractEvent");
            Class<?> baseType = Class.forName("com.ticxo.modelengine.api.entity.BaseEntity");
            Bukkit.getPluginManager().registerEvent((Class<? extends Event>) eventType, modelListener, EventPriority.LOWEST, (listener, event) -> {
                try {
                    Player p = (Player) eventType.getMethod("getPlayer").invoke(event);
                    boolean attack = eventType.getMethod("getAction").invoke(event).toString().equals("ATTACK");
                    if (!attack && eventType.getMethod("getSlot").invoke(event) == EquipmentSlot.OFF_HAND) return;
                    Object base = eventType.getMethod("getBaseEntity").invoke(event);
                    UUID uuid = (UUID) baseType.getMethod("getUUID").invoke(base);
                    Runnable dispatch = () -> { for (Visible v : entities.values()) if (v.base() != null && v.base().getUniqueId().equals(uuid)) {
                        if (event instanceof Cancellable cancellable) cancellable.setCancelled(true);
                        input.click(p, v.definition().id(), attack); break;
                    } };
                    if (event.isAsynchronous()) Bukkit.getScheduler().runTask(plugin, dispatch); else dispatch.run();
                } catch (ReflectiveOperationException e) { throw new org.bukkit.event.EventException(e); }
            }, plugin, false);
        } catch (ReflectiveOperationException | LinkageError e) {
            plugin.getLogger().log(Level.WARNING, "Pengait klik ModelEngine tidak tersedia; NPC manusia dan hitbox Vitae tetap dapat dipakai.", e);
        }
    }
    private void destroyModel(Object model) {
        try {
            Class<?> type = Class.forName("com.ticxo.modelengine.api.model.ModeledEntity");
            Object base = type.getMethod("getBase").invoke(model);
            UUID uuid = (UUID) Class.forName("com.ticxo.modelengine.api.entity.BaseEntity").getMethod("getUUID").invoke(base);
            Class.forName("com.ticxo.modelengine.api.ModelEngineAPI").getMethod("removeModeledEntity", UUID.class).invoke(null, uuid);
            if (!(boolean) type.getMethod("isDestroyed").invoke(model)) type.getMethod("destroy").invoke(model);
        }
        catch (ReflectiveOperationException e) { plugin.getLogger().log(Level.WARNING, "Pembersihan model NPC gagal.", e); }
    }
    private void remove(String id) {
        Visible v = entities.remove(id); if (v == null) return;
        avatars.remove(v.avatarId());
        looking.remove(id); rotations.remove(id); modelRotations.remove(id); rotationErrors.remove(id);
        for (UUID viewer : v.viewers()) { Player p = Bukkit.getPlayer(viewer); if (p != null) hide(p, v); }
        if (v.modeled() != null) destroyModel(v.modeled()); if (v.base() != null) v.base().remove(); if (v.nametag() != null) v.nametag().remove(); v.hitbox().remove();
    }
    @Override public void close() {
        for (String id : List.copyOf(entities.keySet())) remove(id);
        for (Chunk chunk : tickets) chunk.removePluginChunkTicket(plugin); tickets.clear(); HandlerList.unregisterAll(modelListener);
    }
}
'@
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcService.java'
        OriginalHash = '997d10e713ae257703ff9dc65a7874056f67657d9ac6d4676cf6b21947a87b5a'
        Hash = '0f1712e9b1e682079a4d62c87a0d452aa78d03d5a71f61590bab7080bebb633c'
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
            for (var d : state.npcs().values()) if (d.enabled()) NpcQuest.restore(d.quest());
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
        return gates.busy(id) || quests.uses(id) || sessions.values().stream().anyMatch(s -> s.npc.id().equals(id));
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
        if (inUse(id)) throw new IllegalStateException("NPC sedang dipakai. Selesaikan dialog/quest sebelum mengubahnya.");
        JsonObject json = NpcStore.JSON.toJsonTree(old).getAsJsonObject(); change.accept(json);
        json.addProperty("enabled", enable); NpcData.Definition next = NpcStore.JSON.fromJson(json, NpcData.Definition.class);
        if (!next.id().equals(old.id()) || !next.token().equals(old.token())) throw new IllegalArgumentException("Identitas NPC tidak boleh berubah.");
        validateRuntime(next);
        NpcData.State previous = state; commit(state.put(next));
        try { gates.refresh(); renderer.refresh(state.npcs()); if (next.enabled()) NpcQuest.restore(next.quest()); }
        catch (RuntimeException e) { commit(previous); gates.refresh(); renderer.refresh(previous.npcs()); throw e; }
    }
    private void validateRuntime(NpcData.Definition d) {
        validateRuntime(d, true);
    }
    private void validateRuntime(NpcData.Definition d, boolean requireWorld) {
        for (String item : d.gift().items()) decode(item);
        for (String item : d.quest().reward().items()) decode(item);
        if (d.quest().kind() == NpcData.QuestKind.MOB) NpcQuest.mobType(d.quest().mob());
        for (var crop : d.quest().crops()) {
            var plant = Bukkit.createBlockData(crop.plant()); var soil = Bukkit.createBlockData(crop.soil());
            if (!(plant instanceof org.bukkit.block.data.Ageable) || !Set.of(Material.CARROTS, Material.POTATOES, Material.WHEAT, Material.BEETROOTS).contains(plant.getMaterial())
                    || soil.getMaterial() != Material.FARMLAND) throw new IllegalArgumentException("Snapshot ladang tidak valid.");
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
    @EventHandler public void worldLoad(WorldLoadEvent e) { renderer.refresh(state.npcs()); for (var d : state.npcs().values()) if (d.enabled() && !quests.uses(d.id())) NpcQuest.restore(d.quest()); }
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
    }
}
'@
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcStore.java'
        OriginalHash = '5627bcc898bcc07288e2816cecdedcae36b4092d71b5170a9e05b32de9193b16'
        Hash = 'f52a700aa8996c66af6e4a557430f83573c629a553efe15b2f9f05bc7931d524'
        Source = @'
package com.bangzachery.vitae.vitaemanager.npc;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import java.io.*;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
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
        if (Files.size(file) > 16_000_000) throw new IOException("npc.json melebihi 16 MB.");
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
        byte[] bytes = JSON.toJson(state).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 16_000_000) throw new IOException("Data NPC melebihi 16 MB.");
        Path parent = file.toAbsolutePath().getParent(); Files.createDirectories(parent);
        Path temp = Files.createTempFile(parent, "npc-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes); while (buffer.hasRemaining()) channel.write(buffer); channel.force(true);
            }
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
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcText.java'
        OriginalHash = ''
        Hash = '38c28731c8474ec3973d56dfe96bee794c4ba47dafc4649d6a14f2ef379dc12e'
        Source = @'
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
'@
    },
    [PSCustomObject]@{
        Name = 'src/test/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionAftermathDataTest.java'
        OriginalHash = ''
        Hash = '9679bd20ca72a88222c8d8cf961cdf97f7537023f4052bb681c4f75f0b18fdad'
        Source = @'
package com.bangzachery.vitae.vitaemanager.evolution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionAltar.*;
import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionStore.*;
import static org.junit.jupiter.api.Assertions.*;

class EvolutionAftermathDataTest {
    @TempDir Path directory;
    EvolutionStore store() { return new EvolutionStore(directory.resolve("evolution.yml"), EvolutionStoreTest.sounds()); }
    @Test void oldAltarsDepositsAndTicketsMigrateWithoutChangingMusicOrItems() throws Exception {
        var altar = EvolutionStoreTest.altar(); UUID owner = EvolutionStoreTest.OWNER;
        var deposit = new Deposit(UUID.randomUUID(), owner, "api", altar.table(), Map.of(0, "AQ=="), null);
        State state = EvolutionStoreTest.initial().deposit(deposit);
        var yaml = store().document(state); yaml.set("altars.api.aftermath", null); Path file = directory.resolve("evolution.yml"); yaml.save(file.toFile());
        byte[] before = Files.readAllBytes(file); State migrated = store().initialize(); assertEquals(state, migrated);
        assertEquals(100, migrated.altars().get("api").aftermath().ticks()); assertEquals(altar.music(), migrated.altars().get("api").music());
        assertEquals(state.deposits(), migrated.deposits());
        List<Path> backups; try (var files = Files.list(directory)) { backups = files.filter(p -> p.toString().endsWith(".bak")).toList(); }
        assertEquals(1, backups.size()); assertArrayEquals(before, Files.readAllBytes(backups.getFirst()));
        assertEquals(migrated, store().initialize()); try (var files = Files.list(directory)) { assertEquals(1, files.filter(p -> p.toString().endsWith(".bak")).count()); }
    }
    @Test void revealCheckpointSurvivesCrashAndCannotRefundOldWeapon() throws Exception {
        var altar = EvolutionStoreTest.altar(); var owner = EvolutionStoreTest.OWNER;
        Ticket ticket = new Ticket(UUID.randomUUID(), owner, "api", altar.table(), Phase.PREPARED, List.of("AQ=="), "Ag==");
        State state = EvolutionStoreTest.initial().prepare(ticket).phase(owner, ticket.id(), Phase.ACTIVE).phase(owner, ticket.id(), Phase.COMMITTING).phase(owner, ticket.id(), Phase.REVEALING);
        store().save(state); assertEquals(state, store().load());
        assertThrows(IllegalArgumentException.class, () -> state.phase(owner, ticket.id(), Phase.REFUNDING));
        assertThrows(IllegalArgumentException.class, () -> state.phase(owner, ticket.id(), Phase.RETURNED));
        assertEquals(Phase.DELIVERED, state.phase(owner, ticket.id(), Phase.DELIVERED).tickets().get(owner).phase());
        assertEquals(Phase.DISCARDED, state.phase(owner, ticket.id(), Phase.DISCARDED).tickets().get(owner).phase());
    }
    @Test void endingSettingsDoNotChangeExplosionOrMusicTimeline() {
        var yaml = store().document(EvolutionStoreTest.initial()); yaml.set("altars.api.aftermath.duration", 12);
        var altar = store().parse(yaml).altars().get("api"); assertEquals(240, altar.aftermath().ticks());
        assertEquals(180, altar.ticks()); assertEquals(180, altar.music().ticks());
        yaml.set("altars.api.aftermath.enabled", false); assertFalse(store().parse(yaml).altars().get("api").aftermath().enabled());
    }
    @Test void invalidEndingCannotReplaceSavedState() throws Exception {
        store().save(EvolutionStoreTest.initial()); byte[] before = Files.readAllBytes(directory.resolve("evolution.yml"));
        for (double invalid : List.of(0.0, 1.0, 16.0, Double.NaN)) {
            var yaml = store().document(EvolutionStoreTest.initial()); yaml.set("altars.api.aftermath.duration", invalid);
            assertThrows(IllegalArgumentException.class, () -> store().parse(yaml));
        }
        assertArrayEquals(before, Files.readAllBytes(directory.resolve("evolution.yml")));
    }
}
'@
    },
    [PSCustomObject]@{
        Name = 'src/test/java/com/bangzachery/vitae/vitaemanager/npc/NpcStoryDataTest.java'
        OriginalHash = ''
        Hash = '3bc93762110cb35e19fe12764f2cdb23b4d8afb55af502f505396667e4bab8cf'
        Source = @'
package com.bangzachery.vitae.vitaemanager.npc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.*;
import static com.bangzachery.vitae.vitaemanager.npc.NpcData.*;
import static org.junit.jupiter.api.Assertions.*;

class NpcStoryDataTest {
    @TempDir Path directory;
    Definition npc() {
        var json = NpcStore.JSON.toJsonTree(NpcGateDataTest.npc("guard")).getAsJsonObject();
        json.add("nodes", NpcStore.JSON.toJsonTree(Map.of("start", new Node("Bantu aku?", List.of(
                new Choice("Ya", "END", "sikap", "membantu"), new Choice("Tidak", "END", "sikap", "menolak"))),
                "ramah", new Node("Terima kasih", List.of()), "dingin", new Node("Kamu menolak", List.of()))));
        json.add("memory", NpcStore.JSON.toJsonTree(List.of(new MemoryRule("sikap", "membantu", "ramah"), new MemoryRule("sikap", "menolak", "dingin"))));
        json.add("gift", NpcStore.JSON.toJsonTree(new Reward(new BigDecimal("100"), List.of())));
        return NpcStore.JSON.fromJson(json, Definition.class);
    }
    @Test void legacyNpcMigratesOnceWithExactBackupAndKeepsIdentityProgressQuotaAndGift() throws Exception {
        Definition npc = npc().withGate(NpcGateDataTest.gate(GateStatus.OPEN)); UUID player = UUID.randomUUID();
        State state = State.empty().put(npc).finish(player, npc, false, UUID.randomUUID(), 100);
        var json = NpcStore.JSON.toJsonTree(state).getAsJsonObject(); json.remove("memories");
        var record = json.getAsJsonObject("npcs").getAsJsonObject("guard"); record.remove("appearance"); record.remove("memory");
        record.getAsJsonObject("gate").remove("timing");
        for (var entry : record.getAsJsonObject("nodes").entrySet()) for (var choice : entry.getValue().getAsJsonObject().getAsJsonArray("choices")) {
            choice.getAsJsonObject().remove("memoryKey"); choice.getAsJsonObject().remove("memoryValue");
        }
        Path file = directory.resolve("npc.json"); Files.writeString(file, NpcStore.JSON.toJson(json)); byte[] old = Files.readAllBytes(file);
        NpcStore store = new NpcStore(file); State loaded = store.initialize();
        assertEquals(npc.token(), loaded.npcs().get("guard").token()); assertEquals(state.completed(), loaded.completed());
        assertEquals(state.pending(), loaded.pending()); assertEquals(state.quotas(), loaded.quotas());
        assertTrue(loaded.npcs().get("guard").appearance().nametag()); assertTrue(loaded.npcs().get("guard").appearance().track());
        assertEquals(30, loaded.npcs().get("guard").gate().timing().seconds());
        List<Path> backups; try (var files = Files.list(directory)) { backups = files.filter(p -> p.toString().endsWith(".bak")).toList(); }
        assertEquals(1, backups.size()); assertArrayEquals(old, Files.readAllBytes(backups.getFirst()));
        assertEquals(loaded, store.initialize()); try (var files = Files.list(directory)) { assertEquals(1, files.filter(p -> p.toString().endsWith(".bak")).count()); }
    }
    @Test void playersAndNpcsKeepSeparateMemoriesAcrossRestartAndRewardAcknowledgement() throws Exception {
        Definition npc = npc(); UUID yes = UUID.randomUUID(), no = UUID.randomUUID();
        State state = State.empty().put(npc).remember(yes, npc, npc.nodes().get("start").choices().get(0))
                .remember(no, npc, npc.nodes().get("start").choices().get(1)).finish(yes, npc, false, UUID.randomUUID(), 100);
        assertEquals("ramah", state.recalledRoot(yes, npc)); assertEquals("dingin", state.recalledRoot(no, npc));
        state = state.acknowledge(yes, state.pending().get(yes).receipt()); assertEquals("ramah", state.recalledRoot(yes, npc));
        State secondVisit = state.finish(yes, npc, false, UUID.randomUUID(), 101); assertTrue(secondVisit.pending().isEmpty());
        var store = new NpcStore(directory.resolve("npc.json")); store.save(secondVisit); assertEquals(secondVisit, store.initialize());
        Definition other = NpcGateDataTest.npc("other"); assertTrue(secondVisit.memory(yes, other).isEmpty());
    }
    @Test void gateCyclesAndRenamesRetainMemoriesAndOnceOnlyGift() {
        Definition npc = npc().withGate(NpcGateDataTest.gate(GateStatus.CLOSED)); UUID player = UUID.randomUUID();
        State state = State.empty().put(npc).remember(player, npc, npc.nodes().get("start").choices().getFirst()).finish(player, npc, false, UUID.randomUUID(), 100);
        state = state.acknowledge(player, state.pending().get(player).receipt());
        for (int i = 0; i < 3; i++) {
            state = state.put(state.npcs().get("guard").withGate(npc.gate().opened(1000 + i * 40_000)));
            state = state.put(state.npcs().get("guard").withGate(npc.gate().withStatus(GateStatus.CLOSED)));
            state = state.finish(player, state.npcs().get("guard"), false, UUID.randomUUID(), 100);
        }
        var json = NpcStore.JSON.toJsonTree(state.npcs().get("guard")).getAsJsonObject(); json.addProperty("name", "&6Penjaga &eGerbang");
        var renamed = NpcStore.JSON.fromJson(json, Definition.class); state = state.put(renamed);
        assertTrue(state.done(player, renamed)); assertTrue(state.pending().isEmpty()); assertEquals("ramah", state.recalledRoot(player, renamed));
    }
    @Test void resettingOneNpcDoesNotEraseOtherPlayersMemories() {
        Definition npc = npc(); UUID first = UUID.randomUUID(), second = UUID.randomUUID(); Choice choice = npc.nodes().get("start").choices().getFirst();
        State state = State.empty().put(npc).remember(first, npc, choice).remember(second, npc, choice);
        state = state.reset(first, npc); assertEquals("start", state.recalledRoot(first, npc)); assertEquals("ramah", state.recalledRoot(second, npc));
    }
    @Test void changedChoiceOverwritesSameKeyWithoutDestroyingOtherKeys() {
        Definition npc = npc(); UUID player = UUID.randomUUID();
        State state = State.empty().put(npc).remember(player, npc, new Choice("Secret", "END", "rahasia", "tahu"))
                .remember(player, npc, npc.nodes().get("start").choices().getFirst()).remember(player, npc, npc.nodes().get("start").choices().get(1));
        assertEquals(Map.of("sikap", "menolak", "rahasia", "tahu"), state.memory(player, npc)); assertEquals("dingin", state.recalledRoot(player, npc));
    }
    @Test void invalidNewFieldsNeverMigrateOrEraseOldFile() throws Exception {
        var json = NpcStore.JSON.toJsonTree(State.empty().put(npc())).getAsJsonObject();
        json.getAsJsonObject("npcs").getAsJsonObject("guard").getAsJsonObject("appearance").addProperty("track", "yes");
        Path file = directory.resolve("npc.json"); Files.writeString(file, NpcStore.JSON.toJson(json)); byte[] before = Files.readAllBytes(file);
        assertThrows(java.io.IOException.class, () -> new NpcStore(file).initialize()); assertArrayEquals(before, Files.readAllBytes(file));
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
    }
    @Test void timerSurvivesRestartAndRepeatedCompletionDoesNotExtendIt() throws Exception {
        Definition npc = npc().withGate(NpcGateDataTest.gate(GateStatus.CLOSED).opened(1000));
        State state = State.empty().put(npc).finish(UUID.randomUUID(), npc, false, UUID.randomUUID(), 20_000);
        var store = new NpcStore(directory.resolve("npc.json")); store.save(state);
        var gate = store.initialize().npcs().get("guard").gate(); assertEquals(31_000, gate.timing().closeAt());
        assertFalse(gate.due(30_999)); assertTrue(gate.due(31_000)); assertTrue(gate.due(50_000));
        assertFalse(gate.withDelay(0, 0).due(Long.MAX_VALUE));
    }
    @Test void malformedTimerRulesAndIncompleteMemoriesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new GateTiming(-1, 0)); assertThrows(IllegalArgumentException.class, () -> new GateTiming(3601, 0));
        assertThrows(IllegalArgumentException.class, () -> new GateTiming(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new Choice("Yes", "END", "sikap", ""));
        var json = NpcStore.JSON.toJsonTree(npc()).getAsJsonObject(); json.addProperty("enabled", true);
        json.add("memory", NpcStore.JSON.toJsonTree(List.of(new MemoryRule("sikap", "yes", "missing"))));
        assertThrows(RuntimeException.class, () -> NpcStore.JSON.fromJson(json, Definition.class));
    }
    @Test void colouredAndPlainNamesHaveNoPrefixAndNoCommandEvents() {
        assertEquals("Penjaga Gerbang", NpcText.plain("&6Penjaga &eGerbang"));
        assertEquals("Penjaga", NpcText.plain("Penjaga")); assertNull(NpcText.name("&6Penjaga").clickEvent());
    }
    @Test void trackingKeepsSavedIdlePoseAndUsesShortestRotation() {
        Definition npc = npc(); Point idle = npc.facingLocation(), current = idle;
        for (int i = 0; i < 10; i++) current = NpcFacing.towards(npc, new Point(idle.world(), idle.x() + 4, idle.y() + npc.eyeHeight(), idle.z(), 0, 0), current);
        assertEquals(-90, current.yaw(), .001); assertEquals(0, current.pitch(), .001); assertEquals(idle, npc.facingLocation());
        assertEquals(idle.x(), current.x()); assertEquals(idle.y(), current.y()); assertEquals(idle.z(), current.z());
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
if ($plansVitae.Count -eq 0) { Write-Host 'Update NPC cerita dan evo ini sudah terpasang; tidak ditulis ulang.' -ForegroundColor Green; return }
Write-Host ('Pemeriksaan lolos: ' + $plansVitae.Count + ' file siap dipasang.') -ForegroundColor Green
Write-Host 'Main, command lain, dan konfigurasi server tetap utuh.' -ForegroundColor Cyan
if ($CheckOnly) { Write-Host 'CheckOnly selesai. Belum ada file yang diubah.' -ForegroundColor Yellow; return }
$backupVitae = Join-Path $rootVitae ('vitae-npc-cerita-backup-' + [DateTime]::Now.ToString('yyyyMMdd-HHmmss-fff') + '-' + [Guid]::NewGuid().ToString('N').Substring(0,6))
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
Write-Host 'Update NPC cerita dan evo terpasang. Backup source lama:' -ForegroundColor Green
Write-Host $backupVitae
Write-Host 'Lanjutkan: .\gradlew.bat clean build' -ForegroundColor Cyan
Write-Host 'Matikan server, ganti JAR hasil build, biarkan folder data lama, lalu nyalakan server.' -ForegroundColor Yellow
Write-Host 'Bantuan: /vnpc help tampilan, /vnpc help ingatan, /vnpc help gerbang, /evo help aftermath.' -ForegroundColor Cyan
```
<!-- VITAE_INSTALLER_END -->
