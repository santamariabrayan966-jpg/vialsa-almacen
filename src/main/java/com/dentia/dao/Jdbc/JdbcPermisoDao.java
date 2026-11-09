package com.dentia.dao.Jdbc;

import com.dentia.model.interno.Permiso;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class JdbcPermisoDao {

    private final JdbcTemplate jdbc;

    public JdbcPermisoDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // 🔹 Obtener permisos por idRol
    public List<Permiso> obtenerPorRol(int idRol) {

        String sql = """
                SELECT idPermiso, idRol, modulo, puedeAcceder
                FROM permisos
                WHERE idRol = ?
                ORDER BY modulo
                """;

        return jdbc.query(sql, new BeanPropertyRowMapper<>(Permiso.class), idRol);
    }

    // 🔹 Guardar permisos de un rol sin borrar toda la configuración
    @Transactional
    public void guardarPermisos(int idRol, List<Permiso> permisos) {

        if (permisos == null || permisos.isEmpty()) {
            return;
        }

        String sqlExiste = """
                SELECT COUNT(*)
                FROM permisos
                WHERE idRol = ?
                  AND LOWER(TRIM(modulo)) = LOWER(TRIM(?))
                """;

        String sqlUpdate = """
                UPDATE permisos
                SET puedeAcceder = ?
                WHERE idRol = ?
                  AND LOWER(TRIM(modulo)) = LOWER(TRIM(?))
                """;

        String sqlInsert = """
                INSERT INTO permisos (idRol, modulo, puedeAcceder)
                VALUES (?, ?, ?)
                """;

        for (Permiso permiso : permisos) {

            if (permiso == null || permiso.getModulo() == null) {
                continue;
            }

            String moduloNormalizado = permiso.getModulo().trim().toLowerCase();

            if (moduloNormalizado.isBlank()) {
                continue;
            }

            int puedeAcceder = permiso.isPuedeAcceder() ? 1 : 0;

            Integer existe = jdbc.queryForObject(
                    sqlExiste,
                    Integer.class,
                    idRol,
                    moduloNormalizado
            );

            if (existe != null && existe > 0) {

                jdbc.update(
                        sqlUpdate,
                        puedeAcceder,
                        idRol,
                        moduloNormalizado
                );

            } else {

                jdbc.update(
                        sqlInsert,
                        idRol,
                        moduloNormalizado,
                        puedeAcceder
                );
            }
        }
    }

    // 🔹 Obtener idRol a partir del nombre del rol
    public Integer obtenerIdRolPorNombre(String nombreRol) {

        if (nombreRol == null || nombreRol.isBlank()) {
            return null;
        }

        String limpio = nombreRol.trim();

        String sql = """
                SELECT idRol
                FROM roles
                WHERE UPPER(TRIM(nombreRol)) = UPPER(TRIM(?))
                LIMIT 1
                """;

        try {
            return jdbc.queryForObject(sql, Integer.class, limpio);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}