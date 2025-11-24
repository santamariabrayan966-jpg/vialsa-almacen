package com.vialsa.almacen.security;

import com.vialsa.almacen.dao.Jdbc.JdbcPermisoDao;
import com.vialsa.almacen.model.Permiso;
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
     * Este método NO aplica para ROLE_CLIENTE.
     */
    public boolean tieneAcceso(Authentication authentication, String modulo) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Integer idRolEncontrado = null;

        for (GrantedAuthority authority : authentication.getAuthorities()) {

            String nombreRol = authority.getAuthority().trim();

            // 🔹 Clientes NO usan esta lógica de permisos internos
            if (nombreRol.equals("ROLE_CLIENTE")) {
                return false; // No tiene acceso a módulos internos
            }

            nombreRol = nombreRol.replace("ROLE_", "");

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

        return permisos.stream()
                .anyMatch(p -> p.getModulo().equalsIgnoreCase(modulo) && p.isPuedeAcceder());
    }
}
