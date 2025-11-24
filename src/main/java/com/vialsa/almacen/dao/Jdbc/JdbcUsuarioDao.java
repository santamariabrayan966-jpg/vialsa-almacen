package com.vialsa.almacen.dao.Jdbc;

import com.vialsa.almacen.dao.interfaces.UsuarioDao;
import com.vialsa.almacen.model.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JdbcUsuarioDao implements UsuarioDao {

    private final JdbcTemplate jdbc;

    @Autowired
    public JdbcUsuarioDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ============================================================
    // BUSCAR POR USERNAME
    // ============================================================
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

        return jdbc.query(sql, new BeanPropertyRowMapper<>(Usuario.class), username)
                .stream().findFirst();
    }

    // ============================================================
    // BUSCAR POR CORREO
    // ============================================================
    @Override
    public Optional<Usuario> findByCorreo(String correo) {
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
            WHERE u.Correo = ?
            """;

        return jdbc.query(sql, new BeanPropertyRowMapper<>(Usuario.class), correo)
                .stream().findFirst();
    }

    // ============================================================
    // CREAR ADMIN SI NO EXISTE
    // ============================================================
    @Override
    public void ensureAdminUserExists(String username, String rawPasswordHasheada) {

        Integer c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM usuarios WHERE NombreUsuario=?",
                Integer.class,
                username
        );

        if (c != null && c == 0) {

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
                    NombreUsuario,
                    Contrasena,
                    idRol,
                    idEstadoUsuario,
                    activo
                ) VALUES (?, ?, 1, 1, 1)
                """,
                    username,
                    rawPasswordHasheada
            );
        }
    }

    // ============================================================
    // LISTAR TODOS
    // ============================================================
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

    // ============================================================
    // OBTENER POR ID
    // ============================================================
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

        return jdbc.queryForObject(sql, new BeanPropertyRowMapper<>(Usuario.class), idUsuario);
    }

    // ============================================================
    // CREAR USUARIO — CORREGIDO TOTALMENTE
    // ============================================================
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
        FechaCreacion,
        idTipoDocumento,
        idRol,
        idEstadoUsuario,
        activo,
        foto
    ) VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), ?, ?, ?, ?, ?)
    """;

        jdbc.update(sql,
                usuario.getNombreUsuario(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getNroDocumento(),
                usuario.getCorreo(),
                usuario.getTelefono(),
                usuario.getContrasena(),
                usuario.getIdTipoDocumento(),  // ⭐ NECESARIO
                usuario.getIdRol(),
                1, // estado activo
                usuario.getActivo() != null && usuario.getActivo() ? 1 : 0,
                usuario.getFoto()
        );
    }

    // ============================================================
    // ACTUALIZAR
    // ============================================================
    @Override
    public void actualizar(Usuario usuario) {
        String sql = """
            UPDATE usuarios SET
                NombreUsuario = ?,
                Nombres       = ?,
                Apellidos     = ?,
                NroDocumento  = ?,
                Correo        = ?,
                Telefono      = ?,
                Contrasena    = ?,
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
                usuario.getContrasena(),
                usuario.getIdRol(),
                usuario.getActivo() != null && usuario.getActivo() ? 1 : 0,
                usuario.getFoto(),
                usuario.getIdUsuario()
        );
    }

    // ============================================================
    // ACTUALIZAR ROL
    // ============================================================
    @Override
    public void actualizarRol(int idUsuario, int idRol) {
        jdbc.update("UPDATE usuarios SET idRol=? WHERE idUsuario=?", idRol, idUsuario);
    }

    // ============================================================
    // CREAR USUARIO RÁPIDO
    // ============================================================
    @Override
    public void crearUsuario(String nombreUsuario, String passwordHasheada, int idRol) {
        String sql = """
            INSERT INTO usuarios (
                NombreUsuario,
                Contrasena,
                idRol,
                idEstadoUsuario,
                activo
            ) VALUES (?, ?, ?, 1, 1)
            """;

        jdbc.update(sql, nombreUsuario, passwordHasheada, idRol);
    }

    // ============================================================
    // ELIMINAR
    // ============================================================
    @Override
    public void eliminarUsuario(int idUsuario) {
        jdbc.update("DELETE FROM usuarios WHERE idUsuario=?", idUsuario);
    }

    // ============================================================
    // OBTENER POR NOMBRE
    // ============================================================
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

        return jdbc.query(sql, new BeanPropertyRowMapper<>(Usuario.class), nombreUsuario)
                .stream().findFirst();
    }

    // ============================================================
    // ACTUALIZAR PERFIL
    // ============================================================
    @Override
    public void actualizarPerfil(Usuario usuario) {

        if (usuario.getContrasena() != null && !usuario.getContrasena().isBlank()) {

            String sql = """
                UPDATE usuarios SET
                    NombreUsuario = ?,
                    Nombres       = ?,
                    Apellidos     = ?,
                    NroDocumento  = ?,
                    Correo        = ?,
                    Telefono      = ?,
                    Contrasena    = ?,
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
                    usuario.getContrasena(),
                    usuario.getFoto(),
                    usuario.getIdUsuario()
            );

        } else {

            String sql = """
                UPDATE usuarios SET
                    NombreUsuario = ?,
                    Nombres       = ?,
                    Apellidos     = ?,
                    NroDocumento  = ?,
                    Correo        = ?,
                    Telefono      = ?,
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
                    usuario.getFoto(),
                    usuario.getIdUsuario()
            );
        }
    }

    // ============================================================
    // CLIENTE GOOGLE
    // ============================================================
    @Override
    public void crearClienteDesdeGoogle(Usuario usuario) {

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
            idTipoDocumento,
            idEstadoUsuario,
            activo,
            foto,
            FechaCreacion
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, ?, NOW())
        """;

        jdbc.update(sql,
                usuario.getNombreUsuario(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getNroDocumento(),
                usuario.getCorreo(),
                usuario.getTelefono(),
                usuario.getContrasena(),
                usuario.getIdRol(),
                1,                // idTipoDocumento = 1 (DNI)
                usuario.getFoto()
        );
    }


    // ============================================================
    // ACTIVAR / DESACTIVAR
    // ============================================================
    @Override
    public void cambiarEstadoActivo(int idUsuario, boolean activo) {
        jdbc.update("UPDATE usuarios SET activo=? WHERE idUsuario=?", activo ? 1 : 0, idUsuario);
    }

    @Override
    public void cambiarEstadoActivoPorRol(int idRol, boolean activo) {
        jdbc.update("UPDATE usuarios SET activo=? WHERE idRol=?", activo ? 1 : 0, idRol);
    }

    // ============================================================
    // VALIDACIONES
    // ============================================================
    @Override
    public boolean existeCorreo(String correo) {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE Correo=?", Integer.class, correo);
        return c != null && c > 0;
    }

    @Override
    public boolean existeDocumento(String documento) {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE NroDocumento=?", Integer.class, documento);
        return c != null && c > 0;
    }

    // ============================================================
    // LISTAR SOLO USUARIOS INTERNOS
    // ============================================================
    @Override
    public List<Usuario> listarUsuariosInternos() {
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
            WHERE u.idRol != 8
            """;

        return jdbc.query(sql, new BeanPropertyRowMapper<>(Usuario.class));
    }
}
