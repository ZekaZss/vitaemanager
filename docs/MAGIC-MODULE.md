# Modul sihir VitaeManager

Modul berada di project/JAR VitaeManager yang sama. Target tetap Paper 1.21.4 dan Java 21.

## Penyimpanan

- `magic/unlimited_misery.yml`, `magic/telekinesis.yml`, `magic/tunduk.yml`: konfigurasi terpisah; dibuat hanya jika belum ada.
- `magic/data/state.properties`: satu sumber kepemilikan UUID, penggunaan aktif, dan waktu akhir cooldown absolut milidetik. Jangan diedit saat server berjalan.
- `magic/recovery/players.properties`: bukti write-ahead efek potion/crawl milik sihir; jangan dihapus untuk menghilangkan masalah pemulihan.
- `magic/recovery/domains/`: snapshot NBT, progres dan marker penyelesaian domain. Lihat juga dokumentasi pemulihan domain.

Penyimpanan memakai temporary file, force, lalu atomic replacement. Temporary file terputus tidak menggantikan file otoritatif. Kepemilikan tidak dihapus saat reload. Cooldown bersama 300 detik baru dimulai setelah cleanup/pemulihan selesai. Waktu offline/server berhenti tetap mengurangi cooldown yang telah dimulai. Domain yang masih dipulihkan tidak dilanjutkan; owner tetap terkunci sampai pemulihan selesai. Selama pemulihan startup yang belum selesai, aktivasi baru ditolak.

Validasi/snapshot awal domain merupakan reservasi, belum penggunaan. Perintah/klik berulang tidak dapat melewati reservasi; penolakan sebelum pembentukan dimulai tidak menghasilkan cooldown.

## Command dan izin

Semua pemain dapat `/sihir`, `/sihir info <id>`, `/sihir pakai <id>`. Tidak ada `/sihir akses` dan tidak ada command membuka toko NPC jarak jauh di modul ini.

Admin dengan `vitae.sihir.admin` dapat:

- `/sihir beri <player> <id>`
- `/sihir hapus <player> <id>`
- `/sihir cek <player>`
- `/sihir hentikan <player>`
- `/sihir cooldown reset <player>`
- `/sihir reload`

ID awal: `unlimited_misery`, `telekinesis`, `tunduk`. Nama admin target dicocokkan dengan identitas offline/online yang sudah dikenali server. Tidak membuat UUID dari nama baru. Maksimum sembilan ID berbeda; duplikat ditolak. Reset cooldown tidak menghapus penggunaan/reservasi aktif. GUI koleksi/konfirmasi masing-masing sembilan slot; konfirmasi BATAL=2, informasi=4, GUNAKAN=6 (indeks nol). Command pakai dan tombol memakai jalur validasi yang sama.

## Integrasi

LuckPerms bersifat opsional untuk plugin, tetapi diperlukan Unlimited Misery. Tunduk memakai pose native Paper 1.21.4 dan tidak memerlukan GSit. Deklarasi integrasi GSit lama tetap dipertahankan. Tidak memerlukan MythicMobs/MMOItems/MythicLib/ItemsAdder/TAB.

Unlimited Misery memeriksa `User.getQueryOptions()` dan `getInheritedGroups(...)` pada API LuckPerms (tersedia sejak 5.1), termasuk pewarisan dan konteks aktif. OP/wildcard permission bukan bukti grup antihero.

Tunduk memakai `setSwimming(true)` dan `setPose(Pose.SWIMMING, true)`, memeriksa bounding box aktual, serta menyimpan bukti kepemilikan pose sebelum perubahan. Cleanup hanya memulihkan pose milik penggunaan tersebut. Detail dan batas pengujian: [pose native](TUNDUK-NATIVE-POSE.md). Flag WorldGuard tidak diubah.

Jika korban sudah mempunyai crawl lain, kendaraan, atau penumpang, Telekinesis/Tunduk menolak aktivasi tanpa cooldown. Saat pembentukan domain, carry Vitae dilepas melalui lifecycle CarryService; kendaraan biasa dilepas melalui leaveVehicle. Penumpang mekanik lain yang tidak dapat dilepas secara aman membatalkan domain dengan pemulihan, bukan menghapus identitas sistem lain. Mount baru diblokir selama penguncian.

Tunduk/pembentukan domain memblokir interaksi item, inventory, projectile, penyerangan, dan aktivasi sihir; integrasi carry/evolution memeriksa guard modul. Tidak ada klaim memblokir semua command ability plugin eksternal; project tidak mempunyai integrasi API ability eksternal universal.

## Konfigurasi dan mekanik

Setiap YAML mendukung name, description, icon, radius, duration-seconds, particles.count, dan sounds.<activation|formation|active|release|ending> dengan enabled/type/id/volume/pitch/interval-ticks, audience/origin/range, serta daftar cues opsional. `type: custom` memutar ID resource pack langsung, misalnya `my_pack:pressure`; resource pack harus sudah tersedia pada client. ItemsAdder tidak diperlukan hanya untuk ID sound. Default memakai vanilla.

