package com.dentia.security;

import com.dentia.dao.interfaces.UsuarioDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@Order(1)
public class InternalSecurityConfig {

    private static final Logger log =
            LoggerFactory.getLogger(InternalSecurityConfig.class);

    private final CustomAuthorizationService authService;
    private final CustomSuccessHandler successHandler;
    private final UsuarioDao usuarioDao;

    public InternalSecurityConfig(
            CustomAuthorizationService authService,
            CustomSuccessHandler successHandler,
            UsuarioDao usuarioDao
    ) {
        this.authService = authService;
        this.successHandler = successHandler;
        this.usuarioDao = usuarioDao;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain internalFilterChain(
            HttpSecurity http,
            DaoAuthenticationProvider authenticationProvider,
            DbUserDetailsService userDetailsService,
            DentiaRememberMeSettings rememberMeSettings
    ) throws Exception {

        http.authenticationProvider(authenticationProvider);
        http.addFilterBefore(new LoginRateLimitFilter(), UsernamePasswordAuthenticationFilter.class);

        http
                .securityMatcher("/interno/**")

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/interno/login",
                                "/css/**",
                                "/js/**",
                                "/img/**",
                                "/uploads/**"
                        ).permitAll()

                        // =============================
                        // GESTIÓN
                        // =============================
                        .requestMatchers("/interno/productos/**").access((authc, ctx) ->
                                new AuthorizationDecision(
                                        authService.tieneAcceso(authc.get(), "productos")))

                        .requestMatchers("/interno/proveedores/**").access((authc, ctx) ->
                                new AuthorizationDecision(
                                        authService.tieneAcceso(authc.get(), "proveedores")))

                        .requestMatchers("/interno/sedes/**").access((authc, ctx) ->
                                new AuthorizationDecision(
                                        authService.tieneAcceso(authc.get(), "sedes")))

                        .requestMatchers("/interno/clientes/**").access((authc, ctx) ->
                                new AuthorizationDecision(
                                        authService.tieneAcceso(authc.get(), "clientes")))

                        // =============================
                        // TRANSACCIONES
                        // =============================
                        .requestMatchers("/interno/compras/**").access((authc, ctx) ->
                                new AuthorizationDecision(
                                        authService.tieneAcceso(authc.get(), "compras")))

                        .requestMatchers("/interno/ventas/**").access((authc, ctx) ->
                                new AuthorizationDecision(
                                        authService.tieneAcceso(authc.get(), "ventas")))


                        // =============================
                        // INVENTARIO
                        // Importante:
                        // Las rutas específicas van antes que /interno/inventario/**
                        // =============================
                        .requestMatchers("/interno/inventario/existencias/**").access((authc, ctx) ->
                                new AuthorizationDecision(
                                        authService.tieneAcceso(authc.get(), "inventario_existencias")))

                        .requestMatchers("/interno/inventario/transferencias/**").access((authc, ctx) ->
                                new AuthorizationDecision(
                                        authService.tieneAcceso(authc.get(), "inventario_transferencias")))

                        .requestMatchers("/interno/inventario/kardex/**").access((authc, ctx) ->
                                new AuthorizationDecision(
                                        authService.tieneAcceso(authc.get(), "inventario_kardex")))

                        .requestMatchers("/interno/inventario/ajustes/**").access((authc, ctx) ->
                                new AuthorizationDecision(
                                        authService.tieneAcceso(authc.get(), "inventario_ajustes")))

                        // Respaldo para cualquier otra ruta dentro de /interno/inventario/**
                        .requestMatchers("/interno/inventario/**").access((authc, ctx) ->
                                new AuthorizationDecision(
                                        authService.tieneAcceso(authc.get(), "inventario_existencias")
                                                || authService.tieneAcceso(authc.get(), "inventario_transferencias")
                                                || authService.tieneAcceso(authc.get(), "inventario_kardex")
                                                || authService.tieneAcceso(authc.get(), "inventario_ajustes")))

                        // =============================
                        // LOGÍSTICA
                        // =============================
                        .requestMatchers("/interno/entregas/**").access((authc, ctx) ->
                                new AuthorizationDecision(
                                        authService.tieneAcceso(authc.get(), "entregas_preparacion")
                                                || authService.tieneAcceso(authc.get(), "entregas_entrega")
                                                || authService.tieneAcceso(authc.get(), "entregas_historial")))

                        .requestMatchers("/interno/traslados-internos/**").access((authc, ctx) ->
                                new AuthorizationDecision(
                                        authService.tieneAcceso(authc.get(), "traslados_internos")))

                        // =============================
                        // USUARIOS
                        // =============================
                        .requestMatchers("/interno/usuarios/**").access((authc, ctx) ->
                                new AuthorizationDecision(
                                        authService.tieneAcceso(authc.get(), "usuarios")))

                        .requestMatchers("/interno/roles/**").access((authc, ctx) ->
                                new AuthorizationDecision(
                                        authService.tieneAcceso(authc.get(), "roles")))

                        .requestMatchers("/interno/profesionales/**").access((authc, ctx) ->
                                new AuthorizationDecision(
                                        authService.tieneAcceso(authc.get(), "profesionales")))

                        .requestMatchers("/interno/especialidades/**", "/interno/tipos-atencion/**").access((authc, ctx) ->
                                new AuthorizationDecision(authService.tieneAcceso(authc.get(), "servicios")
                                        || authService.tieneAcceso(authc.get(), "profesionales")
                                        || authService.tieneAcceso(authc.get(), "citas")))

                        .requestMatchers("/interno/pacientes/**").access((authc, ctx) ->
                                new AuthorizationDecision(authService.tieneAcceso(authc.get(), "pacientes")))

                        .requestMatchers("/interno/configuracion/**").access((authc, ctx) ->
                                new AuthorizationDecision(authService.tieneAcceso(authc.get(), "configuracion")))

                        .requestMatchers("/interno/citas/**").access((authc, ctx) ->
                                new AuthorizationDecision(
                                        authService.tieneAcceso(authc.get(), "citas")))

                        // =============================
                        // ANÁLISIS
                        // =============================
                        .requestMatchers("/interno/dashboard/**").access((authc, ctx) ->
                                new AuthorizationDecision(
                                        authService.tieneAcceso(authc.get(), "dashboard")))

                        .requestMatchers("/interno/apariencia/**").access((authc, ctx) ->
                                new AuthorizationDecision(
                                        authService.tieneAcceso(authc.get(), "configuracion")))

                        .requestMatchers("/interno/reportes/**").access((authc, ctx) ->
                                new AuthorizationDecision(
                                        authService.tieneAcceso(authc.get(), "reportes")))


                        .requestMatchers("/interno/modulos/**").access((authc, ctx) ->
                                new AuthorizationDecision(authService.esUsuarioInterno(authc.get())))

                        // Los controladores clinicos y de gestion comprueban ademas
                        // el permiso concreto de cada submodulo y operacion.
                        .requestMatchers("/interno/clinica/**", "/interno/gestion/**").access((authc, ctx) ->
                                new AuthorizationDecision(authService.esUsuarioInterno(authc.get())))



                        // =============================
                        // PERFIL / SESIÓN INTERNA
                        // Estas rutas deben estar autenticadas,
                        // no dependen de permisos de módulo.
                        // =============================
                        .requestMatchers(
                                "/interno/perfil/**",
                                "/interno/sesion/**"
                        ).access((authc, ctx) ->
                                new AuthorizationDecision(authService.esUsuarioInterno(authc.get())))

                        .anyRequest().denyAll()
                )

