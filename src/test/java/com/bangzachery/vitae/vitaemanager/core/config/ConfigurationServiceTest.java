package com.bangzachery.vitae.vitaemanager.core.config;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.InvalidConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationServiceTest {

    @TempDir
    Path directory;

    private static final String DEFAULTS = """
            config-version: 1
            core:
              debug: false
            messages:
              prefix: '<gold>Vitae</gold>'
              help: '<yellow>Bantuan</yellow>'
            """;

    @Test
    void bundledConfigurationInitializesSuccessfully() throws Exception {
        try (var resource = getClass().getResourceAsStream("/config.yml")) {
            assertNotNull(resource);

            String text = new String(
                    resource.readAllBytes(),
                    StandardCharsets.UTF_8
            );

            ConfigurationService service = new ConfigurationService(
                    directory.resolve("config.yml"),
                    text
            );

            service.initialize();

            assertNotNull(
                    service.current().messages().get("reload-failed")
            );
        }
    }

    @Test
    void invalidExistingConfigurationIsNotOverwrittenOnStartup()
            throws Exception {
        Path file = directory.resolve("config.yml");
        String broken = "core: [broken\n";

        Files.writeString(file, broken);

        ConfigurationService service =
                new ConfigurationService(file, DEFAULTS);

        assertThrows(
                InvalidConfigurationException.class,
                service::initialize
        );

        assertEquals(broken, Files.readString(file));
        assertThrows(IllegalStateException.class, service::current);
    }

    @Test
    void createsDefaultsOnlyWhenFileIsMissing() throws Exception {
        Path file = directory.resolve("nested/config.yml");

        ConfigurationService service =
                new ConfigurationService(file, DEFAULTS);

        service.initialize();

        assertEquals(DEFAULTS, Files.readString(file));
        assertFalse(service.current().debug());
    }

    @Test
    void legacyConfigurationAndUnknownFieldsAreNotRewritten()
            throws Exception {
        Path file = directory.resolve("config.yml");

        String legacy =
                "maintenance_mode: true\n"
                        + "stuck_items:\n"
                        + "  PAPER: 12\n"
                        + "custom: keep\n";

        Files.writeString(file, legacy);

        ConfigurationService service =
                new ConfigurationService(file, DEFAULTS);

        service.initialize();

        assertEquals(legacy, Files.readString(file));
        assertNotNull(service.current().messages().get("help"));
    }

    @Test
    void emptyLegacyConfigurationUsesDefaultsWithoutRewritingIt()
            throws Exception {
        Path file = directory.resolve("config.yml");
        Files.writeString(file, "");

        ConfigurationService service =
                new ConfigurationService(file, DEFAULTS);

        service.initialize();

        assertEquals("", Files.readString(file));
        assertFalse(service.current().debug());
    }

    @Test
    void invalidYamlKeepsThePreviousSnapshot() throws Exception {
        ConfigurationService service = ready();
        ConfigurationService.Snapshot previous = service.current();

        Files.writeString(
                directory.resolve("config.yml"),
                "core: [broken\n"
        );

        assertThrows(
                InvalidConfigurationException.class,
                service::reload
        );

        assertSame(previous, service.current());
    }

    @Test
    void invalidTypesAndSectionsAreRejected() throws Exception {
        ConfigurationService service = ready();
        ConfigurationService.Snapshot previous = service.current();

        for (String invalid : new String[]{
                "core:\n  debug: 'false'\n",
                "core: wrong\n",
                "messages: wrong\n",
                "messages:\n  help: 123\n",
                "config-version: 2\n"
        }) {
            Files.writeString(directory.resolve("config.yml"), invalid);

            assertThrows(
                    IllegalArgumentException.class,
                    service::reload
            );

            assertSame(previous, service.current());
        }
    }

    @Test
    void invalidMiniMessageDoesNotReplaceWorkingMessages()
            throws Exception {
        ConfigurationService service = ready();
        ConfigurationService.Snapshot previous = service.current();

        Files.writeString(
                directory.resolve("config.yml"),
                "messages:\n  help: '<red>unclosed'\n"
        );

        assertThrows(
                IllegalArgumentException.class,
                service::reload
        );

        assertSame(previous, service.current());
    }

    @Test
    void validReloadReplacesTheSnapshotAndMessagesAreImmutable()
            throws Exception {
        ConfigurationService service = ready();
        ConfigurationService.Snapshot previous = service.current();

        Files.writeString(
                directory.resolve("config.yml"),
                "core:\n  debug: true\n"
        );

        service.reload();

        assertNotSame(previous, service.current());
        assertTrue(service.current().debug());

        assertThrows(
                UnsupportedOperationException.class,
                () -> service.current().messages().put(
                        "help",
                        Component.empty()
                )
        );
    }

    @Test
    void missingFileDuringReloadIsNotRecreated() throws Exception {
        ConfigurationService service = ready();
        ConfigurationService.Snapshot previous = service.current();

        Path file = directory.resolve("config.yml");
        Files.delete(file);

        assertThrows(java.io.IOException.class, service::reload);
        assertSame(previous, service.current());
        assertFalse(Files.exists(file));
    }

    private ConfigurationService ready() throws Exception {
        ConfigurationService service = new ConfigurationService(
                directory.resolve("config.yml"),
                DEFAULTS
        );

        service.initialize();
        return service;
    }
}