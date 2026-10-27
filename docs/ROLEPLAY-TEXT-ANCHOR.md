# Posisi teks `/me` dan `/do` — Paper 1.21.4

Perubahan ini hanya menyentuh tampilan teks roleplay dan konfigurasi jarak bawah teks. Command, permission, durasi, isi teks, carry sebenarnya, dan penyimpanan pemain tetap memakai sistem lama. Target tetap Paper Minecraft 1.21.4 dan Java 21, tanpa dependency tambahan.

## Penyebab pada implementasi sebelumnya

Display lama adalah entity world yang diteleport setiap tick ke `kaki pemain + tinggi pose + 0,5`. Client yang sama juga menerima paket penumpang visual yang menempelkan display ke pemain. Kedua jalur sama-sama dapat mengubah posisi display. Transform tambahan tetap `+0,95` tidak memperhitungkan anchor kursi vanilla maupun tinggi pose. Spawn awal dan posisi setelah penempelan karena itu memakai dasar berbeda. Interpolation tambahan akan menyamarkan gerak dengan keterlambatan dan tidak menghapus penyebab tersebut.

## Jalur posisi sekarang

`World.createEntity(location, TextDisplay.class)` pada API Paper 1.21.4 membuat template **belum di-spawn**. Template diatur melalui API TextDisplay, tetapi tidak dimasukkan ke world/tracker/chunk dan tidak pernah dipanggil `spawn`, `teleport`, `showEntity`, atau `addPassenger`. Adapter `RoleplayDisplayPackets` mengirim spawn, metadata, daftar penumpang, dan remove kepada client yang boleh melihatnya. Adapter tidak mempunyai jalur paket pergerakan/teleport display.

Posisi dunia display sepenuhnya mengikuti penumpang visual pada client yang juga merender gerak pemain. Gerak berjalan, sprint, dan lompat tidak menyebabkan koreksi posisi display oleh scheduler. Scheduler memeriksa durasi/lifecycle serta pose; metadata transform hanya berubah ketika pose, tinggi entity/scale, atau konfigurasi clearance berubah. Tidak ada teleport smoothing ataupun pengiriman ulang anchor periodik.

Perhitungan Y mengompensasi anchor vanilla, bukan menambahkan tinggi pemain lagi:

```
posisi penumpang vanilla = kaki pemain + seat - vehicleAttachment
transformY = tinggi pose pemain + clearance - seat + vehicleAttachment
bagian bawah teks = kaki pemain + tinggi pose pemain + clearance
```

`seat` dan `vehicleAttachment` dibaca dari implementasi native target melalui `getPassengerRidingPosition(Entity)` dan `getVehicleAttachmentPoint(Entity)`, bukan menebak offset armor stand atau mengunci tinggi berdiri. Pose dan `Player.getHeight()` menentukan tinggi yang sekarang berlaku. Angka tinggi pemain, tinggi armor stand, atau tinggi baris teks tidak ditambahkan dua kali.

Renderer TextDisplay 1.21.4 menempatkan dasar background pada origin entity dan baris-baris teks ke atas; teks yang membungkus tidak memerlukan tambahan tinggi pada posisi dasar. Alignment tetap CENTER untuk teks. Billboard menggunakan VERTICAL: arah horizontal mengikuti kamera, sedangkan translasi Y tetap vertikal dunia. CENTER billboard memutar transform translation mengikuti pitch kamera dan tidak cocok untuk mempertahankan clearance vertikal. Saat dilihat persis dari atas/bawah, teks vertikal dapat tampak tipis sebagaimana bidang teks vertikal lainnya.

## Konfigurasi

Tambahkan bila ingin mengubah default:

```yaml
roleplay:
  text-head-clearance: 0.12
```

Nilai berupa angka finite 0–1 blok, dihitung dari **bagian atas pose pemain ke dasar teks**, bukan ke pusat atau baris teratas teks. Default 0,12 menjaga teks dekat kepala. Konfigurasi lama mendapat fallback tanpa penulisan ulang file. `/vitae reload` memuat nilai tersebut secara atomik dan teks aktif menyesuaikan pada pemeriksaan pose berikutnya. Nilai salah ditolak bersama snapshot konfigurasi baru; snapshot sebelumnya tetap digunakan. Konstruktor publik lama `RoleplaySettings(int,int,int,int)` tetap tersedia dengan clearance default.

## Carry, jangkauan, dan cleanup

