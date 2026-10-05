# Vitaemanager — Modul 4: NPC + Dialog + Quest

Dokumen kode lengkap untuk Paper 1.21.4 dan Java 21. Modul ini disusun kembali mengikuti kesepakatan percakapan: NPC milik Vitae, dialog bercabang, kontrol scroll, hadiah, ladang dan mob quest. Tidak membutuhkan Citizens, BetterHUD, atau Typewriter untuk menjalankan NPC Vitae. ModelEngine 4 digunakan hanya bila memilih NPC model custom.

## Sebelum memasang

**Jangan mengganti seluruh `Vitaemanager.java` dengan versi referensi yang lebih pendek.** File 328 baris milikmu belum tersedia untuk dibandingkan. Jumlah baris sendiri tidak membuktikan ada atau tidak ada fitur yang terhapus. Pemasang di bawah menambah startup/shutdown NPC pada isi file yang ada, dan menambah metode pembayaran NPC pada VitiService tanpa mengganti fitur giveaway, evolusi, bisikan, carry, maupun modul lain di file tersebut.

Pemasang mempertahankan isi file yang ada sekarang. Untuk memulihkan bagian yang mungkin sudah hilang ketika versi 247 baris dipaste, tetap diperlukan salinan versi 328 baris aslinya. Dokumen ini memprioritaskan penyelesaian NPC; tidak mengaku telah membandingkan kedua versi itu.

Semua kelas NPC ada utuh dalam dokumen ini. Ada tujuh file NPC: enam kelas modul dan satu kelas penghubung lifecycle (`NpcModule`). `VillagerTradeListener` juga disertakan supaya main file yang sudah merujuk kelas itu memiliki sumber pendukungnya. Pemasang NPC tidak menambahkan pendaftaran listener trading baru ke main file.

## Cara pasang tanpa mencari potongan Java

1. Simpan Markdown ini di folder project `C:\Users\Administrator\IdeaProjects\vitaemanager`, sejajar dengan `gradlew.bat`.
2. Buat `install-npc.ps1` di folder yang sama, lalu salin seluruh blok PowerShell pada bagian **Pemasang lengkap** ke file tersebut.
3. Di terminal PowerShell pada folder project, jalankan pemeriksaan:

```powershell
.\install-npc.ps1 -CheckOnly
```

4. Jika pemeriksaan berhasil, jalankan:

```powershell
.\install-npc.ps1
.\gradlew.bat clean build
```

Pemasang memvalidasi titik integrasi sebelum mengubah file, mengambil kode Java langsung dari dokumen ini, dan menyimpan salinan file lama dalam folder `npc-backup-<waktu>`. Menjalankan pemasang lagi tidak menambah startup atau command duplikat. Jika pola integrasi tidak dikenali, pemasang berhenti dengan penjelasan dan file proyek belum diubah.

