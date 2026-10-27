package com.bangzachery.vitae.vitaemanager.roleplay;

import com.bangzachery.vitae.vitaemanager.core.config.ConfigurationService;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class CarryMessagesTest {
    @TempDir Path root;
    @Test void upgradesBundledLegacyWordingWithoutRewritingUserConfig() throws Exception {
        String defaults;
        try (var in = getClass().getResourceAsStream("/config.yml")) {
            defaults = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String old = defaults.replace("Kamu sedang menggendong seseorang.", "Kamu mengangkat")
                .replace("Kamu sedang digendong seseorang.", "Kamu sedang diangkat oleh")
                .replace("Kamu berhenti menggendong.", "Pemain berhasil diturunkan.");
        Path file = root.resolve("config.yml"); Files.writeString(file, old);
        var service = new ConfigurationService(file, defaults); service.initialize();
        assertEquals(Component.text("Kamu sedang menggendong seseorang.", net.kyori.adventure.text.format.NamedTextColor.GREEN), service.current().messages().get("carry-started"));
        assertEquals(old, Files.readString(file));
        Files.writeString(file, defaults.replace("Kamu sedang menggendong seseorang.", "Kamu menggendong orang lain."));
        service.reload();
        assertEquals(Component.text("Kamu menggendong orang lain.", net.kyori.adventure.text.format.NamedTextColor.GREEN), service.current().messages().get("carry-started"));
    }
}
