package com.dentia.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleOAuthAvailabilityTest {

    @Test
    void exigeIdYSecretoReales() {
        assertFalse(new GoogleOAuthAvailability("", "secreto").isConfigured());
        assertFalse(new GoogleOAuthAvailability("cliente", "  ").isConfigured());
        assertFalse(new GoogleOAuthAvailability("not-configured", "secreto").isConfigured());
        assertFalse(new GoogleOAuthAvailability("cliente", "NOT-CONFIGURED").isConfigured());
        assertFalse(new GoogleOAuthAvailability(null, "secreto").isConfigured());
        assertTrue(new GoogleOAuthAvailability("cliente-real", "secreto-real").isConfigured());
    }
}
