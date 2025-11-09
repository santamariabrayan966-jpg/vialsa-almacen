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

    // 🔹 Buscar usuario por nombre (para LOGIN)
    @Override
    public Optional<Usuario> findByNombreUsuario(String username) {
        String sql = """
            SELECT
                u.idUsuario,
                u.NombreUsuario,
                u.Nombres,
                u.Apellidos,
                u.NroDocumento,
                u.Correo,
                u.Telefono,
                u.Contrasena,
                u.idRol,
                u.activo,
                u.foto
            FROM usuarios u
            WHERE u.NombreUsuario = ?
            """;

        List<Usuario> list = jdbc.query(
                sql,
                new BeanPropertyRowMapper<>(Usuario.class),
                username
        );
        return list.stream().findFirst();
    }

    // 🔹 Crear el usuario admin por defecto si no existe
    @Override
    public void ensureAdminUserExists(String username, String rawPassword) {
        Integer c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM usuarios WHERE NombreUsuario=?",
                Integer.class,
                username
        );
        if (c != null && c == 0) {
            String hash = encoder.encode(rawPassword);

            jdbc.update("""
                INSERT IGNORE INTO roles (idRol, NombreRol, descripcion, activo)
                VALUES (1, 'ADMIN', 'Administrador', 1)
                """);

            jdbc.update("""
                INSERT IGNORE INTO estadousuario (idEstadoUsuario, NombreEstado)
                VALUES (1, 'ACTIVO')
                """);

            jdbc.update("""
                INSERT INTO usuarios (
                    NombreUsuario, Contrasena, idRol, idEstadoUsuario, activo
                ) VALUES (?, ?, 1, 1, 1)
                """, username, hash);
        }
    }

    // 🔹 Listar todos los usuarios (para la tabla de administración)
    @Override
    public List<Usuario> listarTodos() {
        String sql = """
            SELECT
                u.idUsuario,
                u.NombreUsuario,
                u.Nombres,
                u.Apellidos,
                u.NroDocumento,
                u.Correo,
                u.Telefono,
                u.idRol,
                r.NombreRol,
                u.activo,
                u.foto
            FROM usuarios u
            INNER JOIN roles r ON u.idRol = r.idRol
            """;
        return jdbc.query(sql, new BeanPropertyRowMapper<>(Usuario.class));
    }

    // 🔹 Obtener usuario por ID
    @Override
    public Usuario obtenerPorId(int idUsuario) {
        String sql = """
            SELECT
                u.idUsuario,
                u.NombreUsuario,
                u.Nombres,
                u.Apellidos,
                u.NroDocumento,
                u.Correo,
                u.Telefono,
                u.Contrasena,
                u.idRol,
                r.NombreRol,
                u.activo,
                u.foto
            FROM usuarios u
            INNER JOIN roles r ON u.idRol = r.idRol
            WHERE u.idUsuario = ?
            """;
        return jdbc.queryForObject(
                sql,
                new BeanPropertyRowMapper<>(Usuario.class),
                idUsuario
        );
    }

    // 🔹 Crear nuevo usuario (desde formulario de "Nuevo usuario")
    @Override
    public void crear(Usuario usuario) {
        String sql = """
            INSERT INTO usuarios (
                NombreUsuario,
                Nombres,
                Apellidos,
                NroDocumento,
                Correo,
                Telefono,
                Contrasena,
                idRol,
                idEstadoUsuario,
                activo,
                foto
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)
            """;

        String hash = null;
        if (usuario.getContrasena() != null && !usuario.getContrasena().isEmpty()) {
            hash = encoder.encode(usuario.getContrasena());
        }

        jdbc.update(sql,
                usuario.getNombreUsuario(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getNroDocumento(),
                usuario.getCorreo(),
                usuario.getTelefono(),
                hash,
                usuario.getIdRol(),
                usuario.getActivo() != null && usuario.getActivo() ? 1 : 0,
                usuario.getFoto()
        );
    }

    // 🔹 Actualizar TODOS los datos del usuario (para el modal de edición)
    @Override
    public void actualizar(Usuario usuario) {
        String sql = """
            UPDATE usuarios
            SET
                NombreUsuario = ?,
                Nombres       = ?,
                Apellidos     = ?,
                NroDocumento  = ?,
                Correo        = ?,
                Telefono      = ?,
                idRol         = ?,
                activo        = ?,
                foto          = ?
            WHERE idUsuario  = ?
            """;

        jdbc.update(sql,
                usuario.getNombreUsuario(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getNroDocumento(),
                usuario.getCorreo(),
                usuario.getTelefono(),
                usuario.getIdRol(),
                usuario.getActivo() != null && usuario.getActivo() ? 1 : 0,
                usuario.getFoto(),
                usuario.getIdUsuario()
        );
    }

    // 🔹 Actualizar solo el rol (uso puntual)
    @Override
    public void actualizarRol(int idUsuario, int idRol) {
        String sql = "UPDATE usuarios SET idRol = ? WHERE idUsuario = ?";
        jdbc.update(sql, idRol, idUsuario);
    }

    // 🔹 Crear usuario rápido (bootstrap admin, etc.)
    @Override
    public void crearUsuario(String nombreUsuario, String passwordPlano, int idRol) {
        String hashed = encoder.encode(passwordPlano);
        String sql = """
            INSERT INTO usuarios (
                NombreUsuario,
                Contrasena,
                idRol,
                idEstadoUsuario,
                activo
            ) VALUES (?, ?, ?, 1, 1)
            """;
        jdbc.update(sql, nombreUsuario, hashed, idRol);
    }

    // 🔹 Eliminar usuario
    @Override
    public void eliminarUsuario(int idUsuario) {
        String sql = "DELETE FROM usuarios WHERE idUsuario = ?";
        jdbc.update(sql, idUsuario);
    }

    // 🔹 Obtener usuario por nombre (para PERFIL u otros usos)
    @Override
    public Optional<Usuario> obtenerPorNombre(String nombreUsuario) {
        String sql = """
            SELECT
                u.idUsuario,
                u.NombreUsuario,
                u.Nombres,
                u.Apellidos,
                u.NroDocumento,
                u.Correo,
                u.Telefono,
                u.Contrasena,
                u.idRol,
                r.NombreRol,
                u.activo,
                u.foto
            FROM usuarios u
            INNER JOIN roles r ON u.idRol = r.idRol
            WHERE u.NombreUsuario = ?
            """;

        List<Usuario> usuarios = jdbc.query(
                sql,
                new BeanPropertyRowMapper<>(Usuario.class),
                nombreUsuario
        );
        return usuarios.stream().findFirst();
    }

    // 🔹 Actualizar perfil (con o sin contraseña)
    @Override
    public void actualizarPerfil(Usuario usuario) {
        if (usuario.getContrasena() != null && !usuario.getContrasena().isEmpty()) {
            String hash = encoder.encode(usuario.getContrasena());
            String sql = """
                UPDATE usuarios
                SET NombreUsuario = ?,
                    Nombres        = ?,
                    Apellidos      = ?,
                    NroDocumento   = ?,
                    Correo         = ?,
                    Telefono       = ?,
                    Contrasena     = ?,
                    foto           = ?
                WHERE idUsuario  = ?
                """;
            jdbc.update(sql,
                    usuario.getNombreUsuario(),
                    usuario.getNombres(),
                    usuario.getApellidos(),
                    usuario.getNroDocumento(),
                    usuario.getCorreo(),
                    usuario.getTelefono(),
                    hash,
                    usuario.getFoto(),
                    usuario.getIdUsuario()
            );
        } else {
            String sql = """
                UPDATE usuarios
                SET NombreUsuario = ?,
                    Nombres        = ?,
                    Apellidos      = ?,
                    NroDocumento   = ?,
                    Correo         = ?,
                    Telefono       = ?,
                    foto           = ?
                WHERE idUsuario  = ?
                """;
            jdbc.update(sql,
                    usuario.getNombreUsuario(),
                    usuario.getNombres(),
                    usuario.getApellidos(),
                    usuario.getNroDocumento(),
                    usuario.getCorreo(),
                    usuario.getTelefono(),
                    usuario.getFoto(),
                    usuario.getIdUsuario()
            );
        }
    }

    // 🔹 Activar / Desactivar UN usuario
    @Override
    public void cambiarEstadoActivo(int idUsuario, boolean activo) {
        String sql = "UPDATE usuarios SET activo = ? WHERE idUsuario = ?";
        jdbc.update(sql, activo ? 1 : 0, idUsuario);
    }

    // 🔹 Activar / Desactivar TODOS los usuarios de un rol
    @Override
    public void cambiarEstadoActivoPorRol(int idRol, boolean activo) {
        String sql = "UPDATE usuarios SET activo = ? WHERE idRol = ?";
        jdbc.update(sql, activo ? 1 : 0, idRol);
    }
}
