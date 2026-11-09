package com.dentia.security;

import com.dentia.dao.interfaces.ICuentaPacienteDao;
import com.dentia.dao.interfaces.IPacienteDao;
import com.dentia.model.paciente.CuentaPaciente;
import com.dentia.model.paciente.Paciente;
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

import java.time.LocalDate;
import java.util.*;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final ICuentaPacienteDao cuentaPacienteDao;
    private final IPacienteDao pacienteDao;

    public CustomOAuth2UserService(ICuentaPacienteDao cuentaPacienteDao,
                                   IPacienteDao pacienteDao) {
        this.cuentaPacienteDao = cuentaPacienteDao;
        this.pacienteDao = pacienteDao;
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

        Map<String, Object> attr = oauthUser.getAttributes();

        String googleSub = (String) attr.get("sub");
        String email = (String) attr.get("email");
        String name = (String) attr.getOrDefault("name", "");
        String given = (String) attr.getOrDefault("given_name", "");
        String family = (String) attr.getOrDefault("family_name", "");

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_found"),
                    "Google no devolvió correo electrónico."
            );
        }

        if (googleSub == null || googleSub.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("sub_not_found"),
                    "Google no devolvió identificador único de cuenta."
            );
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        log.info(">>> LOGIN GOOGLE PACIENTE DentIA: {}", normalizedEmail);

        Optional<CuentaPaciente> cuentaOpt = cuentaPacienteDao.buscarPorCorreo(normalizedEmail);
        long idPaciente;

        if (cuentaOpt.isEmpty()) {
            log.info(">>> Paciente NO registrado previamente. Creando ficha médica y cuenta de acceso.");

            Paciente paciente = new Paciente();
            String nombreFinal = (!given.isBlank()) ? given.trim() : (!name.isBlank() ? name.trim() : "Paciente");
            String apellidoFinal = (!family.isBlank()) ? family.trim() : "Google";

            paciente.setNombres(nombreFinal);
            paciente.setApellidos(apellidoFinal);
            paciente.setFechaNacimiento(LocalDate.of(2000, 1, 1));
            paciente.setCelular("Sin registrar");
            paciente.setCorreo(normalizedEmail);
            paciente.setActivo(true);

            idPaciente = pacienteDao.crear(paciente);

            CuentaPaciente nuevaCuenta = new CuentaPaciente();
            nuevaCuenta.setIdPaciente(idPaciente);
            nuevaCuenta.setCorreo(normalizedEmail);
            nuevaCuenta.setPasswordHash("{noop}GOOGLE_OAUTH_" + UUID.randomUUID());
            nuevaCuenta.setActivo(true);

            cuentaPacienteDao.crear(nuevaCuenta);
            log.info(">>> Paciente y cuenta creados exitosamente. ID={}", idPaciente);
        } else {
            CuentaPaciente cuenta = cuentaOpt.get();
            idPaciente = cuenta.getIdPaciente();
            log.info(">>> Paciente existente en DentIA. ID Cuenta={}, ID Paciente={}", cuenta.getIdCuentaPaciente(), idPaciente);
        }

        List<GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_PACIENTE"));

        Map<String, Object> att = new HashMap<>(attr);
        att.put("role", "ROLE_PACIENTE");
        att.put("idPaciente", idPaciente);
        att.put("email", normalizedEmail);
        att.put("name", name);

        return new DefaultOAuth2User(authorities, att, "email");
    }
}