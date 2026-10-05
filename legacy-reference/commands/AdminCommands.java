package com.bangzachery.vitae.vitaemanager.commands;

import com.bangzachery.vitae.vitaemanager.guis.AdminPanelGUI;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public final class AdminCommands {

    public static void register(Plugin plugin, LifecycleEventManager<Plugin> manager) {
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands registrar = event.registrar();

            registrar.register(
                    Commands.literal("adminpanel")
                            .requires(source -> source.getSender().hasPermission("vitae.admin"))
                            .executes(context -> {
                                if (context.getSource().getSender() instanceof Player admin) {
                                    // Memanggil kelas AdminPanelGUI yang baru
                                    AdminPanelGUI.openMainPanel(admin);
                                }
                                return 1;
                            })
                            .build(),
                    "Buka GUI Admin Panel 54 Slot",
                    List.of("panel")
            );
        });
    }
}