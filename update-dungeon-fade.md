# Vitaemanager — update dungeon cerita dan screen fade

Untuk Paper **1.21.4**, Java **21**, dengan integrasi opsional MythicMobs **5.11.2**. Dokumen ini berisi source lengkap setiap file yang dipasang, termasuk `Vitaemanager.java`, dan installer PowerShell. Tidak perlu mencari tempat menempelkan potongan Java.

## Yang dibuat

| Bagian | Perilaku |
|---|---|
| Screen fade | Gambar hitam melalui font resource pack; bisa ON/OFF. Batas waktu tiap transisi dan pembersihan dari console. |
| Aksi NPC | Pilihan dialog bisa teleport atau memanggil mob vanilla/MythicMobs di posisi admin. |
| Portal masuk | Seleksi pos1–pos2 menuju world/lokasi dungeon yang disediakan admin. Pemain gagal ditolak sampai sesi berikutnya. |
| Jembatan kaca | Satu peserta menerima → semua peserta aktif di world dipindah ke start → kamera masing-masing 5 detik → satu timer bersama 180 detik. |
| Pijakan salah | Petak kaca 3×3 langsung hilang. Pemain dijatuhkan dan gerak horizontal dikunci agar tidak melompat kabur. |
| Tahap minion | Satu penerimaan membuka gerbang; crossing pertama memanggil mob di semua titik. Progres bersama menghitung UUID mob sesi. |
| Tahap bos | Memerlukan tahap kaca dan minion selesai; gerbang terbuka, crossing pertama memanggil bos sekali. |
| Hadiah | Template item dari GUI; dipilih satu penerima per tahap. Mendahulukan peserta yang belum memperoleh hadiah, lalu membolehkan pengulangan jika tidak ada calon lain. |
| Keluar | Setelah hadiah bos diterima: ancaman runtuh, bossbar 30 detik; portal muncul setelah **15 detik berlalu**, disertai pemberitahuan. Waktu habis membunuh peserta yang belum keluar meskipun mempunyai totem. |
| Pemulihan | Reset/stop/restart menghentikan sesi serta memulihkan kaca, gerbang dan portal sementara. Penempatan blok ditunda bila masih ditempati pemain. |

Proteksi berlaku dalam world dungeon yang ON: tidak bisa membangun/menghancurkan blok, menggunakan ender pearl, chorus fruit, kendaraan, elytra/flying, dan `/carry`. **Tidak ada pemblokiran khusus skill senjata MythicMobs**, sesuai keputusanmu; briefing RP dilakukan manual. Skill mob tetap dapat berjalan, sedangkan kerusakan blok arena dari ledakan dicegah.

Mob vanilla alami dan spawner otomatis dicegah dalam world yang didaftarkan; gamerule mob spawning menjadi false. Mob yang dipanggil oleh aksi/adegan dungeon menggunakan jalur spawn khusus. Progress minion/bos hanya menghitung mob yang dibuat tahap tersebut, bukan semua mob dalam world atau summon tambahan dari skill.

Mati atau disconnect saat permainan berlangsung menggagalkan peserta. Kematian memakai respawn normal pemain; sesi yang dihentikan mengembalikan peserta yang masih aktif ke `home`. Jika seluruh peserta gagal, arena dipulihkan otomatis. Permainan dapat dibuka lagi oleh admin setelah pemulihan selesai.

Fade hanya diminta pada masuk/keluar dungeon jika admin mengaktifkannya, atau pada aksi teleport NPC yang diberi opsi fade. Kamera, kaca, pertarungan, dan perpindahan internal antartahap tidak memakai fade.

## Pasang di proyek IntelliJ

1. Download **update-dungeon-fade.md** dan letakkan sejajar `gradlew.bat` dalam folder proyek `vitaemanager`.
2. Buka PowerShell di folder tersebut dan jalankan:

```powershell
$m=Get-Content -LiteralPath .\update-dungeon-fade.md -Raw -Encoding UTF8; $i=[regex]::Match($m,'(?s)<!-- VITAE_INSTALLER_BEGIN -->\r?\n```powershell\r?\n(.*?)\r?\n```\r?\n<!-- VITAE_INSTALLER_END -->'); if(!$i.Success){throw 'Blok installer tidak ditemukan'}; & ([scriptblock]::Create($i.Groups[1].Value)) -ProjectPath (Get-Location).Path
```

Untuk mengecek tanpa mengubah file, tambahkan `-CheckOnly` di ujung command di atas. Installer memeriksa semua target sebelum menulis. Sumber yang berubah manual dan tidak dikenali ditolak tanpa menimpanya. Sumber lama dicadangkan dalam `vitae-dungeon-backup-...`; kegagalan penulisan memicu usaha pemulihan dari backup. Memasang ulang update yang sama tidak menulis ulang file yang sudah sesuai.

Sumber didasarkan pada repository `ZekaZss/vitaemanager`, commit `2746e4c611be4004fa11e1d6b3666971d580b94c`, ditambah update evo, gerbang, NPC cerita, ladang besar, dan bisikan sinematik dari dokumen kita sebelumnya. Installer juga membawa source pendukung versi terakhir itu agar modul lama tetap lengkap bila sumber GitHub belum memuatnya. Pada proyek yang sudah mempunyai file tersebut, file yang identik dilewati. Dependency Gradle tidak diubah dan tidak ada dependency Mockito ditambahkan ke proyekmu.

3. Setelah pesan pemasangan berhasil, jalankan:

```powershell
.\gradlew.bat clean build
```

4. Matikan server, ambil JAR baru dari `build/libs`, lalu ganti JAR Vitaemanager lama di folder `plugins`. **Pertahankan folder `plugins/vitaemanager` beserta isinya** dan nyalakan server kembali. Jangan memakai `/reload` server untuk mengganti JAR.

Installer hanya menyentuh source/resource/test di proyek. Viti, NPC, quest ladang/mob lama, evo, bisikan, carry, giveaway, pickup jongkok, dan blokir villager trading tetap dibawa. Data runtime lama tidak dikirim ulang atau dihapus; dungeon dan fade menggunakan file baru `dungeons.json` dan `fade.json` di folder plugin yang sama. NPC lama tetap dapat digunakan untuk kebutuhan sebelumnya.

## Aktifkan fade dengan ItemsAdder

Fade utama **OFF** saat pertama dipasang. Resource pack hitam sudah disertakan dalam JAR; tidak perlu membuat ulang gambar Canva atau memasang BetterHUD.

Di server yang sudah mempunyai ItemsAdder:

```text
/vitae fade pack
/iazip
```

Command pertama mengekspor asset dan menempatkannya di:

`plugins/ItemsAdder/contents/vitae_cinematic_fade/resourcepack/assets/vitae_cinematic/`

