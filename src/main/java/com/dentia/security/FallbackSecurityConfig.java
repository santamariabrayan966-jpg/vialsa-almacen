package com.dentia.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

/** Protege tambien las rutas no incluidas en los cuatro portales conocidos. */
@Configuration
@Order(5)
public class FallbackSecurityConfig {
    @Bean
    @Order(5)
    SecurityFilterChain fallbackFilterChain(HttpSecurity http) throws Exception {
        // Esta cadena no debe capturar rutas que pertenecen a otro portal, incluso
        // si Spring registra las cadenas en un orden inesperado.
        http.securityMatcher(new NegatedRequestMatcher(new OrRequestMatcher(
                new AntPathRequestMatcher("/interno/**"),
                new AntPathRequestMatcher("/paciente/**"),
                new AntPathRequestMatcher("/api/**"),
                new AntPathRequestMatcher("/uploads/**"),
                new AntPathRequestMatcher("/"),
                new AntPathRequestMatcher("/disponibilidad"),
                new AntPathRequestMatcher("/reservar-cita"),
                new AntPathRequestMatcher("/oauth2/**"),
                new AntPathRequestMatcher("/login/**"),
                new AntPathRequestMatcher("/app.webmanifest"),
                new AntPathRequestMatcher("/service-worker.js"),
                new AntPathRequestMatcher("/pwa/**"),
                new AntPathRequestMatcher("/recuperar-password"),
                new AntPathRequestMatcher("/validar-codigo-password"),
                new AntPathRequestMatcher("/restablecer-password"))));
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/error", "/css/**", "/js/**", "/img/**",
                        "/vendor/**", "/models/**", "/sw.js", "/favicon.ico").permitAll()
                .anyRequest().denyAll());
        return http.build();
    }
}
