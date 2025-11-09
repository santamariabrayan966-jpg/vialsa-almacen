package com.vialsa.almacen.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final DbUserDetailsService userDetailsService;
    private final CustomAuthorizationService authService;

    public SecurityConfig(DbUserDetailsService userDetailsService,
                          CustomAuthorizationService authService) {
        this.userDetailsService = userDetailsService;
        this.authService = authService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authenticationProvider(authenticationProvider());

        http
                .authorizeHttpRequests(auth -> auth
                        // recursos públicos
                        .requestMatchers(
                                "/login",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico"
                        ).permitAll()

                        // dashboard
                        .requestMatchers("/dashboard/**").authenticated()

                        // módulos con permisos por rol
                        .requestMatchers("/productos/**").access((authentication, context) ->
                                new AuthorizationDecision(authService.tieneAcceso(authentication.get(), "productos")))
                        .requestMatchers("/compras/**").access((authentication, context) ->
                                new AuthorizationDecision(authService.tieneAcceso(authentication.get(), "compras")))
                        .requestMatchers("/ventas/**").access((authentication, context) ->
                                new AuthorizationDecision(authService.tieneAcceso(authentication.get(), "ventas")))
                        .requestMatchers("/inventario/**").access((authentication, context) ->
                                new AuthorizationDecision(authService.tieneAcceso(authentication.get(), "inventario")))
                        .requestMatchers("/usuarios/perfil/**").authenticated()
                        .requestMatchers("/usuarios/**").access((authentication, context) ->
                                new AuthorizationDecision(authService.tieneAcceso(authentication.get(), "usuarios")))
                        .requestMatchers("/roles/**").access((authentication, context) ->
                                new AuthorizationDecision(authService.tieneAcceso(authentication.get(), "roles")))

                        .anyRequest().authenticated()
                )

                // login
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureHandler(authenticationFailureHandler())   // ← importante
                        .permitAll()
                )

                // logout
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                // csrf (si tienes APIs)
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));

        return http.build();
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            log.info(">>> ERROR LOGIN: {} - {}", exception.getClass(), exception.getMessage());

            // Miramos también la causa interna
            Throwable cause = exception.getCause();

            if (exception instanceof DisabledException ||
                    (cause instanceof DisabledException) ||
                    "ROL_INACTIVO".equalsIgnoreCase(exception.getMessage()) ||
                    (cause != null && "ROL_INACTIVO".equalsIgnoreCase(cause.getMessage()))) {

                response.sendRedirect("/login?disabled");
            } else {
                response.sendRedirect("/login?error");
            }
        };
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }
}
