package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.economy.VitiService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.IOException;
import java.util.*;

/** Lifecycle facade: existing modules in Vitaemanager.java need no replacement. */
public final class NpcModule {
    private static final Map<JavaPlugin, NpcService> ACTIVE = new IdentityHashMap<>();
    private NpcModule() {}
    public static void start(JavaPlugin plugin, VitiService viti) throws IOException {
        if (ACTIVE.containsKey(plugin)) return;
        NpcService service = new NpcService(plugin, viti);
        try {
            NpcCommand executor = new NpcCommand(service);
            for (String name : List.of("vnpc", "quest")) {
                PluginCommand command = plugin.getCommand(name);
                if (command == null) throw new IllegalStateException("Command " + name + " belum terdaftar di plugin.yml.");
                command.setExecutor(executor); command.setTabCompleter(executor);
            }
            plugin.getServer().getPluginManager().registerEvents(service, plugin);
            service.start(); ACTIVE.put(plugin, service);
        } catch (RuntimeException e) { service.close(); throw e; }
    }
    public static void stop(JavaPlugin plugin) {
        NpcService service = ACTIVE.remove(plugin); if (service != null) service.close();
    }
}
