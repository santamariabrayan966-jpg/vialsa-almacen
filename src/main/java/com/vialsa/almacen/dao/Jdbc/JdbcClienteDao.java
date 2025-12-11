package com.vialsa.almacen.dao.Jdbc;

import com.vialsa.almacen.dao.interfaces.IClienteDao;
import com.vialsa.almacen.model.Cliente;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class JdbcClienteDao implements IClienteDao {

    private final JdbcTemplate jdbc;

    public JdbcClienteDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ================================================
    // MAPEADOR ESTÁNDAR
    // ================================================
    private static class ClienteMapper implements RowMapper<Cliente> {
        @Override
        public Cliente mapRow(ResultSet rs, int rowNum) throws SQLException {
            Cliente c = new Cliente();
            c.setIdClientes(rs.getInt("idClientes"));
            c.setNombres(rs.getString("nombres"));
            c.setApellidos(rs.getString("apellidos"));
            c.setNro_documento(rs.getString("nro_documento"));
            c.setDireccion(rs.getString("direccion"));
            c.setTelefono(rs.getString("telefono"));
            c.setCorreo(rs.getString("correo"));
            c.setIdTipoDocumento(rs.getInt("idTipoDocumento"));

            // Nuevos campos PRO
            c.setVip(rs.getBoolean("vip"));
            c.setMoroso(rs.getBoolean("moroso"));
            c.setActivo(rs.getBoolean("activo"));
            c.setFoto(rs.getString("foto"));
            c.setFecha_registro(rs.getTimestamp("fecha_registro"));

            return c;
        }
    }

    // ============================================================
    // BÚSQUEDA POR DOCUMENTO
    // ============================================================
    @Override
    public Cliente buscarPorDocumento(String documento) {
        String sql = "SELECT * FROM clientes WHERE nro_documento = ?";
        try {
            return jdbc.queryForObject(sql, new ClienteMapper(), documento);
        } catch (Exception e) {
            return null;
        }
    }

    // ============================================================
    // BÚSQUEDA POR ID
    // ============================================================
    @Override
    public Cliente buscarPorId(Integer idCliente) {
        String sql = "SELECT * FROM clientes WHERE idClientes = ?";
        try {
            return jdbc.queryForObject(sql, new ClienteMapper(), idCliente);
        } catch (Exception e) {
            return null;
        }
    }

    // ============================================================
    // REGISTRAR
    // ============================================================
    @Override
    public int registrar(Cliente cliente) {
        String sql = """
                INSERT INTO clientes 
                (nombres, apellidos, nro_documento, direccion, telefono, correo, 
                 idTipoDocumento, idUsuario, fecha_registro, activo, foto)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), 1, ?)
                """;

        return jdbc.update(sql,
                cliente.getNombres(),
                cliente.getApellidos(),
                cliente.getNro_documento(),
                cliente.getDireccion(),
                cliente.getTelefono(),
                cliente.getCorreo(),
                cliente.getIdTipoDocumento(),
                cliente.getIdUsuario(),
                cliente.getFoto()
        );
    }

    // ============================================================
    // ACTUALIZAR
    // ============================================================
    @Override
    public int actualizar(Cliente cliente) {

        String sql = """
                UPDATE clientes 
                SET nombres = ?, apellidos = ?, nro_documento = ?, direccion = ?, 
                    telefono = ?, correo = ?, idTipoDocumento = ?, idUsuario = ?, foto = ?
                WHERE idClientes = ?
                """;

        return jdbc.update(sql,
                cliente.getNombres(),
                cliente.getApellidos(),
                cliente.getNro_documento(),
                cliente.getDireccion(),
                cliente.getTelefono(),
                cliente.getCorreo(),
                cliente.getIdTipoDocumento(),
                cliente.getIdUsuario(),
                cliente.getFoto(),
                cliente.getIdClientes()
        );
    }

    // ============================================================
    // ELIMINAR
    // ============================================================
    @Override
    public int eliminar(Integer idCliente) {
        return jdbc.update("DELETE FROM clientes WHERE idClientes = ?", idCliente);
    }

    // ============================================================
    // LISTAR
    // ============================================================
    @Override
    public List<Cliente> listarTodos() {
        String sql = "SELECT * FROM clientes ORDER BY idClientes DESC";
        return jdbc.query(sql, new ClienteMapper());
    }

    // ============================================================
    // BUSCAR POR CORREO
    // ============================================================
    @Override
    public Cliente buscarPorCorreo(String correo) {
        String sql = "SELECT * FROM clientes WHERE correo = ?";
        try {
            return jdbc.queryForObject(sql, new ClienteMapper(), correo);
        } catch (Exception e) {
            return null;
        }
    }

    // ============================================================
    // BÚSQUEDA AVANZADA
    // ============================================================
    @Override
    public List<Cliente> buscarClientes(String filtro) {

        String sql = """
                SELECT * FROM clientes
                WHERE nombres LIKE ? OR apellidos LIKE ? OR correo LIKE ?
                OR nro_documento LIKE ? OR telefono LIKE ?
                """;

        String like = "%" + filtro + "%";

        return jdbc.query(sql, new ClienteMapper(), like, like, like, like, like);
    }

    // ============================================================
    // VIP
    // ============================================================
    @Override
    public int marcarVip(Integer idCliente) {
        return jdbc.update("UPDATE clientes SET vip = 1 WHERE idClientes = ?", idCliente);
    }

    @Override
    public int quitarVip(Integer idCliente) {
        return jdbc.update("UPDATE clientes SET vip = 0 WHERE idClientes = ?", idCliente);
    }

    // ============================================================
    // MOROSO
    // ============================================================
    @Override
    public int marcarMoroso(Integer idCliente) {
        return jdbc.update("UPDATE clientes SET moroso = 1 WHERE idClientes = ?", idCliente);
    }

    @Override
    public int quitarMoroso(Integer idCliente) {
        return jdbc.update("UPDATE clientes SET moroso = 0 WHERE idClientes = ?", idCliente);
    }

    // ============================================================
    // ACTIVAR / DESACTIVAR
    // ============================================================
    @Override
    public int activarCliente(Integer idCliente) {
        return jdbc.update("UPDATE clientes SET activo = 1 WHERE idClientes = ?", idCliente);
    }

    @Override
    public int desactivarCliente(Integer idCliente) {
        return jdbc.update("UPDATE clientes SET activo = 0 WHERE idClientes = ?", idCliente);
    }

    // ============================================================
    // NOTAS
    // ============================================================
    @Override
    public int agregarNota(Integer idCliente, String nota) {
        String sql = """
                INSERT INTO notas_cliente(idCliente, nota, fecha)
                VALUES (?, ?, NOW())
                """;
        return jdbc.update(sql, idCliente, nota);
    }

    @Override
    public List<String> obtenerNotas(Integer idCliente) {
        return jdbc.queryForList(
                "SELECT nota FROM notas_cliente WHERE idCliente = ? ORDER BY fecha DESC",
                String.class, idCliente);
    }

    // ============================================================
    // HISTORIAL
    // ============================================================
    @Override
    public int registrarHistorial(Integer idCliente, String accion) {
        String sql = """
                INSERT INTO historial_cliente(idCliente, evento, fecha)
                VALUES (?, ?, NOW())
                """;
        return jdbc.update(sql, idCliente, accion);
    }

    @Override
    public List<String> obtenerHistorial(Integer idCliente) {
        return jdbc.queryForList(
                "SELECT evento FROM historial_cliente WHERE idCliente = ? ORDER BY fecha DESC",
                String.class, idCliente);
    }

    // ============================================================
    // PERFIL COMPLETO (JOIN PRO)
    // ============================================================
    @Override
    public Cliente obtenerPerfilCompleto(Integer idCliente) {

        String sql = """
                SELECT c.*, 
                       t.nombre AS tipoDocumento, 
                       u.username AS usuarioRegistro
                FROM clientes c
                LEFT JOIN tipodocumento t ON c.idTipoDocumento = t.idTipoDocumento
                LEFT JOIN usuarios u ON c.idUsuario = u.idUsuario
                WHERE c.idClientes = ?
                """;

        try {
            return jdbc.queryForObject(sql, (rs, rowNum) -> {

                Cliente c = new Cliente();

                // Datos básicos
                c.setIdClientes(rs.getInt("idClientes"));
                c.setNombres(rs.getString("nombres"));
                c.setApellidos(rs.getString("apellidos"));
                c.setNro_documento(rs.getString("nro_documento"));
                c.setDireccion(rs.getString("direccion"));
                c.setTelefono(rs.getString("telefono"));
                c.setCorreo(rs.getString("correo"));
                c.setIdTipoDocumento(rs.getInt("idTipoDocumento"));

                // Datos adicionales
                c.setTipoDocumentoNombre(rs.getString("tipoDocumento"));
                c.setUsuarioRegistro(rs.getString("usuarioRegistro"));
                c.setVip(rs.getBoolean("vip"));
                c.setMoroso(rs.getBoolean("moroso"));
                c.setActivo(rs.getBoolean("activo"));
                c.setFoto(rs.getString("foto"));
                c.setFecha_registro(rs.getTimestamp("fecha_registro"));

                return c;

            }, idCliente);

        } catch (Exception e) {
            return null;
        }
    }

    // ============================================================
    // IMPORTAR MASIVO
    // ============================================================
    @Override
    public int registrarMasivo(List<Cliente> clientes) {
        int count = 0;
        for (Cliente c : clientes) {
            count += registrar(c);
        }
        return count;
    }

    // ============================================================
    // EXPORTAR
    // ============================================================
    @Override
    public List<Cliente> listarParaExportar() {
        return listarTodos();
    }

    // ============================================================
    // FILTROS
    // ============================================================
    @Override
    public List<Cliente> filtrarClientes(String tipoFiltro) {

        String sql;

        switch (tipoFiltro) {

            case "vip":
                sql = "SELECT * FROM clientes WHERE vip = 1";
                break;

            case "moroso":
                sql = "SELECT * FROM clientes WHERE moroso = 1";
                break;

            case "inactivo":
                sql = "SELECT * FROM clientes WHERE activo = 0";
                break;

            case "nuevo":
                sql = "SELECT * FROM clientes WHERE fecha_registro >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
                break;

            default:
                return listarTodos();
        }

        return jdbc.query(sql, new ClienteMapper());
    }

    @Override
    public Cliente crearAutomatico(Cliente c) {

        String sql = """
        INSERT INTO clientes (nombres, apellidos, nro_documento, direccion, telefono, activo, fecha_registro)
        VALUES (?, ?, ?, ?, ?, 1, NOW())
    """;

        jdbc.update(sql,
                c.getNombres(),
                c.getApellidos(),
                c.getNro_documento(),
                c.getDireccion(),
                c.getTelefono()
        );

        // Obtener ID generado
        Integer id = jdbc.queryForObject(
                "SELECT LAST_INSERT_ID()",
                Integer.class
        );

        // Retornar el cliente ya insertado
        return buscarPorId(id);
    }
    public Integer registrarRapido(String documento, String nombre, String telefono) {

        String sql = "INSERT INTO clientes (nro_documento, nombres, telefono) VALUES (?, ?, ?)";
        jdbc.update(sql, documento, nombre, telefono);

        return jdbc.queryForObject(
                "SELECT LAST_INSERT_ID()",
                Integer.class
        );
    }

}
