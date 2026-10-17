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
        OriginalHashes = @('40511e87d0e399dc017dad8ba111d82ee5841da593fc2edf22a211c04aed8406')
        Hash = '835eb3936c9858b6d2db90ff505d4a5bad10b5a3c5461c0e6c4048873c5aca43'
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
    private com.bangzachery.vitae.vitaemanager.music.MusicService music;

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
            music = new com.bangzachery.vitae.vitaemanager.music.MusicService(this);
            music.start();
            var musicExecutor = new com.bangzachery.vitae.vitaemanager.command.MusicCommand(music);
            executor.music(musicExecutor);
            PluginCommand musicCommand = getCommand("music");
            if (musicCommand == null) throw new IllegalStateException("Command music tidak terdaftar di plugin.yml");
            musicCommand.setExecutor(musicExecutor);
            musicCommand.setTabCompleter(musicExecutor);

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
        if (music != null) music.close();
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
        OriginalHashes = @('6d6b950dd16c3ac8dccc1e79a12a553cf1ebb3211366c1192f2b9cda3d63ff63')
        Hash = 'dc80e8e18a1a17c9a95d1a7d25ce085f317412fb65e3c94289ca88a3247897f6'
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
    private MusicCommand music;
    public void music(MusicCommand music) { this.music = music; }

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
            if (sender.hasPermission("vitae.admin.music")) sender.sendMessage(Component.text("Musik pribadi: /vitae music help • Pemain: /music help", NamedTextColor.AQUA));
            if (sender.hasPermission("vitae.admin.whisper")) sender.sendMessage(messages.component("prefix")
                    .append(Component.text("Bisikan Area: /vitae bisikan help", NamedTextColor.AQUA)));
            return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("music")) {
            if (music == null) { sender.sendMessage(Component.text("Modul musik belum aktif.", NamedTextColor.RED)); return true; }
            return music.admin(sender, java.util.Arrays.copyOfRange(args, 1, args.length));
        }
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
        if (args.length > 1 && music != null && args[0].equalsIgnoreCase("music"))
            return music.completeAdmin(sender, java.util.Arrays.copyOfRange(args, 1, args.length));
        if (args.length > 1 && story != null && List.of("fade", "dungeon").contains(args[0].toLowerCase(Locale.ROOT)))
            return story.complete(sender, args[0].toLowerCase(Locale.ROOT), java.util.Arrays.copyOfRange(args, 1, args.length));
        if (args.length > 1 && args[0].equalsIgnoreCase("bisikan"))
            return whispers.complete(sender, java.util.Arrays.copyOfRange(args, 1, args.length));
        List<String> choices = new ArrayList<>();
        if (args.length == 1) {
            choices.addAll(List.of("help", "panel", "seleksi"));
            if (sender.hasPermission("vitae.admin.music")) choices.add("music");
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
        Name = 'src/main/resources/plugin.yml'
        OriginalHashes = @('b9a7c03e02b041a135d1b28464bf0165eada7532ef416d2c188999d2d7c2a171')
        Hash = 'e60aa994c0d75d5c334698240c5d465b504b70f385e98a62f35bba7cf9e2a7b8'
        Required = $true
        Binary = $false
        Source = @'
name: vitaemanager
version: '${version}'
main: com.bangzachery.vitae.vitaemanager.Vitaemanager
api-version: '1.21.4'
authors: [Bang Zachery]
description: Core roleplay Vitae - remake bertahap
softdepend: [Essentials, AuraSkills, ModelEngine, Shopkeepers, MythicMobs, ItemsAdder, Multiverse-Core]

commands:
  music:
    description: Musik pribadi dan playlist Vitae
    usage: /music help

  vdungeon:
    description: Setup dungeon cerita bersama Vitae
    usage: /vdungeon help
    aliases: [vitaedungeon]
  evo:
    description: Ritual evolusi senjata Vitae
    usage: /evo help
    aliases: [ritual]

  vitae:
    description: Perintah utama administrator Vitae
    usage: /vitae help

  adminpanel:
    description: Buka panel kontrol administrator
    usage: /adminpanel
    aliases: [panel]

  viti:
    description: Saldo dan transaksi Viti
    usage: /viti help
    aliases: [uang, money]

  me:
    description: Teks aksi roleplay di atas pemain
    usage: /me aksi

  do:
    description: Teks kondisi roleplay di atas pemain
    usage: /do kondisi

  ooc:
    description: Obrolan luar roleplay lokal
    usage: /ooc pesan

  oocg:
    description: Obrolan luar roleplay global
    usage: /oocg pesan

  carry:
    description: Angkat atau turunkan pemain di depanmu
    usage: /carry
    aliases: [gendong]
  vnpc:
    description: Setup NPC dialog dan quest Vitae
    usage: /vnpc help
    aliases: [vitaenpc]
  quest:
    description: Status atau batalkan quest Vitae
    usage: /quest status
    aliases: [vitaequest]

permissions:
  vitae.music:
    description: Memutar musik pribadi dari resource pack server
    default: true

  vitae.admin.music:
    description: Mengatur saklar musik server dan memeriksa resource pack
    default: op

  vitae.admin.dungeon:
    description: Setup dan pemulihan dungeon bersama
    default: op
  vitae.admin.fade:
    description: Setup dan bersihkan screen fade
    default: op
  vitae.dungeon.bypass:
    description: Bypass proteksi dungeon khusus pengelola; jangan diberikan pada peserta
    default: false
  vitae.admin:
    description: Akses command utama administrator
    default: op
    children:
      vitae.admin.music: true
      vitae.admin.npc: true
      vitae.admin.dungeon: true
      vitae.admin.fade: true
      vitae.admin.reload: true
      vitae.admin.maintenance: true
      vitae.admin.chat: true
      vitae.admin.chatbypass: true
      vitae.admin.pvp: true
      vitae.admin.mobspawning: true
      vitae.admin.time: true
      vitae.admin.weather: true
      vitae.admin.viti: true
      vitae.admin.profile: true
      vitae.admin.heal: true
      vitae.admin.feed: true
      vitae.admin.revive: true
      vitae.admin.gamemode: true
      vitae.admin.teleport: true
      vitae.admin.pull: true
      vitae.admin.invsee: true
      vitae.admin.totem: true
      vitae.admin.blocker: true
      vitae.admin.stuck: true
      vitae.admin.whisper: true
      vitae.admin.evolution: true

  vitae.admin.reload:
    description: Memuat ulang konfigurasi Vitae
    default: op

  vitae.admin.maintenance:
    description: Mengatur dan melewati maintenance (kompatibel dengan izin lama)
    default: op

  vitae.admin.chat:
    description: Mengatur chat server
    default: op

  vitae.admin.chatbypass:
    description: Mengirim chat saat chat server dimatikan
    default: op

  vitae.admin.pvp:
    description: Mengatur PvP per dunia
    default: op

  vitae.admin.mobspawning:
    description: Mengatur gamerule spawn mob vanilla per dunia
    default: op

  vitae.admin.time:
    description: Mengatur waktu dunia melalui panel
    default: op

  vitae.admin.weather:
    description: Mengatur cuaca dunia melalui panel
    default: op

  vitae.viti:
    description: Menggunakan saldo, transfer, dan uang fisik Viti
    default: true

  vitae.admin.viti:
    description: Mengatur saldo Viti dan memuat ulang viti.yml
    default: op

  vitae.admin.profile:
    description: Membuka daftar dan profil pemain
    default: op

  vitae.admin.heal:
    description: Memulihkan HP melalui profil
    default: op

  vitae.admin.feed:
    description: Memulihkan hunger melalui profil
    default: op

  vitae.admin.revive:
    description: Mengirim command ke plugin revive yang tersedia
    default: op

  vitae.admin.gamemode:
    description: Mengganti gamemode melalui profil
    default: op

  vitae.admin.teleport:
    description: Teleport ke pemain melalui profil
    default: op

  vitae.admin.pull:
    description: Menarik pemain melalui profil
    default: op

  vitae.admin.invsee:
    description: Melihat dan mengedit inventory pemain online
    default: op

  vitae.roleplay:
    description: Akses perintah teks roleplay
    default: true
    children:
      vitae.roleplay.me: true
      vitae.roleplay.do: true
      vitae.roleplay.ooc: true
      vitae.roleplay.oocg: true

  vitae.roleplay.me:
    default: true

  vitae.roleplay.do:
    default: true

  vitae.roleplay.ooc:
    default: true

  vitae.roleplay.oocg:
    default: true

  vitae.carry:
    description: Menggunakan carry dan gendong
    default: true

  vitae.totem:
    description: Menyerap totem menjadi charge tersimpan
    default: true

  vitae.admin.totem:
    description: Mengatur batas totem dan reset cooldown pemain online
    default: op

  vitae.admin.blocker:
    description: Toggle blokir item dan mob
    default: op

  vitae.admin.stuck:
    description: Mengatur batas stack item
    default: op

  vitae.admin.whisper:
    description: Mengatur area bisikan, penerima, dialog, suara, preview dan reset progres
    default: op

  vitae.evolution:
    description: Menggunakan altar evolusi yang ditujukan kepada pemain
    default: true

  vitae.admin.evolution:
    description: Mengatur ritual evolusi senjata
    default: op
  vitae.admin.npc:
    description: Mengatur NPC dialog dan quest
    default: op
  vitae.npc:
    description: Memulai dialog NPC Vitae
    default: true
  vitae.quest:
    description: Menerima dan membatalkan quest Vitae
    default: true
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/music/MusicSettings.java'
        OriginalHashes = @()
        Hash = '9bf185c9e017d5825991c1ee597850470adb6cd6d98d9c0f4a6743e74c086f32'
        Required = $false
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.music;

import org.bukkit.configuration.ConfigurationSection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public record MusicSettings(boolean enabled, boolean requirePack, float volume,
                            double crossfadeSeconds, String disabledMessage, List<Track> tracks) {
    public MusicSettings { tracks = List.copyOf(tracks); }
    public record Track(String id, String name, String sound, double seconds) {
        public long durationNanos() { return (long) Math.ceil(seconds * 1_000_000_000d); }
    }
    public static MusicSettings parse(ConfigurationSection source) {
        boolean enabled = bool(source, "enabled");
        boolean requirePack = bool(source, "require-resource-pack");
        double volume = number(source, "volume");
        double crossfade = number(source, "crossfade-seconds");
        if (volume <= 0 || volume > 1) throw invalid("volume harus lebih dari 0 sampai 1");
        if (crossfade < 0 || crossfade * 1_000_000_000d >= Long.MAX_VALUE / 4d)
            throw invalid("crossfade-seconds harus angka nonnegatif yang valid");
        Object messageValue = source.get("admin-off-message");
        if (!(messageValue instanceof String message) || message.isBlank())
            throw invalid("admin-off-message harus berupa teks");
        ConfigurationSection entries = source.getConfigurationSection("tracks");
        if (entries == null || entries.getKeys(false).isEmpty()) throw invalid("tracks belum diisi");
        List<Track> tracks = new ArrayList<>();
        HashSet<String> ids = new HashSet<>();
        for (String rawId : entries.getKeys(false)) {
            String id = rawId.toLowerCase(Locale.ROOT);
            if (!rawId.equals(id) || !id.matches("[a-z0-9_-]+") || !ids.add(id))
                throw invalid("ID lagu tidak valid: " + rawId);
            ConfigurationSection entry = entries.getConfigurationSection(rawId);
            if (entry == null) throw invalid("tracks." + rawId + " harus berupa section");
            Object nameValue = entry.get("name"), soundValue = entry.get("sound");
            if (!(nameValue instanceof String name) || name.isBlank()) throw invalid("nama lagu kosong: " + id);
            if (!(soundValue instanceof String sound) || !sound.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
                throw invalid("sound harus berupa namespace:id untuk " + id);
            double seconds = number(entry, "duration-seconds");
            if (seconds < .05 || seconds * 1_000_000_000d >= Long.MAX_VALUE / 4d)
                throw invalid("duration-seconds tidak valid untuk " + id);
            tracks.add(new Track(id, name, sound, seconds));
        }
        return new MusicSettings(enabled, requirePack, (float) volume, crossfade, message, tracks);
    }
    public int indexOf(String id) {
        for (int i = 0; i < tracks.size(); i++) if (tracks.get(i).id().equalsIgnoreCase(id)) return i;
        return -1;
    }
    private static boolean bool(ConfigurationSection source, String key) {
        if (!(source.get(key) instanceof Boolean result)) throw invalid(key + " harus true/false");
        return result;
    }
    private static double number(ConfigurationSection source, String key) {
        if (!(source.get(key) instanceof Number number) || !Double.isFinite(number.doubleValue()))
            throw invalid(key + " harus angka yang valid");
        return number.doubleValue();
    }
    private static IllegalArgumentException invalid(String detail) {
        return new IllegalArgumentException("music.yml: " + detail);
    }
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/music/MusicService.java'
        OriginalHashes = @()
        Hash = 'fa2f5c30a7cd3cb3a4ab3c4655277b605575f590ff8a315fe3ed87f144b9edee'
        Required = $false
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.music;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Opt-in, main-thread audio playback. Audio packets are sent to one Player only. */
public final class MusicService implements Listener, AutoCloseable {
    private enum Mode { AUTO, LOOP, TEST }
    private record Playing(long started, long duration) {}
    private static final class Playback {
        private final UUID playerId;
        private final Mode mode;
        private final Map<String, Playing> playing = new LinkedHashMap<>();
        private int index;
        private long started, nextAfter;
        private Playback(UUID playerId, int index, Mode mode) {
            this.playerId = playerId; this.index = index; this.mode = mode;
        }
    }
    private final JavaPlugin plugin;
    private final Path path;
    private final Map<UUID, Playback> playbacks = new HashMap<>();
    private final Map<UUID, Boolean> packStates = new HashMap<>();
    private MusicSettings settings;
    private YamlConfiguration document;
    private BukkitTask task;
    private String failure = "Konfigurasi musik belum dimuat.";
    private boolean closed;

    public MusicService(JavaPlugin plugin) {
        this.plugin = plugin;
        path = plugin.getDataFolder().toPath().resolve("music.yml");
    }
    public void start() {
        try {
            if (!Files.exists(path)) plugin.saveResource("music.yml", false);
            load();
        } catch (Exception exception) {
            failure = exception.getMessage();
            plugin.getLogger().log(Level.SEVERE, "Modul musik belum siap; modul lain tetap berjalan.", exception);
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 2L);
    }
    private void load() throws Exception {
        YamlConfiguration candidate = new YamlConfiguration();
        candidate.load(path.toFile());
        MusicSettings parsed = MusicSettings.parse(candidate);
        stopAll();
        document = candidate; settings = parsed; failure = "";
    }
    public void reload(CommandSender sender) {
        try {
            load();
            tell(sender, "music.yml dimuat. Playlist dihentikan; pemain bisa /music on lagi.");
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Reload music.yml gagal; pengaturan aktif dipertahankan.", exception);
            tell(sender, "Reload gagal: " + exception.getMessage() + ". Pengaturan aktif dipertahankan.", true);
        }
    }
    public boolean enabled() { return settings != null && settings.enabled(); }
    public List<MusicSettings.Track> tracks() { return settings == null ? List.of() : settings.tracks(); }
    private boolean ready(CommandSender sender) {
        if (closed || settings == null) {
            tell(sender, "Modul musik belum siap: " + failure + ". Admin: /vitae music reload.", true); return false;
        }
        return true;
    }
    public void setEnabled(CommandSender sender, boolean enabled) {
        if (!ready(sender)) return;
        if (settings.enabled() == enabled) {
            if (!enabled) stopAll();
            tell(sender, "Musik server sudah " + (enabled ? "ON." : "OFF.")); return;
        }
        YamlConfiguration changed = new YamlConfiguration();
        try {
            changed.loadFromString(document.saveToString());
            changed.set("enabled", enabled);
            MusicSettings parsed = MusicSettings.parse(changed);
            write(changed.saveToString());
            settings = parsed; document = changed;
            if (!enabled) {
                List<UUID> stopped = new ArrayList<>(playbacks.keySet());
                stopAll();
                for (UUID id : stopped) {
                    Player player = Bukkit.getPlayer(id);
                    if (player != null) tell(player, parsed.disabledMessage(), true);
                }
            }
            tell(sender, enabled ? "Musik server ON. Pemain boleh /music on." : "Musik server OFF. Seluruh playlist dihentikan.");
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Gagal menyimpan saklar musik.", exception);
            tell(sender, "Pengaturan gagal disimpan; status sebelumnya dipertahankan.", true);
        }
    }
    private void write(String source) throws IOException {
        Path temporary = Files.createTempFile(path.getParent(), "music-", ".tmp");
        try {
            Files.writeString(temporary, source, StandardCharsets.UTF_8);
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException exception) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
    public void play(Player player, String action, String id) {
        if (!ready(player)) return;
        if (!player.hasPermission("vitae.music")) { tell(player, "Perlu izin vitae.music.", true); return; }
        if (!settings.enabled()) {
            stop(player.getUniqueId()); tell(player, settings.disabledMessage(), true); return;
        }
        if (action.equals("off")) { stop(player.getUniqueId()); tell(player, "Musik pribadimu OFF."); return; }
        if (!packReady(player)) return;
        Playback old = playbacks.get(player.getUniqueId());
        if (action.equals("on") && old != null) { now(player); return; }
        int index = 0;
        Mode mode = Mode.AUTO;
        if (action.equals("playback")) {
            index = settings.indexOf(id); mode = Mode.LOOP;
            if (index < 0) { tell(player, "ID lagu tidak ditemukan. Lihat /music list.", true); return; }
        } else if (old != null) index = action.equals("skip") ? (old.index + 1) % tracks().size() : old.index;
        if (!begin(player, index, mode)) return;
        tell(player, mode == Mode.LOOP ? "Mengulang " + tracks().get(index).name() + ". Kembali ke playlist: /music auto."
                : "Playlist ON: " + tracks().get(index).name() + ".");
    }
    private boolean hasPack(Player player) {
        return packStates.getOrDefault(player.getUniqueId(), player.hasResourcePack());
    }
    private boolean packReady(Player player) {
        if (settings.requirePack() && !hasPack(player)) {
            tell(player, "Resource pack belum selesai dimuat. Terima pack server, lalu coba /music on lagi.", true); return false;
        }
        return true;
    }
    public void test(CommandSender sender, String id, Player player) {
        if (!ready(sender)) return;
        int index = settings.indexOf(id);
        if (index < 0) { tell(sender, "ID tidak ditemukan. Gunakan /music list.", true); return; }
        if (!settings.enabled()) { tell(sender, "Aktifkan dahulu: /vitae music on.", true); return; }
        if (!player.hasPermission("vitae.music")) { tell(sender, "Pemain tidak mempunyai izin vitae.music.", true); return; }
        if (!packReady(player)) {
            if (!sender.equals(player)) tell(sender, "Resource pack pemain belum siap.", true);
            return;
        }
        if (!begin(player, index, Mode.TEST)) return;
        tell(sender, "Tes dikirim ke " + player.getName() + ": " + tracks().get(index).sound() + " | kategori Music, volume " + settings.volume() + ", pitch 1.");
        tell(sender, "Paket suara dikirim; terdengarnya audio bergantung pada resource pack client.");
    }
    private boolean begin(Player player, int index, Mode mode) {
        stop(player.getUniqueId());
        Playback playback = new Playback(player.getUniqueId(), index, mode);
        playbacks.put(player.getUniqueId(), playback);
        try { advance(player, playback, System.nanoTime()); return true; }
        catch (RuntimeException exception) { playbackFailed(player, exception); return false; }
    }
    private void advance(Player player, Playback playback, long now) {
        MusicSettings.Track track = tracks().get(playback.index);
        if (playback.playing.containsKey(track.sound())) player.stopSound(track.sound(), SoundCategory.MUSIC);
        player.playSound(player.getLocation(), track.sound(), SoundCategory.MUSIC, settings.volume(), 1f);
        playback.playing.put(track.sound(), new Playing(now, track.durationNanos()));
        playback.started = now;
        long overlap = 0;
        if (playback.mode == Mode.AUTO && tracks().size() > 1) {
            MusicSettings.Track next = tracks().get((playback.index + 1) % tracks().size());
            if (!track.sound().equals(next.sound()))
                overlap = (long) (Math.min(settings.crossfadeSeconds(), Math.min(track.seconds(), next.seconds()) / 2d) * 1_000_000_000d);
        }
        playback.nextAfter = track.durationNanos() - overlap;
    }
    private void tick() {
        if (closed || settings == null || !settings.enabled()) return;
        long now = System.nanoTime();
        for (Playback playback : new ArrayList<>(playbacks.values())) {
            Player player = Bukkit.getPlayer(playback.playerId);
            if (player == null || !player.isOnline() || !player.hasPermission("vitae.music")) { stop(playback.playerId); continue; }
            try {
                playback.playing.entrySet().removeIf(entry -> now - entry.getValue().started() > entry.getValue().duration() + 1_000_000_000L);
                if (now - playback.started < playback.nextAfter) continue;
                if (playback.mode == Mode.TEST) { stop(playback.playerId); continue; }
                if (playback.mode == Mode.AUTO) playback.index = (playback.index + 1) % tracks().size();
                advance(player, playback, now);
            } catch (RuntimeException exception) { playbackFailed(player, exception); }
        }
    }
    private void playbackFailed(Player player, RuntimeException exception) {
        stop(player.getUniqueId());
        plugin.getLogger().log(Level.WARNING, "Pemutaran musik gagal untuk " + player.getName(), exception);
        tell(player, "Pemutaran gagal. Admin bisa memeriksa /vitae music check.", true);
    }
    private void stop(UUID id) {
        Playback playback = playbacks.remove(id);
        Player player = Bukkit.getPlayer(id);
        if (playback == null || player == null) return;
        for (String sound : playback.playing.keySet()) player.stopSound(sound, SoundCategory.MUSIC);
    }
    private void stopAll() { for (UUID id : new ArrayList<>(playbacks.keySet())) stop(id); }
    public void list(CommandSender sender) {
        if (!ready(sender)) return;
        tell(sender, "Daftar lagu:");
        for (MusicSettings.Track track : tracks()) tell(sender, track.id() + " — " + track.name() + " (" + time(track.seconds()) + ")");
    }
    public void now(Player player) {
        Playback playback = playbacks.get(player.getUniqueId());
        if (playback == null) { tell(player, "Musik pribadimu OFF. /music on untuk mulai."); return; }
        MusicSettings.Track track = tracks().get(playback.index);
        double elapsed = Math.min(track.seconds(), Math.max(0, (System.nanoTime() - playback.started) / 1_000_000_000d));
        String mode = switch (playback.mode) { case AUTO -> "playlist"; case LOOP -> "ulang satu lagu"; case TEST -> "tes sekali"; };
        tell(player, track.name() + " | " + time(elapsed) + "/" + time(track.seconds()) + " | " + mode);
    }
    public void check(CommandSender sender, Player target) {
        tell(sender, "Musik server: " + (enabled() ? "ON" : "OFF") + " | pemutar aktif: " + playbacks.size());
        if (!ready(sender)) return;
        tell(sender, "Konfigurasi: " + path + " | kategori Music, pitch 1.");
        if (target != null) tell(sender, "Pack " + target.getName() + ": " + (hasPack(target) ? "sudah dimuat" : "belum dimuat")
                + " | izin music: " + target.hasPermission("vitae.music"));
        Path archive = plugin.getDataFolder().toPath().getParent().resolve("ItemsAdder/output/generated.zip");
        if (!Files.isRegularFile(archive)) { tell(sender, "Pack belum ditemukan: " + archive + ". Jalankan /iazip dan lihat console.", true); return; }
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            int valid = 0;
            Map<String, JsonObject> namespaces = new HashMap<>();
            for (MusicSettings.Track track : tracks()) {
                String[] key = track.sound().split(":", 2);
                JsonObject definitions = namespaces.get(key[0]);
                if (definitions == null) {
                    ZipEntry entry = zip.getEntry("assets/" + key[0] + "/sounds.json");
                    if (entry == null) { tell(sender, track.id() + ": sounds.json belum masuk pack.", true); continue; }
                    try (var reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
                        definitions = JsonParser.parseReader(reader).getAsJsonObject();
                    }
                    namespaces.put(key[0], definitions);
                }
                boolean found = false;
                JsonElement event = definitions.get(key[1]);
                if (event != null && event.isJsonObject()) {
                    JsonElement files = event.getAsJsonObject().get("sounds");
                    if (files != null && files.isJsonArray()) for (JsonElement element : files.getAsJsonArray()) {
                        String name;
                        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) name = element.getAsString();
                        else if (element.isJsonObject() && element.getAsJsonObject().has("name")) {
                            JsonObject file = element.getAsJsonObject();
                            if (file.has("type") && !file.get("type").getAsString().equals("file")) continue;
                            name = file.get("name").getAsString();
                        } else continue;
                        String qualified = name.contains(":") ? name : "minecraft:" + name;
                        String[] resource = qualified.split(":", 2);
                        ZipEntry audio = zip.getEntry("assets/" + resource[0] + "/sounds/" + resource[1] + ".ogg");
                        if (audio != null && audio.getSize() > 0) found = true;
                    }
                }
                if (found) valid++;
                tell(sender, track.id() + ": " + (found ? "ID dan audio ada di generated.zip." : "ID/audio belum ditemukan dalam pack."), !found);
            }
            tell(sender, "Hasil pack server: " + valid + "/" + tracks().size() + " lagu ditemukan.");
            tell(sender, "Jika pack lengkap tetapi diam: muat pack terbaru dengan /iatexture, lalu cek log Minecraft client.");
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Pemeriksaan pack musik gagal.", exception);
            tell(sender, "Pack belum bisa diperiksa: " + exception.getMessage(), true);
        }
    }
    @EventHandler public void quit(PlayerQuitEvent event) {
        stop(event.getPlayer().getUniqueId()); packStates.remove(event.getPlayer().getUniqueId());
    }
    @EventHandler public void pack(PlayerResourcePackStatusEvent event) {
        boolean loaded = event.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED;
        packStates.put(event.getPlayer().getUniqueId(), loaded);
        if (!loaded) {
            boolean active = playbacks.containsKey(event.getPlayer().getUniqueId());
            stop(event.getPlayer().getUniqueId());
            if (active) tell(event.getPlayer(), "Musik dihentikan saat resource pack berubah. Setelah pack siap, gunakan /music on lagi.");
        }
    }
    private static String time(double seconds) {
        long whole = (long) seconds;
        return (whole / 60) + ":" + String.format(Locale.ROOT, "%02d", whole % 60);
    }
    public static void tell(CommandSender sender, String message) { tell(sender, message, false); }
    public static void tell(CommandSender sender, String message, boolean error) {
        sender.sendMessage(Component.text("[Vitae Music] ", NamedTextColor.AQUA)
                .append(Component.text(message, error ? NamedTextColor.RED : NamedTextColor.WHITE)));
    }
    @Override public void close() {
        closed = true;
        if (task != null) task.cancel();
        task = null; stopAll(); packStates.clear();
    }
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/java/com/bangzachery/vitae/vitaemanager/command/MusicCommand.java'
        OriginalHashes = @()
        Hash = 'cc342b1443b1400f86440f88ae140de5e1d70fceda36cbc244ca9a5eac9a0418'
        Required = $false
        Binary = $false
        Source = @'
package com.bangzachery.vitae.vitaemanager.command;

import com.bangzachery.vitae.vitaemanager.music.MusicService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Locale;

public final class MusicCommand implements CommandExecutor, TabCompleter {
    private final MusicService music;
    public MusicCommand(MusicService music) { this.music = music; }
    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                                        @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("vitae.music")) {
            MusicService.tell(sender, "Perlu izin vitae.music.", true); return true;
        }
        String action = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        if (args.length <= 1 && action.equals("help")) { help(sender); return true; }
        if (args.length == 1 && action.equals("list")) { music.list(sender); return true; }
        if (!(sender instanceof Player player)) {
            MusicService.tell(sender, "Pemutaran pribadi digunakan dari game. Console: /vitae music help.", true); return true;
        }
        if (args.length == 1 && action.equals("now")) { music.now(player); return true; }
        if (args.length == 1 && List.of("on", "off", "skip", "auto").contains(action)) {
            music.play(player, action, null); return true;
        }
        if (args.length == 2 && action.equals("playback")) { music.play(player, action, args[1]); return true; }
        help(sender); return true;
    }
    private void help(CommandSender sender) {
        MusicService.tell(sender, "/music on|off — nyalakan/matikan musikmu sendiri");
        MusicService.tell(sender, "/music skip — lagu berikutnya, kembali ke playlist");
        MusicService.tell(sender, "/music playback <id> — ulang lagu yang dipilih");
        MusicService.tell(sender, "/music auto — kembali ke playlist otomatis");
        MusicService.tell(sender, "/music list | /music now — daftar lagu/status");
    }
    public boolean admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vitae.admin.music")) {
            MusicService.tell(sender, "Perlu izin vitae.admin.music.", true); return true;
        }
        String action = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        if (args.length == 1 && List.of("on", "off").contains(action)) { music.setEnabled(sender, action.equals("on")); return true; }
        if (args.length == 1 && action.equals("reload")) { music.reload(sender); return true; }
        if (args.length == 1 && action.equals("list")) { music.list(sender); return true; }
        if (action.equals("check") && args.length <= 2) {
            Player target = args.length == 2 ? Bukkit.getPlayerExact(args[1]) : sender instanceof Player p ? p : null;
            if (args.length == 2 && target == null) MusicService.tell(sender, "Pemain tidak online.", true);
            else music.check(sender, target);
            return true;
        }
        if (action.equals("test") && (args.length == 2 || args.length == 3)) {
            Player target = args.length == 3 ? Bukkit.getPlayerExact(args[2]) : sender instanceof Player p ? p : null;
            if (target == null) MusicService.tell(sender, "Gunakan /vitae music test <id> <pemain-online> dari console.", true);
            else music.test(sender, args[1], target);
            return true;
        }
        MusicService.tell(sender, "/vitae music on|off — saklar seluruh server");
        MusicService.tell(sender, "/vitae music test <id> [pemain] — coba satu lagu");
        MusicService.tell(sender, "/vitae music check [pemain] — periksa config, pack, dan izin");
        MusicService.tell(sender, "/vitae music list | /vitae music reload"); return true;
    }
    @Override public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                         @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("vitae.music")) return List.of();
        if (args.length == 1) return matching(List.of("help", "on", "off", "skip", "playback", "auto", "list", "now"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("playback")) return matching(music.tracks().stream().map(t -> t.id()).toList(), args[1]);
        return List.of();
    }
    public List<String> completeAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vitae.admin.music")) return List.of();
        if (args.length == 1) return matching(List.of("help", "on", "off", "test", "check", "reload", "list"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("test")) return matching(music.tracks().stream().map(t -> t.id()).toList(), args[1]);
        if ((args.length == 2 && args[0].equalsIgnoreCase("check")) || (args.length == 3 && args[0].equalsIgnoreCase("test")))
            return matching(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[args.length - 1]);
        return List.of();
    }
    private static List<String> matching(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
'@
    }
    [PSCustomObject]@{
        Name = 'src/main/resources/music.yml'
        OriginalHashes = @()
        Hash = 'a48150653a9ae83fb467347214eff381d2d233ed9097ac4c07e2df18d427aa47'
        Required = $false
        Binary = $false
        Source = @'
# Config Vitaemanager; berbeda dari configs/music.yml milik ItemsAdder.
# Audio disediakan resource pack ItemsAdder, bukan JAR plugin.
enabled: false
require-resource-pack: true
volume: 1.0
# Audio fade-in 1.5 detik/fade-out 3 detik. Loop satu lagu menunggu selesai.
crossfade-seconds: 1.5
admin-off-message: 'Musik sedang dimatikan admin. Mau dinyalakan? Coba chat Idan.'
tracks:
  chill1:
    name: 'Chill 1'
    sound: 'vitae_music:music.chill1'
    duration-seconds: 106.536
  chill2:
    name: 'Chill 2'
    sound: 'vitae_music:music.chill2'
    duration-seconds: 226.872
  intense1:
    name: 'Intense 1'
    sound: 'vitae_music:music.intense1'
    duration-seconds: 170.640
  intense2:
    name: 'Intense 2'
    sound: 'vitae_music:music.intense2'
    duration-seconds: 365.760
  intense3:
    name: 'Intense 3'
    sound: 'vitae_music:music.intense3'
    duration-seconds: 161.496
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
    if ((Get-VitaeHash $bytesVitae) -ne $fileVitae.Hash) { throw "Isi installer tidak lengkap/rusak: $($fileVitae.Name). Unduh ulang installer." }
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
if ($changesVitae.Count -eq 0) { Write-Host 'Update musik pribadi sudah terpasang. Tidak ada file yang ditulis ulang.' -ForegroundColor Green; return }
$backupVitae = Join-Path $rootVitae ('vitae-music-backup-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [Guid]::NewGuid().ToString('N').Substring(0,8))
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
Write-Host "Update musik pribadi terpasang: $($changesVitae.Count) file." -ForegroundColor Green
Write-Host "Backup sumber lama: $backupVitae" -ForegroundColor Cyan
Write-Host 'Berikutnya jalankan: .\gradlew.bat clean build' -ForegroundColor Yellow
Write-Host 'Saat mengganti JAR server, pertahankan folder data plugins/vitaemanager.' -ForegroundColor Yellow
