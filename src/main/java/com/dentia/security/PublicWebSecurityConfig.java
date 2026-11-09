package com.dentia.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Order(3)
public class PublicWebSecurityConfig {

    private final CustomSuccessHandler successHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;

    public PublicWebSecurityConfig(
            CustomSuccessHandler successHandler,
            CustomOAuth2UserService customOAuth2UserService,
            CustomOidcUserService customOidcUserService
    ) {
        this.successHandler = successHandler;
        this.customOAuth2UserService = customOAuth2UserService;
        this.customOidcUserService = customOidcUserService;
    }

    @Bean
    @Order(3)
    public SecurityFilterChain publicWebFilterChain(
            HttpSecurity http,
            ClientRegistrationRepository clientRegistrationRepository
    ) throws Exception {

        http
                .securityMatcher(
                        "/",
                        "/disponibilidad",
                        "/reservar-cita",
                        "/oauth2/**",
                        "/login/**",
                        "/app.webmanifest",
                        "/service-worker.js",
                        "/pwa/**",
                        "/recuperar-password",
                        "/validar-codigo-password",
                        "/restablecer-password"
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/disponibilidad",
                                "/reservar-cita",
                                "/oauth2/**",
                                "/login/**",
                                "/app.webmanifest",
                                "/service-worker.js",
                                "/pwa/**",
                                "/recuperar-password",
                                "/validar-codigo-password",
                                "/restablecer-password",
                                "/css/**",
                                "/js/**",
                                "/img/**",
                                "/uploads/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                .oauth2Login(oauth -> oauth
                        .loginPage("/paciente/login")
                        .authorizationEndpoint(auth -> auth
                                .authorizationRequestResolver(
                                        customAuthorizationRequestResolver(
                                                clientRegistrationRepository
                                        )
                                )
                        )
                        .userInfoEndpoint(user -> {
                            user.userService(customOAuth2UserService);
                            user.oidcUserService(customOidcUserService);
                        })
                        .successHandler(successHandler)
                )

                .formLogin(form -> form.disable())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers("/reservar-cita"));

        return http.build();
    }

    private OAuth2AuthorizationRequestResolver customAuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository
    ) {
        DefaultOAuth2AuthorizationRequestResolver defaultResolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository,
                        "/oauth2/authorization"
                );

        return new OAuth2AuthorizationRequestResolver() {
            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                OAuth2AuthorizationRequest authRequest = defaultResolver.resolve(request);
                return customize(authRequest);
            }

            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
                OAuth2AuthorizationRequest authRequest = defaultResolver.resolve(request, clientRegistrationId);
                return customize(authRequest);
            }

            private OAuth2AuthorizationRequest customize(OAuth2AuthorizationRequest authRequest) {
                if (authRequest == null) {
                    return null;
                }
                Map<String, Object> additionalParams = new HashMap<>(authRequest.getAdditionalParameters());
                additionalParams.put("prompt", "select_account");
                return OAuth2AuthorizationRequest.from(authRequest)
                        .additionalParameters(additionalParams)
                        .build();
            }
        };
    }
}
