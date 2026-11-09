package com.dentia.security;

import com.dentia.dao.Jdbc.JdbcPermisoDao;
import com.dentia.model.interno.Permiso;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("authService")
public class CustomAuthorizationService {

    private final JdbcPermisoDao permisoDao;

    public CustomAuthorizationService(JdbcPermisoDao permisoDao) {
        this.permisoDao = permisoDao;
    }

    /**
     * Verifica si el usuario tiene acceso a un módulo interno.
     * Este método rechaza explícitamente ROLE_CLIENTE y ROLE_PACIENTE.
     */
    public boolean tieneAcceso(Authentication authentication, String modulo) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (modulo == null || modulo.isBlank()) {
            return false;
        }

        Integer idRolEncontrado = null;

        for (GrantedAuthority authority : authentication.getAuthorities()) {

            if (authority == null || authority.getAuthority() == null) {
                continue;
            }

            String nombreRol = authority.getAuthority().trim();

            // Clientes y Pacientes NO usan esta lógica de permisos internos
            if (nombreRol.equalsIgnoreCase("ROLE_CLIENTE") || nombreRol.equalsIgnoreCase("ROLE_PACIENTE")) {
                return false;
            }

            nombreRol = nombreRol.replace("ROLE_", "").trim();

            // ADMIN tiene acceso total a todos los módulos internos
            if (nombreRol.equalsIgnoreCase("ADMIN")) {
                return true;
            }

            Integer idRol = permisoDao.obtenerIdRolPorNombre(nombreRol);

            if (idRol != null) {
                idRolEncontrado = idRol;
                break;
            }
        }

        if (idRolEncontrado == null) {
            return false;
        }

        List<Permiso> permisos = permisoDao.obtenerPorRol(idRolEncontrado);

        if (permisos == null || permisos.isEmpty()) {
            return false;
        }

        return permisos.stream()
                .anyMatch(p ->
                        p.getModulo() != null
                                && p.getModulo().equalsIgnoreCase(modulo)
                                && p.isPuedeAcceder()
                );
    }

    public boolean esUsuarioInterno(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(rol -> rol != null && rol.startsWith("ROLE_"))
                .anyMatch(rol -> !"ROLE_ANONYMOUS".equalsIgnoreCase(rol)
                        && !"ROLE_CLIENTE".equalsIgnoreCase(rol)
                        && !"ROLE_PACIENTE".equalsIgnoreCase(rol));
    }
}
