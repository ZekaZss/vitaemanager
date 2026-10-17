package com.bangzachery.vitae.vitaemanager.whisper;

import java.util.Objects;

/** Loop interval is explicit; zero plays the clip once while the sequence continues. */
public record WhisperMusic(WhisperLine.Audio sound, int repeatTicks) {
    public WhisperMusic {
        Objects.requireNonNull(sound);
        if (repeatTicks < 0 || repeatTicks > 12000 || (repeatTicks > 0 && repeatTicks < 20))
            throw new IllegalArgumentException("Loop musik: off atau 1–600 detik.");
    }
}
