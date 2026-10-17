# Update Evo Vitaemanager — Paper 1.21.4 / Java 21

Dasar source: [ZekaZss/vitaemanager](https://github.com/ZekaZss/vitaemanager/tree/2746e4c611be4004fa11e1d6b3666971d580b94c), commit `2746e4c611be4004fa11e1d6b3666971d580b94c`.

Simpan file ini sebagai **update-evo.md**, sejajar `gradlew.bat` di folder proyek IntelliJ. Jalankan dari PowerShell di folder itu.

## Pasang

```powershell
$m=Get-Content -LiteralPath .\update-evo.md -Raw -Encoding UTF8; $i=[regex]::Match($m,'(?s)<!-- VITAE_INSTALLER_BEGIN -->\r?\n```powershell\r?\n(.*?)\r?\n```\r?\n<!-- VITAE_INSTALLER_END -->'); if(!$i.Success){throw 'Blok installer tidak ditemukan'}; & ([scriptblock]::Create($i.Groups[1].Value)) -ProjectPath (Get-Location).Path
.\gradlew.bat clean build
```

Installer membuat backup source lama otomatis, lalu memasang kode lengkap. `Vitaemanager.java`, `plugin.yml`, Gradle, dan konfigurasi server tidak ditulis ulang. Jika source berbeda dari commit patokan, installer berhenti sebelum menimpa file. Perbedaan BOM atau CRLF Windows tetap diterima.

Untuk memeriksa saja, jalankan command pertama dengan tambahan `-CheckOnly` di ujung:

```powershell
$m=Get-Content -LiteralPath .\update-evo.md -Raw -Encoding UTF8; $i=[regex]::Match($m,'(?s)<!-- VITAE_INSTALLER_BEGIN -->\r?\n```powershell\r?\n(.*?)\r?\n```\r?\n<!-- VITAE_INSTALLER_END -->'); if(!$i.Success){throw 'Blok installer tidak ditemukan'}; & ([scriptblock]::Create($i.Groups[1].Value)) -ProjectPath (Get-Location).Path -CheckOnly
```

Setelah build sukses, ganti JAR server dengan hasil dari `build/libs`, lalu restart server. Jalankan build dari proyek Java 21.

## Yang berubah

- GUI crafting native 3×3. Delapan slot bahan mengelilingi senjata di tengah; hasil kanan hanya preview terkunci. Taruh item nyata dari cursor ke slot bahan yang cocok; klik kanan menyetor satu item, klik kiri sebanyak kebutuhan. Drag stack bahan juga ditangani. Slot penuh menjadi kaca hijau. Untuk jumlah yang dibutuhkan, klik slot dengan cursor kosong.
- Bahan langsung masuk setoran persisten. Keluar GUI untuk melihat display, lalu buka kembali untuk melanjutkan. Senjata terakhir memulai animasi dan menutup GUI otomatis. `/evo cancel` mengembalikan setoran ke inventory, sisanya jatuh dengan perlindungan pemilik.
- Musik tersendiri pada kategori Minecraft **Jukebox/Note Blocks (RECORDS)**; efek suara tahap tetap terpisah. Musik berjalan sampai ledakan, lalu dihentikan saat selesai, batal, disconnect, pause atau shutdown. Radius public dicek selama animasi. Pemain yang baru masuk radius setelah musik dimulai tidak diputar ulang dari awal agar tidak meleset dari adegan.
- Mode `lethal` memberi damage fatal lewat event damage normal, sebelum listener totem Vitae. Armor, resistance, blocking dan absorption tidak mengurangi hit ritual tersebut. Pemain survival/adventure dalam radius terkena satu hit; creative/spectator bebas. Totem tersimpan Vitae bisa menyelamatkan pemain. Tanpa totem yang diizinkan server, pemain mati. Cancellation dari plugin proteksi/god mode tetap dihormati.
- Preset tambahan **astral**, **frost**, **phoenix**, dengan lintasan item dan pola particle berbeda. Preset lama tetap tersedia. Petir opsional hanya efek visual tanpa api atau damage tambahan; shockwave juga berupa particle.
- Hasil custom tetap jatuh di atas altar, dilindungi untuk pemilik. NBT/component item tidak diganti dengan item vanilla. Hadiah NPC yang tidak muat juga jatuh sebagai clone item asli, dengan pemilik, receipt, perlindungan damage dan tanpa despawn biasa.
- `/evo preview` melatih adegan dengan musik, title, petir dan particle, tanpa mengambil bahan, memberi hasil, mencatat penyelesaian, atau memberi damage. `/evo pause` dan `/evo resume` tersedia. Jika musik aktif, resume mengulang animasi dari awal agar musik kembali sinkron.

## Setup cepat untuk altar yang sudah ada

Ganti `altarapi` dengan ID altar milikmu. Jalankan satu per satu, dan tunggu pesan **Setup altar tersimpan** sebelum command berikutnya.

```text
/evo preset altarapi phoenix
/evo settings altarapi soundradius 24
/evo music altarapi public minecraft:music_disc.pigstep 15 1
/evo settings altarapi damageradius 9
/evo blast altarapi lethal
/evo effects altarapi lightning on 1.5 3
/evo effects altarapi shockwave on
/evo info altarapi
/evo preview altarapi
```

Contoh rekaman **9 detik** yang sudah terdaftar di resource pack ItemsAdder:

```text
/evo music altarapi public namespace:sound_kamu 9 0.8
```

`namespace:sound_kamu` adalah placeholder; gunakan sound ID asli dari config/resource pack kamu. Minecraft vanilla memiliki musik yang panjang; contoh Pigstep di atas dimainkan 15 detik dan dihentikan saat ledakan. File `.ogg` dan `sounds.json` tetap dikelola ItemsAdder/resource pack; folder plugin tidak mengimpor rekaman.

Durasi musik command sekaligus menjadi durasi ritual, 5–120 detik dengan pitch 1. Sesuaikan durasi ini dengan rekaman custom. Timing memakai tick server (20 tick/detik); lag server dapat menggeser durasi nyata. Suara juga mengikuti slider kategori RECORDS serta master volume client.

| Pengaturan | Command | Batas |
| --- | --- | --- |
| Musik public/private | `/evo music <id> public|private <sound> <detik> [volume]` | 5–120 detik, volume 0–1 |
| Matikan musik | `/evo music <id> off` | Sound tahap tetap berjalan |
| Ledakan fatal | `/evo blast <id> lethal` | Radius mengikuti `damageradius` |
| Damage biasa | `/evo blast <id> damage <jumlah>` | 0–1000, mengikuti armor biasa |
| Matikan damage | `/evo blast <id> off` | Ledakan visual tetap ada |
| Petir | `/evo effects <id> lightning on|off [jeda] [radius]` | Jeda 0.5–10 detik, radius 0–8 |
| Shockwave | `/evo effects <id> shockwave on|off` | Visual |
| Bantuan | `/evo help <command>` | Chat berwarna dengan pemisah |

## Data lama dan pemulihan

Altar, contoh item custom, target, cue dan recovery lama tetap dibaca. Altar lama mempertahankan pengaturan damage sebelumnya; aktifkan `lethal` dengan command di atas. File `evolution.yml` disimpan sebagai schema versi 2 saat perubahan data pertama ditulis. Setoran mengunci altar sampai diteruskan atau direfund; setup altar tersebut tidak dapat diubah saat masih menyimpan bahan.

Pada kegagalan penyimpanan/crash, periksa `/evo pending`. Journal setoran dan receipt pemain membantu menentukan apakah item sudah dipotong/dikembalikan/diberikan. **Recovery tidak memberi item otomatis setelah crash.** Admin harus memastikan inventory dan ground item sebelum mengambil tindakan:

```text
/evo recover NamaPlayer refund confirm
/evo recover NamaPlayer result confirm
/evo recover NamaPlayer discard confirm
```

`result` hanya untuk ritual yang sudah dimulai; setoran bahan belum menjadi hasil. `refund` memerlukan pemain online dan hidup. `discard` menutup catatan tanpa memberi item. Jangan menjalankan discard untuk setoran yang masih ingin dilanjutkan.

## Validasi

Source dibangun dengan Java 21 dan Paper API 1.21.4. Ada pengujian store/migrasi, transfer setoran, crafting, musik dan ledakan. Pengujian installer mencakup pemeriksaan tanpa tulis, backup, pemasangan ulang, CRLF/BOM, source berbeda, data server tetap utuh dan rollback. Tampilan native client, kualitas particle serta sound resource pack tetap perlu dicoba di server kamu.

## Installer dan kode lengkap

Blok berikut adalah installer yang dijalankan command di atas. Seluruh source file yang diubah atau ditambahkan tersimpan lengkap di dalamnya; tidak memerlukan unduhan tambahan saat pemasangan.

<!-- VITAE_INSTALLER_BEGIN -->
```powershell
[CmdletBinding()]
param([string]$ProjectPath = (Get-Location).Path, [switch]$CheckOnly)
$ErrorActionPreference = 'Stop'
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
$mainVitae = Join-Path $rootVitae 'src/main/java/com/bangzachery/vitae/vitaemanager/Vitaemanager.java'
if (-not (Test-Path -LiteralPath $mainVitae -PathType Leaf)) { throw 'Vitaemanager.java tidak ditemukan. Jalankan dari folder proyek, sejajar gradlew.bat.' }
$mainTextVitae = [IO.File]::ReadAllText($mainVitae, $utf8Vitae)
if ($mainTextVitae -notmatch 'new\s+(?:com\.bangzachery\.vitae\.vitaemanager\.evolution\.)?EvolutionService\s*\(' -or
    $mainTextVitae -notmatch 'registerEvents\s*\(\s*evolution\s*,\s*this\s*\)' -or
    $mainTextVitae -notmatch 'evolution\.close\s*\(') {
    throw 'Integrasi evo pada main belum lengkap. Tidak ada file yang diubah.'
}
$filesVitae = @(
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionAltar.java'
        OriginalHash = 'd1b2a19e3c551a62ab914f27be66e956a67983f912edb13f6262185816408552'
        Hash = 'a1abd95076c80460531be83da394355e5d158c0d877ccab6eb78d7990d58a746'
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
        Map<Stage, Cue> cues, Music music, Effects effects, boolean lethal
) {
    public EvolutionAltar {
        require(id != null && id.matches("[a-z0-9_-]{1,32}"),
                "ID: 1–32 huruf kecil, angka, _ atau -.");

        Objects.requireNonNull(table);
        Objects.requireNonNull(preset);
        Objects.requireNonNull(music); Objects.requireNonNull(effects);
        encoded(source, true);
        encoded(result, true);

        ingredients = List.copyOf(ingredients);
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
            require(ingredient.point().distanceSquared(table) <= 64
                            && !ingredient.point().equals(table),
                    "Pedestal harus berbeda dari altar dan maksimal 8 block di world yang sama.");
            require(places.add(ingredient.point()),
                    "Pedestal bahan tidak boleh duplikat.");
        }

        // Validasi parameter: field record belum diisi pada compact constructor.
        require(!enabled || (!source.isEmpty() && !result.isEmpty()
                        && !ingredients.isEmpty() && !targets.isEmpty()),
                "Lengkapi source, result, bahan, dan target sebelum enable.");
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
                && !ingredients.isEmpty() && !targets.isEmpty();
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

    public record Ingredient(String item, int count, Point point) {
        public Ingredient {
            encoded(item, false);
            Objects.requireNonNull(point);
            require(count >= 1 && count <= 64,
                    "Jumlah bahan harus 1–64.");
        }
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
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionAnimation.java'
        OriginalHash = '2caea6dbc9e3182e5a0856bd1ce42cc3b008973f013a6f5a0d451aafaab15a57'
        Hash = 'ea48c3097cf8aa9e390a94e3c0685a1dfcd072b4639eb15dede478dafa09e3dd'
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
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionStore.java'
        OriginalHash = 'e386caae76d8570cddea7a73b725d0b6f49ed5146fa30558412b90e158a74b31'
        Hash = 'ff46f56b7ed672205f9bdea38f420b4cac96eb7b36523c2aa24cca9606705314'
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
        PREPARED, ACTIVE, REFUNDING, COMMITTING,
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
            require(phase != Phase.DELIVERED || old.phase() == Phase.COMMITTING,
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
        return load();
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

                materials.add(new Ingredient(
                        text(entry, "item"),
                        integer(entry, "count"),
                        Point.parse(text(entry, "point"))
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
                    cues, music, effects, (version == 2 || a.contains("lethal")) && bool(a, "lethal")
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
        section.set("point", ingredient.point().encode());
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
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionGrid.java'
        OriginalHash = ''
        Hash = '4c361e822d8cd8c8c2d0a63360ef8e5641606d79e30d5ece769e327a1e81986b'
        Source = @'
package com.bangzachery.vitae.vitaemanager.evolution;

import java.util.Map;

/** Slot 0 hasil terkunci; 1–9 matrix crafting, slot 5 senjata. */
public final class EvolutionGrid {
    private static final int[] SLOTS = {1, 2, 3, 4, 6, 7, 8, 9};
    private EvolutionGrid() { }
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
    public static int accepted(int required, int stored, int offered, boolean single) {
        EvolutionAltar.require(required >= 1 && stored >= 0 && stored <= required && offered >= 0,
                "Jumlah setoran tidak valid.");
        return Math.min(required - stored, Math.min(offered, single ? 1 : offered));
    }
}
'@
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionBlast.java'
        OriginalHash = ''
        Hash = '8b1fd74fc57c15f4ab84ac89f5463b2c3f0738304bb9ab88c5dea1f480129fdf'
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
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionMusic.java'
        OriginalHash = ''
        Hash = 'f98ce36f813826b6087163fdf9735832c7c28d2db4e0aa467d15d2ac5f1ee99e'
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
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionService.java'
        OriginalHash = '8d5cfac70dc6a8e778a8973cf272fb4e4244c10fbf0cd837b89980347b03aa0f'
        Hash = 'b65149fff451e4533c6ed565dc91d556424dd267f176940df440bcae9aa5f0c4'
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
    }

    public void start() {
        require(task == null && !closed,
                "Evolution sudah berjalan atau ditutup.");

        Bukkit.getWorlds().forEach(world ->
                world.getEntities().forEach(this::removeStale));

        current.deposits().values().forEach(d -> leases.put(d.altar(), d.owner()));
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
        editable();
        altar(id);
        require(!leases.containsKey(id) && current.deposits().values().stream().noneMatch(d -> d.altar().equals(id)),
                "Altar masih memiliki ritual atau setoran.");

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
        editable();
        altar(id);
        require(!leases.containsKey(id) && current.deposits().values().stream().noneMatch(d -> d.altar().equals(id)),
                "Altar masih memiliki ritual atau setoran.");

        submit(
                state -> state.remove(id),
                () -> say(sender, "Altar dihapus."),
                () -> saveFailed(sender)
        );
    }

    public void reload(CommandSender sender) {
        editable();
        require(menus.isEmpty() && runs.isEmpty() && recovering.isEmpty(),
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
                "Lengkapi source, result, bahan dan target.");
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
        int index = EvolutionGrid.index(event.getRawSlot()); if (index < 0) return;
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
                int index = EvolutionGrid.index(entry.getKey()); ItemStack old = menu.inventory.getItem(entry.getKey());
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
            if (d != null) paintDeposit(d); else releaseIfEmpty(menu);
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
            for (int i = 0; i < 8; i++) {
                ItemStack item;
                if (i >= menu.altar.ingredients().size()) item = icon(Material.GRAY_STAINED_GLASS_PANE, "Tidak dipakai", false);
                else {
                    Ingredient ingredient = menu.altar.ingredients().get(i);
                    item = amounts.getOrDefault(i, 0) == ingredient.count()
                            ? icon(Material.LIME_STAINED_GLASS_PANE, "Bahan " + (i + 1) + " selesai", true)
                            : decode(ingredient.item()).asOne();
                }
                menu.inventory.setItem(EvolutionGrid.rawSlot(i), item);
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
                require(run.active && !run.paused && !run.finishing,
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

        Phase preparing = action.equals("refund")
                ? Phase.REFUNDING : Phase.COMMITTING;

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

        if (cue.sound().isEmpty() || cue.volume() == 0) return;

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
                        .anyMatch(ingredient -> ingredient.point().equals(point)));
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

    private static String encode(ItemStack item) {
        String result = Base64.getEncoder()
                .encodeToString(item.serializeAsBytes());
        encoded(result, false);
        return result;
    }

    private static ItemStack decode(String encoded) {
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

        boolean consumed, active, paused, finishing, uncertain, musicStarted, commitRequested, commitReady;
        int elapsed;
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
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/command/EvolutionCommand.java'
        OriginalHash = '6bd7dd5b7d383d53de4cc044d2fbae8f04459c4625d665923022f2412241a956'
        Hash = 'dc67723abc4c264b9912e99a77b6d57739de3f2343482c355cd50c60066ceab4'
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
                    for (int i = 0; i < altar.ingredients().size(); i++) {
                        service.say(sender, "Bahan " + (i + 1) + ": x"
                                + altar.ingredients().get(i).count()
                                + " di " + altar.ingredients().get(i).point().encode());
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
                    EvolutionService.point(looked(player))
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
                    "create", "source", "result", "ingredient", "target", "enable")) {
                service.say(sender, "/evo " + usage.get(key));
            }
            service.say(sender,
                    "Pedestal berbeda, solid, maksimal 8 block dari altar. Tunggu pesan tersimpan sebelum command berikutnya.");
        } else if (usage.containsKey(topic)) {
            service.say(sender, "/evo " + usage.get(topic));
        } else {
            service.say(sender,
                    "/evo help setup. Bantuan command: /evo help <command>.");
            usage.values().forEach(line -> service.say(sender, "/evo " + line));
        }

        sender.sendMessage(net.kyori.adventure.text.Component.empty());
        if (topic.equals("music")) service.say(sender, "Durasi musik menentukan waktu ledakan. Cocokkan panjang rekaman custom; pitch tetap 1. Volume 0–1; radius melalui soundradius. ID custom harus ada di resource pack ItemsAdder.");
        if (topic.equals("blast")) service.say(sender, "Lethal lewat sistem totem; tanpa totem pemain mati. Radius dari crafting table. Preview tanpa damage.");
        if (topic.equals("effects")) service.say(sender, "Petir visual tanpa api/damage: jeda 0.5–10s, radius 0–8. Shockwave berupa particle.");
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
                         "info", "preview", "delete", "music", "blast", "effects" ->
                            service.ids().stream().toList();
                    default -> List.of();
                };
            } else if (args.length == 3) {
                choices = switch (action) {
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
    },
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcService.java'
        OriginalHash = '38594bc7f74793f89c07d5392cb10e7570862a8d7eb1dab0ac61e2faf1ef7120'
        Hash = 'c01766df473c0ac7882071413e93b119fd088655dff437ba6b6dae2f3f25de67'
        Source = @'
package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.core.gui.VitaeMenu;
import com.bangzachery.vitae.vitaemanager.economy.*;
import com.destroystokyo.paper.event.player.PlayerUseUnknownEntityEvent;
import com.google.gson.JsonObject;
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
        for (var d : state.npcs().values()) validateRuntime(d, false);
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
        validateRuntime(d, true);
    }
    private void validateRuntime(NpcData.Definition d, boolean requireWorld) {
        for (String item : d.gift().items()) decode(item);
        for (String item : d.quest().reward().items()) decode(item);
        if (d.quest().kind() == NpcData.QuestKind.MOB) NpcQuest.mobType(d.quest().mob());
        for (var crop : d.quest().crops()) {
            var plant = Bukkit.createBlockData(crop.plant()); var soil = Bukkit.createBlockData(crop.soil());
            if (!(plant instanceof org.bukkit.block.data.Ageable) || !Set.of(Material.CARROTS, Material.POTATOES, Material.WHEAT, Material.BEETROOTS).contains(plant.getMaterial())
                    || soil.getMaterial() != Material.FARMLAND) throw new IllegalArgumentException("Snapshot ladang tidak valid.");
        }
        if (requireWorld && d.enabled() && (Bukkit.getWorld(d.location().world()) == null || Bukkit.getWorld(d.stand().world()) == null))
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
    public void reloadModule() throws IOException {
        if (state.npcs().keySet().stream().anyMatch(this::inUse)) throw new IllegalStateException("Selesaikan dialog/quest aktif sebelum reload NPC.");
        NpcModule.stop(plugin); NpcModule.start(plugin, viti);
    }
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
            var transform = e.getTransformation(); transform.getScale().set(new Vector3f(.30f)); e.setTransformation(transform);
        });
        s.player.showEntity(plugin, s.display); render(s);
    }
    private void render(Session s) {
        if (s.display == null) return;
        int total = s.node.text().codePointCount(0, s.node.text().length()); int end = s.node.text().offsetByCodePoints(0, s.shown);
        Component text = Component.text(s.npc.name() + "\n", NamedTextColor.GOLD, TextDecoration.BOLD)
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
        if (sessions.containsKey(p.getUniqueId())) confirm(p); else click(p, id);
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
'@
    },
    [PSCustomObject]@{
        Name = 'src/test/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionStoreTest.java'
        OriginalHash = ''
        Hash = '7ca36420d0da5c6f0240c21edbf2706fc7de2a6cbfa0ecd535154b2e9e1f0f99'
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
    },
    [PSCustomObject]@{
        Name = 'src/test/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionEscrowTest.java'
        OriginalHash = ''
        Hash = '13c05bb81f2e0976bac2bfb63ab4b682c74c141750543418760ecd0241a6d1d5'
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
    },
    [PSCustomObject]@{
        Name = 'src/test/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionAnimationTest.java'
        OriginalHash = ''
        Hash = '6b1b47551db6388911320c4c84fb44c5775592c0b2288613bac11d56377b6089'
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
    },
    [PSCustomObject]@{
        Name = 'src/test/java/com/bangzachery/vitae/vitaemanager/evolution/EvolutionGridTest.java'
        OriginalHash = ''
        Hash = '40a0bae5ba763becc8d872c8fa6a7a55d82fc02ffb6b62188b22becdee41625b'
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
)
$plansVitae = @()
foreach ($fileVitae in $filesVitae) {
    if ($fileVitae.Name -notmatch '^src/(main|test)/java/com/bangzachery/vitae/vitaemanager/[A-Za-z0-9_/]+\.java$') { throw 'Path installer tidak valid.' }
    $targetVitae = Join-Path $rootVitae $fileVitae.Name
    $newTextVitae = Normalize-Vitae $fileVitae.Source
    $bytesVitae = $utf8Vitae.GetBytes($newTextVitae)
    if ((Get-VitaeHash $bytesVitae) -ne $fileVitae.Hash) { throw ('Isi installer tidak utuh: ' + $fileVitae.Name + '. Unduh ulang. Tidak ada file yang diubah.') }
    $existsVitae = Test-Path -LiteralPath $targetVitae -PathType Leaf
    $oldRawHashVitae = ''
    if ($existsVitae) {
        $rawVitae = [IO.File]::ReadAllBytes($targetVitae)
        $oldRawHashVitae = Get-VitaeHash $rawVitae
        $oldHashVitae = Get-VitaeHash ($utf8Vitae.GetBytes((Normalize-Vitae ([IO.File]::ReadAllText($targetVitae, $utf8Vitae)))))
        if ($oldHashVitae -eq $fileVitae.Hash) { continue }
        if ($fileVitae.OriginalHash -eq '' -or $oldHashVitae -ne $fileVitae.OriginalHash) {
            throw ($fileVitae.Name + ' berbeda dari commit sumber pembaruan. Kirim versi terbarunya untuk penyesuaian. Tidak ada file yang diubah.')
        }
        if ((Get-Item -LiteralPath $targetVitae).IsReadOnly) { throw ($fileVitae.Name + ' read-only. Tidak ada file yang diubah.') }
    } elseif ($fileVitae.OriginalHash -ne '') {
        throw ($fileVitae.Name + ' belum ada; modul lama belum lengkap. Tidak ada file yang diubah.')
    } elseif (Test-Path -LiteralPath $targetVitae) {
        throw ($fileVitae.Name + ' adalah direktori, bukan file. Tidak ada file yang diubah.')
    }
    $plansVitae += [PSCustomObject]@{ Name=$fileVitae.Name; Path=$targetVitae; Bytes=$bytesVitae; Existed=$existsVitae; OldRawHash=$oldRawHashVitae }
}
if ($plansVitae.Count -eq 0) { Write-Host 'Update evo ini sudah terpasang; tidak ditulis ulang.' -ForegroundColor Green; return }
Write-Host ('Pemeriksaan lolos: ' + $plansVitae.Count + ' file siap dipasang.') -ForegroundColor Green
Write-Host 'Main, command lain, dan konfigurasi server tetap utuh.' -ForegroundColor Cyan
if ($CheckOnly) { Write-Host 'CheckOnly selesai. Belum ada file yang diubah.' -ForegroundColor Yellow; return }
$backupVitae = Join-Path $rootVitae ('vitae-evo-backup-' + [DateTime]::Now.ToString('yyyyMMdd-HHmmss-fff') + '-' + [Guid]::NewGuid().ToString('N').Substring(0,6))
$temporaryVitae = @()
$attemptedVitae = @()
$createdDirsVitae = @()
try {
    # Pastikan IntelliJ tidak mengubah source di antara pemeriksaan dan pemasangan.
    foreach ($planVitae in $plansVitae) {
        if ($planVitae.Existed) {
            if (-not (Test-Path -LiteralPath $planVitae.Path -PathType Leaf) -or
                (Get-VitaeHash ([IO.File]::ReadAllBytes($planVitae.Path))) -ne $planVitae.OldRawHash) { throw ($planVitae.Name + ' berubah saat pemasangan; hentikan dan ulangi.') }
        } elseif (Test-Path -LiteralPath $planVitae.Path) { throw ($planVitae.Name + ' muncul saat pemasangan; hentikan dan ulangi.') }
    }
    [IO.Directory]::CreateDirectory($backupVitae) | Out-Null
    foreach ($planVitae in $plansVitae) {
        if ($planVitae.Existed) {
            $savedVitae = Join-Path $backupVitae $planVitae.Name
            [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($savedVitae)) | Out-Null
            Copy-Item -LiteralPath $planVitae.Path -Destination $savedVitae -ErrorAction Stop
        }
    }
    foreach ($planVitae in $plansVitae) {
        $parentVitae = [IO.Path]::GetDirectoryName($planVitae.Path)
        if (-not (Test-Path -LiteralPath $parentVitae -PathType Container)) {
            $createdDirsVitae += $parentVitae
            [IO.Directory]::CreateDirectory($parentVitae) | Out-Null
        }
        $tempVitae = $planVitae.Path + '.vitae-evo-' + [Guid]::NewGuid().ToString('N')
        $temporaryVitae += $tempVitae
        [IO.File]::WriteAllBytes($tempVitae, $planVitae.Bytes)
        $planVitae | Add-Member -NotePropertyName Temporary -NotePropertyValue $tempVitae
    }
    foreach ($planVitae in $plansVitae) {
        $attemptedVitae += $planVitae
        Move-Item -LiteralPath $planVitae.Temporary -Destination $planVitae.Path -Force -ErrorAction Stop
    }
} catch {
    $causeVitae = $_.Exception.Message
    $rollbackVitae = @()
    foreach ($planVitae in $attemptedVitae) {
        try {
            if ($planVitae.Existed) { Copy-Item -LiteralPath (Join-Path $backupVitae $planVitae.Name) -Destination $planVitae.Path -Force -ErrorAction Stop }
            elseif (Test-Path -LiteralPath $planVitae.Path -PathType Leaf) { Remove-Item -LiteralPath $planVitae.Path -Force -ErrorAction Stop }
        } catch { $rollbackVitae += ($planVitae.Name + ': ' + $_.Exception.Message) }
    }
    if ($rollbackVitae.Count -gt 0) { throw ('Pemasangan gagal dan sebagian rollback gagal. Backup: ' + $backupVitae + '. Detail: ' + ($rollbackVitae -join '; ')) }
    throw ('Pemasangan dibatalkan; source yang sudah ditulis dipulihkan. Penyebab: ' + $causeVitae + '. Backup: ' + $backupVitae)
} finally {
    foreach ($tempVitae in $temporaryVitae) { if (Test-Path -LiteralPath $tempVitae -PathType Leaf) { Remove-Item -LiteralPath $tempVitae -Force -ErrorAction SilentlyContinue } }
    foreach ($dirVitae in $createdDirsVitae) { if ((Test-Path -LiteralPath $dirVitae -PathType Container) -and @(Get-ChildItem -LiteralPath $dirVitae -Force).Count -eq 0) { Remove-Item -LiteralPath $dirVitae -ErrorAction SilentlyContinue } }
}
Write-Host 'Update evo terpasang. Backup source lama:' -ForegroundColor Green
Write-Host $backupVitae
Write-Host 'Lanjutkan: .\gradlew.bat clean build' -ForegroundColor Cyan
Write-Host 'Ganti JAR hasil build di server, lalu restart server.' -ForegroundColor Yellow
Write-Host 'Bantuan pengaturan: /evo help music, /evo help blast, /evo help effects.' -ForegroundColor Cyan
```
<!-- VITAE_INSTALLER_END -->
