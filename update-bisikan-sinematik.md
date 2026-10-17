# Vitaemanager — update bisikan sinematik

Update Paper 1.21.4 / Java 21 untuk modul `/vitae bisikan`. File Java lengkap dan installer berada dalam dokumen ini; tidak perlu menyalin potongan kode atau mengubah main secara manual.

## Yang bertambah

- Title/subtitle mendukung warna `&0–&f`, tebal `&l`, miring `&o`, reset `&r`, dan hex `&#55AAFF`.
- Musik latar terpisah dari efek suara tiap dialog. Retakan/teriakan tidak memutus musik; kedua suara hanya dikirim ke pemain penerima.
- Volume dan pitch musik/efek dapat diatur sendiri. Musik bisa vanilla atau ID custom yang sudah terdaftar pada resource pack ItemsAdder.
- Efek mengetik opsional. Teks otomatis diselesaikan cukup awal agar pemain masih sempat membacanya. Fade masuk/keluar dapat diatur.
- Command `dialog` membagi teks panjang per kata menjadi halaman subtitle yang lebih pendek. Judul nama atau ancaman pendek tetap bisa memakai `line add` sebagai title besar.
- Lima template cerita: Lunar, Ryujin, Outsider, Elysia, dan penyamar, berisi dialog yang kamu minta, warna, timing, efek vanilla, dan musik horror vanilla.
- Penerima massal, salin area antar-ID, pengaturan suara/timing semua baris, preview dari nomor tertentu, dan tes suara pribadi.
- Help berwarna dengan jarak atas/bawah, tab completion, petunjuk setelah create/template, serta info durasi dan progres.

Template tidak memasukkan pemain atau area secara otomatis. Setiap POV tetap mempunyai ID dan progres sendiri. `plays 1` berarti seluruh rangkaian sekali per pemain, bukan satu baris saja.

`Vitaemanager.java`, plugin.yml, dependency, NPC, quest, evo, Viti, dan modul lain tidak diganti. Data lama `whispers.yml` tetap dibaca: musik default off, efek mengetik default off, dan progres penerima tetap tersimpan. Edit/template tidak otomatis mereset pemain yang sudah selesai.

## Pasang di proyek IntelliJ

Letakkan **update-bisikan-sinematik.md** sejajar `gradlew.bat`, lalu jalankan PowerShell dari folder proyek:

```powershell
$m=Get-Content -LiteralPath .\update-bisikan-sinematik.md -Raw -Encoding UTF8; $i=[regex]::Match($m,'(?s)<!-- VITAE_INSTALLER_BEGIN -->\r?\n```powershell\r?\n(.*?)\r?\n```\r?\n<!-- VITAE_INSTALLER_END -->'); if(!$i.Success){throw 'Blok installer tidak ditemukan'}; & ([scriptblock]::Create($i.Groups[1].Value)) -ProjectPath (Get-Location).Path
```

Installer memeriksa isi semua file sebelum menulis, membuat backup `vitae-bisikan-backup-...`, dan memulihkan perubahan jika pemasangan gagal. Menjalankan installer lagi pada update yang sama tidak menulis ulang. Jika file lama telah diubah manual di luar versi sumber yang dikenali, installer berhenti sebelum menimpa perubahan itu. Pemeriksaan tanpa memasang tersedia dengan menambahkan `-CheckOnly` di ujung command.

Setelah pemasangan:

```powershell
.\gradlew.bat clean build
```

Matikan server, ganti JAR lama dengan JAR hasil build dalam `build/libs`, **biarkan folder data plugin lama tetap ada**, lalu nyalakan server. Tidak perlu setup ulang NPC, Viti, evo, atau bisikan lama. Jangan memakai `/reload` server untuk mengganti JAR.

## Setup lima POV di area dungeon yang sama

Kerjakan command **satu per satu**, tunggu pesan **Tersimpan** sebelum command setup berikutnya. Command yang ditolak karena masih menyimpan harus diulang setelah penyimpanan selesai. Command dijalankan di dalam game oleh admin/OP dengan permission `vitae.admin.whisper`.

Pakai ID baru di bawah jika ini adegan baru. Menggunakan ID yang sudah ada mempertahankan progresnya. Setiap nick MC harus sedang online atau pernah join dan masih dikenal server; jika belum dikenal, gunakan UUID lengkap. Underscore ditulis biasa: `Ryuuz_`, `Klos3_`, dan `Cleron_`.

### 1. Tandai area, lalu buat Lunar

Berdiri di sudut A area yang dilewati pemain:

```text
/vitae bisikan pos1
```

Berdiri di sudut B yang berseberangan, kemudian:

```text
/vitae bisikan pos2
/vitae bisikan template dungeon_lunar lunar
/vitae bisikan area dungeon_lunar
/vitae bisikan target add dungeon_lunar idansus
/vitae bisikan info dungeon_lunar
/vitae bisikan preview dungeon_lunar
```

Pos1/pos2 memakai **blok tempat admin berdiri**, berbeda dari seleksi NPC yang memakai blok bidikan. Pilih A–B sebagai kubus yang mencakup posisi kaki pemain saat lewat; kedua titik di dunia yang sama. Tidak perlu membuat seleksi ulang untuk POV lain.

Template Lunar berisi:

1. **Lunar**, biru.
2. “Sudah lama kita tidak berbicara”.
3. “Kau pikir, Dungeon kali ini akan sama dengan yang dulu?”.
4. “HAHAHAHAHA”.
5. “JANGAN HARAP”.
6. “KAU BISA SELAMAT DI DUNGEON KALI INI”.
7. “HAHAHAHAHA”.

Ancaman terakhir dipisah agar judul tidak terlalu lebar. Efek `block.wood.break` dengan pitch rendah memberi bunyi retakan; latarnya `music_disc.11`. Setelah preview selesai dan sudah cocok:

```text
/vitae bisikan enable dungeon_lunar
```

Keluar dari area lalu masuk kembali untuk uji trigger. Preview tidak memakai jatah; trigger asli untuk `idansus` memakai jatah setelah seluruh rangkaian selesai.

### 2. Ryujin

```text
/vitae bisikan template dungeon_ryujin ryujin
/vitae bisikan area copy dungeon_ryujin dungeon_lunar
/vitae bisikan target add dungeon_ryujin Ryuuz_
/vitae bisikan preview dungeon_ryujin
/vitae bisikan enable dungeon_ryujin
```

Dialog mengikuti pesanmu: RYUJIN → berhati-hati → sedang tidak memegangku → dungeon berbahaya dan berbeda → merasakan sejak menginjakkan kaki → peringatan kepada rekan Revenant → DAN → jangan mengejar hadiah → cukup bertahan sampai selesai. Pesan yang panjang dibagi menjadi halaman lanjutan, dengan warna tetap terbawa.

### 3. Sebelas Outsider

```text
/vitae bisikan template dungeon_outsider outsider
/vitae bisikan area copy dungeon_outsider dungeon_lunar
/vitae bisikan target add dungeon_outsider kael MuunDI Vick0813 Amertaaa Sirellee Klos3_ snowyuna Arnevalis KaizelL CleonnHachi NotedHere
/vitae bisikan preview dungeon_outsider
/vitae bisikan enable dungeon_outsider
```

Dialog: OUTSIDER!!!!! → KALIAN BUKANLAH MAKHLUK DARI DUNIA INI → PERGI KALIAN → DARI SINI!. Efek teriakan ghast, pitch rendah, musik latar horror. Masing-masing pemain mempunyai jatah sendiri; pemain pertama tidak menghabiskan jatah anggota lain.

| Nama RP | Nick MC |
|---|---|
| kael | kael |
| Bulan | MuunDI |
| Veldrick Vendora | Vick0813 |
| Ethan Yves Ragnars | Amertaaa |
| Vivianne Yvette Ragnars | Sirellee |
| Icaelus Yves Ragnars | Klos3_ |
| Mizuki Lisa | snowyuna |
| Vaelzareth Yvette Ragnars | Arnevalis |
| Vaerys Velior | KaizelL |
| Arthur | CleonnHachi |
| Noted Drenji Sansres | NotedHere |

### 4. Elysia