Jika PowerShell memblokir script karena Execution Policy, jalankan script yang sudah kamu simpan melalui satu proses ini:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\install-npc.ps1 -CheckOnly
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\install-npc.ps1
```

Pemasang mengenali dua susunan main file: integrasi `NpcService` langsung dari respons sebelumnya, atau main file yang belum mendaftarkan NPC. Integrasi langsung yang sudah lengkap dipertahankan. Susunan yang berbeda akan ditolak sebelum perubahan.

## Perilaku yang disertakan

| Bagian | Perilaku |
| --- | --- |
| NPC | Banyak NPC dengan ID berbeda; skin pemain atau model ModelEngine; setiap setup tersimpan sendiri. |
| Mulai dialog | Klik kiri NPC. Klik kanan dipakai untuk mengonfirmasi pilihan setelah dialog terbuka. |
| Kamera dan posisi | Kamera beralih ke tinggi kepala yang admin tentukan; posisi dan arah pandang terkunci selama dialog. |
| Teks dan pilihan | Teks muncul bertahap dengan suara vanilla; pilihan baru muncul setelah teks selesai. Scroll memilih tanpa memindahkan slot hotbar. |
| Konfirmasi dan batal | Klik kiri/kanan untuk memilih; Shift membatalkan dan memainkan suara villager. Halaman tanpa pilihan memiliki `Keluar...`. |
| Area pemicu | Titik A–B, transisi gelap pribadi, teleport ke posisi berdiri di depan NPC, lalu dialog otomatis. Hanya dialog yang selesai menandai pemicu tuntas. |
| Hadiah dialog | Viti atau item custom utuh; hanya sekali per pemain untuk NPC tersebut. Interaksi berikutnya menggunakan dialog pengulangan. |
| GUI hadiah | Chest 27 slot; admin meletakkan item contoh. Saat ditutup, template disimpan dan item asli dikembalikan ke admin. |
| Hadiah item player | Item masuk inventory; bagian yang tidak muat dijatuhkan di lokasi pemain. Metadata item disimpan melalui serialisasi ItemStack Paper. |
| Hadiah Viti | Dikreditkan melalui ledger Vitae dengan ID transaksi, disertai pesan atas nama NPC. |
| Ladang | Membaca wortel, kentang, gandum, dan bit yang telah admin tanam di atas farmland; satu gelombang berarti semua tanaman selesai dipanen. |
| Hasil panen | Tidak masuk inventory, tidak menjatuhkan item dan XP; tanaman muncul lagi setelah jeda dua detik untuk gelombang berikutnya. |
| Perlindungan ladang | Farmland tidak berubah karena lompatan; penempatan, perubahan tanaman, aliran air, piston, dan ledakan pada ladang aktif dibatasi. Matikan NPC sebelum mengedit ladang. |
| Alat ladang | Iron hoe harus ada di storage inventory atau offhand; boleh memiliki enchant dan durability berapa pun yang masih bisa dipakai. |
| Quest mob | Mob vanilla muncul satu per satu pada titik aman acak di area. Mob hostile berhenti dengan AI dimatikan; mob biasa bergerak di dalam area. |
| Alat mob | Iron sword harus ada di inventory; membunuh mob quest dilakukan menggunakan iron sword di tangan utama. Mob quest tidak menjatuhkan loot/XP. |
| Giliran | Satu pemain pada area quest yang saling beririsan. Pemain berikutnya mendapat dialog bahwa tempat sedang dipakai. |
| Kuota | Tiga penyelesaian quest dari jenis/NPC apa pun, lalu cooldown 20 menit. Membatalkan atau gagal tidak menambah kuota. |
| Iseng NPC | Saat cooldown: interaksi pertama memberi peringatan, interaksi berikutnya mengusir sekitar tiga blok, dengan pemeriksaan ruang bebas. |
| Batas waktu | Bossbar countdown 30 menit. Mati, disconnect, teleport luar, keluar area, alat hilang, atau waktu habis membatalkan quest dan memulihkan ladang. |
| Bantuan admin | `/vnpc help`, saran saat command salah, dan tab completion. |

Dialog memakai TextDisplay vanilla di depan pandangan pemain. Transisi gelap memakai panel TextDisplay pribadi dan efek Blindness singkat; elemen HUD vanilla tetap mengikuti pengaturan klien. Animasi kamera, panel, paket NPC manusia, ModelEngine, interaksi dengan plugin lain, dan penjatuhan item ketika inventory penuh perlu diperiksa pada server 1.21.4 saat kamu mulai pengujian server.

Progres, cooldown, definisi NPC dan antrean hadiah disimpan dalam `plugins/vitaemanager/npc.json`. File rusak ditolak dan dipertahankan. NPC menyimpan progres berdasarkan UUID pemain. Hadiah Viti dapat dicoba kembali dengan receipt yang sama tanpa mengkredit saldo dua kali. Pengiriman item mengikuti penyimpanan player/world Minecraft; penghentian paksa proses tepat di antara penyimpanan itu masih memiliki batas transaksi bawaan server.

## Setup NPC dialog dan hadiah sekali

Berdirilah pada posisi pemain akan berbicara. NPC baru dibuat dua blok di depanmu, sebagai draft nonaktif.

```text
/vnpc create mira Mira
/vnpc mira skin NamaPemainPremium
/vnpc mira page start Hai Lunar, boleh aku bercerita?
/vnpc mira page cerita Aku menyimpan rahasia tentang senjata itu.
/vnpc mira option start cerita Aku mendengarkan.
/vnpc mira option start END Keluar...
/vnpc mira option cerita END Terima kasih sudah bercerita.
/vnpc mira repeat Hadiahku sudah kuberikan kepadamu. Hus hus, cari bansos lagi!
/vnpc mira reward dialog items
```

Pada GUI yang muncul, letakkan item hadiah dan tutup GUI. Item admin dikembalikan; Vitae menyimpan salinannya sebagai template. Kemudian:

```text
/vnpc mira on
/vnpc mira preview
```

Preview tidak membagikan hadiah, tidak memulai quest, dan tidak menyelesaikan progres pemain. Skin diambil dari pemain online atau profil akun premium; pengambilan skin memerlukan layanan profil Minecraft yang tersedia.

Untuk hadiah uang, gunakan `reward dialog viti 1000` sebagai pengganti `reward dialog items`. Nominal menerima format Viti yang sudah ada, misalnya `1.000` atau `1.000,50`. Setiap perubahan setup membuat NPC menjadi draft nonaktif; jalankan `on` lagi setelah selesai.

## Mengatur lokasi, kepala, model, dan pemicu

```text
/vnpc mira move
/vnpc mira stand
/vnpc mira height 1.62
/vnpc mira speed 1
```

`move` menyimpan posisi NPC di tempat admin berdiri. `stand` menyimpan posisi pemain ketika dibawa ke NPC. Jangan menempatkan stand di dalam blok. `height` menentukan titik kepala yang dilihat kamera; model tinggi mungkin memerlukan angka berbeda. `speed` adalah jumlah karakter per pembaruan teks, dari 1 sampai 8.

Untuk NPC model custom yang sudah tersedia di ModelEngine 4:

```text
/vnpc mira model id_model_kamu
/vnpc mira height 2.4
/vnpc mira on
```

Untuk area yang harus dilewati:

```text
/vnpc mira pos1
/vnpc mira pos2
/vnpc mira trigger set
/vnpc mira on
```

`pos1` dan `pos2` mengambil blok yang admin lihat dalam jarak enam blok; bila tidak ada blok yang terlihat, mengambil blok posisi admin. Tandai seluruh kubus lorong, termasuk ketinggian kaki pemain. Titik A dan B harus dalam dunia yang sama dengan NPC. Simpan posisi `stand` di depan NPC dan di luar lorong pemicu supaya pembatalan dialog memberi ruang untuk mencoba kembali. `/vnpc mira trigger off` melepas pemicu tersebut.

Shift sebelum memilih END atau menerima QUEST tidak menandai dialog selesai. Masuk kembali ke area akan memulai dialog lagi. Menerima quest menandai dialog pengantar selesai; kegagalan quest tetap dapat dicoba kembali melalui klik NPC.

## Setup quest ladang

Siapkan tanaman di atas farmland terlebih dahulu. Quest ini membaca tanaman yang ada, bukan membuat ladang kosong menjadi tanaman. Satu ladang dapat berisi beberapa jenis tanaman; seluruh tanaman yang terbaca dihitung sebagai satu gelombang.

```text
/vnpc create petani Pak Tani
/vnpc petani page start Bantu aku memanen ladang ini lima kali, ya?
/vnpc petani option start QUEST Terima quest.
/vnpc petani option start END Keluar...
```

Pergi ke ladang, tandai A–B yang mencakup tanaman, lalu berdirilah di titik awal player dalam area tersebut:

```text
/vnpc petani pos1
/vnpc petani pos2
/vnpc petani quest farm 5
/vnpc petani reward quest viti 1000
/vnpc petani on
```

`quest farm 5` berarti lima gelombang panen: `1/5` sampai `5/5`. Seluruh tanaman harus dihancurkan sebelum gelombang bertambah. Pindahkan titik awal dengan `queststart` jika diperlukan; titik awal tetap harus dalam area quest. Seleksi ladang maksimum 8192 blok dan 512 tanaman untuk menjaga pembacaan serta pemulihan tetap terbatas.

Pemain harus membawa iron hoe. Saat menerima quest, pemain dipindahkan ke titik awal. Selesai: ladang dipulihkan, pemain dikembalikan ke stand NPC, dan hadiah Viti dikirim.

## Setup quest mob

```text
/vnpc create penjaga Penjaga
/vnpc penjaga page start Bantu aku mengalahkan lima zombie di arena.
/vnpc penjaga option start QUEST Terima quest.
/vnpc penjaga option start END Keluar...
```

Tandai area arena A–B, lalu berdirilah pada titik awal di dalam arena:

```text
/vnpc penjaga pos1
/vnpc penjaga pos2
/vnpc penjaga quest mob 5 ZOMBIE
/vnpc penjaga reward quest viti 1500
/vnpc penjaga on
```

Sediakan lantai padat dan ruang dua blok untuk spawn. Pilihan lain: `COW`, `PIG`, `SHEEP`, `CHICKEN`, `SKELETON` dan mob vanilla yang didukung. Ender dragon tidak diterima karena perilaku dan ukurannya tidak sesuai arena biasa. Monster memakai AI nonaktif dan tidak menyerang. Hewan biasa bergerak; yang keluar dari area dipindahkan kembali ke titik spawn.

Quest mob juga dapat memberi hadiah item melalui `reward quest items`. Untuk quest ladang, hadiah wajib Viti sesuai kesepakatan.

## Command pemain dan pengelolaan

```text
/quest status
/quest quit
/vnpc help
/vnpc list
/vnpc mira off
/vnpc mira on
/vnpc mira reset NamaPemain
```

`reset` menghapus progres dialog NPC itu dan mereset kuota quest global pemain. Jangan gunakan reset rutin jika ingin kuota tetap adil; ini command admin untuk koreksi/pengujian. Tidak bisa reset saat dialog/quest aktif atau ada hadiah yang belum terkirim. Nama pemain harus sudah tercatat di server; UUID juga diterima.

`page` mengganti teks sambil mempertahankan pilihan yang sudah ada. `clearoptions <halaman>` mengosongkan pilihan sehingga halaman memiliki `Keluar...` otomatis. `root <halaman>` menentukan halaman awal. Target pilihan bisa nama halaman, `END`, atau `QUEST`. Semua halaman harus punya jalur keluar. Hindari menggunakan nama halaman `END` dan `QUEST` karena kedua kata itu dipakai sebagai tindakan khusus.

Permission admin: `vitae.admin.npc`. Permission player: `vitae.npc` dan `vitae.quest`, default true. Izin induk `vitae.admin` mendapat child NPC melalui pemasang.

## Pemasang lengkap

Salin blok ini seluruhnya ke `install-npc.ps1` di folder project.

````powershell
param(
    [string]$Document = (Join-Path $PSScriptRoot 'vitaemanager-modul-4-npc-dialog-quest.md'),
    [string]$Project = $PSScriptRoot,
    [switch]$CheckOnly
)
$ErrorActionPreference = 'Stop'
$utf8 = New-Object System.Text.UTF8Encoding($false)
$root = [IO.Path]::GetFullPath($Project)
$docPath = [IO.Path]::GetFullPath($Document)
if (-not (Test-Path -LiteralPath $docPath -PathType Leaf)) { throw 'File Markdown tidak ditemukan.' }
$doc = [IO.File]::ReadAllText($docPath, $utf8)
$base = 'src/main/java/com/bangzachery/vitae/vitaemanager/'
$names = @('npc/NpcData.java','npc/NpcStore.java','npc/NpcRenderer.java','npc/NpcQuest.java',
    'npc/NpcService.java','npc/NpcCommand.java','npc/NpcModule.java','integration/VillagerTradeListener.java')
$allowed = @($names | ForEach-Object { $base + $_ })
$sourceBlocks = [regex]::Matches($doc, '(?ms)^### FILE: (?<path>[^\r\n]+)\r?\n\r?\n```java\r?\n(?<code>.*?)\r?\n```[ \t]*\r?$')
if ($sourceBlocks.Count -ne $allowed.Count) { throw 'Jumlah blok sumber tidak lengkap. Unduh Markdown utuh sebelum memasang.' }
$writes = [ordered]@{}
foreach ($block in $sourceBlocks) {
    $path = $block.Groups['path'].Value
    if ($allowed -notcontains $path -or $writes.Contains($path)) { throw 'Path sumber tidak dikenali atau duplikat.' }
    $writes[$path] = $block.Groups['code'].Value + "`n"
}
function Read-ProjectFile([string]$path) {
    $absolute = Join-Path $root $path
    if (-not (Test-Path -LiteralPath $absolute -PathType Leaf)) { throw "File proyek belum ditemukan: $path" }
    return [IO.File]::ReadAllText($absolute, $utf8)
}
function Replace-One([string]$text, [string]$find, [string]$replacement) {
    $hits = [regex]::Matches($text, [regex]::Escape($find))
    if ($hits.Count -ne 1) { throw "Titik integrasi tidak tunggal: $find. File belum diubah." }
    return $text.Substring(0, $hits[0].Index) + $replacement + $text.Substring($hits[0].Index + $hits[0].Length)
}
function Add-YamlEntry([string]$text, [string]$section, [string]$key, [string]$body) {
    $pattern = '(?ms)^' + [regex]::Escape($section) + ':[ \t]*\r?\n(?<body>.*?)(?=^[^ \t#\r\n][^\r\n]*:|\z)'
    $matches = [regex]::Matches($text, $pattern)
    if ($matches.Count -ne 1) { throw "Bagian YAML $section tidak ditemukan atau duplikat. File belum diubah." }
    $m = $matches[0]; $old = $m.Groups['body'].Value
    if ([regex]::IsMatch($old, '(?m)^  ' + [regex]::Escape($key) + ':')) { return $text }
    $updated = $section + ":`n" + $old.TrimEnd() + "`n" + $body.TrimEnd() + "`n`n"
    return $text.Substring(0, $m.Index) + $updated + $text.Substring($m.Index + $m.Length)
}
$mainPath = $base + 'Vitaemanager.java'
$main = Read-ProjectFile $mainPath
$start = 'com.bangzachery.vitae.vitaemanager.npc.NpcModule.start(this, viti);'
$stop = 'com.bangzachery.vitae.vitaemanager.npc.NpcModule.stop(this);'
$legacy = [regex]::Match($main, '(?<field>\w+)\s*=\s*new\s+(?:com\.bangzachery\.vitae\.vitaemanager\.npc\.)?NpcService\s*\(\s*this\s*,\s*viti\s*\)')
if ($legacy.Success) {
    $field = [regex]::Escape($legacy.Groups['field'].Value)
    $patterns = @(
        ($field + '\.start\s*\(\s*\)'),
        ($field + '\.close\s*\(\s*\)'),
        ('registerEvents\s*\(\s*' + $field + '\s*,\s*this\s*\)')
    )
    foreach ($pattern in $patterns) {
        if (-not [regex]::IsMatch($main, $pattern)) { throw 'Integrasi NpcService lama tidak lengkap. File utama belum diubah.' }
    }
    if ($main.Contains($start)) { throw 'Ditemukan dua cara startup NPC. File utama belum diubah.' }
} else {
    if (-not $main.Contains($start)) {
        $anchor = $null
        foreach ($candidate in @('evolution.start();','whispers.start();','vitiListener.start();')) {
            if ($main.Contains($candidate)) { $anchor = $candidate; break }
        }
        if ($null -eq $anchor) { throw 'Titik startup Viti/bisikan/evolusi tidak ditemukan. File utama belum diubah.' }
        $main = Replace-One $main $anchor ($anchor + "`n            " + $start)
    }
    if (-not $main.Contains($stop)) {
        $match = [regex]::Matches($main, 'public\s+void\s+onDisable\s*\(\s*\)\s*\{')
        if ($match.Count -ne 1) { throw 'onDisable tidak tunggal. File utama belum diubah.' }
        $main = $main.Insert($match[0].Index + $match[0].Length, "`n        " + $stop)
    }
}
$writes[$mainPath] = $main
$vitiPath = $base + 'economy/VitiService.java'
$viti = Read-ProjectFile $vitiPath
if (-not [regex]::IsMatch($viti, 'public\s+void\s+reward\s*\(\s*Player\s+player\s*,\s*UUID\s+receipt\s*,\s*BigDecimal\s+amount\s*,\s*Consumer<String>\s+done\s*\)')) {
    if ([regex]::IsMatch($viti, '\bvoid\s+reward\s*\(')) { throw 'Metode reward berbeda dari versi modul ini. VitiService belum diubah.' }
    $reward = @'
    // Stable receipt: retrying an NPC payment cannot credit the balance twice.
    public void reward(Player player, UUID receipt, BigDecimal amount, Consumer<String> done) {
        if (!ready(done)) return;
        if (current.redeemed().contains(receipt)) { done.accept("viti-success"); return; }
        transact(state -> seed(state, player).redeem(player.getUniqueId(), receipt, amount, true), () -> {}, done);
    }

'@
    $viti = Replace-One $viti '    public void reload(Consumer<String> done) {' ($reward + "`n    public void reload(Consumer<String> done) {")
}
$writes[$vitiPath] = $viti
$ymlPath = 'src/main/resources/plugin.yml'
$yml = Read-ProjectFile $ymlPath
$yml = Add-YamlEntry $yml 'commands' 'vnpc' "  vnpc:`n    description: Setup NPC dialog dan quest Vitae`n    usage: /vnpc help`n    aliases: [vitaenpc]"
$yml = Add-YamlEntry $yml 'commands' 'quest' "  quest:`n    description: Status atau batalkan quest Vitae`n    usage: /quest status`n    aliases: [vitaequest]"
$yml = Add-YamlEntry $yml 'permissions' 'vitae.admin.npc' "  vitae.admin.npc:`n    description: Mengatur NPC dialog dan quest`n    default: op"
$yml = Add-YamlEntry $yml 'permissions' 'vitae.npc' "  vitae.npc:`n    description: Memulai dialog NPC Vitae`n    default: true"
$yml = Add-YamlEntry $yml 'permissions' 'vitae.quest' "  vitae.quest:`n    description: Menerima dan membatalkan quest Vitae`n    default: true"
$admin = [regex]::Match($yml, '(?ms)^  vitae\.admin:[ \t]*\r?\n(?<body>.*?)(?=^  [^ \t#\r\n][^\r\n]*:|^[^ \t#\r\n][^\r\n]*:|\z)')
if ($admin.Success -and -not $admin.Value.Contains('      vitae.admin.npc: true')) {
    $updated = $admin.Value
    $children = [regex]::Match($updated, '(?m)^    children:[ \t]*\r?\n')
    if ($children.Success) { $updated = $updated.Insert($children.Index + $children.Length, "      vitae.admin.npc: true`n") }
    else { $updated = $updated.TrimEnd() + "`n    children:`n      vitae.admin.npc: true`n" }
    $yml = $yml.Substring(0,$admin.Index) + $updated + $yml.Substring($admin.Index+$admin.Length)
}
$soft = [regex]::Match($yml, '(?m)^softdepend:[ \t]*\[(?<plugins>[^\]\r\n]*)\][ \t]*\r?$')
if (-not $soft.Success) { throw 'softdepend harus berupa daftar satu baris pada versi pemasang ini. plugin.yml belum diubah.' }
$plugins = @($soft.Groups['plugins'].Value.Split(',') | ForEach-Object { $_.Trim() } | Where-Object { $_ })
foreach ($name in @('ModelEngine','Shopkeepers')) { if ($plugins -notcontains $name) { $plugins += $name } }
$newSoft = 'softdepend: [' + ($plugins -join ', ') + ']'
$yml = $yml.Substring(0,$soft.Index) + $newSoft + $yml.Substring($soft.Index+$soft.Length)
$writes[$ymlPath] = $yml
$gradlePath = 'build.gradle.kts'
$gradle = Read-ProjectFile $gradlePath
if (-not $gradle.Contains('paperweight-mappings-namespace')) {
    $gradle = $gradle.TrimEnd() + "`n`ntasks.jar {`n    manifest.attributes[`"paperweight-mappings-namespace`"] = `"mojang`"`n}`n"
}
$writes[$gradlePath] = $gradle
if ($CheckOnly) {
    Write-Host ('Pemeriksaan berhasil. File yang akan dipasang/diperbarui: ' + $writes.Count)
    foreach ($path in $writes.Keys) { Write-Host $path }
    return
}
$backup = Join-Path $root ('npc-backup-' + [DateTime]::Now.ToString('yyyyMMdd-HHmmss-fff'))
$originals = @{}; $changed = New-Object System.Collections.Generic.List[string]
foreach ($path in $writes.Keys) {
    $absolute = Join-Path $root $path
    if (Test-Path -LiteralPath $absolute -PathType Leaf) {
        $saved = Join-Path $backup $path
        [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($saved)) | Out-Null
        [IO.File]::Copy($absolute, $saved, $false); $originals[$path] = $saved
    }
}
try {
    foreach ($path in $writes.Keys) {
        $absolute = Join-Path $root $path
        [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($absolute)) | Out-Null
        $changed.Add($path); [IO.File]::WriteAllText($absolute, $writes[$path], $utf8)
    }
} catch {
    foreach ($path in $changed) {
        $absolute = Join-Path $root $path
        if ($originals.ContainsKey($path)) { [IO.File]::Copy($originals[$path], $absolute, $true) }
        elseif (Test-Path -LiteralPath $absolute) { Remove-Item -LiteralPath $absolute }
    }
    throw
}
Write-Host 'NPC selesai dipasang. Isi modul lama di Vitaemanager.java dan VitiService.java dipertahankan.'
Write-Host ('Salinan sebelum pemasangan: ' + $backup)
Write-Host 'Berikutnya jalankan .\gradlew.bat clean build'
````

