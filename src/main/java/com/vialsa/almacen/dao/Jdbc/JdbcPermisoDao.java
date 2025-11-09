package com.vialsa.almacen.dao.Jdbc;

import com.vialsa.almacen.model.Permiso;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcPermisoDao {

    private final JdbcTemplate jdbc;

    public JdbcPermisoDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // 🔹 Obtener permisos por idRol
    public List<Permiso> obtenerPorRol(int idRol) {
        String sql = "SELECT idPermiso, idRol, modulo, puedeAcceder " +
                "FROM permisos WHERE idRol = ?";
        return jdbc.query(sql, new BeanPropertyRowMapper<>(Permiso.class), idRol);
    }

    // 🔹 Guardar (reemplazar) todos los permisos de un rol
    public void guardarPermisos(int idRol, List<Permiso> permisos) {
        // Borramos permisos anteriores
        jdbc.update("DELETE FROM permisos WHERE idRol = ?", idRol);

        String sql = "INSERT INTO permisos (idRol, modulo, puedeAcceder) VALUES (?, ?, ?)";

        for (Permiso p : permisos) {
            // Normalizamos nombre del módulo para evitar problemas de comparación
            String moduloNormalizado = p.getModulo() == null
                    ? ""
                    : p.getModulo().trim().toLowerCase(); // ← importante

            jdbc.update(sql, idRol, moduloNormalizado, p.isPuedeAcceder());
        }
    }

    // 🔹 Obtener idRol a partir del nombre (authority) del rol
    public Integer obtenerIdRolPorNombre(String nombreRol) {
        if (nombreRol == null) {
            return null;
        }

        // Normalizamos: quitamos espacios y usamos comparación case-insensitive
        String limpio = nombreRol.trim();

        String sql = "SELECT idRol " +
                "FROM roles " +
                "WHERE UPPER(TRIM(NombreRol)) = UPPER(TRIM(?)) " +
                "LIMIT 1";

        try {
            return jdbc.queryForObject(sql, Integer.class, limpio);
        } catch (EmptyResultDataAccessException e) {
            // No se encontró el rol (esto devolverá false en tieneAcceso)
            return null;
        }
    }
}