```text
/vitae bisikan template dungeon_elysia elysia
/vitae bisikan area copy dungeon_elysia dungeon_lunar
/vitae bisikan target add dungeon_elysia jjemina
/vitae bisikan preview dungeon_elysia
/vitae bisikan enable dungeon_elysia
```

Dialogmu dipertahankan: sapaan Elysia, keadaan sekarang, tawa, “Aku bisa tenang karena....”, tersisa di keluarganya, tawa, melihat seseorang yang dikenal, lalu suatu saat muncul di hadapannya. Efek ambient warden dengan pitch rendah memberi suara makhluk yang lebih berat; musik latar tetap horror.

### 5. Kael dan Aron yang menyamar

```text
/vitae bisikan template dungeon_penyamar penyamar
/vitae bisikan area copy dungeon_penyamar dungeon_lunar
/vitae bisikan target add dungeon_penyamar Cleron_ kingprowa
/vitae bisikan preview dungeon_penyamar
/vitae bisikan enable dungeon_penyamar
```

Dialog: “Mau sampai kapan kalian berdua bersembunyi menggunakan wujud itu?”. Teks dibagi menjadi halaman lanjutan. Nick `Cleron_` pada POV ini berbeda dari nick `kael` dalam kelompok Outsider.

## Mengubah tampilan dan suasana

Semua contoh memakai `dungeon_lunar`; ganti ID sesuai POV. Edit menghentikan sesi ID yang sedang berjalan, sehingga sebaiknya atur sebelum pemain menerima adegan.

### Volume, efek suara, dan musik

Atur seluruh dialog yang sudah ada:

```text
/vitae bisikan sound dungeon_lunar all minecraft:block.wood.break 0.65 0.65
/vitae bisikan music dungeon_lunar minecraft:music_disc.11 0.5 0.85 off
```

Volume 0–1; pitch 0.5–2. Angka volume kedua command berdiri sendiri. `all` hanya mengubah baris yang sudah ada, jadi gunakan lagi setelah menambahkan dialog baru bila ingin efek yang sama.

Coba efek lain secara pribadi:

```text
/vitae bisikan soundtest minecraft:entity.ghast.scream 0.6 0.65
/vitae bisikan stop
/vitae bisikan soundtest minecraft:entity.warden.ambient 0.5 0.65
/vitae bisikan stop
```

Untuk mencoba latar lain, atur lalu preview:

```text
/vitae bisikan music dungeon_lunar minecraft:music_disc.13 0.45 0.85 off
/vitae bisikan preview dungeon_lunar
```

Efek memakai kategori client **Voice/Speech**; latar memakai **Music**. Pemain tetap perlu menyalakan Master Volume dan slider kategori tersebut. Sound vanilla tidak menjadi rekaman ucapan kalimat: pesan cerita dibaca pada layar, sementara suara mengiringi suasananya.

Custom ItemsAdder: gunakan key yang benar dari `sounds.json`/konfigurasi sound resource pack, contoh:

```text
/vitae bisikan music dungeon_lunar vitae:bisikan_lunar 0.5 1 off
```

`vitae:bisikan_lunar` hanya contoh; ganti dengan key milikmu. Plugin tidak membaca file MP3/OGG dari folder data Vitaemanager. File custom tetap disediakan oleh ItemsAdder/resource pack yang sudah diterima pemain.

Musik dimulai pada dialog pertama dan tidak dimulai ulang setiap pergantian dialog. `off` pada argumen terakhir berarti tidak mengulang otomatis. Jika rekaman lebih pendek daripada adegan, musik tetap selesai sesuai panjang rekaman. Untuk mengulang rekaman 12 detik pada pitch 1, misalnya:

```text
/vitae bisikan music dungeon_lunar vitae:bisikan_lunar 0.5 1 12
```

Setiap ulang menghentikan putaran sebelumnya agar tidak bertumpuk. Pitch mengubah durasi audio; sesuaikan interval dengan panjang suara yang terdengar. Musik dihentikan saat rangkaian selesai, stop, mati, pindah dunia, disconnect, atau plugin dimatikan.

Matikan satu efek/baris atau seluruh latar:

```text
/vitae bisikan sound dungeon_lunar 4 off
/vitae bisikan music dungeon_lunar off
```

### Mengetik, fade, dan kecepatan adegan

```text
/vitae bisikan typing dungeon_lunar on 1
/vitae bisikan fade dungeon_lunar 0.25 0.5
/vitae bisikan pace dungeon_lunar 5 1.5
/vitae bisikan preview dungeon_lunar
```

Typing 1–8 karakter/tick. Jika waktu pendek dan kalimat panjang, kecepatan efektif meningkat agar seluruh teks selesai dalam sekitar separuh waktu tampil. Fade masuk saat mengetik memakai 0 agar tidak berkedip; fade keluar tetap digunakan. Fade masuk biasa dipakai saat typing off.

`pace` mengganti waktu **semua baris yang sudah ada**. Untuk mengubah satu baris sambil mempertahankan efek suaranya:

```text
/vitae bisikan line set dungeon_lunar 1 4 1 &9&lLunar | -
```

Minecraft memakai ukuran font title/subtitle client bawaan. Pemecahan kalimat membantu keterbacaan; judul sebaiknya singkat karena lebar layar, GUI scale, dan resource pack tiap pemain dapat berbeda.

### Membuat cerita sendiri

```text
/vitae bisikan create lorong_baru
/vitae bisikan area copy lorong_baru dungeon_lunar
/vitae bisikan target add lorong_baru idansus
/vitae bisikan line add lorong_baru 3 1 &9Lunar | -
/vitae bisikan dialog lorong_baru 5 1 &cKalimat panjangmu akan dipisah per kata menjadi beberapa halaman subtitle agar lebih mudah dibaca.
/vitae bisikan sound lorong_baru all minecraft:block.wood.break 0.5 0.65
/vitae bisikan music lorong_baru minecraft:music_disc.11 0.4 0.85 off
/vitae bisikan typing lorong_baru on 1
/vitae bisikan preview lorong_baru
/vitae bisikan enable lorong_baru
```

`line add`: title besar sebelum `|`, subtitle setelahnya; `-` berarti kosong. `dialog`: seluruh teks menjadi subtitle, maksimal sekitar 56 karakter terlihat per halaman, warna terbawa. Teks panjang belum pernah ditambahkan tidak diganti oleh `dialog`; command tersebut menambahkan baris baru.

Untuk mengganti seluruh cerita manual pada ID yang sudah ada:

```text
/vitae bisikan line clear lorong_baru confirm
```

ID otomatis OFF, seluruh dialog dihapus, tetapi area, penerima, musik, tampilan, dan progres tetap. Tambahkan dialog baru lalu enable. Template juga mengganti cerita pada ID yang dipilih, sehingga gunakan ID baru untuk adegan lain agar dialog lama tetap ada.

## Pemeriksaan dan uji ulang

```text
/vitae bisikan help
/vitae bisikan help music
/vitae bisikan info dungeon_lunar
/vitae bisikan preview dungeon_lunar 5
/vitae bisikan stop
```

Preview mulai dari nomor yang dipilih sampai akhir dan memainkan musik latar dari awal. Tidak ada perubahan progres. Jika `idansus` sudah menghabiskan jatah melalui trigger asli dan kamu ingin menguji trigger lagi, reset **pemain itu saja**:

```text
/vitae bisikan reset dungeon_lunar idansus
```

Tunggu Tersimpan, keluar area, lalu masuk kembali. Jika kamu ingin seluruh penerima mempunyai kesempatan baru, ada `reset <id> all`; gunakan sengaja karena itu memang menghapus progres ID tersebut. Matikan adegan sementara dengan `disable <id>`, tanpa menghapusnya.

Jika trigger tidak muncul: lihat `info`, pastikan ON, nick/UUID benar, seluruh posisi kaki pemain berada dalam area, dan progres masih di bawah batas. Area boleh sama antar-POV; hanya pemain yang terdaftar menerima masing-masing cerita. Jika pemain terdaftar pada beberapa ID yang terpicu bersamaan, rangkaian diputar bergantian, bukan title bertumpuk.

## Validasi pembaruan

