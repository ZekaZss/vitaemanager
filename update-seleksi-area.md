# Update alat seleksi area Vitaemanager

Dasar update ini adalah `update-dungeon-fade.md` yang sudah terpasang di server. Paper 1.21.4 dan Java 21 tetap dipakai. Tidak ada plugin tambahan.

## Pasang di project IntelliJ

Letakkan file ini sejajar `gradlew.bat` di folder project, kemudian jalankan PowerShell:

```powershell
$m=Get-Content -LiteralPath .\update-seleksi-area.md -Raw -Encoding UTF8; $i=[regex]::Match($m,'(?s)<!-- VITAE_INSTALLER_BEGIN -->\r?\n```powershell\r?\n(.*?)\r?\n```\r?\n<!-- VITAE_INSTALLER_END -->'); if(!$i.Success){throw 'Blok installer tidak ditemukan'}; & ([scriptblock]::Create($i.Groups[1].Value)) -ProjectPath (Get-Location).Path
```

Installer memeriksa seluruh sumber sebelum menulis, membuat backup sumber, dan memulihkan file apabila pemasangan gagal. Perubahan manual yang tidak dikenali menghentikan pemasangan sebelum menulis. Untuk pemeriksaan saja, tambahkan `-CheckOnly` pada akhir command installer.

Setelah berhasil:

```powershell
.\gradlew.bat clean build
```

Hentikan server, ganti JAR lama dengan hasil di `build/libs`, dan hidupkan server kembali. Pertahankan folder `plugins/vitaemanager` dan folder plugin lain. Installer hanya memasang kode project, bukan data server.

## Cara memakai alat

Command memberikan satu prismarine shard khusus ke tangan admin. Item yang sebelumnya di tangan dipindahkan ke slot kosong; bila inventory penuh, command ditolak. Klik kiri blok sudut pertama untuk pos1, klik kanan sudut diagonal untuk pos2. Kedua sudut harus satu world. Setelah keduanya dipilih, alat hilang dan hasil diproses otomatis.

Prismarine shard biasa tidak menjadi alat seleksi. Blok yang dipilih tidak dihancurkan dan klik kanan tidak membuka chest/menekan tuas. Selama memilih, pindah/lempar item alat melalui inventory dicegah. Selesaikan pilihan atau batalkan sebelum mengatur inventory kembali. Alat dibersihkan ketika admin keluar, mati, atau plugin dimatikan.

```text
/vitae seleksi status
/vitae seleksi cancel
```

Cancel membatalkan alat aktif serta pemindaian gerbang/dungeon milik admin tersebut. Pemindaian ladang memakai command lama `/vnpc <id> quest cancel`.

## Dungeon yang sudah kamu setup

ID tetap `dungeon_dp2`, world tetap `hitamhitam`; nama world tidak diubah lewat installer. NPC, hadiah, lokasi pulang, kamera, Archer, dan bos tidak perlu dibuat ulang.

Jika sesi masih aktif, hentikan dan tunggu pemulihan:

```text
/vdungeon dungeon_dp2 reset
/vdungeon dungeon_dp2 status
```

Perbaiki portal masuk dari world utama:

```text
/vdungeon dungeon_dp2 wand entrance
```

Klik kiri lantai di sudut pertama ruang portal, lalu klik kanan lantai sudut diagonal seberangnya. Seleksi mencakup lantai serta dua blok di atasnya, sehingga kaki pemain di lantai biasa atau slab masuk dalam pemicu. Area entrance tersimpan otomatis dan dungeon menjadi OFF; tidak perlu `region entrance` lagi.

| Command alat dungeon | Yang diklik |
|---|---|
| `/vdungeon dungeon_dp2 wand arena` | Dua blok sudut kubus seluruh arena, termasuk ruang jatuh, start, camera trigger dan finish |
| `/vdungeon dungeon_dp2 wand camera` | Dua blok lantai yang membungkus platform start; ruang tubuh otomatis ikut |
| `/vdungeon dungeon_dp2 wand finish` | Dua blok lantai di garis finish |
| `/vdungeon dungeon_dp2 wand miniongate` | Dua blok sudut dinding gerbang minion |
| `/vdungeon dungeon_dp2 wand miniontrigger` | Dua blok lantai setelah gerbang minion |
| `/vdungeon dungeon_dp2 wand bossgate` | Dua blok sudut dinding gerbang bos |
| `/vdungeon dungeon_dp2 wand bosstrigger` | Dua blok lantai setelah gerbang bos |
| `/vdungeon dungeon_dp2 wand exit` | Dua sisi bingkai yang menghadap bagian udara portal, pada dua sudut diagonal |
| `/vdungeon dungeon_dp2 wand bad salah_01` | Dua blok kaca sudut petak salah, satu ketinggian |

Setiap command hanya memilih satu area. Ulangi command untuk area lain. Seleksi `exit` memakai blok di sebelah sisi bingkai yang diklik, sehingga bingkai tidak ikut terpilih; seluruh hasil harus udara. Gerbang dan kaca memakai blok yang diklik langsung. Petak salah boleh lebih besar dari 3×3, tetapi tetap satu lapis dan tidak bertumpuk dengan finish/petak lain.

Lokasi satu titik tetap memakai `location entry|home|start|camera|next`; item alat hanya untuk dua titik area.

Setelah perubahan selesai, jalankan satu per satu dan tunggu pesan selesai masing-masing:

```text
/vdungeon dungeon_dp2 check
/vdungeon dungeon_dp2 on
/vdungeon dungeon_dp2 open
```

Pemeriksaan blok dan snapshot sesi dilakukan bertahap. Saat `check/on/open` masih membaca area, jangan menjalankan tahap berikutnya dulu. Sesi baru masuk setelah pesan `Sesi dibuka`. `check` sekarang menyebut nama area/lokasi yang belum disimpan. Sesi tidak dibuka otomatis ketika player mencoba masuk.

## Bisikan

Contoh memilih ulang area Lunar:

```text
/vitae bisikan wand dungeon_lunar
```

Ganti ID dengan nama bisikan yang benar-benar ada di `/vitae bisikan list`. Klik dua blok lantai sudut area. Area disimpan otomatis, tanpa mengubah dialog, target, suara, musik, atau progres sekali per player. Command `pos1`, `pos2`, dan `area` lama tetap tersedia.

## NPC biasa dan quest

Ganti `id_npc` dengan ID NPC milikmu:

```text
/vnpc id_npc wand trigger
/vnpc id_npc wand quest
/vnpc id_npc wand gate
```

- `trigger`: klik lantai dua sudut; area pemicu dialog tersimpan otomatis. Atur stand di luar pemicu, lalu `/vnpc id_npc on`.
- `quest`: klik blok sudut area; lantai dan dua blok di atasnya ikut. Seleksi tersedia untuk command quest lama. Setelah alat hilang, berdiri di titik start dalam area, lalu `/vnpc id_npc quest farm 5` atau `/vnpc id_npc quest mob 5 COW`. Pilih area lengkap yang membungkus seluruh tanaman/mob, termasuk tinggi yang diperlukan.
- `gate`: klik dua blok dinding. Snapshot disimpan bertahap, NPC OFF sampai diaktifkan. Tunggu `Gerbang tersimpan`. Jika NPC sudah mempunyai gerbang, gunakan `gate off` terlebih dahulu hanya bila memang ingin mengganti gerbang tersebut.

## Batas area dan data lama

Tidak ada lagi batas ukuran area bisikan 256×512×256, gerbang NPC 256 blok, portal keluar 256 blok, atau snapshot dungeon 32768 blok. Batas jumlah tanaman lama tetap dihapus. Tidak ada batas luas kubus arena yang ditambahkan. Pemindaian/mutasi area besar dibagi per tick; visual gerbang memakai sampel agar jumlah entity animasi tidak naik mengikuti setiap blok dinding.

Tidak ada batas jumlah blok buatan plugin. Koordinat harus tetap valid dan berada dalam ketinggian world Minecraft. Area yang lebih besar membutuhkan lebih banyak waktu dan RAM untuk snapshot; ini tidak berarti kapasitas server menjadi tak terbatas. Gerbang tetap menolak chest/blok penyimpan data, dan seleksi tetap memerlukan dua sudut dalam satu world. Jumlah mob aktif, isi GUI hadiah, serta aturan cooldown quest tidak diubah oleh update seleksi ini.

Schema/file runtime lama tetap dipakai: `npc.json`, `whispers.yml`, `dungeons.json`, `fade.json`, `evolution.yml`, Viti dan data player. Tidak ada reset global. Code lama tetap dibawa melalui integrasi lengkap di main class.

## Verifikasi

Revisi ini memperbaiki file WhisperArea.java yang tertinggal dari installer pertama. Tes baru sudah menerima seleksi besar, tetapi kode lama masih menolaknya. Installer revisi dapat dijalankan di atas pemasangan pertama; file yang sudah cocok dilewati dan file yang tertinggal ditambahkan, tanpa reset data.

Build Java 21/Paper 1.21.4 dan 179 pemeriksaan yang sudah ada lolos dari hasil pemasangan installer. Interaksi dua klik, pemilihan sisi bingkai, serta pengaruh plugin proteksi/resource pack tetap perlu dicoba pada server milikmu.

## Installer otomatis

