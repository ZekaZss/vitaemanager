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
            if (sender.hasPermission("vitae.admin.whisper")) sender.sendMessage(messages.component("prefix")
                    .append(Component.text("Bisikan Area: /vitae bisikan help", NamedTextColor.AQUA)));
            return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
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
        if (args.length > 1 && args[0].equalsIgnoreCase("bisikan"))
            return whispers.complete(sender, java.util.Arrays.copyOfRange(args, 1, args.length));
        List<String> choices = new ArrayList<>();
        if (args.length == 1) {
            choices.addAll(List.of("help", "panel"));
            if (sender.hasPermission("vitae.admin.whisper")) choices.add("bisikan");
            if (sender.hasPermission("vitae.admin.blocker")) choices.add("blokir");
            if (sender.hasPermission("vitae.admin.stuck")) choices.add("stuck");
            if (sender.hasPermission("vitae.admin.totem")) choices.addAll(List.of("totem", "reset"));
            for (String action : List.of("reload", "maintenance", "chat", "pvp", "mobspawning")) {
                if (sender.hasPermission("vitae.admin." + action)) choices.add(action);
            }
        } else if (args.length == 2 || args.length == 3) {
            String action = args[0].toLowerCase(Locale.ROOT);
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