- `clean build` berhasil dengan 179 tes proyek, termasuk 11 tes baru warna, halaman, template, data lama, musik, dan progres.
- 11 uji tambahan memakai mock API untuk suara pribadi, pemisahan musik/efek, interval ulang, timing ketikan, preview tanpa jatah, command massal, dan penolakan input invalid.
- 10 skenario installer lolos: CheckOnly, format Windows BOM/CRLF, backup, pemasangan ulang, source berbeda/hilang/read-only, isi installer rusak, main tanpa integrasi, pemulihan setelah kegagalan penulisan, dan versi source dari paket sebelumnya.
- Main, NPC, evo, resource, dan dependency tidak berubah; pemasangan tidak menyentuh data server.


Pengujian tampilan dan rasa seram di client Minecraft tetap dilakukan di server setelah JAR dipasang. Pengujian otomatis memeriksa kode, penyimpanan, timing, penerima, dan pemanggilan API; tidak menggantikan mendengar audio serta melihat layar pemain.

## File yang dipasang

- `src/main/java/com/bangzachery/vitae/vitaemanager/command/WhisperCommand.java`
- `src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperAudio.java`
- `src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperDefinition.java`
- `src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperLine.java`
- `src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperMusic.java`
- `src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperScreen.java`
- `src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperService.java`
- `src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperStore.java`
- `src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperTemplates.java`
- `src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperText.java`
- `src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperVisual.java`
- `src/test/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperCinematicStoreTest.java`
- `src/test/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperCinematicTest.java`

## Installer lengkap

