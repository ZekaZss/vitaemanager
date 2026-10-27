# Update Viti dan toko NPC

Target: Vitaemanager pada Paper 1.21.4, Java 21. Harga memakai mata uang Viti baru: 1.000 Viti lama = 1 Viti baru. Nilai pecahan disimpan persis; tampilan minimal dua desimal dan menampilkan angka tambahan bila diperlukan. Masukkan harga command dengan koma untuk desimal: `0,085`. Tidak memakai kurs USD asli.

## Pasang

1. Stop server hingga selesai. Simpan backup seluruh folder `plugins/vitaemanager` dan JAR lama.
2. Ganti JAR lama dengan JAR update; sisakan hanya satu JAR Vitaemanager. Pertahankan folder data lama.
3. Nyalakan server. Startup pertama menyiapkan backup `currency-v2-backup`, mengonversi saldo/rekor saldo, nominal kertas yang tercatat, giveaway, hadiah dialog dan quest (termasuk farm), serta hadiah tertunda. Area, identitas NPC, progres dan konfigurasi permainan tetap dipakai.
4. Periksa console, saldo satu pemain dan hadiah farm. Migrasi tidak berulang saat restart. Jangan menghapus folder `currency-v2-backup`; jika rollback, pulihkan seluruh backup data dan JAR secara bersamaan, bukan hanya viti.yml.
5. Shopkeepers lama tidak otomatis diimpor ke toko NPC. Sampel harga Viti lama yang dikenali dinormalisasi ke unit baru saat toko lama dibuka; kertas lama perlu dicairkan dan dikonversi lagi agar tampilan item sesuai format baru. Sesuai rencana, hapus toko villager lama setelah toko NPC penggantinya diperiksa. Jangan membuat output Shopkeepers berupa salinan kertas Viti berseri: salinan itu bukan uang baru yang diterbitkan ledger.

Untuk source IntelliJ: letakkan `install-viti-npc-shops.ps1` sejajar `gradlew.bat`, lalu jalankan:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\install-viti-npc-shops.ps1 -CheckOnly
powershell -NoProfile -ExecutionPolicy Bypass -File .\install-viti-npc-shops.ps1
.\gradlew.bat clean build
```

Installer membuat backup source dan menolak menimpa source yang berbeda dari versi yang dikenali. Gunakan JAR yang disertakan jika hanya memasang ke server; source installer untuk menyelaraskan proyek coding. Update ini tidak dipush ke GitHub otomatis.

## Empat template

Berdiri di dekat lokasi toko yang diinginkan. Jalankan perintah satu per satu. NPC baru dibuat dua blok di depan admin.

```text
/vnpc create viti_panen &aPedagang Hasil Panen
/vnpc viti_panen shop template 1
/vnpc viti_panen on

/vnpc create viti_perlengkapan &ePedagang Perlengkapan
/vnpc viti_perlengkapan shop template 2
/vnpc viti_perlengkapan on

/vnpc create viti_enchant &bPedagang Buku Enchant
/vnpc viti_enchant shop template 3
/vnpc viti_enchant on

