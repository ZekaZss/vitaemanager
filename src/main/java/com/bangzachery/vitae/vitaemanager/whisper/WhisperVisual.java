package com.bangzachery.vitae.vitaemanager.whisper;

public record WhisperVisual(boolean typing, int charactersPerTick, int fadeInTicks, int fadeOutTicks) {
    public WhisperVisual {
        if (charactersPerTick < 1 || charactersPerTick > 8 || fadeInTicks < 0 || fadeInTicks > 40 || fadeOutTicks < 0 || fadeOutTicks > 40)
            throw new IllegalArgumentException("Typing 1–8 karakter/tick; fade masuk/keluar 0–2 detik.");
    }
    public static WhisperVisual defaults() { return new WhisperVisual(false, 2, 5, 5); }
    public WhisperVisual withTyping(boolean enabled, int speed) { return new WhisperVisual(enabled, speed, fadeInTicks, fadeOutTicks); }
    public WhisperVisual withFade(int in, int out) { return new WhisperVisual(typing, charactersPerTick, in, out); }
}