Konfigurasi namespace kecil dibuat di `contents/vitae_cinematic_fade/configs/fade.yml` jika belum ada. File utama font ialah `font/screen.json`; tekstur ialah `textures/font/black_screen.png`. Asset memakai font `vitae_cinematic:screen` sendiri dan tidak mengganti shader atau font default Minecraft. Struktur `resourcepack/assets` mengikuti [struktur contents ItemsAdder](https://wiki.itemsadder.com/plugin-usage/beginners/configs-and-resourcepack/).

Tunggu `/iazip` selesai, lalu muat ulang/download resource pack terbaru pada client, misalnya rejoin mengikuti pengaturan pack server. Setelah client menerima pack:

```text
/vitae fade on
/vitae fade preview
/vitae fade times 0.75 0.25 0.75
```

Urutan `times`: waktu menggelap, menahan hitam, lalu kembali terang; angka memakai detik. Masing-masing 0.05–5 detik. Pemain yang belum menerima resource pack diteleport langsung.

Matikan dan sekaligus bersihkan overlay yang sedang tampil:

```text
/vitae fade off
```

Jika pemain tersangkut layar hitam, console panel dapat menjalankan **tanpa slash**:

```text
vitae fade clear
```

Di game command-nya `/vitae fade clear`. Ini membersihkan title seluruh pemain; teleport milik fade yang masih menunggu tetap diselesaikan. Karena memakai title, title cerita lain yang sedang tampil juga ikut dibersihkan oleh command darurat global ini.

## Setup dungeon dari nol

Contoh ID dungeon: `dungeon_rp`. Contoh world: `dungeon_world`. Ganti nama world dan ID MythicMob dengan yang benar-benar tersedia di servermu. World dibuat dan dimuat **manual** dahulu melalui pengelola world yang kamu gunakan. Vitae tidak membuat atau menghapus world.

Admin menggunakan permission `vitae.admin.dungeon`, `vitae.admin.npc`, dan `vitae.admin.fade` (default OP). Peserta tetap memerlukan `vitae.npc`, default true. Permission `vitae.dungeon.bypass` default false, termasuk untuk OP. Bila admin ingin menonton tanpa menjadi peserta, berikan bypass pada akun penonton lewat pengelola permission; jangan berikan pada peserta.

```text
/vdungeon create dungeon_rp dungeon_world
/vdungeon help
```

Semua perubahan setup menjadikan dungeon OFF hingga diaktifkan lagi. Setup tidak bisa diganti saat sesi aktif atau sedang memulihkan arena. Jalankan command satu per satu dan baca petunjuk berwarna yang muncul.

### A. Cara menandai seleksi

Untuk setiap area dalam tabel berikut, pilih sudut A dan B, lalu simpan dengan `region`:

```text
/vdungeon dungeon_rp pos1
/vdungeon dungeon_rp pos2
/vdungeon dungeon_rp region entrance
```

Saat `pos1`/`pos2`, crosshair yang menunjuk blok dalam 6 block akan memilih blok tersebut. Jika tidak ada blok yang dibidik, posisi kaki admin dipakai. Untuk pemicu berjalan, pilih area yang mencakup **ruang kaki/badan pemain**, bukan hanya blok lantai. Dua sudut harus berada di world yang sama. Seleksi terakhir boleh dipakai untuk command area berikutnya, tetapi pilih ulang bila lokasinya berbeda.

| Nama `region` | Area yang ditandai |
|---|---|
| `entrance` | Ruang portal masuk di world asal. Admin boleh memasang end gateway visual di sini. |
| `arena` | Kubus seluruh jembatan kaca, termasuk ruang start, finish, dan bagian bawah tempat jatuh. |
| `camera` | Pemicu pengenalan kamera **yang mencakup posisi start seluruh peserta**. |
| `finish` | Ruang yang dilewati pemain setelah melewati semua kaca. |
| `miniongate` | Hanya dinding yang akan hilang ketika menerima minion. |
| `miniontrigger` | Ruang setelah melewati gerbang minion. |
| `bossgate` | Hanya dinding yang akan hilang ketika menerima bos. |
| `bosstrigger` | Ruang setelah melewati gerbang bos. |
| `exit` | Bagian dalam bingkai portal keluar, **seluruhnya udara**, tidak termasuk bingkai. |

Semua area selain entrance berada di world dungeon. Gerbang tidak boleh memuat chest/blok penyimpan data, bedrock, barrier, atau blok khusus yang tidak sesuai untuk pemulihan. Gerbang dan portal keluar tidak boleh tumpang tindih. Batas snapshot gerbang 32768 blok; portal keluar 256 blok. Ini membatasi blok yang diubah, bukan ukuran world/arena.

Simpan lokasi dengan berdiri tepat di tempat yang dituju, lalu arahkan kamera admin sebagaimana diperlukan:

| Command | Tempat admin berdiri |
|---|---|
| `/vdungeon dungeon_rp location entry` | Tempat peserta pertama tiba dalam world dungeon, dekat NPC kaca. |
| `/vdungeon dungeon_rp location home` | Tujuan pulang di **luar world dungeon**. |
| `/vdungeon dungeon_rp location start` | Start kaca, di dalam seleksi arena dan camera. |
| `/vdungeon dungeon_rp location camera` | Posisi kamera yang memperlihatkan jembatan; yaw/pitch admin ikut disimpan. |
| `/vdungeon dungeon_rp location next` | Tempat peserta yang sudah melewati finish menunggu/bertemu NPC minion. |

Kamera bergerak dari start ke view tersebut lalu kembali ke start; total 5 detik. Semua peserta menerima pengenalan masing-masing secara serentak. Karena start harus berada di pemicu camera, timer tidak dimulai lebih awal untuk peserta yang menerima quest.

### B. Pijakan salah 3×3

Bangun jembatan dan pijakan kaca secara manual. Untuk setiap petak salah: bidik dua blok sudut yang berseberangan pada **sembilan blok kaca 3×3, satu ketinggian**, kemudian:

```text
/vdungeon dungeon_rp pos1
/vdungeon dungeon_rp pos2
/vdungeon dungeon_rp bad add salah_01
```

Ulangi dengan `salah_02`, `salah_03`, dan seterusnya. Pijakan benar tidak perlu ditandai. Petak salah tidak boleh bertumpuk dan harus di dalam arena. Untuk menghapus tanda:

```text
/vdungeon dungeon_rp bad remove salah_01
```

Ketika salah, kaca segera dihapus, pemain dipindah sedikit ke bawah petak dan dipaksa jatuh. Jangan menaruh lantai aman persis di bawah jembatan; sediakan ruang jatuh agar adegannya sesuai. Pemain yang terjatuh tetap tidak bisa melanjutkan sampai sesi di-reset.

### C. Tiga NPC

NPC dungeon memakai **NPC milik Vitae**, bisa skin atau model ModelEngine seperti fitur lama. Gunakan NPC baru khusus tiga tahap, tanpa quest ladang/mob lama, hadiah dialog biasa, atau gerbang lama. Gerbang tahap dikelola oleh dungeon, sehingga pilihan memakai `DUNGEON_...`, bukan `OPEN` atau `QUEST`.

Berdiri di depan lokasi NPC kaca. Create menempatkan NPC dua blok di depanmu dan menyimpan tempatmu sebagai posisi dialog:

```text
/vnpc create penjaga_kaca &bPenjaga Jembatan
/vnpc penjaga_kaca page start Untuk melanjutkan, kalian harus memilih pijakan yang benar. Kalian siap?
/vnpc penjaga_kaca clearoptions start
/vnpc penjaga_kaca option start DUNGEON_GLASS Kami siap.
/vnpc penjaga_kaca option start END Belum sekarang.
/vnpc penjaga_kaca page selesai Selamat! Kamu berhasil melewati jembatan. Ini hadiah untukmu.
/vnpc penjaga_kaca reward dialog none
/vnpc penjaga_kaca track on
/vnpc penjaga_kaca on
/vdungeon dungeon_rp npc glass penjaga_kaca selesai
```

Pindah ke lokasi NPC minion, lalu:

```text
/vnpc create penjaga_minion &6Penjaga Ruang Minion
/vnpc penjaga_minion page start Musnahkan seluruh minion di balik gerbang ini bersama rekan-rekanmu. Kalian siap?
/vnpc penjaga_minion clearoptions start
/vnpc penjaga_minion option start DUNGEON_MINION Kami akan membersihkan ruangan itu.
/vnpc penjaga_minion option start END Kami perlu bersiap dahulu.
/vnpc penjaga_minion page selesai Seluruh minion telah dikalahkan. Ambillah hadiah ini.
/vnpc penjaga_minion reward dialog none
/vnpc penjaga_minion track on
/vnpc penjaga_minion on
/vdungeon dungeon_rp npc minion penjaga_minion selesai
```

Pindah ke lokasi NPC bos, lalu:

```text
/vnpc create penjaga_bos &cPenjaga Ruang Terakhir
/vnpc penjaga_bos page start Jika kalian telah melewati jembatan dan mengalahkan seluruh minion, gerbang terakhir akan kubuka.
/vnpc penjaga_bos clearoptions start
/vnpc penjaga_bos option start DUNGEON_BOSS Kami berhasil menyelesaikan kedua tantangan.
/vnpc penjaga_bos option start END Tunggu, kami belum siap.
/vnpc penjaga_bos page selesai Kalian berhasil mengalahkan penguasa dungeon. Ambillah hadiahmu, lalu segera pergi!
/vnpc penjaga_bos reward dialog none
/vnpc penjaga_bos track on
/vnpc penjaga_bos on
/vdungeon dungeon_rp npc boss penjaga_bos selesai
```

Pilihan **Ambil hadiah** ditambahkan otomatis untuk penerima yang terpilih. Tidak perlu menambahkannya ke dialog semua pemain. Memilih keluar atau membatalkan dialog tidak menghabiskan jatah penerimaan hadiah; klik NPC kembali untuk melanjutkan klaim.

Untuk memakai model knight pada NPC, jalankan sebelum ON akhir:

```text
/vnpc penjaga_kaca model knight
/vnpc penjaga_kaca on
```

Lokasi NPC dapat diganti dengan `move`; tempat pemain berdialog/kembali menerima hadiah disimpan dengan `/vnpc <id> stand`. Tempat dialog harus aman, berada dekat NPC, dan tidak berada dalam gerbang. Setelah mengubah NPC pastikan ON lagi. Preview `/vnpc <id> preview` hanya mengecek dialog; aksi dungeon tidak dijalankan saat preview.

### D. Empat lokasi minion dan satu bos

Berdiri di tiap lokasi spawn dan jalankan satu command. Ini contoh; `DungeonMinion` dan `DungeonBoss` harus diganti ID mob MythicMobs milikmu:

```text
/vdungeon dungeon_rp spawn titik_1 mythic:DungeonMinion 3
/vdungeon dungeon_rp spawn titik_2 mythic:DungeonMinion 3
/vdungeon dungeon_rp spawn titik_3 mythic:DungeonMinion 3
/vdungeon dungeon_rp spawn titik_4 mythic:DungeonMinion 3
```

Total contoh 12 minion. Jumlah boleh berbeda per lokasi dan jenisnya boleh dicampur. Contoh mob vanilla:

```text
/vdungeon dungeon_rp spawn titik_4 vanilla:ZOMBIE 4
```

Nama titik yang sama mengganti konfigurasi titik tersebut. Menghapusnya:

```text
/vdungeon dungeon_rp spawn remove titik_4
```

Berdiri di posisi bos muncul:

```text
/vdungeon dungeon_rp boss mythic:DungeonBoss 1
```

Angka terakhir opsional adalah **level**, bukan jumlah bos. Bos selalu satu. Jangan memberi konfigurasi despawn otomatis pada mob wajib sebelum terbunuh: jika mob sesi menghilang tanpa kematian tercatat, sesi dihentikan dan arena dipulihkan agar progress tidak macet. Mob dipanggil bertahap dalam batch kecil, dengan chunk spawn dipertahankan selama sesi.

### E. Tiga hadiah item dan waktu

```text
/vdungeon dungeon_rp reward glass
```

Terbuka GUI 27 slot. Letakkan item custom/vanilla yang menjadi hadiah, lalu tutup. Item admin dikembalikan; salinan lengkapnya menjadi template hadiah. Ulangi:

```text
/vdungeon dungeon_rp reward minion
/vdungeon dungeon_rp reward boss
```

Semua slot yang diisi menjadi paket hadiah satu tahap. Item mempertahankan metadata yang diserialisasikan Paper, termasuk data item custom. Hadiah masuk inventory; sisa yang tidak muat dijatuhkan dengan pemilik penerima.

```text
/vdungeon dungeon_rp time glass 180
/vdungeon dungeon_rp time escape 30
/vdungeon dungeon_rp time portal 15
```

Portal muncul pada detik ke-15 sejak ancaman dimulai, menyisakan sekitar 15 detik untuk keluar. Seleksi portal hanya mengganti udara dengan end gateway. World tidak benar-benar diruntuhkan.

Jika ingin fade saat masuk dan pulang, setelah resource pack siap:

```text
/vdungeon dungeon_rp fade entry on
/vdungeon dungeon_rp fade exit on
```

Sakelar global `/vitae fade on` tetap harus aktif. Bila tidak ingin fade, gunakan `off` pada kedua flag tersebut.

### F. Validasi dan mulai

```text
/vdungeon dungeon_rp check
/vdungeon dungeon_rp on
/vdungeon dungeon_rp open
/vdungeon dungeon_rp status
```

`check` menjelaskan bagian setup yang belum lengkap, lokasi terhalang, NPC OFF, ID mob yang salah, atau seleksi yang tidak cocok. Setelah `open`, peserta masuk lewat portal. Satu peserta memilih menerima di NPC kaca; seluruh peserta aktif di world mengikuti. Orang yang baru mencoba masuk setelah permainan dimulai ditolak.

Saat first finisher mencapai finish, pemain lain yang mencapai finish pada tick server yang sama ikut diundi. Pemain pertama memperoleh dialog hadiah; finish berikutnya menuju `next`. Setelah peserta lain finish atau gagal, tahap minion bisa diterima. Minion/bos tidak muncul saat NPC diklik: NPC membuka gerbang, lalu trigger crossing pertama memulai spawn.

Hadiah minion memprioritaskan penerima quest awal selama dia masih layak dan belum memperoleh hadiah. Jika tidak, penerima diundi. Hadiah bos diundi dari peserta yang masih aktif dan lolos kaca. Penerima yang belum memperoleh hadiah diprioritaskan; jika seluruh calon sudah memperoleh, hadiah boleh diterima lagi. Penerima yang disconnect diberi batas tunggu 30 detik lalu dipilih penggantinya dari peserta yang tersisa.

Untuk mencoba lagi:

```text
/vdungeon dungeon_rp reset
/vdungeon dungeon_rp status
```

Tunggu status pemulihan selesai, lalu:

```text
/vdungeon dungeon_rp open
```

`stop` juga menghentikan sesi dan memulihkan arena. `off` menghentikan permainan aktif serta mematikan proteksi/portal dungeon itu. Satu dungeon aktif diperbolehkan per world; gunakan world berbeda untuk dungeon lain. Gamerule spawn alami yang dimatikan tidak diaktifkan kembali otomatis oleh `off`.

## Aksi teleport/spawn NPC di luar dungeon

Ini dapat dipakai untuk adegan RP lain. Berdiri di tujuan teleport:

```text
/vdungeon action ke_ruang penjaga_kaca teleport here off
/vnpc penjaga_kaca option start ACTION_ke_ruang Pergi ke ruangan itu.
/vnpc penjaga_kaca on
```

Gunakan NPC tersendiri untuk adegan terpisah; contoh nama di atas hanya menunjukkan bentuk command. Opsi akhir `on` meminta fade bila sakelar global dan pack aktif; `off` teleport langsung. Pilihan NPC harus menggunakan ID aksi yang sama persis.

Berdiri di lokasi mob muncul:

```text
/vdungeon action panggil_mob npc_adegan spawn mythic:DungeonMinion 2 1
/vnpc npc_adegan option start ACTION_panggil_mob Panggil penjaga.
/vnpc npc_adegan on
```

`npc_adegan` harus sudah dibuat. Aksi biasa ini dapat diulang ketika dipilih lagi, dengan jeda singkat antarklik; gunakan tahap dungeon untuk mob dan hadiah yang hanya boleh berjalan sekali per sesi.

## Perintah pemulihan dan batas pengujian

| Command | Kegunaan |
|---|---|
| `/vitae fade clear` | Membersihkan overlay semua pemain; console: `vitae fade clear`. |
| `/vitae fade off` | Mematikan fade dan menyelesaikan transisi yang menunggu. |
| `/vdungeon <id> status` | Status tahap, jumlah peserta dan progress mob. |
| `/vdungeon <id> stop` atau `reset` | Menghentikan sesi, memulangkan peserta aktif, memulihkan snapshot. |
| `/vdungeon <id> receipt boss` | Melihat status pengiriman hadiah; stage bisa glass/minion/boss. |
| `/vitae dungeon reload` | Memuat ulang modul story; sesi berjalan dihentikan dan dipulihkan. |
| `/vdungeon help <topik>` | Help portal/kaca/minion/bos/hadiah/aksi/sesi. |

Pengiriman item dicatat sebelum dan sesudah pemberian, dengan receipt di playerdata untuk menghindari hadiah berulang. Pada crash tepat ketika transaksi item berlangsung, pengiriman yang belum pasti ditahan untuk pemeriksaan admin. `receipt <stage> retry` hanya digunakan jika dipastikan item belum diterima; `delivered` jika dipastikan sudah diterima. Pemeriksaan ini menghindari duplikasi otomatis dalam kondisi crash yang tidak dapat dipastikan.

Build Java dan 179 test lama lulus; tambahan 19 pengujian simulasi lokal memeriksa timer bersama, finish bersamaan, kaca jatuh, spawn tunggal, UUID mob, totem, fallback hadiah, penolakan teleport, pemulihan kamera/restart, reset berulang, portal 15/30 detik, serta fade OFF/clear/timeout. Simulator tambahan tidak menambahkan dependency ke proyekmu. API spawn dan event MythicMobs diperiksa terhadap artefak resminya versi 5.11.2.

**Tampilan kamera/fade, skin/model, item custom, dan skill mob tetap memerlukan uji langsung pada servermu.** Build sukses tidak berarti visual client dan semua konfigurasi plugin lain sudah diuji. Mulai dengan sesi percobaan bersama dua akun sebelum adegan RP utama.

## Source lengkap dan installer

Bagian di bawah adalah satu installer lengkap yang dibaca command pemasangan. Jangan menyalin isi Java satu per satu. PNG disimpan dalam Base64 dan dipulihkan menjadi file gambar oleh installer.

<!-- VITAE_INSTALLER_BEGIN -->
```powershell
[CmdletBinding()]
param([string]$ProjectPath = (Get-Location).Path, [switch]$CheckOnly)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2.0
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
if (-not (Test-Path -LiteralPath (Join-Path $rootVitae 'gradlew.bat') -PathType Leaf) -or
    -not (Test-Path -LiteralPath (Join-Path $rootVitae 'build.gradle.kts') -PathType Leaf)) {
    throw 'Jalankan dari folder proyek Vitaemanager, sejajar gradlew.bat dan build.gradle.kts. Tidak ada file yang diubah.'
}
$filesVitae = @(
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/Vitaemanager.java'
        OriginalHashes = @('d04e0ed4353dc35fa441d8a9f3c0ca3b7b66651611b2ab22c1a3c4360d346ed7')
        Hash = '1a9e800896840422b8532b8e55cbc18a10be2cb934620b6a31aa7280371a5079'
        Required = $true
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager;

import com.bangzachery.vitae.vitaemanager.command.EvolutionCommand;
import com.bangzachery.vitae.vitaemanager.command.RoleplayCommand;
import com.bangzachery.vitae.vitaemanager.command.VitaeCommand;
import com.bangzachery.vitae.vitaemanager.command.VitiCommand;
import com.bangzachery.vitae.vitaemanager.command.WhisperCommand;
import com.bangzachery.vitae.vitaemanager.core.config.ConfigurationService;
import com.bangzachery.vitae.vitaemanager.core.message.MessageService;
import com.bangzachery.vitae.vitaemanager.economy.VitiListener;
import com.bangzachery.vitae.vitaemanager.economy.VitiPaper;
import com.bangzachery.vitae.vitaemanager.economy.VitiService;
import com.bangzachery.vitae.vitaemanager.economy.VitiStore;
import com.bangzachery.vitae.vitaemanager.evolution.EvolutionService;
import com.bangzachery.vitae.vitaemanager.integration.EssentialsListener;
import com.bangzachery.vitae.vitaemanager.items.ItemCatalog;
import com.bangzachery.vitae.vitaemanager.items.ItemRulesListener;
import com.bangzachery.vitae.vitaemanager.items.ItemRulesService;
import com.bangzachery.vitae.vitaemanager.items.ItemRulesStore;
import com.bangzachery.vitae.vitaemanager.profile.ProfilePanel;
import com.bangzachery.vitae.vitaemanager.roleplay.CarryService;
import com.bangzachery.vitae.vitaemanager.roleplay.RoleplayService;
import com.bangzachery.vitae.vitaemanager.servercontrol.ServerControlListener;
import com.bangzachery.vitae.vitaemanager.servercontrol.ServerControlService;
import com.bangzachery.vitae.vitaemanager.servercontrol.ServerControlStore;
import com.bangzachery.vitae.vitaemanager.servercontrol.gui.ServerControlPanel;
import com.bangzachery.vitae.vitaemanager.totem.TotemLimitStore;
import com.bangzachery.vitae.vitaemanager.totem.TotemListener;
import com.bangzachery.vitae.vitaemanager.totem.TotemService;
import com.bangzachery.vitae.vitaemanager.whisper.WhisperService;
import com.bangzachery.vitae.vitaemanager.whisper.WhisperStore;

import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class Vitaemanager extends JavaPlugin {
    private ServerControlService controls;
    private ServerControlListener controlListener;
    private ServerControlPanel panel;

    private VitiService viti;
    private VitiListener vitiListener;

    private ProfilePanel profiles;
    private RoleplayService roleplay;
    private CarryService carry;
    private TotemService totems;

    private ItemRulesService itemRules;
    private ItemRulesListener itemListener;

    private WhisperService whispers;
    private EvolutionService evolution;
    private com.bangzachery.vitae.vitaemanager.story.StoryModule story;

    @Override
    public void onEnable() {
        try {
            getServer().getPluginManager().registerEvents(new com.bangzachery.vitae.vitaemanager.integration.VillagerTradeListener(), this);
            String defaults;

            try (InputStream resource = getResource("config.yml")) {
                if (resource == null) {
                    throw new IllegalStateException(
                            "Resource config.yml tidak ditemukan dalam JAR");
                }

                defaults = new String(
                        resource.readAllBytes(),
                        StandardCharsets.UTF_8
                );
            }

            ConfigurationService configuration = new ConfigurationService(
                    getDataFolder().toPath().resolve("config.yml"),
                    defaults
            );
            configuration.initialize();

            MessageService messages = new MessageService(configuration);

            ServerControlStore store = new ServerControlStore(
                    getDataFolder().toPath().resolve("server-control.yml")
            );

            controls = new ServerControlService(
                    this,
                    store,
                    store.initialize(
                            getDataFolder().toPath().resolve("config.yml")),
                    messages
            );

            controlListener = new ServerControlListener(
                    this, controls, messages);

            VitiStore vitiStore = new VitiStore(
                    getDataFolder().toPath().resolve("viti.yml")
            );

            VitiPaper moneyPaper = new VitiPaper(this);

            viti = new VitiService(
                    this, vitiStore, moneyPaper, vitiStore.initialize());

            var itemCatalog = ItemCatalog.fromPaper();

            var rulesStore = new ItemRulesStore(
                    getDataFolder().toPath().resolve("blocker/blocked.yml"),
                    getDataFolder().toPath().resolve("config.yml"),
                    itemCatalog
            );

            itemRules = new ItemRulesService(
                    this, rulesStore, rulesStore.initialize(), itemCatalog);

            itemListener = new ItemRulesListener(
                    this, configuration, messages, itemRules);

            profiles = new ProfilePanel(
                    this,
                    messages,
                    viti,
                    player -> panel.open(player),
                    itemRules
            );

            panel = new ServerControlPanel(
                    this, controls, messages, profiles::openPlayers);

            TotemLimitStore totemStore = new TotemLimitStore(
                    getDataFolder().toPath().resolve("totem-settings.yml")
            );

            totems = new TotemService(
                    this, configuration, totemStore, totemStore.load());

            var vanillaSounds = org.bukkit.Registry.SOUNDS.stream()
                    .map(sound ->
                            org.bukkit.Registry.SOUNDS
                                    .getKeyOrThrow(sound).toString())
                    .collect(Collectors.toUnmodifiableSet());

            WhisperStore whisperStore = new WhisperStore(
                    getDataFolder().toPath().resolve("whispers.yml"),
                    vanillaSounds
            );

            whispers = new WhisperService(
                    this, whisperStore, whisperStore.initialize());

            WhisperCommand whisperCommand = new WhisperCommand(
                    whispers, messages);

            evolution = new EvolutionService(
                    this, itemRules, messages, vanillaSounds);

            EvolutionCommand evolutionCommand = new EvolutionCommand(
                    evolution);

            PluginCommand evoCommand = getCommand("evo");

            if (evoCommand == null) {
                throw new IllegalStateException(
                        "Command evo tidak terdaftar di plugin.yml");
            }

            evoCommand.setExecutor(evolutionCommand);
            evoCommand.setTabCompleter(evolutionCommand);

            VitaeCommand executor = new VitaeCommand(
                    configuration,
                    messages,
                    getLogger(),
                    controls,
                    panel,
                    totems,
                    itemRules,
                    whisperCommand
            );

            PluginCommand command = getCommand("vitae");

            if (command == null) {
                throw new IllegalStateException(
                        "Command vitae tidak terdaftar di plugin.yml");
            }

            command.setExecutor(executor);
            command.setTabCompleter(executor);

            PluginCommand adminPanel = getCommand("adminpanel");

            if (adminPanel == null) {
                throw new IllegalStateException(
                        "Command adminpanel tidak terdaftar di plugin.yml");
            }

            adminPanel.setExecutor(executor);
            adminPanel.setTabCompleter(executor);

            getServer().getPluginManager().registerEvents(
                    controlListener, this);
            getServer().getPluginManager().registerEvents(panel, this);
            controlListener.start();

            vitiListener = new VitiListener(
                    this, viti, moneyPaper, messages);

            VitiCommand vitiExecutor = new VitiCommand(viti, messages);
            PluginCommand vitiCommand = getCommand("viti");

            if (vitiCommand == null) {
                throw new IllegalStateException(
                        "Command viti tidak terdaftar di plugin.yml");
            }

            vitiCommand.setExecutor(vitiExecutor);
            vitiCommand.setTabCompleter(vitiExecutor);

            getServer().getPluginManager().registerEvents(
                    vitiListener, this);
            vitiListener.start();

            getServer().getPluginManager().registerEvents(profiles, this);
            profiles.start();

            roleplay = new RoleplayService(this, configuration);

            carry = new CarryService(
                    this, configuration, messages, roleplay);

            RoleplayCommand roleplayExecutor = new RoleplayCommand(
                    configuration, messages, controls, roleplay, carry);

            for (String name : List.of("me", "do", "ooc", "oocg", "carry")) {
                PluginCommand roleplayCommand = getCommand(name);

                if (roleplayCommand == null) {
                    throw new IllegalStateException(
                            "Command " + name
                                    + " tidak terdaftar di plugin.yml");
                }

                roleplayCommand.setExecutor(roleplayExecutor);
                roleplayCommand.setTabCompleter(roleplayExecutor);
            }

            getServer().getPluginManager().registerEvents(roleplay, this);
            getServer().getPluginManager().registerEvents(carry, this);

            roleplay.start();
            carry.start();

            getServer().getPluginManager().registerEvents(
                    new TotemListener(this, totems, messages),
                    this
            );

            getServer().getPluginManager().registerEvents(
                    itemListener, this);

            getServer().getPluginManager().registerEvents(
                    new EssentialsListener(configuration, messages),
                    this
            );

            itemListener.start();

            getServer().getPluginManager().registerEvents(whispers, this);
            whispers.start();

            getServer().getPluginManager().registerEvents(evolution, this);
            evolution.start();
            com.bangzachery.vitae.vitaemanager.npc.NpcModule.start(this, viti);
            story = new com.bangzachery.vitae.vitaemanager.story.StoryModule(this);
            story.start();
            executor.story(story);
            carry.guard(story::carryBlocked);

            getLogger().info(
                    "VitaeManager remake: fondasi, kontrol server, Viti, "
                            + "profil, roleplay/carry, totem, aturan item "
                            + "dan bisikan area aktif."
            );

            if (configuration.current().debug()) {
                getLogger().info(
                        "Debug aktif. Data folder: "
                                + getDataFolder().getAbsolutePath());
            }
        } catch (IOException | InvalidConfigurationException
                 | IllegalArgumentException | IllegalStateException exception) {
            getLogger().log(
                    Level.SEVERE,
                    "VitaeManager gagal diaktifkan.",
                    exception
            );

            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (story != null) story.close();
        com.bangzachery.vitae.vitaemanager.npc.NpcModule.stop(this);
        if (evolution != null) evolution.close();
        if (whispers != null) whispers.close();

        if (itemListener != null) itemListener.close();
        if (itemRules != null) itemRules.close();

        if (totems != null) totems.close();
        if (carry != null) carry.close();
        if (roleplay != null) roleplay.close();

        if (profiles != null) profiles.close();

        if (vitiListener != null) vitiListener.close();
        if (viti != null) viti.close();

        if (controlListener != null) controlListener.close();
        if (panel != null) panel.close();
        if (controls != null) controls.close();

        getLogger().info("VitaeManager dimatikan.");
    }
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/command/EvolutionCommand.java'
        OriginalHashes = @('54ccdc0a5715a3002a5b666d576d7e3b847e415f4e5363f19ad8d62797efa6e7', '6bd7dd5b7d383d53de4cc044d2fbae8f04459c4625d665923022f2412241a956', 'dc67723abc4c264b9912e99a77b6d57739de3f2343482c355cd50c60066ceab4')
        Hash = '9c5847ba752add6cae12d05d6d618598641756b8b79c9d71e07e339fd48aef98'
        Required = $true
        Binary = $false
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
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/command/VitaeCommand.java'
        OriginalHashes = @('4237aa3fccc14d6adfb12b26bb30d98322afab74870ada4e649c053baa645c5d')
        Hash = '03beb620a61446f28040da52eeb9d11425be429915731caf7499f5877baed5ca'
        Required = $true
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.command;

import com.bangzachery.vitae.vitaemanager.items.ItemRulesService;

import com.bangzachery.vitae.vitaemanager.core.config.ConfigurationService;
import com.bangzachery.vitae.vitaemanager.core.message.MessageService;
import com.bangzachery.vitae.vitaemanager.servercontrol.ServerControlService;
import com.bangzachery.vitae.vitaemanager.servercontrol.gui.ServerControlPanel;
import com.bangzachery.vitae.vitaemanager.totem.TotemService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class VitaeCommand implements CommandExecutor, TabCompleter {
    private final ConfigurationService configuration;
    private final MessageService messages;
    private final Logger logger;
    private final ServerControlService controls;
    private final ServerControlPanel panel;
    private final TotemService totems;
    private final ItemRulesService itemRules;
    private final WhisperCommand whispers;
    private com.bangzachery.vitae.vitaemanager.story.StoryModule story;
    public void story(com.bangzachery.vitae.vitaemanager.story.StoryModule story) { this.story = story; }

    public VitaeCommand(ConfigurationService configuration, MessageService messages, Logger logger,
                        ServerControlService controls, ServerControlPanel panel, TotemService totems,
                        ItemRulesService itemRules, WhisperCommand whispers) {
        this.whispers = whispers;
        this.itemRules = itemRules;
        this.configuration = configuration;
        this.messages = messages;
        this.logger = logger;
        this.controls = controls;
        this.panel = panel;
        this.totems = totems;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("vitae.admin")) { messages.send(sender, "no-permission"); return true; }
        if (command.getName().equalsIgnoreCase("adminpanel")) {
            if (args.length == 0) openPanel(sender);
            else messages.send(sender, "usage");
            return true;
        }
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            messages.send(sender, "help");
            messages.send(sender, "server-help");
            messages.send(sender, "totem-help");
            messages.send(sender, "item-help");
            sender.sendMessage(Component.text("Dungeon: /vdungeon help • Fade layar: /vitae fade help", NamedTextColor.AQUA));
            if (sender.hasPermission("vitae.admin.whisper")) sender.sendMessage(messages.component("prefix")
                    .append(Component.text("Bisikan Area: /vitae bisikan help", NamedTextColor.AQUA)));
            return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("fade") || action.equals("dungeon")) {
            if (story == null) { sender.sendMessage(Component.text("Modul story belum aktif.", NamedTextColor.RED)); return true; }
            return story.execute(sender, action, java.util.Arrays.copyOfRange(args, 1, args.length));
        }
        if (action.equals("bisikan")) return whispers.execute(sender, java.util.Arrays.copyOfRange(args, 1, args.length));
        if (action.equals("blokir") || action.equals("stuck")) {
            if (!allowed(sender, action.equals("blokir") ? "blocker" : "stuck")) return true;
            if (args.length != 3) { messages.send(sender, "item-help"); return true; }
            java.util.function.Consumer<String> done = key -> {
                if (sender instanceof Player player && !player.isOnline()) return;
                String detail = "";
                if (key.equals("item-rules-saved")) {
                    if (action.equals("blokir") && args[1].equalsIgnoreCase("item")) {
                        var material = itemRules.catalog().material(args[2]);
                        detail = material.name() + (itemRules.current().blockedItems().contains(material) ? " diblokir" : " diizinkan");
                    } else if (action.equals("blokir")) {
                        var type = itemRules.catalog().mob(args[2]);
                        detail = type.name() + (itemRules.current().blockedMobs().contains(type) ? " diblokir" : " diizinkan");
                    } else detail = args[1].toUpperCase(Locale.ROOT) + " batas " + args[2];
                }
                messages.send(sender, key, detail.isEmpty() ? Component.empty() : Component.text(" " + detail, NamedTextColor.WHITE));
            };
            try {
                if (action.equals("blokir")) {
                    if (args[1].equalsIgnoreCase("item")) itemRules.toggleItem(itemRules.catalog().material(args[2]), done);
                    else if (args[1].equalsIgnoreCase("mob")) itemRules.toggleMob(itemRules.catalog().mob(args[2]), done);
                    else messages.send(sender, "item-help");
                } else {
                    var material = itemRules.catalog().material(args[1]);
                    Integer limit = args[2].equalsIgnoreCase("off") ? null
                            : args[2].matches("[0-9]{1,2}") ? Integer.valueOf(args[2]) : 0;
                    if (limit != null) itemRules.catalog().limit(material, limit);
                    itemRules.stack(material, limit, done);
                }
            } catch (IllegalArgumentException exception) { messages.send(sender, "item-invalid"); }
            return true;
        }
        if (action.equals("totem")) {
            if (!allowed(sender, "totem")) return true;
            if (args.length != 2 || !args[1].matches("[0-9]{1,4}")) {
                messages.send(sender, "totem-help"); return true;
            }
            int limit = Integer.parseInt(args[1]);
            if (limit < 1 || limit > 1000) { messages.send(sender, "totem-invalid"); return true; }
            totems.setLimit(limit, key -> {
                if (!(sender instanceof Player player) || player.isOnline()) {
                    messages.send(sender, key, key.equals("totem-limit-set")
                            ? Component.text(" " + limit, NamedTextColor.WHITE) : Component.empty());
                }
            });
            return true;
        }
        if (action.equals("reset")) {
            if (!allowed(sender, "totem")) return true;
            if (args.length != 2 || !args[1].equalsIgnoreCase("totem")) {
                messages.send(sender, "totem-help"); return true;
            }
            int succeeded = 0, failed = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (totems.resetCooldown(player)) succeeded++;
                else failed++;
            }
            messages.send(sender, "totem-reset", Component.text(" " + succeeded + " berhasil, " + failed + " gagal", NamedTextColor.WHITE));
            return true;
        }
        if (action.equals("reload") && args.length == 1) {
            if (!allowed(sender, "reload")) return true;
            try {
                configuration.reload();
                messages.send(sender, "reloaded");
            } catch (IOException | InvalidConfigurationException | IllegalArgumentException exception) {
                logger.log(Level.WARNING, "Reload config.yml gagal; konfigurasi aktif dipertahankan.", exception);
                messages.send(sender, "reload-failed");
            }
            itemRules.reload(key -> {
                if (!(sender instanceof Player player) || player.isOnline()) messages.send(sender, key);
            });
            if (sender.hasPermission("vitae.admin.whisper")) whispers.execute(sender, new String[]{"reload"});
            return true;
        }
        if (action.equals("panel") && args.length == 1) { openPanel(sender); return true; }
        if (!List.of("maintenance", "chat", "pvp", "mobspawning").contains(action)) {
            messages.send(sender, "usage");
            return true;
        }
        if (!allowed(sender, action)) return true;
        boolean worldAction = action.equals("pvp") || action.equals("mobspawning");
        if (args.length < 2 || args.length > (worldAction ? 3 : 2)
                || (!args[1].equalsIgnoreCase("on") && !args[1].equalsIgnoreCase("off"))) {
            messages.send(sender, "server-help");
            return true;
        }
        boolean enabled = args[1].equalsIgnoreCase("on");
        String description = action + " " + (enabled ? "ON" : "OFF");
        if (worldAction) {
            World world = args.length == 3 ? Bukkit.getWorld(args[2])
                    : sender instanceof Player player ? player.getWorld() : null;
            if (world == null) { messages.send(sender, "world-not-found"); return true; }
            String detail = description + " | " + world.getName();
            controls.world(world, action.equals("pvp") ? enabled : null,
                    action.equals("mobspawning") ? enabled : null, result -> feedback(sender, result, detail));
        } else if (action.equals("maintenance")) {
            controls.maintenance(enabled, result -> feedback(sender, result, description));
        } else {
            controls.chatMuted(!enabled, result -> feedback(sender, result, description));
        }
        return true;
    }

    private void openPanel(CommandSender sender) {
        if (sender instanceof Player player) panel.open(player);
        else messages.send(sender, "player-only");
    }

    private boolean allowed(CommandSender sender, String action) {
        if (sender.hasPermission("vitae.admin." + action)) return true;
        messages.send(sender, "no-permission");
        return false;
    }

    private void feedback(CommandSender sender, ServerControlService.Result result, String detail) {
        if (sender instanceof Player player && !player.isOnline()) return;
        String key = switch (result) {
            case APPLIED -> "control-changed";
            case BUSY -> "control-busy";
            case FAILED -> "control-failed";
        };
        messages.send(sender, key, result == ServerControlService.Result.APPLIED
                ? Component.text(" " + detail, NamedTextColor.GRAY) : Component.empty());
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                               @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("vitae.admin") || command.getName().equalsIgnoreCase("adminpanel")) return List.of();
        if (args.length > 1 && story != null && List.of("fade", "dungeon").contains(args[0].toLowerCase(Locale.ROOT)))
            return story.complete(sender, args[0].toLowerCase(Locale.ROOT), java.util.Arrays.copyOfRange(args, 1, args.length));
        if (args.length > 1 && args[0].equalsIgnoreCase("bisikan"))
            return whispers.complete(sender, java.util.Arrays.copyOfRange(args, 1, args.length));
        List<String> choices = new ArrayList<>();
        if (args.length == 1) {
            choices.addAll(List.of("help", "panel"));
            if (sender.hasPermission("vitae.admin.fade")) choices.add("fade");
            if (sender.hasPermission("vitae.admin.dungeon")) choices.add("dungeon");
            if (sender.hasPermission("vitae.admin.whisper")) choices.add("bisikan");
            if (sender.hasPermission("vitae.admin.blocker")) choices.add("blokir");
            if (sender.hasPermission("vitae.admin.stuck")) choices.add("stuck");
            if (sender.hasPermission("vitae.admin.totem")) choices.addAll(List.of("totem", "reset"));
            for (String action : List.of("reload", "maintenance", "chat", "pvp", "mobspawning")) {
                if (sender.hasPermission("vitae.admin." + action)) choices.add(action);
            }
        } else if (args.length == 2 || args.length == 3) {
            String action = args[0].toLowerCase(Locale.ROOT);
            if (args.length == 2 && sender.hasPermission("vitae.admin.totem")) {
                if (action.equals("reset")) return "totem".startsWith(args[1].toLowerCase(Locale.ROOT)) ? List.of("totem") : List.of();
                if (action.equals("totem")) return List.of("1", "2", "3", "5", "10").stream()
                        .filter(value -> value.startsWith(args[1])).toList();
            }
            if (action.equals("blokir") && sender.hasPermission("vitae.admin.blocker")) {
                if (args.length == 2) choices.addAll(List.of("item", "mob"));
                else if (args[1].equalsIgnoreCase("item")) {
                    for (var material : itemRules.catalog().materials()) choices.add(material.name().toLowerCase(Locale.ROOT));
                } else if (args[1].equalsIgnoreCase("mob")) {
                    for (var type : itemRules.catalog().mobs()) choices.add(type.name().toLowerCase(Locale.ROOT));
                }
                return matching(choices, args);
            }
            if (action.equals("stuck") && sender.hasPermission("vitae.admin.stuck")) {
                if (args.length == 2) {
                    for (var material : itemRules.catalog().materials()) choices.add(material.name().toLowerCase(Locale.ROOT));
                } else {
                    try {
                        var material = itemRules.catalog().material(args[1]);
                        choices.addAll(itemRules.catalog().damageable().contains(material)
                                ? List.of("off", "1") : List.of("off", "1", "16", "32", "64", "99"));
                    } catch (IllegalArgumentException ignored) { return List.of(); }
                }
                return matching(choices, args);
            }
            if (!List.of("maintenance", "chat", "pvp", "mobspawning").contains(action)
                    || !sender.hasPermission("vitae.admin." + action)) return List.of();
            if (args.length == 2) choices.addAll(List.of("on", "off"));
            else if ((action.equals("pvp") || action.equals("mobspawning"))
                    && (args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off"))) {
                Bukkit.getWorlds().forEach(world -> choices.add(world.getName()));
            }
        }
        if (args.length == 0) return List.of();
        return matching(choices, args);
    }

    private List<String> matching(List<String> choices, String[] args) {
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return choices.stream().filter(choice -> choice.toLowerCase(Locale.ROOT).startsWith(prefix)).sorted().toList();
    }
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/command/WhisperCommand.java'
        OriginalHashes = @('568e9260a6cbe7f0d3cbfe72deb47ef193ec4b3cbdf957cb5be4f1f40377b10c', '7a9994f9022a8985ec3d441b6b6c7d07de96586b56176e5551cc2b05b5453f86')
        Hash = '971771950082cfc5b835354b0ed826324afa37c3fb0abef49031d571a3f9e770'
        Required = $true
        Binary = $false
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
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionAltar.java'
        OriginalHashes = @('3b2ebd6d55376364e2498c4205662304e9e78afbd29beb2bf67b3b3399cbede7', 'a1abd95076c80460531be83da394355e5d158c0d877ccab6eb78d7990d58a746', 'd1b2a19e3c551a62ab914f27be66e956a67983f912edb13f6262185816408552')
        Hash = 'b0b470f498df2fbea131a7ec4ca3b7f749007710edaf7688f66a14bcfe4b15e3'
        Required = $true
        Binary = $false
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
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionAnimation.java'
        OriginalHashes = @('2caea6dbc9e3182e5a0856bd1ce42cc3b008973f013a6f5a0d451aafaab15a57')
        Hash = 'ea48c3097cf8aa9e390a94e3c0685a1dfcd072b4639eb15dede478dafa09e3dd'
        Required = $true
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.evolution;

import java.util.*;

import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionAltar.*;

public final class EvolutionAnimation {
    private EvolutionAnimation() {
    }

    public record Vec(double x, double y, double z) {
        public Vec mix(Vec end, double p) {
            return new Vec(
                    x + (end.x - x) * p,
                    y + (end.y - y) * p,
                    z + (end.z - z) * p
            );
        }
    }

    public record Frame(
            Vec weapon,
            List<Vec> ingredients,
            double rotation,
            double tilt,
            Stage stage
    ) {
    }

    public static Frame frame(
            Preset preset, double progress, int elapsed, List<Vec> origins
    ) {
        range(progress, 0, 1, "Progress");
        require(elapsed >= 0 && !origins.isEmpty() && origins.size() <= 8,
                "Frame tidak valid.");

        double lift = clamp((progress - .15) / .20);
        double gather = lift;
        double shrink = 1 - clamp((progress - .70) / .30);

        double rotation = switch (preset) {
            case INFERNO -> elapsed * .055;
            case STORMFORGE -> Math.floor(elapsed / 6.0) * Math.PI / 3;
            case VOID -> -elapsed * .035;
            case ASTRAL -> elapsed * .035;
            case FROST -> elapsed * .018;
            case PHOENIX -> elapsed * .07;
        };

        Vec weapon = new Vec(
                0,
                lift * 2.4 + Math.sin(elapsed * .08) * lift * .10 * shrink,
                0
        );

        List<Vec> positions = new ArrayList<>();

        for (int i = 0; i < origins.size(); i++) {
            double angle = rotation * 2 + Math.PI * 2 * i / origins.size();
            double height = switch (preset) {
                case INFERNO -> Math.sin(angle * 2) * .25;
                case STORMFORGE -> i % 2 == 0 ? .45 : -.45;
                case VOID -> Math.sin(elapsed * .06 + i) * .8;
                case ASTRAL -> Math.sin(angle) * 1.15;
                case FROST -> -.65 + Math.sin(angle * 6) * .12;
                case PHOENIX -> Math.sin(angle * 2) * .9;
            };

            Vec orbit = new Vec(
                    Math.cos(angle) * (preset == Preset.FROST ? 2.2 : 1.8) * shrink,
                    weapon.y + height * shrink,
                    Math.sin(angle) * (preset == Preset.ASTRAL ? .9 : 1.8) * shrink
            );
            positions.add(origins.get(i).mix(orbit, gather));
        }

        return new Frame(
                weapon,
                List.copyOf(positions),
                rotation,
                preset == Preset.STORMFORGE ? .35 : 0,
                stage(progress)
        );
    }

    public static Stage stage(double progress) {
        return progress < .15 ? Stage.AWAKEN
                : progress < .35 ? Stage.LIFT
                  : progress < .70 ? Stage.ORBIT
                    : progress < 1 ? Stage.ABSORB : Stage.FINISH;
    }

    public static int remaining(Stage stage, int ticks, int elapsed) {
        double end = switch (stage) {
            case AWAKEN -> .15;
            case LIFT -> .35;
            case ORBIT -> .70;
            case ABSORB -> 1;
            case FINISH -> 0;
        };
        return stage == Stage.FINISH
                ? 100 : Math.max(10, (int) Math.ceil(ticks * end) - elapsed);
    }

    private static double clamp(double progress) {
        return Math.max(0, Math.min(1, progress));
    }
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionBlast.java'
        OriginalHashes = @()
        Hash = '8b1fd74fc57c15f4ab84ac89f5463b2c3f0738304bb9ab88c5dea1f480129fdf'
        Required = $false
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.evolution;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import java.util.*;

/** Event damage normal menjaga kompatibilitas dengan TotemListener Vitae. */
public final class EvolutionBlast {
    private final Set<UUID> lethalHits = new HashSet<>();
    public void hit(Player player, boolean lethal, double damage) {
        if (player.isDead() || (player.getGameMode() != GameMode.SURVIVAL
                && player.getGameMode() != GameMode.ADVENTURE)) return;
        if (!lethal) { if (damage > 0) player.damage(damage); return; }
        if (!lethalHits.add(player.getUniqueId())) return;
        int previous = player.getNoDamageTicks();
        try {
            player.setNoDamageTicks(0);
            player.damage(Math.max(1000, player.getHealth() + player.getAbsorptionAmount() + 1));
        } finally {
            lethalHits.remove(player.getUniqueId());
            if (!player.isDead()) player.setNoDamageTicks(Math.max(previous, player.getNoDamageTicks()));
        }
    }
    @SuppressWarnings("removal")
    public void adjust(EntityDamageEvent event) {
        if (event.isCancelled() || !(event.getEntity() instanceof Player player)
                || !lethalHits.contains(player.getUniqueId())) return;
        event.setDamage(Math.max(1, player.getHealth() + player.getAbsorptionAmount() + 1));
        for (EntityDamageEvent.DamageModifier modifier : EntityDamageEvent.DamageModifier.values())
            if (modifier != EntityDamageEvent.DamageModifier.BASE && event.isApplicable(modifier))
                event.setDamage(modifier, 0);
    }
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionGrid.java'
        OriginalHashes = @('4c361e822d8cd8c8c2d0a63360ef8e5641606d79e30d5ece769e327a1e81986b')
        Hash = 'cf1ee814be8a72dd4855c467c59c29b123a9cce92a6713a2b19e898a1516c42d'
        Required = $false
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.evolution;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/** Slot 0 hasil terkunci; 1–9 matrix crafting, slot 5 senjata. */
public final class EvolutionGrid {
    private static final int[] SLOTS = {1, 2, 3, 4, 6, 7, 8, 9};
    private EvolutionGrid() { }
    public static boolean ingredientSlot(int rawSlot) {
        for (int slot : SLOTS) if (slot == rawSlot) return true;
        return false;
    }
    public static int rawSlot(EvolutionAltar altar, int ingredient) {
        return altar.ingredients().get(ingredient).slot();
    }
    public static int index(EvolutionAltar altar, int rawSlot) {
        if (rawSlot == 5) return 8;
        for (int i = 0; i < altar.ingredients().size(); i++)
            if (altar.ingredients().get(i).slot() == rawSlot) return i;
        return -1;
    }
    public static int rawSlot(int ingredient) {
        if (ingredient < 0 || ingredient > 7) throw new IllegalArgumentException("Nomor bahan tidak valid.");
        return SLOTS[ingredient];
    }
    public static int index(int rawSlot) {
        if (rawSlot == 5) return 8;
        for (int i = 0; i < SLOTS.length; i++) if (SLOTS[i] == rawSlot) return i;
        return -1;
    }
    public static boolean materialsComplete(EvolutionAltar altar, Map<Integer, Integer> amounts) {
        if (altar.ingredients().isEmpty()) return false;
        for (int i = 0; i < altar.ingredients().size(); i++)
            if (amounts.getOrDefault(i, 0) != altar.ingredients().get(i).count()) return false;
        return true;
    }
    public static List<Integer> newlyComplete(EvolutionAltar altar, Map<Integer, Integer> before, Map<Integer, Integer> after) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < altar.ingredients().size(); i++) {
            int required = altar.ingredients().get(i).count();
            if (before.getOrDefault(i, 0) < required && after.getOrDefault(i, 0) == required) result.add(i);
        }
        return List.copyOf(result);
    }
    public static int accepted(int required, int stored, int offered, boolean single) {
        EvolutionAltar.require(required >= 1 && stored >= 0 && stored <= required && offered >= 0,
                "Jumlah setoran tidak valid.");
        return Math.min(required - stored, Math.min(offered, single ? 1 : offered));
    }
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionLayout.java'
        OriginalHashes = @()
        Hash = 'ae278489659d53514a1cab5e88663d0f807ec0c675803b84d8e9195ee57ed77c'
        Required = $false
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.evolution;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;
import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionAltar.*;

/** Template-only native crafting editor. Preview items never leave its matrix. */
public final class EvolutionLayout implements Listener, AutoCloseable {
    private static final class Editor {
        final UUID owner;
        final EvolutionAltar before;
        final CraftingInventory inventory;
        final Map<Integer, ItemStack> samples = new TreeMap<>();
        boolean rendering;
        Editor(UUID owner, EvolutionAltar before, CraftingInventory inventory) {
            this.owner = owner; this.before = before; this.inventory = inventory;
            for (Ingredient ingredient : before.ingredients()) samples.put(ingredient.slot(),
                    EvolutionService.decode(ingredient.item()).asQuantity(ingredient.count()));
        }
    }
    private final JavaPlugin plugin;
    private final EvolutionService service;
    private final Map<UUID, Editor> editors = new HashMap<>();
    private boolean closed;
    EvolutionLayout(JavaPlugin plugin, EvolutionService service) { this.plugin = plugin; this.service = service; }
    public boolean inUse(String id) { return editors.values().stream().anyMatch(e -> e.before.id().equals(id)); }
    public boolean empty() { return editors.isEmpty(); }
    public void open(Player admin, String id) {
        require(!closed && (admin.hasPermission("vitae.admin") && admin.hasPermission("vitae.admin.evolution")), "Perlu izin vitae.admin.evolution.");
        service.checkSetup(id);
        EvolutionAltar altar = service.altar(id);
        require(!altar.source().isEmpty() && !altar.result().isEmpty(), "Set source dan result dahulu, lalu /evo layout " + id + ".");
        require(!editors.containsKey(admin.getUniqueId()), "Tutup editor sebelumnya dahulu.");
        require(admin.getItemOnCursor() == null || admin.getItemOnCursor().getType().isAir(), "Kosongkan cursor sebelum membuka editor.");
        World world = Bukkit.getWorld(altar.table().world());
        require(world != null && admin.getWorld().equals(world), "Admin harus di dunia altar.");
        require(world.getBlockAt(altar.table().x(), altar.table().y(), altar.table().z()).getType() == Material.CRAFTING_TABLE,
                "Crafting table altar hilang.");
        var view = admin.openWorkbench(new Location(world, altar.table().x(), altar.table().y(), altar.table().z()), true);
        require(view != null && view.getTopInventory() instanceof CraftingInventory, "Editor crafting tidak dapat dibuka.");
        var editor = new Editor(admin.getUniqueId(), altar, (CraftingInventory) view.getTopInventory());
        editors.put(editor.owner, editor); paint(editor);
        service.say(admin, "EDITOR ADMIN: cursor menyalin contoh bahan, tidak mengambil item asli. Klik kiri menyalin jumlah stack; klik kanan satu.");
        service.say(admin, "Cursor kosong + klik slot menghapus contoh. Slot 5 dan hasil kanan terkunci. Tutup GUI untuk menyimpan susunan sebagai draft OFF.");
    }
    private Editor editor(InventoryView view) {
        Editor editor = editors.get(view.getPlayer().getUniqueId());
        return editor != null && editor.inventory == view.getTopInventory() ? editor : null;
    }
    private void paint(Editor editor) {
        editor.rendering = true;
        try {
            editor.inventory.clear(); editor.samples.forEach((slot, sample) -> editor.inventory.setItem(slot, sample.clone()));
            editor.inventory.setItem(5, EvolutionService.decode(editor.before.source()).asOne());
            editor.inventory.setResult(EvolutionService.decode(editor.before.result()).asOne());
        } finally { editor.rendering = false; }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void craft(PrepareItemCraftEvent event) {
        Editor editor = editor(event.getView());
        if (editor != null && !editor.rendering) editor.inventory.setResult(EvolutionService.decode(editor.before.result()).asOne());
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void recipe(com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent event) {
        if (editor(event.getPlayer().getOpenInventory()) != null) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void click(InventoryClickEvent event) {
        Editor editor = editor(event.getView()); if (editor == null) return;
        boolean denied = event.isCancelled(), simple = event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT;
        if (event.getRawSlot() >= 10 && simple && !denied) return;
        event.setCancelled(true);
        if (denied || !simple || !EvolutionGrid.ingredientSlot(event.getRawSlot()) || !(event.getWhoClicked() instanceof Player player)) return;
        try {
            require((player.hasPermission("vitae.admin") && player.hasPermission("vitae.admin.evolution")), "Izin admin berubah; susunan tidak disimpan.");
            ItemStack cursor = event.getCursor();
            if (cursor == null || cursor.getType().isAir()) editor.samples.remove(event.getRawSlot());
            else {
                service.templateAllowed(cursor);
                editor.samples.put(event.getRawSlot(), cursor.asQuantity(event.getClick() == ClickType.RIGHT ? 1 : cursor.getAmount()));
            }
            paint(editor);
        } catch (RuntimeException e) { service.say(player, e.getMessage()); }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void drag(InventoryDragEvent event) {
        Editor editor = editor(event.getView()); if (editor == null) return;
        if (event.getRawSlots().stream().noneMatch(s -> s < 10)) return;
        boolean denied = event.isCancelled(); event.setCancelled(true);
        if (denied || !(event.getWhoClicked() instanceof Player player)) return;
        try {
            require((player.hasPermission("vitae.admin") && player.hasPermission("vitae.admin.evolution")), "Izin admin berubah.");
            require(event.getRawSlots().stream().allMatch(EvolutionGrid::ingredientSlot), "Drag contoh hanya di slot pinggir.");
            Map<Integer, ItemStack> changed = new HashMap<>();
            for (var entry : event.getNewItems().entrySet()) {
                service.templateAllowed(entry.getValue()); changed.put(entry.getKey(), entry.getValue().clone());
            }
            editor.samples.putAll(changed); paint(editor);
        } catch (RuntimeException e) { service.say(player, e.getMessage()); }
    }
    @EventHandler(priority = EventPriority.LOWEST)
    public void closeEditor(InventoryCloseEvent event) {
        Editor editor = editor(event.getView()); if (editor == null) return;
        editors.remove(editor.owner); editor.inventory.clear(); // Before native crafting returns its matrix.
        if (closed || !(event.getPlayer() instanceof Player admin)) return;
        try {
            require(admin.isOnline() && !admin.isDead() && (admin.hasPermission("vitae.admin") && admin.hasPermission("vitae.admin.evolution")), "Editor ditutup; susunan tidak disimpan.");
            require(service.altar(editor.before.id()).equals(editor.before), "Altar berubah; buka ulang editor.");
            List<Ingredient> ingredients = new ArrayList<>();
            for (var entry : editor.samples.entrySet()) {
                Point pedestal = editor.before.ingredients().stream().filter(i -> i.slot() == entry.getKey())
                        .map(Ingredient::point).filter(Objects::nonNull).findFirst().orElse(null);
                ingredients.add(new Ingredient(EvolutionService.encode(entry.getValue().asOne()), entry.getValue().getAmount(), pedestal, entry.getKey()));
            }
            service.edit(editor.before.id(), section -> {
                section.set("enabled", false); section.set("ingredients", null);
                var root = section.createSection("ingredients");
                for (int i = 0; i < ingredients.size(); i++) EvolutionStore.writeIngredient(root.createSection(Integer.toString(i + 1)), ingredients.get(i));
            }, admin);
            service.say(admin, "Susunan dikirim untuk disimpan. Setelah pesan tersimpan: lihat pedestal, /evo pedestal " + editor.before.id() + " <slot>. Terakhir /evo enable " + editor.before.id() + ".");
        } catch (RuntimeException e) { service.say(admin, e.getMessage()); }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void drop(PlayerDropItemEvent event) { if (editors.containsKey(event.getPlayer().getUniqueId())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void swap(PlayerSwapHandItemsEvent event) { if (editors.containsKey(event.getPlayer().getUniqueId())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.LOWEST)
    public void death(org.bukkit.event.entity.PlayerDeathEvent event) { discard(event.getEntity()); }
    @EventHandler(priority = EventPriority.LOWEST)
    public void quit(PlayerQuitEvent event) { discard(event.getPlayer()); }
    private void discard(Player player) {
        Editor editor = editors.remove(player.getUniqueId()); if (editor != null) editor.inventory.clear();
    }
    @Override public void close() {
        closed = true;
        for (Editor editor : List.copyOf(editors.values())) {
            editor.inventory.clear(); Player player = Bukkit.getPlayer(editor.owner);
            if (player != null && player.getOpenInventory().getTopInventory() == editor.inventory) player.closeInventory();
        }
        editors.clear(); HandlerList.unregisterAll(this);
    }
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionMusic.java'
        OriginalHashes = @()
        Hash = 'f98ce36f813826b6087163fdf9735832c7c28d2db4e0aa467d15d2ac5f1ee99e'
        Required = $false
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.evolution;

import org.bukkit.*;
import org.bukkit.entity.Player;
import java.util.*;

/** Musik memakai kategori RECORDS, terpisah dari VOICE untuk cue dialog/tahap. */
public final class EvolutionMusic implements AutoCloseable {
    private record Track(EvolutionAltar altar, Set<UUID> audience) { }
    private final Map<UUID, Track> tracks = new HashMap<>();
    private final Map<UUID, Map<String, UUID>> ownership = new HashMap<>();
    public void start(UUID id, UUID owner, EvolutionAltar altar) {
        if (tracks.containsKey(id) || altar.music().sound().isEmpty() || altar.music().volume() == 0) return;
        World world = Bukkit.getWorld(altar.table().world()); if (world == null) return;
        Player player = Bukkit.getPlayer(owner);
        List<Player> audience = altar.music().privateSound() ? player == null ? List.of() : List.of(player)
                : world.getPlayers().stream().filter(p -> inside(p, altar)).toList();
        Set<UUID> ids = new HashSet<>();
        for (Player listener : audience) {
            listener.playSound(listener.getLocation(), altar.music().sound(), SoundCategory.RECORDS, altar.music().volume(), 1);
            ids.add(listener.getUniqueId());
            ownership.computeIfAbsent(listener.getUniqueId(), key -> new HashMap<>()).put(altar.music().sound(), id);
        }
        tracks.put(id, new Track(altar, ids));
    }
    public void trim(UUID id) {
        Track track = tracks.get(id); if (track == null || track.altar().music().privateSound()) return;
        for (UUID listenerId : Set.copyOf(track.audience())) {
            Player listener = Bukkit.getPlayer(listenerId);
            if (listener == null || !inside(listener, track.altar())) stopFor(id, track, listenerId);
        }
    }
    private static boolean inside(Player player, EvolutionAltar altar) {
        EvolutionAltar.Point point = altar.table(); Location location = player.getLocation();
        if (!location.getWorld().getUID().equals(point.world())) return false;
        double dx=location.getX()-point.x()-.5, dy=location.getY()-point.y()-.5, dz=location.getZ()-point.z()-.5;
        return dx*dx+dy*dy+dz*dz <= altar.soundRadius()*altar.soundRadius();
    }
    private void stopFor(UUID id, Track track, UUID listenerId) {
        String key = track.altar().music().sound(); Map<String, UUID> sounds = ownership.get(listenerId);
        if (sounds != null && sounds.remove(key, id)) {
            Player player = Bukkit.getPlayer(listenerId); if (player != null) player.stopSound(key, SoundCategory.RECORDS);
            if (sounds.isEmpty()) ownership.remove(listenerId);
        }
        track.audience().remove(listenerId);
    }
    public void stop(UUID id) {
        Track track = tracks.remove(id); if (track != null)
            for (UUID listener : Set.copyOf(track.audience())) stopFor(id, track, listener);
    }
    public void quit(UUID listener) {
        for (var entry : tracks.entrySet()) if (entry.getValue().audience().contains(listener))
            stopFor(entry.getKey(), entry.getValue(), listener);
    }
    @Override public void close() { for (UUID id : Set.copyOf(tracks.keySet())) stop(id); }
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionService.java'
        OriginalHashes = @('63b142a5e3e9de801e60597402f5e7dde822a92a9ca154007b897d3d69fdfa35', '8d5cfac70dc6a8e778a8973cf272fb4e4244c10fbf0cd837b89980347b03aa0f', 'b65149fff451e4533c6ed565dc91d556424dd267f176940df440bcae9aa5f0c4')
        Hash = '68ce8f6aff0fce2e86727f4fbe788d215cc9cb556d4ac5f56da292340f53ecb2'
        Required = $true
        Binary = $false
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
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionStore.java'
        OriginalHashes = @('53598e287e3a5fc445e0f580943f65440d16bbe647063b30286a790c950f417b', 'e386caae76d8570cddea7a73b725d0b6f49ed5146fa30558412b90e158a74b31', 'ff46f56b7ed672205f9bdea38f420b4cac96eb7b36523c2aa24cca9606705314')
        Hash = '7c54a9774c5f10b1b64699d5395a8edab66304aeff8994cd3e48c1cf7addc0ba'
        Required = $true
        Binary = $false
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
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcCommand.java'
        OriginalHashes = @('68d8df96b476e686e51bd98d9fb7ace4f2808b31ba0c49ff40d664f050c06a5b', '809851a6db2321e73531e27c347cdceb656dafd0ec714dbe1bfb4a13ad0ff3d6', 'bf1fa613a382ee098c23d854215da6c8480d78ad73036d387975fa2222e1f0e1', 'e0290f06a2ee5e692839e05955a727eb6e256c5301d33b74fb1aa7e4b3495fdb')
        Hash = '6b7a3e618b529b941b0023ee50eaea79e6a338fd8737191a743bd2799027cd50'
        Required = $true
        Binary = $false
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
        else if (a.length == 4 && a[1].equalsIgnoreCase("option")) { values.addAll(List.of("END", "QUEST", "OPEN", "DUNGEON_GLASS", "DUNGEON_MINION", "DUNGEON_BOSS", "DUNGEON_REWARD", "ACTION_")); values.addAll(service.get(a[0]).nodes().keySet()); }
        else if (a.length == 5 && a[1].equalsIgnoreCase("quest") && a[2].equalsIgnoreCase("mob")) values.addAll(List.of("COW", "PIG", "SHEEP", "CHICKEN", "ZOMBIE", "SKELETON"));
        String last = a.length == 0 ? "" : a[a.length - 1].toLowerCase(Locale.ROOT);
        return values.stream().filter(v -> v.toLowerCase(Locale.ROOT).startsWith(last)).sorted().toList();
    }
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcData.java'
        OriginalHashes = @('10a34d23245c29daadcd13487575c1258a519d2221f9cdb2595c69fb00c0735a', '569a4b30150cb1fa63e7dc59d02e37d5175f32b1693ee309c637a86a0d9aaaee', '594ef5662c976c940e2e3e78546c4254807622c2320d35d691a3bc589187fb86', 'ec5a3242168e59cb7d12474143efd76400fe0623172e9500fb29ccb6faa363c0')
        Hash = '5e480bf3759e27d69055e13db5df81ff924dd94b4c2019f4355950176466c867'
        Required = $true
        Binary = $false
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
            } else if (!c.target().equals("END") && !com.bangzachery.vitae.vitaemanager.story.StoryNpcActionEvent.target(c.target()) && !nodes.containsKey(c.target()))
                throw new IllegalArgumentException("Tujuan pilihan tidak ditemukan: " + c.target());
        }
        Set<String> exits = new HashSet<>();
        boolean changed;
        do {
            changed = false;
            for (var entry : nodes.entrySet()) if (!exits.contains(entry.getKey())
                    && entry.getValue().available().stream().anyMatch(c -> c.target().equals("END") || c.target().equals("OPEN")
                    || c.target().equals("QUEST") || com.bangzachery.vitae.vitaemanager.story.StoryNpcActionEvent.target(c.target()) || exits.contains(c.target()))) changed |= exits.add(entry.getKey());
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
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcFacing.java'
        OriginalHashes = @()
        Hash = 'de684821d755ed3267d9711de50662314fecd994a1877d5b22b969ff8f410fd8'
        Required = $false
        Binary = $false
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
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcFarm.java'
        OriginalHashes = @()
        Hash = '2cc96c14192a40191772cde7aded02ddb4fbab27dbad828452e880dd413a17b8'
        Required = $false
        Binary = $false
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
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcFarmCursor.java'
        OriginalHashes = @()
        Hash = '8ddc81b0bca051332853d80d00e82c3a0dc22cc073b2e2f4eae1613581c2b4fc'
        Required = $false
        Binary = $false
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
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcGates.java'
        OriginalHashes = @('1e9914c5acb9591c78d043f8c7790322ece96f20fa0bc7fe7d1b5a15f05b0335')
        Hash = '921104a726360c8689e22810558bbeac5eb6076b2238fe66b38e6b41f50fd673'
        Required = $false
        Binary = $false
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
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcModule.java'
        OriginalHashes = @('73a22e6a89092c2ba55ded73179e1e5d920f67689b36d7ba283bb43118f19028')
        Hash = 'ac466534186a7fe26b33dcd493a27f9b02de34c63033120f67e3f9ac89fe44f1'
        Required = $true
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.economy.VitiService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/** Lifecycle facade: existing modules in Vitaemanager.java need no replacement. */
public final class NpcModule {
    private static final Map<JavaPlugin, NpcService> ACTIVE = new IdentityHashMap<>();
    private static final Map<JavaPlugin, Throwable> FAILED = new IdentityHashMap<>();
    @FunctionalInterface interface Factory { NpcService create() throws IOException; }
    private NpcModule() {}
    public static NpcService active(JavaPlugin plugin) { return ACTIVE.get(plugin); }
    public static void start(JavaPlugin plugin, VitiService viti) throws IOException {
        start(plugin, viti, () -> new NpcService(plugin, viti));
    }
    static void start(JavaPlugin plugin, VitiService viti, Factory factory) {
        if (ACTIVE.containsKey(plugin)) return;
        NpcService service = null;
        try {
            service = factory.create();
            NpcCommand executor = new NpcCommand(service);
            for (String name : List.of("vnpc", "quest")) {
                PluginCommand command = plugin.getCommand(name);
                if (command == null) throw new IllegalStateException("Command " + name + " belum terdaftar di plugin.yml.");
                command.setExecutor(executor); command.setTabCompleter(executor);
            }
            plugin.getServer().getPluginManager().registerEvents(service, plugin);
            service.start(); ACTIVE.put(plugin, service); FAILED.remove(plugin);
        } catch (IOException | RuntimeException | LinkageError error) {
            if (service != null) try { service.close(); }
            catch (RuntimeException | LinkageError cleanup) { error.addSuppressed(cleanup); }
            FAILED.put(plugin, error);
            plugin.getLogger().log(Level.SEVERE, "Modul NPC gagal diaktifkan. Modul Vitae lain tetap berjalan; data npc.json dipertahankan. Gunakan /vnpc status.", error);
            recovery(plugin, viti);
        }
    }
    public static void stop(JavaPlugin plugin) {
        FAILED.remove(plugin);
        NpcService service = ACTIVE.remove(plugin);
        if (service != null) try { service.close(); }
        catch (RuntimeException | LinkageError error) { plugin.getLogger().log(Level.SEVERE, "Pembersihan modul NPC gagal.", error); }
    }
    private static void recovery(JavaPlugin plugin, VitiService viti) {
        CommandExecutor executor = (sender, command, label, args) -> {
            if (command.getName().equalsIgnoreCase("quest")) {
                tell(sender, "Modul quest belum aktif. Minta admin memeriksa /vnpc status.", NamedTextColor.RED); return true;
            }
            if (!sender.hasPermission("vitae.admin.npc")) {
                tell(sender, "Perlu izin vitae.admin.npc.", NamedTextColor.RED); return true;
            }
            try {
                if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                    start(plugin, viti); report(plugin, sender); return true;
                }
                if (args.length == 2 && args[1].equalsIgnoreCase("off")) {
                    NpcStore store = new NpcStore(plugin.getDataFolder().toPath().resolve("npc.json"));
                    NpcData.State state = store.initialize(); var definition = state.npcs().get(args[0]);
                    if (definition == null) throw new IllegalArgumentException("NPC tidak ditemukan: " + args[0]);
                    var json = NpcStore.JSON.toJsonTree(definition).getAsJsonObject(); json.addProperty("enabled", false);
                    store.save(state.put(NpcStore.JSON.fromJson(json, NpcData.Definition.class)));
                    tell(sender, "NPC " + args[0] + " dinonaktifkan. Definisi dan progres tetap tersimpan.", NamedTextColor.YELLOW);
                    start(plugin, viti); report(plugin, sender); return true;
                }
                report(plugin, sender);
            } catch (IOException | RuntimeException error) {
                tell(sender, reason(error), NamedTextColor.RED);
                plugin.getLogger().log(Level.SEVERE, "Pemulihan NPC belum berhasil; data lama dipertahankan.", error);
            }
            return true;
        };
        for (String name : List.of("vnpc", "quest")) {
            PluginCommand command = plugin.getCommand(name);
            if (command == null) continue;
            command.setExecutor(executor);
            command.setTabCompleter((sender, cmd, alias, args) -> {
                if (!sender.hasPermission("vitae.admin.npc")) return List.of();
                if (args.length == 1) return List.of("status", "reload", "help").stream()
                        .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
                return args.length == 2 && "off".startsWith(args[1].toLowerCase(Locale.ROOT)) ? List.of("off") : List.of();
            });
        }
    }
    private static void report(JavaPlugin plugin, CommandSender sender) {
        sender.sendMessage(Component.empty());
        if (ACTIVE.containsKey(plugin)) tell(sender, "Modul NPC aktif kembali.", NamedTextColor.GREEN);
        else {
            tell(sender, "Modul NPC belum aktif: " + reason(FAILED.get(plugin)), NamedTextColor.RED);
            tell(sender, "Perbaiki penyebabnya, lalu /vnpc reload. Jika satu NPC/model bermasalah: /vnpc <id> off.", NamedTextColor.AQUA);
            tell(sender, "Error lengkap ada pada log startup. File npc.json tidak dikosongkan.", NamedTextColor.GRAY);
        }
        sender.sendMessage(Component.empty());
    }
    private static String reason(Throwable error) {
        if (error == null) return "Periksa log startup server.";
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        while (error.getCause() != null && seen.add(error)) error = error.getCause();
        return error.getClass().getSimpleName() + ": " + Objects.requireNonNullElse(error.getMessage(), "Periksa log startup server.");
    }
    private static void tell(CommandSender sender, String text, NamedTextColor color) {
        sender.sendMessage(Component.text("[Vitae NPC] ", NamedTextColor.GOLD).append(Component.text(text, color)));
    }
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcQuest.java'
        OriginalHashes = @('32cdfb12c381e999abd91f7bbf3b407255c9fc1004521337e9827f6b2b3b1484', '370aa44f067787f13fd8c0ca88b2877c37274b50cc0635536326d33d329890e2')
        Hash = 'a90eda18fa3dab8a9517f3147e782a1082c8ce1e19e77a840a3e3a489b0d6071'
        Required = $true
        Binary = $false
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
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcRenderer.java'
        OriginalHashes = @('27e727bb4fbcefd077b4cd3bd8a63a503d276dcb5822d1872f423246c992bafb')
        Hash = '1605b43a16da0f9513f298e540a9b5195cae8d2e3b713194fc71b9ad154a131d'
        Required = $true
        Binary = $false
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
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcService.java'
        OriginalHashes = @('0f1712e9b1e682079a4d62c87a0d452aa78d03d5a71f61590bab7080bebb633c', '38594bc7f74793f89c07d5392cb10e7570862a8d7eb1dab0ac61e2faf1ef7120', '997d10e713ae257703ff9dc65a7874056f67657d9ac6d4676cf6b21947a87b5a', 'c01766df473c0ac7882071413e93b119fd088655dff437ba6b6dae2f3f25de67', 'd79f24718a388fa9a81d5691ac5ea87d3be1bede60a4cf242735d0dd553470e5')
        Hash = '5c185c3455eab0cf071c108a8533d4b32ada8baf726fdba93959cc9d1a810b15'
        Required = $true
        Binary = $false
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
    public void cancelStoryDialog(Player player) {
        if (player == null) return;
        Session session = sessions.get(player.getUniqueId());
        if (session != null) end(session, false);
    }
    public boolean storyReward(Player player, String id, String completionPage) {
        NpcData.Definition definition = get(id);
        String text = completionPage.isEmpty() ? "Selamat! Kamu telah menyelesaikan tantanganku. Ambillah hadiahmu."
                : definition.nodes().get(completionPage).text();
        if (!begin(player, definition, false, false, text)) return false;
        Session session = sessions.get(player.getUniqueId());
        session.node = new NpcData.Node(text, List.of(new NpcData.Choice("Ambil hadiah", "DUNGEON_REWARD")));
        return true;
    }
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
        com.bangzachery.vitae.vitaemanager.story.StoryNpcDialogEvent storyDialog = null;
        if (!preview && specialText == null) {
            storyDialog = new com.bangzachery.vitae.vitaemanager.story.StoryNpcDialogEvent(p, d.id());
            Bukkit.getPluginManager().callEvent(storyDialog);
        }
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
                    && d.memory().isEmpty() && (storyDialog == null || !storyDialog.managed())) specialText = d.repeat();
        }
        Session s = new Session(p, d, preview, specialText != null || (storyDialog != null && (storyDialog.managed() || storyDialog.node() != null)), trigger);
        s.node = specialText == null ? d.nodes().get(state.recalledRoot(p.getUniqueId(), d)) : new NpcData.Node(specialText, List.of());
        if (storyDialog != null && storyDialog.node() != null) s.node = storyDialog.node();
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
        if (com.bangzachery.vitae.vitaemanager.story.StoryNpcActionEvent.target(c.target())) {
            if (s.preview) { message(p, "Preview selesai. Aksi story tidak dijalankan saat preview."); end(s, false); return; }
            String npcId = s.npc.id(); end(s, false);
            Bukkit.getPluginManager().callEvent(new com.bangzachery.vitae.vitaemanager.story.StoryNpcActionEvent(p, npcId, c.target()));
        } else if (c.target().equals("END")) {
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
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcStore.java'
        OriginalHashes = @('5627bcc898bcc07288e2816cecdedcae36b4092d71b5170a9e05b32de9193b16', 'd21958085c2a7742b015033bd9df372b213f87fe3d047945ed4e57043c695b65', 'f52a700aa8996c66af6e4a557430f83573c629a553efe15b2f9f05bc7931d524')
        Hash = '534da37797d029896c4e485ab668184de664cb97858ace08e310e030defadb4d'
        Required = $true
        Binary = $false
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
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcText.java'
        OriginalHashes = @()
        Hash = '38c28731c8474ec3973d56dfe96bee794c4ba47dafc4649d6a14f2ef379dc12e'
        Required = $false
        Binary = $false
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
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/roleplay/CarryService.java'
        OriginalHashes = @('d354a81bc3e95b75c9fdd5635b3336eaacf0ece936bf885b48d06df88c9c4429')
        Hash = '59f653878b181ac2a8c91bdfe1159bda1a52ccc65cfdc0f783aa5426f218efb6'
        Required = $true
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.roleplay;

import com.bangzachery.vitae.vitaemanager.core.config.ConfigurationService;
import com.bangzachery.vitae.vitaemanager.core.message.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public final class CarryService implements Listener, AutoCloseable {
    private final JavaPlugin plugin;
    private final ConfigurationService configuration;
    private final MessageService messages;
    private final RoleplayService roleplay;
    private final CarryPairs pairs = new CarryPairs();
    private BukkitTask task;
    private boolean closed;
    private java.util.function.Predicate<Player> guard = player -> false;
    public void guard(java.util.function.Predicate<Player> guard) { this.guard = java.util.Objects.requireNonNull(guard); }

    public CarryService(JavaPlugin plugin, ConfigurationService configuration,
                        MessageService messages, RoleplayService roleplay) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.messages = messages;
        this.roleplay = roleplay;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::reconcile, 5L, 5L);
    }

    public void toggle(Player carrier) {
        if (closed) return;
        if (guard.test(carrier)) {
            releasePlayer(carrier.getUniqueId());
            carrier.sendMessage(Component.text("Carry tidak boleh digunakan dalam dungeon.", net.kyori.adventure.text.format.NamedTextColor.RED));
            return;
        }
        reconcile();
        UUID riderId = pairs.rider(carrier.getUniqueId());
        if (riderId != null) {
            boolean released = release(carrier.getUniqueId(), riderId);
            messages.send(carrier, released ? "carry-released" : "carry-failed");
            return;
        }
        if (!eligible(carrier) || !pairs.available(carrier.getUniqueId())) {
            messages.send(carrier, "carry-unavailable"); return;
        }
        double range = configuration.current().roleplay().carryDistance();
        var eye = carrier.getEyeLocation();
        var hit = carrier.getWorld().rayTrace(eye, eye.getDirection(), range,
                FluidCollisionMode.NEVER, true, 0.05,
                entity -> entity instanceof Player && entity != carrier);
        if (hit == null || !(hit.getHitEntity() instanceof Player rider)
                || !roleplay.visible(carrier, rider)) {
            messages.send(carrier, "carry-no-target"); return;
        }
        if (!eligible(rider) || !pairs.available(rider.getUniqueId())) {
            messages.send(carrier, "carry-unavailable"); return;
        }
        // The platform can reject/cancel mounting; never announce success before checking it.
        if (!carrier.addPassenger(rider) || rider.getVehicle() != carrier) {
            messages.send(carrier, "carry-failed"); return;
        }
        pairs.add(carrier.getUniqueId(), rider.getUniqueId());
        messages.send(carrier, "carry-started", Component.text(" " + rider.getName()));
        messages.send(rider, "carry-riding", Component.text(" " + carrier.getName()
                + ". Shift tidak dapat menurunkanmu; penggendong memakai /carry lagi untuk menurunkan."));
    }

    private boolean eligible(Player player) {
        return !guard.test(player) && player.isOnline() && !player.isDead() && player.getGameMode() != GameMode.SPECTATOR
                && player.getVehicle() == null
                && player.getPassengers().stream().noneMatch(entity -> entity instanceof Player);
    }

    private boolean release(UUID carrierId, UUID riderId) {
        if (!riderId.equals(pairs.rider(carrierId))) return true;
        Player carrier = Bukkit.getPlayer(carrierId), rider = Bukkit.getPlayer(riderId);
        if (carrier != null && rider != null && rider.getVehicle() == carrier) {
            pairs.beginRelease(carrierId, riderId);
            try { carrier.removePassenger(rider); }
            finally { pairs.endRelease(riderId); }
            if (rider.getVehicle() == carrier) return false;
        }
        pairs.remove(carrierId, riderId);
        return true;
    }

    private boolean validPair(Player carrier, Player rider) {
        return !guard.test(carrier) && !guard.test(rider) && carrier.isOnline() && rider.isOnline() && !carrier.isDead() && !rider.isDead()
                && carrier.getVehicle() == null
                && carrier.getGameMode() != GameMode.SPECTATOR && rider.getGameMode() != GameMode.SPECTATOR
                && roleplay.visible(carrier, rider);
    }

    private void releasePlayer(UUID player) {
        for (var pair : pairs.snapshot().entrySet()) {
            if (pair.getKey().equals(player) || pair.getValue().equals(player)) {
                release(pair.getKey(), pair.getValue());
            }
        }
    }

    private void reconcile() {
        for (var pair : pairs.snapshot().entrySet()) {
            Player carrier = Bukkit.getPlayer(pair.getKey()), rider = Bukkit.getPlayer(pair.getValue());
            if (carrier == null || rider == null || rider.getVehicle() != carrier) {
                pairs.remove(pair.getKey(), pair.getValue());
            } else if (!validPair(carrier, rider)) {
                release(pair.getKey(), pair.getValue());
            }
        }
    }

    @EventHandler public void quit(PlayerQuitEvent event) { releasePlayer(event.getPlayer().getUniqueId()); }
    @EventHandler public void death(PlayerDeathEvent event) { releasePlayer(event.getEntity().getUniqueId()); }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void teleport(PlayerTeleportEvent event) { releasePlayer(event.getPlayer().getUniqueId()); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void sneak(PlayerToggleSneakEvent event) {
        Player rider = event.getPlayer();
        if (!closed && event.isSneaking() && rider.getVehicle() instanceof Player carrier
                && validPair(carrier, rider)
                && pairs.locksDismount(carrier.getUniqueId(), rider.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void dismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player rider) || !(event.getDismounted() instanceof Player carrier)
                || !rider.getUniqueId().equals(pairs.rider(carrier.getUniqueId()))) return;
        if (!closed && event.isCancellable() && validPair(carrier, rider)
                && pairs.locksDismount(carrier.getUniqueId(), rider.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        // Forced/platform cleanup and our own /carry release are allowed.
        if (!closed) Bukkit.getScheduler().runTask(plugin, () -> { if (!closed) reconcile(); });
    }

    @Override public void close() {
        closed = true;
        if (task != null) task.cancel();
        for (var pair : pairs.snapshot().entrySet()) {
            if (!release(pair.getKey(), pair.getValue())) {
                plugin.getLogger().warning("Plugin lain menolak pelepasan carry: " + pair.getValue());
            }
        }
    }
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/story/DungeonCamera.java'
        OriginalHashes = @()
        Hash = '934d0218329db4bdc44221116722971b42d8845aa71e660f55b6ce3d16f0c482'
        Required = $false
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.story;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import java.util.*;
import java.util.function.BiPredicate;

/** Cameras move only their owner, with recoverable mode/location snapshots. */
public final class DungeonCamera implements Listener {
    public static final class CameraFailure extends IllegalStateException {
        public final UUID player;
        CameraFailure(Player player,RuntimeException cause){super("Kamera gagal untuk "+player.getName(),cause);this.player=player.getUniqueId();}
    }
    private record Shot(Player player,Location from,Location view,Location back,DungeonData.Participant participant,long until,int length,Runnable done){}
    private final JavaPlugin plugin;
    private final BiPredicate<Player,Location> teleport;
    private final Map<UUID,Shot> shots=new HashMap<>();
    private long tick;
    public DungeonCamera(JavaPlugin plugin,BiPredicate<Player,Location> teleport){this.plugin=plugin;this.teleport=teleport;Bukkit.getPluginManager().registerEvents(this,plugin);}
    public boolean busy(UUID id){return shots.containsKey(id);}
    public void start(Player p,Location view,DungeonData.Participant participant,int length,Runnable done){
        stop(p.getUniqueId(),false);Location back=p.getLocation().clone();
        participant.recovery=DungeonData.point(back);participant.mode=p.getGameMode().name();participant.flight=p.getAllowFlight();participant.flying=p.isFlying();
        if(!p.getWorld().equals(view.getWorld()))throw new IllegalArgumentException("Kamera harus satu world.");
        Shot shot=new Shot(p,back.clone(),view.clone(),back,participant,tick+length,length,done);shots.put(p.getUniqueId(),shot);
        p.setGameMode(GameMode.SPECTATOR);p.setSpectatorTarget(null);p.setVelocity(new Vector());
        if(p.getGameMode()!=GameMode.SPECTATOR){shots.remove(p.getUniqueId());throw new IllegalStateException("Plugin lain menolak mode kamera.");}
    }
    public void tick(){
        tick++;for(Shot s:List.copyOf(shots.values())){
            try{
            if(!s.player.isOnline()||s.player.isDead()){stop(s.player.getUniqueId(),false);continue;}
            int age=s.length-(int)(s.until-tick);double t=Math.min(1,age/20.0),ease=t*t*(3-2*t);
            Location at=s.from.clone();at.setX(at.getX()+(s.view.getX()-at.getX())*ease);at.setY(at.getY()+(s.view.getY()-at.getY())*ease);at.setZ(at.getZ()+(s.view.getZ()-at.getZ())*ease);
            float delta=(s.view.getYaw()-s.from.getYaw())%360;delta=(delta+540)%360-180;at.setYaw(s.from.getYaw()+delta*(float)ease);at.setPitch(s.from.getPitch()+(s.view.getPitch()-s.from.getPitch())*(float)ease);
            if(!teleport.test(s.player,at)){stop(s.player.getUniqueId(),false);throw new IllegalStateException("Teleport kamera ditolak untuk "+s.player.getName());}
            if(tick>=s.until)stop(s.player.getUniqueId(),true);
            }catch(RuntimeException error){try{stop(s.player.getUniqueId(),false);}catch(RuntimeException recovery){error.addSuppressed(recovery);}throw new CameraFailure(s.player,error);}
        }
    }
    public void stop(UUID id,boolean complete){
        Shot s=shots.remove(id);if(s==null)return;
        if(s.player.isOnline()&&!s.player.isDead()){
            if(s.player.getGameMode()==GameMode.SPECTATOR)s.player.setSpectatorTarget(null);
            s.player.setGameMode(GameMode.valueOf(s.participant.mode));s.player.setAllowFlight(s.participant.flight);s.player.setFlying(s.participant.flight&&s.participant.flying);
            if(s.player.getGameMode()!=GameMode.valueOf(s.participant.mode))throw new IllegalStateException("Plugin lain menolak pemulihan mode kamera.");
            s.player.setVelocity(new Vector());s.player.setFallDistance(0);
            if(!teleport.test(s.player,s.back))throw new IllegalStateException("Lokasi kembali kamera ditolak untuk "+s.player.getName());
            s.participant.recovery=null;s.participant.mode=null;
            if(complete)s.done.run();
        }
    }
    public void restore(Player p,DungeonData.Participant participant){
        if(participant.mode==null)return;if(p.getGameMode()==GameMode.SPECTATOR)p.setSpectatorTarget(null);
        p.setGameMode(GameMode.valueOf(participant.mode));p.setAllowFlight(participant.flight);p.setFlying(participant.flight&&participant.flying);
        if(p.getGameMode()!=GameMode.valueOf(participant.mode))throw new IllegalStateException("Mode pemain belum berhasil dipulihkan.");
        p.setFallDistance(0);if(participant.recovery!=null&&participant.status!=DungeonData.Status.DEAD&&!teleport.test(p,DungeonData.location(participant.recovery)))throw new IllegalStateException("Pemulihan posisi kamera ditolak.");participant.mode=null;participant.recovery=null;
    }
    @EventHandler(priority=EventPriority.HIGHEST)public void move(PlayerMoveEvent e){if(busy(e.getPlayer().getUniqueId())&&!(e instanceof PlayerTeleportEvent))e.setTo(e.getFrom());}
    @EventHandler(priority=EventPriority.HIGHEST)public void damage(EntityDamageEvent e){if(e.getEntity() instanceof Player p&&busy(p.getUniqueId()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST)public void command(PlayerCommandPreprocessEvent e){if(busy(e.getPlayer().getUniqueId())&&!e.getPlayer().hasPermission("vitae.dungeon.bypass")){e.setCancelled(true);e.getPlayer().sendMessage("Tunggu pengenalan kamera selesai.");}}
    @EventHandler(priority=EventPriority.HIGHEST)public void interact(PlayerInteractEvent e){if(busy(e.getPlayer().getUniqueId()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST)public void interact(PlayerInteractEntityEvent e){if(busy(e.getPlayer().getUniqueId()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST)public void sneak(PlayerToggleSneakEvent e){if(busy(e.getPlayer().getUniqueId()))e.setCancelled(true);}
    public void close(){for(UUID id:List.copyOf(shots.keySet()))try{stop(id,false);}catch(RuntimeException error){plugin.getLogger().warning(error.getMessage());}HandlerList.unregisterAll(this);}
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/story/DungeonCommand.java'
        OriginalHashes = @()
        Hash = 'bb860484e9371600e6ac58a3079202d309b0a5462f671f7c41813d4e2bcdd8db'
        Required = $false
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.story;

import com.bangzachery.vitae.vitaemanager.npc.NpcData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;
import static com.bangzachery.vitae.vitaemanager.story.DungeonData.*;

public final class DungeonCommand implements CommandExecutor,TabCompleter {
    private record Selection(Location a,Location b){}
    private final DungeonService service;
    private final Map<UUID,Selection> selections=new HashMap<>();
    private static final List<String> TOPICS=List.of("portal","kaca","minion","bos","hadiah","aksi","sesi");
    private static final List<String> OPS=List.of("pos1","pos2","region","location","bad","spawn","boss","npc","reward","time","fade","check","on","off","open","stop","reset","status","join","receipt");
    public DungeonCommand(DungeonService service){this.service=service;}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){return execute(sender,args);}
    public boolean execute(CommandSender sender,String[] args){
        if(!sender.hasPermission("vitae.admin.dungeon")){sender.sendMessage(Component.text("Perlu izin vitae.admin.dungeon.",NamedTextColor.RED));return true;}
        try{
            if(args.length==0||args[0].equalsIgnoreCase("help")){help(sender,args.length>1?args[1]:"all");return true;}
            if(args[0].equalsIgnoreCase("list")){for(String id:service.current().definitions.keySet())service.status(sender,id);if(service.current().definitions.isEmpty())tell(sender,"Belum ada dungeon. /vdungeon create <id> <world>");return true;}
            if(args[0].equalsIgnoreCase("create")){arity(args,3,"/vdungeon create <id> <world>");World w=Bukkit.getWorld(args[2]);if(w==null)throw new IllegalArgumentException("World belum dimuat. Buat/muat world dahulu melalui pengelola world server.");service.create(args[1],w);tell(sender,"Dungeon dibuat dalam keadaan OFF. Berikutnya: /vdungeon help portal");return true;}
            if(args[0].equalsIgnoreCase("action")){action(sender,args);return true;}
            String id=NpcData.id(args[0]);service.get(id);if(args.length<2){service.status(sender,id);help(sender,"all");return true;}
            String op=args[1].toLowerCase(Locale.ROOT);
            switch(op){
                case "pos1","pos2"->{arity(args,2,"/vdungeon <id> pos1|pos2");Player p=player(sender);Selection old=selections.getOrDefault(p.getUniqueId(),new Selection(null,null));var hit=p.getTargetBlockExact(6);Location at=hit==null?p.getLocation():hit.getLocation();selections.put(p.getUniqueId(),op.equals("pos1")?new Selection(at.clone(),old.b):new Selection(old.a,at.clone()));tell(sender,op+" = "+at.getBlockX()+", "+at.getBlockY()+", "+at.getBlockZ()+". Arahkan crosshair ke blok untuk memilih blok; lihat udara untuk memilih posisi kaki.");}
                case "region"->{arity(args,3,"/vdungeon <id> region entrance|arena|camera|finish|miniongate|miniontrigger|bossgate|bosstrigger|exit");Box b=selection(player(sender));service.edit(id,d->{switch(args[2].toLowerCase(Locale.ROOT)){case "entrance"->d.entrance=b;case "arena"->d.arena=b;case "camera"->d.cameraZone=b;case "finish"->d.finish=b;case "miniongate"->d.minionGate=b;case "miniontrigger"->d.minionTrigger=b;case "bossgate"->d.bossGate=b;case "bosstrigger"->d.bossTrigger=b;case "exit"->d.exit=b;default->throw new IllegalArgumentException("Jenis area tidak dikenal. /vdungeon help portal");}});tell(sender,"Area "+args[2]+" tersimpan ("+b.volume()+" blok). Dungeon OFF sampai setup selesai.");}
                case "location"->{arity(args,3,"/vdungeon <id> location entry|home|start|camera|next");NpcData.Point at=point(player(sender).getLocation());service.edit(id,d->{switch(args[2].toLowerCase(Locale.ROOT)){case "entry"->d.entry=at;case "home"->d.home=at;case "start"->d.start=at;case "camera"->d.camera=at;case "next"->d.next=at;default->throw new IllegalArgumentException("Jenis lokasi tidak dikenal.");}});tell(sender,"Lokasi "+args[2]+" dan arah pandangnya tersimpan.");}
                case "bad"->{arity(args,4,"/vdungeon <id> bad add|remove <nama-petak>");String tile=NpcData.id(args[3]);Box b=args[2].equalsIgnoreCase("add")?selection(player(sender)):null;service.edit(id,d->{if(args[2].equalsIgnoreCase("add")){if(b.y1()!=b.y2()||b.x2()-b.x1()!=2||b.z2()-b.z1()!=2)throw new IllegalArgumentException("Seleksi petak kaca 3×3 satu lapis.");d.bad.put(tile,b);}else if(args[2].equalsIgnoreCase("remove"))d.bad.remove(tile);else throw new IllegalArgumentException("bad add|remove");});tell(sender,"Pijakan salah "+tile+" diperbarui.");}
                case "spawn"->{if(args.length==4&&args[2].equalsIgnoreCase("remove")){service.edit(id,d->d.minions.remove(args[3]));tell(sender,"Spawn dihapus.");break;}
                    if(args.length<5||args.length>6)throw new IllegalArgumentException("/vdungeon <id> spawn <nama-titik> vanilla:COW|mythic:ID <jumlah> [level]");String name=NpcData.id(args[2]);Mob m=new Mob(args[3],number(args[4],1,4096),args.length==6?Double.parseDouble(args[5]):1,point(player(sender).getLocation()));service.edit(id,d->d.minions.put(name,m));tell(sender,"Spawn "+name+" tersimpan. Total minion: "+service.get(id).minions.values().stream().mapToInt(Mob::count).sum());}
                case "boss"->{if(args.length<3||args.length>4)throw new IllegalArgumentException("/vdungeon <id> boss mythic:ID|vanilla:TYPE [level]");Mob m=new Mob(args[2],1,args.length==4?Double.parseDouble(args[3]):1,point(player(sender).getLocation()));service.edit(id,d->d.boss=m);tell(sender,"Lokasi dan jenis bos tersimpan.");}
                case "npc"->{if(args.length<4||args.length>5)throw new IllegalArgumentException("/vdungeon <id> npc glass|minion|boss <id-npc> [halaman-selamat]");String npc=NpcData.id(args[3]),page=args.length==5?NpcData.id(args[4]):"";service.edit(id,d->{switch(args[2].toLowerCase(Locale.ROOT)){case "glass"->{d.glassNpc=npc;d.glassPage=page;}case "minion"->{d.minionNpc=npc;d.minionPage=page;}case "boss"->{d.bossNpc=npc;d.bossPage=page;}default->throw new IllegalArgumentException("Tahap glass|minion|boss.");}});tell(sender,"NPC "+npc+" dihubungkan ke "+args[2]+". Buat pilihan DUNGEON_"+args[2].toUpperCase(Locale.ROOT)+" di dialog /vnpc.");}
                case "reward"->{arity(args,3,"/vdungeon <id> reward glass|minion|boss");service.rewardMenu(player(sender),id,args[2].toLowerCase(Locale.ROOT));}
                case "time"->{arity(args,4,"/vdungeon <id> time glass|escape|portal <detik>");int n=number(args[3],1,3600);service.edit(id,d->{switch(args[2].toLowerCase(Locale.ROOT)){case "glass"->d.glassSeconds=n;case "escape"->d.escapeSeconds=n;case "portal"->d.portalAfter=n;default->throw new IllegalArgumentException("Waktu glass|escape|portal.");}});tell(sender,"Waktu "+args[2]+" = "+n+" detik. Default: kaca 180, keluar 30, portal setelah 15.");}
                case "fade"->{arity(args,4,"/vdungeon <id> fade entry|exit on|off");if(!Set.of("on","off").contains(args[3].toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Pilih on atau off.");boolean on=args[3].equalsIgnoreCase("on");service.edit(id,d->{if(args[2].equalsIgnoreCase("entry"))d.fadeEntry=on;else if(args[2].equalsIgnoreCase("exit"))d.fadeExit=on;else throw new IllegalArgumentException("Fade hanya entry|exit.");});tell(sender,"Fade "+args[2]+" "+args[3]+". Sakelar utama tetap /vitae fade on|off.");}
                case "check"->{arity(args,2,"/vdungeon <id> check");service.check(service.get(id));tell(sender,"Setup valid. Berikutnya /vdungeon "+id+" on lalu open.");}
                case "on","off"->{arity(args,2,"/vdungeon <id> on|off");service.enable(id,op.equals("on"));tell(sender,"Dungeon "+op+".");}
                case "open"->{arity(args,2,"/vdungeon <id> open");service.open(id);tell(sender,"Sesi dibuka. Peserta masuk melalui portal; NPC kaca memulai permainan bersama.");}
                case "stop","reset"->{arity(args,2,"/vdungeon <id> stop|reset");service.stop(id,"Sesi dihentikan admin.");tell(sender,"Peserta dipulangkan dan blok dipulihkan bertahap. Cek status sebelum open kembali.");}
                case "status"->{arity(args,2,"/vdungeon <id> status");service.status(sender,id);}
                case "join"->{if(args.length>3)throw new IllegalArgumentException("/vdungeon <id> join [pemain]");Player target=args.length==3?Bukkit.getPlayerExact(args[2]):player(sender);if(target==null)throw new IllegalArgumentException("Pemain tidak online.");service.join(target,id);}
                case "receipt"->{if(args.length<3||args.length>4)throw new IllegalArgumentException("/vdungeon <id> receipt glass|minion|boss [retry|delivered]");service.receipt(sender,id,args[2],args.length==4?args[3]:"status");}
                default->{help(sender,"all");throw new IllegalArgumentException("Subcommand tidak dikenal: "+op);}
            }
        }catch(Exception error){sender.sendMessage(Component.text("[Vitae Dungeon] "+Objects.requireNonNullElse(error.getMessage(),"Periksa log server."),NamedTextColor.RED));}
        return true;
    }
    private void action(CommandSender sender,String[] args)throws Exception{
        if(args.length<5)throw new IllegalArgumentException("/vdungeon action <id> <id-npc> teleport|spawn ... • /vdungeon help aksi");Action a=new Action();a.id=NpcData.id(args[1]);if(a.id.length()>41)throw new IllegalArgumentException("ID aksi maksimal 41 huruf agar target pilihan NPC muat.");a.npc=NpcData.id(args[2]);a.kind=args[3].toLowerCase(Locale.ROOT);a.enabled=true;
        if(a.kind.equals("teleport")){if(args.length<5||args.length>6)throw new IllegalArgumentException("/vdungeon action <id> <npc> teleport here [on|off untuk fade]");if(!args[4].equalsIgnoreCase("here"))throw new IllegalArgumentException("Berdiri di tujuan, gunakan teleport here.");if(args.length==6&&!Set.of("on","off").contains(args[5].toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Fade on|off.");a.point=point(player(sender).getLocation());a.fade=args.length==6&&args[5].equalsIgnoreCase("on");}
        else if(a.kind.equals("spawn")){if(args.length<6||args.length>7)throw new IllegalArgumentException("/vdungeon action <id> <npc> spawn vanilla:TYPE|mythic:ID <jumlah> [level]");a.mob=new Mob(args[4],number(args[5],1,64),args.length==7?Double.parseDouble(args[6]):1,point(player(sender).getLocation()));}
        else throw new IllegalArgumentException("Aksi teleport|spawn.");service.actionEdit(a);tell(sender,"Aksi tersimpan. Pilihan NPC memakai target ACTION_"+a.id+". Contoh: /vnpc "+a.npc+" option start ACTION_"+a.id+" Lanjutkan");
    }
    private Box selection(Player p){Selection s=selections.get(p.getUniqueId());if(s==null||s.a==null||s.b==null)throw new IllegalArgumentException("Tandai /vdungeon <id> pos1 dan pos2 dahulu.");return Box.between(s.a,s.b);}
    private Player player(CommandSender s){if(!(s instanceof Player p))throw new IllegalArgumentException("Perintah lokasi/seleksi/GUI harus dilakukan di dalam game.");return p;}
    private int number(String s,int min,int max){int n=Integer.parseInt(s);if(n<min||n>max)throw new IllegalArgumentException("Angka harus "+min+"–"+max);return n;}
    private void arity(String[] a,int n,String usage){if(a.length!=n)throw new IllegalArgumentException(usage);}
    private void tell(CommandSender s,String text){DungeonService.tell(s,text);}
    private void help(CommandSender s,String topic){
        s.sendMessage(Component.empty());s.sendMessage(Component.text("━━ Vitae Dungeon • "+topic+" ━━",NamedTextColor.GOLD));
        switch(topic.toLowerCase(Locale.ROOT)){
            case "portal"->{tell(s,"create <id> <world> • <id> pos1|pos2 • <id> region entrance|exit");tell(s,"Berdiri di tujuan: <id> location entry|home. Isi portal keluar harus udara; bingkai tidak ikut diseleksi.");tell(s,"<id> fade entry|exit on|off • <id> time escape 30 • <id> time portal 15");}
            case "kaca"->{tell(s,"<id> region arena|camera|finish • <id> location start|camera|next");tell(s,"Posisi start wajib di area pemicu camera. Lokasi camera merekam posisi, yaw, dan pitch admin; durasi 5 detik.");tell(s,"Pilih dua sudut petak salah 3×3 satu lapis: <id> bad add <nama> • bad remove <nama>");tell(s,"<id> time glass 180 • <id> npc glass <npc> [page-selamat] • target dialog DUNGEON_GLASS");}
            case "minion"->{tell(s,"<id> region miniongate|miniontrigger • <id> npc minion <npc> [page-selamat]");tell(s,"Di lokasi spawn: <id> spawn <nama-titik> mythic:ID|vanilla:COW <jumlah> [level]. Ulangi untuk tiap titik.");tell(s,"Target dialog DUNGEON_MINION. Progres semua peserta sama; hanya mob sesi yang dihitung.");}
            case "bos"->{tell(s,"<id> region bossgate|bosstrigger • <id> boss mythic:ID [level]");tell(s,"<id> npc boss <npc> [page-selamat]. Target dialog DUNGEON_BOSS. Syarat kaca dan minion selesai.");}
            case "hadiah"->{tell(s,"<id> reward glass|minion|boss → letakkan item dalam GUI → tutup untuk simpan; item admin dikembalikan.");tell(s,"Halaman selamat memakai teks dari NPC; pilihan Ambil hadiah diberikan otomatis untuk penerima terpilih.");tell(s,"Belum pernah menerima diprioritaskan; jika tidak ada, peserta sebelumnya boleh mendapat lagi.");}
            case "aksi"->{tell(s,"action <id> <npc> teleport here • action <id> <npc> spawn mythic:ID|vanilla:TYPE <jumlah> [level]");tell(s,"/vnpc <npc> option <halaman> ACTION_<id> <label pilihan>. Aksi ini bisa digunakan di luar dungeon.");}
            case "sesi"->{tell(s,"<id> check → on → open. Saat satu peserta menerima kaca, semua peserta aktif di world mengikuti.");tell(s,"<id> status|stop|reset • list • <id> join [pemain]. Pemain mati/disconnect saat permainan tidak bisa masuk lagi.");tell(s,"Reset memulihkan blok bertahap. Setelah selesai, open membuat sesi baru.");}
            default->{tell(s,"/vdungeon create <id> <world> • /vdungeon list");tell(s,"/vdungeon help portal|kaca|minion|bos|hadiah|aksi|sesi");tell(s,"Urutan: world/portal → kaca/kamera → NPC/minion/bos → hadiah → check → on → open.");}
        }
        s.sendMessage(Component.empty());
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){
        if(!sender.hasPermission("vitae.admin.dungeon"))return List.of();List<String> values=List.of();
        if(args.length==1){var v=new ArrayList<>(service.current().definitions.keySet());v.addAll(List.of("help","create","list","action"));values=v;}
        else if(args.length==2)values=args[0].equalsIgnoreCase("help")?TOPICS:OPS;
        else if(args.length==3)values=switch(args[1].toLowerCase(Locale.ROOT)){case "region"->List.of("entrance","arena","camera","finish","miniongate","miniontrigger","bossgate","bosstrigger","exit");case "location"->List.of("entry","home","start","camera","next");case "reward","npc","receipt"->List.of("glass","minion","boss");case "bad"->List.of("add","remove");case "time"->List.of("glass","escape","portal");case "fade"->List.of("entry","exit");default->List.of();};
        else if(args.length==4&&args[1].equalsIgnoreCase("fade"))values=List.of("on","off");
        String prefix=args[args.length-1].toLowerCase(Locale.ROOT);return values.stream().filter(v->v.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/story/DungeonData.java'
        OriginalHashes = @()
        Hash = '634488fce5589167a209de38bec45b88775cf78863cd8c4670b0b52d89f72de0'
        Required = $false
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.story;

import com.bangzachery.vitae.vitaemanager.npc.NpcData;
import org.bukkit.*;
import java.util.*;

public final class DungeonData {
    private DungeonData() {}
    public enum Phase { WAITING, CAMERA, GLASS, GLASS_DONE, MINION_READY, MINION, MINION_DONE, BOSS_READY, BOSS, REWARD, ESCAPE, COMPLETE, ABORTED }
    public enum Status { ACTIVE, PASSED, DEAD, EXITED }
    public enum ReceiptState { RESERVED, DELIVERING, CLAIMED }
    public record Box(UUID world, int x1, int y1, int z1, int x2, int y2, int z2) {
        public Box {
            Objects.requireNonNull(world);
            if (x1 > x2 || y1 > y2 || z1 > z2 || Math.abs((long)x1) > 30_000_000 || Math.abs((long)x2) > 30_000_000
                    || Math.abs((long)z1) > 30_000_000 || Math.abs((long)z2) > 30_000_000 || y1 < -4096 || y2 > 4096)
                throw new IllegalArgumentException("Seleksi area tidak valid.");
        }
        public static Box between(Location a, Location b) {
            if (!a.getWorld().equals(b.getWorld())) throw new IllegalArgumentException("Pos1 dan pos2 harus satu world.");
            return new Box(a.getWorld().getUID(), Math.min(a.getBlockX(),b.getBlockX()), Math.min(a.getBlockY(),b.getBlockY()), Math.min(a.getBlockZ(),b.getBlockZ()),
                    Math.max(a.getBlockX(),b.getBlockX()), Math.max(a.getBlockY(),b.getBlockY()), Math.max(a.getBlockZ(),b.getBlockZ()));
        }
        public boolean contains(Location p) { return p.getWorld() != null && world.equals(p.getWorld().getUID()) && contains(p.getX(),p.getY(),p.getZ()); }
        public boolean contains(double x,double y,double z) { return x >= x1 && x < x2+1.0 && y >= y1 && y < y2+1.0 && z >= z1 && z < z2+1.0; }
        public boolean contains(Box b) { return world.equals(b.world) && x1 <= b.x1 && x2 >= b.x2 && y1 <= b.y1 && y2 >= b.y2 && z1 <= b.z1 && z2 >= b.z2; }
        public boolean overlaps(Box b) { return world.equals(b.world) && x1 <= b.x2 && x2 >= b.x1 && y1 <= b.y2 && y2 >= b.y1 && z1 <= b.z2 && z2 >= b.z1; }
        public long volume() { return Math.multiplyExact(Math.multiplyExact(x2-x1+1L,y2-y1+1L),z2-z1+1L); }
        public boolean crosses(Location a, Location b) {
            if (a.getWorld() == null || b.getWorld() == null || !world.equals(a.getWorld().getUID()) || !world.equals(b.getWorld().getUID())) return false;
            double lo=0,hi=1; double[] start={a.getX(),a.getY(),a.getZ()}, end={b.getX(),b.getY(),b.getZ()}, min={x1,y1,z1}, max={x2+1.0,y2+1.0,z2+1.0};
            for(int i=0;i<3;i++) { double d=end[i]-start[i]; if(Math.abs(d)<1e-9) { if(start[i]<min[i] || start[i]>=max[i])return false; }
                else { double t1=(min[i]-start[i])/d,t2=(max[i]-start[i])/d;lo=Math.max(lo,Math.min(t1,t2));hi=Math.min(hi,Math.max(t1,t2));if(lo>hi)return false; } }
            return true;
        }
    }
    public record Block(UUID world,int x,int y,int z,String data) {
        public Block{Objects.requireNonNull(world);if(Math.abs((long)x)>30_000_000||Math.abs((long)z)>30_000_000||Math.abs((long)y)>4096||data==null||data.isBlank()||data.length()>4096)throw new IllegalArgumentException("Snapshot blok tidak valid.");}
        public String key() { return world+":"+x+":"+y+":"+z; }
        public Location location() { World w=Bukkit.getWorld(world); if(w==null)throw new IllegalStateException("World snapshot belum dimuat.");return new Location(w,x,y,z); }
    }
    public record Mob(String type, int count, double level, NpcData.Point point) {
        public Mob { if(type==null || !type.matches("(vanilla|mythic):[A-Za-z0-9_-]{1,100}") || count<1 || count>4096 || !Double.isFinite(level) || level<=0 || level>10000 || point==null)
            throw new IllegalArgumentException("Mob: vanilla:COW atau mythic:ID, jumlah 1–4096, level > 0."); }
    }
    public static final class Definition {
        public String id;
        public boolean enabled;
        public UUID world;
        public Box entrance,arena,cameraZone,finish,minionGate,minionTrigger,bossGate,bossTrigger,exit;
        public NpcData.Point entry,home,start,camera,next;
        public Map<String,Box> bad = new LinkedHashMap<>();
        public Map<String,Mob> minions = new LinkedHashMap<>();
        public Map<String,List<String>> rewards = new LinkedHashMap<>();
        public Mob boss;
        public String glassNpc="",minionNpc="",bossNpc="";
        public String glassPage="",minionPage="",bossPage="";
        public int glassSeconds=180,cameraTicks=100,escapeSeconds=30,portalAfter=15;
        public boolean fadeEntry,fadeExit;
        public Definition(String id,UUID world) { this.id=NpcData.id(id);this.world=world; }
        public void validate() {
            NpcData.id(id);Objects.requireNonNull(world,"World dungeon belum diatur.");
            if(bad==null || minions==null || rewards==null || glassSeconds<10 || glassSeconds>3600 || cameraTicks!=100 || escapeSeconds<5 || escapeSeconds>300 || portalAfter<1 || portalAfter>=escapeSeconds)
                throw new IllegalArgumentException("Pengaturan waktu dungeon tidak valid.");
            if(!enabled)return;
            for(Object obj:new Object[]{entrance,arena,cameraZone,finish,minionGate,minionTrigger,bossGate,bossTrigger,exit,entry,home,start,camera,next,boss})
                Objects.requireNonNull(obj,"Setup belum lengkap. Gunakan /vdungeon "+id+" check.");
            for(Box b:List.of(arena,cameraZone,finish,minionGate,minionTrigger,bossGate,bossTrigger,exit)) if(!world.equals(b.world))throw new IllegalArgumentException("Area permainan harus berada di world dungeon.");
            for(NpcData.Point p:List.of(entry,start,camera,next,boss.point()))if(!world.equals(p.world()))throw new IllegalArgumentException("Lokasi permainan harus berada di world dungeon.");
            if(world.equals(home.world()))throw new IllegalArgumentException("Lokasi home harus di luar world dungeon.");
            if(!cameraZone.contains(location(start)))throw new IllegalArgumentException("Area kamera harus mencakup posisi start agar pengenalan dimulai serentak.");
            if(!arena.contains(cameraZone) || !arena.contains(finish) || !arena.contains(location(start)))throw new IllegalArgumentException("Start, kamera pemicu, dan finish harus di dalam arena kaca.");
            if(exit.volume()>256 || minionGate.volume()>32768 || bossGate.volume()>32768)throw new IllegalArgumentException("Portal maks. 256 blok; gerbang maks. 32768 blok yang akan dipulihkan.");
            if(minionGate.overlaps(bossGate) || exit.overlaps(minionGate) || exit.overlaps(bossGate))throw new IllegalArgumentException("Gerbang dan portal keluar tidak boleh bertumpuk.");
            if(bad.isEmpty() || minions.isEmpty() || minions.values().stream().mapToLong(Mob::count).sum()>8192)throw new IllegalArgumentException("Isi pijakan salah dan spawn minion (total maks. 8192). Batas ini hanya mob aktif, bukan luas world.");
            for(Box b:bad.values()) if(!arena.contains(b) || b.y1!=b.y2 || b.x2-b.x1!=2 || b.z2-b.z1!=2 || b.overlaps(finish))throw new IllegalArgumentException("Pijakan salah harus petak kaca 3×3, satu lapis, di arena dan tidak menimpa finish.");
            var list=new ArrayList<>(bad.values());for(int i=0;i<list.size();i++)for(int j=i+1;j<list.size();j++)if(list.get(i).overlaps(list.get(j)))throw new IllegalArgumentException("Pijakan salah bertumpuk.");
            for(Mob m:minions.values())if(!world.equals(m.point.world()))throw new IllegalArgumentException("Spawn minion berada di world lain.");
            for(String npc:List.of(glassNpc,minionNpc,bossNpc))NpcData.id(npc);
            for(String stage:List.of("glass","minion","boss"))if(!rewards.containsKey(stage)||rewards.get(stage).isEmpty()||rewards.get(stage).size()>27)throw new IllegalArgumentException("Hadiah item "+stage+" belum disetel.");
            if(new HashSet<>(List.of(glassNpc,minionNpc,bossNpc)).size()!=3)throw new IllegalArgumentException("Gunakan tiga NPC berbeda untuk tiga tahap.");
        }
    }
    public static final class Participant {
        public Status status=Status.ACTIVE;
        public boolean falling,cameraDone;
        public NpcData.Point recovery;
        public String mode;
        public boolean flight,flying;
        public Participant() {}
    }
    public static final class Award {
        public UUID receipt=UUID.randomUUID(),player;
        public String stage,npc,page="";
        public List<String> items=new ArrayList<>();
        public ReceiptState state=ReceiptState.RESERVED;
        public long chosenAt;
    }
    public static final class Run {
        public UUID token=UUID.randomUUID();
        public String dungeon;
        public Phase phase=Phase.WAITING;
        public Map<UUID,Participant> participants=new LinkedHashMap<>();
        public List<Block> blocks=new ArrayList<>();
        public Map<UUID,String> mobs=new LinkedHashMap<>();
        public Map<String,Award> awards=new LinkedHashMap<>();
        public Set<String> broken=new HashSet<>();
        public long deadline;
        public int killed,total;
        public boolean restoring;
        public Run(String id) {dungeon=id;}
        public boolean active() {return phase!=Phase.COMPLETE && phase!=Phase.ABORTED;}
    }
    public static final class Action {
        public String id,npc,kind;
        public boolean fade;
        public NpcData.Point point;
        public Mob mob;
        public boolean enabled;
    }
    public static final class State {
        public int version=1;
        public Map<String,Definition> definitions=new LinkedHashMap<>();
        public Map<String,Run> runs=new LinkedHashMap<>();
        public Map<String,Action> actions=new LinkedHashMap<>();
        public Map<UUID,NpcData.Point> returns=new LinkedHashMap<>();
        public void validate() {
            if(version!=1 || definitions==null || runs==null || actions==null || returns==null)throw new IllegalArgumentException("Format dungeon tidak valid.");
            definitions.forEach((id,d)->{if(!id.equals(d.id))throw new IllegalArgumentException("ID dungeon tidak cocok.");d.validate();});
            runs.forEach((id,r)->{if(!id.equals(r.dungeon) || !definitions.containsKey(id) || r.token==null || r.phase==null || r.participants==null || r.blocks==null || r.mobs==null || r.awards==null || r.broken==null || r.killed<0 || r.total<0 || r.killed>r.total)
                throw new IllegalArgumentException("State sesi dungeon tidak valid.");
                r.participants.forEach((uuid,p)->{Objects.requireNonNull(uuid);if(p==null||p.status==null)throw new IllegalArgumentException("Peserta sesi tidak valid.");if(p.mode!=null)GameMode.valueOf(p.mode);});
                r.awards.forEach((stage,a)->{if(a==null||a.receipt==null||a.player==null||a.state==null||!stage.equals(a.stage)||a.items==null||a.items.size()>27)throw new IllegalArgumentException("Receipt hadiah tidak valid.");});
            });
            actions.forEach((id,a)->{NpcData.id(id);NpcData.id(a.npc);if(!id.equals(a.id) || !("teleport".equals(a.kind)||"spawn".equals(a.kind)) || ("teleport".equals(a.kind)?a.point==null:a.mob==null))throw new IllegalArgumentException("Aksi NPC tidak valid.");});
        }
    }
    public static NpcData.Point point(Location l) {return new NpcData.Point(l.getWorld().getUID(),l.getX(),l.getY(),l.getZ(),l.getYaw(),l.getPitch());}
    public static Location location(NpcData.Point p) {
        if(p==null)throw new IllegalArgumentException("Lokasi belum disetel.");World w=Bukkit.getWorld(p.world());
        if(w==null)throw new IllegalStateException("World belum dimuat: "+p.world());return new Location(w,p.x(),p.y(),p.z(),p.yaw(),p.pitch());
    }
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/story/DungeonMobs.java'
        OriginalHashes = @()
        Hash = '10f9873028138085da54143a53553ed7e56c2373c6d4df4d8484ff2fe94b38d6'
        Required = $false
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.story;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Predicate;

/** Optional MythicMobs 5.x adapter: its classes are loaded only from MythicMobs. */
public final class DungeonMobs implements AutoCloseable {
    private final JavaPlugin plugin;
    private final Listener spawnListener=new Listener(){};
    public final NamespacedKey sessionKey;
    public DungeonMobs(JavaPlugin plugin){this.plugin=plugin;sessionKey=new NamespacedKey(plugin,"dungeon_session");}
    @SuppressWarnings("unchecked")
    public void guardNaturalSpawns(Predicate<World> protectedWorld){
        Plugin mythic=Bukkit.getPluginManager().getPlugin("MythicMobs");if(mythic==null||!mythic.isEnabled())return;
        try{
            Class<?> event=Class.forName("io.lumine.mythic.bukkit.events.MythicMobSpawnEvent",true,mythic.getClass().getClassLoader());
            Method location=event.getMethod("getLocation"),fromSpawner=event.getMethod("isFromMythicSpawner"),reason=event.getMethod("getSpawnReason");
            Bukkit.getPluginManager().registerEvent((Class<? extends Event>)event,spawnListener,EventPriority.HIGHEST,(listener,e)->{
                try{
                    Location at=(Location)location.invoke(e);if(at==null||!protectedWorld.test(at.getWorld()))return;
                    Object why=reason.invoke(e);boolean natural=Boolean.TRUE.equals(fromSpawner.invoke(e));
                    if(why!=null){Object bukkitReason=why.getClass().getMethod("getBukkitReason").invoke(why);natural|=bukkitReason==CreatureSpawnEvent.SpawnReason.NATURAL||bukkitReason==CreatureSpawnEvent.SpawnReason.SPAWNER;}
                    if(natural)((Cancellable)e).setCancelled(true);
                }catch(ReflectiveOperationException error){throw new EventException(error);}
            },plugin,true);
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Pelindung spawn MythicMobs tidak cocok.",error);}
    }
    private Object manager()throws ReflectiveOperationException{
        Plugin mythic=Bukkit.getPluginManager().getPlugin("MythicMobs");if(mythic==null || !mythic.isEnabled())throw new IllegalStateException("MythicMobs belum aktif.");
        Class<?> api=Class.forName("io.lumine.mythic.bukkit.MythicBukkit",true,mythic.getClass().getClassLoader());
        Object core=api.getMethod("inst").invoke(null);return api.getMethod("getMobManager").invoke(core);
    }
    public void validate(DungeonData.Mob m){
        try{
            String name=m.type().substring(m.type().indexOf(':')+1);
            if(m.type().startsWith("vanilla:")){EntityType type=EntityType.valueOf(name.toUpperCase(Locale.ROOT));if(!type.isAlive()||!type.isSpawnable()||type==EntityType.PLAYER)throw new IllegalArgumentException("Mob vanilla tidak dapat dipanggil.");}
            else {Object manager=manager();Object found=manager.getClass().getMethod("getMythicMob",String.class).invoke(manager,name);if(!(found instanceof Optional<?> opt)||opt.isEmpty())throw new IllegalArgumentException("ID MythicMob tidak ditemukan: "+name);}
        }catch(ReflectiveOperationException e){throw new IllegalStateException("API MythicMobs belum cocok: "+root(e),e);}
    }
    public LivingEntity spawn(DungeonData.Mob m,UUID session){
        Location at=DungeonData.location(m.point());at.getChunk().load();Entity entity;
        try{
            String name=m.type().substring(m.type().indexOf(':')+1);
            if(m.type().startsWith("vanilla:"))entity=at.getWorld().spawnEntity(at,EntityType.valueOf(name.toUpperCase(Locale.ROOT)),CreatureSpawnEvent.SpawnReason.CUSTOM);
            else {Object manager=manager();Object active=manager.getClass().getMethod("spawnMob",String.class,Location.class,double.class).invoke(manager,name,at,m.level());
                if(active==null)throw new IllegalStateException("MythicMobs menolak spawn "+name);Object wrapped=active.getClass().getMethod("getEntity").invoke(active);entity=(Entity)wrapped.getClass().getMethod("getBukkitEntity").invoke(wrapped);}
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Pemanggilan MythicMob gagal: "+root(e),e);}
        if(!(entity instanceof LivingEntity living)||!entity.isValid()||living.isDead()){if(entity!=null)entity.remove();throw new IllegalStateException("Spawn dibatalkan atau mob bukan LivingEntity.");}
        living.setPersistent(true);living.setRemoveWhenFarAway(false);if(session!=null)living.getPersistentDataContainer().set(sessionKey,PersistentDataType.STRING,session.toString());return living;
    }
    public void remove(UUID id){Entity entity=Bukkit.getEntity(id);if(entity==null)return;
        Plugin mythic=Bukkit.getPluginManager().getPlugin("MythicMobs");
        if(mythic!=null && mythic.isEnabled())try{Object manager=manager();Object found=manager.getClass().getMethod("getActiveMob",UUID.class).invoke(manager,id);
            if(found instanceof Optional<?> opt && opt.isPresent()){Object active=opt.get();try{active.getClass().getMethod("remove").invoke(active);}catch(NoSuchMethodException ignored){entity.remove();}}}
        catch(ReflectiveOperationException e){plugin.getLogger().warning("Cleanup MythicMob: "+root(e));}
        if(entity.isValid())entity.remove();
    }
    private String root(Throwable e){while(e.getCause()!=null)e=e.getCause();return Objects.requireNonNullElse(e.getMessage(),e.getClass().getSimpleName());}
    @Override public void close(){HandlerList.unregisterAll(spawnListener);}
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/story/DungeonService.java'
        OriginalHashes = @()
        Hash = 'cde250a97216bb4e7e214f5d6ff2f6b1ee050697fa28a53300bfa7608b952794'
        Required = $false
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.story;

import com.bangzachery.vitae.vitaemanager.core.gui.VitaeMenu;
import com.bangzachery.vitae.vitaemanager.npc.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.TileState;
import org.bukkit.boss.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.world.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import static com.bangzachery.vitae.vitaemanager.story.DungeonData.*;
import com.bangzachery.vitae.vitaemanager.story.DungeonData.Mob;
import com.bangzachery.vitae.vitaemanager.story.DungeonData.Action;

public final class DungeonService implements Listener,AutoCloseable {
    private final JavaPlugin plugin;
    private final StoryStore<State> store;
    private State state;
    private final FadeService fade;
    private final DungeonMobs mobs;
    private final DungeonCamera camera;
    private final Set<UUID> internal=new HashSet<>(),hardDeaths=new HashSet<>();
    private final Map<UUID,Long> portals=new HashMap<>(),warns=new HashMap<>(),actionClicks=new HashMap<>();
    private final Map<String,BossBar> bars=new HashMap<>();
    private final Map<String,Set<UUID>> finishes=new HashMap<>();
    private final Map<String,Long> decideAt=new HashMap<>();
    private final Map<String,ArrayDeque<Mob>> spawnQueues=new HashMap<>();
    private record WorldJob(String dungeon,Runnable work){}
    private final ArrayDeque<WorldJob> worldWork=new ArrayDeque<>();
    private final Map<String,Set<Chunk>> tickets=new HashMap<>();
    private final Map<String,Integer> outstanding=new HashMap<>();
    private final NamespacedKey receiptKey;
    private BukkitTask task;
    private long tick;
    private boolean closed;
    public DungeonService(JavaPlugin plugin,FadeService fade)throws IOException{
        this.plugin=plugin;this.fade=fade;mobs=new DungeonMobs(plugin);receiptKey=new NamespacedKey(plugin,"dungeon_rewards");
        store=new StoryStore<>(plugin.getDataFolder().toPath().resolve("dungeons.json"),State.class);state=store.load(new State());state.validate();
        for(Definition d:state.definitions.values())for(List<String> items:d.rewards.values())for(String item:items)NpcService.decode(item);
        camera=new DungeonCamera(plugin,this::teleport);
    }
    public State current(){return state;}
    public Definition get(String id){Definition d=state.definitions.get(id);if(d==null)throw new IllegalArgumentException("Dungeon tidak ditemukan. /vdungeon list");return d;}
    public void start()throws IOException{
        Bukkit.getPluginManager().registerEvents(this,plugin);
        mobs.guardNaturalSpawns(w->definition(w)!=null);
        for(Definition d:state.definitions.values())if(d.enabled){World w=Bukkit.getWorld(d.world);if(w!=null)w.setGameRule(GameRule.DO_MOB_SPAWNING,false);}
        // A restart stops the previous scene instead of resuming a partly executed spawn/camera.
        for(Run r:state.runs.values())if(r.active()||r.restoring){r.phase=Phase.ABORTED;r.restoring=true;
            for(var e:r.participants.entrySet())if(e.getValue().status!=Status.DEAD&&e.getValue().status!=Status.EXITED){e.getValue().status=Status.EXITED;state.returns.put(e.getKey(),get(r.dungeon).home);}
            queueRestore(r);}
        save();task=Bukkit.getScheduler().runTaskTimer(plugin,this::tick,1,1);
        for(Player p:Bukkit.getOnlinePlayers())recover(p);
    }
    private void save()throws IOException{store.save(state);}
    private State draft(){State next=store.copy(state);next.runs=new LinkedHashMap<>(state.runs);return next;}
    private void persist(){try{save();}catch(IOException e){throw new IllegalStateException("State dungeon belum tersimpan. Permainan dihentikan agar progres tidak rusak.",e);}}
    public void create(String id,World world)throws IOException{
        if(state.definitions.containsKey(id))throw new IllegalArgumentException("ID sudah digunakan.");
        State next=draft();next.definitions.put(id,new Definition(id,world.getUID()));store.save(next);state=next;
    }
    public void edit(String id,Consumer<Definition> change)throws IOException{
        Run r=state.runs.get(id);if(r!=null && (r.active()||r.restoring||outstanding.containsKey(id)))throw new IllegalStateException("Selesaikan/reset sesi sebelum mengubah setup.");
        State next=draft();Definition d=next.definitions.get(id);if(d==null)throw new IllegalArgumentException("ID tidak ditemukan.");change.accept(d);d.enabled=false;d.validate();store.save(next);state=next;
    }
    public void enable(String id,boolean enabled)throws IOException{
        if(!enabled){Run r=state.runs.get(id);if(r!=null&&r.active())stop(id,"Dungeon dinonaktifkan.");}
        State next=draft();Definition d=next.definitions.get(id);if(d==null)throw new IllegalArgumentException("ID tidak ditemukan.");d.enabled=enabled;
        if(enabled){check(d);for(Definition other:next.definitions.values())if(other!=d&&other.enabled&&(d.world.equals(other.world)||d.entrance.overlaps(other.entrance)))throw new IllegalArgumentException("Satu sesi dungeon per world; portal masuk tidak boleh bertumpuk.");}
        store.save(next);state=next;if(enabled)Bukkit.getWorld(d.world).setGameRule(GameRule.DO_MOB_SPAWNING,false);
    }
    public void check(Definition d){
        boolean before=d.enabled;d.enabled=true;try{d.validate();}finally{d.enabled=before;}
        World world=Bukkit.getWorld(d.world);if(world==null)throw new IllegalArgumentException("World dungeon belum dimuat.");
        for(NpcData.Point p:List.of(d.entry,d.home,d.start,d.next))safe(p);
        for(String npc:List.of(d.glassNpc,d.minionNpc,d.bossNpc)){
            var n=npc().get(npc);if(!n.enabled())throw new IllegalArgumentException("NPC belum ON: "+npc);
            if(!d.world.equals(n.location().world()))throw new IllegalArgumentException("NPC harus di world dungeon: "+npc);
            if(!n.gift().empty()||n.quest().kind()!=NpcData.QuestKind.NONE)throw new IllegalArgumentException("NPC dungeon harus tanpa gift/quest lama. Hadiah diatur dengan /vdungeon <id> reward <tahap>.");
            safe(n.stand());
        }
        for(var pair:List.of(Map.entry(d.glassNpc,d.glassPage),Map.entry(d.minionNpc,d.minionPage),Map.entry(d.bossNpc,d.bossPage)))if(!pair.getValue().isEmpty()&&!npc().get(pair.getKey()).nodes().containsKey(pair.getValue()))throw new IllegalArgumentException("Halaman selamat tidak ada: "+pair.getValue());
        for(Box b:d.bad.values())for(Block block:capture(b,false))if(!Bukkit.createBlockData(block.data()).getMaterial().name().endsWith("GLASS"))throw new IllegalArgumentException("Petak salah hanya boleh berisi blok kaca penuh.");
        capture(d.minionGate,true);capture(d.bossGate,true);
        for(Block block:capture(d.exit,false))if(!Bukkit.createBlockData(block.data()).getMaterial().isAir())throw new IllegalArgumentException("Bagian dalam portal keluar harus kosong/udara sebelum sesi.");
        for(Mob m:d.minions.values())mobs.validate(m);mobs.validate(d.boss);
        for(List<String> items:d.rewards.values())for(String item:items)NpcService.decode(item);
    }
    private void safe(NpcData.Point point){Location l=location(point);if(l.getY()<l.getWorld().getMinHeight()||l.getY()+1>=l.getWorld().getMaxHeight()||!l.getBlock().isPassable()||!l.clone().add(0,1,0).getBlock().isPassable())throw new IllegalArgumentException("Lokasi teleport terhalang: "+l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ());}
    private NpcService npc(){NpcService service=NpcModule.active(plugin);if(service==null)throw new IllegalStateException("Modul NPC belum aktif. /vnpc status");return service;}
    private List<Block> capture(Box box,boolean gate){
        if(box.volume()>32768)throw new IllegalArgumentException("Snapshot terlalu besar.");World world=Bukkit.getWorld(box.world());if(world==null)throw new IllegalArgumentException("World area belum dimuat.");
        var result=new ArrayList<Block>();for(int x=box.x1();x<=box.x2();x++)for(int y=box.y1();y<=box.y2();y++)for(int z=box.z1();z<=box.z2();z++){
            var b=world.getBlockAt(x,y,z);if(b.getState() instanceof TileState || (gate&&Set.of(Material.BEDROCK,Material.BARRIER,Material.END_PORTAL_FRAME,Material.REINFORCED_DEEPSLATE).contains(b.getType())))throw new IllegalArgumentException("Seleksi memuat blok khusus atau penyimpan data; perkecil seleksi.");
            result.add(new Block(box.world(),x,y,z,b.getBlockData().getAsString()));
        }return result;
    }
    private Run run(Definition d){Run r=state.runs.get(d.id);if(r==null||!r.active())throw new IllegalStateException("Sesi belum dibuka. /vdungeon "+d.id+" open");return r;}
    public void open(String id)throws IOException{
        Definition d=get(id);if(!d.enabled)throw new IllegalStateException("Dungeon belum ON. /vdungeon "+id+" check lalu on.");check(d);
        Run old=state.runs.get(id);if(old!=null && (old.active()||old.restoring))throw new IllegalStateException("Sesi masih aktif/pemulihan belum selesai.");
        Run next=new Run(id);var snapshot=new LinkedHashMap<String,Block>();for(Box b:d.bad.values())for(Block block:capture(b,false))snapshot.put(block.key(),block);
        for(Box b:List.of(d.minionGate,d.bossGate,d.exit))for(Block block:capture(b,true))snapshot.put(block.key(),block);
        next.blocks.addAll(snapshot.values());State copy=draft();copy.runs.put(id,next);store.save(copy);state=copy;
    }
    public void join(Player p,String id){
        Definition d=get(id);if(!d.enabled)throw new IllegalStateException("Dungeon belum aktif.");Run r=state.runs.get(id);if(r==null)try{open(id);r=run(d);}catch(IOException e){throw new IllegalStateException(e);}
        if(!r.active()||r.phase!=Phase.WAITING)throw new IllegalStateException("Permainan sudah dimulai/selesai. Tunggu admin membuka sesi baru.");
        if(p.isDead()||p.getGameMode()==GameMode.SPECTATOR)throw new IllegalStateException("Pemain belum bisa masuk.");Participant before=r.participants.get(p.getUniqueId());if(before!=null&&before.status==Status.DEAD)throw new IllegalStateException("Kamu telah gagal dan tidak bisa masuk lagi pada sesi ini.");
        r.participants.putIfAbsent(p.getUniqueId(),new Participant());persist();Player player=p;
        fade.teleport(player,location(d.entry),d.fadeEntry,ok->{if(!ok)failed(player,"Teleport masuk ditolak.",false);else tell(player,"Terdaftar sebagai peserta dungeon. Tunggu permainan kaca dimulai.");});
    }
    public boolean carryBlocked(Player p){return definition(p.getWorld())!=null&&!bypass(p);}
    private Definition definition(World world){return state.definitions.values().stream().filter(d->d.enabled&&d.world.equals(world.getUID())).findFirst().orElse(null);}
    private boolean bypass(Player p){return p.hasPermission("vitae.dungeon.bypass");}
    private Participant participant(Run r,Player p){Participant v=r.participants.get(p.getUniqueId());if(v==null||v.status==Status.DEAD||v.status==Status.EXITED)throw new IllegalStateException("Kamu bukan peserta aktif sesi ini.");return v;}
    private boolean teleport(Player p,Location at){internal.add(p.getUniqueId());try{p.setFallDistance(0);p.setVelocity(new Vector());return p.teleport(at,PlayerTeleportEvent.TeleportCause.PLUGIN);}finally{internal.remove(p.getUniqueId());}}
    private void glass(Player actor,Definition d){
        Run r=run(d);if(r.phase!=Phase.WAITING)throw new IllegalStateException("Permainan kaca sudah dimulai.");
        for(Player p:Bukkit.getWorld(d.world).getPlayers())if(!p.isDead()&&p.getGameMode()!=GameMode.SPECTATOR&&!bypass(p))r.participants.putIfAbsent(p.getUniqueId(),new Participant());
        participant(r,actor);check(d);r.phase=Phase.CAMERA;
        for(var e:r.participants.entrySet()){Player p=Bukkit.getPlayer(e.getKey());Participant part=e.getValue();if(p==null||p.isDead()||part.status!=Status.ACTIVE){part.status=Status.DEAD;continue;}
            part.cameraDone=false;part.recovery=d.start;part.mode=p.getGameMode().name();part.flight=p.getAllowFlight();part.flying=p.isFlying();}
        persist();
        for(var e:r.participants.entrySet())if(e.getValue().status==Status.ACTIVE){Player p=Bukkit.getPlayer(e.getKey());npc().cancelStoryDialog(p);
            if(p.getVehicle()!=null)p.leaveVehicle();p.eject();if(!teleport(p,location(d.start)))throw new IllegalStateException("Teleport start ditolak untuk "+p.getName());
            camera.start(p,location(d.camera),e.getValue(),d.cameraTicks,()->{e.getValue().cameraDone=true;title(p,"Pilihlah pijakan yang benar","Satu langkah salah, kamu jatuh.");});}
        persist();broadcast(r,"Pengenalan arena dimulai. Timer berjalan setelah kamera selesai.");
    }
    private void accept(Player actor,Definition d,boolean boss){
        Run r=run(d);Participant p=participant(r,actor);if(p.status!=Status.PASSED)throw new IllegalStateException("Selesaikan permainan kaca terlebih dahulu.");
        if(r.phase!=(boss?Phase.MINION_DONE:Phase.GLASS_DONE))throw new IllegalStateException(boss?"Tahap kaca dan minion belum selesai.":"Permainan kaca belum selesai.");
        if(outstanding.containsKey(d.id))throw new IllegalStateException("Gerbang masih dipulihkan.");
        r.phase=boss?Phase.BOSS_READY:Phase.MINION_READY;persist();
        breakArea(r,boss?d.bossGate:d.minionGate);broadcast(r,boss?"Gerbang bos terbuka. Bersiaplah!":"Gerbang minion terbuka. Lewati gerbang untuk memulai pertarungan.");
        if(!boss)r.awards.put("minion-request",request(actor,d));persist();
    }
    private Award request(Player p,Definition d){Award a=new Award();a.player=p.getUniqueId();a.stage="minion-request";a.npc=d.minionNpc;return a;}
    private void spawn(Run r,Definition d,boolean boss){
        if(outstanding.containsKey(d.id))return;r.phase=boss?Phase.BOSS:Phase.MINION;r.killed=0;r.total=boss?1:d.minions.values().stream().mapToInt(Mob::count).sum();persist();
        ArrayDeque<Mob> queue=new ArrayDeque<>();for(Mob m:boss?List.of(d.boss):d.minions.values())for(int i=0;i<m.count();i++)queue.add(m);
        spawnQueues.put(d.id,queue);broadcast(r,boss?"Bos dungeon telah muncul!":"Musnahkan seluruh minion: 0/"+r.total);
    }
    private void combatComplete(Run r,Definition d){
        boolean boss=r.phase==Phase.BOSS;r.phase=boss?Phase.REWARD:Phase.MINION_DONE;persist();removeBar(d.id);
        if(boss){for(Player p:players(r)){npc().cancelStoryDialog(p);if(!teleport(p,location(npc().get(d.bossNpc).stand())))throw new IllegalStateException("Teleport kembali NPC ditolak.");}}
        UUID prefer=boss?null:Optional.ofNullable(r.awards.remove("minion-request")).map(a->a.player).orElse(null);
        choose(r,d,boss?"boss":"minion",prefer);broadcast(r,boss?"Bos dikalahkan! Tunggu hadiah diberikan, lalu segera bersiap keluar.":"Semua minion telah dimusnahkan.");
    }
    private void choose(Run r,Definition d,String stage,UUID prefer){
        List<UUID> candidates=new ArrayList<>();for(var e:r.participants.entrySet()){Player p=Bukkit.getPlayer(e.getKey());if(p!=null&&!p.isDead()&&e.getValue().status==Status.PASSED)candidates.add(e.getKey());}
        Set<UUID> previous=new HashSet<>();for(Award a:r.awards.values())if(a.state==ReceiptState.CLAIMED)previous.add(a.player);
        var unused=candidates.stream().filter(id->!previous.contains(id)).toList();if(!unused.isEmpty())candidates=new ArrayList<>(unused);
        if(candidates.isEmpty()){tellAdmins("Tidak ada peserta online untuk hadiah "+stage+". Hadiah akan dipilih saat peserta kembali.");return;}
        UUID winner=prefer!=null&&candidates.contains(prefer)?prefer:candidates.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(candidates.size()));
        Award a=new Award();a.player=winner;a.stage=stage;a.npc=switch(stage){case "glass"->d.glassNpc;case "minion"->d.minionNpc;default->d.bossNpc;};
        a.page=switch(stage){case "glass"->d.glassPage;case "minion"->d.minionPage;default->d.bossPage;};a.items=new ArrayList<>(d.rewards.get(stage));a.chosenAt=System.currentTimeMillis();r.awards.put(stage,a);persist();
        showAward(r,d,a);
    }
    private void showAward(Run r,Definition d,Award a){
        Player p=Bukkit.getPlayer(a.player);if(p==null||p.isDead())return;NpcService service=npc();service.cancelStoryDialog(p);
        if(!teleport(p,location(service.get(a.npc).stand())))throw new IllegalStateException("Posisi hadiah NPC terhalang/teleport ditolak.");
        service.storyReward(p,a.npc,a.page);tell(p,"Kamu terpilih menerima hadiah "+a.stage+". Pilih Ambil hadiah pada dialog.");
    }
    private void claim(Player p,Definition d,String npc){
        Run r=run(d);participant(r,p);Award a=r.awards.values().stream().filter(v->v.npc.equals(npc)&&v.player.equals(p.getUniqueId())&&v.state!=ReceiptState.CLAIMED&&!v.stage.endsWith("request")).findFirst().orElseThrow(()->new IllegalStateException("Tidak ada hadiah yang dipilih untukmu."));
        if(a.state==ReceiptState.DELIVERING){if(hasReceipt(p,a.receipt)){a.state=ReceiptState.CLAIMED;persist();}else throw new IllegalStateException("Pengiriman sebelumnya belum terkonfirmasi; admin perlu /vdungeon <id> receipt.");}
        else {
            ItemStack[] items=a.items.stream().map(NpcService::decode).toArray(ItemStack[]::new);a.state=ReceiptState.DELIVERING;persist();
            List<Item> dropped=new ArrayList<>();ItemStack[] before=Arrays.stream(p.getInventory().getStorageContents()).map(i->i==null?null:i.clone()).toArray(ItemStack[]::new);
            String marker=p.getPersistentDataContainer().get(receiptKey,PersistentDataType.STRING);
            try {
                if(!hasReceipt(p,a.receipt)){for(ItemStack item:p.getInventory().addItem(items).values()){Item drop=p.getWorld().dropItem(p.getLocation(),item);drop.setOwner(p.getUniqueId());drop.setPickupDelay(0);dropped.add(drop);}markReceipt(p,a.receipt);p.saveData();}
                a.state=ReceiptState.CLAIMED;persist();
            }catch(RuntimeException error){p.getInventory().setStorageContents(before);for(Item drop:dropped)drop.remove();if(marker==null)p.getPersistentDataContainer().remove(receiptKey);else p.getPersistentDataContainer().set(receiptKey,PersistentDataType.STRING,marker);a.state=ReceiptState.RESERVED;try{save();}catch(IOException e){error.addSuppressed(e);}throw error;}
        }
        tell(p,"Hadiah diberikan. Item yang tidak muat jatuh di dekatmu.");
        if(a.stage.equals("boss")){r.phase=Phase.ESCAPE;r.deadline=System.currentTimeMillis()+d.escapeSeconds*1000L;persist();for(Player player:players(r)){npc().cancelStoryDialog(player);title(player,"DUNGEON MULAI RUNTUH!","Segera keluar sebelum waktu habis!");player.playSound(player.getLocation(),"minecraft:entity.wither.spawn",.6f,.8f);}broadcast(r,"Portal keluar akan terbuka setelah "+d.portalAfter+" detik.");}
        else if(a.stage.equals("glass"))teleport(p,location(d.next));
    }
    private boolean hasReceipt(Player p,UUID id){String text=p.getPersistentDataContainer().get(receiptKey,PersistentDataType.STRING);return text!=null&&Arrays.asList(text.split(",")).contains(id.toString());}
    private void markReceipt(Player p,UUID id){String text=p.getPersistentDataContainer().get(receiptKey,PersistentDataType.STRING);ArrayDeque<String> ids=new ArrayDeque<>(text==null?List.of():Arrays.asList(text.split(",")));ids.add(id.toString());while(ids.size()>64)ids.removeFirst();p.getPersistentDataContainer().set(receiptKey,PersistentDataType.STRING,String.join(",",ids));}
    public void receipt(CommandSender sender,String id,String stage,String outcome){
        Run r=state.runs.get(id);if(r==null)throw new IllegalArgumentException("Sesi tidak ditemukan.");Award a=r.awards.get(stage);if(a==null)throw new IllegalArgumentException("Hadiah tidak ditemukan.");
        if(outcome.equals("retry")){if(a.state!=ReceiptState.DELIVERING)throw new IllegalArgumentException("Hanya receipt DELIVERING yang bisa dikoreksi.");Player p=Bukkit.getPlayer(a.player);if(p!=null&&hasReceipt(p,a.receipt))throw new IllegalArgumentException("Receipt sudah ada di playerdata; gunakan delivered.");a.state=ReceiptState.RESERVED;persist();}
        else if(outcome.equals("delivered")){a.state=ReceiptState.CLAIMED;persist();if(stage.equals("boss")&&r.phase==Phase.REWARD){r.phase=Phase.ESCAPE;r.deadline=System.currentTimeMillis()+get(id).escapeSeconds*1000L;persist();}}
        else tell(sender,"Receipt "+stage+": "+a.state+", pemain "+a.player+". Koreksi retry hanya jika dipastikan item belum diterima; delivered jika sudah diterima.");
    }
    public void stop(String id,String reason){
        Run r=state.runs.get(id);if(r==null||r.restoring)return;r.phase=Phase.ABORTED;r.restoring=true;
        spawnQueues.remove(id);finishes.remove(id);decideAt.remove(id);removeBar(id);
        worldWork.removeIf(job->job.dungeon.equals(id));outstanding.remove(id);
        for(var e:r.participants.entrySet()){Player p=Bukkit.getPlayer(e.getKey());if(p!=null)try{camera.stop(e.getKey(),false);camera.restore(p,e.getValue());npc().cancelStoryDialog(p);}catch(RuntimeException error){fault(error);}
            if(e.getValue().status!=Status.DEAD&&e.getValue().status!=Status.EXITED){e.getValue().status=Status.EXITED;state.returns.put(e.getKey(),get(id).home);}}
        try{save();}catch(IOException error){fault(error);}for(Player p:playersAll(r))recover(p);queueRestore(r);broadcast(r,reason);
    }
    private void queueRestore(Run r){
        releaseTickets(r.dungeon);
        for(UUID id:List.copyOf(r.mobs.keySet()))mobs.remove(id);r.mobs.clear();Definition d=get(r.dungeon);
        World w=Bukkit.getWorld(d.world);if(w!=null)for(Entity entity:w.getEntities())if(r.token.toString().equals(entity.getPersistentDataContainer().get(mobs.sessionKey,PersistentDataType.STRING)))mobs.remove(entity.getUniqueId());
        outstanding.put(d.id,r.blocks.size());
        for(Block block:r.blocks)worldWork.add(new WorldJob(r.dungeon,()->restoreBlock(r,block)));
        if(r.blocks.isEmpty()){r.restoring=false;outstanding.remove(d.id);}
    }
    private void restoreBlock(Run r,Block block){
        if(Bukkit.getWorld(block.world())==null){worldWork.add(new WorldJob(r.dungeon,()->restoreBlock(r,block)));return;}
        Location at=block.location();var bounds=new org.bukkit.util.BoundingBox(block.x(),block.y(),block.z(),block.x()+1,block.y()+1,block.z()+1);
        if(!Bukkit.createBlockData(block.data()).getMaterial().isAir()&&at.getWorld().getPlayers().stream().anyMatch(p->!p.isDead()&&p.getGameMode()!=GameMode.SPECTATOR&&p.getBoundingBox().overlaps(bounds))){worldWork.add(new WorldJob(r.dungeon,()->restoreBlock(r,block)));return;}
        at.getBlock().setBlockData(Bukkit.createBlockData(block.data()),false);int left=outstanding.compute(r.dungeon,(k,v)->v==null?0:v-1);if(left<=0){outstanding.remove(r.dungeon);r.restoring=false;persist();}
    }
    private void breakArea(Run r,Box area){
        List<Block> selected=r.blocks.stream().filter(b->area.contains(b.location())).toList();outstanding.merge(r.dungeon,selected.size(),Integer::sum);
        for(Block b:selected)worldWork.add(new WorldJob(r.dungeon,()->{var block=b.location().getBlock();block.getWorld().spawnParticle(Particle.BLOCK,block.getLocation().add(.5,.5,.5),8,.4,.4,.4,.1,block.getBlockData());block.setType(Material.AIR,false);
            int n=outstanding.compute(r.dungeon,(k,v)->v==null?0:v-1);if(n<=0)outstanding.remove(r.dungeon);}));
    }
    private Location breakTile(Player p,Run r,Definition d,String name,Box tile){
        Participant part=participant(r,p);part.falling=true;r.broken.add(name);persist();
        // The failing player cannot preserve horizontal momentum to escape the hole.
        Location below=p.getLocation().clone();below.setX(Math.max(tile.x1()+.6,Math.min(tile.x2()+.4,below.getX())));below.setZ(Math.max(tile.z1()+.6,Math.min(tile.z2()+.4,below.getZ())));below.setY(tile.y1()-.35);
        for(Block b:r.blocks)if(tile.contains(b.location()))b.location().getBlock().setType(Material.AIR,false);
        p.setVelocity(new Vector(0,-1.1,0));p.playSound(p.getLocation(),"minecraft:block.glass.break",1,.75f);tell(p,"Pijakan salah! Kamu gagal.");return below;
    }
    private void failed(Player p,String why,boolean kill){
        Definition d=definition(p.getWorld());if(d==null)return;Run r=state.runs.get(d.id);if(r==null)return;Participant part=r.participants.get(p.getUniqueId());if(part==null||part.status==Status.DEAD||part.status==Status.EXITED)return;
        camera.stop(p.getUniqueId(),false);part.status=Status.DEAD;persist();tell(p,why);if(kill&&!p.isDead()){hardDeaths.add(p.getUniqueId());p.setHealth(0);}
    }
    private void escape(Player p,Definition d,Run r){
        if(closed||r.phase!=Phase.ESCAPE||!r.broken.contains("exit-open"))return;Participant part=r.participants.get(p.getUniqueId());if(part==null||part.status!=Status.PASSED)return;part.status=Status.EXITED;persist();removePlayerBar(p,d.id);portals.put(p.getUniqueId(),tick+40);
        fade.teleport(p,location(d.home),d.fadeExit,ok->{if(ok)tell(p,"Kamu berhasil keluar dari dungeon.");else{part.status=Status.PASSED;persist();tell(p,"Teleport keluar ditolak. Coba portal lagi atau panggil admin.");}});
    }
    private void openExit(Run r,Definition d){
        r.broken.add("exit-open");persist();for(Block block:r.blocks)if(d.exit.contains(block.location())&&Bukkit.createBlockData(block.data()).getMaterial().isAir())block.location().getBlock().setType(Material.END_GATEWAY,false);
        for(Player p:players(r)){title(p,"PORTAL KELUAR TERBUKA","Segera tinggalkan tempat ini!");p.playSound(p.getLocation(),"minecraft:block.end_portal.spawn",.75f,1);}
    }
    private void tick(){
        if(closed)return;tick++;
        try{camera.tick();}catch(DungeonCamera.CameraFailure error){fault(error);for(Run r:state.runs.values())if(r.active()&&r.participants.containsKey(error.player))stop(r.dungeon,"Kamera ditolak; sesi dipulihkan.");}
        int jobs=Math.min(128,worldWork.size());for(int i=0;i<jobs&&!worldWork.isEmpty();i++){WorldJob job=worldWork.remove();try{job.work.run();}catch(RuntimeException error){if(tick%100==0)fault(error);Run r=state.runs.get(job.dungeon);if(r!=null&&r.restoring)worldWork.add(job);else stop(job.dungeon,"Pemulihan diperlukan setelah perubahan blok gagal.");}}
        for(Definition d:List.copyOf(state.definitions.values())){
            Run r=state.runs.get(d.id);if(r==null||!r.active())continue;
            try{
                if(r.phase==Phase.CAMERA && r.participants.values().stream().filter(p->p.status==Status.ACTIVE).allMatch(p->p.cameraDone)){
                    r.phase=Phase.GLASS;r.deadline=System.currentTimeMillis()+d.glassSeconds*1000L;persist();broadcast(r,"Permainan kaca dimulai! Waktu "+d.glassSeconds+" detik.");}
                if(decideAt.containsKey(d.id)&&tick>=decideAt.get(d.id))decideGlass(r,d);
                if(spawnQueues.containsKey(d.id)){ArrayDeque<Mob> queue=spawnQueues.get(d.id);for(int i=0;i<8&&!queue.isEmpty();i++){Mob spec=queue.remove();LivingEntity entity=mobs.spawn(spec,r.token);r.mobs.put(entity.getUniqueId(),spec.type());pin(d.id,entity.getLocation().getChunk());}if(queue.isEmpty()){spawnQueues.remove(d.id);persist();}else if(tick%20==0)persist();}
                if((r.phase==Phase.MINION||r.phase==Phase.BOSS)&&!spawnQueues.containsKey(d.id)&&r.killed>=r.total)combatComplete(r,d);
                if(tick%20==0){
                    if(r.phase==Phase.GLASS){bar(r,d,"Jembatan kaca",d.glassSeconds);if(System.currentTimeMillis()>=r.deadline){for(Player p:players(r))if(r.participants.get(p.getUniqueId()).status==Status.ACTIVE)failed(p,"Waktu kaca habis. Kamu gagal.",true);}
                        if(r.participants.values().stream().noneMatch(p->p.status==Status.ACTIVE)){r.phase=Phase.GLASS_DONE;persist();removeBar(d.id);broadcast(r,"Permainan kaca selesai. Peserta yang lolos dapat melanjutkan.");}}
                    else if(r.phase==Phase.ESCAPE){bar(r,d,"Dungeon runtuh • segera keluar",d.escapeSeconds);long left=r.deadline-System.currentTimeMillis();if(left<=(d.escapeSeconds-d.portalAfter)*1000L&&!r.broken.contains("exit-open"))openExit(r,d);
                        if(left<=0){for(Player p:players(r))if(p.getWorld().getUID().equals(d.world))failed(p,"Waktu keluar habis.",true);r.phase=Phase.COMPLETE;r.restoring=true;persist();removeBar(d.id);queueRestore(r);}}
                    else if(r.phase==Phase.MINION||r.phase==Phase.BOSS)combatBar(r,d);
                    for(Award a:List.copyOf(r.awards.values()))if(a.state==ReceiptState.RESERVED&&!a.stage.endsWith("request")&&(Bukkit.getPlayer(a.player)==null||Optional.ofNullable(r.participants.get(a.player)).map(p->p.status==Status.DEAD||p.status==Status.EXITED).orElse(true))&&System.currentTimeMillis()-a.chosenAt>=30_000){r.awards.remove(a.stage);choose(r,d,a.stage,null);}
                    if(r.phase==Phase.REWARD&&!r.awards.containsKey("boss"))choose(r,d,"boss",null);
                }
                if(tick%40==0&&(r.phase==Phase.MINION||r.phase==Phase.BOSS)&&!spawnQueues.containsKey(d.id))for(UUID id:r.mobs.keySet()){
                    Entity entity=Bukkit.getEntity(id);if(entity==null||(!entity.isValid()&&!entity.isDead()))throw new IllegalStateException("Mob sesi hilang tanpa kematian yang tercatat: "+id+". Sesi dihentikan; reset dan mulai ulang.");else if(!entity.isDead())pin(d.id,entity.getLocation().getChunk());}
                if(r.active()&&r.phase!=Phase.WAITING&&!r.participants.isEmpty()&&r.participants.values().stream().noneMatch(p->p.status==Status.ACTIVE||p.status==Status.PASSED)){
                    if(r.phase==Phase.ESCAPE&&r.participants.values().stream().anyMatch(p->p.status==Status.EXITED)){r.phase=Phase.COMPLETE;r.restoring=true;persist();removeBar(d.id);queueRestore(r);}
                    else stop(d.id,"Semua peserta gagal. Arena dipulihkan untuk sesi berikutnya.");continue;}
                for(Player p:players(r)){Participant part=r.participants.get(p.getUniqueId());if(!camera.busy(p.getUniqueId())&&!bypass(p)){if(p.isFlying())p.setFlying(false);if(p.isGliding())p.setGliding(false);}if(part.falling&&!p.isDead()){p.setVelocity(new Vector(0,Math.min(-.5,p.getVelocity().getY()),0));if(p.getY()<d.arena.y1()-5)failed(p,"Kamu jatuh dari pijakan kaca.",true);}}
            }catch(RuntimeException error){fault(error);try{stop(d.id,"Dungeon dihentikan karena error. Hubungi admin.");}catch(RuntimeException cleanup){plugin.getLogger().severe(cleanup.toString());}}
        }
    }
    private void pin(String id,Chunk chunk){if(tickets.computeIfAbsent(id,k->new HashSet<>()).add(chunk))chunk.addPluginChunkTicket(plugin);}
    private void releaseTickets(String id){Set<Chunk> set=tickets.remove(id);if(set!=null)for(Chunk chunk:set)chunk.removePluginChunkTicket(plugin);}
    private void decideGlass(Run r,Definition d){
        Set<UUID> set=finishes.remove(d.id);decideAt.remove(d.id);if(set==null||r.awards.containsKey("glass"))return;
        List<UUID> valid=set.stream().filter(id->{Participant p=r.participants.get(id);Player online=Bukkit.getPlayer(id);return p!=null&&p.status==Status.PASSED&&online!=null&&!online.isDead();}).toList();
        if(!valid.isEmpty())choose(r,d,"glass",valid.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(valid.size())));
    }
    private void bar(Run r,Definition d,String name,int seconds){BossBar b=bars.computeIfAbsent(d.id,id->Bukkit.createBossBar(name,BarColor.RED,BarStyle.SOLID));long remaining=Math.max(0,(r.deadline-System.currentTimeMillis()+999)/1000);b.setTitle(name+" • "+remaining+" detik");b.setProgress(Math.max(0,Math.min(1,remaining/(double)seconds)));syncBar(b,r);}
    private void combatBar(Run r,Definition d){BossBar b=bars.computeIfAbsent(d.id,id->Bukkit.createBossBar("Dungeon",BarColor.PURPLE,BarStyle.SOLID));b.setTitle((r.phase==Phase.BOSS?"Bos":"Minion")+" • "+r.killed+"/"+r.total);b.setProgress(r.total==0?0:Math.max(0,Math.min(1,r.killed/(double)r.total)));syncBar(b,r);}
    private void syncBar(BossBar b,Run r){Set<Player> visible=new HashSet<>(players(r));for(Player p:List.copyOf(b.getPlayers()))if(!visible.contains(p))b.removePlayer(p);for(Player p:visible)b.addPlayer(p);}
    private void removeBar(String id){BossBar b=bars.remove(id);if(b!=null)b.removeAll();}
    private void removePlayerBar(Player p,String id){BossBar b=bars.get(id);if(b!=null)b.removePlayer(p);}
    private List<Player> players(Run r){return r.participants.entrySet().stream().filter(e->e.getValue().status!=Status.DEAD&&e.getValue().status!=Status.EXITED).map(e->Bukkit.getPlayer(e.getKey())).filter(p->p!=null&&!p.isDead()).toList();}
    private List<Player> playersAll(Run r){return r.participants.keySet().stream().map(Bukkit::getPlayer).filter(Objects::nonNull).toList();}
    public void status(CommandSender sender,String id){Definition d=get(id);Run r=state.runs.get(id);tell(sender,d.id+" • "+(d.enabled?"ON":"OFF")+" • "+(r==null?"belum ada sesi":r.phase+" • peserta "+r.participants.size()+" • mob "+r.killed+"/"+r.total)+(r!=null&&r.restoring?" • memulihkan blok":""));}
    private void recover(Player p){
        try{for(Run r:state.runs.values()){Participant part=r.participants.get(p.getUniqueId());if(part!=null&&part.mode!=null)camera.restore(p,part);}
            NpcData.Point home=state.returns.get(p.getUniqueId());if(home!=null&&!p.isDead()&&teleport(p,location(home))){state.returns.remove(p.getUniqueId());persist();tell(p,"Sesi sebelumnya dihentikan; kamu dipulangkan dengan aman.");}}
        catch(RuntimeException error){fault(error);tell(p,"Pemulihan dungeon tertunda; minta admin memeriksa tujuan pulang.");}
    }
    private void title(Player p,String title,String sub){p.showTitle(Title.title(Component.text(title,NamedTextColor.RED),Component.text(sub,NamedTextColor.YELLOW),Title.Times.times(Duration.ofMillis(200),Duration.ofSeconds(3),Duration.ofMillis(500))));}
    private void broadcast(Run r,String text){for(Player p:playersAll(r))tell(p,text);}
    public static void tell(CommandSender s,String text){s.sendMessage(Component.text("[Vitae Dungeon] ",NamedTextColor.GOLD).append(Component.text(text,NamedTextColor.AQUA)));}
    private void tellAdmins(String text){plugin.getLogger().warning(text);for(Player p:Bukkit.getOnlinePlayers())if(p.hasPermission("vitae.admin.dungeon"))tell(p,text);}
    private void fault(Throwable e){plugin.getLogger().log(java.util.logging.Level.SEVERE,"Dungeon: "+e.getMessage(),e);}
    @EventHandler public void action(StoryNpcActionEvent e){
        Definition selected=null;Phase before=null;
        try{
            if(e.action().startsWith("ACTION_")){standalone(e);return;}
            Definition d=state.definitions.values().stream().filter(v->v.enabled&&List.of(v.glassNpc,v.minionNpc,v.bossNpc).contains(e.npc())).findFirst().orElseThrow(()->new IllegalStateException("NPC belum dihubungkan ke dungeon aktif."));
            selected=d;Run existing=state.runs.get(d.id);before=existing==null?null:existing.phase;
            switch(e.action()){
                case "DUNGEON_GLASS"->{if(!e.npc().equals(d.glassNpc))throw new IllegalStateException("NPC ini bukan pemberi kaca.");glass(e.player(),d);}
                case "DUNGEON_MINION"->{if(!e.npc().equals(d.minionNpc))throw new IllegalStateException("NPC ini bukan pemberi minion.");accept(e.player(),d,false);}
                case "DUNGEON_BOSS"->{if(!e.npc().equals(d.bossNpc))throw new IllegalStateException("NPC ini bukan pemberi bos.");accept(e.player(),d,true);}
                case "DUNGEON_REWARD"->claim(e.player(),d,e.npc());
                default->throw new IllegalArgumentException("Aksi dungeon tidak dikenal.");
            }
        }catch(RuntimeException error){tell(e.player(),Objects.requireNonNullElse(error.getMessage(),"Aksi tidak dapat dijalankan."));if(!(error instanceof IllegalArgumentException))plugin.getLogger().warning(error.toString());
            if(selected!=null){Run r=state.runs.get(selected.id);if(r!=null&&r.active()&&r.phase!=before)stop(selected.id,"Adegan gagal dimulai; peserta dan arena dipulihkan.");}}
    }
    @EventHandler public void dialog(StoryNpcDialogEvent e){
        Definition d=state.definitions.values().stream().filter(v->v.enabled&&List.of(v.glassNpc,v.minionNpc,v.bossNpc).contains(e.npc())).findFirst().orElse(null);
        if(d==null){if(state.actions.values().stream().anyMatch(a->a.enabled&&a.npc.equals(e.npc())))e.managed(true);return;}e.managed(true);Run r=state.runs.get(d.id);Participant part=r==null?null:r.participants.get(e.player().getUniqueId());
        String refusal=null;
        if(r==null||!r.active())refusal="Dungeon belum dibuka. Tunggu pengelola memulai sesi.";
        else if(part==null||part.status==Status.DEAD||part.status==Status.EXITED)refusal="Kamu bukan peserta aktif atau telah gagal pada sesi ini.";
        else {
            for(Award a:r.awards.values())if(a.npc.equals(e.npc())&&a.player.equals(e.player().getUniqueId())&&a.state==ReceiptState.RESERVED&&!a.stage.endsWith("request")){
                String text=a.page.isEmpty()?"Selamat! Tantanganmu selesai. Ambillah hadiahmu.":npc().get(a.npc).nodes().get(a.page).text();
                e.node(new NpcData.Node(text,List.of(new NpcData.Choice("Ambil hadiah","DUNGEON_REWARD"))));return;
            }
            if(e.npc().equals(d.glassNpc)&&r.phase!=Phase.WAITING)refusal="Permainan kaca sudah dimulai. Lanjutkan perjalananmu.";
            else if(e.npc().equals(d.minionNpc)&&r.phase!=Phase.GLASS_DONE)refusal=r.phase==Phase.MINION||r.phase==Phase.MINION_READY?"Musnahkan semua minion bersama rekanmu.":"Selesaikan permainan kaca terlebih dahulu.";
            else if(e.npc().equals(d.bossNpc)&&r.phase!=Phase.MINION_DONE)refusal=r.phase==Phase.BOSS||r.phase==Phase.BOSS_READY?"Bos menunggu. Selesaikan pertarungan bersama rekanmu.":"Selesaikan permainan kaca dan seluruh minion terlebih dahulu.";
        }
        if(refusal!=null)e.node(new NpcData.Node(refusal,List.of()));
    }
    public void actionEdit(Action action)throws IOException{NpcData.id(action.id);NpcData.id(action.npc);npc().get(action.npc);if(action.mob!=null)mobs.validate(action.mob);if(action.point!=null)safe(action.point);State copy=draft();copy.actions.put(action.id,action);copy.validate();store.save(copy);state=copy;}
    private void standalone(StoryNpcActionEvent e){Action a=state.actions.get(e.action().substring(7));if(a==null||!a.enabled||!a.npc.equals(e.npc()))throw new IllegalStateException("Aksi NPC tidak aktif atau ID tidak cocok.");
        if(tick-actionClicks.getOrDefault(e.player().getUniqueId(),-100L)<40)return;actionClicks.put(e.player().getUniqueId(),tick);
        if(a.kind.equals("teleport")){fade.teleport(e.player(),location(a.point),a.fade,ok->{if(!ok)tell(e.player(),"Teleport ditolak.");});}
        else {for(int i=0;i<a.mob.count();i++)mobs.spawn(a.mob,null);tell(e.player(),"Mob telah dipanggil.");}
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void move(PlayerMoveEvent e){
        if(e instanceof PlayerTeleportEvent||e.getTo()==null||internal.contains(e.getPlayer().getUniqueId()))return;Player p=e.getPlayer();if(bypass(p))return;
        try{
            if(!fade.busy(p)&&portals.getOrDefault(p.getUniqueId(),0L)<=tick)for(Definition d:state.definitions.values())if(d.enabled&&d.entrance.crosses(e.getFrom(),e.getTo())){portals.put(p.getUniqueId(),tick+40);e.setTo(e.getFrom());Bukkit.getScheduler().runTask(plugin,()->{try{join(p,d.id);}catch(RuntimeException error){tell(p,error.getMessage());}});return;}
            Definition d=definition(p.getWorld());if(d==null)return;Run r=state.runs.get(d.id);if(r==null||!r.active())return;Participant part=r.participants.get(p.getUniqueId());if(part==null||part.status==Status.DEAD||part.status==Status.EXITED)return;
            if(camera.busy(p.getUniqueId()))return;
            if(part.falling){Location to=e.getTo().clone();to.setX(e.getFrom().getX());to.setZ(e.getFrom().getZ());to.setY(Math.min(to.getY(),e.getFrom().getY()));e.setTo(to);return;}
            if(r.phase==Phase.GLASS&&part.status==Status.ACTIVE){
                if(System.currentTimeMillis()>=r.deadline){e.setTo(e.getFrom());failed(p,"Waktu kaca habis. Kamu gagal.",true);return;}
                for(var tile:d.bad.entrySet()){
                    Box b=tile.getValue();Box surface=new Box(b.world(),b.x1(),b.y1()+1,b.z1(),b.x2(),b.y2()+1,b.z2());
                    if(surface.crosses(e.getFrom(),e.getTo()) && e.getTo().getY()<=b.y1()+1.15 && e.getTo().getY()>=b.y1()-.1){
                        e.setTo(breakTile(p,r,d,tile.getKey(),b));return;}
                }
                if(d.finish.crosses(e.getFrom(),e.getTo())){part.status=Status.PASSED;persist();finishes.computeIfAbsent(d.id,id->new HashSet<>()).add(p.getUniqueId());decideAt.putIfAbsent(d.id,tick+1);e.setTo(e.getFrom());Bukkit.getScheduler().runTask(plugin,()->{if(r.active()&&part.status==Status.PASSED&&!p.isDead())teleport(p,location(d.next));});return;}
            }
            if(part.status==Status.PASSED){if(r.phase==Phase.MINION_READY&&d.minionTrigger.crosses(e.getFrom(),e.getTo()))spawn(r,d,false);else if(r.phase==Phase.BOSS_READY&&d.bossTrigger.crosses(e.getFrom(),e.getTo()))spawn(r,d,true);
                else if(r.phase==Phase.ESCAPE&&r.broken.contains("exit-open")&&d.exit.crosses(e.getFrom(),e.getTo())&&portals.getOrDefault(p.getUniqueId(),0L)<=tick){portals.put(p.getUniqueId(),tick+40);e.setTo(e.getFrom());Bukkit.getScheduler().runTask(plugin,()->escape(p,d,r));}}
        }catch(RuntimeException error){e.setTo(e.getFrom());warn(p,error.getMessage());}
    }
    private void warn(Player p,String text){if(tick-warns.getOrDefault(p.getUniqueId(),-100L)>40){tell(p,Objects.requireNonNullElse(text,"Aksi ditolak."));warns.put(p.getUniqueId(),tick);}}
    @EventHandler(priority=EventPriority.HIGHEST)public void teleportEvent(PlayerTeleportEvent e){
        Player p=e.getPlayer();if(bypass(p)||internal.contains(p.getUniqueId()))return;
        if(e.getCause()==PlayerTeleportEvent.TeleportCause.END_GATEWAY){for(Definition d:state.definitions.values())if(d.enabled&&(d.entrance.contains(e.getFrom())||d.exit.contains(e.getFrom()))){e.setCancelled(true);
            if(!fade.busy(p)&&portals.getOrDefault(p.getUniqueId(),0L)<=tick){Run r=state.runs.get(d.id);
                if(d.entrance.contains(e.getFrom())){portals.put(p.getUniqueId(),tick+40);Bukkit.getScheduler().runTask(plugin,()->{try{join(p,d.id);}catch(RuntimeException error){tell(p,error.getMessage());}});}
                else if(r!=null&&r.phase==Phase.ESCAPE&&r.broken.contains("exit-open")){portals.put(p.getUniqueId(),tick+40);Bukkit.getScheduler().runTask(plugin,()->escape(p,d,r));}}
            return;}}
        Definition d=definition(p.getWorld());if(d==null)return;Run r=state.runs.get(d.id);if(r==null||!r.active())return;
        Participant part=r.participants.get(p.getUniqueId());
        if(e.getCause()==PlayerTeleportEvent.TeleportCause.ENDER_PEARL||e.getCause()==PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT||camera.busy(p.getUniqueId())||(part!=null&&part.falling)){e.setCancelled(true);return;}
        // Successful external world changes are handled after the teleport completes.
    }
    @EventHandler public void worldChanged(PlayerChangedWorldEvent e){Player p=e.getPlayer();if(bypass(p)||internal.contains(p.getUniqueId())||fade.busy(p))return;Definition d=definition(e.getFrom());if(d==null)return;Run r=state.runs.get(d.id);if(r==null||!r.active())return;Participant part=r.participants.get(p.getUniqueId());if(part!=null&&part.status!=Status.DEAD&&part.status!=Status.EXITED){part.status=Status.DEAD;persist();tell(p,"Kamu meninggalkan dungeon dan dianggap gagal pada sesi ini.");}}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)public void died(PlayerDeathEvent e){hardDeaths.remove(e.getEntity().getUniqueId());failed(e.getEntity(),"Kamu gugur dan tidak dapat masuk kembali pada sesi ini.",false);}
    @EventHandler(priority=EventPriority.HIGHEST)public void resurrect(EntityResurrectEvent e){if(hardDeaths.contains(e.getEntity().getUniqueId()))e.setCancelled(true);}
    @EventHandler public void quit(PlayerQuitEvent e){Player p=e.getPlayer();Definition d=definition(p.getWorld());if(d!=null){Run r=state.runs.get(d.id);if(r!=null&&r.phase!=Phase.WAITING){failed(p,"",false);state.returns.put(p.getUniqueId(),d.home);persist();}}portals.remove(p.getUniqueId());warns.remove(p.getUniqueId());}
    @EventHandler public void joined(PlayerJoinEvent e){Bukkit.getScheduler().runTask(plugin,()->recover(e.getPlayer()));}
    @EventHandler public void respawn(PlayerRespawnEvent e){hardDeaths.remove(e.getPlayer().getUniqueId());Bukkit.getScheduler().runTask(plugin,()->recover(e.getPlayer()));}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)public void mobDeath(EntityDeathEvent e){
        UUID id=e.getEntity().getUniqueId();for(Run r:state.runs.values())if(r.active()&&r.mobs.containsKey(id)){
            Bukkit.getScheduler().runTask(plugin,()->{if(closed||e.isCancelled()||!r.active()||r.mobs.remove(id)==null)return;r.killed++;persist();broadcast(r,"Progres bersama: "+r.killed+"/"+r.total);if(r.killed>=r.total&&!spawnQueues.containsKey(r.dungeon))combatComplete(r,get(r.dungeon));});return;
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void mobSpawn(CreatureSpawnEvent e){if(definition(e.getLocation().getWorld())!=null&&e.getSpawnReason()!=CreatureSpawnEvent.SpawnReason.CUSTOM)e.setCancelled(true);}
    @EventHandler public void worldLoad(WorldLoadEvent e){if(definition(e.getWorld())!=null)e.getWorld().setGameRule(GameRule.DO_MOB_SPAWNING,false);}
    @EventHandler public void chunkLoad(ChunkLoadEvent e){if(definition(e.getWorld())==null)return;for(Entity entity:e.getChunk().getEntities()){String token=entity.getPersistentDataContainer().get(mobs.sessionKey,PersistentDataType.STRING);if(token!=null&&state.runs.values().stream().noneMatch(r->r.active()&&r.token.toString().equals(token)))mobs.remove(entity.getUniqueId());}}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void blockBreak(BlockBreakEvent e){if(carryBlocked(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void blockPlace(BlockPlaceEvent e){if(carryBlocked(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void interact(PlayerInteractEvent e){if(!carryBlocked(e.getPlayer()))return;ItemStack i=e.getItem();if(i!=null&&(Set.of(Material.ENDER_PEARL,Material.CHORUS_FRUIT,Material.WATER_BUCKET,Material.LAVA_BUCKET,Material.BUCKET).contains(i.getType())||i.getType().name().endsWith("BOAT")||i.getType().name().endsWith("RAFT")||i.getType().name().endsWith("MINECART"))){e.setCancelled(true);warn(e.getPlayer(),"Item ini tidak boleh digunakan dalam dungeon.");}}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void consume(PlayerItemConsumeEvent e){if(carryBlocked(e.getPlayer())&&e.getItem().getType()==Material.CHORUS_FRUIT)e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void launch(ProjectileLaunchEvent e){if(e.getEntity() instanceof EnderPearl&&e.getEntity().getShooter() instanceof Player p&&carryBlocked(p))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void glide(EntityToggleGlideEvent e){if(e.getEntity() instanceof Player p&&carryBlocked(p)&&e.isGliding())e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void fly(PlayerToggleFlightEvent e){if(carryBlocked(e.getPlayer())&&!camera.busy(e.getPlayer().getUniqueId())&&e.isFlying())e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void mount(EntityMountEvent e){if(e.getEntity() instanceof Player p&&carryBlocked(p))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void explode(EntityExplodeEvent e){if(definition(e.getLocation().getWorld())!=null)e.blockList().clear();}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void explode(BlockExplodeEvent e){if(definition(e.getBlock().getWorld())!=null)e.blockList().clear();}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)public void entityBlock(EntityChangeBlockEvent e){if(definition(e.getBlock().getWorld())!=null)e.setCancelled(true);}
    public void rewardMenu(Player p,String id,String stage){get(id);if(!Set.of("glass","minion","boss").contains(stage))throw new IllegalArgumentException("Tahap glass|minion|boss.");Run r=state.runs.get(id);if(r!=null&&r.active())throw new IllegalStateException("Reset sesi sebelum mengganti hadiah.");p.openInventory(new RewardMenu(p.getUniqueId(),id,stage,StoryStore.JSON.toJson(get(id))).inventory);}
    private static final class RewardMenu implements VitaeMenu{final UUID admin;final String id,stage,before;final Inventory inventory;
        RewardMenu(UUID admin,String id,String stage,String before){this.admin=admin;this.id=id;this.stage=stage;this.before=before;inventory=Bukkit.createInventory(this,27,Component.text("Hadiah "+stage+" • tutup untuk simpan"));}@Override public Inventory getInventory(){return inventory;}}
    @EventHandler public void rewardClose(InventoryCloseEvent e){if(!(e.getInventory().getHolder() instanceof RewardMenu menu)||!(e.getPlayer() instanceof Player p))return;var items=new ArrayList<String>();
        for(ItemStack item:e.getInventory().getContents())if(item!=null&&!item.getType().isAir()){ItemStack copy=item.clone();items.add(Base64.getEncoder().encodeToString(copy.serializeAsBytes()));for(ItemStack overflow:p.getInventory().addItem(copy).values())p.getWorld().dropItem(p.getLocation(),overflow).setOwner(p.getUniqueId());}e.getInventory().clear();
        if(closed)return;try{if(!p.getUniqueId().equals(menu.admin)||!p.hasPermission("vitae.admin.dungeon")||!menu.before.equals(StoryStore.JSON.toJson(get(menu.id))))throw new IllegalStateException("Setup/izin berubah; hadiah tidak disimpan.");edit(menu.id,d->d.rewards.put(menu.stage,items));tell(p,"Hadiah "+menu.stage+" tersimpan. Item admin dikembalikan; dungeon OFF sampai setup diaktifkan.");}catch(Exception error){tell(p,error.getMessage());}}
    @Override public void close(){
        closed=true;if(task!=null)task.cancel();for(String id:List.copyOf(bars.keySet()))removeBar(id);
        for(Run r:state.runs.values())if(r.active()){r.phase=Phase.ABORTED;r.restoring=true;for(var e:r.participants.entrySet())if(e.getValue().status!=Status.DEAD&&e.getValue().status!=Status.EXITED)state.returns.put(e.getKey(),get(r.dungeon).home);}
        camera.close();try{save();}catch(IOException e){fault(e);}for(Run r:state.runs.values())if(r.restoring){for(UUID id:r.mobs.keySet())mobs.remove(id);}
        for(String id:List.copyOf(tickets.keySet()))releaseTickets(id);mobs.close();
        // Remaining block snapshots are deliberately retained for next startup recovery.
        HandlerList.unregisterAll(this);
    }
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/story/FadeService.java'
        OriginalHashes = @()
        Hash = '65793b4123d40654758568a7ee84d8acebe653c68c72e41866676c5467d8434a'
        Required = $false
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.story;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

/** Fullscreen glyph uses its own font. No global Minecraft shader is replaced. */
public final class FadeService implements Listener, AutoCloseable {
    public static final class Settings { public int version=1;public boolean enabled;public int inTicks=15,holdTicks=5,outTicks=15; }
    private static final class Playback {
        final UUID token=UUID.randomUUID(); final Player player; final Location destination;final Consumer<Boolean> done;
        final long middle,end; boolean moved;
        Playback(Player player,Location destination,Consumer<Boolean> done,long middle,long end) {this.player=player;this.destination=destination;this.done=done;this.middle=middle;this.end=end;}
    }
    private final JavaPlugin plugin;
    private final StoryStore<Settings> store;
    private Settings settings;
    private final Map<UUID,Playback> active=new HashMap<>();
    private BukkitTask task;
    private long tick;
    private boolean closed;
    public FadeService(JavaPlugin plugin)throws IOException {
        this.plugin=plugin;store=new StoryStore<>(plugin.getDataFolder().toPath().resolve("fade.json"),Settings.class);settings=store.load(new Settings());
        if(settings.version!=1 || settings.inTicks<1 || settings.inTicks>100 || settings.holdTicks<1 || settings.holdTicks>100 || settings.outTicks<1 || settings.outTicks>100)throw new IOException("fade.json tidak valid; file dipertahankan.");
    }
    public void start(){Bukkit.getPluginManager().registerEvents(this,plugin);task=Bukkit.getScheduler().runTaskTimer(plugin,this::tick,1,1);}
    public boolean enabled(){return settings.enabled;}
    public boolean busy(Player p){return active.containsKey(p.getUniqueId());}
    public void teleport(Player p,Location destination,boolean requested,Consumer<Boolean> done){
        clear(p,true);if(closed || !p.isOnline() || p.isDead()){finish(done,false);return;}
        // A missing/declined pack must never delay gameplay or show the missing-glyph box.
        if(!requested || !settings.enabled || !p.hasResourcePack()){finish(done,p.teleport(destination));return;}
        play(p,destination,done);
    }
    private void play(Player p,Location destination,Consumer<Boolean> done){
        Playback b=new Playback(p,destination,done,tick+settings.inTicks,tick+settings.inTicks+settings.holdTicks+settings.outTicks);
        active.put(p.getUniqueId(),b);
        Component glyph=Component.text("\uE001",NamedTextColor.WHITE).font(Key.key("vitae_cinematic:screen"));
        p.showTitle(Title.title(glyph,Component.empty(),Title.Times.times(Duration.ofMillis(settings.inTicks*50L),Duration.ofMillis(settings.holdTicks*50L),Duration.ofMillis(settings.outTicks*50L))));
    }
    private void tick(){
        tick++;for(Playback b:List.copyOf(active.values())) {
            if(!b.player.isOnline() || b.player.isDead()){clear(b.player,false);continue;}
            if(!b.moved && tick>=b.middle){b.moved=true;try{boolean success=b.destination==null || b.player.teleport(b.destination);finish(b.done,success);if(!success){clear(b.player,false);continue;}}catch(RuntimeException error){plugin.getLogger().warning("Fade dihentikan: "+error.getMessage());finish(b.done,false);clear(b.player,false);continue;}}
            if(tick>=b.end+2)clear(b.player,false);
        }
    }
    private void clear(Player p,boolean completeTeleport){
        Playback b=active.remove(p.getUniqueId());if(b==null)return;clearTitle(p);
        if(!b.moved){b.moved=true;boolean success=false;try{success=completeTeleport && p.isOnline() && !p.isDead() && (b.destination==null || p.teleport(b.destination));}finally{finish(b.done,success);}}
    }
    private void finish(Consumer<Boolean> done,boolean success){try{done.accept(success);}catch(RuntimeException error){plugin.getLogger().warning("Callback fade: "+error.getMessage());}}
    private void clearTitle(Player p){try{p.clearTitle();}catch(RuntimeException error){plugin.getLogger().warning("Pembersihan title "+p.getName()+": "+error.getMessage());}}
    public void clearAll(){for(Player p:Bukkit.getOnlinePlayers()){try{clear(p,true);}catch(RuntimeException e){plugin.getLogger().warning("Pembersihan fade: "+e.getMessage());}clearTitle(p);}}
    public boolean command(CommandSender sender,String[] args){
        if(!sender.hasPermission("vitae.admin.fade")){sender.sendMessage(Component.text("Perlu izin vitae.admin.fade.",NamedTextColor.RED));return true;}
        try{
            if(args.length==1 && args[0].equalsIgnoreCase("clear")){clearAll();tell(sender,"Fade seluruh pemain dibersihkan. Teleport yang menunggu tetap diselesaikan.");}
            else if(args.length==1 && Set.of("on","off").contains(args[0].toLowerCase(Locale.ROOT))){Settings next=store.copy(settings);next.enabled=args[0].equalsIgnoreCase("on");store.save(next);settings=next;if(!settings.enabled)clearAll();tell(sender,"Fade "+(settings.enabled?"aktif":"mati")+".");}
            else if(args.length==4 && args[0].equalsIgnoreCase("times")){Settings next=store.copy(settings);next.inTicks=duration(args[1]);next.holdTicks=duration(args[2]);next.outTicks=duration(args[3]);store.save(next);settings=next;tell(sender,"Waktu fade tersimpan.");}
            else if(args.length>=1 && args[0].equalsIgnoreCase("preview")){Player p=args.length==2?Bukkit.getPlayerExact(args[1]):sender instanceof Player pl?pl:null;if(p==null)throw new IllegalArgumentException("/vitae fade preview [pemain-online]");if(!settings.enabled)throw new IllegalStateException("Aktifkan /vitae fade on terlebih dahulu.");if(!p.hasResourcePack())throw new IllegalStateException("Pemain belum memuat resource pack.");clear(p,false);play(p,null,ok->{});tell(sender,"Preview fade dimulai untuk "+p.getName()+".");}
            else if(args.length==1 && args[0].equalsIgnoreCase("pack")){exportPack();tell(sender,"Asset diekspor ke plugins/vitaemanager/fade-resourcepack/ dan dipasang ke contents/vitae_cinematic_fade/resourcepack/ jika ItemsAdder tersedia. Jalankan /iazip lalu muat ulang pack pemain.");}
            else {sender.sendMessage(Component.empty());tell(sender,"Fade: "+(settings.enabled?"ON":"OFF")+" • /vitae fade on|off|clear|pack|preview [pemain]");tell(sender,"/vitae fade times <gelap-detik> <tahan-detik> <terang-detik> (0.05–5 masing-masing)");sender.sendMessage(Component.empty());}
        }catch(Exception e){sender.sendMessage(Component.text("[Vitae Fade] "+Objects.requireNonNullElse(e.getMessage(),"Periksa log."),NamedTextColor.RED));}
        return true;
    }
    private int duration(String value){double s=Double.parseDouble(value);if(!Double.isFinite(s)||s<.05||s>5)throw new IllegalArgumentException("Durasi 0.05–5 detik.");return (int)Math.round(s*20);}
    public void exportPack()throws IOException{
        for(String name:List.of("assets/vitae_cinematic/font/screen.json","assets/vitae_cinematic/textures/font/black_screen.png")){
            Path to=plugin.getDataFolder().toPath().resolve("fade-resourcepack").resolve(name);Files.createDirectories(to.getParent());
            try(var input=plugin.getResource("fade-pack/"+name)){if(input==null)throw new IOException("Asset fade tidak ada dalam JAR.");Files.copy(input,to,StandardCopyOption.REPLACE_EXISTING);}
            org.bukkit.plugin.Plugin ia=Bukkit.getPluginManager().getPlugin("ItemsAdder");
            if(ia!=null){Path content=ia.getDataFolder().toPath().resolve("contents/vitae_cinematic_fade");Path pack=content.resolve("resourcepack").resolve(name);Files.createDirectories(pack.getParent());Files.copy(to,pack,StandardCopyOption.REPLACE_EXISTING);
                Path config=content.resolve("configs/fade.yml");Files.createDirectories(config.getParent());if(!Files.exists(config))Files.writeString(config,"info:\n  namespace: vitae_cinematic\n",StandardOpenOption.CREATE_NEW);}
        }
    }
    private void tell(CommandSender s,String text){s.sendMessage(Component.text("[Vitae Fade] ",NamedTextColor.GOLD).append(Component.text(text,NamedTextColor.AQUA)));}
    @EventHandler public void quit(PlayerQuitEvent e){clear(e.getPlayer(),false);}
    @EventHandler public void death(org.bukkit.event.entity.PlayerDeathEvent e){clear(e.getEntity(),false);}
    @Override public void close(){closed=true;if(task!=null)task.cancel();clearAll();HandlerList.unregisterAll(this);}
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/story/StoryModule.java'
        OriginalHashes = @()
        Hash = '5d20d06d1f74166c2d344c69106a0038604773ecdb00dbcffd1acb846c0d74e2'
        Required = $false
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.story;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

/** Isolates startup errors to new story modules; old economy/NPC data are untouched. */
public final class StoryModule implements AutoCloseable {
    private final JavaPlugin plugin;
    private FadeService fade;
    private DungeonService dungeon;
    private DungeonCommand command;
    private String failure="";
    public StoryModule(JavaPlugin plugin){this.plugin=plugin;}
    public void start(){
        try{fade=new FadeService(plugin);fade.start();dungeon=new DungeonService(plugin,fade);dungeon.start();command=new DungeonCommand(dungeon);failure="";}
        catch(Exception|LinkageError e){close();failure=Objects.requireNonNullElse(e.getMessage(),e.getClass().getSimpleName());plugin.getLogger().log(java.util.logging.Level.SEVERE,"Modul story belum aktif; modul dan data lama tetap tersedia.",e);}
        PluginCommand cmd=plugin.getCommand("vdungeon");if(cmd!=null){cmd.setExecutor((s,c,l,a)->execute(s,"dungeon",a));cmd.setTabCompleter((s,c,l,a)->command==null?List.of("reload"):command.onTabComplete(s,c,l,a));}
    }
    public boolean carryBlocked(Player p){return dungeon!=null&&dungeon.carryBlocked(p);}
    public boolean execute(CommandSender sender,String topic,String[] args){
        if(topic.equals("fade")&&args.length==1&&args[0].equalsIgnoreCase("clear")){
            if(!sender.hasPermission("vitae.admin.fade")){sender.sendMessage(Component.text("Perlu izin vitae.admin.fade.",NamedTextColor.RED));return true;}
            if(fade!=null)fade.clearAll();else for(Player p:Bukkit.getOnlinePlayers())p.clearTitle();DungeonService.tell(sender,"Layar fade seluruh pemain dibersihkan.");return true;
        }
        if(args.length==1&&args[0].equalsIgnoreCase("reload")){
            if(!sender.hasPermission("vitae.admin.dungeon")){sender.sendMessage(Component.text("Perlu izin vitae.admin.dungeon.",NamedTextColor.RED));return true;}close();start();DungeonService.tell(sender,command==null?"Belum aktif: "+failure:"Modul story dimuat. Sesi sebelumnya dihentikan dengan pemulihan.");return true;
        }
        if(command==null){sender.sendMessage(Component.text("Story belum aktif: "+failure+". Periksa log, lalu /vitae dungeon reload.",NamedTextColor.RED));return true;}
        return topic.equals("fade")?fade.command(sender,args):command.execute(sender,args);
    }
    public List<String> complete(CommandSender s,String topic,String[] args){if(topic.equals("fade"))return args.length==1?List.of("help","on","off","clear","times","preview","pack").stream().filter(v->v.startsWith(args[0].toLowerCase(Locale.ROOT))).toList():List.of();return command==null?List.of("reload"):command.onTabComplete(s,null,"vdungeon",args);}
    @Override public void close(){try{if(dungeon!=null)dungeon.close();}catch(RuntimeException|LinkageError error){plugin.getLogger().warning("Pembersihan dungeon: "+error.getMessage());}
        finally{try{if(fade!=null)fade.close();}catch(RuntimeException|LinkageError error){plugin.getLogger().warning("Pembersihan fade: "+error.getMessage());}dungeon=null;fade=null;command=null;}}
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/story/StoryNpcActionEvent.java'
        OriginalHashes = @()
        Hash = 'd4d533cffe3a63d7060d557c2b96869e95640fd3dd947130e74091de94abd983'
        Required = $false
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.story;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** A dialog choice requests an action after its movement lock has been released. */
public final class StoryNpcActionEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String npc, action;
    public StoryNpcActionEvent(Player player, String npc, String action) { this.player = player; this.npc = npc; this.action = action; }
    public Player player() { return player; }
    public String npc() { return npc; }
    public String action() { return action; }
    public static boolean target(String target) { return java.util.Set.of("DUNGEON_GLASS","DUNGEON_MINION","DUNGEON_BOSS","DUNGEON_REWARD").contains(target) || target.matches("ACTION_[A-Za-z0-9_-]{1,41}"); }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/story/StoryNpcDialogEvent.java'
        OriginalHashes = @()
        Hash = 'b8dddb0d69550db6da4f39d659956edaeb82bc98e5f2f13f9ff0cfc45b676a64'
        Required = $false
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.story;

import com.bangzachery.vitae.vitaemanager.npc.NpcData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class StoryNpcDialogEvent extends Event {
    private static final HandlerList HANDLERS=new HandlerList();
    private final Player player;
    private final String npc;
    private NpcData.Node node;
    private boolean managed;
    public StoryNpcDialogEvent(Player player,String npc){this.player=player;this.npc=npc;}
    public Player player(){return player;}
    public String npc(){return npc;}
    public NpcData.Node node(){return node;}
    public void node(NpcData.Node node){this.node=node;}
    public boolean managed(){return managed;}
    public void managed(boolean managed){this.managed=managed;}
    @Override public HandlerList getHandlers(){return HANDLERS;}
    public static HandlerList getHandlerList(){return HANDLERS;}
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/story/StoryStore.java'
        OriginalHashes = @()
        Hash = 'b5310972679a5476129ac0dd50301c2bf05f043ec98501f4d0367c02dc5423bc'
        Required = $false
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.story;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** One atomic file per story module. Never replaces malformed existing data. */
public final class StoryStore<T> {
    public static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Path path;
    private final Class<T> type;
    public StoryStore(Path path, Class<T> type) { this.path = path; this.type = type; }
    public T load(T initial) throws IOException {
        if (!Files.exists(path)) { save(initial); return initial; }
        try {
            T value = JSON.fromJson(Files.readString(path), type);
            if (value == null) throw new IllegalArgumentException("Isi file kosong.");
            return value;
        } catch (RuntimeException error) { throw new IOException("Data " + path.getFileName() + " tidak valid; file dipertahankan.", error); }
    }
    public void save(T value) throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), ".story-", ".tmp");
        try {
            byte[] bytes = JSON.toJson(value).getBytes(StandardCharsets.UTF_8);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException error) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
    public T copy(T value) { return JSON.fromJson(JSON.toJson(value), type); }
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperAudio.java'
        OriginalHashes = @()
        Hash = '31e585a368a46eb320e28e1092253d77ca307957ad6222b14db9cb6ffc70be05'
        Required = $false
        Binary = $false
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
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperDefinition.java'
        OriginalHashes = @('7d7ddeaaf3b47094d8f6977d4c7749d63e027f0e80f87d8e76b792ccc67027a0')
        Hash = 'c8c256796a47c0ac9b3a3d4d9dccc1e710dcd617a29d20a4feac9024be69abdb'
        Required = $true
        Binary = $false
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
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperLine.java'
        OriginalHashes = @('8031e086989bb8f300bf85064fa2cd012593d08b885e5c6ef2aaeab52fd5b459')
        Hash = 'e050e7a39ad0d0a47ae6aef31a3c70ecee23fcba52c1073cf982f03800770ecb'
        Required = $true
        Binary = $false
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
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperMusic.java'
        OriginalHashes = @()
        Hash = '21e48cdaa6d1de2592b67222281095c76523da45852173cf30e0981cf33a7d74'
        Required = $false
        Binary = $false
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
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperScreen.java'
        OriginalHashes = @()
        Hash = 'bda0cb32c71ca5edecae78e80d868032c7241c81ac456c4d0503791040103c21'
        Required = $false
        Binary = $false
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
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperService.java'
        OriginalHashes = @('5b6ad82116b80fc5eef13944b8e0c3bebe4d5c65e58361fe3676eb506a61d9c9')
        Hash = 'fb086397eac289cf069f9ea9613c9cfd25898b3641affd6da0bc8e5698e19d9c'
        Required = $true
        Binary = $false
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
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperStore.java'
        OriginalHashes = @('69e08da343a7c616200d5ae26de760371b13b5c7bef78a46c695eeb23460c85e')
        Hash = '99433bb101195b1da0fd31df3cab0271ad0b76430cb820f93db8fac29cc0f428'
        Required = $true
        Binary = $false
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
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperTemplates.java'
        OriginalHashes = @()
        Hash = '3ae691ae2b2883424455ce503a5bd5125f9d1b914a6113ddcc1e6ac50ed5ab34'
        Required = $false
        Binary = $false
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
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperText.java'
        OriginalHashes = @()
        Hash = '1914097153066e3294ceed3cd985e0dbd1e7f56cdc097ae1cb572f803128dcf2'
        Required = $false
        Binary = $false
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
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperVisual.java'
        OriginalHashes = @()
        Hash = '8a97868070ae8f7a53884e5ca12c089cfc48494a41f5091daf162287b6db9d32'
        Required = $false
        Binary = $false
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
    }
    [PSCustomObject]@{
        Name = 'src/main/resources/fade-pack/assets/vitae_cinematic/font/screen.json'
        OriginalHashes = @()
        Hash = 'ac375a5646440b0e8d4849d23c305ca19037dd0c41b93a4b11f8e9ff15cdf0de'
        Required = $false
        Binary = $false
        Source = @'
{
  "providers": [
    {
      "type": "bitmap",
      "file": "vitae_cinematic:font/black_screen.png",
      "height": 8192,
      "ascent": 4096,
      "chars": [
        "\ue001"
      ]
    }
  ]
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/resources/fade-pack/assets/vitae_cinematic/textures/font/black_screen.png'
        OriginalHashes = @()
        Hash = '004c0da60be05efebf543eb7ed2a51aba399a57d19c58d74290344ce5c54a1f3'
        Required = $false
        Binary = $true
        Source = @'
iVBORw0KGgoAAAANSUhEUgAAAQAAAAEACAYAAABccqhmAAADHUlEQVR4nO3UMQEAIAzAsIF/zyBjRxMFvXpm5g2QdLcDgD0GAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEGAGEfdCIC/5NkVo8AAAAASUVORK5CYII=
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/resources/plugin.yml'
        OriginalHashes = @('6bfabee77ba01f8e430be65f7696062736e6ca6f3ac2d6ef77947ccff79b640e')
        Hash = 'b9a7c03e02b041a135d1b28464bf0165eada7532ef416d2c188999d2d7c2a171'
        Required = $true
        Binary = $false
        Source = @'
name: vitaemanager
version: '${version}'
main: com.bangzachery.vitae.vitaemanager.Vitaemanager
api-version: '1.21.4'
authors: [Bang Zachery]
description: Core roleplay Vitae - remake bertahap
softdepend: [Essentials, AuraSkills, ModelEngine, Shopkeepers, MythicMobs, ItemsAdder, Multiverse-Core]

commands:
  vdungeon:
    description: Setup dungeon cerita bersama Vitae
    usage: /vdungeon help
    aliases: [vitaedungeon]
  evo:
    description: Ritual evolusi senjata Vitae
    usage: /evo help
    aliases: [ritual]

  vitae:
    description: Perintah utama administrator Vitae
    usage: /vitae help

  adminpanel:
    description: Buka panel kontrol administrator
    usage: /adminpanel
    aliases: [panel]

  viti:
    description: Saldo dan transaksi Viti
    usage: /viti help
    aliases: [uang, money]

  me:
    description: Teks aksi roleplay di atas pemain
    usage: /me aksi

  do:
    description: Teks kondisi roleplay di atas pemain
    usage: /do kondisi

  ooc:
    description: Obrolan luar roleplay lokal
    usage: /ooc pesan

  oocg:
    description: Obrolan luar roleplay global
    usage: /oocg pesan

  carry:
    description: Angkat atau turunkan pemain di depanmu
    usage: /carry
    aliases: [gendong]
  vnpc:
    description: Setup NPC dialog dan quest Vitae
    usage: /vnpc help
    aliases: [vitaenpc]
  quest:
    description: Status atau batalkan quest Vitae
    usage: /quest status
    aliases: [vitaequest]

permissions:
  vitae.admin.dungeon:
    description: Setup dan pemulihan dungeon bersama
    default: op
  vitae.admin.fade:
    description: Setup dan bersihkan screen fade
    default: op
  vitae.dungeon.bypass:
    description: Bypass proteksi dungeon khusus pengelola; jangan diberikan pada peserta
    default: false
  vitae.admin:
    description: Akses command utama administrator
    default: op
    children:
      vitae.admin.npc: true
      vitae.admin.dungeon: true
      vitae.admin.fade: true
      vitae.admin.reload: true
      vitae.admin.maintenance: true
      vitae.admin.chat: true
      vitae.admin.chatbypass: true
      vitae.admin.pvp: true
      vitae.admin.mobspawning: true
      vitae.admin.time: true
      vitae.admin.weather: true
      vitae.admin.viti: true
      vitae.admin.profile: true
      vitae.admin.heal: true
      vitae.admin.feed: true
      vitae.admin.revive: true
      vitae.admin.gamemode: true
      vitae.admin.teleport: true
      vitae.admin.pull: true
      vitae.admin.invsee: true
      vitae.admin.totem: true
      vitae.admin.blocker: true
      vitae.admin.stuck: true
      vitae.admin.whisper: true
      vitae.admin.evolution: true

  vitae.admin.reload:
    description: Memuat ulang konfigurasi Vitae
    default: op

  vitae.admin.maintenance:
    description: Mengatur dan melewati maintenance (kompatibel dengan izin lama)
    default: op

  vitae.admin.chat:
    description: Mengatur chat server
    default: op

  vitae.admin.chatbypass:
    description: Mengirim chat saat chat server dimatikan
    default: op

  vitae.admin.pvp:
    description: Mengatur PvP per dunia
    default: op

  vitae.admin.mobspawning:
    description: Mengatur gamerule spawn mob vanilla per dunia
    default: op

  vitae.admin.time:
    description: Mengatur waktu dunia melalui panel
    default: op

  vitae.admin.weather:
    description: Mengatur cuaca dunia melalui panel
    default: op

  vitae.viti:
    description: Menggunakan saldo, transfer, dan uang fisik Viti
    default: true

  vitae.admin.viti:
    description: Mengatur saldo Viti dan memuat ulang viti.yml
    default: op

  vitae.admin.profile:
    description: Membuka daftar dan profil pemain
    default: op

  vitae.admin.heal:
    description: Memulihkan HP melalui profil
    default: op

  vitae.admin.feed:
    description: Memulihkan hunger melalui profil
    default: op

  vitae.admin.revive:
    description: Mengirim command ke plugin revive yang tersedia
    default: op

  vitae.admin.gamemode:
    description: Mengganti gamemode melalui profil
    default: op

  vitae.admin.teleport:
    description: Teleport ke pemain melalui profil
    default: op

  vitae.admin.pull:
    description: Menarik pemain melalui profil
    default: op

  vitae.admin.invsee:
    description: Melihat dan mengedit inventory pemain online
    default: op

  vitae.roleplay:
    description: Akses perintah teks roleplay
    default: true
    children:
      vitae.roleplay.me: true
      vitae.roleplay.do: true
      vitae.roleplay.ooc: true
      vitae.roleplay.oocg: true

  vitae.roleplay.me:
    default: true

  vitae.roleplay.do:
    default: true

  vitae.roleplay.ooc:
    default: true

  vitae.roleplay.oocg:
    default: true

  vitae.carry:
    description: Menggunakan carry dan gendong
    default: true

  vitae.totem:
    description: Menyerap totem menjadi charge tersimpan
    default: true

  vitae.admin.totem:
    description: Mengatur batas totem dan reset cooldown pemain online
    default: op

  vitae.admin.blocker:
    description: Toggle blokir item dan mob
    default: op

  vitae.admin.stuck:
    description: Mengatur batas stack item
    default: op

  vitae.admin.whisper:
    description: Mengatur area bisikan, penerima, dialog, suara, preview dan reset progres
    default: op

  vitae.evolution:
    description: Menggunakan altar evolusi yang ditujukan kepada pemain
    default: true

  vitae.admin.evolution:
    description: Mengatur ritual evolusi senjata
    default: op
  vitae.admin.npc:
    description: Mengatur NPC dialog dan quest
    default: op
  vitae.npc:
    description: Memulai dialog NPC Vitae
    default: true
  vitae.quest:
    description: Menerima dan membatalkan quest Vitae
    default: true
'@
    }
    [PSCustomObject]@{
        Name = 'src/test/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionAftermathDataTest.java'
        OriginalHashes = @()
        Hash = '9679bd20ca72a88222c8d8cf961cdf97f7537023f4052bb681c4f75f0b18fdad'
        Required = $false
        Binary = $false
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
    }
    [PSCustomObject]@{
        Name = 'src/test/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionAnimationTest.java'
        OriginalHashes = @()
        Hash = '6b1b47551db6388911320c4c84fb44c5775592c0b2288613bac11d56377b6089'
        Required = $false
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.evolution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionAltar.*;

class EvolutionAnimationTest {
    List<EvolutionAnimation.Vec> origins(){return List.of(new EvolutionAnimation.Vec(3,0,0),new EvolutionAnimation.Vec(-3,0,0));}
    @ParameterizedTest @EnumSource(Preset.class)
    void eachPresetStartsAtPedestalsAndFinishesInsideTheWeapon(Preset preset) {
        assertEquals(origins(),EvolutionAnimation.frame(preset,0,0,origins()).ingredients());
        var end=EvolutionAnimation.frame(preset,1,180,origins());
        for(var p:end.ingredients()) {assertEquals(end.weapon().x(),p.x(),1e-10);assertEquals(end.weapon().y(),p.y(),1e-10);assertEquals(end.weapon().z(),p.z(),1e-10);}
        assertEquals(Stage.FINISH,end.stage());
    }
    @ParameterizedTest @EnumSource(Preset.class)
    void nineSecondTimelineHasFinitePositions(Preset preset) {
        for(int ticks=0;ticks<=180;ticks+=2) {
            var frame=EvolutionAnimation.frame(preset,ticks/180.0,ticks,origins());assertTrue(Double.isFinite(frame.rotation()));
            for(var p:frame.ingredients())assertTrue(Double.isFinite(p.x())&&Double.isFinite(p.y())&&Double.isFinite(p.z()));
        }
    }
    @Test void newPresetsHaveDifferentPaths() {
        Set<List<EvolutionAnimation.Vec>> paths=new HashSet<>();
        for(Preset p:List.of(Preset.ASTRAL,Preset.FROST,Preset.PHOENIX))assertTrue(paths.add(EvolutionAnimation.frame(p,.5,90,origins()).ingredients()));
    }
    @Test void invalidTimingSoundAndLightningRangesAreRejected() {
        assertThrows(IllegalArgumentException.class,()->new Music("vitae:fire",false,1,99));
        assertThrows(IllegalArgumentException.class,()->new Music("file.wav",false,1,180));
        assertThrows(IllegalArgumentException.class,()->new Effects(true,1,3,true));
        assertThrows(IllegalArgumentException.class,()->new Effects(true,30,9,true));
        assertThrows(IllegalArgumentException.class,()->EvolutionAnimation.frame(Preset.VOID,Double.NaN,0,origins()));
    }
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/test/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionEscrowTest.java'
        OriginalHashes = @()
        Hash = '13c05bb81f2e0976bac2bfb63ab4b682c74c141750543418760ecd0241a6d1d5'
        Required = $false
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.evolution;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionStore.*;
import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionStoreTest.*;

class EvolutionEscrowTest {
    @Test void preparedCursorCommitsOnlyAfterMatchingWithdrawalReceipt() {
        UUID id=UUID.randomUUID(),receipt=UUID.randomUUID();
        Deposit d=new Deposit(id,OWNER,"api",altar().table(),Map.of(),new Transfer(receipt,ITEM,Map.of(0,ITEM)));
        State pending=initial().deposit(d);assertTrue(pending.deposits().get(OWNER).items().isEmpty());
        State committed=pending.settle(OWNER,id,receipt,true);assertEquals(Map.of(0,ITEM),committed.deposits().get(OWNER).items());
        assertNull(committed.deposits().get(OWNER).transfer());assertFalse(pending.settle(OWNER,id,receipt,false).deposits().containsKey(OWNER));
        assertThrows(IllegalArgumentException.class,()->committed.settle(OWNER,id,receipt,true));
        assertThrows(IllegalArgumentException.class,()->pending.settle(OWNER,id,UUID.randomUUID(),true));
    }
    @Test void closingBeforeWithdrawalKeepsAllPreviousDeposits() {
        UUID id=UUID.randomUUID(),receipt=UUID.randomUUID();
        Deposit d=new Deposit(id,OWNER,"api",altar().table(),Map.of(0,ITEM),new Transfer(receipt,ITEM,Map.of(0,ITEM,8,ITEM)));
        State next=initial().deposit(d).settle(OWNER,id,receipt,false);
        assertEquals(Map.of(0,ITEM),next.deposits().get(OWNER).items());assertNull(next.deposits().get(OWNER).transfer());
    }
    @Test void oneAltarCannotContainTwoOwnersOrBeDeletedWhileHoldingItems() {
        Deposit d=new Deposit(UUID.randomUUID(),OWNER,"api",altar().table(),Map.of(0,ITEM),null);
        State state=initial().deposit(d);
        assertThrows(IllegalArgumentException.class,()->state.deposit(new Deposit(UUID.randomUUID(),UUID.randomUUID(),"api",altar().table(),Map.of(0,ITEM),null)));
        assertThrows(IllegalArgumentException.class,()->state.remove("api"));assertEquals(Map.of(0,ITEM),state.deposits().get(OWNER).items());
    }
    @Test void escrowMovesIntoRitualExactlyOnceWithoutChangingInputs() {
        Deposit d=new Deposit(UUID.randomUUID(),OWNER,"api",altar().table(),Map.of(0,ITEM,8,"Ag=="),null);State state=initial().deposit(d);
        Ticket t=new Ticket(UUID.randomUUID(),OWNER,"api",altar().table(),Phase.PREPARED,List.of(ITEM,"Ag=="),"Aw==");
        State begun=state.beginDeposit(t,d.id());assertTrue(begun.deposits().isEmpty());assertEquals(t,begun.tickets().get(OWNER));
        assertThrows(IllegalArgumentException.class,()->begun.beginDeposit(t,d.id()));
        Ticket wrong=new Ticket(t.id(),OWNER,"api",altar().table(),Phase.PREPARED,List.of("BA=="),"Aw==");
        assertThrows(IllegalArgumentException.class,()->state.beginDeposit(wrong,d.id()));
    }
    @Test void unsettledTransferCannotStartAndDifferentReceiptCannotRemoveItems() {
        Deposit d=new Deposit(UUID.randomUUID(),OWNER,"api",altar().table(),Map.of(0,ITEM),new Transfer(UUID.randomUUID(),ITEM,Map.of(0,ITEM,8,ITEM)));
        State state=initial().deposit(d);Ticket t=new Ticket(UUID.randomUUID(),OWNER,"api",altar().table(),Phase.PREPARED,List.of(ITEM),ITEM);
        assertThrows(IllegalArgumentException.class,()->state.beginDeposit(t,d.id()));assertThrows(IllegalArgumentException.class,()->state.removeDeposit(OWNER,UUID.randomUUID()));
    }
    @Test void committedOrRefundedTicketCannotPayAgain() {
        Ticket t=new Ticket(UUID.randomUUID(),OWNER,"api",altar().table(),Phase.PREPARED,List.of(ITEM),ITEM);
        State active=initial().prepare(t).phase(OWNER,t.id(),Phase.ACTIVE);
        assertThrows(IllegalArgumentException.class,()->active.phase(OWNER,t.id(),Phase.DELIVERED));
        State done=active.phase(OWNER,t.id(),Phase.COMMITTING).phase(OWNER,t.id(),Phase.DELIVERED);
        assertThrows(IllegalArgumentException.class,()->done.phase(OWNER,t.id(),Phase.REFUNDING));assertThrows(IllegalArgumentException.class,()->done.phase(OWNER,t.id(),Phase.DELIVERED));
    }
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/test/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionGridTest.java'
        OriginalHashes = @()
        Hash = '40a0bae5ba763becc8d872c8fa6a7a55d82fc02ffb6b62188b22becdee41625b'
        Required = $false
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.evolution;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class EvolutionGridTest {
    @Test void eightMaterialsSurroundWeaponAndNeverIncludeTheLockedResult() {
        Set<Integer> slots=new HashSet<>();
        for(int i=0;i<8;i++){int raw=EvolutionGrid.rawSlot(i);assertTrue(slots.add(raw));assertNotEquals(5,raw);assertEquals(i,EvolutionGrid.index(raw));}
        assertEquals(8,EvolutionGrid.index(5));assertEquals(-1,EvolutionGrid.index(0));assertEquals(-1,EvolutionGrid.index(10));
    }
    @Test void partialStacksNeverConsumeMoreThanRequiredOrOffered() {
        assertEquals(3,EvolutionGrid.accepted(4,1,64,false));assertEquals(1,EvolutionGrid.accepted(4,1,64,true));
        assertEquals(0,EvolutionGrid.accepted(4,4,64,false));assertEquals(2,EvolutionGrid.accepted(64,0,2,false));
        assertThrows(IllegalArgumentException.class,()->EvolutionGrid.accepted(4,5,1,false));
    }
    @Test void weaponDoesNotUnlockWithIncompleteMaterials() {
        var altar=EvolutionStoreTest.altar();assertFalse(EvolutionGrid.materialsComplete(altar,Map.of()));
        assertFalse(EvolutionGrid.materialsComplete(altar,Map.of(0,3)));assertTrue(EvolutionGrid.materialsComplete(altar,Map.of(0,4)));
        assertFalse(EvolutionGrid.materialsComplete(altar,Map.of(0,5)));
    }
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/test/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionLayoutDataTest.java'
        OriginalHashes = @()
        Hash = 'a0d7420e2f749a8bc5a40076d5300521f63de396518906af37c342293e123570'
        Required = $false
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.evolution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionAltar.*;
import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionStore.*;

class EvolutionLayoutDataTest {
    @TempDir Path directory;
    private EvolutionAltar altar(List<Ingredient> ingredients, boolean enabled) {
        var a = EvolutionStoreTest.altar();
        return new EvolutionAltar(a.id(), a.table(), a.source(), a.result(), ingredients, a.targets(), a.preset(),
                a.ticks(), a.scale(), a.density(), a.soundRadius(), a.damageRadius(), a.damage(), enabled, a.cues(), a.music(), a.effects(), a.lethal());
    }
    @Test void sparseLayoutPreservesChosenCellsAndEscrowIndexesAcrossRestart() throws Exception {
        var a = altar(List.of(new Ingredient("AQ==", 4, new Point(EvolutionStoreTest.WORLD, 16, 64, 0), 9),
                new Ingredient("Ag==", 2, new Point(EvolutionStoreTest.WORLD, -16, 64, 0), 1)), true);
        assertEquals(0, EvolutionGrid.index(a, 9)); assertEquals(1, EvolutionGrid.index(a, 1));
        assertEquals(-1, EvolutionGrid.index(a, 2)); assertEquals(8, EvolutionGrid.index(a, 5));
        assertEquals(9, EvolutionGrid.rawSlot(a, 0));
        Deposit deposit = new Deposit(UUID.randomUUID(), EvolutionStoreTest.OWNER, a.id(), a.table(), Map.of(0, "AQ=="), null);
        State state = new State(Map.of(a.id(), a), Map.of()).deposit(deposit);
        var store = new EvolutionStore(directory.resolve("evolution.yml"), EvolutionStoreTest.sounds());
        store.save(state); assertEquals(state, store.load());
    }
    @Test void legacyIngredientsWithoutSlotsRetainTheirOriginalPositions() {
        var store = new EvolutionStore(directory.resolve("evolution.yml"), EvolutionStoreTest.sounds());
        var old = EvolutionStoreTest.initial(); var yaml = store.document(old); yaml.set("altars.api.ingredients.1.slot", null);
        assertEquals(old, store.parse(yaml));
        assertEquals(1, store.parse(yaml).altars().get("api").ingredients().getFirst().slot());
    }
    @Test void draftCanSaveUnmappedPedestalsButCannotBeEnabled() throws Exception {
        var material = new Ingredient("AQ==", 4, null, 7); var a = altar(List.of(material), false);
        assertFalse(a.complete());
        assertThrows(IllegalArgumentException.class, () -> altar(List.of(material), true));
        var store = new EvolutionStore(directory.resolve("evolution.yml"), EvolutionStoreTest.sounds());
        var state = new State(Map.of(a.id(), a), Map.of()); store.save(state); assertEquals(state, store.load());
    }
    @Test void slotAndPedestalValidationRejectsDuplicatesCenterAndDistancesBeyondSixteen() {
        var point = new Point(EvolutionStoreTest.WORLD, 2, 64, 0);
        assertThrows(IllegalArgumentException.class, () -> new Ingredient("AQ==", 1, point, 5));
        assertThrows(IllegalArgumentException.class, () -> altar(List.of(new Ingredient("AQ==", 1, point, 1), new Ingredient("Ag==", 1, new Point(point.world(), 3, 64, 0), 1)), false));
        assertThrows(IllegalArgumentException.class, () -> altar(List.of(new Ingredient("AQ==", 1, point, 1), new Ingredient("Ag==", 1, point, 9)), false));
        assertThrows(IllegalArgumentException.class, () -> altar(List.of(new Ingredient("AQ==", 1, new Point(point.world(), 17, 64, 0), 1)), false));
        assertThrows(IllegalArgumentException.class, () -> altar(List.of(new Ingredient("AQ==", 1, new Point(UUID.randomUUID(), 2, 64, 0), 1)), false));
    }
    @Test void lightningOnlyFiresOnFirstMaterialCompletionAndNeverOnWeaponOrRepeatedRender() {
        var a = EvolutionStoreTest.altar();
        assertEquals(List.of(), EvolutionGrid.newlyComplete(a, Map.of(), Map.of(0, 2)));
        assertEquals(List.of(0), EvolutionGrid.newlyComplete(a, Map.of(0, 2), Map.of(0, 4)));
        assertEquals(List.of(), EvolutionGrid.newlyComplete(a, Map.of(0, 4), Map.of(0, 4, 8, 1)));
        assertEquals(List.of(), EvolutionGrid.newlyComplete(a, Map.of(0, 4), Map.of(0, 4)));
    }
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/test/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionStoreTest.java'
        OriginalHashes = @()
        Hash = '7ca36420d0da5c6f0240c21edbf2706fc7de2a6cbfa0ecd535154b2e9e1f0f99'
        Required = $false
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.evolution;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionAltar.*;
import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionStore.*;

class EvolutionStoreTest {
    @TempDir Path directory;
    static final UUID WORLD=UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID OWNER=UUID.fromString("22222222-2222-2222-2222-222222222222");
    static final String ITEM="AQ==";
    static EvolutionAltar altar() {
        return new EvolutionAltar("api",new Point(WORLD,0,64,0),ITEM,"Ag==",
                List.of(new Ingredient(ITEM,4,new Point(WORLD,2,64,0))),Set.of(OWNER),
                Preset.INFERNO,180,1,24,24,9,0,true,Map.of(),
                new Music("vitae:ritual/fire",false,.8f,180),new Effects(true,30,3,true),true);
    }
    static Set<String> sounds() {
        Set<String> result=new HashSet<>();
        for(Preset preset:Preset.values()) {
            var a=new EvolutionAltar("test",altar().table(),"","",List.of(),Set.of(),preset,180,1,24,24,9,0,false,Map.of());
            for(Stage stage:Stage.values()) result.add(a.cue(stage).sound());
        }
        result.add("minecraft:music_disc.pigstep"); return result;
    }
    EvolutionStore store(){return new EvolutionStore(directory.resolve("evolution.yml"),sounds());}
    static State initial(){return new State(Map.of("api",altar()),Map.of());}
    @Test void settingsDepositTransferAndRecoverySurviveAtomicRestart() throws Exception {
        Deposit d=new Deposit(UUID.randomUUID(),OWNER,"api",altar().table(),Map.of(0,ITEM),
                new Transfer(UUID.randomUUID(),ITEM,Map.of(0,ITEM,8,ITEM)));
        State state=initial().deposit(d);store().save(state);
        assertEquals(state,store().initialize()); assertEquals(2,store().document(state).getInt("state-version"));
        assertEquals(180,store().load().altars().get("api").ticks());
        assertEquals("vitae:ritual/fire",store().load().altars().get("api").music().sound());
    }
    @Test void legacyConfigurationAndPendingTicketsMigrateWithoutActivatingLethalOrMusic() throws Exception {
        Ticket t=new Ticket(UUID.randomUUID(),OWNER,"api",altar().table(),Phase.ACTIVE,List.of(ITEM),ITEM);
        YamlConfiguration yaml=store().document(initial().prepare(t));
        yaml.set("state-version",1);yaml.set("deposits",null);
        for(String field:List.of("music","effects","lethal"))yaml.set("altars.api."+field,null);
        State migrated=store().parse(yaml);
        assertFalse(migrated.altars().get("api").lethal());assertEquals("",migrated.altars().get("api").music().sound());
        assertEquals(t,migrated.tickets().get(OWNER));assertEquals(altar().ingredients(),migrated.altars().get("api").ingredients());
        store().save(migrated);assertEquals(migrated,store().load());
    }
    @Test void malformedAudioAndUnsafeEffectsNeverReplaceSavedSnapshot() throws Exception {
        store().save(initial());byte[] before=Files.readAllBytes(directory.resolve("evolution.yml"));
        Map<String,Object> invalid=Map.of("music.volume",2.0,"music.private","yes","music.sound","minecraft:no_such_sound",
                "effects.interval-ticks",1,"effects.radius",9,"lethal",1,"duration",Double.NaN);
        for(var entry:invalid.entrySet()) {
            var yaml=store().document(initial());yaml.set("altars.api."+entry.getKey(),entry.getValue());
            assertThrows(IllegalArgumentException.class,()->store().parse(yaml),entry.getKey());
        }
        assertArrayEquals(before,Files.readAllBytes(directory.resolve("evolution.yml")));
    }
    @Test void corruptSectionsUnknownVersionAndNoncanonicalSlotsAreRejected() {
        for(String path:List.of("deposits","altars.api.effects","altars.api.music")) {
            var yaml=store().document(initial());yaml.set(path,"wrong");assertThrows(IllegalArgumentException.class,()->store().parse(yaml));
        }
        var yaml=store().document(initial());yaml.set("state-version",3);assertThrows(IllegalArgumentException.class,()->store().parse(yaml));
        Deposit d=new Deposit(UUID.randomUUID(),OWNER,"api",altar().table(),Map.of(0,ITEM),null);
        var slots=store().document(initial().deposit(d));slots.set("deposits."+OWNER+".items.00",ITEM);
        assertThrows(IllegalArgumentException.class,()->store().parse(slots));
    }
    @Test void missingReloadNeverCreatesAnEmptyReplacement() throws Exception {
        assertEquals(new State(Map.of(),Map.of()),store().initialize());Files.delete(directory.resolve("evolution.yml"));
        assertThrows(java.io.IOException.class,()->store().load());assertFalse(Files.exists(directory.resolve("evolution.yml")));
    }
    @Test void unicodeAndAllLineEndingsRemainReadable() throws Exception {
        store().save(initial());String text=Files.readString(directory.resolve("evolution.yml"));
        for(String newline:List.of("\n","\r\n","\r")) {
            Files.writeString(directory.resolve("evolution.yml"),text.replace("\n",newline));assertEquals(initial(),store().load());
        }
    }
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/test/java/com/bangzachery/vitae/vitaemanager/npc/NpcFarmDataTest.java'
        OriginalHashes = @()
        Hash = 'd7fa7558381057ce95d6b25e4c44317f4b9486f1f0a3c76b955c49b7e31b491b'
        Required = $false
        Binary = $false
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
    [PSCustomObject]@{
        Name = 'src/test/java/com/bangzachery/vitae/vitaemanager/npc/NpcGateDataTest.java'
        OriginalHashes = @()
        Hash = 'f1993f6c98e90f033277fb49ac1689410daa572f9cf71b6783df3badf395cae7'
        Required = $false
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.whisper.WhisperArea;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.bangzachery.vitae.vitaemanager.npc.NpcData.*;

class NpcGateDataTest {
    @TempDir Path directory;
    static final UUID WORLD = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static Definition npc(String id) { return Definition.create(id, "Penjaga", new Point(WORLD, 4, 64, 0, 0, 0), new Point(WORLD, 3, 64, 0, 0, 0)); }
    static Gate gate(GateStatus status) { return new Gate(new WhisperArea(WORLD, 0, 64, 0, 0, 64, 0), List.of(new GateBlock(0, 64, 0, "minecraft:stone")), status); }
    @Test void oldNpcJsonWithoutFacingOrGateRetainsProgressQuotasAndPendingReward() throws Exception {
        var json = NpcStore.JSON.toJsonTree(npc("guard")).getAsJsonObject();
        json.add("gift", NpcStore.JSON.toJsonTree(new Reward(new BigDecimal("1000"), List.of())));
        var definition = NpcStore.JSON.fromJson(json, Definition.class); UUID owner = UUID.randomUUID();
        State state = State.empty().put(definition).finish(owner, definition, false, UUID.randomUUID(), 100);
        var root = NpcStore.JSON.toJsonTree(state).getAsJsonObject(); var record = root.getAsJsonObject("npcs").getAsJsonObject("guard");
        record.remove("facing"); record.remove("gate");
        Path file = directory.resolve("npc.json"); Files.writeString(file, NpcStore.JSON.toJson(root));
        var store = new NpcStore(file); assertEquals(state, store.initialize()); store.save(store.initialize()); assertEquals(state, store.initialize());
    }
    @Test void allGateStatusesSurviveRestartAndDoNotRemoveQuestHistory() throws Exception {
        var store = new NpcStore(directory.resolve("npc.json")); UUID player = UUID.randomUUID();
        for (GateStatus status : GateStatus.values()) {
            Definition definition = npc("guard").withGate(gate(status));
            State state = State.empty().put(definition).heard(player, definition); store.save(state);
            assertEquals(state, store.initialize()); assertTrue(store.initialize().done(player, definition));
        }
    }
    @Test void openChoiceNeedsConfiguredGateAndCountsAsAnExit() {
        var json = NpcStore.JSON.toJsonTree(npc("guard")).getAsJsonObject(); json.addProperty("enabled", true);
        json.add("nodes", NpcStore.JSON.toJsonTree(Map.of("start", new Node("Masuklah.", List.of(new Choice("Buka", "OPEN"))))));
        assertThrows(RuntimeException.class, () -> NpcStore.JSON.fromJson(json, Definition.class));
        json.add("gate", NpcStore.JSON.toJsonTree(gate(GateStatus.CLOSED)));
        assertNotNull(NpcStore.JSON.fromJson(json, Definition.class));
    }
    @Test void gateSelectionRejectsLargeSnapshotsDuplicatesAndOutsideCoordinates() {
        var block = new GateBlock(0, 64, 0, "minecraft:stone"); var area = gate(GateStatus.CLOSED).area();
        assertThrows(IllegalArgumentException.class, () -> new Gate(area, List.of(block, block), GateStatus.CLOSED));
        assertThrows(IllegalArgumentException.class, () -> new Gate(area, List.of(new GateBlock(1, 64, 0, "minecraft:stone")), GateStatus.CLOSED));
        assertThrows(IllegalArgumentException.class, () -> new Gate(new WhisperArea(WORLD, 0, 64, 0, 16, 80, 0), List.of(block), GateStatus.CLOSED));
    }
    @Test void distinctNpcsCannotClaimTheSameWall() {
        var first = npc("first").withGate(gate(GateStatus.CLOSED)); var second = npc("second").withGate(gate(GateStatus.CLOSED));
        assertThrows(IllegalArgumentException.class, () -> State.empty().put(first).put(second));
    }
    @Test void finishByAnotherPlayerCannotOverwriteOpeningStatusAndRewardsRemainOnceOnly() {
        var json = NpcStore.JSON.toJsonTree(npc("guard").withGate(gate(GateStatus.CLOSED))).getAsJsonObject();
        json.add("gift", NpcStore.JSON.toJsonTree(new Reward(new BigDecimal("1000"), List.of())));
        var closed = NpcStore.JSON.fromJson(json, Definition.class); UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        State state = State.empty().put(closed).finish(first, closed, false, UUID.randomUUID(), 100).put(closed.withGate(gate(GateStatus.OPENING)));
        state = state.finish(second, closed, false, UUID.randomUUID(), 101);
        assertEquals(GateStatus.OPENING, state.npcs().get("guard").gate().status()); assertEquals(2, state.pending().size());
        UUID receipt = state.pending().get(first).receipt(); state = state.acknowledge(first, receipt);
        assertSame(state, state.finish(first, closed, false, UUID.randomUUID(), 102));
        assertEquals(0, state.quota(first, 103).count());
    }
    @Test void invalidGateDataIsRejectedWithoutErasingSavedNpcFile() throws Exception {
        var store = new NpcStore(directory.resolve("npc.json")); State state = State.empty().put(npc("guard").withGate(gate(GateStatus.CLOSED))); store.save(state);
        var root = NpcStore.JSON.toJsonTree(state).getAsJsonObject(); root.getAsJsonObject("npcs").getAsJsonObject("guard").getAsJsonObject("gate").addProperty("status", true);
        Path file = directory.resolve("npc.json"); Files.writeString(file, NpcStore.JSON.toJson(root)); byte[] before = Files.readAllBytes(file);
        assertThrows(java.io.IOException.class, store::initialize); assertArrayEquals(before, Files.readAllBytes(file));
    }
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/test/java/com/bangzachery/vitae/vitaemanager/npc/NpcStoryDataTest.java'
        OriginalHashes = @()
        Hash = '3bc93762110cb35e19fe12764f2cdb23b4d8afb55af502f505396667e4bab8cf'
        Required = $false
        Binary = $false
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
    [PSCustomObject]@{
        Name = 'src/test/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperCinematicStoreTest.java'
        OriginalHashes = @()
        Hash = 'f3a9aa3d91b28e3a47b5413bea5e3719d1085ac35d5c70755fffca4853990a2c'
        Required = $false
        Binary = $false
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
    }
    [PSCustomObject]@{
        Name = 'src/test/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperCinematicTest.java'
        OriginalHashes = @()
        Hash = '3baafc211fc594f462a2aab8e221b2fce3927fc5eb2482baf45943fe1d54b28e'
        Required = $false
        Binary = $false
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
$changesVitae = New-Object 'System.Collections.Generic.List[object]'
$errorsVitae = New-Object 'System.Collections.Generic.List[string]'
$seenVitae = New-Object 'System.Collections.Generic.HashSet[string]'
foreach ($fileVitae in $filesVitae) {
    if ($fileVitae.Name -notmatch '^src/(main|test)/[A-Za-z0-9_./-]+$' -or $fileVitae.Name.Contains('..') -or
        -not $seenVitae.Add($fileVitae.Name)) { throw 'Daftar sumber installer tidak valid.' }
    $pathVitae = Join-Path $rootVitae $fileVitae.Name
    if ($fileVitae.Binary) { $bytesVitae = [Convert]::FromBase64String($fileVitae.Source) }
    else { $bytesVitae = $utf8Vitae.GetBytes((Normalize-Vitae $fileVitae.Source)) }
    if ((Get-VitaeHash $bytesVitae) -ne $fileVitae.Hash) { throw "Isi installer tidak lengkap/rusak: $($fileVitae.Name). Unduh ulang MD." }
    $existsVitae = Test-Path -LiteralPath $pathVitae -PathType Leaf
    if ((Test-Path -LiteralPath $pathVitae) -and -not $existsVitae) { $errorsVitae.Add("Path bukan file: $($fileVitae.Name)"); continue }
    if ($existsVitae) {
        $rawBeforeVitae = [IO.File]::ReadAllBytes($pathVitae)
        if ($fileVitae.Binary) { $beforeVitae = $rawBeforeVitae }
        else { $beforeVitae = $utf8Vitae.GetBytes((Normalize-Vitae ([IO.File]::ReadAllText($pathVitae, $utf8Vitae)))) }
        $oldHashVitae = Get-VitaeHash $beforeVitae
        if ($oldHashVitae -eq $fileVitae.Hash) {
            $hasBomVitae = $rawBeforeVitae.Length -ge 3 -and $rawBeforeVitae[0] -eq 239 -and $rawBeforeVitae[1] -eq 187 -and $rawBeforeVitae[2] -eq 191
            if ($fileVitae.Binary -or -not $hasBomVitae) { continue }
        } elseif ($fileVitae.OriginalHashes -notcontains $oldHashVitae) {
            $errorsVitae.Add("Sumber berbeda dari versi yang dikenali: $($fileVitae.Name)"); continue
        }
    } elseif ($fileVitae.Required) { $errorsVitae.Add("File dasar proyek hilang: $($fileVitae.Name)"); continue }
    $changesVitae.Add([PSCustomObject]@{ Name=$fileVitae.Name; Path=$pathVitae; Bytes=$bytesVitae; Existed=$existsVitae; Hash=$fileVitae.Hash })
}
if ($errorsVitae.Count -gt 0) {
    foreach ($errorVitae in $errorsVitae) { Write-Host $errorVitae -ForegroundColor Red }
    throw 'Pemeriksaan berhenti sebelum menulis. Simpan perubahan manualmu; kirim pesan di atas agar installer bisa disesuaikan.'
}
if ($CheckOnly) { Write-Host "CheckOnly lolos. $($changesVitae.Count) file perlu dipasang; belum ada file yang diubah." -ForegroundColor Green; return }
if ($changesVitae.Count -eq 0) { Write-Host 'Update dungeon/fade sudah terpasang. Tidak ada file yang ditulis ulang.' -ForegroundColor Green; return }
$backupVitae = Join-Path $rootVitae ('vitae-dungeon-backup-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [Guid]::NewGuid().ToString('N').Substring(0,8))
[IO.Directory]::CreateDirectory($backupVitae) | Out-Null
foreach ($changeVitae in $changesVitae) {
    $originalVitae = Join-Path (Join-Path $backupVitae 'original') $changeVitae.Name
    $stageVitae = Join-Path (Join-Path $backupVitae 'staged') $changeVitae.Name
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($stageVitae)) | Out-Null
    if ($changeVitae.Existed) {
        [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($originalVitae)) | Out-Null
        [IO.File]::Copy($changeVitae.Path, $originalVitae, $false)
    }
    [IO.File]::WriteAllBytes($stageVitae, $changeVitae.Bytes)
}
$manifestVitae = @($changesVitae | Select-Object Name,Existed,Hash) | ConvertTo-Json -Depth 4
[IO.File]::WriteAllText((Join-Path $backupVitae 'manifest.json'), $manifestVitae, $utf8Vitae)
$writtenVitae = New-Object 'System.Collections.Generic.List[object]'
try {
    foreach ($changeVitae in $changesVitae) {
        [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($changeVitae.Path)) | Out-Null
        $writtenVitae.Add($changeVitae)
        [IO.File]::Copy((Join-Path (Join-Path $backupVitae 'staged') $changeVitae.Name), $changeVitae.Path, $true)
        if ((Get-VitaeHash ([IO.File]::ReadAllBytes($changeVitae.Path))) -ne $changeVitae.Hash) { throw "Verifikasi gagal: $($changeVitae.Name)" }
    }
} catch {
    $causeVitae = $_.Exception.Message
    $restoreErrorsVitae = New-Object 'System.Collections.Generic.List[string]'
    for ($indexVitae=$writtenVitae.Count-1; $indexVitae -ge 0; $indexVitae--) {
        $changeVitae = $writtenVitae[$indexVitae]
        try {
            if ($changeVitae.Existed) { [IO.File]::Copy((Join-Path (Join-Path $backupVitae 'original') $changeVitae.Name), $changeVitae.Path, $true) }
            elseif (Test-Path -LiteralPath $changeVitae.Path -PathType Leaf) { [IO.File]::Delete($changeVitae.Path) }
        } catch { $restoreErrorsVitae.Add($changeVitae.Name) }
    }
    if ($restoreErrorsVitae.Count -gt 0) { throw "Pemasangan gagal: $causeVitae. Pemulihan perlu dibantu untuk: $($restoreErrorsVitae -join ', '). Backup: $backupVitae" }
    throw "Pemasangan gagal dan file yang sempat diubah sudah dipulihkan: $causeVitae. Backup: $backupVitae"
}
Write-Host "Update dungeon/fade terpasang: $($changesVitae.Count) file." -ForegroundColor Green
Write-Host "Backup sumber lama: $backupVitae" -ForegroundColor Cyan
Write-Host 'Berikutnya jalankan: .\gradlew.bat clean build' -ForegroundColor Yellow
Write-Host 'Saat mengganti JAR server, pertahankan folder data plugins/vitaemanager.' -ForegroundColor Yellow
```
<!-- VITAE_INSTALLER_END -->
