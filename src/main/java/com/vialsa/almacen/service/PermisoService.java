package com.vialsa.almacen.service;

import com.vialsa.almacen.dao.Jdbc.JdbcPermisoDao;
import com.vialsa.almacen.model.Permiso;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PermisoService {

    private final JdbcPermisoDao permisoDao;

    public PermisoService(JdbcPermisoDao permisoDao) {
        this.permisoDao = permisoDao;
    }

    // 🔹 Permisos por rol (lo que ya tenías)
    public List<Permiso> obtenerPorRol(int idRol) {
        return permisoDao.obtenerPorRol(idRol);
    }

    public void guardarPermisos(int idRol, List<Permiso> permisos) {
        permisoDao.guardarPermisos(idRol, permisos);
    }

    // 🔹 NUEVO: mapa de permisos para el usuario logueado (para el header)
    public Map<String, Boolean> getPermisos(Authentication auth) {

        if (auth == null || !auth.isAuthenticated()) {
            return Map.of();
        }

        // Ej: ROLE_ADMIN -> ADMIN
        String authority = auth.getAuthorities().iterator().next().getAuthority();
        String nombreRol = authority.replace("ROLE_", "").trim();

        // Buscar idRol en BD
        Integer idRol = permisoDao.obtenerIdRolPorNombre(nombreRol);
        if (idRol == null) {
            return Map.of();
        }

        // Lista de permisos del rol
        List<Permiso> lista = obtenerPorRol(idRol);

        // Convertir a mapa: modulo -> puedeAcceder
        return lista.stream()
                .collect(Collectors.toMap(
                        Permiso::getModulo,
                        Permiso::isPuedeAcceder,
                        (a, b) -> b // por si se repite módulo
                ));
    }
}