## Kode sumber lengkap

Bagian ini juga merupakan sumber yang dibaca pemasang. Pertahankan judul `### FILE:` dan pagar kode saat menyimpan Markdown. Kamu dapat menyalin setiap kelas secara manual ke path yang tercantum, tetapi pemasang di atas mengerjakan penempatan dan integrasinya sekaligus.

### FILE: src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcData.java

```java
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
    public record Choice(String label, String target) {
        public Choice {
            label = text(label, 80, "Label pilihan");
            target = id(target);
        }
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
                             Reward gift, Quest quest, String repeat, int speed, boolean enabled) {
        public Definition {
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
            if (enabled) validate(nodes, root, kind, model, quest);
        }
        public static Definition create(String id, String name, Point location, Point stand) {
            return new Definition(id, UUID.randomUUID(), name, Kind.HUMAN, "", "", "", location, stand,
                    1.62, null, "start", Map.of("start", new Node("Halo, ada yang bisa kubantu?", List.of())),
                    Reward.none(), Quest.none(), "Aku sudah memberikan hadiah kepadamu. Pergi sana, kamu mencari bansos lagi?", 1, false);
        }
    }
    private static void validate(Map<String, Node> nodes, String root, Kind kind, String model, Quest quest) {
        if (kind == Kind.MODEL && model.isBlank()) throw new IllegalArgumentException("ID model belum diatur.");
        for (Node node : nodes.values()) for (Choice c : node.available()) {
            if (c.target().equals("QUEST")) {
                if (quest.kind() == QuestKind.NONE || quest.reward().empty())
                    throw new IllegalArgumentException("Pilihan QUEST memerlukan quest dan hadiah.");
            } else if (!c.target().equals("END") && !nodes.containsKey(c.target()))
                throw new IllegalArgumentException("Tujuan pilihan tidak ditemukan: " + c.target());
        }
        Set<String> exits = new HashSet<>();
        boolean changed;
        do {
            changed = false;
            for (var entry : nodes.entrySet()) if (!exits.contains(entry.getKey())
                    && entry.getValue().available().stream().anyMatch(c -> c.target().equals("END")
                    || c.target().equals("QUEST") || exits.contains(c.target()))) changed |= exits.add(entry.getKey());
        } while (changed);
        if (exits.size() != nodes.size() || !exits.contains(root))
            throw new IllegalArgumentException("Setiap halaman harus memiliki jalan menuju END atau QUEST.");
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
                        Map<UUID, Quota> quotas, Map<UUID, Award> pending) {
        public State {
            if (version != 1 || npcs.size() > 128) throw new IllegalArgumentException("Versi data atau jumlah NPC tidak valid.");
            npcs = Map.copyOf(npcs); quotas = Map.copyOf(quotas); pending = Map.copyOf(pending);
            var progress = new HashMap<UUID, Set<UUID>>();
            completed.forEach((player, tokens) -> progress.put(Objects.requireNonNull(player), Set.copyOf(tokens)));
            completed = Map.copyOf(progress);
            npcs.forEach((key, value) -> { if (!key.equals(value.id())) throw new IllegalArgumentException("ID NPC berbeda."); });
            pending.forEach((key, value) -> { if (!key.equals(value.owner())) throw new IllegalArgumentException("Pemilik hadiah berbeda."); });
        }
        public static State empty() { return new State(1, Map.of(), Map.of(), Map.of(), Map.of()); }
        public Quota quota(UUID player, long now) { return quotas.getOrDefault(player, new Quota(0, 0)).effective(now); }
        public boolean done(UUID player, Definition npc) { return completed.getOrDefault(player, Set.of()).contains(npc.token()); }
        public State put(Definition d) { var copy = new HashMap<>(npcs); copy.put(d.id(), d); return new State(1, copy, completed, quotas, pending); }
        public State remove(String id) { var copy = new HashMap<>(npcs); copy.remove(id); return new State(1, copy, completed, quotas, pending); }
        public State heard(UUID player, Definition npc) {
            var copy = new HashMap<>(completed); var tokens = new HashSet<>(copy.getOrDefault(player, Set.of()));
            tokens.add(npc.token()); copy.put(player, tokens); return new State(1, npcs, copy, quotas, pending);
        }
        public State finish(UUID player, Definition npc, boolean quest, UUID receipt, long now) {
            if (pending.containsKey(player)) throw new IllegalStateException("Hadiah sebelumnya belum dikirim.");
            if (!quest && done(player, npc)) return this;
            State heard = heard(player, npc);
            var q = new HashMap<>(quotas); if (quest) q.put(player, quota(player, now).complete(now));
            var awards = new HashMap<>(pending);
            Reward reward = quest ? npc.quest().reward() : npc.gift();
            if (!reward.empty()) awards.put(player, new Award(receipt, player, npc.name(), reward));
            return new State(1, npcs, heard.completed, q, awards);
        }
        public State acknowledge(UUID player, UUID receipt) {
            Award a = pending.get(player); if (a == null || !a.receipt().equals(receipt)) return this;
            var copy = new HashMap<>(pending); copy.remove(player); return new State(1, npcs, completed, quotas, copy);
        }
        public State reset(UUID player, Definition npc) {
            if (pending.containsKey(player)) throw new IllegalStateException("Tunggu hadiah terkirim sebelum reset.");
            var c = new HashMap<>(completed); var tokens = new HashSet<>(c.getOrDefault(player, Set.of()));
            tokens.remove(npc.token()); c.put(player, tokens);
            var q = new HashMap<>(quotas); q.remove(player); return new State(1, npcs, c, q, pending);
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
```