                .formLogin(form -> form
                        .loginPage("/interno/login")
                        .loginProcessingUrl("/interno/login")
                        .successHandler(successHandler)
                        .failureHandler(authenticationFailureHandler())
                        .permitAll()
                )

                .rememberMe(remember -> remember
                        .key(rememberMeSettings.internalKey())
                        .userDetailsService(userDetailsService)
                        .rememberMeParameter("remember-device")
                        .rememberMeCookieName(DentiaRememberMeSettings.INTERNAL_COOKIE)
                        .tokenValiditySeconds(rememberMeSettings.validitySeconds())
                        .useSecureCookie(rememberMeSettings.secureCookies())
                        .alwaysRemember(false)
                )

                .logout(logout -> logout
                        .logoutUrl("/interno/logout")
                        .logoutSuccessUrl("/interno/login?logout")
                        .deleteCookies("JSESSIONID", "ADMINSESSION", DentiaRememberMeSettings.INTERNAL_COOKIE)
                        .invalidateHttpSession(true)
                )

                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {

        return (request, response, exception) -> {

            String username = request.getParameter("username");

            log.warn("Intento de inicio de sesion fallido");

            Throwable cause = exception.getCause();

            if (exception instanceof DisabledException ||
                    cause instanceof DisabledException) {

                String mensaje = exception.getMessage();

                if (mensaje == null && cause != null) {
                    mensaje = cause.getMessage();
                }

                if ("CUENTA_BLOQUEADA_TEMPORAL".equals(mensaje)) {
                    response.sendRedirect("/interno/login?bloqueado");
                } else {
                    response.sendRedirect("/interno/login?disabled");
                }

                return;
            }

            if (username != null && !username.isBlank()) {

                usuarioDao.incrementarIntentosFallidos(username);

                int intentos =
                        usuarioDao.obtenerIntentosFallidos(username);

                if (intentos >= 5) {

                    usuarioDao.bloquearTemporalmente(username);

                    response.sendRedirect("/interno/login?bloqueado");
                    return;
                }

                response.sendRedirect("/interno/login?error");

                return;
            }

            response.sendRedirect("/interno/login?error");
        };
    }
}