Java lengkap setiap file berada pada properti `Source` di bawah. Jalankan command pemasangan di atas; tidak perlu menyalin Java secara manual.

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
if ($mainTextVitae -notmatch 'new\s+(?:com\.bangzachery\.vitae\.vitaemanager\.whisper\.)?WhisperService\s*\(' -or
    $mainTextVitae -notmatch 'registerEvents\s*\(\s*whispers\s*,\s*this\s*\)' -or
    $mainTextVitae -notmatch 'whispers\.start\s*\(' -or $mainTextVitae -notmatch 'whispers\.close\s*\(') {
    throw 'Integrasi bisikan pada main belum lengkap. Tidak ada file yang diubah.'
}
$commandVitae = Join-Path $rootVitae 'src/main/java/com/bangzachery/vitae/vitaemanager/command/VitaeCommand.java'
if (-not (Test-Path -LiteralPath $commandVitae -PathType Leaf) -or
    [IO.File]::ReadAllText($commandVitae, $utf8Vitae) -notmatch 'whispers\.execute\s*\(') {
    throw 'Routing /vitae bisikan belum ada. Tidak ada file yang diubah.'
}
$filesVitae = @(
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/command/WhisperCommand.java'
        OriginalHashes = @('7a9994f9022a8985ec3d441b6b6c7d07de96586b56176e5551cc2b05b5453f86', '568e9260a6cbe7f0d3cbfe72deb47ef193ec4b3cbdf957cb5be4f1f40377b10c')
        Hash = '971771950082cfc5b835354b0ed826324afa37c3fb0abef49031d571a3f9e770'
        Source = @'
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
    private static final List<String> ACTIONS = List.of("help", "pos1", "pos2", "create", "template", "area", "target", "line",
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
            default -> "Setup cepat: pos1 → pos2 → template <id> lunar → area <id> → target add <id> <nick-MC> → preview <id> → enable <id>.\n"
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
'@
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperAudio.java'
        OriginalHashes = @()
        Hash = '31e585a368a46eb320e28e1092253d77ca307957ad6222b14db9cb6ffc70be05'
        Source = @'
package com.bangzachery.vitae.vitaemanager.whisper;

import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import java.util.*;

/** Private entity-bound audio, with separate voice and music ownership. */
public final class WhisperAudio {
    private record Track(UUID ticket, WhisperMusic music, long nextLoop) {}
    private final Map<UUID, String> voices = new HashMap<>();
    private final Map<UUID, Track> tracks = new HashMap<>();
    public void begin(Player player, WhisperPlayback.Run run, long tick) {
        Track track = tracks.get(player.getUniqueId());
        if (track != null && track.ticket().equals(run.ticket())) return;
        stopMusic(player);
        if (run.definition().music() == null) return;
        WhisperMusic music = run.definition().music(); playMusic(player, music);
        tracks.put(player.getUniqueId(), new Track(run.ticket(), music, music.repeatTicks() > 0 ? tick + music.repeatTicks() : 0));
    }
    public void voice(Player player, WhisperLine.Audio sound) {
        stopVoice(player);
        if (sound == null) return;
        player.playSound(player, sound.key(), SoundCategory.VOICE, sound.volume(), sound.pitch());
        voices.put(player.getUniqueId(), sound.key());
    }
    public void tick(Player player, WhisperPlayback.Run run, long tick) {
        Track track = tracks.get(player.getUniqueId());
        if (track == null || !track.ticket().equals(run.ticket()) || track.nextLoop() == 0 || tick < track.nextLoop()) return;
        player.stopSound(track.music().sound().key(), SoundCategory.MUSIC);
        playMusic(player, track.music());
        tracks.put(player.getUniqueId(), new Track(track.ticket(), track.music(), tick + track.music().repeatTicks()));
    }
    private void playMusic(Player player, WhisperMusic music) {
        var sound = music.sound(); player.playSound(player, sound.key(), SoundCategory.MUSIC, sound.volume(), sound.pitch());
    }
    private void stopVoice(Player player) {
        String key = voices.remove(player.getUniqueId()); if (key != null) player.stopSound(key, SoundCategory.VOICE);
    }
    private void stopMusic(Player player) {
        Track track = tracks.remove(player.getUniqueId()); if (track != null) player.stopSound(track.music().sound().key(), SoundCategory.MUSIC);
    }
    public void clear(Player player) { stopVoice(player); stopMusic(player); }
    public void forget(UUID player) { voices.remove(player); tracks.remove(player); }
    public Set<UUID> players() { Set<UUID> players = new HashSet<>(voices.keySet()); players.addAll(tracks.keySet()); return players; }
}
'@
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperDefinition.java'
        OriginalHashes = @('7d7ddeaaf3b47094d8f6977d4c7749d63e027f0e80f87d8e76b792ccc67027a0')
        Hash = 'c8c256796a47c0ac9b3a3d4d9dccc1e710dcd617a29d20a4feac9024be69abdb'
        Source = @'
package com.bangzachery.vitae.vitaemanager.whisper;

import java.util.*;

public record WhisperDefinition(String id, WhisperArea area, Set<UUID> targets, List<WhisperLine> lines,
                                int limit, boolean enabled, WhisperMusic music, WhisperVisual visual) {
    public WhisperDefinition {
        requireId(id);
        targets = Set.copyOf(targets);
        lines = List.copyOf(lines);
        visual = Objects.requireNonNullElse(visual, WhisperVisual.defaults());
        if (targets.size() > 1000 || lines.size() > 100 || limit < 1 || limit > 100)
            throw new IllegalArgumentException("Maksimum 1000 penerima, 100 dialog dan batas ulang 1–100.");
        if (enabled && (area == null || targets.isEmpty() || lines.isEmpty()))
            throw new IllegalArgumentException("Lengkapi area, penerima dan dialog sebelum enable.");
    }
    public WhisperDefinition(String id, WhisperArea area, Set<UUID> targets, List<WhisperLine> lines, int limit, boolean enabled) {
        this(id, area, targets, lines, limit, enabled, null, WhisperVisual.defaults());
    }

    public static void requireId(String id) {
        if (id == null || !id.matches("[a-z0-9][a-z0-9_-]{0,47}"))
            throw new IllegalArgumentException("ID harus 1–48 huruf kecil/angka/underscore/tanda minus.");
    }
    public static WhisperDefinition draft(String id) { return new WhisperDefinition(id, null, Set.of(), List.of(), 1, false); }
    public WhisperDefinition withArea(WhisperArea value) { return new WhisperDefinition(id, value, targets, lines, limit, enabled, music, visual); }
    public WhisperDefinition withTargets(Set<UUID> value) { return new WhisperDefinition(id, area, value, lines, limit, enabled, music, visual); }
    public WhisperDefinition withLines(List<WhisperLine> value) { return new WhisperDefinition(id, area, targets, value, limit, enabled, music, visual); }
    public WhisperDefinition withLimit(int value) { return new WhisperDefinition(id, area, targets, lines, value, enabled, music, visual); }
    public WhisperDefinition withEnabled(boolean value) { return new WhisperDefinition(id, area, targets, lines, limit, value, music, visual); }
    public WhisperDefinition withMusic(WhisperMusic value) { return new WhisperDefinition(id, area, targets, lines, limit, enabled, value, visual); }
    public WhisperDefinition withVisual(WhisperVisual value) { return new WhisperDefinition(id, area, targets, lines, limit, enabled, music, value); }
    public long durationTicks() {
        long total = 0;
        for (int i = 0; i < lines.size(); i++) total += lines.get(i).durationTicks() + (i + 1 < lines.size() ? lines.get(i).gapTicks() : 0);
        return total;
    }
}
'@
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperLine.java'
        OriginalHashes = @('8031e086989bb8f300bf85064fa2cd012593d08b885e5c6ef2aaeab52fd5b459')
        Hash = 'e050e7a39ad0d0a47ae6aef31a3c70ecee23fcba52c1073cf982f03800770ecb'
        Source = @'
package com.bangzachery.vitae.vitaemanager.whisper;

/** Text supports legacy colours, with no MiniMessage events. Timing is in server ticks. */
public record WhisperLine(String title, String subtitle, int durationTicks, int gapTicks, Audio audio) {
    public WhisperLine {
        title = text(title);
        subtitle = text(subtitle);
        if (WhisperText.plain(title).isBlank() && WhisperText.plain(subtitle).isBlank()) throw new IllegalArgumentException("Title dan subtitle tidak boleh keduanya kosong.");
        if (durationTicks < 20 || durationTicks > 1200 || gapTicks < 0 || gapTicks > 1200)
            throw new IllegalArgumentException("Durasi 1–60 detik; jeda 0–60 detik.");
    }

    private static String text(String value) {
        if (value == null || value.codePointCount(0, value.length()) > 160
                || value.codePoints().anyMatch(c -> Character.isISOControl(c) || c == 0x2028 || c == 0x2029))
            throw new IllegalArgumentException("Teks maksimum 160 karakter dan tidak boleh mengandung baris baru/kontrol.");
        return value;
    }

    public WhisperLine withAudio(Audio value) { return new WhisperLine(title, subtitle, durationTicks, gapTicks, value); }

    public record Audio(String key, float volume, float pitch) {
        public Audio {
            if (key == null || !key.matches("[a-z0-9._-]+:[a-z0-9/._-]+"))
                throw new IllegalArgumentException("Sound gunakan ID seperti minecraft:block.note_block.hat atau namespace:suara.");
            if (!Float.isFinite(volume) || volume < 0 || volume > 1 || !Float.isFinite(pitch) || pitch < 0.5f || pitch > 2)
                throw new IllegalArgumentException("Volume 0–1; pitch 0.5–2.");
        }
    }
}
'@
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperMusic.java'
        OriginalHashes = @()
        Hash = '21e48cdaa6d1de2592b67222281095c76523da45852173cf30e0981cf33a7d74'
        Source = @'
package com.bangzachery.vitae.vitaemanager.whisper;

import java.util.Objects;

/** Loop interval is explicit; zero plays the clip once while the sequence continues. */
public record WhisperMusic(WhisperLine.Audio sound, int repeatTicks) {
    public WhisperMusic {
        Objects.requireNonNull(sound);
        if (repeatTicks < 0 || repeatTicks > 12000 || (repeatTicks > 0 && repeatTicks < 20))
            throw new IllegalArgumentException("Loop musik: off atau 1–600 detik.");
    }
}
'@
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperScreen.java'
        OriginalHashes = @()
        Hash = 'bda0cb32c71ca5edecae78e80d868032c7241c81ac456c4d0503791040103c21'
        Source = @'
package com.bangzachery.vitae.vitaemanager.whisper;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import java.time.Duration;
import java.util.*;

/** Optional typewriter refreshes only while text is growing, at most once every two ticks. */
public final class WhisperScreen {
    private static final class Frame {
        final WhisperLine line;
        final WhisperVisual visual;
        final long start;
        final int titleLength, length, speed;
        int shown = -1;
        Frame(WhisperLine line, WhisperVisual visual, long start) {
            this.line = line; this.visual = visual; this.start = start;
            titleLength = WhisperText.length(line.title()); length = titleLength + WhisperText.length(line.subtitle());
            speed = Math.max(visual.charactersPerTick(), (length + Math.max(1, line.durationTicks() / 2) - 1) / Math.max(1, line.durationTicks() / 2));
        }
    }
    private final Map<UUID, Frame> frames = new HashMap<>();
    public void start(Player player, WhisperLine line, WhisperVisual visual, long tick) {
        Frame frame = new Frame(line, visual, tick); frames.put(player.getUniqueId(), frame); render(player, frame, tick);
    }
    public void tick(Player player, long tick) {
        Frame frame = frames.get(player.getUniqueId()); if (frame == null) return;
        if (tick - frame.start >= frame.line.durationTicks()) { frames.remove(player.getUniqueId()); return; }
        if (frame.visual.typing() && frame.shown < frame.length && (tick - frame.start) % 2 == 0) render(player, frame, tick);
    }
    private void render(Player player, Frame frame, long tick) {
        int age = (int) (tick - frame.start);
        int shown = frame.visual.typing() ? Math.min(frame.length, (age + 1) * frame.speed) : frame.length;
        if (shown == frame.shown) return;
        frame.shown = shown;
        int left = frame.line.durationTicks() - age;
        int fadeIn = frame.visual.typing() ? 0 : Math.min(frame.visual.fadeInTicks(), left / 3);
        int fadeOut = Math.min(frame.visual.fadeOutTicks(), left / 3);
        var times = Title.Times.times(Duration.ofMillis(fadeIn * 50L), Duration.ofMillis((left - fadeIn - fadeOut) * 50L), Duration.ofMillis(fadeOut * 50L));
        player.showTitle(Title.title(WhisperText.component(WhisperText.prefix(frame.line.title(), shown), NamedTextColor.WHITE),
                WhisperText.component(WhisperText.prefix(frame.line.subtitle(), Math.max(0, shown - frame.titleLength)), NamedTextColor.GRAY), times));
    }
    public void clear(Player player) { if (frames.remove(player.getUniqueId()) != null) player.clearTitle(); }
    public void forget(UUID player) { frames.remove(player); }
    public Set<UUID> players() { return Set.copyOf(frames.keySet()); }
}
'@
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperService.java'
        OriginalHashes = @('5b6ad82116b80fc5eef13944b8e0c3bebe4d5c65e58361fe3676eb506a61d9c9')
        Hash = 'fb086397eac289cf069f9ea9613c9cfd25898b3641affd6da0bc8e5698e19d9c'
        Source = @'
package com.bangzachery.vitae.vitaemanager.whisper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.Level;

public final class WhisperService implements Listener, AutoCloseable {
    private final JavaPlugin plugin;
    private final WhisperStore store;
    private final WhisperPlayback playback = new WhisperPlayback();
    private final ExecutorService writer;
    private WhisperState current;
    private WhisperState persisted; // Used only by the one writer after construction.
    private Map<UUID, List<WhisperDefinition>> byTarget = Map.of();
    private final Map<UUID, Point[]> selections = new HashMap<>();
    private final WhisperAudio audio = new WhisperAudio();
    private final WhisperScreen screen = new WhisperScreen();
    public record Point(UUID world, int x, int y, int z) { }
    private BukkitTask task;
    private long tick;
    private int pendingWrites;
    private volatile boolean closed;

    public WhisperService(JavaPlugin plugin, WhisperStore store, WhisperState initial) {
        this.plugin = plugin; this.store = store; current = initial; persisted = initial;
        writer = Executors.newSingleThreadExecutor(action -> new Thread(action, "Vitae-Whisper-Writer"));
        index();
    }
    public WhisperState current() { return current; }
    public void validateAudio(WhisperLine.Audio audio) { store.validateAudio(audio); }
    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::advance, 1, 1);
        // Also handles players online when the plugin is enabled without restarting the server.
        for (Player player : Bukkit.getOnlinePlayers()) enterAt(player);
    }
    public void select(Player player, boolean first) {
        Location location = player.getLocation();
        Point[] points = selections.computeIfAbsent(player.getUniqueId(), ignored -> new Point[2]);
        Point point = new Point(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        if (first && points[1] != null && !points[1].world().equals(point.world())) points[1] = null;
        if (!first && points[0] != null && !points[0].world().equals(point.world()))
            throw new IllegalArgumentException("A dan B harus dalam dunia yang sama. Mulai seleksi baru dengan pos1 di dunia ini.");
        points[first ? 0 : 1] = point;
    }
    public WhisperArea selection(Player player) {
        Point[] points = selections.get(player.getUniqueId());
        if (points == null || points[0] == null || points[1] == null)
            throw new IllegalArgumentException("Tentukan /vitae bisikan pos1 dan pos2 dulu; titik adalah blok tempat kamu berdiri.");
        Point a = points[0], b = points[1];
        return WhisperArea.between(a.world(), a.x(), a.y(), a.z(), b.x(), b.y(), b.z());
    }

    /** Validate on the main thread before stopping sessions or submitting disk work. */
    public void change(String affectedId, UnaryOperator<WhisperState> operation, Consumer<String> result) {
        if (!ready(result)) return;
        operation.apply(current);
        cancel(affectedId);
        submit(operation, () -> result.accept("Tersimpan. Edit/reset menghentikan sesi area ini; masuk kembali untuk memicu."),
                () -> result.accept("Gagal menyimpan; setup/progres sebelumnya dipertahankan. Periksa log server."));
    }
    public void reload(Consumer<String> result) {
        if (!ready(result)) return;
        pendingWrites++;
        writer.execute(() -> {
            try {
                WhisperState loaded = store.load(); persisted = loaded;
                post(() -> { clearAll(); current = loaded; index(); pendingWrites--; result.accept("whispers.yml dimuat ulang."); });
            } catch (Exception exception) {
                failure(exception);
                post(() -> { pendingWrites--; result.accept("Reload gagal; bisikan aktif dipertahankan. Periksa whispers.yml dan log server."); });
            }
        });
    }
    public void preview(Player player, String id) {
        preview(player, id, 1);
    }
    public void preview(Player player, String id, int firstLine) {
        var definition = current.require(id);
        if (firstLine < 1 || firstLine > definition.lines().size()) throw new IllegalArgumentException("Nomor dialog tidak ada; lihat info.");
        var selected = definition.withLines(definition.lines().subList(firstLine - 1, definition.lines().size()));
        if (closed || !playback.enqueue(player.getUniqueId(), new WhisperPlayback.Run(selected, true)))
            throw new IllegalArgumentException("Dialog kosong, preview/area ini sudah antre, atau antrean penuh (32).");
    }
    public void soundTest(Player player, WhisperLine.Audio sound) {
        validateAudio(sound);
        if (closed || playback.head(player.getUniqueId()) != null) throw new IllegalArgumentException("Hentikan bisikan/preview dengan stop sebelum tes suara.");
        audio.voice(player, sound);
    }
    public void stop(Player player) { clear(player); }

    private boolean ready(Consumer<String> result) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Bisikan harus dipanggil di server thread");
        if (closed || pendingWrites > 0) {
            result.accept("Masih menyimpan bisikan/progres. Tunggu sebentar, lalu ulangi command."); return false;
        }
        return true;
    }
    private void submit(UnaryOperator<WhisperState> operation, Runnable success, Runnable failed) {
        pendingWrites++;
        writer.execute(() -> {
            try {
                WhisperState candidate = operation.apply(persisted);
                store.save(candidate); persisted = candidate;
                post(() -> {
                    boolean definitionsChanged = !current.definitions().equals(candidate.definitions());
                    current = candidate;
                    if (definitionsChanged) index();
                    pendingWrites--; success.run();
                });
            } catch (Exception exception) {
                failure(exception); post(() -> { pendingWrites--; failed.run(); });
            }
        });
    }
    private void failure(Exception exception) { plugin.getLogger().log(Level.WARNING, "Operasi whispers.yml gagal; snapshot terakhir dipertahankan.", exception); }
    private void post(Runnable action) {
        if (closed) return;
        try { Bukkit.getScheduler().runTask(plugin, () -> { if (!closed) action.run(); }); }
        catch (IllegalPluginAccessException ignored) { }
    }
    private void index() {
        var index = new HashMap<UUID, List<WhisperDefinition>>();
        for (var definition : current.definitions().values().stream().sorted(Comparator.comparing(WhisperDefinition::id)).toList()) {
            if (definition.enabled()) for (UUID target : definition.targets())
                index.computeIfAbsent(target, ignored -> new ArrayList<>()).add(definition);
        }
        var immutable = new HashMap<UUID, List<WhisperDefinition>>();
        index.forEach((id, list) -> immutable.put(id, List.copyOf(list)));
        byTarget = Map.copyOf(immutable);
    }
    private void trigger(Player player, Location from, Location to, boolean teleport) {
        if (closed || player.isDead() || to.getWorld() == null) return;
        UUID uuid = player.getUniqueId(), world = to.getWorld().getUID();
        for (var definition : byTarget.getOrDefault(uuid, List.of())) {
            WhisperArea area = definition.area();
            boolean entered;
            if (from == null || from.getWorld() == null || !from.getWorld().getUID().equals(world))
                entered = area.contains(world, to.getX(), to.getY(), to.getZ());
            else if (teleport)
                entered = area.contains(world, to.getX(), to.getY(), to.getZ())
                        && !area.contains(world, from.getX(), from.getY(), from.getZ());
            else entered = area.crossed(world, from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ());
            if (entered && current.eligible(definition, uuid))
                playback.enqueue(uuid, new WhisperPlayback.Run(definition, false));
        }
    }
    private void enterAt(Player player) { if (player.isOnline()) trigger(player, null, player.getLocation(), true); }
    private void advance() {
        tick++;
        for (UUID id : playback.players()) {
            Player player = Bukkit.getPlayer(id);
            if (player == null) { playback.clear(id); audio.forget(id); screen.forget(id); continue; }
            if (!player.isOnline() || player.isDead()) { clear(player); continue; }
            WhisperPlayback.Run run = playback.head(id);
            if (!run.preview() && (!run.definition().equals(current.definitions().get(run.definition().id()))
                    || !current.eligible(run.definition(), id) || !run.definition().area().world().equals(player.getWorld().getUID()))) {
                screen.clear(player); audio.clear(player);
                playback.acknowledge(id, run); continue;
            }
            WhisperPlayback.Step step = playback.advance(id, tick);
            if (step != null && step.complete()) {
                screen.clear(player); audio.clear(player);
                if (run.preview()) playback.acknowledge(id, run);
                else submit(state -> state.finish(run.definition(), id), () -> playback.acknowledge(id, run), () -> {
                    playback.acknowledge(id, run);
                    if (player.isOnline()) player.sendMessage(Component.text("[Vitae] Progres bisikan belum tersimpan; hubungi admin.", NamedTextColor.RED));
                });
            } else {
                audio.tick(player, run, tick); screen.tick(player, tick);
                if (step == null) continue;
                var line = step.line();
                audio.begin(player, run, tick);
                screen.start(player, line, run.definition().visual(), tick);
                audio.voice(player, line.audio());
            }
        }
    }
    private void cancel(String id) {
        for (UUID uuid : playback.players()) {
            boolean displayed = playback.cancel(uuid, id);
            Player player = Bukkit.getPlayer(uuid);
            if (displayed && player != null) { screen.clear(player); audio.clear(player); }
        }
    }
    private void clear(Player player) {
        screen.clear(player);
        playback.clear(player.getUniqueId());
        audio.clear(player);
    }
    private void clearAll() {
        Set<UUID> players = new HashSet<>(playback.players()); players.addAll(audio.players()); players.addAll(screen.players());
        for (UUID id : players) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) clear(player); else { playback.clear(id); audio.forget(id); screen.forget(id); }
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void move(PlayerMoveEvent event) {
        if (!(event instanceof PlayerTeleportEvent) && event.hasChangedPosition())
            trigger(event.getPlayer(), event.getFrom(), event.getTo(), false);
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void teleport(PlayerTeleportEvent event) {
        // Teleports do not trigger areas lying between the source and destination.
        trigger(event.getPlayer(), event.getFrom(), event.getTo(), true);
    }
    @EventHandler public void join(PlayerJoinEvent event) { Bukkit.getScheduler().runTask(plugin, () -> enterAt(event.getPlayer())); }
    @EventHandler public void respawn(PlayerRespawnEvent event) { Bukkit.getScheduler().runTask(plugin, () -> enterAt(event.getPlayer())); }
    @EventHandler public void quit(PlayerQuitEvent event) { clear(event.getPlayer()); selections.remove(event.getPlayer().getUniqueId()); }
    @EventHandler public void death(PlayerDeathEvent event) { clear(event.getEntity()); }
    @EventHandler public void world(PlayerChangedWorldEvent event) {
        clear(event.getPlayer());
        Bukkit.getScheduler().runTask(plugin, () -> enterAt(event.getPlayer()));
    }
    @Override public void close() {
        closed = true;
        if (task != null) task.cancel();
        clearAll(); selections.clear(); writer.shutdown();
        try {
            if (!writer.awaitTermination(10, TimeUnit.SECONDS)) plugin.getLogger().warning("Writer bisikan belum selesai setelah 10 detik.");
        } catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
    }
}
'@
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperStore.java'
        OriginalHashes = @('69e08da343a7c616200d5ae26de760371b13b5c7bef78a46c695eeb23460c85e')
        Hash = '99433bb101195b1da0fd31df3cab0271ad0b76430cb820f93db8fac29cc0f428'
        Source = @'
package com.bangzachery.vitae.vitaemanager.whisper;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Strict owned schema, one serial writer; never rewrites malformed or missing previously loaded files. */
public final class WhisperStore {
    private final Path file;
    private final Set<String> vanillaSounds;
    public WhisperStore(Path file, Set<String> vanillaSounds) {
        this.file = file.toAbsolutePath(); this.vanillaSounds = Set.copyOf(vanillaSounds);
    }
    public WhisperState initialize() throws IOException, InvalidConfigurationException {
        if (Files.notExists(file)) save(WhisperState.empty());
        return load();
    }
    public void validateAudio(WhisperLine.Audio audio) {
        if (audio != null && audio.key().startsWith("minecraft:") && !vanillaSounds.contains(audio.key()))
            throw new IllegalArgumentException("Sound vanilla tidak tersedia di Paper 1.21.4: " + audio.key());
    }
    public WhisperState load() throws IOException, InvalidConfigurationException {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        var yaml = new YamlConfiguration(); yaml.loadFromString(source);
        if (integer(yaml, "state-version") != 1) throw invalid("state-version harus integer 1");
        ConfigurationSection entries = section(yaml, "whispers");
        Map<String, WhisperDefinition> definitions = new HashMap<>();
        Map<String, Map<UUID, Integer>> progress = new HashMap<>();
        for (String id : entries.getKeys(false)) {
            WhisperDefinition.requireId(id);
            ConfigurationSection entry = section(entries, id);
            Object flag = entry.get("enabled");
            if (!(flag instanceof Boolean enabled)) throw invalid(id + ".enabled harus boolean");
            WhisperArea area = null;
            if (entry.isSet("area")) {
                var region = section(entry, "area");
                UUID world = uuid(string(region, "world"));
                List<Integer> min = coordinates(region.get("min")), max = coordinates(region.get("max"));
                area = new WhisperArea(world, min.get(0), min.get(1), min.get(2), max.get(0), max.get(1), max.get(2));
            }
            Set<UUID> targets = new HashSet<>();
            Object targetList = entry.get("targets");
            if (!(targetList instanceof List<?> list)) throw invalid(id + ".targets harus list UUID");
            for (Object item : list) {
                if (!(item instanceof String name) || !targets.add(uuid(name))) throw invalid(id + ": penerima tidak valid/duplikat");
            }
            Object rawLines = entry.get("lines");
            if (!(rawLines instanceof List<?> listLines)) throw invalid(id + ".lines harus list dialog");
            List<WhisperLine> lines = new ArrayList<>();
            for (Object raw : listLines) {
                if (!(raw instanceof Map<?, ?> map)) throw invalid(id + ": dialog harus map");
                var line = new YamlConfiguration();
                for (var value : map.entrySet()) {
                    if (!(value.getKey() instanceof String key)) throw invalid(id + ": key dialog harus teks");
                    if (value.getValue() instanceof Map<?, ?> nested) {
                        var child = line.createSection(key);
                        for (var field : nested.entrySet()) {
                            if (!(field.getKey() instanceof String name)) throw invalid(id + ": key sound harus teks");
                            child.set(name, field.getValue());
                        }
                    } else line.set(key, value.getValue());
                }
                WhisperLine.Audio audio = null;
                if (line.isSet("sound")) {
                    var sound = section(line, "sound");
                    audio = new WhisperLine.Audio(string(sound, "key"), decimal(sound, "volume"), decimal(sound, "pitch"));
                    validateAudio(audio);
                }
                lines.add(new WhisperLine(string(line, "title"), string(line, "subtitle"),
                        integer(line, "duration-ticks"), integer(line, "gap-ticks"), audio));
            }
            WhisperMusic music = null;
            if (entry.isSet("music")) {
                var sound = section(entry, "music");
                var audio = new WhisperLine.Audio(string(sound, "key"), decimal(sound, "volume"), decimal(sound, "pitch"));
                validateAudio(audio); music = new WhisperMusic(audio, integer(sound, "repeat-ticks"));
            }
            WhisperVisual visual = WhisperVisual.defaults();
            if (entry.isSet("visual")) {
                var settings = section(entry, "visual");
                if (!(settings.get("typing") instanceof Boolean typing)) throw invalid(id + ".visual.typing harus boolean");
                visual = new WhisperVisual(typing, integer(settings, "characters-per-tick"), integer(settings, "fade-in-ticks"), integer(settings, "fade-out-ticks"));
            }
            definitions.put(id, new WhisperDefinition(id, area, targets, lines, integer(entry, "plays"), enabled, music, visual));
            var counts = section(entry, "completed");
            Map<UUID, Integer> completed = new HashMap<>();
            for (String player : counts.getKeys(false)) {
                if (completed.put(uuid(player), integer(counts, player)) != null) throw invalid("Progres UUID duplikat");
            }
            progress.put(id, completed);
        }
        return new WhisperState(definitions, progress);
    }

    public void save(WhisperState state) throws IOException {
        var yaml = new YamlConfiguration();
        yaml.options().setHeader(List.of("Vitae Bisikan Area — /vitae bisikan help", "Progres 'completed' adalah jumlah rangkaian selesai per UUID.",
                "Sound minecraft: memakai vanilla; namespace custom memerlukan resource pack pemain."));
        yaml.set("state-version", 1);
        var entries = yaml.createSection("whispers");
        for (String id : state.definitions().keySet().stream().sorted().toList()) {
            var definition = state.require(id);
            var entry = entries.createSection(id);
            entry.set("enabled", definition.enabled()); entry.set("plays", definition.limit());
            if (definition.music() != null) {
                validateAudio(definition.music().sound());
                var sound = entry.createSection("music"); var audio = definition.music().sound();
                sound.set("key", audio.key()); sound.set("volume", audio.volume()); sound.set("pitch", audio.pitch());
                sound.set("repeat-ticks", definition.music().repeatTicks());
            }
            var visual = entry.createSection("visual");
            visual.set("typing", definition.visual().typing()); visual.set("characters-per-tick", definition.visual().charactersPerTick());
            visual.set("fade-in-ticks", definition.visual().fadeInTicks()); visual.set("fade-out-ticks", definition.visual().fadeOutTicks());
            if (definition.area() != null) {
                var area = definition.area(); var region = entry.createSection("area");
                region.set("world", area.world().toString());
                region.set("min", List.of(area.minX(), area.minY(), area.minZ()));
                region.set("max", List.of(area.maxX(), area.maxY(), area.maxZ()));
            }
            entry.set("targets", definition.targets().stream().map(UUID::toString).sorted().toList());
            List<Map<String, Object>> lines = new ArrayList<>();
            for (var line : definition.lines()) {
                validateAudio(line.audio());
                var map = new LinkedHashMap<String, Object>();
                map.put("title", line.title()); map.put("subtitle", line.subtitle());
                map.put("duration-ticks", line.durationTicks()); map.put("gap-ticks", line.gapTicks());
                if (line.audio() != null) {
                    var audio = line.audio();
                    map.put("sound", Map.of("key", audio.key(), "volume", audio.volume(), "pitch", audio.pitch()));
                }
                lines.add(map);
            }
            entry.set("lines", lines);
            var counts = entry.createSection("completed");
            state.completed().getOrDefault(id, Map.of()).entrySet().stream()
                    .sorted(Map.Entry.comparingByKey()).forEach(value -> counts.set(value.getKey().toString(), value.getValue()));
        }
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "whispers-", ".tmp");
        try {
            ByteBuffer bytes = ByteBuffer.wrap(yaml.saveToString().getBytes(StandardCharsets.UTF_8));
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }

    private IllegalArgumentException invalid(String message) { return new IllegalArgumentException(file + ": " + message); }
    private ConfigurationSection section(ConfigurationSection source, String key) {
        if (!(source.get(key) instanceof ConfigurationSection section)) throw invalid(key + " harus section");
        return section;
    }
    private String string(ConfigurationSection source, String key) {
        if (!(source.get(key) instanceof String value)) throw invalid(key + " harus teks");
        return value;
    }
    private int integer(ConfigurationSection source, String key) {
        if (!(source.get(key) instanceof Integer value)) throw invalid(key + " harus integer");
        return value;
    }
    private float decimal(ConfigurationSection source, String key) {
        if (!(source.get(key) instanceof Number value)) throw invalid(key + " harus angka");
        return value.floatValue();
    }
    private List<Integer> coordinates(Object value) {
        if (!(value instanceof List<?> list) || list.size() != 3 || list.stream().anyMatch(item -> !(item instanceof Integer)))
            throw invalid("Koordinat harus [x, y, z] berisi tiga integer");
        return list.stream().map(Integer.class::cast).toList();
    }
    private UUID uuid(String text) {
        if (!text.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")) throw invalid("UUID tidak valid: " + text);
        return UUID.fromString(text);
    }
}
'@
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperTemplates.java'
        OriginalHashes = @()
        Hash = '3ae691ae2b2883424455ce503a5bd5125f9d1b914a6113ddcc1e6ac50ed5ab34'
        Source = @'
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
'@
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperText.java'
        OriginalHashes = @()
        Hash = '1914097153066e3294ceed3cd985e0dbd1e7f56cdc097ae1cb572f803128dcf2'
        Source = @'
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
'@
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperVisual.java'
        OriginalHashes = @()
        Hash = '8a97868070ae8f7a53884e5ca12c089cfc48494a41f5091daf162287b6db9d32'
        Source = @'
package com.bangzachery.vitae.vitaemanager.whisper;

public record WhisperVisual(boolean typing, int charactersPerTick, int fadeInTicks, int fadeOutTicks) {
    public WhisperVisual {
        if (charactersPerTick < 1 || charactersPerTick > 8 || fadeInTicks < 0 || fadeInTicks > 40 || fadeOutTicks < 0 || fadeOutTicks > 40)
            throw new IllegalArgumentException("Typing 1–8 karakter/tick; fade masuk/keluar 0–2 detik.");
    }
    public static WhisperVisual defaults() { return new WhisperVisual(false, 2, 5, 5); }
    public WhisperVisual withTyping(boolean enabled, int speed) { return new WhisperVisual(enabled, speed, fadeInTicks, fadeOutTicks); }
    public WhisperVisual withFade(int in, int out) { return new WhisperVisual(typing, charactersPerTick, in, out); }
}
'@
    },
    [PSCustomObject]@{
        Name = 'src/test/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperCinematicStoreTest.java'
        OriginalHashes = @()
        Hash = 'f3a9aa3d91b28e3a47b5413bea5e3719d1085ac35d5c70755fffca4853990a2c'
        Source = @'
package com.bangzachery.vitae.vitaemanager.whisper;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WhisperCinematicStoreTest {
    @TempDir Path directory;
    private Path file() { return directory.resolve("whispers.yml"); }
    private WhisperStore store() { return new WhisperStore(file(), Set.of("minecraft:music_disc.11", "minecraft:block.wood.break", "minecraft:entity.ghast.scream", "minecraft:entity.warden.ambient")); }
    @Test void oldDataLoadsWithoutRewritingBytesOrLosingCompletedPlayers() throws Exception {
        var definition = WhisperStateTest.ready("old");
        var state = WhisperState.empty().put(definition).finish(definition, WhisperStateTest.PLAYER); store().save(state);
        var yaml = YamlConfiguration.loadConfiguration(file().toFile());
        yaml.set("whispers.old.visual", null); yaml.set("whispers.old.music", null);
        Files.writeString(file(), yaml.saveToString()); byte[] original = Files.readAllBytes(file());
        assertEquals(state, store().load()); assertArrayEquals(original, Files.readAllBytes(file()));
    }
    @Test void colouredTemplatesMusicTypingAndCompletedPlayersSurviveReload() throws Exception {
        var base = WhisperStateTest.ready("lunar");
        var state = WhisperState.empty().put(base).finish(base, WhisperStateTest.PLAYER);
        var preset = WhisperTemplates.apply(base, "lunar").withEnabled(true);
        state = state.put(preset); store().save(state);
        assertEquals(state, store().load());
        assertEquals("&9Lunar", store().load().require("lunar").lines().getFirst().title());
        assertEquals(1, store().load().count("lunar", WhisperStateTest.PLAYER));
    }
    @Test void badMusicAndVisualSettingsCannotOverwritePreviousBytes() throws Exception {
        var definition = WhisperTemplates.apply(WhisperStateTest.ready("a"), "lunar");
        store().save(WhisperState.empty().put(definition)); String original = Files.readString(file());
        for (var change : List.of(Map.entry("whispers.a.music.repeat-ticks", (Object) 1), Map.entry("whispers.a.music.volume", (Object) 1.1),
                Map.entry("whispers.a.music.key", (Object) "minecraft:does.not.exist"), Map.entry("whispers.a.visual.typing", (Object) "true"),
                Map.entry("whispers.a.visual.characters-per-tick", (Object) 9), Map.entry("whispers.a.visual.fade-in-ticks", (Object) (-1)))) {
            var yaml = new YamlConfiguration(); yaml.loadFromString(original); yaml.set(change.getKey(), change.getValue());
            Files.writeString(file(), yaml.saveToString()); byte[] bad = Files.readAllBytes(file());
            assertThrows(Exception.class, () -> store().load()); assertArrayEquals(bad, Files.readAllBytes(file()));
        }
    }
    @Test void unknownVanillaMusicIsRefusedBeforeDiskReplacementButCustomMusicIsAllowed() throws Exception {
        var store = store(); store.initialize(); byte[] original = Files.readAllBytes(file());
        var definition = WhisperStateTest.ready("a").withMusic(new WhisperMusic(new WhisperLine.Audio("minecraft:wrong", .5f, 1), 0));
        assertThrows(IllegalArgumentException.class, () -> store.save(WhisperState.empty().put(definition)));
        assertArrayEquals(original, Files.readAllBytes(file()));
        var custom = definition.withMusic(new WhisperMusic(new WhisperLine.Audio("vitae:horor/lunar", .5f, 1), 200));
        var state = WhisperState.empty().put(custom); store.save(state); assertEquals(state, store.load());
    }
}
'@
    },
    [PSCustomObject]@{
        Name = 'src/test/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperCinematicTest.java'
        OriginalHashes = @()
        Hash = '3baafc211fc594f462a2aab8e221b2fce3927fc5eb2482baf45943fe1d54b28e'
        Source = @'
package com.bangzachery.vitae.vitaemanager.whisper;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WhisperCinematicTest {
    @Test void coloursAreRenderedWithoutTurningMiniMessageIntoInstructions() {
        var serializer = LegacyComponentSerializer.builder().character('&').hexColors().build();
        assertEquals("Lunar", WhisperText.plain("&9Lunar"));
        assertEquals("Lunar", WhisperText.plain("§9Lunar"));
        assertEquals("Lunar", WhisperText.plain("&A&LLunar"));
        assertEquals("Biru", WhisperText.plain("&#55AAFFBiru"));
        assertTrue(serializer.serialize(WhisperText.component("&9Lunar", NamedTextColor.WHITE)).contains("&9Lunar"));
        assertEquals("<red>tetap literal", WhisperText.plain("<red>tetap literal"));
        assertEquals(NamedTextColor.WHITE, WhisperText.component("tanpa warna", NamedTextColor.WHITE).color());
        assertThrows(IllegalArgumentException.class, () -> new WhisperLine("&9&l", "", 20, 0, null));
    }
    @Test void typingPrefixDoesNotCutColourCodesOrUnicodeCharacters() {
        String source = "&#55AAFF&lLunar 🌙 datang";
        assertEquals("Lunar 🌙", WhisperText.plain(WhisperText.prefix(source, 7)));
        assertEquals("", WhisperText.prefix(source, 0));
        assertEquals(source, WhisperText.prefix(source, 100));
        assertEquals(14, WhisperText.length(source));
    }
    @Test void longDialogSplitsAtWordsAndCarriesColourAndStyle() {
        String text = "&c&lKali ini kau harus berhati-hati dan beri tahu kepada rekan Revenant-mu bahwa tempat ini bukan dungeon seperti sebelumnya.";
        var pages = WhisperText.pages(text);
        assertTrue(pages.size() > 1);
        assertEquals(WhisperText.plain(text), String.join(" ", pages.stream().map(WhisperText::plain).toList()));
        for (String page : pages) {
            assertTrue(page.startsWith("&c&l"));
            assertTrue(WhisperText.length(page) <= WhisperText.PAGE_WIDTH);
            assertTrue(page.codePointCount(0, page.length()) <= 160);
            assertDoesNotThrow(() -> new WhisperLine("", page, 80, 10, null));
        }
    }
    @Test void longWordsUnicodeAndHexPagesHaveNoLostCharacters() {
        String text = "&#33AAFF" + "🌙".repeat(120);
        var pages = WhisperText.pages(text);
        assertEquals("🌙".repeat(120), String.join("", pages.stream().map(WhisperText::plain).toList()));
        pages.forEach(page -> assertTrue(page.startsWith("&#33AAFF")));
        for (String invalid : List.of("", "&c&l", "a\nb", "a\u2028b", "x".repeat(4001)))
            assertThrows(IllegalArgumentException.class, () -> WhisperText.pages(invalid));
    }
    @Test void templatesKeepAreaReceiversAndCompletedProgressAndRequireEnable() {
        var original = WhisperStateTest.ready("dungeon");
        var finished = WhisperState.empty().put(original).finish(original, WhisperStateTest.PLAYER);
        for (String name : WhisperTemplates.names()) {
            var preset = WhisperTemplates.apply(original, name);
            assertEquals(original.area(), preset.area()); assertEquals(original.targets(), preset.targets());
            assertEquals(original.id(), preset.id()); assertEquals(1, preset.limit()); assertFalse(preset.enabled());
            assertTrue(preset.visual().typing()); assertNotNull(preset.music());
            assertTrue(preset.lines().stream().allMatch(line -> line.audio() != null));
            var changed = finished.put(preset.withEnabled(true));
            assertEquals(1, changed.count(original.id(), WhisperStateTest.PLAYER));
            assertFalse(changed.eligible(changed.require(original.id()), WhisperStateTest.PLAYER));
        }
        assertEquals("&9Lunar", WhisperTemplates.apply(original, "lunar").lines().getFirst().title());
    }
    @Test void metadataSurvivesEveryLegacyDefinitionEdit() {
        var music = new WhisperMusic(new WhisperLine.Audio("vitae:whisper", .5f, .8f), 200);
        var visual = new WhisperVisual(true, 1, 10, 15);
        var source = WhisperStateTest.ready("a").withMusic(music).withVisual(visual);
        for (var edited : List.of(source.withArea(source.area()), source.withTargets(source.targets()), source.withLines(source.lines()), source.withLimit(2), source.withEnabled(false))) {
            assertEquals(music, edited.music()); assertEquals(visual, edited.visual());
        }
        var legacy = new WhisperDefinition("old", source.area(), source.targets(), source.lines(), 1, true);
        assertNull(legacy.music()); assertEquals(WhisperVisual.defaults(), legacy.visual());
        assertThrows(IllegalArgumentException.class, () -> new WhisperMusic(music.sound(), 1));
        assertThrows(IllegalArgumentException.class, () -> new WhisperVisual(true, 0, 5, 5));
    }
    @Test void durationAndQueueMatchTheLastLineWithoutAnExtraFinalGap() {
        var definition = WhisperStateTest.ready("a").withLines(List.of(new WhisperLine("a", "", 40, 20, null), new WhisperLine("b", "", 60, 100, null)));
        assertEquals(120, definition.durationTicks());
        var queue = new WhisperPlayback(); var run = new WhisperPlayback.Run(definition, true);
        assertTrue(queue.enqueue(WhisperStateTest.PLAYER, run));
        assertNotNull(queue.advance(WhisperStateTest.PLAYER, 1));
        assertNull(queue.advance(WhisperStateTest.PLAYER, 60));
        assertEquals("b", queue.advance(WhisperStateTest.PLAYER, 61).line().title());
        assertNull(queue.advance(WhisperStateTest.PLAYER, 120));
        assertTrue(queue.advance(WhisperStateTest.PLAYER, 121).complete());
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
        if ($fileVitae.OriginalHashes.Count -eq 0 -or $oldHashVitae -notin $fileVitae.OriginalHashes) {
            throw ($fileVitae.Name + ' berbeda dari commit sumber pembaruan. Kirim versi terbarunya untuk penyesuaian. Tidak ada file yang diubah.')
        }
        if ((Get-Item -LiteralPath $targetVitae).IsReadOnly) { throw ($fileVitae.Name + ' read-only. Tidak ada file yang diubah.') }
    } elseif ($fileVitae.OriginalHashes.Count -gt 0) {
        throw ($fileVitae.Name + ' belum ada; modul lama belum lengkap. Tidak ada file yang diubah.')
    } elseif (Test-Path -LiteralPath $targetVitae) {
        throw ($fileVitae.Name + ' adalah direktori, bukan file. Tidak ada file yang diubah.')
    }
    $plansVitae += [PSCustomObject]@{ Name=$fileVitae.Name; Path=$targetVitae; Bytes=$bytesVitae; Existed=$existsVitae; OldRawHash=$oldRawHashVitae }
}
if ($plansVitae.Count -eq 0) { Write-Host 'Update bisikan sinematik ini sudah terpasang; tidak ditulis ulang.' -ForegroundColor Green; return }
Write-Host ('Pemeriksaan lolos: ' + $plansVitae.Count + ' file siap dipasang.') -ForegroundColor Green
Write-Host 'Main, command lain, dan konfigurasi server tetap utuh.' -ForegroundColor Cyan
if ($CheckOnly) { Write-Host 'CheckOnly selesai. Belum ada file yang diubah.' -ForegroundColor Yellow; return }
$backupVitae = Join-Path $rootVitae ('vitae-bisikan-backup-' + [DateTime]::Now.ToString('yyyyMMdd-HHmmss-fff') + '-' + [Guid]::NewGuid().ToString('N').Substring(0,6))
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
        $tempVitae = $planVitae.Path + '.vitae-bisikan-' + [Guid]::NewGuid().ToString('N')
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
Write-Host 'Update bisikan sinematik terpasang. Backup source lama:' -ForegroundColor Green
Write-Host $backupVitae
Write-Host 'Lanjutkan: .\gradlew.bat clean build' -ForegroundColor Cyan
Write-Host 'Matikan server, ganti JAR hasil build, biarkan folder data lama, lalu nyalakan server.' -ForegroundColor Yellow
Write-Host 'Bantuan: /vitae bisikan help. Template: lunar, ryujin, outsider, elysia, penyamar.' -ForegroundColor Cyan
```
<!-- VITAE_INSTALLER_END -->