### FILE: src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcStore.java

```java
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
        try (var reader = new JsonReader(Files.newBufferedReader(file, StandardCharsets.UTF_8))) {
            reader.setStrictness(Strictness.STRICT);
            JsonElement json = JsonParser.parseReader(reader);
            if (reader.peek() != com.google.gson.stream.JsonToken.END_DOCUMENT) throw new IllegalArgumentException("Data tambahan setelah JSON.");
            validate(json, NpcData.State.class);
            return Objects.requireNonNull(JSON.fromJson(json, NpcData.State.class));
        } catch (RuntimeException e) { throw new IOException("npc.json tidak valid. File lama dipertahankan.", e); }
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
    private static void validate(JsonElement value, Type type) {
        if (value == null || value.isJsonNull()) {
            if (type == NpcData.Point.class || type == com.bangzachery.vitae.vitaemanager.whisper.WhisperArea.class) return;
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
            for (RecordComponent field : c.getRecordComponents()) validate(value.getAsJsonObject().get(field.getName()), field.getGenericType());
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
```

### FILE: src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcRenderer.java

```java
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
    private record Visible(NpcData.Definition definition, Interaction hitbox, ArmorStand base,
                           Object modeled, Set<UUID> viewers, Set<UUID> failed) {}
    private final JavaPlugin plugin;
    private final NamespacedKey key;
    private final Input input;
    private final Map<String, Visible> entities = new HashMap<>();
    private final Listener modelListener = new Listener() {};
    private final Set<Chunk> tickets = new HashSet<>();
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
    public void refresh(Map<String, NpcData.Definition> definitions) {
        for (String id : List.copyOf(entities.keySet())) {
            NpcData.Definition next = definitions.get(id);
            if (next == null || !next.enabled() || !next.equals(entities.get(id).definition())) remove(id);
        }
        for (var d : definitions.values()) if (d.enabled() && !entities.containsKey(d.id()) && Bukkit.getWorld(d.location().world()) != null) {
            Interaction box = null; ArmorStand base = null; Object modeled = null;
            try {
                Location at = location(d.location());
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
                }
                entities.put(d.id(), new Visible(d, box, base, modeled, new HashSet<>(), new HashSet<>()));
            } catch (Exception e) {
                if (modeled != null) destroyModel(modeled); if (base != null) base.remove(); if (box != null) box.remove();
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
        for (Visible v : entities.values()) { v.viewers().remove(player.getUniqueId()); v.failed().remove(player.getUniqueId()); }
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void show(Player player, Visible v) throws ReflectiveOperationException {
        var d = v.definition(); UUID uuid = v.hitbox().getUniqueId(); int entityId = v.hitbox().getEntityId();
        Class<?> profileType = Class.forName("com.mojang.authlib.GameProfile");
        Object profile = profileType.getConstructor(UUID.class, String.class).newInstance(uuid, "VN_" + d.id().substring(0, Math.min(13, d.id().length())));
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
        Object spawn = Class.forName(prefix + "ClientboundAddEntityPacket")
                .getConstructor(int.class, UUID.class, double.class, double.class, double.class, float.class, float.class, entityType, int.class, vector, double.class)
                .newInstance(entityId, uuid, d.location().x(), d.location().y(), d.location().z(), d.location().pitch(), d.location().yaw(),
                        entityType.getField("PLAYER").get(null), 0, vector.getField("ZERO").get(null), (double) d.location().yaw());
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
    private void hide(Player p, Visible v) {
        try { send(p, Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket")
                .getConstructor(List.class).newInstance(List.of(v.hitbox().getUniqueId()))); }
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
                        input.click(p, v.definition().id(), attack); break;
                    } };
                    if (event.isAsynchronous()) Bukkit.getScheduler().runTask(plugin, dispatch); else dispatch.run();
                } catch (ReflectiveOperationException e) { throw new org.bukkit.event.EventException(e); }
            }, plugin, false);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("API ModelEngine 4 tidak sesuai.", e); }
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
        for (UUID viewer : v.viewers()) { Player p = Bukkit.getPlayer(viewer); if (p != null) hide(p, v); }
        if (v.modeled() != null) destroyModel(v.modeled()); if (v.base() != null) v.base().remove(); v.hitbox().remove();
    }
    @Override public void close() {
        for (String id : List.copyOf(entities.keySet())) remove(id);
        for (Chunk chunk : tickets) chunk.removePluginChunkTicket(plugin); tickets.clear(); HandlerList.unregisterAll(modelListener);
    }
}
```

