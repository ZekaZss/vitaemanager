# Tunduk: pose internal Paper 1.21.4

Dokumen ini menggantikan keterangan lama yang menjadikan GSit syarat aktivasi Tunduk. Tunduk sekarang memakai implementasi internal pada JAR VitaeManager yang sama, tanpa dependency tambahan, tanpa blok pembantu di atas kepala, dan tanpa damage.

`InternalCrawlPose` memakai API Paper `Player.setPose(Pose.SWIMMING, true)` bersama state native `setSwimming(true)`. Ini mengubah pose entity server, bukan hanya mengirim metadata kosmetik. Sesudah diterapkan, implementasi memeriksa fixed pose, swimming state dan bounding box server: tinggi crawl harus mendekati lebarnya (sekitar 0,6 blok pada skala normal). Jika server tidak menerapkan hitbox yang sesuai, aktivasi gagal dan keadaan sebelumnya dikembalikan. Flag swimming membantu sinkronisasi state ke client; input berhenti berenang tidak menghapus lock.

API dan implementasi yang diperiksa:

- [Paper 1.21.4 Entity#setPose dan hasFixedPose](https://jd.papermc.io/paper/1.21.4/org/bukkit/entity/Entity.html#setPose(org.bukkit.entity.Pose,boolean)): pose memengaruhi hitbox; parameter fixed mempertahankan pose sampai diubah manual.
- [CraftEntity pada branch ver/1.21.4](https://github.com/PaperMC/Paper/blob/ver/1.21.4/paper-server/src/main/java/org/bukkit/craftbukkit/entity/CraftEntity.java): API memanggil pose entity asli dan mengatur fixedPose.
- [Patch entity Paper 1.21.4](https://github.com/PaperMC/Paper/blob/ver/1.21.4/paper-server/patches/sources/net/minecraft/world/entity/Entity.java.patch): perubahan pose native diabaikan selama fixedPose aktif.

Semua operasi entity dilakukan pada thread server melalui lifecycle MagicService yang sudah tersedia. Penguncian posisi tetap terpisah dari pose; korban tetap dapat menoleh, tetapi tidak bergerak, menyerang, menggunakan item atau mengaktifkan sihir/ability pada jalur project yang telah diintegrasikan. Radius20, durasi20detik, pengecualian owner, cooldown bersama dan penyimpanan kepemilikan tidak berubah. GSit yang tidak dipasang tidak menghalangi Tunduk. Bila GSit tersedia, pemeriksaan pasif tetap menghindari mengambil alih crawl GSit yang sudah aktif. Adapter GSit tidak lagi memulai/menghentikan sesi baru.

## Kepemilikan dan pemulihan

Sebelum mengubah pemain, journal `magic/recovery/players.properties` mendapat record `tunduk_pose` dengan versi, token lease, UUID owner penggunaan, pose sebelumnya dan swimming sebelumnya. PDC pemain `vitaemanager:magic_tunduk_pose` memuat token yang sama. Write-ahead harus berhasil sebelum pose atau PDC diubah.

Target dengan fixed pose yang sudah dimiliki sistem lain ditolak sebelum perubahan. Cleanup hanya mengembalikan state bila token cocok dan entity masih mempunyai fixed swimming pose milik lease tersebut. Perubahan pose eksternal menyerahkan kepemilikan pose kepada sumber baru; cleanup tidak menimpa pose baru itu, sedangkan penguncian gerak/interaksi tetap berlaku sampai Tunduk selesai. API Paper tidak menyediakan identitas penulis untuk penulisan ulang pose dan fixed flag yang persis sama tanpa pose-change event; koordinasi plugin lain yang memaksa pose identik tetap perlu diuji pada server sebenarnya.

Penggunaan normal, penghentian admin, owner mati/logout, korban mati/logout, pergantian world dan disable plugin melewati cleanup yang sama. Membersihkan satu korban tidak menghentikan korban lain. Ketika mati, fixed pose dilepas ke DYING, bukan memaksakan STANDING di atas pose kematian. Keadaan berenang alami sebelum aktivasi dikembalikan bila lease masih dimiliki.

Startup/join tidak melanjutkan Tunduk. Jika record dan token cocok serta fixed swimming pose masih aktif (misalnya lifecycle plugin terputus), keadaan sebelumnya dipulihkan. Field fixedPose Paper bersifat transient; jika setelah restart penuh entity sudah tidak fixed, pose native yang baru tidak ditimpa. Bukti dengan token/pose yang bertentangan dipertahankan dan dilaporkan, bukan menebak sumber pose. Record `crawl` GSit lama tetap diperlakukan sebagai bukti pemulihan lama: tidak dihapus dengan menebak kepemilikan dan tidak menjadi syarat penggunaan baru.

## Verifikasi dan batas pengujian

Dua belas pengujian otomatis `InternalCrawlPoseTest` memeriksa penggunaan/restorasi, idempotensi, fixed pose sumber lain, foreign pose takeover, recovery token cocok, restart penuh tanpa fixed pose, kegagalan hitbox dan rollback, berenang alami, death cleanup (termasuk event pose DYING sebelum death event), isolasi dua korban, token bertentangan, dan kegagalan write-ahead sebelum mutasi. Fixture Player/PDC adalah proxy: pengujian ini memverifikasi logika dan kontrak API, bukan pengganti server Minecraft.

Belum dilakukan pengujian langsung Paper/client Minecraft. Paper sendiri mencatat bahwa pose yang dilihat pemain lokal dapat berbeda dari pose entity server. Karena itu tampilan orang pertama/ketiga, kamera, hitbox aktual, kontrol menoleh, input sneak/jump, interaksi dengan GSit/WorldGuard/carry, serta death/relog/restart harus diuji memakai server Paper1.21.4 dan client yang akan digunakan. Build dan pengujian proxy tidak membuktikan tampilan crawl sudah identik pada seluruh client.
