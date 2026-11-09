package com.dentia.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Order(4)
public class PlatformSecurityConfig {

    @Bean
    @Order(4)
    SecurityFilterChain platformFilterChain(HttpSecurity http,
                                            CustomAuthorizationService authorizationService) throws Exception {
        http
                .securityMatcher("/api/**", "/uploads/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/uploads/productos/**",
                                "/uploads/marca/**",
                                "/uploads/login/**",
                                "/uploads/sedes/**"
                        ).permitAll()
                        .requestMatchers("/api/usuario/**").access((authentication, context) ->
                                new AuthorizationDecision(authorizationService.tieneAcceso(authentication.get(), "usuarios")))
                        .requestMatchers("/api/externo/**").access((authentication, context) ->
                                new AuthorizationDecision(
                                        authorizationService.tieneAcceso(authentication.get(), "pacientes")
                                                || authorizationService.tieneAcceso(authentication.get(), "profesionales")
                                                || authorizationService.tieneAcceso(authentication.get(), "usuarios")
                                                || authorizationService.tieneAcceso(authentication.get(), "clientes")
                                                || authorizationService.tieneAcceso(authentication.get(), "ventas")
                                                || authorizationService.tieneAcceso(authentication.get(), "inventario_existencias")))
                        .requestMatchers("/api/notificaciones/pacientes-rapido").access((authentication, context) ->
                                new AuthorizationDecision(authorizationService.tieneAcceso(authentication.get(), "pacientes")))
                        .requestMatchers("/api/notificaciones/**").access((authentication, context) ->
                                new AuthorizationDecision(authorizationService.tieneAcceso(authentication.get(), "citas")))
                        .requestMatchers("/uploads/usuarios/**", "/uploads/profesionales/**").access((authentication, context) ->
                                new AuthorizationDecision(authorizationService.esUsuarioInterno(authentication.get())))
                        .requestMatchers("/api/**").access((authentication, context) ->
                                new AuthorizationDecision(
                                        authorizationService.esUsuarioInterno(authentication.get())
                                ))
                        .anyRequest().access((authentication, context) ->
                                new AuthorizationDecision(
                                        authorizationService.esUsuarioInterno(authentication.get())
                                ))
                );
        return http.build();
    }
}
