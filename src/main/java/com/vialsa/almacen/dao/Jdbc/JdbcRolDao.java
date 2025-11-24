package com.vialsa.almacen.dao.Jdbc;

import com.vialsa.almacen.model.Rol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcRolDao {

    private final JdbcTemplate jdbc;

    @Autowired
    public JdbcRolDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // 📋 Listar TODOS los roles (activos e inactivos)
    public List<Rol> listarRoles() {
        String sql = """
                SELECT
                    idRol,
                    NombreRol AS nombreRol,
                    activo
                FROM roles
                """;
        return jdbc.query(sql, new BeanPropertyRowMapper<>(Rol.class));
    }

    // 🔍 Obtener rol por ID (aunque esté inactivo)
    public Rol obtenerPorId(int idRol) {
        String sql = """
                SELECT
                    idRol,
                    NombreRol AS nombreRol,
                    activo
                FROM roles
                WHERE idRol = ?
                """;
        return jdbc.queryForObject(sql, new BeanPropertyRowMapper<>(Rol.class), idRol);
    }

    // ➕ Crear rol (por defecto activo = 1)
    public void crearRol(Rol rol) {
        String sql = "INSERT INTO roles (NombreRol, activo) VALUES (?, 1)";
        jdbc.update(sql, rol.getNombreRol());
    }

    // ✏️ Actualizar nombre del rol
    public void actualizarRol(Rol rol) {
        String sql = "UPDATE roles SET NombreRol = ? WHERE idRol = ?";
        jdbc.update(sql, rol.getNombreRol(), rol.getIdRol());
    }

    // 🗑️ Eliminar FÍSICAMENTE el rol
    public void eliminarRol(int idRol) {
        String sql = "DELETE FROM roles WHERE idRol = ?";
        jdbc.update(sql, idRol);
    }

    // 🔁 Cambiar estado activo / inactivo (soft toggle)
    public void cambiarEstadoActivo(int idRol, boolean activo) {
        String sql = "UPDATE roles SET activo = ? WHERE idRol = ?";
        jdbc.update(sql, activo ? 1 : 0, idRol);
    }

    // Saber si un rol está activo
    public Boolean esRolActivo(int idRol) {
        String sql = "SELECT activo FROM roles WHERE idRol = ?";
        try {
            // MySQL -> 0/1; Spring lo convierte a Boolean
            return jdbc.queryForObject(sql, Boolean.class, idRol);
        } catch (EmptyResultDataAccessException e) {
            // Si no existe, lo tratamos como "no activo"
            return false;
        }
    }
    public String obtenerNombreRol(int idRol) {
        String sql = "SELECT NombreRol FROM roles WHERE idRol = ?";
        try {
            return jdbc.queryForObject(sql, String.class, idRol);
        } catch (Exception e) {
            return null;
        }
    }

}