### FILE: src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcQuest.java

```java
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
                run.bar.name(Component.text("Quest " + run.npc.name() + " • " + String.format("%02d:%02d", seconds / 60, seconds % 60)));
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
    private static void tell(Player p, NpcData.Definition d, String message) { p.sendMessage(Component.text(d.name() + ": " + message)); }
    @Override public void close() { for (UUID player : List.copyOf(runs.keySet())) cancel(player, "", false); HandlerList.unregisterAll(this); }
}
```

### FILE: src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcService.java

```java
package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.core.gui.VitaeMenu;
import com.bangzachery.vitae.vitaemanager.economy.*;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    private static final class Session {
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
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, Long> clicks = new HashMap<>(), warned = new HashMap<>();
    private final Map<UUID, BukkitTask> pushes = new HashMap<>();
    private final Set<UUID> internalTeleport = new HashSet<>(), paying = new HashSet<>();
    private final NamespacedKey awardKey;
    private BukkitTask task;
    private boolean closed;
    private long ticks;
    public NpcService(JavaPlugin plugin, VitiService viti) throws IOException {
        this.plugin = plugin; this.viti = viti; awardKey = new NamespacedKey(plugin, "npc_last_award");
        store = new NpcStore(plugin.getDataFolder().toPath().resolve("npc.json")); state = store.initialize();
        for (var d : state.npcs().values()) validateRuntime(d);
        for (var a : state.pending().values()) for (String bytes : a.reward().items()) decode(bytes);
        renderer = new NpcRenderer(plugin, this::modelClick);
        quests = new NpcQuest(plugin, () -> state.npcs().values(), this::questComplete, this::teleport);
        Bukkit.getPluginManager().registerEvents(quests, plugin);
    }
    public void start() {
        try {
            renderer.refresh(state.npcs());
            for (var d : state.npcs().values()) if (d.enabled()) NpcQuest.restore(d.quest());
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1, 1);
        } catch (RuntimeException e) { close(); throw e; }
    }
    public NpcData.State current() { return state; }
    public NpcData.Definition get(String id) {
        NpcData.Definition d = state.npcs().get(id);
        if (d == null) throw new IllegalArgumentException("NPC tidak ditemukan: " + id + ". /vnpc list"); return d;
    }
    public boolean inUse(String id) {
        return quests.uses(id) || sessions.values().stream().anyMatch(s -> s.npc.id().equals(id));
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
        try { renderer.refresh(state.npcs()); if (next.enabled()) NpcQuest.restore(next.quest()); }
        catch (RuntimeException e) { commit(previous); renderer.refresh(previous.npcs()); throw e; }
    }
    private void validateRuntime(NpcData.Definition d) {
        for (String item : d.gift().items()) decode(item);
        for (String item : d.quest().reward().items()) decode(item);
        if (d.quest().kind() == NpcData.QuestKind.MOB) NpcQuest.mobType(d.quest().mob());
        for (var crop : d.quest().crops()) {
            var plant = Bukkit.createBlockData(crop.plant()); var soil = Bukkit.createBlockData(crop.soil());
            if (!(plant instanceof org.bukkit.block.data.Ageable) || !Set.of(Material.CARROTS, Material.POTATOES, Material.WHEAT, Material.BEETROOTS).contains(plant.getMaterial())
                    || soil.getMaterial() != Material.FARMLAND) throw new IllegalArgumentException("Snapshot ladang tidak valid.");
        }
        if (d.enabled() && (Bukkit.getWorld(d.location().world()) == null || Bukkit.getWorld(d.stand().world()) == null))
            throw new IllegalArgumentException("Dunia NPC belum dimuat.");
    }
    public void delete(String id) throws IOException {
        get(id); if (inUse(id)) throw new IllegalStateException("NPC sedang dipakai."); commit(state.remove(id)); renderer.refresh(state.npcs());
    }
    public void reset(UUID player, String id) throws IOException {
        if (quests.run(player) != null || sessions.containsKey(player)) throw new IllegalStateException("Pemain sedang dialog/quest.");
        commit(state.reset(player, get(id))); warned.remove(player);
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
    public void status(Player p) {
        NpcQuest.Run r = quests.run(p.getUniqueId()); var quota = state.quota(p.getUniqueId(), System.currentTimeMillis());
        message(p, r == null ? "Tidak ada quest aktif. Selesai: " + quota.count() + "/3."
                : "Quest " + r.npc.name() + ": " + r.count + "/" + r.npc.quest().goal());
        if (quota.count() == 3) message(p, "Cooldown tersisa " + Math.max(1, (quota.until() - System.currentTimeMillis() + 999) / 1000) + " detik.");
    }
    private void tick() {
        if (closed) return; ticks++;
        if (ticks % 10 == 0) renderer.tick(); quests.tick();
        for (Session s : List.copyOf(sessions.values())) {
            if (!s.player.isOnline() || s.player.isDead() || System.currentTimeMillis() >= s.deadline) { end(s, false); continue; }
            if (s.phase == 0) {
                if (++s.age < 12) continue;
                removeBlindness(s); if (s.blackout != null) { s.blackout.remove(); s.blackout = null; }
                if (!teleport(s.player, NpcRenderer.location(s.npc.stand()))) { end(s, true); continue; }
                s.anchor = s.player.getLocation().clone(); s.phase = 1; s.age = 0; aim(s);
            } else if (s.phase == 1) {
                float t = Math.min(1, ++s.age / 12f);
                s.anchor.setYaw(s.fromYaw + shortest(s.goalYaw - s.fromYaw) * t);
                s.anchor.setPitch(s.fromPitch + (s.goalPitch - s.fromPitch) * t);
                if (!teleport(s.player, s.anchor)) { end(s, false); continue; }
                if (s.age >= 12) { s.phase = 2; s.age = 0; display(s); }
            } else if (ticks % 2 == 0) {
                int length = s.node.text().codePointCount(0, s.node.text().length());
                if (s.shown < length) { s.shown = Math.min(length, s.shown + s.npc.speed()); sound(s.player, Sound.BLOCK_NOTE_BLOCK_HAT, .15f, 1.4f); }
                render(s);
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
            var transform = e.getTransformation(); transform.getScale().set(new Vector3f(.22f)); e.setTransformation(transform);
        });
        s.player.showEntity(plugin, s.display); render(s);
    }
    private void render(Session s) {
        if (s.display == null) return;
        int total = s.node.text().codePointCount(0, s.node.text().length()); int end = s.node.text().offsetByCodePoints(0, s.shown);
        Component text = Component.text(s.npc.name() + "\n", NamedTextColor.GOLD).append(Component.text(s.node.text().substring(0, end), NamedTextColor.WHITE));
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
            } else if (d.quest().kind() == NpcData.QuestKind.NONE && state.done(p.getUniqueId(), d)) specialText = d.repeat();
        }
        Session s = new Session(p, d, preview, specialText != null, trigger);
        s.node = specialText == null ? d.nodes().get(d.root()) : new NpcData.Node(specialText, List.of());
        sessions.put(p.getUniqueId(), s);
        if (trigger) {
            Location blackAt = p.getEyeLocation().add(p.getEyeLocation().getDirection());
            s.blackout = blackAt.getWorld().spawn(blackAt, TextDisplay.class, e -> {
                e.setPersistent(false); e.setVisibleByDefault(false); e.setBillboard(Display.Billboard.CENTER);
                e.setSeeThrough(true); e.setBackgroundColor(Color.fromARGB(255, 0, 0, 0));
                e.text(Component.text("                                        \n                                        \n                                        "));
                var t = e.getTransformation(); t.getScale().set(new Vector3f(100)); e.setTransformation(t);
            });
            p.showEntity(plugin, s.blackout);
            if (!p.hasPotionEffect(PotionEffectType.BLINDNESS)) { p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 0, false, false)); s.blind = true; }
        } else aim(s);
        return true;
    }
    private void modelClick(Player p, String id, boolean attack) {
        if (sessions.containsKey(p.getUniqueId())) confirm(p); else if (attack) click(p, id);
    }
    private void click(Player p, String id) {
        if (ticks - clicks.getOrDefault(p.getUniqueId(), -100L) < 6) return; clicks.put(p.getUniqueId(), ticks);
        begin(p, get(id), false, false, null);
    }
    private void confirm(Player p) {
        Session s = sessions.get(p.getUniqueId());
        if (s == null || s.phase != 2 || s.shown < s.node.text().codePointCount(0, s.node.text().length())
                || ticks - clicks.getOrDefault(p.getUniqueId(), -100L) < 4) return;
        clicks.put(p.getUniqueId(), ticks); NpcData.Choice c = s.node.available().get(s.selected);
        sound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, .4f, 1.3f);
        if (c.target().equals("END")) {
            try {
                if (!s.preview && !s.special) commit(state.finish(p.getUniqueId(), s.npc, false, UUID.randomUUID(), System.currentTimeMillis()));
                end(s, false); pay(p);
            } catch (IOException e) { message(p, "Progres belum tersimpan; coba pilih kembali."); log(e); }
        } else if (c.target().equals("QUEST")) {
            if (s.preview) { message(p, "Preview selesai. Quest dan hadiah tidak dijalankan saat preview."); end(s, false); return; }
            String refusal = p.hasPermission("vitae.quest") ? quests.refusal(p, s.npc) : "Kamu tidak memiliki izin quest.";
            var quota = state.quota(p.getUniqueId(), System.currentTimeMillis()); if (quota.count() == 3) refusal = "Kamu masih cooldown.";
            if (refusal != null) { page(s, new NpcData.Node(refusal, List.of())); return; }
            end(s, false);
            if (quests.start(p, s.npc)) {
                try { commit(state.heard(p.getUniqueId(), s.npc)); }
                catch (IOException e) { quests.cancel(p.getUniqueId(), "Progres gagal disimpan; coba kembali.", true); log(e); }
            }
        } else page(s, s.npc.nodes().get(c.target()));
    }
    private void page(Session s, NpcData.Node node) { s.node = node; s.shown = 0; s.selected = 0; render(s); }
    private void end(Session s, boolean sound) {
        sessions.remove(s.player.getUniqueId()); removeBlindness(s); if (s.blackout != null) s.blackout.remove(); if (s.display != null) s.display.remove();
        s.player.sendActionBar(Component.empty()); if (sound) sound(s.player, Sound.ENTITY_VILLAGER_NO, .45f, 1);
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
        try { return p.teleport(location); } finally { internalTeleport.remove(p.getUniqueId()); }
    }
    private void push(Player p, NpcData.Definition d) {
        message(p, d.name() + ": Sudah kubilang istirahat dulu! Hus, hus!" );
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
            ItemStack[] before = p.getInventory().getStorageContents(); var dropped = new ArrayList<Item>();
            String marker = p.getPersistentDataContainer().get(awardKey, PersistentDataType.STRING);
            try {
                if (!award.receipt().toString().equals(marker)) {
                    ItemStack[] items = award.reward().items().stream().map(NpcService::decode).toArray(ItemStack[]::new);
                    for (ItemStack item : p.getInventory().addItem(items).values()) dropped.add(p.getWorld().dropItem(p.getLocation(), item));
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
    private void notifyAward(Player p, NpcData.Award award) {
        if (award.receipt().toString().equals(p.getPersistentDataContainer().get(awardKey, PersistentDataType.STRING))) return;
        p.getPersistentDataContainer().set(awardKey, PersistentDataType.STRING, award.receipt().toString()); p.saveData();
        p.sendMessage(Component.text(award.npcName() + ": " + (award.reward().viti().signum() > 0
                ? "Aku memberikan " + VitiAmount.format(award.reward().viti()) + " Viti kepadamu." : "Hadiahku sudah diberikan. Item yang tidak muat jatuh di dekatmu.")));
        sound(p, Sound.ENTITY_PLAYER_LEVELUP, .4f, 1);
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
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true) public void move(PlayerMoveEvent e) {
        if (internalTeleport.contains(e.getPlayer().getUniqueId())) return;
        Session s = sessions.get(e.getPlayer().getUniqueId());
        if (s != null) { e.setTo(s.anchor.clone()); return; }
        if (e.getTo() == null || quests.run(e.getPlayer().getUniqueId()) != null) return;
        for (var d : state.npcs().values()) if (d.enabled() && d.trigger() != null && !state.done(e.getPlayer().getUniqueId(), d)
                && (d.trigger().contains(e.getTo().getWorld().getUID(), e.getTo().getX(), e.getTo().getY(), e.getTo().getZ())
                || d.trigger().crossed(e.getFrom().getWorld().getUID(), e.getFrom().getX(), e.getFrom().getY(), e.getFrom().getZ(), e.getTo().getX(), e.getTo().getY(), e.getTo().getZ()))) {
            begin(e.getPlayer(), d, true, false, null); break;
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void externalTeleport(PlayerTeleportEvent e) {
        if (internalTeleport.contains(e.getPlayer().getUniqueId())) return;
        Session s = sessions.get(e.getPlayer().getUniqueId()); if (s != null) end(s, false);
        quests.cancel(e.getPlayer().getUniqueId(), "Teleport membatalkan quest.", false); renderer.forget(e.getPlayer());
    }
    @EventHandler(priority = EventPriority.LOWEST) public void held(PlayerItemHeldEvent e) {
        Session s = sessions.get(e.getPlayer().getUniqueId()); if (s == null) return; e.setCancelled(true);
        if (s.phase != 2 || s.shown < s.node.text().codePointCount(0, s.node.text().length())) return;
        int delta = Math.floorMod(e.getNewSlot() - e.getPreviousSlot() + 4, 9) - 4;
        if (delta == 0) return; s.selected = Math.floorMod(s.selected + (delta > 0 ? 1 : -1), s.node.available().size());
        sound(s.player, Sound.UI_BUTTON_CLICK, .3f, 1.2f); render(s);
    }
    @EventHandler(priority = EventPriority.LOWEST) public void sneak(PlayerToggleSneakEvent e) {
        Session s = sessions.get(e.getPlayer().getUniqueId()); if (s != null && e.isSneaking()) { e.setCancelled(true); end(s, true); }
    }
    @EventHandler(priority = EventPriority.LOWEST) public void swing(PlayerAnimationEvent e) {
        if (e.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
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
        if (renderer.id(e.getRightClicked()) != null || sessions.containsKey(e.getPlayer().getUniqueId())) e.setCancelled(true);
        if (e.getHand() == EquipmentSlot.HAND && sessions.containsKey(e.getPlayer().getUniqueId())) confirm(e.getPlayer());
    }
    @EventHandler(priority = EventPriority.LOWEST) public void entityInteractAt(PlayerInteractAtEntityEvent e) { entityInteract(e); }
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
    @EventHandler public void quit(PlayerQuitEvent e) { disconnect(e.getPlayer()); clicks.remove(e.getPlayer().getUniqueId()); warned.remove(e.getPlayer().getUniqueId()); }
    @EventHandler public void respawn(PlayerRespawnEvent e) { renderer.forget(e.getPlayer()); }
    @EventHandler public void world(PlayerChangedWorldEvent e) { renderer.forget(e.getPlayer()); }
    @EventHandler public void worldLoad(WorldLoadEvent e) { renderer.refresh(state.npcs()); for (var d : state.npcs().values()) if (d.enabled() && !quests.uses(d.id())) NpcQuest.restore(d.quest()); }
    private void disconnect(Player p) { BukkitTask push = pushes.remove(p.getUniqueId()); if (push != null) push.cancel(); Session s = sessions.get(p.getUniqueId()); if (s != null) end(s, false); quests.cancel(p.getUniqueId(), "", false); renderer.forget(p); }
    private static void sound(Player p, Sound sound, float volume, float pitch) { p.playSound(p.getLocation(), sound, volume, pitch); }
    public static void message(Player p, String text) { p.sendMessage(Component.text("[Vitae NPC] " + Objects.requireNonNullElse(text, "Setup gagal."), NamedTextColor.YELLOW)); }
    private void log(Exception e) { plugin.getLogger().log(Level.SEVERE, "Operasi NPC gagal; progres/hadiah belum dihapus.", e); }
    @Override public void close() {
        if (closed) return; closed = true; if (task != null) task.cancel();
        for (Player p : Bukkit.getOnlinePlayers()) if (p.getOpenInventory().getTopInventory().getHolder() instanceof RewardMenu) p.closeInventory();
        for (Session s : List.copyOf(sessions.values())) end(s, false);
        for (BukkitTask push : pushes.values()) push.cancel(); pushes.clear();
        quests.close(); renderer.close(); HandlerList.unregisterAll(this);
    }
}
```

