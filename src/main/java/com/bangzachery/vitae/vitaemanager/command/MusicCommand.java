package com.bangzachery.vitae.vitaemanager.command;

import com.bangzachery.vitae.vitaemanager.music.MusicService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Locale;

public final class MusicCommand implements CommandExecutor, TabCompleter {
    private final MusicService music;
    public MusicCommand(MusicService music) { this.music = music; }
    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                                        @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("vitae.music")) {
            MusicService.tell(sender, "Perlu izin vitae.music.", true); return true;
        }
        String action = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        if (args.length <= 1 && action.equals("help")) { help(sender); return true; }
        if (args.length == 1 && action.equals("list")) { music.list(sender); return true; }
        if (!(sender instanceof Player player)) {
            MusicService.tell(sender, "Pemutaran pribadi digunakan dari game. Console: /vitae music help.", true); return true;
        }
        if (args.length == 1 && action.equals("now")) { music.now(player); return true; }
        if (args.length == 1 && List.of("on", "off", "skip", "auto").contains(action)) {
            music.play(player, action, null); return true;
        }
        if (args.length == 2 && action.equals("playback")) { music.play(player, action, args[1]); return true; }
        help(sender); return true;
    }
    private void help(CommandSender sender) {
        MusicService.tell(sender, "/music on|off — nyalakan/matikan musikmu sendiri");
        MusicService.tell(sender, "/music skip — lagu berikutnya, kembali ke playlist");
        MusicService.tell(sender, "/music playback <id> — ulang lagu yang dipilih");
        MusicService.tell(sender, "/music auto — kembali ke playlist otomatis");
        MusicService.tell(sender, "/music list | /music now — daftar lagu/status");
    }
    public boolean admin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vitae.admin.music")) {
            MusicService.tell(sender, "Perlu izin vitae.admin.music.", true); return true;
        }
        String action = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        if (args.length == 1 && List.of("on", "off").contains(action)) { music.setEnabled(sender, action.equals("on")); return true; }
        if (args.length == 1 && action.equals("reload")) { music.reload(sender); return true; }
        if (args.length == 1 && action.equals("list")) { music.list(sender); return true; }
        if (action.equals("check") && args.length <= 2) {
            Player target = args.length == 2 ? Bukkit.getPlayerExact(args[1]) : sender instanceof Player p ? p : null;
            if (args.length == 2 && target == null) MusicService.tell(sender, "Pemain tidak online.", true);
            else music.check(sender, target);
            return true;
        }
        if (action.equals("test") && (args.length == 2 || args.length == 3)) {
            Player target = args.length == 3 ? Bukkit.getPlayerExact(args[2]) : sender instanceof Player p ? p : null;
            if (target == null) MusicService.tell(sender, "Gunakan /vitae music test <id> <pemain-online> dari console.", true);
            else music.test(sender, args[1], target);
            return true;
        }
        MusicService.tell(sender, "/vitae music on|off — saklar seluruh server");
        MusicService.tell(sender, "/vitae music test <id> [pemain] — coba satu lagu");
        MusicService.tell(sender, "/vitae music check [pemain] — periksa config, pack, dan izin");
        MusicService.tell(sender, "/vitae music list | /vitae music reload"); return true;
    }
    @Override public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                         @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("vitae.music")) return List.of();
        if (args.length == 1) return matching(List.of("help", "on", "off", "skip", "playback", "auto", "list", "now"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("playback")) return matching(music.tracks().stream().map(t -> t.id()).toList(), args[1]);
        return List.of();
    }
    public List<String> completeAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vitae.admin.music")) return List.of();
        if (args.length == 1) return matching(List.of("help", "on", "off", "test", "check", "reload", "list"), args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("test")) return matching(music.tracks().stream().map(t -> t.id()).toList(), args[1]);
        if ((args.length == 2 && args[0].equalsIgnoreCase("check")) || (args.length == 3 && args[0].equalsIgnoreCase("test")))
            return matching(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[args.length - 1]);
        return List.of();
    }
    private static List<String> matching(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
