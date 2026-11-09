package com.dentia.service.interno;

import com.dentia.dao.Jdbc.JdbcPermisoDao;
import com.dentia.model.interno.Permiso;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PermisoService {

    private static final List<String> MODULOS_BASE = List.of(
            "dashboard",
            "pacientes",
            "citas",
            "historia_clinica",
            "odontograma",
            "odontograma_2d",
            "odontograma_3d",
            "diagnosticos",
            "tratamientos",
            "plan_tratamiento",
            "atenciones",
            "pagos",
            "reportes",
            "presupuestos",
            "documentos",
            "seguimiento",
            "profesionales",
            "servicios",
            "usuarios",
            "roles",
            "configuracion"
    );

    private final JdbcPermisoDao permisoDao;

    public PermisoService(JdbcPermisoDao permisoDao) {
        this.permisoDao = permisoDao;
    }

    // 🔹 Permisos por rol
    public List<Permiso> obtenerPorRol(int idRol) {
        return permisoDao.obtenerPorRol(idRol);
    }

    public void guardarPermisos(int idRol, List<Permiso> permisos) {
        permisoDao.guardarPermisos(idRol, permisos);
    }

    // 🔹 Mapa de permisos para el usuario logueado
    public Map<String, Boolean> getPermisos(Authentication auth) {

        if (auth == null || !auth.isAuthenticated()) {
            return Map.of();
        }

        String nombreRol = obtenerNombreRol(auth);

        if (nombreRol == null || nombreRol.isBlank()) {
            return Map.of();
        }

        // ✅ ADMIN ve todos los módulos, aunque todavía no existan en la tabla permisos
        if ("ADMIN".equalsIgnoreCase(nombreRol)) {
            return permisosAdmin();
        }

        Integer idRol = permisoDao.obtenerIdRolPorNombre(nombreRol);

        if (idRol == null) {
            return Map.of();
        }

        List<Permiso> lista = obtenerPorRol(idRol);

        return lista.stream()
                .filter(p -> p.getModulo() != null)
                .collect(Collectors.toMap(
                        p -> p.getModulo().trim().toLowerCase(),
                        Permiso::isPuedeAcceder,
                        (a, b) -> b
                ));
    }

    public boolean tienePermiso(Authentication auth, String modulo) {

        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        if (modulo == null || modulo.isBlank()) {
            return false;
        }

        Map<String, Boolean> permisos = getPermisos(auth);

        return Boolean.TRUE.equals(permisos.get(modulo.trim().toLowerCase()));
    }

    private String obtenerNombreRol(Authentication auth) {

        for (GrantedAuthority authority : auth.getAuthorities()) {

            if (authority == null || authority.getAuthority() == null) {
                continue;
            }

            String nombreRol = authority.getAuthority().trim();

            if ("ROLE_CLIENTE".equalsIgnoreCase(nombreRol)) {
                return "CLIENTE";
            }

            if (nombreRol.startsWith("ROLE_")) {
                nombreRol = nombreRol.replace("ROLE_", "");
            }

            return nombreRol.trim();
        }

        return null;
    }

    private Map<String, Boolean> permisosAdmin() {

        Map<String, Boolean> permisos = new LinkedHashMap<>();

        for (String modulo : MODULOS_BASE) {
            permisos.put(modulo, true);
        }

        return permisos;
    }
}
