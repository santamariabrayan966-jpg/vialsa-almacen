package com.dentia.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;

@Configuration
public class DentiaRememberMeSettings {

    public static final String PATIENT_COOKIE = "DENTIA_PATIENT_REMEMBER";
    public static final String INTERNAL_COOKIE = "DENTIA_INTERNAL_REMEMBER";

    private static final Logger log = LoggerFactory.getLogger(DentiaRememberMeSettings.class);
    private static final Set<String> REMEMBER_ME_COOKIES = Set.of(PATIENT_COOKIE, INTERNAL_COOKIE);

    private final int validitySeconds;
    private final boolean secureCookies;
    private final String effectiveKey;

    public DentiaRememberMeSettings(
            @Value("${dentia.security.remember-me-days:7}") int rememberMeDays,
            @Value("${dentia.security.remember-me-key:}") String configuredKey,
            @Value("${dentia.security.cookies-secure:false}") boolean secureCookies
    ) {
        if (rememberMeDays < 1 || rememberMeDays > 30) {
            throw new IllegalArgumentException("dentia.security.remember-me-days debe estar entre 1 y 30");
        }
        if (configuredKey != null && !configuredKey.isBlank() && configuredKey.trim().length() < 32) {
            throw new IllegalArgumentException("DENTIA_REMEMBER_ME_KEY debe tener al menos 32 caracteres");
        }
        this.validitySeconds = Math.toIntExact(Duration.ofDays(rememberMeDays).getSeconds());
        this.secureCookies = secureCookies;
        this.effectiveKey = configuredKey == null || configuredKey.isBlank()
                ? generateEphemeralKey()
                : configuredKey.trim();

        if (configuredKey == null || configuredKey.isBlank()) {
            log.warn("DENTIA_REMEMBER_ME_KEY no esta configurada; se usara una clave efimera y las sesiones recordadas no sobreviviran al reinicio.");
        }
    }

    public int validitySeconds() {
        return validitySeconds;
    }

    public boolean secureCookies() {
        return secureCookies;
    }

    public String patientKey() {
        return effectiveKey + ":paciente";
    }

    public String internalKey() {
        return effectiveKey + ":interno";
    }

    @Bean
    CookieSameSiteSupplier dentiaRememberMeSameSiteSupplier() {
        return CookieSameSiteSupplier.ofLax()
                .when(cookie -> REMEMBER_ME_COOKIES.contains(cookie.getName()));
    }

    private static String generateEphemeralKey() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
