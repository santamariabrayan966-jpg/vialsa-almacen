package com.dentia.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Expone en un solo lugar si el acceso de pacientes mediante Google puede utilizarse. */
@Component
public class GoogleOAuthAvailability {

    private final String clientId;
    private final String clientSecret;

    public GoogleOAuthAvailability(
            @Value("${spring.security.oauth2.client.registration.google.client-id:}") String clientId,
            @Value("${spring.security.oauth2.client.registration.google.client-secret:}") String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public boolean isConfigured() {
        return isValid(clientId) && isValid(clientSecret);
    }

    private boolean isValid(String value) {
        return value != null && !value.isBlank() && !"not-configured".equalsIgnoreCase(value.trim());
    }
}