### FILE: src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcCommand.java

```java
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
```

### FILE: src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcModule.java

```java
package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.economy.VitiService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.IOException;
import java.util.*;

/** Lifecycle facade: existing modules in Vitaemanager.java need no replacement. */
public final class NpcModule {
    private static final Map<JavaPlugin, NpcService> ACTIVE = new IdentityHashMap<>();
    private NpcModule() {}
    public static void start(JavaPlugin plugin, VitiService viti) throws IOException {
        if (ACTIVE.containsKey(plugin)) return;
        NpcService service = new NpcService(plugin, viti);
        try {
            NpcCommand executor = new NpcCommand(service);
            for (String name : List.of("vnpc", "quest")) {
                PluginCommand command = plugin.getCommand(name);
                if (command == null) throw new IllegalStateException("Command " + name + " belum terdaftar di plugin.yml.");
                command.setExecutor(executor); command.setTabCompleter(executor);
            }
            plugin.getServer().getPluginManager().registerEvents(service, plugin);
            service.start(); ACTIVE.put(plugin, service);
        } catch (RuntimeException e) { service.close(); throw e; }
    }
    public static void stop(JavaPlugin plugin) {
        NpcService service = ACTIVE.remove(plugin); if (service != null) service.close();
    }
}
```

### FILE: src/main/java/com/bangzachery/vitae/vitaemanager/integration/VillagerTradeListener.java

