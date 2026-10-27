package com.bangzachery.vitae.vitaemanager.roleplay;

import com.bangzachery.vitae.vitaemanager.core.config.ConfigurationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import static org.junit.jupiter.api.Assertions.*;

class RoleplayConfigurationTest {
    @TempDir Path directory;

    private String defaults() throws Exception {
        try (var resource = getClass().getResourceAsStream("/config.yml")) {
            return new String(
                    Objects.requireNonNull(resource).readAllBytes(),
                    StandardCharsets.UTF_8
            ).replace("\r\n", "\n").replace("\r", "\n");
        }
    }

    @Test void oldConfigUsesRoleplayDefaultsWithoutRewrite() throws Exception {
        Path file = directory.resolve("config.yml");
        String legacy = "config-version: 1\ncore:\n  debug: false\nmessages:\n  prefix: '<gold>Custom</gold>'\n";
        Files.writeString(file, legacy);
        var service = new ConfigurationService(file, defaults());
        service.initialize();
        assertEquals(new ConfigurationService.RoleplaySettings(200, 30, 256, 2), service.current().roleplay());
        assertNotNull(service.current().messages().get("carry-released"));
        assertEquals(legacy, Files.readString(file));
    }

    @Test void validReloadUpdatesAllSettingsTogether() throws Exception {
        Path file = directory.resolve("config.yml");
        var service = new ConfigurationService(file, defaults());
        service.initialize();
        String edited = defaults().replace("text-duration-ticks: 200", "text-duration-ticks: 100")
                .replace("ooc-radius: 30", "ooc-radius: 40")
                .replace("max-text-length: 256", "max-text-length: 128")
                .replace("carry-distance: 2", "carry-distance: 3");
        Files.writeString(file, edited);
        service.reload();
        assertEquals(new ConfigurationService.RoleplaySettings(100, 40, 128, 3), service.current().roleplay());
    }

    @Test void configuredBottomClearanceReloadsWithoutRewritingUserFile() throws Exception {
        Path file = directory.resolve("config.yml"); var service = new ConfigurationService(file, defaults()); service.initialize();
        String edited = defaults().replace("text-head-clearance: 0.12", "text-head-clearance: 0.08"); Files.writeString(file, edited); service.reload();
        assertEquals(.08, service.current().roleplay().textHeadClearance()); assertEquals(edited, Files.readString(file));
    }

    @Test void invalidBottomClearanceKeepsEntirePreviousSnapshot() throws Exception {
        Path file = directory.resolve("config.yml"); String defaults = defaults(); var service = new ConfigurationService(file, defaults); service.initialize(); var old = service.current();
        for (String value : new String[]{"-0.01", "1.01", ".NaN", ".Inf", "'0.12'", "false"}) {
            String invalid = defaults.replace("text-head-clearance: 0.12", "text-head-clearance: " + value); Files.writeString(file, invalid);
            assertThrows(IllegalArgumentException.class, service::reload, value); assertSame(old, service.current()); assertEquals(invalid, Files.readString(file));
        }
    }

    @Test void legacyPublicConstructorRetainsDefaultClearanceAndValues() {
        var oldApi = new ConfigurationService.RoleplaySettings(100, 40, 128, 3);
        assertEquals(new ConfigurationService.RoleplaySettings(100, 40, 128, 3, .12), oldApi);
    }

    @Test void invalidRangesTypesOrSectionKeepPreviousSnapshot() throws Exception {
        Path file = directory.resolve("config.yml");
        String defaults = defaults();
        var service = new ConfigurationService(file, defaults);
        service.initialize();
        var old = service.current();
        for (String invalid : new String[]{
                defaults.replace("text-duration-ticks: 200", "text-duration-ticks: 0"),
                defaults.replace("ooc-radius: 30", "ooc-radius: 129"),
                defaults.replace("max-text-length: 256", "max-text-length: 513"),
                defaults.replace("carry-distance: 2", "carry-distance: 2.0"),
                defaults.replace("carry-distance: 2", "carry-distance: '2'"),
                defaults.replaceFirst("(?s)roleplay:.*?\\ntotem:", "roleplay: false\n\ntotem:")}) {
            Files.writeString(file, invalid);
            assertThrows(IllegalArgumentException.class, service::reload);
            assertSame(old, service.current());
            assertEquals(invalid, Files.readString(file));
        }
    }
}
