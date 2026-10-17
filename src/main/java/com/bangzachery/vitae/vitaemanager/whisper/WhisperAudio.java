package com.bangzachery.vitae.vitaemanager.whisper;

import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import java.util.*;

/** Private entity-bound audio, with separate voice and music ownership. */
public final class WhisperAudio {
    private record Track(UUID ticket, WhisperMusic music, long nextLoop) {}
    private final Map<UUID, String> voices = new HashMap<>();
    private final Map<UUID, Track> tracks = new HashMap<>();
    public void begin(Player player, WhisperPlayback.Run run, long tick) {
        Track track = tracks.get(player.getUniqueId());
        if (track != null && track.ticket().equals(run.ticket())) return;
        stopMusic(player);
        if (run.definition().music() == null) return;
        WhisperMusic music = run.definition().music(); playMusic(player, music);
        tracks.put(player.getUniqueId(), new Track(run.ticket(), music, music.repeatTicks() > 0 ? tick + music.repeatTicks() : 0));
    }
    public void voice(Player player, WhisperLine.Audio sound) {
        stopVoice(player);
        if (sound == null) return;
        player.playSound(player, sound.key(), SoundCategory.VOICE, sound.volume(), sound.pitch());
        voices.put(player.getUniqueId(), sound.key());
    }
    public void tick(Player player, WhisperPlayback.Run run, long tick) {
        Track track = tracks.get(player.getUniqueId());
        if (track == null || !track.ticket().equals(run.ticket()) || track.nextLoop() == 0 || tick < track.nextLoop()) return;
        player.stopSound(track.music().sound().key(), SoundCategory.MUSIC);
        playMusic(player, track.music());
        tracks.put(player.getUniqueId(), new Track(track.ticket(), track.music(), tick + track.music().repeatTicks()));
    }
    private void playMusic(Player player, WhisperMusic music) {
        var sound = music.sound(); player.playSound(player, sound.key(), SoundCategory.MUSIC, sound.volume(), sound.pitch());
    }
    private void stopVoice(Player player) {
        String key = voices.remove(player.getUniqueId()); if (key != null) player.stopSound(key, SoundCategory.VOICE);
    }
    private void stopMusic(Player player) {
        Track track = tracks.remove(player.getUniqueId()); if (track != null) player.stopSound(track.music().sound().key(), SoundCategory.MUSIC);
    }
    public void clear(Player player) { stopVoice(player); stopMusic(player); }
    public void forget(UUID player) { voices.remove(player); tracks.remove(player); }
    public Set<UUID> players() { Set<UUID> players = new HashSet<>(voices.keySet()); players.addAll(tracks.keySet()); return players; }
}
