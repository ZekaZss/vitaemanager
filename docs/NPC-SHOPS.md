# Toko NPC Viti

Toko menjadi bagian modul NPC VitaeManager pada Paper 1.21.4/Java 21. Saldo, historical peak, penulisan ekonomi, dan receipt menggunakan `VitiService`/`VitiLedger` yang sama dengan `/viti`. Tidak ada Vault, Shopkeepers, ItemsAdder, atau plugin NPC tambahan yang diwajibkan oleh fitur toko ini. Integrasi NPC lama tetap mengikuti kebutuhan renderer NPC yang dipilih.

## Setup admin

Permission tetap `vitae.admin.npc`. Tidak ada command pemain untuk membuka toko dari jauh.

1. `/vnpc create viti_panen Pedagang Panen`
2. `/vnpc viti_panen shop template 1`
3. `/vnpc viti_panen on`
4. Pemain klik kiri atau kanan NPC. Admin jongkok + klik kanan NPC untuk membuka editor, atau memakai `/vnpc viti_panen shop edit`.

Template hanya boleh mengisi katalog kosong: `1` panen/ikan untuk dijual pemain, `2` barang untuk dibeli pemain, `3` enchanted book, `4` Splash Potion of Luck biasa. Nominal template mengikuti Viti asli, termasuk Mending/Swift Sneak 5.000 Viti dan potion Luck 1.600 Viti; tidak ada konversi mata uang.

Editor mempunyai 45 slot produk. Klik slot tujuan, kemudian klik contoh item pada inventory bawah. Contoh **disalin**, bukan diambil. Ikon katalog berjumlah satu; contoh lengkap memakai serialisasi item Paper, termasuk custom metadata, potion dan stored enchantment buku. Tombol mode berputar antara `SELL` (pemain menjual), `BUY` (pemain membeli), `BOTH`. Tombol kecocokan memilih metadata lengkap (`isSimilar`) atau material saja. Shift+klik kanan produk menghapusnya dari draft. SIMPAN membuka konfirmasi; BUANG dan KEMBALI kembali tanpa menyimpan. Draft memakai revisi katalog: perubahan admin lain menyebabkan penolakan simpan dan tidak ditimpa.

Harga menggunakan indeks produk 1–45, jumlah dasar dan nominal Indonesia:

```
/vnpc viti_panen shop price 1 sell 64 85
/vnpc viti_panen shop price 1 buy 64 100
```

Contoh pertama menghasilkan tepat 2,65625 Viti untuk dua carrot. Harga per satuan harus merupakan desimal berhingga yang bisa disimpan persis; misalnya harga 1 Viti untuk 3 item ditolak dengan alasan jelas, bukan dibulatkan. Sintaks nominal mengikuti parser ekonomi lama (pemisah ribuan titik, desimal koma, maksimal dua digit pada nominal dasar); pecahan hasil pembagian tidak dibulatkan menjadi dua digit.

Pemain memilih JUAL/BELI, jumlah satuan, ±64 atau MAKSIMAL, melihat total lengkap, lalu konfirmasi. Seluruh inventory harus cukup sebelum item dimasukkan; tidak ada item sisa yang dijatuhkan. Mode, katalog/revisi, harga, saldo, kecocokan item, pengaruh sihir yang membatasi ability, dan status transaksi diperiksa lagi tepat sebelum perubahan. Stok admin tidak terbatas dan tidak mempunyai kas terpisah. Hasil jual langsung masuk saldo, tanpa kertas Viti.

Nama, skin, move, on/off dan create memakai command NPC lama. `/vnpc viti_panen shop copy toko_baru` atau `/vnpc viti_panen copy toko_baru` menyalin penampilan dan katalog pada lokasi admin sebagai NPC OFF dengan UUID generasi baru. Salinan tidak membawa quest, gate, hadiah, ingatan atau progres. Katalog baru dapat diedit sendiri. Penghapusan NPC menghapus katalog generasinya; ID yang digunakan ulang tidak akan mendapatkan katalog lama. NPC dengan transaksi belum selesai tidak boleh dihapus/diedit/reload sebelum transaksi diperiksa.

## File dan pemulihan

Semua lokasi berikut relatif terhadap folder data aktual plugin:

```
npc/shops/catalogs.json       katalog per UUID generasi NPC + revisi
npc/shops/journal/<UUID>.json receipt, fase dan snapshot inventory sebelum/sesudah
npc/shops/audit/<UUID>.json   hasil akhir COMPLETE atau ABORTED
```

Saldo serta receipt uang tetap berada pada `viti.yml`. Journal lama Shopkeepers `viti-shop-spent.log` tetap pada lokasi ekonominya. Toko baru tidak melakukan migrasi/konversi nominal dan tidak memindahkan database. Katalog/journal baru ditulis melalui file sementara, force, dan penggantian atomik. Receipt terminal tetap disimpan; pemrosesan ulang memakai ID yang sama, bukan receipt baru.