```java
package com.bangzachery.vitae.vitaemanager.integration;

import io.papermc.paper.event.player.PlayerTradeEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.plugin.Plugin;
import java.util.*;
import java.util.function.*;

/** Keeps Shopkeepers entity shops and virtual merchants available. */
public final class VillagerTradeListener implements Listener {
    private final Supplier<Plugin> shops;
    private final LongSupplier clock;
    private final Map<UUID, Long> feedback = new HashMap<>();
    private static final Component MESSAGE = Component.text("[Vitae] Trading dengan villager biasa dilarang. Silakan datang ke villager toko yang sudah disediakan di masing-masing base.", NamedTextColor.YELLOW);
    public VillagerTradeListener() { this(() -> Bukkit.getPluginManager().getPlugin("Shopkeepers"), System::nanoTime); }
    VillagerTradeListener(Supplier<Plugin> shops, LongSupplier clock) { this.shops = shops; this.clock = clock; }
    private boolean blocked(Entity entity) {
        if (!(entity instanceof AbstractVillager)) return false;
        Plugin owner = shops.get();
        return owner == null || !owner.isEnabled() || entity.getMetadata("shopkeeper").stream().noneMatch(m -> m.getOwningPlugin() == owner);
    }
    private void denied(Player player) {
        long now = clock.getAsLong(); Long old = feedback.get(player.getUniqueId());
        if (old == null || now - old >= 1_000_000_000L) { feedback.put(player.getUniqueId(), now); player.sendMessage(MESSAGE); }
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void interact(PlayerInteractEntityEvent e) {
        if (blocked(e.getRightClicked())) { e.setCancelled(true); denied(e.getPlayer()); }
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void interactAt(PlayerInteractAtEntityEvent e) { interact(e); }
    @EventHandler(priority = EventPriority.HIGHEST) public void open(InventoryOpenEvent e) {
        if (e.getInventory() instanceof MerchantInventory inventory && inventory.getMerchant() instanceof AbstractVillager villager
                && blocked(villager)) { e.setCancelled(true); if (e.getPlayer() instanceof Player player) denied(player); }
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void trade(PlayerTradeEvent e) {
        if (blocked(e.getVillager())) { e.setCancelled(true); denied(e.getPlayer()); }
    }
    @EventHandler public void quit(PlayerQuitEvent e) { feedback.remove(e.getPlayer().getUniqueId()); }
}
```