/vnpc create viti_ramuan &dPedagang Ramuan
/vnpc viti_ramuan shop template 4
/vnpc viti_ramuan on
```

Buat masing-masing NPC sambil berdiri di lokasinya. Template hanya dapat diterapkan pada toko kosong; tidak menimpa katalog yang sudah berisi produk. Perintah template sudah menggunakan harga Viti baru; jangan membagi harga itu lagi.

### NPC 1: pemain menjual ke toko

| Slot | Item | Jumlah dasar | Harga baru |
|---|---|---:|---:|
| 1 | Carrot | 64 | 0,085 Viti |
| 2 | Wheat | 64 | 0,085 Viti |
| 3 | Rotten flesh | 32 | 0,075 Viti |
| 4 | Hay bale | 16 | 0,06 Viti |
| 5 | Bread | 20 | 0,04 Viti |
| 6 | Raw cod | 1 | 0,01 Viti |
| 7 | Raw salmon | 1 | 0,01 Viti |
| 8 | Tropical fish | 1 | 0,01 Viti |
| 9 | Pufferfish | 1 | 0,01 Viti |

Harga ikan ditafsirkan per satu ikan. Template memakai item polos dengan kecocokan metadata lengkap agar item custom yang kebetulan berbahan sama tidak ikut terjual. Ubah kecocokan dari editor jika memang ingin menerima semua item dengan material itu.

### NPC 2: pemain membeli dari toko

| Slot | Item | Jumlah dasar | Harga baru |
|---|---|---:|---:|
| 1 | Golden apple biasa | 16 | 0,10 Viti |
| 2 | Ender pearl | 16 | 0,085 Viti |
| 3 | Golden carrot | 64 | 0,05 Viti |
| 4 | Bottle o' enchanting | 64 | 0,035 Viti |

### NPC 3: pemain membeli satu buku

| Slot | Enchant | Harga baru |
|---|---|---:|
| 1 | Mending I | 5,00 Viti |
| 2 | Swift Sneak III | 5,00 Viti |
| 3 | Unbreaking III | 0,035 Viti |
| 4 | Fire Aspect II | 0,035 Viti |
| 5 | Looting III | 0,035 Viti |
| 6 | Fortune III | 0,035 Viti |
| 7 | Efficiency V | 0,035 Viti |
| 8 | Feather Falling IV | 0,035 Viti |
| 9 | Infinity I | 0,035 Viti |
| 10 | Knockback II | 0,035 Viti |
| 11 | Power V | 0,035 Viti |
| 12 | Protection IV | 0,035 Viti |
| 13 | Sharpness V | 0,035 Viti |
| 14 | Silk Touch I | 0,035 Viti |
| 15 | Sweeping Edge III | 0,035 Viti |

Setiap buku hanya mempunyai satu enchant di atas. Metadata buku disimpan dan disalin lengkap; buku tidak dipaksa menjadi stack 64.

### NPC 4: pemain membeli ramuan

Slot 1: satu Splash Potion of Luck standar Minecraft = **1,60 Viti**. Bila ramuan lama adalah item custom atau punya durasi/amplifier khusus, ganti contoh item template ini di editor dengan item asli tersebut, kemudian atur harga kembali.

## Alur pemain

Klik kiri/kanan NPC -> pilih produk -> pilih Jual atau Beli -> atur −1/+1, −16/+16, atau Maksimal -> lihat jumlah dan total -> Konfirmasi. Penjualan menghitung item yang cocok dalam storage inventory (bukan armor atau off-hand), kemudian menambah saldo langsung. Pembelian memotong saldo dan memasukkan item langsung; inventory penuh menolak transaksi. Tombol Maksimal untuk membeli mempertimbangkan saldo dan kapasitas item asli. Tidak ada command player untuk membuka toko dari jauh.

Stok toko admin tidak terbatas dan tidak memerlukan kas. Konversi 1000:1 menjaga nilai relatif ekonomi; angka lebih kecil sendiri tidak mengurangi inflasi dari pemasukan farm.

## Editor admin

Jongkok + klik kanan NPC, atau `/vnpc viti_panen shop edit` (khusus admin). Pilih item asli dari inventory ke cursor, klik slot atas untuk menyalin contoh, lalu kembalikan item asli ke inventory. Shift-klik item di inventory juga menyalin ke slot kosong pertama. Item admin tidak dikonsumsi; ikon produk selalu satu item.

Klik produk dengan cursor kosong untuk memilih. Tombol bawah memilih mode Pemain Jual / Pemain Beli / Keduanya, mengubah kecocokan exact/material, menghapus produk terpilih, atau memberi contoh command harga. Nomor slot pada command dihitung mulai 1. Menutup editor yang berubah menampilkan Simpan / Buang / Kembali. Menutup menu pilihan lagi memungkinkan mengetik command; draft tetap ada sampai disimpan, dibuang, atau admin logout. Logout membuang draft, bukan item asli admin.

Contoh harga baru:

```text
/vnpc viti_panen shop price 1 sell 64 0,085
/vnpc viti_perlengkapan shop price 1 buy 16 0,10
/vnpc viti_enchant shop price 1 buy 1 5,00
```

Jika ada draft, command harga mengubah draft. Buka editor lagi dan tekan **Konfirmasi & simpan**. Bila tidak ada draft, command harga menyimpan langsung. Angka harga adalah total untuk jumlah dasar pada command, bukan selalu harga per satu item. Harga satuan yang menghasilkan desimal berulang ditolak; gunakan jumlah dasar/harga yang dapat dibagi persis atau atur per satu item. Nol tidak dipakai untuk transaksi gratis; produk yang belum diberi harga belum dapat ditransaksikan.

Tutup/buka toko:

```text
/vnpc viti_panen shop off
/vnpc viti_panen shop on
```

Perubahan katalog dicek kembali saat konfirmasi pemain. Editor admin lain yang sudah kedaluwarsa ditolak saat menyimpan agar tidak menimpa perubahan baru.

## Pindah dan copy NPC

Berdiri di posisi baru:

```text
/vnpc viti_panen move
/vnpc viti_panen on
```

Untuk salinan independen, berdiri di lokasi salinannya:

```text
/vnpc viti_panen copy viti_panen_cabang
/vnpc viti_panen_cabang on
```

Skin/model dan katalog ikut disalin. ID/token salinan baru dan katalog berdiri sendiri. Copy ini hanya untuk NPC toko tersendiri, bukan NPC yang sekaligus mempunyai quest/hadiah/gerbang. NPC lama lain tetap menggunakan perintah yang sudah ada.

## Jika transaksi terputus

Transaksi mempunyai intent di `shop-transactions/<UUID>.json`, receipt inventory di playerdata, dan receipt saldo di ledger. Plugin membaca ulang receipt dari file playerdata sebelum memproses saldo; bila penyimpanan playerdata dimatikan, transaksi ditolak sebelum mengubah inventory. Pemain yang masih mempunyai transaksi tertunda tidak bisa menjalankan transaksi uang lain sampai diselesaikan. Saldo tidak diproses ulang untuk receipt yang sama. Audit tersimpan di `shop-transactions/audit.jsonl` dan journal per transaksi.

Admin:

```text
/vnpc viti_panen shop pending
```

Jika transaksi tidak bisa dipulihkan otomatis, cocokkan UUID, jumlah, arah, saldo, dan inventory pemain dengan journal. **complete** hanya setelah memastikan perubahan item memang sudah terjadi dan tersimpan; **cancel** hanya setelah memastikan perubahan item belum terjadi atau sudah dikoreksi manual. Keduanya tidak mengulangi pemberian/pengambilan item:

```text
/vnpc viti_panen shop recover UUID_RECEIPT complete
/vnpc viti_panen shop recover UUID_RECEIPT cancel
```

Jangan menebak keputusan dari pesan pemain saja. Jangan hapus receipt/journal untuk mencoba ulang: itu merusak pelacakan transaksi.

## Pengujian setelah dipasang

- Lihat saldo lama setelah konversi dan klaim satu hadiah farm; restart dan pastikan tidak terbagi lagi.
- Jual 1 carrot, 2 carrot, 64 carrot dan Maksimal dari beberapa stack. Periksa inventory dan saldo.
- Beli satu buku Mending/Swift Sneak; periksa enchant, level dan kapasitas inventory.
- Coba pembelian saat saldo kurang/inventory penuh dan klik konfirmasi berulang.
- Uji editor dengan item custom, mode keduanya, command harga, tutup/buang draft dan dua admin bersamaan.
- Pindah/copy NPC dan pastikan harga salinan dapat diedit sendiri.
- Uji `/me` dan `/do` dari client lain saat berjalan, sprint, lompat, jongkok, ganti chunk, teleport, vanish dan carry. Client-only text tidak menjadikan player kendaraan sungguhan. Verifikasi visual langsung diperlukan; unit test tidak membuktikan tampilan di client.
