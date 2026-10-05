package com.bangzachery.vitae.vitaemanager.commands;

import com.bangzachery.vitae.vitaemanager.managers.FloatingTextManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public final class RoleplayCommands {

    public static void register(Plugin plugin, LifecycleEventManager<Plugin> manager) {
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands registrar = event.registrar();
            final MiniMessage mm = MiniMessage.miniMessage();

            // --- COMMAND: /CARRY ---
            registrar.register(
                    Commands.literal("carry").executes(context -> {
                        if (!(context.getSource().getSender() instanceof Player sender)) return 0;

                        // Kalau lagi nge-carry orang, turunin dulu
                        if (!sender.getPassengers().isEmpty()) {
                            sender.getPassengers().forEach(sender::removePassenger);
                            sender.sendMessage(mm.deserialize("<green>Kamu telah menurunkan player."));
                            return 1;
                        }

                        // Ray-trace untuk ngecek player di jarak pandang max 2 blok
                        Entity targetEntity = sender.getTargetEntity(2, false);
                        if (targetEntity instanceof Player target) {
                            if (target.getPassengers().contains(sender)) {
                                sender.sendMessage(mm.deserialize("<red>Tidak bisa meng-carry orang yang sedang meng-carry kamu!"));
                                return 0;
                            }
                            sender.addPassenger(target);
                            sender.sendMessage(mm.deserialize("<green>Kamu mengangkat " + target.getName()));
                            target.sendMessage(mm.deserialize("<green>Kamu sedang diangkat oleh " + sender.getName()));
                        } else {
                            sender.sendMessage(mm.deserialize("<red>Tidak ada player di hadapanmu."));
                        }
                        return 1;
                    }).build(), "Gendong player di depanmu", List.of("gendong")
            );

            // --- COMMAND: /ME ---
            registrar.register(
                    Commands.literal("me").then(Commands.argument("aksi", StringArgumentType.greedyString())
                            .executes(context -> {
                                if (context.getSource().getSender() instanceof Player player) {
                                    String aksi = StringArgumentType.getString(context, "aksi");
                                    String format = "<green>\"" + aksi + "\"";
                                    FloatingTextManager.showText(player, format, plugin);
                                }
                                return 1;
                            })).build(), "Munculkan teks roleplay (me)", List.of()
            );

            // --- COMMAND: /DO ---
            registrar.register(
                    Commands.literal("do").then(Commands.argument("kondisi", StringArgumentType.greedyString())
                            .executes(context -> {
                                if (context.getSource().getSender() instanceof Player player) {
                                    String kondisi = StringArgumentType.getString(context, "kondisi");
                                    String format = "<aqua>\"" + kondisi + "\"";
                                    FloatingTextManager.showText(player, format, plugin);
                                }
                                return 1;
                            })).build(), "Munculkan teks roleplay (do)", List.of()
            );

            // --- COMMAND: /OOC (Radius 30 Blok) ---
            registrar.register(
                    Commands.literal("ooc").then(Commands.argument("pesan", StringArgumentType.greedyString())
                            .executes(context -> {
                                if (context.getSource().getSender() instanceof Player sender) {
                                    String pesan = StringArgumentType.getString(context, "pesan");
                                    String format = "<gray>[OOC] " + sender.getName() + ": " + pesan;

                                    for (Player target : sender.getWorld().getPlayers()) {
                                        if (target.getLocation().distanceSquared(sender.getLocation()) <= 900) { // 30*30 = 900
                                            target.sendMessage(mm.deserialize(format));
                                        }
                                    }
                                }
                                return 1;
                            })).build(), "Obrolan luar roleplay (Lokal)", List.of()
            );

            // --- COMMAND: /OOCG (Global) ---
            registrar.register(
                    Commands.literal("oocg").then(Commands.argument("pesan", StringArgumentType.greedyString())
                            .executes(context -> {
                                if (context.getSource().getSender() instanceof Player sender) {
                                    String pesan = StringArgumentType.getString(context, "pesan");
                                    String format = "<#3b82f6>[OOC Global] " + sender.getName() + ": " + pesan; // Biru sedikit tua
                                    Bukkit.broadcast(mm.deserialize(format));
                                }
                                return 1;
                            })).build(), "Obrolan luar roleplay (Global)", List.of()
            );
        });
    }
}