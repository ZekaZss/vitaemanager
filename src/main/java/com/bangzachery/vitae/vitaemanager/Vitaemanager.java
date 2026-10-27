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
    private com.bangzachery.vitae.vitaemanager.integration.PlaceholderHook placeholders;

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
    private com.bangzachery.vitae.vitaemanager.magic.MagicService magic;

    @Override
    public void onEnable() {
        try {
            com.bangzachery.vitae.vitaemanager.core.data.DataLayout.initialize(getDataFolder().toPath());
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
                    getDataFolder().toPath().resolve("server/state.yml")
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
            placeholders = new com.bangzachery.vitae.vitaemanager.integration.PlaceholderHook(this, viti);
            placeholders.start();

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
                    getDataFolder().toPath().resolve("totem/settings.yml")
            );

            totems = new TotemService(
                    this, configuration, totemStore, totemStore.load());

            var vanillaSounds = org.bukkit.Registry.SOUNDS.stream()
                    .map(sound ->
                            org.bukkit.Registry.SOUNDS
                                    .getKeyOrThrow(sound).toString())
                    .collect(Collectors.toUnmodifiableSet());

            WhisperStore whisperStore = new WhisperStore(
                    getDataFolder().toPath().resolve("whisper/whispers.yml"),
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
            magic = new com.bangzachery.vitae.vitaemanager.magic.MagicService(this);
            magic.carryRelease(carry::releaseForMagic);
            magic.start();
            carry.abilityGuard(player -> magic.isAbilityBlocked(player.getUniqueId()));
            evolution.abilityGuard(player -> magic.isAbilityBlocked(player.getUniqueId()));
            var npcService = com.bangzachery.vitae.vitaemanager.npc.NpcModule.active(this);
            if (npcService != null) npcService.abilityGuard(player -> magic.isAbilityBlocked(player.getUniqueId()));
            var musicExecutor = new com.bangzachery.vitae.vitaemanager.command.MusicCommand(music);
            executor.music(musicExecutor);
            PluginCommand musicCommand = getCommand("music");
            if (musicCommand == null) throw new IllegalStateException("Command music tidak terdaftar di plugin.yml");
            musicCommand.setExecutor(musicExecutor);
            musicCommand.setTabCompleter(musicExecutor);

            getLogger().info(
                    "VitaeManager remake: fondasi, kontrol server, Viti, "
                            + "profil, roleplay/carry, totem, aturan item "
                            + "bisikan area, toko NPC, dan modul sihir aktif."
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
        closeModule("magic", magic);
        closeModule("music", music);
        closeModule("selection", selection);
        closeModule("story", story);
        com.bangzachery.vitae.vitaemanager.npc.NpcModule.stop(this);
        closeModule("evolution", evolution);
        closeModule("whispers", whispers);

        closeModule("itemListener", itemListener);
        closeModule("itemRules", itemRules);

        closeModule("totems", totems);
        closeModule("carry", carry);
        closeModule("roleplay", roleplay);

        closeModule("profiles", profiles);

        closeModule("vitiListener", vitiListener);
        closeModule("placeholders", placeholders);
        closeModule("viti", viti);

        closeModule("controlListener", controlListener);
        closeModule("panel", panel);
        closeModule("controls", controls);

        getLogger().info("VitaeManager dimatikan.");
    }
    private void closeModule(String name, AutoCloseable module) {
        if (module == null) return;
        try { module.close(); }
        catch (Exception | LinkageError error) {
            getLogger().log(Level.SEVERE, "Cleanup modul " + name + " gagal; cleanup modul lain dilanjutkan.", error);
        }
    }
}
