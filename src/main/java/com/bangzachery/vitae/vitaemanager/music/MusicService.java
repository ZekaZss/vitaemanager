package com.bangzachery.vitae.vitaemanager.music;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Opt-in, main-thread audio playback. Audio packets are sent to one Player only. */
public final class MusicService implements Listener, AutoCloseable {
    private enum Mode { AUTO, LOOP, TEST }
    private record Playing(long started, long duration) {}
    private static final class Playback {
        private final UUID playerId;
        private final Mode mode;
        private final Map<String, Playing> playing = new LinkedHashMap<>();
        private int index;
        private long started, nextAfter;
        private Playback(UUID playerId, int index, Mode mode) {
            this.playerId = playerId; this.index = index; this.mode = mode;
        }
    }
    private final JavaPlugin plugin;
    private final Path path;
    private final Map<UUID, Playback> playbacks = new HashMap<>();
    private final Map<UUID, Boolean> packStates = new HashMap<>();
    private MusicSettings settings;
    private YamlConfiguration document;
    private BukkitTask task;
    private String failure = "Konfigurasi musik belum dimuat.";
    private boolean closed;

    public MusicService(JavaPlugin plugin) {
        this.plugin = plugin;
        path = plugin.getDataFolder().toPath().resolve("music.yml");
    }
    public void start() {
        try {
            if (!Files.exists(path)) plugin.saveResource("music.yml", false);
            load();
        } catch (Exception exception) {
            failure = exception.getMessage();
            plugin.getLogger().log(Level.SEVERE, "Modul musik belum siap; modul lain tetap berjalan.", exception);
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 2L);
    }
    private void load() throws Exception {
        YamlConfiguration candidate = new YamlConfiguration();
        candidate.load(path.toFile());
        MusicSettings parsed = MusicSettings.parse(candidate);
        stopAll();
        document = candidate; settings = parsed; failure = "";
    }
    public void reload(CommandSender sender) {
        try {
            load();
            tell(sender, "music.yml dimuat. Playlist dihentikan; pemain bisa /music on lagi.");
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Reload music.yml gagal; pengaturan aktif dipertahankan.", exception);
            tell(sender, "Reload gagal: " + exception.getMessage() + ". Pengaturan aktif dipertahankan.", true);
        }
    }
    public boolean enabled() { return settings != null && settings.enabled(); }
    public List<MusicSettings.Track> tracks() { return settings == null ? List.of() : settings.tracks(); }
    private boolean ready(CommandSender sender) {
        if (closed || settings == null) {
            tell(sender, "Modul musik belum siap: " + failure + ". Admin: /vitae music reload.", true); return false;
        }
        return true;
    }
    public void setEnabled(CommandSender sender, boolean enabled) {
        if (!ready(sender)) return;
        if (settings.enabled() == enabled) {
            if (!enabled) stopAll();
            tell(sender, "Musik server sudah " + (enabled ? "ON." : "OFF.")); return;
        }
        YamlConfiguration changed = new YamlConfiguration();
        try {
            changed.loadFromString(document.saveToString());
            changed.set("enabled", enabled);
            MusicSettings parsed = MusicSettings.parse(changed);
            write(changed.saveToString());
            settings = parsed; document = changed;
            if (!enabled) {
                List<UUID> stopped = new ArrayList<>(playbacks.keySet());
                stopAll();
                for (UUID id : stopped) {
                    Player player = Bukkit.getPlayer(id);
                    if (player != null) tell(player, parsed.disabledMessage(), true);
                }
            }
            tell(sender, enabled ? "Musik server ON. Pemain boleh /music on." : "Musik server OFF. Seluruh playlist dihentikan.");
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Gagal menyimpan saklar musik.", exception);
            tell(sender, "Pengaturan gagal disimpan; status sebelumnya dipertahankan.", true);
        }
    }
    private void write(String source) throws IOException {
        Path temporary = Files.createTempFile(path.getParent(), "music-", ".tmp");
        try {
            Files.writeString(temporary, source, StandardCharsets.UTF_8);
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException exception) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
    public void play(Player player, String action, String id) {
        if (!ready(player)) return;
        if (!player.hasPermission("vitae.music")) { tell(player, "Perlu izin vitae.music.", true); return; }
        if (!settings.enabled()) {
            stop(player.getUniqueId()); tell(player, settings.disabledMessage(), true); return;
        }
        if (action.equals("off")) { stop(player.getUniqueId()); tell(player, "Musik pribadimu OFF."); return; }
        if (!packReady(player)) return;
        Playback old = playbacks.get(player.getUniqueId());
        if (action.equals("on") && old != null) { now(player); return; }
        int index = 0;
        Mode mode = Mode.AUTO;
        if (action.equals("playback")) {
            index = settings.indexOf(id); mode = Mode.LOOP;
            if (index < 0) { tell(player, "ID lagu tidak ditemukan. Lihat /music list.", true); return; }
        } else if (old != null) index = action.equals("skip") ? (old.index + 1) % tracks().size() : old.index;
        if (!begin(player, index, mode)) return;
        tell(player, mode == Mode.LOOP ? "Mengulang " + tracks().get(index).name() + ". Kembali ke playlist: /music auto."
                : "Playlist ON: " + tracks().get(index).name() + ".");
    }
    private boolean hasPack(Player player) {
        return packStates.getOrDefault(player.getUniqueId(), player.hasResourcePack());
    }
    private boolean packReady(Player player) {
        if (settings.requirePack() && !hasPack(player)) {
            tell(player, "Resource pack belum selesai dimuat. Terima pack server, lalu coba /music on lagi.", true); return false;
        }
        return true;
    }
    public void test(CommandSender sender, String id, Player player) {
        if (!ready(sender)) return;
        int index = settings.indexOf(id);
        if (index < 0) { tell(sender, "ID tidak ditemukan. Gunakan /music list.", true); return; }
        if (!settings.enabled()) { tell(sender, "Aktifkan dahulu: /vitae music on.", true); return; }
        if (!player.hasPermission("vitae.music")) { tell(sender, "Pemain tidak mempunyai izin vitae.music.", true); return; }
        if (!packReady(player)) {
            if (!sender.equals(player)) tell(sender, "Resource pack pemain belum siap.", true);
            return;
        }
        if (!begin(player, index, Mode.TEST)) return;
        tell(sender, "Tes dikirim ke " + player.getName() + ": " + tracks().get(index).sound() + " | kategori Music, volume " + settings.volume() + ", pitch 1.");
        tell(sender, "Paket suara dikirim; terdengarnya audio bergantung pada resource pack client.");
    }
    private boolean begin(Player player, int index, Mode mode) {
        stop(player.getUniqueId());
        Playback playback = new Playback(player.getUniqueId(), index, mode);
        playbacks.put(player.getUniqueId(), playback);
        try { advance(player, playback, System.nanoTime()); return true; }
        catch (RuntimeException exception) { playbackFailed(player, exception); return false; }
    }
    private void advance(Player player, Playback playback, long now) {
        MusicSettings.Track track = tracks().get(playback.index);
        if (playback.playing.containsKey(track.sound())) player.stopSound(track.sound(), SoundCategory.MUSIC);
        player.playSound(player.getLocation(), track.sound(), SoundCategory.MUSIC, settings.volume(), 1f);
        playback.playing.put(track.sound(), new Playing(now, track.durationNanos()));
        playback.started = now;
        long overlap = 0;
        if (playback.mode == Mode.AUTO && tracks().size() > 1) {
            MusicSettings.Track next = tracks().get((playback.index + 1) % tracks().size());
            if (!track.sound().equals(next.sound()))
                overlap = (long) (Math.min(settings.crossfadeSeconds(), Math.min(track.seconds(), next.seconds()) / 2d) * 1_000_000_000d);
        }
        playback.nextAfter = track.durationNanos() - overlap;
    }
    private void tick() {
        if (closed || settings == null || !settings.enabled()) return;
        long now = System.nanoTime();
        for (Playback playback : new ArrayList<>(playbacks.values())) {
            Player player = Bukkit.getPlayer(playback.playerId);
            if (player == null || !player.isOnline() || !player.hasPermission("vitae.music")) { stop(playback.playerId); continue; }
            try {
                playback.playing.entrySet().removeIf(entry -> now - entry.getValue().started() > entry.getValue().duration() + 1_000_000_000L);
                if (now - playback.started < playback.nextAfter) continue;
                if (playback.mode == Mode.TEST) { stop(playback.playerId); continue; }
                if (playback.mode == Mode.AUTO) playback.index = (playback.index + 1) % tracks().size();
                advance(player, playback, now);
            } catch (RuntimeException exception) { playbackFailed(player, exception); }
        }
    }
    private void playbackFailed(Player player, RuntimeException exception) {
        stop(player.getUniqueId());
        plugin.getLogger().log(Level.WARNING, "Pemutaran musik gagal untuk " + player.getName(), exception);
        tell(player, "Pemutaran gagal. Admin bisa memeriksa /vitae music check.", true);
    }
    private void stop(UUID id) {
        Playback playback = playbacks.remove(id);
        Player player = Bukkit.getPlayer(id);
        if (playback == null || player == null) return;
        for (String sound : playback.playing.keySet()) player.stopSound(sound, SoundCategory.MUSIC);
    }
    private void stopAll() { for (UUID id : new ArrayList<>(playbacks.keySet())) stop(id); }
    public void list(CommandSender sender) {
        if (!ready(sender)) return;
        tell(sender, "Daftar lagu:");
        for (MusicSettings.Track track : tracks()) tell(sender, track.id() + " — " + track.name() + " (" + time(track.seconds()) + ")");
    }
    public void now(Player player) {
        Playback playback = playbacks.get(player.getUniqueId());
        if (playback == null) { tell(player, "Musik pribadimu OFF. /music on untuk mulai."); return; }
        MusicSettings.Track track = tracks().get(playback.index);
        double elapsed = Math.min(track.seconds(), Math.max(0, (System.nanoTime() - playback.started) / 1_000_000_000d));
        String mode = switch (playback.mode) { case AUTO -> "playlist"; case LOOP -> "ulang satu lagu"; case TEST -> "tes sekali"; };
        tell(player, track.name() + " | " + time(elapsed) + "/" + time(track.seconds()) + " | " + mode);
    }
    public void check(CommandSender sender, Player target) {
        tell(sender, "Musik server: " + (enabled() ? "ON" : "OFF") + " | pemutar aktif: " + playbacks.size());
        if (!ready(sender)) return;
        tell(sender, "Konfigurasi: " + path + " | kategori Music, pitch 1.");
        if (target != null) tell(sender, "Pack " + target.getName() + ": " + (hasPack(target) ? "sudah dimuat" : "belum dimuat")
                + " | izin music: " + target.hasPermission("vitae.music"));
        Path archive = plugin.getDataFolder().toPath().getParent().resolve("ItemsAdder/output/generated.zip");
        if (!Files.isRegularFile(archive)) { tell(sender, "Pack belum ditemukan: " + archive + ". Jalankan /iazip dan lihat console.", true); return; }
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            int valid = 0;
            Map<String, JsonObject> namespaces = new HashMap<>();
            for (MusicSettings.Track track : tracks()) {
                String[] key = track.sound().split(":", 2);
                JsonObject definitions = namespaces.get(key[0]);
                if (definitions == null) {
                    ZipEntry entry = zip.getEntry("assets/" + key[0] + "/sounds.json");
                    if (entry == null) { tell(sender, track.id() + ": sounds.json belum masuk pack.", true); continue; }
                    try (var reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
                        definitions = JsonParser.parseReader(reader).getAsJsonObject();
                    }
                    namespaces.put(key[0], definitions);
                }
                boolean found = false;
                JsonElement event = definitions.get(key[1]);
                if (event != null && event.isJsonObject()) {
                    JsonElement files = event.getAsJsonObject().get("sounds");
                    if (files != null && files.isJsonArray()) for (JsonElement element : files.getAsJsonArray()) {
                        String name;
                        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) name = element.getAsString();
                        else if (element.isJsonObject() && element.getAsJsonObject().has("name")) {
                            JsonObject file = element.getAsJsonObject();
                            if (file.has("type") && !file.get("type").getAsString().equals("file")) continue;
                            name = file.get("name").getAsString();
                        } else continue;
                        String qualified = name.contains(":") ? name : "minecraft:" + name;
                        String[] resource = qualified.split(":", 2);
                        ZipEntry audio = zip.getEntry("assets/" + resource[0] + "/sounds/" + resource[1] + ".ogg");
                        if (audio != null && audio.getSize() > 0) found = true;
                    }
                }
                if (found) valid++;
                tell(sender, track.id() + ": " + (found ? "ID dan audio ada di generated.zip." : "ID/audio belum ditemukan dalam pack."), !found);
            }
            tell(sender, "Hasil pack server: " + valid + "/" + tracks().size() + " lagu ditemukan.");
            tell(sender, "Jika pack lengkap tetapi diam: muat pack terbaru dengan /iatexture, lalu cek log Minecraft client.");
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING, "Pemeriksaan pack musik gagal.", exception);
            tell(sender, "Pack belum bisa diperiksa: " + exception.getMessage(), true);
        }
    }
    @EventHandler public void quit(PlayerQuitEvent event) {
        stop(event.getPlayer().getUniqueId()); packStates.remove(event.getPlayer().getUniqueId());
    }
    @EventHandler public void pack(PlayerResourcePackStatusEvent event) {
        boolean loaded = event.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED;
        packStates.put(event.getPlayer().getUniqueId(), loaded);
        if (!loaded) {
            boolean active = playbacks.containsKey(event.getPlayer().getUniqueId());
            stop(event.getPlayer().getUniqueId());
            if (active) tell(event.getPlayer(), "Musik dihentikan saat resource pack berubah. Setelah pack siap, gunakan /music on lagi.");
        }
    }
    private static String time(double seconds) {
        long whole = (long) seconds;
        return (whole / 60) + ":" + String.format(Locale.ROOT, "%02d", whole % 60);
    }
    public static void tell(CommandSender sender, String message) { tell(sender, message, false); }
    public static void tell(CommandSender sender, String message, boolean error) {
        sender.sendMessage(Component.text("[Vitae Music] ", NamedTextColor.AQUA)
                .append(Component.text(message, error ? NamedTextColor.RED : NamedTextColor.WHITE)));
    }
    @Override public void close() {
        closed = true;
        if (task != null) task.cancel();
        task = null; stopAll(); packStates.clear();
    }
}
