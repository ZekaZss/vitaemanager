package com.bangzachery.vitae.vitaemanager.command;

import com.bangzachery.vitae.vitaemanager.core.config.ConfigurationService;
import com.bangzachery.vitae.vitaemanager.core.message.MessageService;
import com.bangzachery.vitae.vitaemanager.roleplay.CarryService;
import com.bangzachery.vitae.vitaemanager.roleplay.RoleplayService;
import com.bangzachery.vitae.vitaemanager.roleplay.RoleplayText;
import com.bangzachery.vitae.vitaemanager.servercontrol.ServerControlService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public final class RoleplayCommand implements CommandExecutor, TabCompleter {
    private final ConfigurationService configuration;
    private final MessageService messages;
    private final ServerControlService controls;
    private final RoleplayService roleplay;
    private final CarryService carry;

    public RoleplayCommand(ConfigurationService configuration, MessageService messages,
                           ServerControlService controls, RoleplayService roleplay, CarryService carry) {
        this.configuration = configuration;
        this.messages = messages;
        this.controls = controls;
        this.roleplay = roleplay;
        this.carry = carry;
    }

    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) { messages.send(sender, "player-only"); return true; }
        String action = command.getName().toLowerCase(Locale.ROOT);
        if (!player.hasPermission(action.equals("carry") ? "vitae.carry" : "vitae.roleplay." + action)) {
            messages.send(player, "no-permission"); return true;
        }
        if (player.isDead()) { messages.send(player, "roleplay-unavailable"); return true; }
        if (action.equals("carry")) {
            if (args.length == 0) carry.toggle(player);
            else messages.send(player, "roleplay-help");
            return true;
        }
        if (controls.current().chatMuted() && !player.hasPermission("vitae.admin.chatbypass")) {
            messages.send(player, "chat-muted"); return true;
        }
        if (args.length == 0) { messages.send(player, "roleplay-help"); return true; }
        String text;
        try { text = RoleplayText.validate(String.join(" ", args), configuration.current().roleplay().maxTextLength()); }
        catch (IllegalArgumentException exception) { messages.send(player, "roleplay-invalid"); return true; }
        if (action.equals("me") || action.equals("do")) {
            roleplay.show(player, RoleplayText.floating(text, action.equals("do")));
            return true;
        }
        boolean global = action.equals("oocg");
        var message = RoleplayText.ooc(player.getName(), text, global);
        var location = player.getLocation();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!roleplay.visible(viewer, player)) continue;
            if (global || (viewer.getWorld() == player.getWorld()
                    && RoleplayText.inRange(viewer.getLocation().distanceSquared(location),
                    configuration.current().roleplay().oocRadius()))) {
                viewer.sendMessage(message);
            }
        }
        return true;
    }

    @Override public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                         @NotNull String alias, @NotNull String[] args) {
        return List.of();
    }
}