package com.bangzachery.vitae.vitaemanager.command;

import com.bangzachery.vitae.vitaemanager.core.message.MessageService;
import com.bangzachery.vitae.vitaemanager.economy.VitiAmount;
import com.bangzachery.vitae.vitaemanager.economy.VitiService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class VitiCommand implements CommandExecutor, TabCompleter {
    private final VitiService service;
    private final MessageService messages;

    public VitiCommand(VitiService service, MessageService messages) { this.service = service; this.messages = messages; }

    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("vitae.viti")) { messages.send(sender, "no-permission"); return true; }
        String action = args.length == 0 ? "lihat" : args[0].toLowerCase(Locale.ROOT);
        Consumer<String> done = key -> {
            if (!(sender instanceof Player player) || player.isOnline()) messages.send(sender, key);
        };
        try {
            if (action.equals("help") && args.length == 1) { help(sender); return true; }
            if (action.equals("lihat") && args.length <= 1) {
                if (sender instanceof Player player) messages.send(sender, "viti-balance",
                        Component.text(" " + VitiAmount.format(service.balance(player)), NamedTextColor.WHITE));
                else messages.send(sender, "player-only");
                return true;
            }
            if (action.equals("board") && args.length == 1) {
                messages.send(sender, "viti-board");
                int rank = 1;
                for (var entry : service.current().top()) {
                    sender.sendMessage(Component.text(rank++ + ". " + entry.getValue().name() + " - "
                            + VitiAmount.format(entry.getValue().highest()) + " Viti", NamedTextColor.YELLOW));
                }
                return true;
            }
            if (action.equals("reload") && args.length == 1) {
                if (admin(sender)) service.reload(done);
                return true;
            }
            if (action.equals("giveaway")) {
                if (!admin(sender)) return true;
                if (args.length != 2) { giveawayHelp(sender); return true; }
                if (args[1].equalsIgnoreCase("status")) {
                    var round = service.current().giveaway();
                    String detail = round == null ? "Belum ada giveaway."
                            : "Giveaway " + (round.active() ? "aktif" : "dihentikan") + ": "
                              + VitiAmount.format(round.amount()) + " Viti/pemain; "
                              + round.claimed().size() + " pemain sudah menerima.";
                    sender.sendMessage(messages.component("prefix").append(Component.text(detail, NamedTextColor.YELLOW)));
                } else if (args[1].equalsIgnoreCase("stop")) {
                    service.stopGiveaway(key -> {
                        if (key.equals("viti-success")) feedback(sender, key, "Giveaway dihentikan. Saldo penerima tetap tersimpan.");
                        else done.accept(key);
                    });
                } else {
                    BigDecimal amount = VitiAmount.positive(VitiAmount.parse(args[1]));
                    service.giveaway(amount, key -> {
                        if (key.equals("viti-success")) feedback(sender, key,
                                "Putaran giveaway baru: " + VitiAmount.format(amount) + " Viti/pemain. Pemain yang join nanti juga menerima sekali.");
                        else done.accept(key);
                    });
                }
                return true;
            }
            if (List.of("add", "set", "remove").contains(action)) {
                if (!admin(sender)) return true;
                if (args.length != 3) { help(sender); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { messages.send(sender, "viti-player-not-found"); return true; }
                BigDecimal amount = VitiAmount.parse(args[2]);
                service.admin(target, action, amount, key -> {
                    if (key.equals("viti-success")) feedback(sender, key, action + " " + target.getName() + " " + VitiAmount.format(amount));
                    else done.accept(key);
                });
                return true;
            }
            if ((action.equals("beri") || action.equals("convert")) && args.length == 2) {
                if (!(sender instanceof Player player)) { messages.send(sender, "player-only"); return true; }
                BigDecimal amount = VitiAmount.positive(VitiAmount.parse(args[1]));
                if (action.equals("convert")) service.withdraw(player, amount, done);
                else {
                    var eye = player.getEyeLocation();
                    var hit = player.getWorld().rayTrace(eye, eye.getDirection(), 2.0,
                            FluidCollisionMode.NEVER, true, 0.05,
                            entity -> entity instanceof Player && !entity.getUniqueId().equals(player.getUniqueId()));
                    if (hit == null || !(hit.getHitEntity() instanceof Player target)
                            || !target.isOnline() || target.getLocation().distanceSquared(player.getLocation()) > 2.25) {
                        messages.send(sender, "viti-near-player"); return true;
                    }
                    service.transfer(player, target, amount, key -> {
                        if (key.equals("viti-success")) feedback(sender, key,
                                VitiAmount.format(amount) + " Viti kepada " + target.getName());
                        else done.accept(key);
                        if (key.equals("viti-success") && target.isOnline()) messages.send(target, "viti-received",
                                Component.text(" " + VitiAmount.format(amount) + " Viti dari " + player.getName(), NamedTextColor.WHITE));
                    });
                }
                return true;
            }
            help(sender);
        } catch (IllegalArgumentException exception) {
            messages.send(sender, "viti-invalid");
            if (action.equals("giveaway")) giveawayHelp(sender);
        }
        return true;
    }

    private boolean admin(CommandSender sender) {
        if (sender.hasPermission("vitae.admin.viti")) return true;
        messages.send(sender, "no-permission"); return false;
    }

    private void help(CommandSender sender) {
        messages.send(sender, "viti-help");
        if (sender.hasPermission("vitae.admin.viti")) giveawayHelp(sender);
    }

    private void giveawayHelp(CommandSender sender) {
        sender.sendMessage(Component.text("/viti giveaway <nominal> | status | stop\n"
                        + "Contoh: /viti giveaway 1.000\n"
                        + "Nominal harus lebih dari 0. Setiap command nominal memulai putaran baru dan mengganti putaran sebelumnya.",
                NamedTextColor.YELLOW));
    }

    private void feedback(CommandSender sender, String key, String detail) {
        if (!(sender instanceof Player player) || player.isOnline()) messages.send(sender, key,
                Component.text(" " + detail, NamedTextColor.WHITE));
    }

    @Override public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                         @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("vitae.viti") || args.length == 0) return List.of();
        var choices = new ArrayList<String>();
        if (args.length == 1) {
            choices.addAll(List.of("lihat", "help", "beri", "convert", "board"));
            if (sender.hasPermission("vitae.admin.viti")) choices.addAll(List.of("add", "set", "remove", "reload", "giveaway"));
        } else if (args.length == 2 && sender.hasPermission("vitae.admin.viti")
                && args[0].equalsIgnoreCase("giveaway")) {
            choices.addAll(List.of("status", "stop", "1000"));
        } else if (args.length == 2 && sender.hasPermission("vitae.admin.viti")
                && List.of("add", "set", "remove").contains(args[0].toLowerCase(Locale.ROOT))) {
            Bukkit.getOnlinePlayers().forEach(player -> choices.add(player.getName()));
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return choices.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}