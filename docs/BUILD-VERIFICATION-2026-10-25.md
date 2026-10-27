# Verifikasi build VitaeManager - 25 Oktober 2026

Pemeriksaan dilakukan langsung di project IntelliJ yang tersedia. Tidak membuat project/plugin pengganti atau ZIP. `PANDUAN-SETUP.md` yang semula staged tetap staged dan tidak diubah; tidak menjalankan reset, commit, atau staging tambahan.

## Build dan pengujian otomatis

Perintah final: `.\gradlew.bat clean build --no-configuration-cache`.

Hasil: **BUILD SUCCESSFUL**, exit 0, 6 actionable tasks (5 executed, 1 compileTestJava from cache). Task `test` benar-benar dieksekusi sesudah `clean`, bukan hanya UP-TO-DATE. Gradle melaporkan sebagian API deprecated; tidak ada error kompilasi.

Laporan XML final: **227 tes**, **40 suite**, **0 failure**, **0 error**, **0 skipped**. Termasuk 48 tes baru: migrasi layout6, formatter1, placeholder nilai1, classloading tanpa PlaceholderAPI1, pesan carry1, domain7, magic13, toko18. Tes lain merupakan regresi fitur project yang sudah ada.

Pengujian domain membuktikan geometri/journal/NBT sintetis; pengujian toko membuktikan aritmetika/revisi/journal/NBT sintetis. Pengujian magic membuktikan persistence state/victim journal dan validasi parameter. Tes tidak menjalankan Minecraft server atau mensimulasikan semua event/physics Bukkit. Tes classloading tanpa PAPI memeriksa facade dan main dapat dimuat ketika class API tidak ada; ini belum pengujian onEnable langsung di server.

## Pemeriksaan artefak

- JAR: `build/libs/vitaemanager-2.0.0-dev.1.jar`
- Ukuran: 835585 byte.
- SHA-256: `5d88cadbf5e4d4beb730773e9039294d3cf89108b8ea40ba4466bc92780bb0fa`.
- ZIP/JAR integrity: OK, seluruh entri dibaca tanpa korupsi.
- `plugin.yml`: main/nama lama tetap, `api-version: 1.21.4`, command sihir dan soft dependencies terdaftar.
- Main class bytecode major65 (Java21).
- Ketiga YAML sihir serta class magic/domain/toko/placeholder terdapat dalam JAR.
- Tidak membundel Bukkit/Paper/Adventure/PlaceholderAPI/JUnit.
- `git diff --check`: exit0. Tidak ada perubahan pada file user yang sudah staged sebelum pekerjaan.

## Pemeriksaan kode dan review

Perbaikan hasil review mencakup: tujuan migrasi aktif yang hilang tidak dipulihkan dari salinan usang; receipt pemulihan toko memverifikasi ulang native inventory; hold saldo dibaca sebelum startup NPC; partial-start cleanup tidak menghentikan cleanup modul lama; domain memakai snapshot lengkap sebelum mutasi dan flush `World.save(true)` sebelum completion; mount dan teleport korban dikendalikan; preflight domain tidak memakan cooldown; konfigurasi kecepatan/nominal/interval/sound divalidasi.

Snapshot domain disimpan permanen untuk investigasi dan pemulihan. Transaksi ambigu dikarantina dengan journal, bukan ditebak/diselesaikan dua kali. Ini membutuhkan admin memeriksa bukti bila persistensi native gagal atau data di-rollback secara eksternal.

## Belum diuji langsung

**Tidak ada pengujian Minecraft server/client dalam sesi ini.** Versi GSit/LuckPerms terpasang tidak tersedia. Uji live wajib meliputi lifecycle dan crash pada tiap fase, container custom, GSit/WorldGuard, NPC renderer/API native playerdata, antihero diwariskan, kemampuan eksternal, Telekinesis plafon/fall damage, title/partikel/audio/kamera, serta kelancaran `/me`/`/do` sambil carry.

Batas implementasi: domain sengaja menolak blok/struktur yang belum dapat dipulihkan aman; crawl asing setelah restart tidak dihapus berdasarkan tebakan; checkpoint durasi potion dapat berbeda sampai sekitar satu detik sesudah crash; damage/ability eksternal yang tidak dapat diatribusikan memerlukan adapter; kamera hanya animasi hurt tanpa damage dan kepadatan partikel bergantung client. Detail tersedia pada [panduan update](UPDATE-2026-10-25.md), [magic](MAGIC-MODULE.md), [domain](DOMAIN-RECOVERY.md), dan [toko](NPC-SHOPS.md).
