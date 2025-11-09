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

    // Verifica si el rol actual tiene permiso sobre un módulo
    public boolean tieneAcceso(Authentication authentication, String modulo) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Integer idRolEncontrado = null;

        // Recorremos todas las authorities del usuario (ROLE_ADMIN, ROLE_SUPERVISOR, etc.)
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String nombreRol = authority.getAuthority()
                    .replace("ROLE_", "")
                    .trim(); // por si acaso

            Integer idRol = permisoDao.obtenerIdRolPorNombre(nombreRol);
            if (idRol != null) {
                idRolEncontrado = idRol;
                break; // usamos el primer rol válido que exista en la tabla roles
            }
        }

        if (idRolEncontrado == null) {
            return false; // el rol del usuario no coincide con ningún registro de roles
        }

        // Obtenemos los permisos de ese rol
        List<Permiso> permisos = permisoDao.obtenerPorRol(idRolEncontrado);

        // Verificamos si el módulo tiene acceso
        return permisos.stream()
                .anyMatch(p -> p.getModulo().equalsIgnoreCase(modulo) && p.isPuedeAcceder());
    }
}
