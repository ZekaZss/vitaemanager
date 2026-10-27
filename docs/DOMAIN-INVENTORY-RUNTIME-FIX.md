# Perbaikan runtime perlindungan inventory domain

## Bukti dan akar masalah

Brief runtime melaporkan 120 error `Could not pass event PaperInventoryMoveItemEvent to vitaemanager`, `StackOverflowError`, serta pengulangan `DomainManager.inventoryProtected` pada baris lama 174. Source project memang mempunyai rekursi pada baris tersebut. `RULES.md` sekarang tersedia dan telah dibaca; instruksi data/API, perubahan terfokus, dan verifikasi dipertahankan.

Jalur lama:

```text
inventory gabungan.getHolder() -> DoubleChest
DoubleChest.getLeftSide() -> holder Chest sisi kiri
Chest.getInventory() -> inventory gabungan yang sama
inventoryProtected(inventory gabungan) -> mengulang tanpa batas
```

Pemanggilan sisi kanan juga dapat kembali ke inventory gabungan. Rekursi kiri yang tidak pernah selesai bahkan mencegah pemeriksaan sisi kanan yang dilindungi. Error dapat terjadi ketika tidak ada domain aktif, karena kode lama tetap menelusuri double chest sebelum memeriksa blok.

API dari dependency Paper **1.21.4** diperiksa melalui `javap`: `DoubleChest.getLeftSide/getRightSide` memanggil `Inventory.getHolder()` pada inventory sisi; `getInventory()` pada DoubleChest mengembalikan inventory gabungannya. [Implementasi CraftInventoryDoubleChest Paper 1.21.4](https://github.com/PaperMC/Paper/blob/ver/1.21.4/paper-server/src/main/java/org/bukkit/craftbukkit/inventory/CraftInventoryDoubleChest.java) menyimpan inventory kiri/kanan dan membuat holder DoubleChest baru saat `getHolder()`. Karena itu identity holder tidak boleh dijadikan satu-satunya penanda kunjungan.

## Perubahan terfokus

Hanya algoritme privat `DomainManager.inventoryProtected` dan import API terkait yang berubah:

- Antrean iteratif menggantikan rekursi.
- Set berbasis **identitas inventory** mencatat node yang sudah diperiksa; back-edge tidak diperiksa ulang meskipun holder DoubleChest dibuat baru.
- `DoubleChestInventory.getLeftSide/getRightSide` memasukkan kedua inventory fisik ke antrean. Holder BlockState diperiksa langsung melalui bloknya, tanpa memanggil `Chest.getInventory()` yang menyatukan sisi kembali.
- View inventory biasa yang mengembalikan holder DoubleChest diarahkan ke inventory gabungannya dan menggunakan set kunjungan yang sama.
- Ketika tidak ada handle domain, pemeriksaan langsung menghasilkan false. Handle yang sudah dipulihkan tidak lagi melindungi bloknya.

Seluruh pemanggil ditelusuri: penutupan GUI container saat persiapan domain, klik inventory, drag, perpindahan inventory sumber/tujuan, dan pickup hopper. Kondisi pembatalan event tetap hanya berdasarkan perlindungan container terkait. Pembatalan oleh plugin lain tidak dibuka kembali. Tidak menangkap StackOverflowError, mengubah item, menonaktifkan proteksi, membatalkan semua transfer, atau menyembunyikan error.

Tidak ada perubahan API publik, permission, command, dependency, format konfigurasi/data, journal, saldo, ownership, atau progres. Tidak ada migrasi tambahan untuk perbaikan ini.

## Pengujian regresi

`DomainInventoryProtectionTest` memakai class **DoubleChest asli dari dependency Paper**, fixture inventory/Chest yang mempertahankan hubungan objek sebenarnya, event InventoryMoveItemEvent, dan handle domain dari bentuk journal yang sudah ada. Tes berjalan tanpa world Minecraft. Data world tidak diubah dan scheduler tidak dimulai.

Sebelum perbaikan, **5 dari 7 tes awal gagal dengan StackOverflowError**. Sesudah perbaikan dan penambahan dua kasus, **9 tes regresi lulus**:

1. Kedua holder Chest sisi mengembalikan inventory gabungan yang sama; double chest di luar domain tidak dilindungi.
2. Perlindungan sisi kiri maupun kanan, dengan urutan sisi dibalik.
3. Single chest pada shell dan di luar shell.
4. Inventory sisi yang menunjuk kembali ke objek sebelumnya, termasuk holder DoubleChest baru pada setiap permintaan; cabang kanan tetap diperiksa.
5. Hopper ke single/double chest dan sebaliknya; container terlindungi diblokir, container di luar perlindungan tetap dapat memindahkan item.
6. View inventory biasa dengan holder gabungan baru dan edge kembali ke view semula.
7. Container kembali berfungsi setelah handle domain berstatus selesai dipulihkan.
8. Single/double chest dan transfer hopper ketika tidak ada domain aktif.
9. Inventory tanpa holder dan event yang sebelumnya dibatalkan sistem lain.

Hasil clean build, hitungan seluruh suite, ukuran dan checksum JAR dicatat pada `DOMAIN-INVENTORY-FIX-BUILD.json` di folder dokumentasi ini. Tes tidak menyalakan Paper atau meniru seluruh perilaku CraftBukkit/hopper.

## Warning dependency cycle: pemeriksaan terpisah

File lokal `C:/Users/Administrator/Downloads/latest.log` memuat warning pada baris 2070-2071, diterbitkan oleh **MythicLib**, dengan path `[vitaemanager, ItemsAdder, nightcore, ItemsAdder]`. Log lokal ini tidak memuat 120 error inventory dari brief; hitungan itu tetap bukti yang diberikan pengguna, bukan hitungan yang diklaim dari file lokal tersebut. Log lokal mengidentifikasi Paper 1.21.4 build 232, ItemsAdder 4.0.9, nightcore 2.13.2, dan MythicLib 1.7.1-SNAPSHOT.

Temuan deklarasi/load order:

- VitaeManager hanya mempunyai `plugin.yml`; tidak mempunyai `paper-plugin.yml`, hard `depend`, `loadbefore`, atau dependency ke nightcore. `ItemsAdder` tetap ada dalam `softdepend` bersama integrasi lama.
- JAR lokal ItemsAdder **4.0.9-hotfix-1** dan **4.0.16** diperiksa tanpa dijalankan: dependency wajib ProtocolLib dan daftar soft dependencies integrasi; tidak ada deklarasi nightcore atau VitaeManager. JAR ini tidak boleh diasumsikan identik dengan JAR ItemsAdder 4.0.9 yang menghasilkan log.
- Bytecode `DependencyCycleCheck`/`DependencyNode` dari JAR lokal MythicLib 1.7.1-SNAPSHOT diperiksa. Checker membentuk edge dari `getDepend`, `getSoftDepend`, dan arah kebalikan `getLoadBefore`, lalu mengembalikan seluruh stack DFS ditambah node yang berulang. Dengan algoritme itu, prefix `vitaemanager` pada path bukan bukti bahwa VitaeManager berada pada segmen siklus tertutup. Segmen yang berulang pada warning adalah `ItemsAdder -> nightcore -> ItemsAdder`.
- [Metadata NightCore pada source resmi saat diperiksa](https://github.com/nulli0n/nightcore-spigot/blob/master/main/src/main/resources/paper-plugin.yml) menyebut ItemsAdder sebagai dependency Paper opsional dengan `load: AFTER`. Source master tersebut **bukan verifikasi metadata versi 2.13.2 yang terpasang**. JAR nightcore 2.13.2 dari server penghasil log tidak tersedia secara lokal, sehingga edge efektif dan cara descriptor Paper diproyeksikan ke descriptor Bukkit pada server itu belum dapat dipastikan.

Kesimpulan yang didukung bukti: warning load-order ini terpisah dari rekursi inventory dan tidak membuktikan perlunya menghapus integrasi ItemsAdder dari VitaeManager. Metadata plugin tidak diubah. Untuk menyelesaikan warning secara definitif, periksa `paper-plugin.yml`/`plugin.yml` dari **JAR ItemsAdder, nightcore dan MythicLib yang benar-benar dipasang**, berikut metadata efektif saat Paper berjalan. Jangan menghapus softdepend sekadar untuk menghilangkan nama VitaeManager dari prefix warning.

Log lokal juga mempunyai error ReviveMe (versi/event damage), MiniMOTD (class event tidak tersedia), Skript/SkBee, konfigurasi skill MMOItems/MythicLib, SQL MCPets, dan resource/texture ItemsAdder. Itu bukan stack rekursi DomainManager dan tidak diubah oleh patch ini. Tidak ada klaim error plugin lain sudah diperbaiki.

## Pemasangan dan uji langsung Paper 1.21.4

1. Hentikan server secara normal, backup world/playerdata dan folder data plugin bersama. Ganti JAR VitaeManager dengan JAR dari clean build. Jangan menyimpan dua JAR VitaeManager aktif. Jangan menghapus konfigurasi, journal, snapshot domain, atau data pemain. Gunakan restart penuh, bukan hot reload.
2. Tanpa domain aktif, pasang single chest dan double chest dalam dua orientasi, dengan hopper memasukkan serta menarik item. Kedua arah harus berfungsi normal tanpa StackOverflowError. Ulangi membuka, klik/drag, dan pickup hopper.
3. Pada salinan world, aktifkan domain di lokasi yang lolos validasi. Jangan memaksa double chest pada shell: validasi domain memang menolak struktur multiblok yang tidak aman. Single chest shell yang didukung harus tetap dilindungi selama snapshot/pembentukan/pemulihan; hopper di kedua arah tidak boleh mengambil/memasukkan isinya. Chest di luar shell tetap berfungsi. Perlindungan dua sisi double chest diperiksa oleh tes state sintetis; pengujian langsung jangan melewati penolakan lokasi/mengedit journal untuk memaksanya.
4. Jika shell semula berisi chest, bloknya menjadi obsidian selama domain berlangsung; jangan mengharapkan GUI chest tersedia saat tahap tersebut. Periksa kembali akses/hopper setelah blok dipulihkan tetapi sebelum cleanup domain selesai, serta setelah perlindungan dilepas. Jalankan pengujian pada salinan server dan bandingkan jumlah item sebelum/sesudah agar tidak merusak data produksi.
5. Ulangi penghentian admin serta restart saat pemulihan; journal harus tetap dipakai oleh mekanisme lama. Setelah pemulihan lengkap, container dan hopper harus normal. Periksa `latest.log` baru untuk `PaperInventoryMoveItemEvent`, `StackOverflowError`, dan error domain.
6. Untuk warning cycle, kumpulkan descriptor JAR yang benar-benar dipakai dan startup log baru. Periksa error enable plugin yang terlibat secara terpisah dari error event inventory. Jangan menyimpulkan warning sudah hilang hanya dari unit test atau build.

**Belum dilakukan:** pengujian server/client Minecraft langsung dan verifikasi startup dependency graph pada JAR server yang menghasilkan warning. Build dan tes otomatis membuktikan perbaikan traversal pada kasus yang diuji, bukan seluruh runtime Minecraft.
