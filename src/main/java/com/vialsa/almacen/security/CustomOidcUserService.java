package com.vialsa.almacen.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomOidcUserService extends OidcUserService {

    private final CustomOAuth2UserService customOAuth2UserService;

    public CustomOidcUserService(CustomOAuth2UserService customOAuth2UserService) {
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest request) {

        // Delegamos en OidcUserService de Spring (DEVUELVE OidcUser REAL)
        OidcUser oidcUser = super.loadUser(request);

        // Usamos tu servicio para sincronizar usuario y cliente
        var customUser = customOAuth2UserService.loadUser(request);

        // Obtenemos el rol asignado por CustomOAuth2UserService
        String role = (String) customUser.getAttributes().get("role");

        // Creamos authorities correctas
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));

        // Devolvemos un nuevo usuario con el mismo ID Token + user info + authorities modificadas
        return new org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser(
                authorities,
                oidcUser.getIdToken(),
                oidcUser.getUserInfo(),
                "email"
        );
    }
}
