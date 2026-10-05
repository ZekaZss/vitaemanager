package com.bangzachery.vitae.vitaemanager.managers;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.UUID;

public class FloatingTextManager {
    private static final HashMap<UUID, TextDisplay> activeDisplays = new HashMap<>();

    public static void showText(Player player, String text, Plugin plugin) {
        removeText(player);

        TextDisplay display = player.getWorld().spawn(player.getLocation(), TextDisplay.class, entity -> {
            entity.text(MiniMessage.miniMessage().deserialize(text));
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setDefaultBackground(false);
            entity.setBackgroundColor(Color.fromARGB(100, 0, 0, 0));
            entity.setSeeThrough(false);

            // Perbaikan Tinggi: Karena posisi awal penumpang (passenger) sudah di pundak/kepala,
            // kita hanya perlu menggesernya sedikit (0.3 blok) agar pas di atas nametag.
            Transformation transform = new Transformation(
                    new Vector3f(0f, 0.3f, 0f),
                    new AxisAngle4f(0f, 0f, 0f, 1f),
                    new Vector3f(1f, 1f, 1f),
                    new AxisAngle4f(0f, 0f, 0f, 1f)
            );
            entity.setTransformation(transform);
        });

        player.addPassenger(display);
        activeDisplays.put(player.getUniqueId(), display);

        // Hapus teks setelah 10 detik
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            removeText(player);
        }, 200L);
    }

    public static void removeText(Player player) {
        TextDisplay old = activeDisplays.remove(player.getUniqueId());
        if (old != null && !old.isDead()) {
            old.remove();
        }
    }
}