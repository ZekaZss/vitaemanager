package com.bangzachery.vitae.vitaemanager.servercontrol;

import com.bangzachery.vitae.vitaemanager.core.message.MessageService;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ServerControlListener
        implements Listener, AutoCloseable {
    private final JavaPlugin plugin;
    private final ServerControlService service;
    private final MessageService messages;

    private volatile Set<UUID> chatBypass = Set.of();
    private volatile boolean closed;
    private BukkitTask refreshTask;

    public ServerControlListener(
            JavaPlugin plugin,
            ServerControlService service,
            MessageService messages) {
        this.plugin = plugin;
        this.service = service;
        this.messages = messages;
    }

    public void start() {
        Bukkit.getWorlds().forEach(service::applyWorld);
        refreshPermissions();
        service.enforceMaintenance();

        refreshTask = Bukkit.getScheduler().runTaskTimer(
                plugin, this::refreshPermissions, 20L, 20L);
    }

    private void refreshPermissions() {
        Set<UUID> updated = new HashSet<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("vitae.admin.chatbypass")) {
                updated.add(player.getUniqueId());
            }
        }

        chatBypass = Set.copyOf(updated);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void login(PlayerLoginEvent event) {
        // Jangan memakai allow(): keputusan ban/whitelist tetap berlaku.
        if (event.getResult() == PlayerLoginEvent.Result.ALLOWED
                && service.current().maintenance()
                && !service.maintenanceBypass(event.getPlayer())) {
            event.disallow(
                    PlayerLoginEvent.Result.KICK_OTHER,
                    messages.component("maintenance-kick"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void join(PlayerJoinEvent event) {
        refreshPermissions();

        if (service.current().maintenance()
                && !service.maintenanceBypass(event.getPlayer())) {
            event.getPlayer().kick(
                    messages.component("maintenance-kick"));
        }
    }

    @EventHandler
    public void quit(PlayerQuitEvent event) {
        Set<UUID> updated = new HashSet<>(chatBypass);
        updated.remove(event.getPlayer().getUniqueId());
        chatBypass = Set.copyOf(updated);
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true)
    public void chat(AsyncChatEvent event) {
        UUID id = event.getPlayer().getUniqueId();

        if (!service.current().chatMuted()
                || chatBypass.contains(id)) {
            return;
        }

        event.setCancelled(true);

        if (closed) return;

        try {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (closed) return;

                Player player = Bukkit.getPlayer(id);

                if (player != null) {
                    messages.send(player, "chat-muted");
                }
            });
        } catch (IllegalPluginAccessException ignored) {
            // Plugin bisa dimatikan saat event async masih berjalan.
        }
    }

    @EventHandler
    public void worldLoad(WorldLoadEvent event) {
        service.applyWorld(event.getWorld());
    }

    @Override
    public void close() {
        closed = true;

        if (refreshTask != null) {
            refreshTask.cancel();
        }

        chatBypass = Set.of();
    }
}