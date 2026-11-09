package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.UsuarioDao;
import com.dentia.model.interno.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
            u.direccion,
            u.Contrasena,
            u.idRol,
            u.idSedePrincipal,
            s.nombre AS nombreSedePrincipal,
            u.puedeCambiarSede,
            u.idEstadoUsuario,
            eu.NombreEstado AS nombreEstado,
            u.foto
        FROM usuarios u
        INNER JOIN estadousuario eu ON u.idEstadoUsuario = eu.idEstadoUsuario
        LEFT JOIN sedes s ON u.idSedePrincipal = s.idSede
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
            u.direccion,
            u.Contrasena,
            u.idRol,
            u.idSedePrincipal,
            s.nombre AS nombreSedePrincipal,
            u.puedeCambiarSede,
            u.idEstadoUsuario,
            eu.NombreEstado AS nombreEstado,
            u.foto
        FROM usuarios u
        INNER JOIN estadousuario eu ON u.idEstadoUsuario = eu.idEstadoUsuario
        LEFT JOIN sedes s ON u.idSedePrincipal = s.idSede
        WHERE u.Correo = ?
        """;

        return jdbc.query(sql, new BeanPropertyRowMapper<>(Usuario.class), correo)
                .stream().findFirst();
    }

    @Override
    public Optional<Usuario> findByCorreoRecuperacion(String correo) {

        String sql = """
        SELECT
            u.idUsuario,
            u.NombreUsuario,
            u.Nombres,
            u.Apellidos,
            u.NroDocumento,
            u.Correo,
            u.correo_recuperacion AS correoRecuperacion,
            u.Telefono,
            u.direccion,
            u.Contrasena,
            u.idRol,
            u.idSedePrincipal,
            s.nombre AS nombreSedePrincipal,
            u.puedeCambiarSede,
            u.idEstadoUsuario,
            eu.NombreEstado AS nombreEstado,
            u.foto
        FROM usuarios u
        INNER JOIN estadousuario eu ON u.idEstadoUsuario = eu.idEstadoUsuario
        LEFT JOIN sedes s ON u.idSedePrincipal = s.idSede
        WHERE u.correo_recuperacion = ?
        """;

        return jdbc.query(
                sql,
                new BeanPropertyRowMapper<>(Usuario.class),
                correo
        ).stream().findFirst();
    }

    // ============================================================
    // CREAR ADMIN SI NO EXISTE
    // ============================================================
    @Override
    @Transactional
    public boolean ensureAdminUserExists(String username, String passwordHash) {
        jdbc.update("""
                INSERT INTO roles (idRol, NombreRol, descripcion, tipo_rol, activo)
                VALUES (1, 'ADMIN', 'Administrador de DentIA', 'INTERNO', 1)
                ON DUPLICATE KEY UPDATE activo = 1, tipo_rol = 'INTERNO'
                """);

        jdbc.update("""
                INSERT INTO estadousuario (idEstadoUsuario, NombreEstado)
                VALUES (1, 'ACTIVO')
                ON DUPLICATE KEY UPDATE NombreEstado = VALUES(NombreEstado)
                """);

        jdbc.update("""
                INSERT INTO sedes
                    (idSede, nombre, codigo, tipo, descripcion, activo, fechaRegistro)
                VALUES
                    (1, 'Consultorio DentIA', 'DENTIA-UNICA', 'CONSULTORIO',
                     'Registro tecnico unico para compatibilidad interna.', 1, NOW())
                ON DUPLICATE KEY UPDATE activo = 1
                """);

        Integer idRolAdmin = jdbc.queryForObject(
                "SELECT idRol FROM roles WHERE UPPER(NombreRol) = 'ADMIN' LIMIT 1",
                Integer.class
        );

        List<String> permisosDentia = List.of(
                "dashboard", "pacientes", "citas", "historia_clinica", "odontograma",
                "diagnosticos", "tratamientos", "plan_tratamiento", "presupuestos",
                "atenciones", "pagos", "seguimiento", "documentos", "profesionales",
                "reportes", "usuarios", "roles", "configuracion"
        );

        for (String modulo : permisosDentia) {
            jdbc.update("""
                    INSERT INTO permisos (idRol, modulo, puedeAcceder)
                    VALUES (?, ?, 1)
                    ON DUPLICATE KEY UPDATE puedeAcceder = 1
                    """, idRolAdmin, modulo);
        }

        Integer existentes = jdbc.queryForObject(
                "SELECT COUNT(*) FROM usuarios WHERE NombreUsuario = ? OR idRol = ?",
                Integer.class,
                username,
                idRolAdmin
        );
        if (existentes != null && existentes > 0) {
            return false;
        }

        jdbc.update("""
                INSERT INTO usuarios (
                    NombreUsuario, Nombres, Apellidos, Contrasena,
                    idRol, idSedePrincipal, puedeCambiarSede,
                    idEstadoUsuario, activo, FechaCreacion
                ) VALUES (?, 'Administrador', 'DentIA', ?, ?, 1, 0, 1, 1, NOW())
                """, username, passwordHash, idRolAdmin);
        return true;
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
            u.correo_recuperacion AS correoRecuperacion,
            u.Telefono,
            u.direccion,
            u.idRol,
            r.NombreRol,
            u.idSedePrincipal,
            s.nombre AS nombreSedePrincipal,
            u.puedeCambiarSede,
            u.idEstadoUsuario,
            eu.NombreEstado AS nombreEstado,
            u.foto
        FROM usuarios u
        INNER JOIN estadousuario eu ON u.idEstadoUsuario = eu.idEstadoUsuario
        INNER JOIN roles r ON u.idRol = r.idRol
        LEFT JOIN sedes s ON u.idSedePrincipal = s.idSede
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
            u.correo_recuperacion AS correoRecuperacion,
            u.Telefono,
            u.direccion,
            u.Contrasena,
            u.idRol,
            r.NombreRol,
            u.idSedePrincipal,
            s.nombre AS nombreSedePrincipal,
            u.puedeCambiarSede,
            u.idEstadoUsuario,
            eu.NombreEstado AS nombreEstado,
            u.foto
        FROM usuarios u
        INNER JOIN roles r ON u.idRol = r.idRol
        INNER JOIN estadousuario eu ON u.idEstadoUsuario = eu.idEstadoUsuario
        LEFT JOIN sedes s ON u.idSedePrincipal = s.idSede
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
            correo_recuperacion,
            Telefono,
            direccion,
            Contrasena,
            FechaCreacion,
            idTipoDocumento,
            idRol,
            idSedePrincipal,
            puedeCambiarSede,
            idEstadoUsuario,
            foto
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?, ?, ?, ?, 1, ?)
        """;

        int puedeCambiarSede = 0;

        jdbc.update(sql,
                usuario.getNombreUsuario(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getNroDocumento(),
                usuario.getCorreo(),
                usuario.getCorreoRecuperacion(),
                usuario.getTelefono(),
                usuario.getDireccion(),
                usuario.getContrasena(),
                usuario.getIdTipoDocumento(),
                usuario.getIdRol(),
                usuario.getIdSedePrincipal(),
                puedeCambiarSede,
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
            correo_recuperacion = ?,
            Telefono      = ?,
            direccion    = ?,
            Contrasena    = ?,
            idRol         = ?,
            idSedePrincipal = ?,
            puedeCambiarSede = ?,
            foto          = ?
        WHERE idUsuario  = ?
        """;

        int puedeCambiarSede = 0;

        jdbc.update(sql,
                usuario.getNombreUsuario(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getNroDocumento(),
                usuario.getCorreo(),
                usuario.getCorreoRecuperacion(),
                usuario.getTelefono(),
                usuario.getDireccion(),
                usuario.getContrasena(),
                usuario.getIdRol(),
                usuario.getIdSedePrincipal(),
                puedeCambiarSede,
                usuario.getFoto(),
                usuario.getIdUsuario()
        );
    }

    // ============================================================
    // ACTUALIZAR ROL
    // ============================================================
    @Override
    public void actualizarRol(int idUsuario, int idRol) {
        int puedeCambiarSede = 0;

        jdbc.update("""
        UPDATE usuarios 
        SET idRol = ?,
            puedeCambiarSede = ?
        WHERE idUsuario = ?
        """, idRol, puedeCambiarSede, idUsuario);
    }

    @Override
    public void actualizarContrasena(int idUsuario, String passwordHash) {
        jdbc.update("UPDATE usuarios SET Contrasena=?, intentos_fallidos=0, bloqueado_hasta=NULL WHERE idUsuario=?",
                passwordHash, idUsuario);
    }

    // ============================================================
    // CREAR USUARIO RÁPIDO
    // ============================================================
    @Override
    public void crearUsuario(String nombreUsuario, String passwordHasheada, int idRol) {
        Integer idSedeDefault = jdbc.queryForObject(
                "SELECT idSede FROM sedes WHERE activo = 1 ORDER BY idSede LIMIT 1",
                Integer.class
        );

        int puedeCambiarSede = 0;

        String sql = """
        INSERT INTO usuarios (
            NombreUsuario,
            Contrasena,
            idRol,
            idSedePrincipal,
            puedeCambiarSede,
            idEstadoUsuario
        ) VALUES (?, ?, ?, ?, ?, 1)
        """;

        jdbc.update(sql, nombreUsuario, passwordHasheada, idRol, idSedeDefault, puedeCambiarSede);
    }


    // ============================================================
// ELIMINAR LÓGICO
// ============================================================
    @Override
    public void eliminarLogico(int idUsuario) {
        jdbc.update("""
        UPDATE usuarios 
        SET idEstadoUsuario = 3
        WHERE idUsuario = ?
    """, idUsuario);
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
            u.direccion,
            u.Contrasena,
            u.idRol,
            r.NombreRol,
            u.idSedePrincipal,
            s.nombre AS nombreSedePrincipal,
            u.puedeCambiarSede,
            u.idEstadoUsuario,
            eu.NombreEstado AS nombreEstado,
            u.foto
        FROM usuarios u
        INNER JOIN roles r ON u.idRol = r.idRol
        INNER JOIN estadousuario eu ON u.idEstadoUsuario = eu.idEstadoUsuario
        LEFT JOIN sedes s ON u.idSedePrincipal = s.idSede
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
                    correo_recuperacion = ?,
                    Telefono      = ?,
                    direccion    = ?,
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
                    usuario.getCorreoRecuperacion(),
                    usuario.getTelefono(),
                    usuario.getDireccion(),
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
                    correo_recuperacion = ?,
                    Telefono      = ?,
                    direccion    = ?,
                    foto          = ?
                WHERE idUsuario  = ?
                """;

            jdbc.update(sql,
                    usuario.getNombreUsuario(),
                    usuario.getNombres(),
                    usuario.getApellidos(),
                    usuario.getNroDocumento(),
                    usuario.getCorreo(),
                    usuario.getCorreoRecuperacion(),
                    usuario.getTelefono(),
                    usuario.getDireccion(),
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
                    foto,
                    FechaCreacion
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, NOW())
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
    public void cambiarEstadoUsuario(int idUsuario, int idEstadoUsuario) {

        jdbc.update("""
        UPDATE usuarios
        SET idEstadoUsuario = ?
        WHERE idUsuario = ?
    """, idEstadoUsuario, idUsuario);
    }

    @Override
    public void cambiarEstadoUsuarioPorRol(int idRol, int idEstadoUsuario) {
        jdbc.update("""
        UPDATE usuarios
        SET idEstadoUsuario = ?
        WHERE idRol = ?
          AND idRol != 1
    """, idEstadoUsuario, idRol);
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
            u.correo_recuperacion AS correoRecuperacion,
            u.Telefono,
            u.direccion,
            u.idRol,
            r.NombreRol,
            u.idSedePrincipal,
            s.nombre AS nombreSedePrincipal,
            u.puedeCambiarSede,
            u.idEstadoUsuario,
            eu.NombreEstado AS nombreEstado,
            u.foto
        FROM usuarios u
        INNER JOIN roles r ON u.idRol = r.idRol
        INNER JOIN estadousuario eu ON u.idEstadoUsuario = eu.idEstadoUsuario
        LEFT JOIN sedes s ON u.idSedePrincipal = s.idSede
        WHERE u.idRol != 8
          AND u.idEstadoUsuario <> 3
        """;

        return jdbc.query(sql, new BeanPropertyRowMapper<>(Usuario.class));
    }

    // ============================================================
// SEGURIDAD LOGIN
// ============================================================

    @Override
    public void incrementarIntentosFallidos(String username) {
        String sql = """
        UPDATE usuarios
        SET intentos_fallidos = COALESCE(intentos_fallidos, 0) + 1
        WHERE NombreUsuario = ? OR Correo = ?
        """;

        jdbc.update(sql, username, username);
    }

    @Override
    public void resetearIntentosFallidos(String username) {
        String sql = """
        UPDATE usuarios
        SET intentos_fallidos = 0,
            bloqueado_hasta = NULL
        WHERE NombreUsuario = ? OR Correo = ?
        """;

        jdbc.update(sql, username, username);
    }

    @Override
    public int obtenerIntentosFallidos(String username) {
        String sql = """
        SELECT COALESCE(intentos_fallidos, 0)
        FROM usuarios
        WHERE NombreUsuario = ? OR Correo = ?
        LIMIT 1
        """;

        return jdbc.query(sql, rs -> rs.next() ? rs.getInt(1) : 0, username, username);
    }

    @Override
    public void bloquearTemporalmente(String username) {
        String sql = """
        UPDATE usuarios
        SET bloqueado_hasta = DATE_ADD(NOW(), INTERVAL 5 MINUTE)
        WHERE NombreUsuario = ? OR Correo = ?
        """;

        jdbc.update(sql, username, username);
    }

    @Override
    public boolean estaBloqueado(String username) {
        String sql = """
        SELECT COUNT(*)
        FROM usuarios
        WHERE (NombreUsuario = ? OR Correo = ?)
          AND bloqueado_hasta IS NOT NULL
          AND bloqueado_hasta > NOW()
        """;

        Integer count = jdbc.queryForObject(sql, Integer.class, username, username);
        return count != null && count > 0;
    }
}