Urutan transaksi:

1. Simpan marker preflight pada PDC pemain menggunakan `Player.saveData`, flush file playerdata dan **baca kembali NBT file native**. Bandingkan marker unik dan seluruh item storage slot 0–35 dengan snapshot `ItemStack.serializeAsBytes`, termasuk component metadata. Hanya `Slot` dan `DataVersion` level item dinormalisasi. Jika format/lokasi/simpan native tidak terbukti cocok, tolak sebelum item berubah.
2. Simpan receipt `PREPARED` dengan snapshot lengkap sebelum/sesudah dan kedua marker. Tahan akun pada layanan ekonomi resmi dan interaksi inventory pemain untuk mencegah transaksi bersamaan.
3. Ubah inventory, pasang marker receipt, simpan native, flush dan bandingkan kembali NBT. Hanya setelah bukti cocok, simpan fase `INVENTORY_SAVED`.
4. Kredit/debit saldo resmi dan receipt secara atomik pada ledger. Pengulangan ID receipt tidak mengubah saldo lagi. Setelah ledger berhasil, simpan audit dan fase `COMPLETE`, lalu lepaskan hold.

Transfer (kedua pihak), admin add/set/remove, convert, redeem dan hadiah NPC pada akun yang masih di-hold mengembalikan BUSY. `/viti reload` ditahan selama ada kewajiban transaksi NPC agar perubahan file manual tidak melangkahi kewajiban itu. Hold dipulihkan dari journal pada inisialisasi ekonomi **sebelum** renderer/definisi NPC dimuat. Journal yang rusak membuat perubahan saldo dijeda; saldo masih tersedia untuk dibaca, dan data tidak dikosongkan.

Setelah restart tidak ada perubahan inventory yang ditebak atau dijalankan ulang. Jika ledger sudah memiliki receipt, penyelesaian hanya memperbaiki audit tanpa mengulangi uang/item. Jika ledger belum memiliki receipt, pemulihan memverifikasi lagi marker dan inventory native sesudah transaksi. `PREPARED` yang terbukti masih memiliki marker+snapshot sebelum transaksi (dan belum ada receipt ledger) boleh menjadi `ABORTED` tanpa perubahan saldo. Semua keadaan lain menjadi `QUARANTINED`; data/evidence dipertahankan dan akun/inventory tetap ditahan untuk pemeriksaan admin. Hold tidak hilang hanya dengan relog/reload.

```
/vnpc viti_panen shop status
/vnpc viti_panen shop retry <receipt-uuid>
```

`retry` memverifikasi bukti, bukan memaksa hasil. Receipt ledger yang sudah selesai dapat diselesaikan auditnya tanpa pemain online. Receipt lain membutuhkan pemilik online dan bukti native yang cocok. Jika bukti tetap bertentangan, hentikan aktivitas pemain terkait dan periksa snapshot journal, `viti.yml`, playerdata, serta backup yang berasal dari titik waktu sama. **Jangan menghapus journal/receipt, mengubah fase manual, atau menebak transaksi sukses/gagal.** Pemulihan state eksternal yang ambigu memerlukan keputusan admin berdasarkan bukti/backup; plugin sengaja tidak membuat payout/rollback otomatis.

Verifier native mengasumsikan playerdata standar Paper pada folder `playerdata` world utama (`Bukkit.getWorlds().getFirst()`). Plugin inventory per-world, plugin yang mengganti penyimpanan pemain, atau modifikasi NBT di luar mekanisme standar dapat ditolak sebelum transaksi, atau dikarantina bila berubah setelah transaksi. Parser mempunyai batas ukuran/kedalaman. World/player APIs dan save native tetap pada server thread; penulisan ledger memakai writer ekonomi lama. Save/read/flush native dapat memblokir sementara; uji latensi pada hardware server privat.

## Verifikasi

Pengujian otomatis mencakup pecahan tepat, penolakan pecahan berulang, optimistic concurrency, receipt debit/kredit idempotent, insufficient funds, katalog copy/generasi terpisah, journal restart/terminal audit, file rusak tetap dipertahankan, marker native, jumlah item, metadata item, NBT rusak dan batas alokasi. Fixture NBT tidak menggantikan server Paper sebenarnya.

Uji langsung yang masih wajib: klik kiri/kanan NPC semua renderer yang dipakai; shift+klik kanan editor; pengambilan/drag/shift/hotbar/creative GUI; metadata buku/potion/custom item; inventory penuh dan saldo/item kurang; perubahan harga/admin simultan; penggunaan sihir mengunci queued konfirmasi; crash pada setiap fase; kematian/logout saat ledger pending; native save gagal/disabled; restart berulang ketika pemulihan; pemeriksaan bahwa item dan saldo tepat tanpa duplikasi. Tidak ada klaim perilaku dalam game berhasil hanya berdasarkan build/unit tests.
