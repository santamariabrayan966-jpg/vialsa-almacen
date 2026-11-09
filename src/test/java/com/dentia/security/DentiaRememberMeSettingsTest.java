package com.dentia.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DentiaRememberMeSettingsTest {

    @Test
    void configuraSieteDiasYCookiesSeguras() {
        DentiaRememberMeSettings settings = new DentiaRememberMeSettings(7, "clave-de-prueba-suficiente-de-32-o-mas", true);

        assertEquals(604_800, settings.validitySeconds());
        assertTrue(settings.secureCookies());
    }

    @Test
    void separaCriptograficamentePacienteEInterno() {
        DentiaRememberMeSettings settings = new DentiaRememberMeSettings(7, "clave-compartida-suficientemente-larga", false);

        assertNotEquals(settings.patientKey(), settings.internalKey());
        assertTrue(settings.patientKey().endsWith(":paciente"));
        assertTrue(settings.internalKey().endsWith(":interno"));
    }

    @Test
    void rechazaDuracionesFueraDelLimiteSeguro() {
        assertThrows(IllegalArgumentException.class,
                () -> new DentiaRememberMeSettings(0, "clave", false));
        assertThrows(IllegalArgumentException.class,
                () -> new DentiaRememberMeSettings(31, "clave", false));
        assertThrows(IllegalArgumentException.class,
                () -> new DentiaRememberMeSettings(7, "clave", false));
    }
}