Blok berikut dieksekusi oleh command pemasangan di atas. Tidak perlu menyalin Java per bagian.

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
        OriginalHashes = @('1a9e800896840422b8532b8e55cbc18a10be2cb934620b6a31aa7280371a5079')
        Hash = '40511e87d0e399dc017dad8ba111d82ee5841da593fc2edf22a211c04aed8406'
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
    private com.bangzachery.vitae.vitaemanager.selection.SelectionService selection;

    @Override
    public void onEnable() {
        try {
            selection = new com.bangzachery.vitae.vitaemanager.selection.SelectionService(this);
            selection.start();
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
        if (selection != null) selection.close();
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
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/command/VitaeCommand.java'
        OriginalHashes = @('03beb620a61446f28040da52eeb9d11425be429915731caf7499f5877baed5ca')
        Hash = '6d6b950dd16c3ac8dccc1e79a12a553cf1ebb3211366c1192f2b9cda3d63ff63'
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
            sender.sendMessage(Component.text("Dungeon: /vdungeon help • Fade: /vitae fade help • Alat seleksi: /vitae seleksi help", NamedTextColor.AQUA));
            if (sender.hasPermission("vitae.admin.whisper")) sender.sendMessage(messages.component("prefix")
                    .append(Component.text("Bisikan Area: /vitae bisikan help", NamedTextColor.AQUA)));
            return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("seleksi")) {
            if (!(sender instanceof Player p)) { sender.sendMessage(Component.text("Seleksi digunakan dari dalam game.")); return true; }
            try {
                var tool = com.bangzachery.vitae.vitaemanager.selection.SelectionService.require();
                if (args.length == 2 && args[1].equalsIgnoreCase("cancel")) { tool.cancel(p); tool.scanner().cancelOwner(p.getUniqueId()); tool.say(p,"Seleksi/pemindaian milikmu dibatalkan."); }
                else if (args.length == 2 && args[1].equalsIgnoreCase("status")) tool.status(p);
                else p.sendMessage(Component.text("/vitae seleksi cancel|status • /vdungeon <id> wand <area> • /vnpc <id> wand quest|trigger|gate • /vitae bisikan wand <id>",NamedTextColor.AQUA));
            } catch (RuntimeException e) { p.sendMessage(Component.text(e.getMessage(),NamedTextColor.RED)); }
            return true;
        }
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
            choices.addAll(List.of("help", "panel", "seleksi"));
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
            if (args.length==2&&action.equals("seleksi")) choices.addAll(List.of("help","cancel","status"));
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
        OriginalHashes = @('971771950082cfc5b835354b0ed826324afa37c3fb0abef49031d571a3f9e770')
        Hash = 'e49feaf7a67e8eb772e2bf8f91d57e9818a93ad600917acafdc9edb91cd5beb6'
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
    private static final List<String> ACTIONS = List.of("help", "wand", "pos1", "pos2", "create", "template", "area", "target", "line",
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
                case "wand" -> { length(args, 2); service.wand(player(sender), args[1]); }
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
            case "wand" -> "/vitae bisikan wand <id>\nKlik kiri/kanan dua blok lantai; ruang di atasnya disimpan otomatis sebagai area bisikan.";
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
            default -> "Setup cepat: template <id> lunar → wand <id> → dua klik lantai → target add <id> <nick-MC> → preview <id> → enable <id>.\n"
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
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcCommand.java'
        OriginalHashes = @('6b7a3e618b529b941b0023ee50eaea79e6a338fd8737191a743bd2799027cd50')
        Hash = '88758db8105f25ca8f1b63a5de7db29d845306ab5fd730982bab90cb4668b874'
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
            "clearoptions", "root", "repeat", "speed", "wand", "pos1", "pos2", "trigger", "gate", "quest", "queststart", "reward", "on", "off", "preview", "reset", "delete");
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
                case "wand" -> {
                    require(args.length==3,"/vnpc <id> wand quest|trigger|gate");String mode=args[2].toLowerCase(Locale.ROOT);
                    require(Set.of("quest","trigger","gate").contains(mode),"Wand: quest, trigger, gate.");
                    var tool=com.bangzachery.vitae.vitaemanager.selection.SelectionService.require();
                    var kind=mode.equals("trigger")?com.bangzachery.vitae.vitaemanager.selection.SelectionService.Mode.WALK:
                        mode.equals("quest")?com.bangzachery.vitae.vitaemanager.selection.SelectionService.Mode.VOLUME:com.bangzachery.vitae.vitaemanager.selection.SelectionService.Mode.BLOCKS;
                    tool.arm(p,"vitae.admin.npc","NPC "+id+" • "+mode,kind,(admin,a,b)->{
                        var selected=com.bangzachery.vitae.vitaemanager.selection.SelectionService.area(a,b);
                        if(mode.equals("gate"))service.setupGate(admin,id,selected);
                        else if(mode.equals("trigger")){service.edit(id,j->j.add("trigger",NpcStore.JSON.toJsonTree(selected)),false);NpcService.message(admin,"Pemicu tersimpan. Aktifkan NPC setelah setup selesai.");}
                        else{selections.put(admin.getUniqueId(),new Corners(a,b));NpcService.message(admin,"Area quest dipilih. Berdiri di titik start dalam area, lalu /vnpc "+id+" quest farm <putaran> atau quest mob <jumlah> <jenis>.");}
                    });return true;
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
            case "wand", "pos1", "pos2", "trigger" -> "pemicu";
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
                tip(sender, cmd + "wand quest", "Ambil alat untuk memilih area ladang dengan dua klik");
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
                tip(sender, cmd + "wand quest", "Pilih area quest dengan dua klik");
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
                tip(sender, cmd + "wand trigger", "Klik dua blok lantai; pemicu dialog otomatis tersimpan");
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
                tip(sender, cmd + "wand gate", "Dua klik memilih dinding; pemindaian/simpan otomatis");
                tip(sender, cmd + "gate pos1", "Aktifkan pemilihan A, lalu pukul block sudut dinding; block tidak pecah");
                tip(sender, cmd + "gate pos2", "Aktifkan pemilihan B, lalu pukul sudut diagonal seberangnya");
                tip(sender, cmd + "gate set", "Simpan dinding tanpa batas jumlah blok; pemindaian berjalan bertahap");
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
            case "wand" -> values.addAll(List.of("quest", "trigger", "gate"));
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
        OriginalHashes = @('5e480bf3759e27d69055e13db5df81ff924dd94b4c2019f4355950176466c867')
        Hash = '3f452c0c0b8f9ad73d7be87c98995e3268e9ef841553e31c7473a8f91fda039c'
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
            if (blocks.isEmpty()) throw new IllegalArgumentException("Seleksi gerbang harus memuat minimal satu blok dinding.");
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
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcFarmCursor.java'
        OriginalHashes = @('8ddc81b0bca051332853d80d00e82c3a0dc22cc073b2e2f4eae1613581c2b4fc')
        Hash = '233eca70f3858c1b626336bff43fd10d47b32658ee071dfb0fbcf3859ce9a478'
        Required = $true
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
        return com.bangzachery.vitae.vitaemanager.selection.AreaScanner.volume(area);
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
        OriginalHashes = @('921104a726360c8689e22810558bbeac5eb6076b2238fe66b38e6b41f50fd673')
        Hash = '8a19cf9102ff311abdf0dfed61651c95fec510c4883cedfa25b955dd5c1905f1'
        Required = $true
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
        int age; boolean opened, initialized;
        Run(String id, Gate gate) { this.id = id; this.gate = gate; }
    }
    private final JavaPlugin plugin;
    private final Supplier<Map<String, Definition>> definitions;
    private final StatusWriter writer;
    private final Consumer<Exception> failure;
    private final Map<String, Run> runs = new HashMap<>();
    private final Map<String,com.bangzachery.vitae.vitaemanager.selection.AreaScanner.Handle> scans=new HashMap<>();
    private static final class Change {
        final String id;final Gate gate;final boolean clearing,effects;final Iterator<GateBlock> cells;final Runnable done;
        GateBlock pending;java.util.concurrent.CompletableFuture<Chunk> loading;long since;
        Change(String id,Gate gate,boolean clearing,boolean effects,Iterator<GateBlock> cells,Runnable done){this.id=id;this.gate=gate;this.clearing=clearing;this.effects=effects;this.cells=cells;this.done=done;}
    }
    private final Deque<Change> changes=new ArrayDeque<>();
    private final Set<String> changing=new HashSet<>();
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
    public boolean busy(String id) { return runs.containsKey(id)||scans.containsKey(id)||changing.contains(id); }
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
                change(definition.id(),gate,true,false,()->{});
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
    public WhisperArea selectedArea(Player admin,String id){
        Corners c=corners.get(admin.getUniqueId());
        if(c==null||!c.id.equals(id)||c.first==null||c.second==null)throw new IllegalArgumentException("Pilih kedua sudut atau gunakan /vnpc "+id+" wand gate.");
        if(!c.first.world.equals(c.second.world))throw new IllegalArgumentException("Kedua sudut harus satu world.");
        return WhisperArea.between(c.first.world,c.first.x,c.first.y,c.first.z,c.second.x,c.second.y,c.second.z);
    }
    public void scan(Player admin,String id,WhisperArea area,BooleanSupplier active,Consumer<Gate> success){
        if(busy(id))throw new IllegalStateException("Gerbang sedang dipakai/dipindai.");var cells=new ArrayList<GateBlock>();
        var handle=com.bangzachery.vitae.vitaemanager.selection.SelectionService.require().scanner().scan(area,
            ()->!closed&&admin.isOnline()&&admin.hasPermission("vitae.admin.npc")&&active.getAsBoolean(),
            (read,total)->admin.sendActionBar(Component.text("Membaca gerbang "+id+": "+read+"/"+total,NamedTextColor.AQUA)),
            block->{if(block.getType().isAir())return;if(!safe(block))throw new IllegalArgumentException("Gerbang perlu dinding solid biasa; blok tidak didukung: "+block.getType());cells.add(new GateBlock(block.getX(),block.getY(),block.getZ(),block.getBlockData().getAsString()));},
            ()->{scans.remove(id);admin.sendActionBar(Component.empty());success.accept(new Gate(area,cells,GateStatus.CLOSED));},
            reason->{scans.remove(id);admin.sendActionBar(Component.empty());NpcService.message(admin,reason);}).owned(admin.getUniqueId());
        scans.put(id,handle);NpcService.message(admin,"Membaca gerbang bertahap. Tunggu pesan 'Gerbang tersimpan' sebelum on.");
    }
    private void change(String id,Gate gate,boolean clearing,boolean effects,Runnable done){
        if(!changing.add(id))return;changes.addLast(new Change(id,gate,clearing,effects,gate.blocks().iterator(),done));
    }
    private void changeTick(){
        int used=0,idle=0;long end=System.nanoTime()+2_000_000;
        while(!changes.isEmpty()&&used<128&&System.nanoTime()<end){Change c=changes.removeFirst();World w=Bukkit.getWorld(c.gate.area().world());
            try{
                if(w==null||(!c.clearing&&occupied(c.gate))){changes.addLast(c);if(++idle>=changes.size())break;continue;}
                int slice=0;boolean waiting=false;
                while((c.pending!=null||c.cells.hasNext())&&slice<16&&used<128&&System.nanoTime()<end){
                    if(c.pending==null)c.pending=c.cells.next();GateBlock cell=c.pending;
                    if(!w.isChunkLoaded(cell.x()>>4,cell.z()>>4)){
                        if(c.loading==null){c.since=System.currentTimeMillis();c.loading=w.getChunkAtAsync(cell.x()>>4,cell.z()>>4,false);}
                        else if(c.loading.isDone()){if(c.loading.join()==null)throw new IllegalStateException("Chunk gerbang belum tersedia.");c.loading=null;}
                        if(System.currentTimeMillis()-c.since>60000)throw new IllegalStateException("Chunk gerbang belum bisa dimuat.");waiting=true;break;
                    }
                    c.loading=null;Block block=w.getBlockAt(cell.x(),cell.y(),cell.z());
                    if(c.clearing){if(block.getBlockData().getAsString().equals(cell.data())&&safe(block))block.setType(Material.AIR,false);}
                    else if(block.getType().isAir())block.setBlockData(Bukkit.createBlockData(cell.data()),false);
                    if(c.effects&&slice==0&&used%32==0)w.spawnParticle(Particle.BLOCK,block.getLocation().add(.5,.5,.5),2,.15,.15,.15,Bukkit.createBlockData(cell.data()));
                    c.pending=null;slice++;used++;
                }
                if(c.pending!=null||c.cells.hasNext()){changes.addLast(c);idle=waiting?idle+1:0;if(idle>=changes.size())break;}
                else{changing.remove(c.id);if(c.effects)w.playSound(center(c.gate),"minecraft:block.stone.place",.8f,.7f);c.done.run();idle=0;}
            }catch(RuntimeException e){changing.remove(c.id);Run run=runs.get(c.id);if(run!=null)abort(run);repairs.add(c.id);failure.accept(e);}
        }
    }
    static boolean safe(Block block) {
        Material type = block.getType();
        return type.isSolid() && !type.hasGravity() && !type.isInteractable() && !block.isLiquid() && !block.isPassable() && !(block.getState() instanceof TileState)
                && !Set.of(Material.BEDROCK, Material.BARRIER, Material.END_PORTAL_FRAME, Material.STRUCTURE_VOID, Material.REINFORCED_DEEPSLATE).contains(type);
    }
    public boolean open(String id, Claim claim) throws IOException {
        Definition definition = definitions.get().get(id); Gate gate = definition == null ? null : definition.gate();
        if (closed || gate == null) throw new IllegalStateException("Gerbang belum disiapkan.");
        if (busy(id) || gate.status() != GateStatus.CLOSED) return false;
        World world = Objects.requireNonNull(Bukkit.getWorld(gate.area().world()),"Dunia gerbang belum dimuat.");
        Gate opening=gate.withStatus(GateStatus.OPENING);claim.save(opening);
        Run run=new Run(id,opening);runs.put(id,run);
        try{
            Map<String,GateBlock> expected=new HashMap<>();for(GateBlock c:gate.blocks())expected.put(c.key(),c);
            var handle=com.bangzachery.vitae.vitaemanager.selection.SelectionService.require().scanner().scan(gate.area(),()->!closed&&runs.get(id)==run,
                (read,total)->{},block->{GateBlock c=expected.get(block.getX()+":"+block.getY()+":"+block.getZ());if(c!=null&&(!block.getBlockData().getAsString().equals(c.data())||!safe(block)))throw new IllegalStateException("Dinding berubah dari snapshot. Pulihkan atau setup ulang.");},
                ()->{scans.remove(id);try{
                    for(int i=0;i<Math.min(64,gate.blocks().size());i++){GateBlock c=gate.blocks().get(i);BlockDisplay v=world.spawn(new Location(world,c.x(),c.y(),c.z()),BlockDisplay.class);run.visuals.add(v);v.setBlock(Bukkit.createBlockData(c.data()));v.setPersistent(false);v.setInvulnerable(true);v.setGravity(false);v.setTeleportDuration(1);v.setInterpolationDuration(1);}
                    run.initialized=true;world.playSound(center(gate),"minecraft:block.stone.hit",.7f,.6f);
                }catch(RuntimeException e){abort(run);failure.accept(e);}},
                reason->{scans.remove(id);abort(run);failure.accept(new IllegalStateException(reason));});scans.put(id,handle);
        }catch(RuntimeException e){abort(run);failure.accept(e);throw e;}
        return true;
    }
    public void tick() {
        if (closed) return; ticks++;changeTick();
        suppressed.entrySet().removeIf(e -> e.getValue().until() < ticks);
        for (Run run : List.copyOf(runs.values())) try {
            if(!run.initialized)continue;
            run.age++;
            if (!run.opened) {
                double amplitude = .025 + .08 * run.age / 40.0;
                for (int i = 0; i < run.visuals.size(); i++) {
                    GateBlock block = run.gate.blocks().get(i);
                    Location location = new Location(Bukkit.getWorld(run.gate.area().world()), block.x() + Math.sin(run.age * 2.4 + i) * amplitude,
                            block.y() + Math.cos(run.age * 2 + i) * amplitude / 3, block.z() + Math.cos(run.age * 2.2 + i) * amplitude);
                    if (!run.visuals.get(i).isValid() || !run.visuals.get(i).teleport(location)) throw new IllegalStateException("Visual gerbang terputus.");
                }
                if (run.age % 10 == 0) center(run.gate).getWorld().playSound(center(run.gate), "minecraft:block.stone.hit", .5f, .5f);
                if (run.age >= 40) {
                    if(changing.contains(run.id))continue;
                    run.initialized=false;
                    change(run.id,run.gate,true,false,()->{try{writer.write(run.id,run.gate.opened(System.currentTimeMillis()));run.opened=true;run.initialized=true;run.age=40;}catch(IOException e){abort(run);failure.accept(e);}});
                    World world = center(run.gate).getWorld(); world.playSound(center(run.gate), "minecraft:entity.generic.explode", .9f, .8f);
                    world.spawnParticle(Particle.EXPLOSION_EMITTER, center(run.gate), 1);
                    for (GateBlock block : run.gate.blocks().subList(0,Math.min(64,run.gate.blocks().size()))) world.spawnParticle(Particle.BLOCK,
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
        } catch (RuntimeException e) { abort(run); failure.accept(e); }
        if (ticks % 20 == 0) autoClose(System.currentTimeMillis());
    }
    void autoClose(long now) {
        for (Definition definition : List.copyOf(definitions.get().values())) {
            Gate gate = definition.gate();
            if (gate == null || busy(definition.id())) continue;
            try {
                if (gate.status() == GateStatus.OPEN && gate.timing().seconds() > 0) {
                    long remaining = gate.timing().closeAt() - now;
                    if (remaining > 0 && remaining <= 5000) warnNearby(gate, "Gerbang akan ditutup dalam " + ((remaining + 999) / 1000) + " detik", true);
                    if (gate.due(now)) {
                        if (occupied(gate)) { warnNearby(gate, "Gerbang menunggu pemain keluar dari posisi dinding", false); continue; }
                        writer.write(definition.id(), gate.withStatus(GateStatus.CLOSING));
                        change(definition.id(),gate,false,true,()->{try{writer.write(definition.id(),gate.withStatus(GateStatus.CLOSED));}catch(IOException e){failure.accept(e);}});
                    }
                } else if (gate.status() == GateStatus.CLOSING) {
                    if (occupied(gate)) continue;
                    change(definition.id(),gate,false,false,()->{try{writer.write(definition.id(),gate.withStatus(GateStatus.CLOSED));}catch(IOException e){failure.accept(e);}});
                } else if (gate.status() == GateStatus.CLOSED && repairs.contains(definition.id()) && !occupied(gate)) {
                    change(definition.id(),gate,false,false,()->repairs.remove(definition.id()));
                }
                reported.remove(definition.id());
            } catch (IOException | RuntimeException e) {
                if (reported.add(definition.id())) failure.accept(e); // One report until the condition is resolved.
            }
        }
    }
    static boolean occupied(Gate gate) {
        World world = Bukkit.getWorld(gate.area().world());
        if (world == null) return true;
        WhisperArea a=gate.area();var bounds=new org.bukkit.util.BoundingBox(a.minX(),a.minY(),a.minZ(),a.maxX()+1.0,a.maxY()+1.0,a.maxZ()+1.0);
        for(Player p:world.getPlayers())if(p.getBoundingBox().overlaps(bounds))return true;
        return false;
    }
    private static void warnNearby(Gate gate, String text, boolean sound) {
        Location center = center(gate); if (center.getWorld() == null) return;
        for (Player player : center.getWorld().getPlayers()) if (player.getLocation().distanceSquared(center) <= 144) {
            player.sendActionBar(Component.text(text, NamedTextColor.YELLOW));
            if (sound) player.playSound(player.getLocation(), "minecraft:block.note_block.hat", .35f, 1.2f);
        }
    }
    private void abort(Run run) {
        remove(run);
        if (!run.opened) try { writer.write(run.id, run.gate.withStatus(GateStatus.CLOSED)); }
        catch (IOException | RuntimeException e) { failure.accept(e); }
    }
    private void remove(Run run) { var scan=scans.remove(run.id);if(scan!=null)scan.cancel();runs.remove(run.id); for (BlockDisplay visual : run.visuals) if (visual.isValid()) visual.remove(); }
    public void reset(String id, ResetSave saveClosed) throws IOException {
        Definition definition = definitions.get().get(id); Gate gate = definition == null ? null : definition.gate();
        if (gate == null || busy(id)) throw new IllegalArgumentException("Gerbang belum ada atau masih beranimasi.");
        World world = Objects.requireNonNull(Bukkit.getWorld(gate.area().world()),"Dunia belum dimuat.");
        for (Player player : world.getPlayers()) {
            var box = player.getBoundingBox(); WhisperArea area = gate.area();
            if (box.overlaps(new org.bukkit.util.BoundingBox(area.minX(), area.minY(), area.minZ(), area.maxX() + 1, area.maxY() + 1, area.maxZ() + 1)))
                throw new IllegalStateException("Pindahkan pemain dari area gerbang sebelum reset.");
        }
        writer.write(id, gate.withStatus(GateStatus.CLOSING));
        change(id,gate,false,true,()->{try{saveClosed.save();refresh();}catch(IOException e){failure.accept(e);}});
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
            if (definition.gate().status() == GateStatus.OPEN) change(definition.id(),definition.gate(),true,false,()->{});
            else if (definition.gate().status() == GateStatus.CLOSED) repairs.add(definition.id());
        }
    }
    @EventHandler public void quit(PlayerQuitEvent event) { picks.remove(event.getPlayer().getUniqueId()); corners.remove(event.getPlayer().getUniqueId()); suppressed.remove(event.getPlayer().getUniqueId()); }
    @Override public void close() {
        closed = true;for(var h:scans.values())h.cancel();scans.clear();changes.clear();changing.clear(); for (Run run : List.copyOf(runs.values())) abort(run);
        picks.clear(); corners.clear(); suppressed.clear(); protectedCells.clear(); repairs.clear(); reported.clear(); HandlerList.unregisterAll(this);
    }
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/npc/NpcService.java'
        OriginalHashes = @('5c185c3455eab0cf071c108a8533d4b32ada8baf726fdba93959cc9d1a810b15')
        Hash = '42a433840361ccf3db804506939b1207c864377e38d190b232d1f72d8a3fdfcd'
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
        if(closed)throw new IllegalStateException("Modul NPC sudah dimuat ulang. Ambil alat seleksi baru.");
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
    public void gateSet(Player admin, String id) throws IOException {setupGate(admin,id,gates.selectedArea(admin,id));}
    public void setupGate(Player admin,String id,com.bangzachery.vitae.vitaemanager.whisper.WhisperArea area){
        NpcData.Definition before=get(id);
        if(before.gate()!=null)throw new IllegalArgumentException("Gerbang sudah ada. Gunakan gate off sebelum mengganti seleksi.");
        if(inUse(id))throw new IllegalStateException("NPC sedang dipakai.");
        gates.scan(admin,id,area,()->state.npcs().get(id)==before,gate->{
            try{edit(id,json->{json.add("gate",NpcStore.JSON.toJsonTree(gate));json.add("trigger",JsonNull.INSTANCE);},false);
                message(admin,"Gerbang tersimpan: "+gate.blocks().size()+" blok. NPC OFF sampai /vnpc "+id+" on.");
            }catch(IOException|RuntimeException e){message(admin,e.getMessage());log(e);}
        });
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
        if (gates.selecting(e.getPlayer().getUniqueId()) || com.bangzachery.vitae.vitaemanager.selection.SelectionService.selecting(e.getPlayer())) return;
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
        if(com.bangzachery.vitae.vitaemanager.selection.SelectionService.selecting(e.getPlayer()))return;
        String id = renderer.id(e.getRightClicked());
        boolean dialog = sessions.containsKey(e.getPlayer().getUniqueId());
        if (id == null && !dialog) return;
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (dialog) confirm(e.getPlayer()); else click(e.getPlayer(), id);
    }
    @EventHandler(priority = EventPriority.LOWEST) public void entityInteractAt(PlayerInteractAtEntityEvent e) { entityInteract(e); }
    @EventHandler(priority = EventPriority.LOWEST) public void virtualInteract(PlayerUseUnknownEntityEvent e) {
        if(com.bangzachery.vitae.vitaemanager.selection.SelectionService.selecting(e.getPlayer()))return;
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
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/selection/AreaScanner.java'
        OriginalHashes = @()
        Hash = 'e5482dbaec1911fb8860b720b3cf63a92d9f51b0a276734258111c83e42d6134'
        Required = $false
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.selection;

import com.bangzachery.vitae.vitaemanager.whisper.WhisperArea;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;

/** Shared main-thread budget; only the currently visited chunk stays pinned. */
public final class AreaScanner implements AutoCloseable {
    public static final class Cursor {
        private final WhisperArea area;
        private int cx,cz,x,y,z; private long visited; private boolean done;
        public Cursor(WhisperArea area){this.area=area;cx=area.minX()>>4;cz=area.minZ()>>4;first();}
        private void first(){x=Math.max(area.minX(),cx<<4);z=Math.max(area.minZ(),cz<<4);y=area.minY();}
        public int x(){return x;}public int y(){return y;}public int z(){return z;}
        public boolean done(){return done;}public long visited(){return visited;}
        public void next(){if(done)return;if(visited<Long.MAX_VALUE)visited++;if(++y<=area.maxY())return;y=area.minY();
            if(++x<=Math.min(area.maxX(),(cx<<4)+15))return;x=Math.max(area.minX(),cx<<4);
            if(++z<=Math.min(area.maxZ(),(cz<<4)+15))return;
            if(++cx>(area.maxX()>>4)){cx=area.minX()>>4;cz++;}if(cz>(area.maxZ()>>4)){done=true;return;}first();}
    }
    public static long volume(WhisperArea a){
        long n=1;for(long d:new long[]{(long)a.maxX()-a.minX()+1,(long)a.maxY()-a.minY()+1,(long)a.maxZ()-a.minZ()+1}){
            if(n>Long.MAX_VALUE/d)return Long.MAX_VALUE;n*=d;}return n;
    }
    private final JavaPlugin plugin;private final Deque<Job> jobs=new ArrayDeque<>();private final Map<Chunk,Integer> pins=new HashMap<>();private BukkitTask task;private boolean closed;
    public final class Handle {private final Job job;private Handle(Job job){this.job=job;}public Handle owned(UUID owner){job.owner=owner;return this;}public void cancel(){job.cancelled=true;jobs.remove(job);release(job);}public boolean done(){return job.done||job.cancelled;}}
    private static final class Job {
        final WhisperArea area;final Cursor cursor;final BooleanSupplier active;final BiConsumer<Long,Long> progress;
        final Consumer<Block> visit;final Runnable success;final Consumer<String> failure;
        UUID owner;Chunk pinned;CompletableFuture<Chunk> loading;long since,lastProgress;boolean done,cancelled;
        Job(WhisperArea a,BooleanSupplier active,BiConsumer<Long,Long> progress,Consumer<Block> visit,Runnable success,Consumer<String> failure){area=a;cursor=new Cursor(a);this.active=active;this.progress=progress;this.visit=visit;this.success=success;this.failure=failure;}
    }
    public AreaScanner(JavaPlugin plugin){this.plugin=plugin;}
    public void start(){task=Bukkit.getScheduler().runTaskTimer(plugin,this::tick,1,1);}
    public Handle scan(WhisperArea area,BooleanSupplier active,BiConsumer<Long,Long> progress,Consumer<Block> visit,Runnable success,Consumer<String> failure){
        if(closed)throw new IllegalStateException("Pemrosesan area sedang ditutup.");World w=Objects.requireNonNull(Bukkit.getWorld(area.world()),"World area belum dimuat.");
        if(area.minY()<w.getMinHeight()||area.maxY()>=w.getMaxHeight())throw new IllegalArgumentException("Seleksi harus di dalam ketinggian world: "+w.getMinHeight()+" sampai "+(w.getMaxHeight()-1)+".");
        Job j=new Job(area,active,progress,visit,success,failure);jobs.add(j);return new Handle(j);
    }
    public void cancelOwner(UUID owner){for(Job j:List.copyOf(jobs))if(owner.equals(j.owner)){j.cancelled=true;jobs.remove(j);release(j);j.failure.accept("Pemindaian dibatalkan admin; setup lama dipertahankan.");}}
    private void pin(Job j,Chunk c){if(pins.merge(c,1,Integer::sum)==1)c.addPluginChunkTicket(plugin);j.pinned=c;}
    private void release(Job j){if(j.pinned!=null){Chunk c=j.pinned;if(pins.compute(c,(k,n)->n==null||n<=1?null:n-1)==null)c.removePluginChunkTicket(plugin);j.pinned=null;}}
    private boolean ready(Job j,World w){
        int cx=j.cursor.x()>>4,cz=j.cursor.z()>>4;if(j.pinned!=null&&j.pinned.getX()==cx&&j.pinned.getZ()==cz)return true;
        release(j);
        if(j.loading!=null){if(!j.loading.isDone()){if(System.currentTimeMillis()-j.since>60000)throw new IllegalStateException("Chunk area belum selesai dimuat. Coba kembali setelah world siap.");return false;}
            Chunk c=j.loading.join();j.loading=null;if(c==null)throw new IllegalStateException("Chunk belum tersedia. Bangun area tersebut dahulu.");pin(j,c);return true;}
        if(w.isChunkLoaded(cx,cz)){pin(j,w.getChunkAt(cx,cz));return true;}
        j.since=System.currentTimeMillis();j.loading=w.getChunkAtAsync(cx,cz,false);return false;
    }
    private void tick(){
        int count=0,idle=0;long until=System.nanoTime()+3_000_000;
        while(!jobs.isEmpty()&&count<512&&System.nanoTime()<until){Job j=jobs.removeFirst();if(j.cancelled){release(j);continue;}
            try{if(!j.active.getAsBoolean())throw new IllegalStateException("Pemrosesan dibatalkan; setup lama dipertahankan.");World w=Bukkit.getWorld(j.area.world());if(w==null)throw new IllegalStateException("World area tidak lagi dimuat.");
                int used=0;while(!j.cursor.done()&&used<32&&count<512&&System.nanoTime()<until){if(!ready(j,w))break;j.visit.accept(w.getBlockAt(j.cursor.x(),j.cursor.y(),j.cursor.z()));j.cursor.next();used++;count++;}
                long now=System.currentTimeMillis();if(now-j.lastProgress>=1000||j.cursor.done()){j.progress.accept(j.cursor.visited(),volume(j.area));j.lastProgress=now;}
                if(j.cursor.done()){j.done=true;release(j);j.success.run();idle=0;}else{jobs.addLast(j);idle=used==0?idle+1:0;if(idle>=jobs.size())break;}
            }catch(RuntimeException e){j.cancelled=true;release(j);j.failure.accept(Objects.requireNonNullElse(e.getMessage(),"Pemrosesan area gagal."));}}
    }
    @Override public void close(){closed=true;if(task!=null)task.cancel();for(Job j:jobs){j.cancelled=true;release(j);}jobs.clear();}
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/selection/SelectionService.java'
        OriginalHashes = @()
        Hash = '6f81ed81448d53f50bf63087e9af3ca54e09086c1405c7c7e558dc3add920716'
        Required = $false
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.selection;

import com.bangzachery.vitae.vitaemanager.whisper.WhisperArea;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

/** A single temporary, owner-bound tool shared by story setup commands. */
public final class SelectionService implements Listener, AutoCloseable {
    public enum Mode { BLOCKS, WALK, VOLUME, FACE }
    @FunctionalInterface public interface Saved { void save(Player player, Location a, Location b) throws Exception; }
    private static final class Session {
        final UUID token = UUID.randomUUID();
        final String permission, label; final Mode mode; final Saved saved;
        Location a, b;
        Session(String permission, String label, Mode mode, Saved saved) {
            this.permission=permission;this.label=label;this.mode=mode;this.saved=saved;
        }
    }
    private final JavaPlugin plugin;
    private final NamespacedKey key;
    private final Map<UUID, Session> sessions=new HashMap<>();
    private final Map<UUID, Long> suppress=new HashMap<>();
    private final AreaScanner scanner;
    public SelectionService(JavaPlugin plugin) {
        this.plugin=plugin;key=new NamespacedKey(plugin,"selection_wand");scanner=new AreaScanner(plugin);
    }
    public void start() {
        Bukkit.getServicesManager().register(SelectionService.class,this,plugin,ServicePriority.Normal);
        Bukkit.getPluginManager().registerEvents(this,plugin);scanner.start();
        for(Player p:Bukkit.getOnlinePlayers())clean(p);
    }
    public static SelectionService require() {
        SelectionService s=Bukkit.getServicesManager().load(SelectionService.class);
        if(s==null)throw new IllegalStateException("Alat seleksi belum aktif. Periksa log startup.");return s;
    }
    public AreaScanner scanner(){return scanner;}
    public static boolean selecting(Player p){SelectionService s=Bukkit.getServicesManager().load(SelectionService.class);return s!=null&&s.protecting(p);}
    public void arm(Player p,String permission,String label,Mode mode,Saved saved) {
        if(!p.hasPermission(permission))throw new IllegalArgumentException("Perlu izin "+permission+".");
        if(p.getOpenInventory().getTopInventory().getType()!=InventoryType.CRAFTING)
            throw new IllegalArgumentException("Tutup GUI dahulu sebelum mengambil alat seleksi.");
        int empty=p.getInventory().firstEmpty();
        if(empty<0&&!tagged(p.getInventory().getItemInMainHand()))
            throw new IllegalArgumentException("Kosongkan satu slot inventory dahulu; item milikmu tidak diganti atau dibuang.");
        cancel(p);Session s=new Session(permission,label,mode,saved);sessions.put(p.getUniqueId(),s);
        ItemStack wand=new ItemStack(Material.PRISMARINE_SHARD);var meta=wand.getItemMeta();
        meta.displayName(Component.text("Seleksi Vitae • "+label,NamedTextColor.AQUA));
        meta.lore(List.of(Component.text("Klik kiri: pos1 • Klik kanan: pos2",NamedTextColor.YELLOW),
            Component.text("Hilang setelah kedua titik dipilih",NamedTextColor.GRAY),
            Component.text("Batal: /vitae seleksi cancel",NamedTextColor.GRAY)));
        meta.getPersistentDataContainer().set(key,PersistentDataType.STRING,s.token.toString());wand.setItemMeta(meta);
        int hand=p.getInventory().getHeldItemSlot();ItemStack old=p.getInventory().getItem(hand);
        if(old!=null&&!old.getType().isAir())p.getInventory().setItem(p.getInventory().firstEmpty(),old);
        p.getInventory().setItem(hand,wand);
        say(p,"Seleksi "+label+": klik kiri sudut pertama, klik kanan sudut seberangnya. Blok tidak dihancurkan.");
        say(p,switch(mode){case WALK->"Klik lantai di kedua sudut; ruang di atas lantai otomatis dipilih untuk pemain lewat.";
            case VOLUME->"Klik blok sudut area; seleksi juga mencakup dua blok di atasnya.";
            case FACE->"Klik sisi bingkai yang menghadap ruang kosong portal, di dua sudut diagonal bagian dalam.";
            case BLOCKS->"Klik dua blok sudut diagonal; blok yang diklik ikut dalam seleksi.";});
    }
    private boolean tagged(ItemStack item){return item!=null&&item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(key,PersistentDataType.STRING);}
    private void clean(Player p){for(int i=0;i<p.getInventory().getSize();i++)if(tagged(p.getInventory().getItem(i)))p.getInventory().setItem(i,null);if(tagged(p.getItemOnCursor()))p.setItemOnCursor(null);}
    public void cancel(Player p){sessions.remove(p.getUniqueId());clean(p);}
    public void status(Player p){Session s=sessions.get(p.getUniqueId());say(p,s==null?"Tidak ada seleksi aktif.":s.label+" • pos1 "+(s.a==null?"belum":"dipilih")+" • pos2 "+(s.b==null?"belum":"dipilih"));}
    private boolean protecting(Player p){return sessions.containsKey(p.getUniqueId())||suppress.getOrDefault(p.getUniqueId(),0L)>System.currentTimeMillis();}
    @EventHandler(priority=EventPriority.LOWEST)
    public void click(PlayerInteractEvent e){
        Player p=e.getPlayer();if(!tagged(e.getItem()))return;
        e.setCancelled(true);e.setUseInteractedBlock(Event.Result.DENY);e.setUseItemInHand(Event.Result.DENY);
        Session s=sessions.get(p.getUniqueId());
        if(s==null||!Objects.equals(e.getItem().getItemMeta().getPersistentDataContainer().get(key,PersistentDataType.STRING),s.token.toString())){clean(p);return;}
        if(e.getHand()!=EquipmentSlot.HAND)return;
        if(!p.hasPermission(s.permission)){cancel(p);say(p,"Seleksi dibatalkan karena izin admin berubah.");return;}
        if(e.getClickedBlock()==null)return;
        if(e.getAction()!=Action.LEFT_CLICK_BLOCK&&e.getAction()!=Action.RIGHT_CLICK_BLOCK)return;
        Location at=e.getClickedBlock().getLocation();
        if(s.mode==Mode.FACE){BlockFace face=e.getBlockFace();if(face==BlockFace.SELF){say(p,"Klik sisi bingkai yang menghadap bagian dalam.");return;}at.add(face.getModX(),face.getModY(),face.getModZ());}
        boolean first=e.getAction()==Action.LEFT_CLICK_BLOCK;Location other=first?s.b:s.a;
        if(other!=null&&!other.getWorld().equals(at.getWorld())){say(p,"Kedua sudut harus satu world. Seleksi masih aktif.");return;}
        if(first)s.a=at;else s.b=at;
        suppress.put(p.getUniqueId(),System.currentTimeMillis()+500);
        say(p,(first?"Pos1":"Pos2")+" = "+at.getWorld().getName()+" "+at.getBlockX()+", "+at.getBlockY()+", "+at.getBlockZ());
        p.playSound(p.getLocation(),"minecraft:block.note_block.hat",.4f,first?1:1.3f);
        if(s.a==null||s.b==null)return;
        Location a=s.a.clone(),b=s.b.clone();
        if(s.mode==Mode.WALK){int lo=Math.min(a.getBlockY(),b.getBlockY()),hi=Math.max(a.getBlockY(),b.getBlockY())+2;a.setY(lo);b.setY(hi);}
        else if(s.mode==Mode.VOLUME){int lo=Math.min(a.getBlockY(),b.getBlockY()),hi=Math.max(a.getBlockY(),b.getBlockY())+2;a.setY(lo);b.setY(hi);}
        sessions.remove(p.getUniqueId());clean(p);
        // Avoid starting GUI/snapshot work inside the interaction event.
        Bukkit.getScheduler().runTask(plugin,()->{if(!p.isOnline()||!p.hasPermission(s.permission))return;
            try{s.saved.save(p,a,b);}catch(Exception error){say(p,"Seleksi belum disimpan: "+Objects.requireNonNullElse(error.getMessage(),"periksa log server"));}});
    }
    public static WhisperArea area(Location a,Location b){if(!a.getWorld().equals(b.getWorld()))throw new IllegalArgumentException("Kedua titik harus satu world.");return WhisperArea.between(a.getWorld().getUID(),a.getBlockX(),a.getBlockY(),a.getBlockZ(),b.getBlockX(),b.getBlockY(),b.getBlockZ());}
    public static void say(Player p,String text){p.sendMessage(Component.text("[Vitae Seleksi] ",NamedTextColor.GOLD).append(Component.text(text,NamedTextColor.AQUA)));}
    @EventHandler(priority=EventPriority.HIGHEST)public void preventUse(PlayerInteractEvent e){if(tagged(e.getItem())){e.setCancelled(true);e.setUseInteractedBlock(Event.Result.DENY);e.setUseItemInHand(Event.Result.DENY);}}
    @EventHandler(priority=EventPriority.HIGHEST)public void damage(BlockDamageEvent e){if(protecting(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST)public void breaking(BlockBreakEvent e){if(protecting(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST)public void place(BlockPlaceEvent e){if(tagged(e.getItemInHand()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST)public void drop(PlayerDropItemEvent e){if(tagged(e.getItemDrop().getItemStack()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST)public void swap(PlayerSwapHandItemsEvent e){if(tagged(e.getMainHandItem())||tagged(e.getOffHandItem()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST)public void inventory(InventoryClickEvent e){if(e.getWhoClicked() instanceof Player p&&(sessions.containsKey(p.getUniqueId())||tagged(e.getCurrentItem())||tagged(e.getCursor())))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST)public void drag(InventoryDragEvent e){if(e.getWhoClicked() instanceof Player p&&(sessions.containsKey(p.getUniqueId())||tagged(e.getOldCursor())))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST)public void interactEntity(PlayerInteractEntityEvent e){if(tagged(e.getPlayer().getInventory().getItemInMainHand()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.LOWEST)public void attack(EntityDamageByEntityEvent e){if(e.getDamager() instanceof Player p&&tagged(p.getInventory().getItemInMainHand()))e.setCancelled(true);}
    @EventHandler public void quit(PlayerQuitEvent e){cancel(e.getPlayer());scanner.cancelOwner(e.getPlayer().getUniqueId());suppress.remove(e.getPlayer().getUniqueId());}
    @EventHandler public void join(PlayerJoinEvent e){clean(e.getPlayer());}
    @EventHandler public void death(PlayerDeathEvent e){e.getDrops().removeIf(this::tagged);cancel(e.getEntity());}
    @Override public void close(){for(Player p:Bukkit.getOnlinePlayers())cancel(p);sessions.clear();suppress.clear();scanner.close();HandlerList.unregisterAll(this);Bukkit.getServicesManager().unregister(SelectionService.class,this);}
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/story/DungeonCommand.java'
        OriginalHashes = @('bb860484e9371600e6ac58a3079202d309b0a5462f671f7c41813d4e2bcdd8db')
        Hash = '1685e2118fcd1e999d6649790e527bd3e52cda87b531604cfdcf4a9c7dbb2e8d'
        Required = $true
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
    private static final List<String> OPS=List.of("wand","pos1","pos2","region","location","bad","spawn","boss","npc","reward","time","fade","check","on","off","open","stop","reset","status","join","receipt");
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
                case "wand"->{
                    if(args.length<3||args.length>4)throw new IllegalArgumentException("/vdungeon <id> wand <area> | wand bad <nama-petak>");
                    String kind=args[2].toLowerCase(Locale.ROOT);String tile=kind.equals("bad")&&args.length==4?NpcData.id(args[3]):"";
                    if(kind.equals("bad")?tile.isEmpty():!List.of("entrance","arena","camera","finish","miniongate","miniontrigger","bossgate","bosstrigger","exit").contains(kind))throw new IllegalArgumentException("Pilih nama area atau bad <nama-petak>.");
                    if(!kind.equals("bad")&&args.length!=3)throw new IllegalArgumentException("/vdungeon <id> wand <area>");
                    service.assertEditable(id);
                    var mode=kind.equals("exit")?com.bangzachery.vitae.vitaemanager.selection.SelectionService.Mode.FACE:
                        Set.of("entrance","camera","finish","miniontrigger","bosstrigger").contains(kind)?com.bangzachery.vitae.vitaemanager.selection.SelectionService.Mode.WALK:com.bangzachery.vitae.vitaemanager.selection.SelectionService.Mode.BLOCKS;
                    com.bangzachery.vitae.vitaemanager.selection.SelectionService.require().arm(player(sender),"vitae.admin.dungeon",id+" • "+kind,mode,(p,a,b)->{
                        selections.put(p.getUniqueId(),new Selection(a,b));
                        execute(p,kind.equals("bad")?new String[]{id,"bad","add",tile}:new String[]{id,"region",kind});
                    });
                }
                case "pos1","pos2"->{arity(args,2,"/vdungeon <id> pos1|pos2");Player p=player(sender);Selection old=selections.getOrDefault(p.getUniqueId(),new Selection(null,null));var hit=p.getTargetBlockExact(6);Location at=hit==null?p.getLocation():hit.getLocation();selections.put(p.getUniqueId(),op.equals("pos1")?new Selection(at.clone(),old.b):new Selection(old.a,at.clone()));tell(sender,op+" = "+at.getBlockX()+", "+at.getBlockY()+", "+at.getBlockZ()+". Arahkan crosshair ke blok untuk memilih blok; lihat udara untuk memilih posisi kaki.");}
                case "region"->{arity(args,3,"/vdungeon <id> region entrance|arena|camera|finish|miniongate|miniontrigger|bossgate|bosstrigger|exit");Box b=selection(player(sender));service.edit(id,d->{switch(args[2].toLowerCase(Locale.ROOT)){case "entrance"->d.entrance=b;case "arena"->d.arena=b;case "camera"->d.cameraZone=b;case "finish"->d.finish=b;case "miniongate"->d.minionGate=b;case "miniontrigger"->d.minionTrigger=b;case "bossgate"->d.bossGate=b;case "bosstrigger"->d.bossTrigger=b;case "exit"->d.exit=b;default->throw new IllegalArgumentException("Jenis area tidak dikenal. /vdungeon help portal");}});tell(sender,"Area "+args[2]+" tersimpan ("+b.volume()+" blok). Dungeon OFF sampai setup selesai.");}
                case "location"->{arity(args,3,"/vdungeon <id> location entry|home|start|camera|next");NpcData.Point at=point(player(sender).getLocation());service.edit(id,d->{switch(args[2].toLowerCase(Locale.ROOT)){case "entry"->d.entry=at;case "home"->d.home=at;case "start"->d.start=at;case "camera"->d.camera=at;case "next"->d.next=at;default->throw new IllegalArgumentException("Jenis lokasi tidak dikenal.");}});tell(sender,"Lokasi "+args[2]+" dan arah pandangnya tersimpan.");}
                case "bad"->{arity(args,4,"/vdungeon <id> bad add|remove <nama-petak>");String tile=NpcData.id(args[3]);Box b=args[2].equalsIgnoreCase("add")?selection(player(sender)):null;service.edit(id,d->{if(args[2].equalsIgnoreCase("add")){if(b.y1()!=b.y2())throw new IllegalArgumentException("Seleksi petak kaca harus satu lapis.");d.bad.put(tile,b);}else if(args[2].equalsIgnoreCase("remove"))d.bad.remove(tile);else throw new IllegalArgumentException("bad add|remove");});tell(sender,"Pijakan salah "+tile+" diperbarui.");}
                case "spawn"->{if(args.length==4&&args[2].equalsIgnoreCase("remove")){service.edit(id,d->d.minions.remove(args[3]));tell(sender,"Spawn dihapus.");break;}
                    if(args.length<5||args.length>6)throw new IllegalArgumentException("/vdungeon <id> spawn <nama-titik> vanilla:COW|mythic:ID <jumlah> [level]");String name=NpcData.id(args[2]);Mob m=new Mob(args[3],number(args[4],1,4096),args.length==6?Double.parseDouble(args[5]):1,point(player(sender).getLocation()));service.edit(id,d->d.minions.put(name,m));tell(sender,"Spawn "+name+" tersimpan. Total minion: "+service.get(id).minions.values().stream().mapToInt(Mob::count).sum());}
                case "boss"->{if(args.length<3||args.length>4)throw new IllegalArgumentException("/vdungeon <id> boss mythic:ID|vanilla:TYPE [level]");Mob m=new Mob(args[2],1,args.length==4?Double.parseDouble(args[3]):1,point(player(sender).getLocation()));service.edit(id,d->d.boss=m);tell(sender,"Lokasi dan jenis bos tersimpan.");}
                case "npc"->{if(args.length<4||args.length>5)throw new IllegalArgumentException("/vdungeon <id> npc glass|minion|boss <id-npc> [halaman-selamat]");String npc=NpcData.id(args[3]),page=args.length==5?NpcData.id(args[4]):"";service.edit(id,d->{switch(args[2].toLowerCase(Locale.ROOT)){case "glass"->{d.glassNpc=npc;d.glassPage=page;}case "minion"->{d.minionNpc=npc;d.minionPage=page;}case "boss"->{d.bossNpc=npc;d.bossPage=page;}default->throw new IllegalArgumentException("Tahap glass|minion|boss.");}});tell(sender,"NPC "+npc+" dihubungkan ke "+args[2]+". Buat pilihan DUNGEON_"+args[2].toUpperCase(Locale.ROOT)+" di dialog /vnpc.");}
                case "reward"->{arity(args,3,"/vdungeon <id> reward glass|minion|boss");service.rewardMenu(player(sender),id,args[2].toLowerCase(Locale.ROOT));}
                case "time"->{arity(args,4,"/vdungeon <id> time glass|escape|portal <detik>");int n=number(args[3],1,3600);service.edit(id,d->{switch(args[2].toLowerCase(Locale.ROOT)){case "glass"->d.glassSeconds=n;case "escape"->d.escapeSeconds=n;case "portal"->d.portalAfter=n;default->throw new IllegalArgumentException("Waktu glass|escape|portal.");}});tell(sender,"Waktu "+args[2]+" = "+n+" detik. Default: kaca 180, keluar 30, portal setelah 15.");}
                case "fade"->{arity(args,4,"/vdungeon <id> fade entry|exit on|off");if(!Set.of("on","off").contains(args[3].toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Pilih on atau off.");boolean on=args[3].equalsIgnoreCase("on");service.edit(id,d->{if(args[2].equalsIgnoreCase("entry"))d.fadeEntry=on;else if(args[2].equalsIgnoreCase("exit"))d.fadeExit=on;else throw new IllegalArgumentException("Fade hanya entry|exit.");});tell(sender,"Fade "+args[2]+" "+args[3]+". Sakelar utama tetap /vitae fade on|off.");}
                case "check"->{arity(args,2,"/vdungeon <id> check");service.prepare(sender,id,"check");}
                case "on","off"->{arity(args,2,"/vdungeon <id> on|off");if(op.equals("on"))service.prepare(sender,id,"on");else{service.enable(id,false);tell(sender,"Dungeon OFF.");}}
                case "open"->{arity(args,2,"/vdungeon <id> open");service.prepare(sender,id,"open");}
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
            case "kaca"->{tell(s,"<id> region arena|camera|finish • <id> location start|camera|next");tell(s,"Posisi start wajib di area pemicu camera. Lokasi camera merekam posisi, yaw, dan pitch admin; durasi 5 detik.");tell(s,"Pilih dua sudut petak salah satu lapis, ukuran bebas: <id> bad add <nama> • bad remove <nama>");tell(s,"<id> time glass 180 • <id> npc glass <npc> [page-selamat] • target dialog DUNGEON_GLASS");}
            case "minion"->{tell(s,"<id> region miniongate|miniontrigger • <id> npc minion <npc> [page-selamat]");tell(s,"Di lokasi spawn: <id> spawn <nama-titik> mythic:ID|vanilla:COW <jumlah> [level]. Ulangi untuk tiap titik.");tell(s,"Target dialog DUNGEON_MINION. Progres semua peserta sama; hanya mob sesi yang dihitung.");}
            case "bos"->{tell(s,"<id> region bossgate|bosstrigger • <id> boss mythic:ID [level]");tell(s,"<id> npc boss <npc> [page-selamat]. Target dialog DUNGEON_BOSS. Syarat kaca dan minion selesai.");}
            case "hadiah"->{tell(s,"<id> reward glass|minion|boss → letakkan item dalam GUI → tutup untuk simpan; item admin dikembalikan.");tell(s,"Halaman selamat memakai teks dari NPC; pilihan Ambil hadiah diberikan otomatis untuk penerima terpilih.");tell(s,"Belum pernah menerima diprioritaskan; jika tidak ada, peserta sebelumnya boleh mendapat lagi.");}
            case "aksi"->{tell(s,"action <id> <npc> teleport here • action <id> <npc> spawn mythic:ID|vanilla:TYPE <jumlah> [level]");tell(s,"/vnpc <npc> option <halaman> ACTION_<id> <label pilihan>. Aksi ini bisa digunakan di luar dungeon.");}
            case "sesi"->{tell(s,"<id> check → on → open. Saat satu peserta menerima kaca, semua peserta aktif di world mengikuti.");tell(s,"<id> status|stop|reset • list • <id> join [pemain]. Pemain mati/disconnect saat permainan tidak bisa masuk lagi.");tell(s,"Reset memulihkan blok bertahap. Setelah selesai, open membuat sesi baru.");}
            default->{tell(s,"/vdungeon create <id> <world> • /vdungeon list");tell(s,"/vdungeon help portal|kaca|minion|bos|hadiah|aksi|sesi");tell(s,"Urutan: world/portal → kaca/kamera → NPC/minion/bos → hadiah → check → on → open.");}
        }
        tell(s,"Alat seleksi: /vdungeon <id> wand <area> • klik kiri pos1, klik kanan pos2; otomatis tersimpan.");
        s.sendMessage(Component.empty());
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){
        if(!sender.hasPermission("vitae.admin.dungeon"))return List.of();List<String> values=List.of();
        if(args.length==1){var v=new ArrayList<>(service.current().definitions.keySet());v.addAll(List.of("help","create","list","action"));values=v;}
        else if(args.length==2)values=args[0].equalsIgnoreCase("help")?TOPICS:OPS;
        else if(args.length==3)values=switch(args[1].toLowerCase(Locale.ROOT)){case "wand"->List.of("entrance","arena","camera","finish","miniongate","miniontrigger","bossgate","bosstrigger","exit","bad");case "region"->List.of("entrance","arena","camera","finish","miniongate","miniontrigger","bossgate","bosstrigger","exit");case "location"->List.of("entry","home","start","camera","next");case "reward","npc","receipt"->List.of("glass","minion","boss");case "bad"->List.of("add","remove");case "time"->List.of("glass","escape","portal");case "fade"->List.of("entry","exit");default->List.of();};
        else if(args.length==4&&args[1].equalsIgnoreCase("fade"))values=List.of("on","off");
        String prefix=args[args.length-1].toLowerCase(Locale.ROOT);return values.stream().filter(v->v.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/story/DungeonData.java'
        OriginalHashes = @('634488fce5589167a209de38bec45b88775cf78863cd8c4670b0b52d89f72de0')
        Hash = '79f6467227dfe5479558bc37c05ad9aeb0d430cda1fa6b3da283c41aad721815'
        Required = $true
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
        public long volume() { return com.bangzachery.vitae.vitaemanager.selection.AreaScanner.volume(com.bangzachery.vitae.vitaemanager.whisper.WhisperArea.between(world,x1,y1,z1,x2,y2,z2)); }
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
            Object[] required={entrance,arena,cameraZone,finish,minionGate,minionTrigger,bossGate,bossTrigger,exit,entry,home,start,camera,next,boss};
            String[] names={"region entrance","region arena","region camera","region finish","region miniongate","region miniontrigger","region bossgate","region bosstrigger","region exit","location entry","location home","location start","location camera","location next","boss"};
            var missing=new ArrayList<String>();for(int i=0;i<required.length;i++)if(required[i]==null)missing.add(names[i]);
            if(!missing.isEmpty())throw new IllegalArgumentException("Setup belum lengkap: "+String.join(", ",missing));
            for(Box b:List.of(arena,cameraZone,finish,minionGate,minionTrigger,bossGate,bossTrigger,exit)) if(!world.equals(b.world))throw new IllegalArgumentException("Area permainan harus berada di world dungeon.");
            for(NpcData.Point p:List.of(entry,start,camera,next,boss.point()))if(!world.equals(p.world()))throw new IllegalArgumentException("Lokasi permainan harus berada di world dungeon.");
            if(world.equals(home.world()))throw new IllegalArgumentException("Lokasi home harus di luar world dungeon.");
            if(!cameraZone.contains(location(start)))throw new IllegalArgumentException("Area kamera harus mencakup posisi start agar pengenalan dimulai serentak.");
            if(!arena.contains(cameraZone) || !arena.contains(finish) || !arena.contains(location(start)))throw new IllegalArgumentException("Start, kamera pemicu, dan finish harus di dalam arena kaca.");
            if(minionGate.overlaps(bossGate) || exit.overlaps(minionGate) || exit.overlaps(bossGate))throw new IllegalArgumentException("Gerbang dan portal keluar tidak boleh bertumpuk.");
            if(bad.isEmpty() || minions.isEmpty() || minions.values().stream().mapToLong(Mob::count).sum()>8192)throw new IllegalArgumentException("Isi pijakan salah dan spawn minion (total maks. 8192). Batas ini hanya mob aktif, bukan luas world.");
            for(Box b:bad.values()) if(!arena.contains(b) || b.y1!=b.y2 || b.overlaps(finish))throw new IllegalArgumentException("Pijakan salah harus satu lapis kaca di arena dan tidak menimpa finish.");
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
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/story/DungeonService.java'
        OriginalHashes = @('cde250a97216bb4e7e214f5d6ff2f6b1ee050697fa28a53300bfa7608b952794')
        Hash = 'a020dac17e3992149b1203a479cd5e5a20a98cb686e12d41c4e2e476249df0c2'
        Required = $true
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
    private final Map<String,com.bangzachery.vitae.vitaemanager.selection.AreaScanner.Handle> scans=new HashMap<>();
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
    private State draft(){State next=new State();next.version=state.version;
        state.definitions.forEach((id,d)->next.definitions.put(id,StoryStore.JSON.fromJson(StoryStore.JSON.toJson(d),Definition.class)));
        next.runs=new LinkedHashMap<>(state.runs);next.actions=new LinkedHashMap<>(state.actions);next.returns=new LinkedHashMap<>(state.returns);return next;}
    private void persist(){try{save();}catch(IOException e){throw new IllegalStateException("State dungeon belum tersimpan. Permainan dihentikan agar progres tidak rusak.",e);}}
    public void create(String id,World world)throws IOException{
        if(state.definitions.containsKey(id))throw new IllegalArgumentException("ID sudah digunakan.");
        State next=draft();next.definitions.put(id,new Definition(id,world.getUID()));store.save(next);state=next;
    }
    public void assertEditable(String id){
        if(closed)throw new IllegalStateException("Modul dungeon sudah dimuat ulang. Ambil alat seleksi baru.");
        get(id);Run r=state.runs.get(id);
        if(scans.containsKey(id)||(r!=null&&(r.active()||r.restoring||outstanding.containsKey(id))))throw new IllegalStateException("Reset/tunggu sesi atau pemindaian selesai sebelum mengubah setup.");
    }
    public void edit(String id,Consumer<Definition> change)throws IOException{
        assertEditable(id);
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
        // Block validation is performed by prepare() with a per-tick budget.
        for(Mob m:d.minions.values())mobs.validate(m);mobs.validate(d.boss);
        for(List<String> items:d.rewards.values())for(String item:items)NpcService.decode(item);
    }
    private void safe(NpcData.Point point){Location l=location(point);if(l.getY()<l.getWorld().getMinHeight()||l.getY()+1>=l.getWorld().getMaxHeight()||!l.getBlock().isPassable()||!l.clone().add(0,1,0).getBlock().isPassable())throw new IllegalArgumentException("Lokasi teleport terhalang: "+l.getBlockX()+","+l.getBlockY()+","+l.getBlockZ());}
    private NpcService npc(){NpcService service=NpcModule.active(plugin);if(service==null)throw new IllegalStateException("Modul NPC belum aktif. /vnpc status");return service;}
    private record ScanPart(Box box,String kind){}
    private static com.bangzachery.vitae.vitaemanager.whisper.WhisperArea area(Box b){return com.bangzachery.vitae.vitaemanager.whisper.WhisperArea.between(b.world(),b.x1(),b.y1(),b.z1(),b.x2(),b.y2(),b.z2());}
    public void prepare(CommandSender sender,String id,String operation){
        Definition d=get(id);check(d);
        Run previous=state.runs.get(id);if(previous!=null&&(previous.active()||previous.restoring))throw new IllegalStateException("Reset sesi sebelum memeriksa/membuka setup kembali.");
        if(scans.containsKey(id))throw new IllegalStateException("Pemindaian masih berjalan. Tunggu pesan selesai atau /vitae seleksi cancel.");
        if(operation.equals("open")&&!d.enabled)throw new IllegalStateException("Dungeon belum ON.");
        var todo=new ArrayDeque<ScanPart>();for(Box box:d.bad.values())todo.add(new ScanPart(box,"kaca"));
        todo.add(new ScanPart(d.minionGate,"gerbang minion"));todo.add(new ScanPart(d.bossGate,"gerbang bos"));todo.add(new ScanPart(d.exit,"portal keluar"));
        tell(sender,"Memeriksa blok area bertahap. Tunggu pesan selesai sebelum command berikutnya.");
        scanNext(sender,d,operation,todo,new LinkedHashMap<>());
    }
    private void scanNext(CommandSender sender,Definition d,String operation,ArrayDeque<ScanPart> todo,Map<String,Block> snapshot){
        if(todo.isEmpty()){
            scans.remove(d.id);
            try{check(d);if(operation.equals("on")){enable(d.id,true);tell(sender,"Dungeon ON. Berikutnya /vdungeon "+d.id+" open.");}
                else if(operation.equals("open")){Run next=new Run(d.id);next.blocks.addAll(snapshot.values());State copy=draft();copy.runs.put(d.id,next);store.save(copy);state=copy;tell(sender,"Sesi dibuka. Peserta masuk melalui portal.");}
                else tell(sender,"Setup valid. Berikutnya /vdungeon "+d.id+" on lalu open.");
            }catch(Exception error){tell(sender,"Pemeriksaan gagal: "+error.getMessage());}return;
        }
        ScanPart part=todo.removeFirst();
        try{
            var handle=com.bangzachery.vitae.vitaemanager.selection.SelectionService.require().scanner().scan(area(part.box),
                ()->!closed&&state.definitions.get(d.id)==d&&(!(sender instanceof Player p)||p.isOnline()&&p.hasPermission("vitae.admin.dungeon")),
                (read,total)->{if(sender instanceof Player p)p.sendActionBar(Component.text("Memeriksa "+part.kind+": "+read+"/"+total,NamedTextColor.AQUA));},
                block->{Material type=block.getType();if(block.getState() instanceof TileState)throw new IllegalArgumentException(part.kind+" memuat blok penyimpan data.");
                    if(part.kind.equals("kaca")&&!type.name().endsWith("GLASS"))throw new IllegalArgumentException("Petak salah harus berupa blok kaca penuh.");
                    if(part.kind.equals("portal keluar")&&!type.isAir())throw new IllegalArgumentException("Bagian dalam portal keluar harus kosong/udara.");
                    if(part.kind.startsWith("gerbang")&&Set.of(Material.BEDROCK,Material.BARRIER,Material.END_PORTAL_FRAME,Material.REINFORCED_DEEPSLATE).contains(type))throw new IllegalArgumentException("Gerbang memuat blok khusus.");
                    if(operation.equals("open")){Block cell=new Block(block.getWorld().getUID(),block.getX(),block.getY(),block.getZ(),block.getBlockData().getAsString());snapshot.put(cell.key(),cell);}},
                ()->scanNext(sender,d,operation,todo,snapshot),
                reason->{scans.remove(d.id);if(sender instanceof Player p)p.sendActionBar(Component.empty());tell(sender,reason);});
            if(sender instanceof Player p)handle.owned(p.getUniqueId());scans.put(d.id,handle);
        }catch(RuntimeException e){scans.remove(d.id);tell(sender,e.getMessage());}
    }
    private Run run(Definition d){Run r=state.runs.get(d.id);if(r==null||!r.active())throw new IllegalStateException("Sesi belum dibuka. /vdungeon "+d.id+" open");return r;}
    public void join(Player p,String id){
        Definition d=get(id);if(!d.enabled)throw new IllegalStateException("Dungeon belum aktif.");Run r=run(d);
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
        var scan=scans.remove(id);if(scan!=null)scan.cancel();
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
        if(tile.volume()<=128){for(int x=tile.x1();x<=tile.x2();x++)for(int z=tile.z1();z<=tile.z2();z++)p.getWorld().getBlockAt(x,tile.y1(),z).setType(Material.AIR,false);}
        else{for(int x=Math.max(tile.x1(),p.getLocation().getBlockX()-1);x<=Math.min(tile.x2(),p.getLocation().getBlockX()+1);x++)for(int z=Math.max(tile.z1(),p.getLocation().getBlockZ()-1);z<=Math.min(tile.z2(),p.getLocation().getBlockZ()+1);z++)p.getWorld().getBlockAt(x,tile.y1(),z).setType(Material.AIR,false);breakArea(r,tile);}
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
        r.broken.add("exit-open");persist();
        for(Block block:r.blocks)if(d.exit.contains(block.location())&&Bukkit.createBlockData(block.data()).getMaterial().isAir())worldWork.add(new WorldJob(d.id,()->block.location().getBlock().setType(Material.END_GATEWAY,false)));
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
        closed=true;for(var scan:scans.values())scan.cancel();scans.clear();if(task!=null)task.cancel();for(String id:List.copyOf(bars.keySet()))removeBar(id);
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
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperArea.java'
        OriginalHashes = @('fcf8de50ca17192864ffd343980aa421ce91efab90740c85f5f7849dd57b3896')
        Hash = '68951677d9c7a507cb01b417530229d8b08d46523880fc77b0c8ea2b6856c7fb'
        Required = $true
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.whisper;

import java.util.Objects;
import java.util.UUID;

/** Inclusive block corners; containment uses the whole selected blocks, not their centers. */
public record WhisperArea(UUID world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public WhisperArea {
        Objects.requireNonNull(world);
        if (minX > maxX || minY > maxY || minZ > maxZ
                || minX < -30_000_000 || maxX > 30_000_000 || minZ < -30_000_000 || maxZ > 30_000_000
                || minY < -4096 || maxY > 4096) {
            throw new IllegalArgumentException("Area tidak valid; periksa urutan sudut dan koordinat dunia.");
        }
    }

    public static WhisperArea between(UUID world, int ax, int ay, int az, int bx, int by, int bz) {
        return new WhisperArea(world, Math.min(ax, bx), Math.min(ay, by), Math.min(az, bz),
                Math.max(ax, bx), Math.max(ay, by), Math.max(az, bz));
    }

    public boolean contains(UUID worldId, double x, double y, double z) {
        return world.equals(worldId) && finite(x, y, z)
                && x >= minX && x < (double) maxX + 1 && y >= minY && y < (double) maxY + 1
                && z >= minZ && z < (double) maxZ + 1;
    }

    /** Swept movement also catches walking completely across a one-block trigger in one update. */
    public boolean crossed(UUID worldId, double ax, double ay, double az, double bx, double by, double bz) {
        if (!world.equals(worldId) || !finite(ax, ay, az) || !finite(bx, by, bz)
                || contains(worldId, ax, ay, az)) return false;
        double lo = 0, hi = 1;
        double[] from = {ax, ay, az}, to = {bx, by, bz};
        double[] min = {minX, minY, minZ}, max = {maxX + 1.0, maxY + 1.0, maxZ + 1.0};
        for (int axis = 0; axis < 3; axis++) {
            double delta = to[axis] - from[axis];
            if (delta == 0) {
                if (from[axis] < min[axis] || from[axis] >= max[axis]) return false;
            } else {
                double enter = (min[axis] - from[axis]) / delta;
                double exit = (max[axis] - from[axis]) / delta;
                lo = Math.max(lo, Math.min(enter, exit));
                hi = Math.min(hi, Math.max(enter, exit));
                if (lo > hi) return false;
            }
        }
        // Test an interior point; touching the exclusive upper face does not count as entering.
        double t = (lo + hi) / 2;
        return contains(worldId, ax + (bx - ax) * t, ay + (by - ay) * t, az + (bz - az) * t);
    }

    private static boolean finite(double x, double y, double z) {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
    }
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperService.java'
        OriginalHashes = @('fb086397eac289cf069f9ea9613c9cfd25898b3641affd6da0bc8e5698e19d9c')
        Hash = '0b55f98dc43aecde255ea76f79879d87b9d90adc926bd0e483a565015ff5eb9c'
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
    public void wand(Player p, String id) {
        current.require(id);
        com.bangzachery.vitae.vitaemanager.selection.SelectionService.require().arm(p,"vitae.admin.whisper","Bisikan " + id,
            com.bangzachery.vitae.vitaemanager.selection.SelectionService.Mode.WALK,(admin,a,b)->{
                var area=com.bangzachery.vitae.vitaemanager.selection.SelectionService.area(a,b);
                change(id,state->state.put(state.require(id).withArea(area)),text->com.bangzachery.vitae.vitaemanager.selection.SelectionService.say(admin,text));
            });
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
        Name = 'src/test/java/com/bangzachery/vitae/vitaemanager/npc/NpcGateDataTest.java'
        OriginalHashes = @('f1993f6c98e90f033277fb49ac1689410daa572f9cf71b6783df3badf395cae7')
        Hash = 'e57be3a92141c0d1f436fd99028dafc4b903ec84341cfc6d78c8d560b13fe196'
        Required = $true
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
        assertDoesNotThrow(() -> new Gate(new WhisperArea(WORLD, 0, 64, 0, 16, 80, 0), List.of(block), GateStatus.CLOSED));
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
        Name = 'src/test/java/com/bangzachery/vitae/vitaemanager/whisper/WhisperAreaTest.java'
        OriginalHashes = @('c3c658a2a55a4b2e0ddb482bd722f69edc731f40a53c1a8f50144f11a89bcdd8')
        Hash = '94a87ae690f43df409b1a614d1a5962b55dded7a410f3e3b227684c48f701eb8'
        Required = $true
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.whisper;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class WhisperAreaTest {
    private final UUID world = UUID.randomUUID();
    @Test void reversedCornersIncludeWholeBlocksAndRespectWorldAndUpperFace() {
        var area = WhisperArea.between(world, 5, 70, 8, -2, 64, 3);
        assertTrue(area.contains(world, -2, 64, 3));
        assertTrue(area.contains(world, 5.99, 70.99, 8.99));
        assertFalse(area.contains(world, 6, 70, 8));
        assertFalse(area.contains(world, -2.01, 64, 3));
        assertFalse(area.contains(UUID.randomUUID(), 0, 65, 4));
        assertFalse(area.contains(world, Double.NaN, 65, 4));
    }
    @Test void equalCornersMakeAOneBlockAreaAndSweptMovementCannotSkipIt() {
        var area = WhisperArea.between(world, 0, 64, 0, 0, 64, 0);
        assertTrue(area.crossed(world, -3, 64.5, 0.5, 3, 64.5, 0.5));
        assertTrue(area.crossed(world, -3, 64.5, 0.5, 0.5, 64.5, 0.5));
        assertFalse(area.crossed(world, 0.5, 64.5, 0.5, 3, 64.5, 0.5));
        assertFalse(area.crossed(world, -3, 65, 0.5, 3, 65, 0.5));
        assertFalse(area.crossed(world, -3, 66, 0.5, 3, 66, 0.5));
        assertFalse(area.crossed(UUID.randomUUID(), -3, 64.5, 0.5, 3, 64.5, 0.5));
    }
    @Test void negativeCoordinatesAndVerticalEntryAreSupported() {
        var area = WhisperArea.between(world, -2, -10, -3, -2, -10, -3);
        assertTrue(area.contains(world, -1.5, -9.5, -2.5));
        assertTrue(area.crossed(world, -1.5, -12, -2.5, -1.5, -8, -2.5));
        assertFalse(area.crossed(world, Double.POSITIVE_INFINITY, 0, 0, 0, 0, 0));
    }
    @Test void oversizedAndOverflowingSelectionsAreRejected() {
        assertDoesNotThrow(() -> WhisperArea.between(world, 0, 0, 0, 256, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> WhisperArea.between(world, Integer.MIN_VALUE, 0, 0, Integer.MAX_VALUE, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new WhisperArea(world, 2, 0, 0, 1, 0, 0));
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
if ($changesVitae.Count -eq 0) { Write-Host 'Update seleksi area sudah terpasang. Tidak ada file yang ditulis ulang.' -ForegroundColor Green; return }
$backupVitae = Join-Path $rootVitae ('vitae-selection-backup-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [Guid]::NewGuid().ToString('N').Substring(0,8))
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
Write-Host "Update seleksi area terpasang: $($changesVitae.Count) file." -ForegroundColor Green
Write-Host "Backup sumber lama: $backupVitae" -ForegroundColor Cyan
Write-Host 'Berikutnya jalankan: .\gradlew.bat clean build' -ForegroundColor Yellow
Write-Host 'Saat mengganti JAR server, pertahankan folder data plugins/vitaemanager.' -ForegroundColor Yellow
```
<!-- VITAE_INSTALLER_END -->
