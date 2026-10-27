# Pembaruan pose, teks, terrain, dan audio — 25 Oktober 2026

Project/JAR yang sama, Paper **1.21.4**, Java **21**. `RULES.md`, `PROJECT.md`, dan `ARCHITECTURE.md` dibaca. Tidak menambah dependency, mengganti command/permission, atau memodifikasi nominal Viti, katalog NPC, dan data pemain. Hash 23 source ekonomi/NPC dibandingkan dengan baseline awal pekerjaan ini dan tetap sama. Perubahan lokal sebelumnya dipertahankan.

## Akar masalah dan perubahan

- **Tunduk:** adapter sebelumnya mensyaratkan signature crawl GSit tertentu. Sekarang memakai pose native Paper, swimming flag, fixed pose, dan pemeriksaan hitbox server aktual. Radius default20, durasi20detik; posisi terkunci tetapi yaw/pitch bebas. Tidak memasang blok, mengirim damage, atau memerlukan GSit. Bukti kepemilikan pose ditulis sebelum perubahan; death/logout/admin stop/disable/restart tidak melanjutkan penggunaan lama. Pose eksternal tidak ditimpa saat cleanup. Detail: [Tunduk native](TUNDUK-NATIVE-POSE.md).
- **Teks:** entity world yang diteleport dan paket passenger client sebelumnya sama-sama mengatur posisi; transform tambahan juga tidak mengompensasi anchor native. Sekarang template TextDisplay tidak di-spawn ke world; hanya paket spawn/metadata/passenger/remove kepada audience yang sah. Tidak ada paket gerak display atau teleport scheduler. Transform mengompensasi seat native sekali dan menggunakan tinggi pose aktual. Carry asli tetap disertakan. Detail rumus, API, dan cleanup: [anchor teks](ROLEPLAY-TEXT-ANCHOR.md).
- **Terrain:** validasi lama terlalu membatasi blok permukaan. Kini solid terrain dan vegetasi yang aman didukung tanpa persyaratan tanah rata/seluruh shell di udara. Geometri bola tertutup tetap sama; hanya shell diganti. Snapshot NBT persisten tetap mendahului perubahan. Pemulihan blok biasa memakai block data tanpa physics; container yang didukung memakai pemulihan NBT lengkap dan verifikasi. Waterlogged, gravity/fluida, tile aktif, linked structure yang belum aman, serta tetangga terhubung berisiko ditolak dengan koordinat/alasan spesifik. Ini batas keselamatan data, bukan larangan lereng. Detail: [pemulihan domain](DOMAIN-RECOVERY.md).
- **Double chest:** perbaikan sebelumnya tetap dipertahankan. `getLeftSide().getInventory().getHolder()` dapat kembali ke DoubleChest yang sama. Traversal iteratif melacak identitas inventory/holder dan memeriksa kedua sisi; tidak merekursi tanpa batas atau membatalkan semua hopper. Sembilan tes regresi mencakup hubungan objek siklik. Detail: [runtime inventory](DOMAIN-INVENTORY-RUNTIME-FIX.md).
- **Audio:** satu scheduler berbasis tick milik penggunaan mengatur setiap fase, tanpa task suara terpisah. Pergantian fase menghentikan loop fase lama; selesai/logout/disable idempoten. Penerima dan jarak dipilih per cue, bukan broadcast server. Registry kepemilikan mencegah cleanup satu penggunaan menghentikan ID yang masih dipakai penggunaan Vitae lain. Cleanup korban melepaskan penerima dan sumber suara korban tersebut.

## Pengaturan admin

Tambahkan pada `config.yml` bila perlu (fallback default berlaku tanpa menulis ulang konfigurasi lama):

```yaml
roleplay:
  text-head-clearance: 0.12
```

Rentang finite0–1 blok dari atas pose ke bagian bawah teks. Reload dengan `/vitae reload`. Default tidak menjamin hasil identik untuk semua font/resource pack/client.

Setiap `magic/<id>.yml` mendukung fase `activation`, `formation`, `active`, `release`, dan `ending`. Tidak setiap mekanik memiliki semua fase: Tunduk langsung aktif; Telekinesis formation berarti pengangkatan; domain formation/active/release mengikuti pembentukan/aktif/pemulihan. `activation` dan `ending` selalu sekali. Contoh daftar cue:

```yaml
sounds:
  active:
    enabled: true
    audience: nearby
    origin: center
    range: 24
    cues:
      - type: vanilla
        id: ENTITY_WARDEN_HEARTBEAT
        volume: 0.7
        pitch: 0.5
        interval-ticks: 50
      - type: custom
        id: my_pack:pressure
        volume: 0.5
        pitch: 0.8
        interval-ticks: 100
```

- `audience`: `owner`, `victims`, `participants` (owner+korban), `nearby` (pemain dalam world/jarak yang sama).
- `origin`: `center`, `owner`, atau `victim`. Kombinasi victims/victim hanya memperdengarkan sumber tubuh korban itu sendiri. Telekinesis memakai kombinasi ini untuk kelelawar default setiap50tick.
- `range`: batas seleksi penerima0–128blok dari sumber; suara tetap mengikuti atenuasi client vanilla.
- `interval-ticks`:0 berarti sekali ketika fase dimulai; positif berarti berulang, maksimum72000. Scheduler tidak menumpuk pemutaran tertinggal. Volume0–16; pitch0,01–2. Tidak ada suara setiap tick pada preset default.
- `type: vanilla` memakai enum/key sound vanilla yang tervalidasi. `custom` memakai ID resource pack client, tanpa dependency ItemsAdder.
- Format lama satu `id/type/volume/pitch/interval-ticks` per fase tetap berlaku dan mengambil prioritas atas daftar default. `enabled: false` tetap dihormati. Fase yang belum ada memakai fallback resource bundled; file pengguna tidak ditimpa. Untuk memakai seluruh rangkaian baru, backup lalu gabungkan bagian `sounds` dari resource JAR secara manual. `/sihir reload` memvalidasi konfigurasi; penggunaan aktif mempertahankan snapshot awal.