## Integrasi yang dilakukan pemasang

Pemasang menambahkan `NpcModule.start(this, viti)` setelah startup evolusi, bisikan, atau Viti yang ditemukan pada main file. Saat shutdown, `NpcModule.stop(this)` dijalankan sebelum Viti ditutup. Jika versi main file sudah memulai NpcService secara langsung dan memiliki startup, registrasi event, serta close yang lengkap, susunan itu dipertahankan.

Metode `reward(Player, UUID, BigDecimal, Consumer<String>)` ditambahkan ke VitiService hanya bila belum ada. Metode ini memakai receipt pada ledger redeemed dan mempertahankan seluruh metode giveaway yang sudah kamu miliki. Command dan permission NPC ditambahkan pada plugin.yml; softdepend ModelEngine/Shopkeepers digabung dengan daftar sebelumnya. Pada build.gradle.kts ditambahkan manifest namespace Mojang untuk refleksi NPC manusia Paper 1.21.4. Konfigurasi Gradle lain dipertahankan.

Full replacement main file yang mengklaim identik dengan versi 328 baris belum diberikan, karena sumber versi itu belum ada. Pemasang ini memberi integrasi yang dapat digunakan tanpa membuat klaim tersebut. Untuk pemeriksaan persis bagian yang hilang dari 328 ke 247, kirim sumber 328 baris aslinya setelah modul NPC ini tersimpan.

## Verifikasi

`build` berhasil pada salinan proyek yang tersedia dengan Java 21 dan dependensi Paper 1.21.4. Ada 152 tes yang lulus, termasuk 20 tes NPC untuk kuota lintas jenis, cooldown, hadiah sekali, receipt, reset, validasi dialog/ladang, dan penyimpanan. Angka ini berlaku untuk sumber yang tersedia dalam pengujian ini, bukan hasil membandingkan proyek Windows atau main file 328 baris milikmu.

Pemasang diperiksa melalui PowerShell: struktur proyek sebelum NPC dan struktur NpcService lama, hasil ekstraksi kode, penggabungan command/permission YAML, pemeliharaan isi main/VitiService, pemasangan berulang tanpa duplikasi, serta penolakan struktur main yang tidak dikenali sebelum ada file yang berubah. Pemeriksaan script dijalankan dengan PowerShell 7.5.2; script memakai sintaks dasar yang ditujukan juga untuk Windows PowerShell 5.1. Lingkungan Windows milikmu belum dijalankan di sini.

Pengujian server belum dijalankan. Ini mengikuti rencanamu untuk mencoba di server setelah seluruh plugin siap. Prioritas pengujian server: NPC skin dan model, kamera/dialog scroll, trigger yang di-cancel, satu hadiah per player, tiga quest lintas jenis, cooldown, giliran area, disconnect/death, pemulihan ladang, dan inventory penuh.

Referensi API: [Paper 1.21.4 PlayerProfile](https://jd.papermc.io/paper/1.21.4/com/destroystokyo/paper/profile/PlayerProfile.html), [Paper mappings namespace](https://docs.papermc.io/paper/dev/userdev/), [ModelEngine 4 BaseEntityInteractEvent](https://ticxo.github.io/Model-Engine-4.0-JavaDocs/com/ticxo/modelengine/api/events/BaseEntityInteractEvent.html).
