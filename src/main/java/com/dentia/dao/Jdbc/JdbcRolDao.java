package com.dentia.dao.Jdbc;

import com.dentia.model.interno.Rol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import com.dentia.dao.interfaces.RolDao;

import java.sql.PreparedStatement;
import java.sql.Statement;

import java.util.List;

@Repository
public class JdbcRolDao implements RolDao {

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
                    descripcion,
                    activo,
                    (SELECT COUNT(*) FROM usuarios u WHERE u.idRol=roles.idRol AND u.idEstadoUsuario<>3) cantidadUsuarios
                FROM roles
                WHERE tipo_rol='INTERNO'
                ORDER BY CASE WHEN UPPER(NombreRol)='ADMIN' THEN 0 ELSE 1 END, NombreRol
                """;
        return jdbc.query(sql, new BeanPropertyRowMapper<>(Rol.class));
    }

    // 🔍 Obtener rol por ID (aunque esté inactivo)
    public Rol obtenerPorId(int idRol) {
        String sql = """
                SELECT
                    idRol,
                    NombreRol AS nombreRol,
                    descripcion,
                    activo
                FROM roles
                WHERE idRol = ?
                """;
        return jdbc.queryForObject(sql, new BeanPropertyRowMapper<>(Rol.class), idRol);
    }

    // ➕ Crear rol (por defecto activo = 1)
    @Override
    public int crearRol(Rol rol) {
        String sql = "INSERT INTO roles (NombreRol, descripcion, tipo_rol, activo) VALUES (?, ?, 'INTERNO', 1)";
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, rol.getNombreRol());
            ps.setString(2, rol.getDescripcion());
            return ps;
        }, keys);
        Number id = keys.getKey();
        if (id == null) throw new IllegalStateException("MySQL no devolvio el idRol generado.");
        return id.intValue();
    }

    // ✏️ Actualizar nombre del rol
    public void actualizarRol(Rol rol) {
        String sql = "UPDATE roles SET NombreRol = ?, descripcion = ? WHERE idRol = ?";
        jdbc.update(sql, rol.getNombreRol().trim(), rol.getDescripcion(), rol.getIdRol());
    }

    // 🗑️ Eliminar FÍSICAMENTE el rol
    public void eliminarRol(int idRol) {
        cambiarEstadoActivo(idRol, false);
    }

    public int contarUsuariosAsociados(int idRol) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE idRol=? AND idEstadoUsuario<>3", Integer.class, idRol);
        return total == null ? 0 : total;
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

    // 📋 Listar solo roles internos activos
    @Override
    public List<Rol> listarRolesInternos() {

        String sql = """
        SELECT
            idRol,
            NombreRol AS nombreRol,
            descripcion,
            activo
        FROM roles
        WHERE tipo_rol = 'INTERNO'
        AND activo = 1
        """;

        return jdbc.query(sql, new BeanPropertyRowMapper<>(Rol.class));
    }

}