Preset baru: domain memakai Wither/Warden, heartbeat/portal, lalu dragon/deepslate dan beacon; Telekinesis memakai sonic/Wither, kepakan dan kelelawar berkala, lalu elder guardian/beacon; Tunduk memakai Warden/Wither, heartbeat/respawn anchor, dragon hurt/beacon. Pemutaran normal berakhir sekali; logout/death/disable menghentikan loop tanpa mengulang rangkaian penutupan. API stop-sound client hanya mengenali ID+kategori, sehingga tidak dapat membedakan sound dengan ID sama yang kebetulan diputar plugin lain. Gunakan ID custom terpisah bila isolasi audio lintas plugin diperlukan.

GSit tetap tercantum sebagai integrasi lama opsional, tetapi tidak menjadi prasyarat Tunduk. LuckPerms masih dibutuhkan untuk evaluasi grup Unlimited Misery. Tidak mengubah deklarasi load order ItemsAdder/nightcore: warning dependency cycle sebelumnya terpisah dari rekursi inventory dan bukan bukti bahwa GSit atau perubahan ini penyebabnya; lihat laporan runtime sebelumnya. Error plugin lain tidak diperlakukan sebagai error VitaeManager.

## Pemasangan dan verifikasi langsung

Matikan server dengan normal, backup data plugin dan world, ganti JAR VitaeManager lama dengan JAR hasil build ini, lalu mulai ulang. Jangan menghapus folder data, journal, receipt, cooldown, atau katalog. Tidak perlu reset/migrasi saldo. Jika pemulihan domain belum selesai, biarkan journal diproses; jangan menghapusnya untuk melewati validasi.

Belum menjalankan Paper/client Minecraft dalam pekerjaan ini. Pemeriksaan API/bytecode, tes logika dan clean build tidak membuktikan perilaku visual, reflection runtime, world-save crash recovery, atau pengalaman suara. Sebelum produksi uji dengan salinan world dan minimal dua client:

1. Tunduk tanpa GSit: hitbox benar-benar pendek (F3+B dan ruang crawl), bebas menoleh, tidak berpindah/menyerang/item/sihir. Uji normal, korban/pengguna mati atau logout, admin stop, disable, restart, serta pose dari sumber lain.
2. `/me` dan `/do` dari client pengamat: jalan/sprint/lompat/sneak/crawl, font/wrapping, carry dua arah, tracking/vanish, teleport/worldchange/death/relog. Sesuaikan clearance; periksa jitter tanpa menerima klaim visual dari unit test.
3. Domain radius30 pada tanah rata, lereng, bukit, lembah; shell atas/bawah tertutup dan interior tetap. Uji container yang didukung dan bandingkan NBT/isi/terrain sebelum-sesudah normal, penghentian, restart/crash serta restart saat recovery. Lokasi unsafe harus ditolak sebelum mutasi dengan alasan spesifik.
4. Single chest, kedua sisi double chest, hopper→chest, chest→hopper, tanpa domain serta dalam perlindungan. Area luar harus normal; tidak ada StackOverflowError.
5. Dengarkan tiap fase dari pengguna, korban, sekitar, luar jangkauan/world lain; bats berkala Telekinesis. Uji dua penggunaan dengan ID sound sama, korban logout/mati, pergantian fase dan disable: tidak ada loop/task tertinggal atau penutupan berulang.

## Hasil verifikasi final

Perintah `./gradlew.bat clean build --no-configuration-cache --no-build-cache` selesai dengan **BUILD SUCCESSFUL**, seluruh enam task dieksekusi. XML JUnit final: **281 tes, 45 suite, 0 gagal, 0 error, 0 dilewati**. Termasuk 12 tes pose, 25 tes domain (9 di antaranya regresi inventory), 13 tes audio, 8 tes layout teks dan 6 tes konfigurasi roleplay. Kasus delivery audio menutup scheduler di tengah daftar cue diuji gagal sebelum perbaikan dan lulus setelah perbaikan. Tidak ada tes Minecraft langsung.

JAR: `build/libs/vitaemanager-2.0.0-dev.1.jar`, ukuran870432byte, SHA-256 `024fed04c79d6d4c7825630fafa4f6f3760013c07c642428ca3d8ebac7d36cc6`. Isi JAR dibandingkan dengan class hasil build; resource magic/config cocok, plugin.yml hasil ekspansi versi cocok, API tetap1.21.4 dan bytecode Java21 (major65). Tidak membundel API Bukkit/Paper/LuckPerms. `git diff --check` lulus; compiler masih mengeluarkan catatan penggunaan deprecated API.

Laporan HTML otomatis: `build/reports/tests/test/index.html`. Ringkasan machine-readable: `build/final-pose-terrain-audio-verification.json`. Baseline hash source ekonomi/NPC disimpan di `docs/task-unrelated-source-hashes.json`; 23 file tetap sama. Hash ini membandingkan awal dan akhir pekerjaan lanjutan, bukan terhadap HEAD lama yang sudah memiliki perubahan lokal.
