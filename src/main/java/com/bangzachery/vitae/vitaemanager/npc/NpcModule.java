package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.economy.VitiService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/** Lifecycle facade: existing modules in Vitaemanager.java need no replacement. */
public final class NpcModule {
    private static final Map<JavaPlugin, NpcService> ACTIVE = new IdentityHashMap<>();
    private static final Map<JavaPlugin, Throwable> FAILED = new IdentityHashMap<>();
    @FunctionalInterface interface Factory { NpcService create() throws IOException; }
    private NpcModule() {}
    public static void start(JavaPlugin plugin, VitiService viti) throws IOException {
        start(plugin, viti, () -> new NpcService(plugin, viti));
    }
    static void start(JavaPlugin plugin, VitiService viti, Factory factory) {
        if (ACTIVE.containsKey(plugin)) return;
        NpcService service = null;
        try {
            service = factory.create();
            NpcCommand executor = new NpcCommand(service);
            for (String name : List.of("vnpc", "quest")) {
                PluginCommand command = plugin.getCommand(name);
                if (command == null) throw new IllegalStateException("Command " + name + " belum terdaftar di plugin.yml.");
                command.setExecutor(executor); command.setTabCompleter(executor);
            }
            plugin.getServer().getPluginManager().registerEvents(service, plugin);
            service.start(); ACTIVE.put(plugin, service); FAILED.remove(plugin);
        } catch (IOException | RuntimeException | LinkageError error) {
            if (service != null) try { service.close(); }
            catch (RuntimeException | LinkageError cleanup) { error.addSuppressed(cleanup); }
            FAILED.put(plugin, error);
            plugin.getLogger().log(Level.SEVERE, "Modul NPC gagal diaktifkan. Modul Vitae lain tetap berjalan; data npc.json dipertahankan. Gunakan /vnpc status.", error);
            recovery(plugin, viti);
        }
    }
    public static void stop(JavaPlugin plugin) {
        FAILED.remove(plugin);
        NpcService service = ACTIVE.remove(plugin);
        if (service != null) try { service.close(); }
        catch (RuntimeException | LinkageError error) { plugin.getLogger().log(Level.SEVERE, "Pembersihan modul NPC gagal.", error); }
    }
    private static void recovery(JavaPlugin plugin, VitiService viti) {
        CommandExecutor executor = (sender, command, label, args) -> {
            if (command.getName().equalsIgnoreCase("quest")) {
                tell(sender, "Modul quest belum aktif. Minta admin memeriksa /vnpc status.", NamedTextColor.RED); return true;
            }
            if (!sender.hasPermission("vitae.admin.npc")) {
                tell(sender, "Perlu izin vitae.admin.npc.", NamedTextColor.RED); return true;
            }
            try {
                if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                    start(plugin, viti); report(plugin, sender); return true;
                }
                if (args.length == 2 && args[1].equalsIgnoreCase("off")) {
                    NpcStore store = new NpcStore(plugin.getDataFolder().toPath().resolve("npc.json"));
                    NpcData.State state = store.initialize(); var definition = state.npcs().get(args[0]);
                    if (definition == null) throw new IllegalArgumentException("NPC tidak ditemukan: " + args[0]);
                    var json = NpcStore.JSON.toJsonTree(definition).getAsJsonObject(); json.addProperty("enabled", false);
                    store.save(state.put(NpcStore.JSON.fromJson(json, NpcData.Definition.class)));
                    tell(sender, "NPC " + args[0] + " dinonaktifkan. Definisi dan progres tetap tersimpan.", NamedTextColor.YELLOW);
                    start(plugin, viti); report(plugin, sender); return true;
                }
                report(plugin, sender);
            } catch (IOException | RuntimeException error) {
                tell(sender, reason(error), NamedTextColor.RED);
                plugin.getLogger().log(Level.SEVERE, "Pemulihan NPC belum berhasil; data lama dipertahankan.", error);
            }
            return true;
        };
        for (String name : List.of("vnpc", "quest")) {
            PluginCommand command = plugin.getCommand(name);
            if (command == null) continue;
            command.setExecutor(executor);
            command.setTabCompleter((sender, cmd, alias, args) -> {
                if (!sender.hasPermission("vitae.admin.npc")) return List.of();
                if (args.length == 1) return List.of("status", "reload", "help").stream()
                        .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
                return args.length == 2 && "off".startsWith(args[1].toLowerCase(Locale.ROOT)) ? List.of("off") : List.of();
            });
        }
    }
    private static void report(JavaPlugin plugin, CommandSender sender) {
        sender.sendMessage(Component.empty());
        if (ACTIVE.containsKey(plugin)) tell(sender, "Modul NPC aktif kembali.", NamedTextColor.GREEN);
        else {
            tell(sender, "Modul NPC belum aktif: " + reason(FAILED.get(plugin)), NamedTextColor.RED);
            tell(sender, "Perbaiki penyebabnya, lalu /vnpc reload. Jika satu NPC/model bermasalah: /vnpc <id> off.", NamedTextColor.AQUA);
            tell(sender, "Error lengkap ada pada log startup. File npc.json tidak dikosongkan.", NamedTextColor.GRAY);
        }
        sender.sendMessage(Component.empty());
    }
    private static String reason(Throwable error) {
        if (error == null) return "Periksa log startup server.";
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        while (error.getCause() != null && seen.add(error)) error = error.getCause();
        return error.getClass().getSimpleName() + ": " + Objects.requireNonNullElse(error.getMessage(), "Periksa log startup server.");
    }
    private static void tell(CommandSender sender, String text, NamedTextColor color) {
        sender.sendMessage(Component.text("[Vitae NPC] ", NamedTextColor.GOLD).append(Component.text(text, color)));
    }
}
