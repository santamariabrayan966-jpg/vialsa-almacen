package com.vialsa.almacen.dao.Jdbc;

import com.vialsa.almacen.dao.interfaces.UsuarioDao;
import com.vialsa.almacen.model.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JdbcUsuarioDao implements UsuarioDao {

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;

    @Autowired
    public JdbcUsuarioDao(JdbcTemplate jdbc, PasswordEncoder encoder) {
        this.jdbc = jdbc;
        this.encoder = encoder;
    }

    // 🔹 Buscar usuario por nombre (para login)
    @Override
    public Optional<Usuario> findByNombreUsuario(String username) {
        List<Usuario> list = jdbc.query(
                "SELECT * FROM usuarios WHERE NombreUsuario = ?",
                new BeanPropertyRowMapper<>(Usuario.class),
                username
        );
        return list.stream().findFirst();
    }

    // 🔹 Crear el usuario admin por defecto si no existe
    @Override
    public void ensureAdminUserExists(String username, String rawPassword) {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE NombreUsuario=?", Integer.class, username);
        if (c != null && c == 0) {
            String hash = encoder.encode(rawPassword);
            jdbc.update("INSERT IGNORE INTO roles (idRol, NombreRol, descripcion) VALUES (1, 'ADMIN', 'Administrador')");
            jdbc.update("INSERT IGNORE INTO estadousuario (idEstadoUsuario, NombreEstado) VALUES (1, 'ACTIVO')");
            jdbc.update("INSERT INTO usuarios (NombreUsuario, Contrasena, idRol, idEstadoUsuario) VALUES (?, ?, 1, 1)",
                    username, hash);
        }
    }

    // 🔹 Listar todos los usuarios
    @Override
    public List<Usuario> listarTodos() {
        String sql = """
            SELECT u.idUsuario, u.NombreUsuario, r.NombreRol, u.idRol
            FROM usuarios u
            INNER JOIN roles r ON u.idRol = r.idRol
            """;
        return jdbc.query(sql, new BeanPropertyRowMapper<>(Usuario.class));
    }

    // 🔹 Obtener usuario por ID
    @Override
    public Usuario obtenerPorId(int idUsuario) {
        String sql = """
            SELECT u.idUsuario, u.NombreUsuario, r.NombreRol, u.idRol
            FROM usuarios u
            INNER JOIN roles r ON u.idRol = r.idRol
            WHERE u.idUsuario = ?
            """;
        return jdbc.queryForObject(sql, new BeanPropertyRowMapper<>(Usuario.class), idUsuario);
    }

    // 🔹 Actualizar rol de usuario
    @Override
    public void actualizarRol(int idUsuario, int idRol) {
        String sql = "UPDATE usuarios SET idRol = ? WHERE idUsuario = ?";
        jdbc.update(sql, idRol, idUsuario);
    }

    // 🔹 Crear nuevo usuario
    @Override
    public void crearUsuario(String nombreUsuario, String passwordPlano, int idRol) {
        String hashed = encoder.encode(passwordPlano);
        String sql = "INSERT INTO usuarios (NombreUsuario, Contrasena, idRol, idEstadoUsuario) VALUES (?, ?, ?, 1)";
        jdbc.update(sql, nombreUsuario, hashed, idRol);
    }

    // 🔹 Eliminar usuario
    @Override
    public void eliminarUsuario(int idUsuario) {
        String sql = "DELETE FROM usuarios WHERE idUsuario = ?";
        jdbc.update(sql, idUsuario);
    }
}
