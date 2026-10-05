package com.bangzachery.vitae.vitaemanager.whisper;

/** Text is literal, not player-supplied MiniMessage. Timing is in server ticks. */
public record WhisperLine(String title, String subtitle, int durationTicks, int gapTicks, Audio audio) {
    public WhisperLine {
        title = text(title);
        subtitle = text(subtitle);
        if (title.isBlank() && subtitle.isBlank()) throw new IllegalArgumentException("Title dan subtitle tidak boleh keduanya kosong.");
        if (durationTicks < 20 || durationTicks > 1200 || gapTicks < 0 || gapTicks > 1200)
            throw new IllegalArgumentException("Durasi 1–60 detik; jeda 0–60 detik.");
    }

    private static String text(String value) {
        if (value == null || value.codePointCount(0, value.length()) > 160
                || value.codePoints().anyMatch(c -> Character.isISOControl(c) || c == 0x2028 || c == 0x2029))
            throw new IllegalArgumentException("Teks maksimum 160 karakter dan tidak boleh mengandung baris baru/kontrol.");
        return value;
    }

    public WhisperLine withAudio(Audio value) { return new WhisperLine(title, subtitle, durationTicks, gapTicks, value); }

    public record Audio(String key, float volume, float pitch) {
        public Audio {
            if (key == null || !key.matches("[a-z0-9._-]+:[a-z0-9/._-]+"))
                throw new IllegalArgumentException("Sound gunakan ID seperti minecraft:block.note_block.hat atau namespace:suara.");
            if (!Float.isFinite(volume) || volume < 0 || volume > 1 || !Float.isFinite(pitch) || pitch < 0.5f || pitch > 2)
                throw new IllegalArgumentException("Volume 0–1; pitch 0.5–2.");
        }
    }
}