Pesan mulai/akhir dapat diatur dengan `messages.started` (mendukung `{name}`) dan `messages.ended`. Reload memvalidasi konfigurasi baru sebelum mengganti referensi konfigurasi. Penggunaan aktif mempertahankan konfigurasi awal. Batas finite/rentang mencegah kecepatan naik tidak aman, NaN, level/interval negatif atau jumlah partikel berlebihan.

Unlimited Misery default radius30, pembentukan400tick, aktif300detik, pemulihan400tick. GUI menampilkan total340detik. Selama pembentukan korban dikunci, owner/antihero dikecualikan. Pelepasan menunggu shell benar-benar dibangun. Judul memakai Adventure gradient merah ke putih pucat, tanpa subtitle. Fase aktif memberikan Resistance II/Fire Resistance II pada owner/antihero; owner dalam domain menerima Instant Health X tiap20tick. Korban default Slowness III, Darkness I, Nausea I, pengali damage0.5. `healing.level` (1..20; mencegah overflow instant-heal), `healing.interval-ticks`, `debuff.slowness-level`, `debuff.damage-multiplier` tersedia. Pengurangan damage memakai Paper DamageSource causing/direct entity dan fallback shooter projectile pemain; damage lingkungan/tidak teratribusi dan API damage ability eksternal memerlukan pemeriksaan game tambahan. Buff/debuff dihentikan saat pemulihan mulai; efek keluar area dievaluasi tiap20tick.

Telekinesis default radius20, total30detik, naik0.05blok/tick sampai maksimum7blok. `maximum-rise` dibatasi0..7 dan `rise-per-tick` 0.001..0.25. Pemeriksaan bounding box tiap langkah mencegah menembus plafon. Setelah selesai korban dilepas tanpa Slow Falling, bonus pendaratan atau reset fall distance lanjutan. Hanya ender pearl yang diblokir khusus; menyerang dan item lain tetap diizinkan. Partikel DUST hitam/merah mengikuti tubuh. Camera shake memakai Paper Player.playHurtAnimation tanpa damage asli (interval konfigurasi camera.interval-ticks, default10); ini animasi hurt yang didukung, bukan rotasi kamera arbitrer. Kepadatan visual bergantung pada pengaturan client. Suara kelelawar tiap50tick default, bukan setiaptick.

Tunduk default radius20 dan20detik, pose crawl native dengan hitbox pendek, posisi terkunci tetapi yaw/pitch tetap bebas. Perubahan pose dari sumber lain tidak menghapus lock; cleanup tidak menimpa pose baru milik sumber tersebut. GSit tidak menjadi syarat aktivasi.

Efek potion mencatat efek sebelumnya sebelum diganti dan mengembalikannya dengan durasi yang tersisa. Perubahan potion eksternal membatalkan kepemilikan lease sehingga cleanup tidak menghapus efek baru itu. Journal pemain menyimpan fingerprint lease pendek ambient tanpa partikel; startup/join hanya membersihkan fingerprint yang sesuai. Durasi efek sebelumnya dicheckpoint tiap20tick; setelah crash pemulihan durasi dapat berbeda sampai sekitar satu detik. Crawl GSit tidak dilanjutkan setelah restart; jika API melaporkan crawl yang sudah aktif saat startup, sumbernya tidak ditebak dan bukti dipertahankan dengan pesan log agar admin dapat memeriksanya.

Tambahan mekanik memerlukan implementasi Java `SpellMechanic` (validate/begin/tick/cleanup), registrasi sebelum start dan resource konfigurasi sendiri. YAML tidak membuat mekanik baru secara otomatis. Lifecycle/ownership/cooldown tetap dimiliki MagicService.

## Pengujian

Pengujian otomatis modul: 6 MagicStateTest + 4 VictimJournalTest + 3 SpellConfigTest, meliputi batas/duplikat/durabilitas kepemilikan, cooldown restart/idempotensi/reset, aktivasi batal, temporary write terputus, checkpoint, isolasi korban, file rusak dipertahankan, parameter gerak/healing/interval aman dan sintaks sound custom. Seluruh suite project juga telah dijalankan sesudah integrasi; laporan build akhir memuat jumlah keseluruhan.

Belum ada pengujian client/server Minecraft pada sesi ini. Sebelum produksi uji minimal: sphere radius30 di lingkungan/container berbeda; restart/crash setiap fase; pemulihan terputus; peran owner/korban mati/logout; inheritance/context LuckPerms; pose native/WorldGuard; entity mounts/carry; plafon Telekinesis dan fall damage; efek potion dari sumber lain; pergerakan/yaw crawl; semua jalur ability yang benar-benar digunakan server; resource-pack sound; partikel/title/performa dengan banyak pemain. Build sukses tidak membuktikan perilaku visual atau crash world-save di dalam game.


Pembaruan terbaru, format sound dan checklist server: [pose, terrain dan audio](POSE-TERRAIN-AUDIO-UPDATE.md).
