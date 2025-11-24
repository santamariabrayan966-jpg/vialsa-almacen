package com.vialsa.almacen.security;

import com.vialsa.almacen.dao.interfaces.UsuarioDao;
import com.vialsa.almacen.model.Cliente;
import com.vialsa.almacen.model.Usuario;
import com.vialsa.almacen.service.ClienteService;
import com.vialsa.almacen.service.UsuarioServiceCliente;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final UsuarioDao usuarioDao;
    private final UsuarioServiceCliente usuarioServiceCliente;
    private final ClienteService clienteService;

    public CustomOAuth2UserService(
            UsuarioDao usuarioDao,
            UsuarioServiceCliente usuarioServiceCliente,
            ClienteService clienteService
    ) {
        this.usuarioDao = usuarioDao;
        this.usuarioServiceCliente = usuarioServiceCliente;
        this.clienteService = clienteService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {

        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate =
                new DefaultOAuth2UserService();

        OAuth2User oauthUser = delegate.loadUser(request);

        String registrationId = request.getClientRegistration().getRegistrationId();
        if (!"google".equalsIgnoreCase(registrationId)) {
            return oauthUser;
        }

        // === ATRIBUTOS ===
        Map<String, Object> attr = oauthUser.getAttributes();

        String email  = (String) attr.get("email");
        String name   = (String) attr.getOrDefault("name", "");
        String given  = (String) attr.getOrDefault("given_name", "");
        String family = (String) attr.getOrDefault("family_name", "");
        String picture = (String) attr.get("picture");

        if (email == null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_found"),
                    "Google no devolvió email"
            );
        }

        log.info(">>> LOGIN GOOGLE: {}", email);

        // === BUSCAMOS EL USUARIO ===
        Usuario usuario = usuarioDao.findByCorreo(email).orElse(null);

        if (usuario == null) {
            log.info(">>> Usuario NO existe. Creando CLIENTE.");

            Usuario nuevo = new Usuario();
            nuevo.setCorreo(email);
            nuevo.setNombreUsuario(email);
            nuevo.setFoto(picture);
            nuevo.setActivo(true);

            String fullName = (!given.isBlank() || !family.isBlank())
                    ? (given + " " + family).trim()
                    : name;

            nuevo.setNombres(fullName);
            nuevo.setApellidos(null);

            nuevo.setIdRol(8); // CLIENTE

            usuario = usuarioServiceCliente.registrarNuevoCliente(nuevo);

            Cliente c = new Cliente();
            c.setNombres(usuario.getNombres());
            c.setApellidos(usuario.getApellidos());
            c.setCorreo(usuario.getCorreo());
            c.setIdUsuario(usuario.getIdUsuario());

            clienteService.crear(c);

        } else {
            log.info(">>> Usuario YA existe. ID={}", usuario.getIdUsuario());
        }

        // === ASIGNAR ROL ===
        String role = switch (usuario.getIdRol()) {
            case 8 -> "ROLE_CLIENTE";
            case 1 -> "ROLE_ADMIN";
            default -> "ROLE_USER";
        };

        log.info(">>> ROL DEFINITIVO PARA LOGIN = {}", role);

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));

        // agregar "role" a los atributos
        Map<String, Object> att = new HashMap<>(attr);
        att.put("role", role);

        return new DefaultOAuth2User(authorities, att, "email");
    }
}
