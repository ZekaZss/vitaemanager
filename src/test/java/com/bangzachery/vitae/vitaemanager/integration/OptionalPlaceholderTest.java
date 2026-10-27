package com.bangzachery.vitae.vitaemanager.integration;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OptionalPlaceholderTest {
    @Test void lifecycleFacadeLoadsWithNoPlaceholderApiOnRuntimeClasspath() throws Exception {
        assertThrows(ClassNotFoundException.class, () -> Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion"));
        Class<?> facade = Class.forName("com.bangzachery.vitae.vitaemanager.integration.PlaceholderHook");
        assertNotNull(facade.getDeclaredMethod("start"));
        assertNotNull(facade.getDeclaredMethod("close"));
        assertNotNull(Class.forName("com.bangzachery.vitae.vitaemanager.Vitaemanager"));
    }
}
