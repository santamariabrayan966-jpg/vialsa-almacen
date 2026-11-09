package com.dentia.security;

import com.dentia.service.paciente.PatientUserDetailsService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Order(2)
public class PatientSecurityConfig {

    @Bean("patientAuthenticationProvider")
    public AuthenticationProvider patientAuthenticationProvider(
            PatientUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    @Order(2)
    public SecurityFilterChain patientFilterChain(
            HttpSecurity http,
            @Qualifier("patientAuthenticationProvider") AuthenticationProvider provider,
            PatientUserDetailsService userDetailsService,
            DentiaRememberMeSettings rememberMeSettings
    ) throws Exception {
        http
                .securityMatcher("/paciente/**")
                .authenticationProvider(provider)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/paciente/login",
                                "/paciente/registro",
                                "/paciente/login-google-demo" // Alias de redirección OAuth; no autentica.
                        ).permitAll()
                        .anyRequest().hasRole("PACIENTE")
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendRedirect("/?loginGoogleRequired"))
                )
                .rememberMe(remember -> remember
                        .key(rememberMeSettings.patientKey())
                        .userDetailsService(userDetailsService)
                        .rememberMeParameter("remember-me")
                        .rememberMeCookieName(DentiaRememberMeSettings.PATIENT_COOKIE)
                        .tokenValiditySeconds(rememberMeSettings.validitySeconds())
                        .useSecureCookie(rememberMeSettings.secureCookies())
                        .alwaysRemember(false)
                )
                .logout(logout -> logout
                        .logoutUrl("/paciente/logout")
                        .logoutSuccessUrl("/?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", DentiaRememberMeSettings.PATIENT_COOKIE)
                );
        return http.build();
    }
}
