package com.vialsa.almacen.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final DbUserDetailsService userDetailsService;
    private final CustomAuthorizationService authService;
    private final CustomSuccessHandler successHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;

    public SecurityConfig(DbUserDetailsService userDetailsService,
                          CustomAuthorizationService authService,
                          CustomSuccessHandler successHandler,
                          CustomOAuth2UserService customOAuth2UserService,
                          CustomOidcUserService customOidcUserService) {

        this.userDetailsService = userDetailsService;
        this.authService = authService;
        this.successHandler = successHandler;
        this.customOAuth2UserService = customOAuth2UserService;
        this.customOidcUserService = customOidcUserService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   DaoAuthenticationProvider authenticationProvider)
            throws Exception {

        http.authenticationProvider(authenticationProvider);

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login",
                                "/register",
                                "/css/**",
                                "/js/**",
                                "/img/**",
                                "/images/**",
                                "/assets/**",
                                "/vendor/**",
                                "/webjars/**",
                                "/uploads/**",
                                "/favicon.ico"
                        ).permitAll()

                        .requestMatchers("/clientes/importar").permitAll()
                        .requestMatchers("/clientes/exportar/**").permitAll()


                        .requestMatchers("/api/externo/**").permitAll()

                        // 👉 Solo la página principal de tienda es pública
                        .requestMatchers("/", "/tienda", "/categorias/**", "/producto/**").permitAll()

                        .requestMatchers("/registro-cliente").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()

                        .requestMatchers("/api/login").permitAll()

                        // 👉 Rutas de cliente
                        .requestMatchers("/carrito/**", "/checkout/**",
                                "/tienda/mis-pedidos/**", "/mis-pedidos/**")
                        .hasRole("CLIENTE")

                        // 👉 Perfil de la TIENDA (cliente) – requiere login
                        .requestMatchers("/tienda/perfil", "/tienda/perfil/actualizar")
                        .authenticated()

                        // 👉 Panel interno
                        .requestMatchers("/dashboard/**").authenticated()

                        .requestMatchers("/productos/**").access((authc, ctx) ->
                                new AuthorizationDecision(authService.tieneAcceso(authc.get(), "productos")))

                        .requestMatchers("/compras/**").access((authc, ctx) ->
                                new AuthorizationDecision(authService.tieneAcceso(authc.get(), "compras")))

                        .requestMatchers("/ventas/**").access((authc, ctx) ->
                                new AuthorizationDecision(authService.tieneAcceso(authc.get(), "ventas")))

                        .requestMatchers("/inventario/**").access((authc, ctx) ->
                                new AuthorizationDecision(authService.tieneAcceso(authc.get(), "inventario")))

                        // Perfil de usuario interno
                        .requestMatchers("/usuarios/perfil/**").authenticated()

                        .requestMatchers("/usuarios/**").access((authc, ctx) ->
                                new AuthorizationDecision(authService.tieneAcceso(authc.get(), "usuarios")))

                        .requestMatchers("/roles/**").access((authc, ctx) ->
                                new AuthorizationDecision(authService.tieneAcceso(authc.get(), "roles")))

                        .anyRequest().authenticated()


                )

                // ============================
                // LOGIN FORMULARIO
                // ============================
                .formLogin(form -> form
                        .loginPage("/tienda")
                        .loginProcessingUrl("/login")
                        .successHandler(successHandler)
                        .failureHandler(authenticationFailureHandler())
                        .permitAll()
                )

                // ============================
                // LOGIN GOOGLE OAUTH2 + OIDC
                // ============================
                .oauth2Login(oauth -> oauth
                        .loginPage("/tienda")
                        .userInfoEndpoint(user -> {
                            user.userService(customOAuth2UserService);     // OAuth2 normal
                            user.oidcUserService(customOidcUserService);   // Google OpenID
                        })
                        .successHandler(successHandler)
                )

                // ============================
                // LOGOUT
                // ============================
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/tienda")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                )

                // ============================
                // CSRF
                // ============================

                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        "/api/**",
                        "/clientes/**",          // ⬅️ AGREGA ESTO
                        "/clientes/importar",    // (ya estaba)
                        "/clientes/exportar/**"  // (ya estaba)
                ))
        ;
        http.headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
        );



        return http.build();
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {

            log.info(">>> ERROR LOGIN: {} - {}", exception.getClass(), exception.getMessage());

            Throwable cause = exception.getCause();

            if (exception instanceof DisabledException ||
                    (cause instanceof DisabledException)) {
                response.sendRedirect("/tienda?disabled");
            } else {
                response.sendRedirect("/tienda?error");
            }
        };
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