Daftar penumpang server tidak pernah diubah oleh teks. Setiap paket penempelan memuat penumpang asli yang ada **saat paket dibuat**, dalam urutan asli, kemudian ID visual milik teks. Saat melepas teks, paket kembali memuat daftar penumpang asli terkini. Mount/dismount carry menjadwalkan pembaruan anchor setelah perubahan server selesai.

Audience tetap memakai world, `canSee`, metadata vanish, PDC vanish, serta tracker pemain sumber. Pemeriksaan visibility lama setiap lima tick dipertahankan. Event Paper `PlayerTrackEntityEvent` mengantre pairing setelah spawn pemain selesai. `PlayerUntrackEntityEvent` segera melepas ID visual dan membersihkan audience, sehingga keluar/masuk tracking cepat tidak meninggalkan teks terlepas. Tidak ada penambahan/penghapusan entity world di event tracking. View range render client vanilla tetap berlaku.

Cleanup mencakup penggantian teks, durasi selesai, logout, mati, teleport, ganti world, hilang visibility/tracking, dan plugin disable. Teleport juga membuang teks pemain lain yang sebelumnya dilihat viewer sehingga pairing ulang tidak bergantung pada state audience lama. Remove visual tetap dicoba walaupun paket pemulihan daftar penumpang gagal. Tidak ada passenger teks sungguhan yang bisa memblokir teleport atau mengganti penumpang carry.

Jika native packet API tidak cocok, fitur menampilkan alasan pada log dan memberi feedback kegagalan `/me`/`/do`. Tidak ada fallback ke transport teleport yang kembali menimbulkan dua sumber posisi. Fitur lain tetap memakai lifecycle dan permission yang sudah ada.

## Bukti API dan pengujian

API `RegionAccessor.createEntity`, TextDisplay, interpolation, dan event track/untrack diperiksa dari artifact source `paper-api:1.21.4-R0.1-SNAPSHOT` yang dipakai build. Bentuk packet/metadata/native attachment diperiksa terhadap mapping dan bytecode client resmi Mojang 1.21.4, bukan diasumsikan dari versi lain:

- [Metadata versi resmi](https://piston-meta.mojang.com/v1/packages/ee8ab0f6ec89b3127436d35e36aa766c1e85c3a9/1.21.4.json)
- [Mapping client resmi](https://piston-data.mojang.com/v1/objects/0cf2a0b7f056da1a5a5dd99fc6dc752f33987150/client.txt)
- [JAR client resmi yang diinspeksi dengan javap](https://piston-data.mojang.com/v1/objects/a7e5a6024bfd3cd614625aa05629adf760020304/client.jar), SHA-1 `a7e5a6024bfd3cd614625aa05629adf760020304`.

Tidak ada client Minecraft yang dijalankan dalam pemeriksaan tersebut. Bytecode yang ditelusuri mencakup `Entity.positionRider`, kedua metode attachment, `DisplayRenderer.TextDisplayRenderer.renderInner`, dan signature packet spawn/metadata/passenger/remove.

Pengujian otomatis terfokus: 21 tes lulus pada `RoleplayTextLayoutTest` (8), `RoleplayConfigurationTest` (6), `RoleplayTextTest` (4), dan `CarryPairsTest` (3). Pengujian memeriksa posisi dasar pada tinggi pose/scale berbeda, kompensasi native attachment sekali saja, perubahan ketinggian gerak/lompat tanpa offset tambahan, nilai konfigurasi invalid/reload/kompatibilitas konstruktor, urutan penumpang carry, cleanup memakai penumpang terkini, dan tidak memutasi array penumpang sumber. Ini merupakan pengujian logika, **bukan bukti kelancaran visual client atau eksekusi reflection pada server**.

Verifikasi langsung tetap diperlukan dengan pemain sumber dan client pengamat: diam/jalan/sprint/lompat/sneak/crawl, perubahan scale bila dipakai server, teks pendek dan wrapping, view pitch dari atas/bawah, carry dengan kedua pihak mengirim teks, mount/dismount saat teks aktif, keluar/masuk tracking, vanish/hide/show, teleport world sama/lintas world, kematian/logout/relog/disable. Periksa jarak dasar teks dan gerak pada FPS/ping nyata. Resource-pack font, client mod, render distance, TPS/ping dan cara client merender pose dapat memengaruhi hasil. Build sukses tidak membuktikan teks sudah mulus pada client tersebut.
