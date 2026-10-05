package com.bangzachery.vitae.vitaemanager.profile;

import com.bangzachery.vitae.vitaemanager.core.config.ConfigurationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ProfileConfigurationTest {
    @TempDir Path directory;

    @Test void bundledMessagesValidateAndExistingConfigReceivesDefaultsWithoutRewrite() throws Exception {
        String defaults;
        try (var resource = getClass().getResourceAsStream("/config.yml")) {
            defaults = new String(java.util.Objects.requireNonNull(resource).readAllBytes(), StandardCharsets.UTF_8);
        }
        String existing = "config-version: 1\ncore:\n  debug: false\nmessages:\n  prefix: '<gold>Custom</gold>'\nlegacy: keep\n";
        Path file = directory.resolve("config.yml");
        Files.writeString(file, existing);
        var service = new ConfigurationService(file, defaults);
        service.initialize();
        assertNotNull(service.current().messages().get("profile-inventory-changed"));
        assertNotNull(service.current().messages().get("profile-revive-requested"));
        assertEquals(existing, Files.readString(file));
        var snapshot = service.current();
        Files.writeString(file, existing.replace("legacy: keep", "  profile-action-done: false\nlegacy: keep"));
        assertThrows(IllegalArgumentException.class, service::reload);
        assertSame(snapshot, service.current());
    }
}