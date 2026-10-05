package com.bangzachery.vitae.vitaemanager.totem;

import com.bangzachery.vitae.vitaemanager.core.config.ConfigurationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import static org.junit.jupiter.api.Assertions.*;

class TotemConfigurationTest {
    @TempDir Path directory;
    private String defaults() throws Exception {
        try (var resource = getClass().getResourceAsStream("/config.yml")) {
            return new String(
                    Objects.requireNonNull(resource).readAllBytes(),
                    StandardCharsets.UTF_8
            ).replace("\r\n", "\n").replace("\r", "\n");
        }
    }

    @Test void oldConfigLoadsCompatibleDefaultsAndNewMessagesWithoutRewrite() throws Exception {
        Path file = directory.resolve("config.yml");
        String legacy = "config-version: 1\ncore:\n  debug: false\nmessages:\n  prefix: '<gold>Custom</gold>'\n";
        Files.writeString(file, legacy);
        var service = new ConfigurationService(file, defaults());
        service.initialize();
        assertEquals(new ConfigurationService.TotemSettings(3,15,true), service.current().totem());
        assertNotNull(service.current().messages().get("totem-absorbed"));
        assertNotNull(service.current().messages().get("totem-used"));
        assertEquals(legacy, Files.readString(file));
    }

    @Test void validReloadCommitsSettingsTogether() throws Exception {
        Path file = directory.resolve("config.yml");
        String original = defaults();
        var service = new ConfigurationService(file, original);
        service.initialize();
        Files.writeString(file, original.replace("limit: 3", "limit: 5")
                .replace("cooldown-hours: 15", "cooldown-hours: 0")
                .replace("block-vanilla-resurrection: true", "block-vanilla-resurrection: false"));
        service.reload();
        assertEquals(new ConfigurationService.TotemSettings(5,0,false), service.current().totem());
    }

    @Test void invalidSettingsAndMessageTypeKeepPreviousSnapshotAndDisk() throws Exception {
        Path file = directory.resolve("config.yml");
        String original = defaults();
        var service = new ConfigurationService(file, original);
        service.initialize();
        var previous = service.current();
        for (String text : new String[]{
                original.replace("limit: 3", "limit: 0"),
                original.replace("limit: 3", "limit: 1001"),
                original.replace("cooldown-hours: 15", "cooldown-hours: -1"),
                original.replace("cooldown-hours: 15", "cooldown-hours: 8761"),
                original.replace("cooldown-hours: 15", "cooldown-hours: 15.0"),
                original.replace("block-vanilla-resurrection: true", "block-vanilla-resurrection: 'true'"),
                original.replace("totem:\n  limit: 3\n  cooldown-hours: 15\n  block-vanilla-resurrection: true", "totem: false"),
                original.replace("totem-invalid: '<red>Batas totem harus bilangan bulat 1 sampai 1000.</red>'", "totem-invalid: false")}) {
            Files.writeString(file, text);
            assertThrows(IllegalArgumentException.class, service::reload);
            assertSame(previous, service.current());
            assertEquals(text, Files.readString(file));
        }
    }
}