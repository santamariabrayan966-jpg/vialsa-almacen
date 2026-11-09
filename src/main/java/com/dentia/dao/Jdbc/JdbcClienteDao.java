package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.IClienteDao;
import com.dentia.dto.interno.clientes.ClienteCompraDto;
import com.dentia.dto.interno.clientes.ClienteRankingDto;
import com.dentia.dto.interno.clientes.ClienteResumenComercialDto;
import com.dentia.model.interno.Cliente;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
            c.setRazonSocial(rs.getString("razon_social"));
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

            c.setGoogleSub(rs.getString("google_sub"));
            c.setCuentaOnlineActiva(rs.getBoolean("cuenta_online_activa"));
            c.setFechaVinculacionGoogle(rs.getTimestamp("fecha_vinculacion_google"));
            c.setPerfilCompleto(rs.getBoolean("perfil_completo"));
            c.setSujetoDetraccion(rs.getBoolean("sujetoDetraccion"));

            return c;
        }
    }

    // ============================================================
    // BÚSQUEDA POR DOCUMENTO
    // ============================================================
    @Override
    public Cliente buscarPorDocumento(String documento) {
        String sql = "SELECT * FROM clientes WHERE nro_documento = ?";
        List<Cliente> clientes = jdbc.query(sql, new ClienteMapper(), documento);
        return clientes.isEmpty() ? null : clientes.get(0);
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
                (nombres, apellidos, razon_social, nro_documento, direccion, telefono, correo,
                 idTipoDocumento, fecha_registro, activo, foto,
                 google_sub, cuenta_online_activa, fecha_vinculacion_google, perfil_completo, sujetoDetraccion)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), 1, ?, ?, ?, ?, ?, ?)
            """;

        return jdbc.update(sql,
                cliente.getNombres(),
                cliente.getApellidos(),
                cliente.getRazonSocial(),
                cliente.getNro_documento(),
                cliente.getDireccion(),
                cliente.getTelefono(),
                cliente.getCorreo(),
                cliente.getIdTipoDocumento(),
                cliente.getFoto(),
                cliente.getGoogleSub(),
                cliente.isCuentaOnlineActiva(),
                cliente.getFechaVinculacionGoogle(),
                cliente.isPerfilCompleto(),
                cliente.isSujetoDetraccion()
        );
    }

    // ============================================================
    // ACTUALIZAR
    // ============================================================
    @Override
    public int actualizar(Cliente cliente) {
        String sql = """
            UPDATE clientes
                SET nombres = ?,\s
                    apellidos = ?,\s
                    razon_social = ?,
                    nro_documento = ?, direccion = ?,
                telefono = ?, correo = ?, idTipoDocumento = ?, foto = ?,
                google_sub = ?, cuenta_online_activa = ?,
                fecha_vinculacion_google = ?, perfil_completo = ?, sujetoDetraccion = ?
            WHERE idClientes = ?
            """;

        return jdbc.update(sql,
                cliente.getNombres(),
                cliente.getApellidos(),
                cliente.getRazonSocial(),
                cliente.getNro_documento(),
                cliente.getDireccion(),
                cliente.getTelefono(),
                cliente.getCorreo(),
                cliente.getIdTipoDocumento(),
                cliente.getFoto(),
                cliente.getGoogleSub(),
                cliente.isCuentaOnlineActiva(),
                cliente.getFechaVinculacionGoogle(),
                cliente.isPerfilCompleto(),
                cliente.isSujetoDetraccion(),
                cliente.getIdClientes()
        );
    }

    // ============================================================
    // ELIMINAR
    // ============================================================
    @Override
    public int eliminar(Integer idCliente) {
        return jdbc.update("UPDATE clientes SET activo = 0 WHERE idClientes = ?", idCliente);
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
        WHERE nombres LIKE ?
           OR apellidos LIKE ?
           OR razon_social LIKE ?
           OR correo LIKE ?
           OR nro_documento LIKE ?
           OR telefono LIKE ?
        """;

        String like = "%" + filtro + "%";

        return jdbc.query(sql, new ClienteMapper(), like, like, like, like, like, like);
    }

    @Override
    public Cliente buscarPorDocumentoParaActualizar(String documento) {
        List<Cliente> clientes = jdbc.query(
                "SELECT * FROM clientes WHERE nro_documento = ? FOR UPDATE",
                new ClienteMapper(), documento);
        return clientes.isEmpty() ? null : clientes.get(0);
    }

    @Override
    public List<Cliente> listarPagina(String busqueda, String filtro, int limite, int offset) {
        StringBuilder sql = new StringBuilder("SELECT c.* FROM clientes c WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();
        agregarFiltrosClientes(sql, parametros, busqueda, filtro);
        sql.append(" ORDER BY c.fecha_registro DESC, c.idClientes DESC LIMIT ? OFFSET ?");
        parametros.add(limite);
        parametros.add(offset);
        return jdbc.query(sql.toString(), new ClienteMapper(), parametros.toArray());
    }

    @Override
    public long contarClientes(String busqueda, String filtro) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM clientes c WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();
        agregarFiltrosClientes(sql, parametros, busqueda, filtro);
        Long total = jdbc.queryForObject(sql.toString(), Long.class, parametros.toArray());
        return total != null ? total : 0;
    }

    private void agregarFiltrosClientes(StringBuilder sql, List<Object> parametros, String busqueda, String filtro) {
        if (busqueda != null && !busqueda.isBlank()) {
            String like = "%" + busqueda.trim() + "%";
            sql.append(" AND (c.nombres LIKE ? OR c.apellidos LIKE ? OR c.razon_social LIKE ? OR c.correo LIKE ? OR c.nro_documento LIKE ? OR c.telefono LIKE ?)");
            for (int i = 0; i < 6; i++) parametros.add(like);
        }

        String filtroNormalizado = filtro == null ? "TODOS" : filtro.trim().toUpperCase();
        switch (filtroNormalizado) {
            case "VIP" -> sql.append(" AND c.vip = 1");
            case "MOROSO" -> sql.append(" AND c.moroso = 1");
            case "INACTIVO" -> sql.append(" AND c.activo = 0");
            case "NUEVO" -> sql.append(" AND c.fecha_registro >= DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')");
            case "ONLINE" -> {
                sql.append(" AND EXISTS (SELECT 1 FROM ventas v WHERE v.idClientes = c.idClientes AND ").append(VENTAS_VALIDAS)
                        .append(" AND UPPER(COALESCE(v.canal_venta, '')) = 'ONLINE')")
                        .append(" AND NOT EXISTS (SELECT 1 FROM ventas v WHERE v.idClientes = c.idClientes AND ").append(VENTAS_VALIDAS)
                        .append(" AND UPPER(COALESCE(v.canal_venta, '')) = 'PRESENCIAL')");
            }
            case "PRESENCIAL" -> {
                sql.append(" AND EXISTS (SELECT 1 FROM ventas v WHERE v.idClientes = c.idClientes AND ").append(VENTAS_VALIDAS)
                        .append(" AND UPPER(COALESCE(v.canal_venta, '')) = 'PRESENCIAL')")
                        .append(" AND NOT EXISTS (SELECT 1 FROM ventas v WHERE v.idClientes = c.idClientes AND ").append(VENTAS_VALIDAS)
                        .append(" AND UPPER(COALESCE(v.canal_venta, '')) = 'ONLINE')");
            }
            case "MIXTO" -> sql.append(" AND EXISTS (SELECT 1 FROM ventas v WHERE v.idClientes = c.idClientes AND ").append(VENTAS_VALIDAS)
                    .append(" AND UPPER(COALESCE(v.canal_venta, '')) = 'ONLINE')")
                    .append(" AND EXISTS (SELECT 1 FROM ventas v WHERE v.idClientes = c.idClientes AND ").append(VENTAS_VALIDAS)
                    .append(" AND UPPER(COALESCE(v.canal_venta, '')) = 'PRESENCIAL')");
            case "SIN_COMPRAS" -> sql.append(" AND NOT EXISTS (SELECT 1 FROM ventas v WHERE v.idClientes = c.idClientes AND ").append(VENTAS_VALIDAS).append(")");
            default -> { }
        }
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
               t.nombre AS tipoDocumento
        FROM clientes c
        LEFT JOIN tipodocumento t ON c.idTipoDocumento = t.idTipoDocumento
        WHERE c.idClientes = ?
        """;

        try {
            return jdbc.queryForObject(sql, (rs, rowNum) -> {

                Cliente c = new Cliente();

                // Datos básicos
                c.setIdClientes(rs.getInt("idClientes"));
                c.setNombres(rs.getString("nombres"));
                c.setApellidos(rs.getString("apellidos"));
                c.setRazonSocial(rs.getString("razon_social"));
                c.setNro_documento(rs.getString("nro_documento"));
                c.setDireccion(rs.getString("direccion"));
                c.setTelefono(rs.getString("telefono"));
                c.setCorreo(rs.getString("correo"));
                c.setIdTipoDocumento(rs.getInt("idTipoDocumento"));

                // Datos adicionales
                c.setTipoDocumentoNombre(rs.getString("tipoDocumento"));
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
                sql = "SELECT * FROM clientes WHERE fecha_registro >= DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')";
                break;

            default:
                return listarTodos();
        }

        return jdbc.query(sql, new ClienteMapper());
    }

    @Override
    public Cliente crearAutomatico(Cliente c) {

        registrar(c);

        Integer id = jdbc.queryForObject(
                "SELECT LAST_INSERT_ID()",
                Integer.class
        );

        return buscarPorId(id);
    }

    @Override
    public Cliente buscarPorGoogleSub(String googleSub) {
        String sql = "SELECT * FROM clientes WHERE google_sub = ?";
        try {
            return jdbc.queryForObject(sql, new ClienteMapper(), googleSub);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public int actualizarDatosGoogle(Cliente cliente) {
        String sql = """
            UPDATE clientes
            SET correo = ?, foto = ?, google_sub = ?, cuenta_online_activa = ?,
                fecha_vinculacion_google = ?
            WHERE idClientes = ?
            """;

        return jdbc.update(sql,
                cliente.getCorreo(),
                cliente.getFoto(),
                cliente.getGoogleSub(),
                cliente.isCuentaOnlineActiva(),
                cliente.getFechaVinculacionGoogle(),
                cliente.getIdClientes()
        );
    }

    private static final String VENTAS_VALIDAS = """
            UPPER(v.EstadoVenta) IN ('PAGADO', 'CREDITO')
            AND NOT EXISTS (
                SELECT 1 FROM pedidos pv
                INNER JOIN estadopedido epv ON epv.idEstadoPedido = pv.idEstadoPedido
                WHERE pv.idVenta = v.idVentas AND UPPER(epv.nombreEstado) = 'ANULADO'
            )
            """;

    private ClienteResumenComercialDto mapResumen(ResultSet rs) throws SQLException {
        Timestamp primera = rs.getTimestamp("primeraCompra");
        Timestamp ultima = rs.getTimestamp("ultimaCompra");
        return new ClienteResumenComercialDto(
                rs.getInt("idCliente"),
                rs.getString("clasificacion"),
                rs.getLong("cantidadCompras"),
                valorDecimal(rs, "totalHistorico"),
                valorDecimal(rs, "ticketPromedio"),
                primera != null ? primera.toLocalDateTime() : null,
                ultima != null ? ultima.toLocalDateTime() : null,
                rs.getString("sedePrincipal"),
                rs.getString("canalPrincipal"),
                rs.getString("metodoPagoPrincipal"),
                rs.getLong("cantidadCanales"),
                rs.getLong("cantidadSedes")
        );
    }

    private BigDecimal valorDecimal(ResultSet rs, String columna) throws SQLException {
        BigDecimal valor = rs.getBigDecimal(columna);
        return valor != null ? valor : BigDecimal.ZERO;
    }

    @Override
    public List<ClienteResumenComercialDto> listarResumenesComerciales() {
        String sql = resumenComercialSql("", "");
        return jdbc.query(sql, (rs, rowNum) -> mapResumen(rs));
    }

    @Override
    public List<ClienteResumenComercialDto> listarResumenesComercialesPorIds(List<Integer> idsClientes) {
        if (idsClientes == null || idsClientes.isEmpty()) return List.of();
        String marcadores = String.join(",", java.util.Collections.nCopies(idsClientes.size(), "?"));
        String sql = resumenComercialSql("WHERE c.idClientes IN (" + marcadores + ")", "");
        return jdbc.query(sql, (rs, rowNum) -> mapResumen(rs), idsClientes.toArray());
    }

    @Override
    public ClienteResumenComercialDto obtenerResumenComercial(Integer idCliente) {
        String sql = resumenComercialSql("WHERE c.idClientes = ?", "");
        List<ClienteResumenComercialDto> resultados = jdbc.query(sql, (rs, rowNum) -> mapResumen(rs), idCliente);
        return resultados.isEmpty() ? resumenVacio(idCliente) : resultados.get(0);
    }

    private String resumenComercialSql(String condicionCliente, String sufijo) {
        return """
            SELECT c.idClientes AS idCliente,
                   CASE
                     WHEN COUNT(v.idVentas) = 0 THEN 'SIN COMPRAS'
                     WHEN SUM(CASE WHEN UPPER(COALESCE(v.canal_venta, '')) = 'ONLINE' THEN 1 ELSE 0 END) > 0
                      AND SUM(CASE WHEN UPPER(COALESCE(v.canal_venta, '')) = 'PRESENCIAL' THEN 1 ELSE 0 END) > 0 THEN 'MIXTO'
                     WHEN SUM(CASE WHEN UPPER(COALESCE(v.canal_venta, '')) = 'ONLINE' THEN 1 ELSE 0 END) > 0 THEN 'ONLINE'
                     WHEN SUM(CASE WHEN UPPER(COALESCE(v.canal_venta, '')) = 'PRESENCIAL' THEN 1 ELSE 0 END) > 0 THEN 'PRESENCIAL'
                     ELSE 'SIN COMPRAS'
                   END AS clasificacion,
                   COUNT(v.idVentas) AS cantidadCompras,
                   COALESCE(SUM(v.TotalVenta), 0) AS totalHistorico,
                   COALESCE(AVG(v.TotalVenta), 0) AS ticketPromedio,
                   MIN(v.FechaVenta) AS primeraCompra,
                   MAX(v.FechaVenta) AS ultimaCompra,
                   (SELECT s2.nombre
                      FROM ventas v2 LEFT JOIN sedes s2 ON s2.idSede = v2.idSede
                     WHERE v2.idClientes = c.idClientes AND UPPER(v2.EstadoVenta) IN ('PAGADO', 'CREDITO')
                       AND NOT EXISTS (SELECT 1 FROM pedidos p2 INNER JOIN estadopedido e2 ON e2.idEstadoPedido = p2.idEstadoPedido WHERE p2.idVenta = v2.idVentas AND UPPER(e2.nombreEstado) = 'ANULADO')
                     GROUP BY v2.idSede, s2.nombre ORDER BY COUNT(*) DESC, MAX(v2.FechaVenta) DESC LIMIT 1) AS sedePrincipal,
                   (SELECT UPPER(COALESCE(NULLIF(v3.canal_venta, ''), 'SIN CANAL'))
                      FROM ventas v3
                     WHERE v3.idClientes = c.idClientes AND UPPER(v3.EstadoVenta) IN ('PAGADO', 'CREDITO')
                       AND NOT EXISTS (SELECT 1 FROM pedidos p3 INNER JOIN estadopedido e3 ON e3.idEstadoPedido = p3.idEstadoPedido WHERE p3.idVenta = v3.idVentas AND UPPER(e3.nombreEstado) = 'ANULADO')
                     GROUP BY UPPER(COALESCE(NULLIF(v3.canal_venta, ''), 'SIN CANAL'))
                     ORDER BY COUNT(*) DESC, MAX(v3.FechaVenta) DESC LIMIT 1) AS canalPrincipal,
                   (SELECT UPPER(COALESCE(NULLIF(v4.metodoPago, ''), NULLIF(v4.FormaPago, ''), 'SIN REGISTRO'))
                      FROM ventas v4
                     WHERE v4.idClientes = c.idClientes AND UPPER(v4.EstadoVenta) IN ('PAGADO', 'CREDITO')
                       AND NOT EXISTS (SELECT 1 FROM pedidos p4 INNER JOIN estadopedido e4 ON e4.idEstadoPedido = p4.idEstadoPedido WHERE p4.idVenta = v4.idVentas AND UPPER(e4.nombreEstado) = 'ANULADO')
                     GROUP BY UPPER(COALESCE(NULLIF(v4.metodoPago, ''), NULLIF(v4.FormaPago, ''), 'SIN REGISTRO'))
                     ORDER BY COUNT(*) DESC, MAX(v4.FechaVenta) DESC LIMIT 1) AS metodoPagoPrincipal,
                   COUNT(DISTINCT NULLIF(UPPER(v.canal_venta), '')) AS cantidadCanales,
                   COUNT(DISTINCT v.idSede) AS cantidadSedes
              FROM clientes c
              LEFT JOIN ventas v ON v.idClientes = c.idClientes
                                AND UPPER(v.EstadoVenta) IN ('PAGADO', 'CREDITO')
            """ + condicionCliente + " GROUP BY c.idClientes " + sufijo;
    }

    private ClienteResumenComercialDto resumenVacio(Integer idCliente) {
        return new ClienteResumenComercialDto(idCliente, "SIN COMPRAS", 0, BigDecimal.ZERO,
                BigDecimal.ZERO, null, null, null, null, null, 0, 0);
    }

    @Override
    public List<ClienteCompraDto> listarComprasCliente(
            Integer idCliente, String busqueda, String canal, String estado,
            Integer idSede, LocalDate fecha, Integer mes, Integer anio, LocalDate fechaDesde,
            LocalDate fechaHasta, int limite, int offset) {

        StringBuilder sql = new StringBuilder("""
            SELECT v.idVentas, v.CodigoTicket, v.TipoComprobante, v.NroComprobante,
                   UPPER(COALESCE(NULLIF(v.canal_venta, ''), 'SIN CANAL')) AS canal,
                   v.FechaVenta, s.nombre AS sede, u.NombreUsuario AS vendedor,
                   UPPER(COALESCE(NULLIF(v.metodoPago, ''), NULLIF(v.FormaPago, ''), 'SIN REGISTRO')) AS metodoPago,
                   v.TotalVenta, v.EstadoVenta,
                   (SELECT ep.nombreEstado FROM pedidos p
                     LEFT JOIN estadopedido ep ON ep.idEstadoPedido = p.idEstadoPedido
                     WHERE p.idVenta = v.idVentas ORDER BY p.idPedido DESC LIMIT 1) AS estadoEntrega
              FROM ventas v
              LEFT JOIN sedes s ON s.idSede = v.idSede
              LEFT JOIN usuarios u ON u.idUsuario = v.idUsuario
             WHERE v.idClientes = ? AND """ + " " + VENTAS_VALIDAS);
        List<Object> parametros = new ArrayList<>();
        parametros.add(idCliente);
        agregarFiltrosCompras(sql, parametros, busqueda, canal, estado, idSede, fecha, mes, anio, fechaDesde, fechaHasta);
        sql.append(" ORDER BY v.FechaVenta DESC, v.idVentas DESC LIMIT ? OFFSET ?");
        parametros.add(limite);
        parametros.add(offset);

        return jdbc.query(sql.toString(), (rs, rowNum) -> {
            Timestamp fechaVenta = rs.getTimestamp("FechaVenta");
            String comprobante = rs.getString("NroComprobante");
            boolean disponible = comprobante != null && !comprobante.isBlank();
            int idVenta = rs.getInt("idVentas");
            String codigoTicket = rs.getString("CodigoTicket");
            String codigo = codigoTicket != null && !codigoTicket.isBlank() ? codigoTicket : comprobante;
            return new ClienteCompraDto(
                    idVenta, codigo, rs.getString("TipoComprobante"), comprobante,
                    rs.getString("canal"), fechaVenta != null ? fechaVenta.toLocalDateTime() : null,
                    rs.getString("sede"), rs.getString("vendedor"), rs.getString("metodoPago"),
                    valorDecimal(rs, "TotalVenta"), rs.getString("EstadoVenta"),
                    rs.getString("estadoEntrega"), disponible,
                    "/interno/ventas/detalle/" + idVenta,
                    disponible ? "/interno/ventas/pdf/" + idVenta : null
            );
        }, parametros.toArray());
    }

    @Override
    public long contarComprasCliente(
            Integer idCliente, String busqueda, String canal, String estado,
            Integer idSede, LocalDate fecha, Integer mes, Integer anio, LocalDate fechaDesde,
            LocalDate fechaHasta) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ventas v WHERE v.idClientes = ? AND " + VENTAS_VALIDAS);
        List<Object> parametros = new ArrayList<>();
        parametros.add(idCliente);
        agregarFiltrosCompras(sql, parametros, busqueda, canal, estado, idSede, fecha, mes, anio, fechaDesde, fechaHasta);
        Long total = jdbc.queryForObject(sql.toString(), Long.class, parametros.toArray());
        return total != null ? total : 0;
    }

    @Override
    public ClienteResumenComercialDto resumirComprasCliente(
            Integer idCliente, String busqueda, String canal, String estado,
            Integer idSede, LocalDate fecha, Integer mes, Integer anio, LocalDate fechaDesde,
            LocalDate fechaHasta) {
        StringBuilder sql = new StringBuilder("""
            SELECT ? AS idCliente,
                   CASE
                     WHEN COUNT(*) = 0 THEN 'SIN COMPRAS'
                     WHEN COUNT(DISTINCT UPPER(NULLIF(v.canal_venta, ''))) > 1 THEN 'MIXTO'
                     WHEN MAX(UPPER(v.canal_venta)) = 'ONLINE' THEN 'ONLINE'
                     WHEN MAX(UPPER(v.canal_venta)) = 'PRESENCIAL' THEN 'PRESENCIAL'
                     ELSE 'SIN COMPRAS'
                   END AS clasificacion,
                   COUNT(*) AS cantidadCompras,
                   COALESCE(SUM(v.TotalVenta), 0) AS totalHistorico,
                   COALESCE(AVG(v.TotalVenta), 0) AS ticketPromedio,
                   MIN(v.FechaVenta) AS primeraCompra,
                   MAX(v.FechaVenta) AS ultimaCompra,
                   NULL AS sedePrincipal, NULL AS canalPrincipal, NULL AS metodoPagoPrincipal,
                   COUNT(DISTINCT NULLIF(UPPER(v.canal_venta), '')) AS cantidadCanales,
                   COUNT(DISTINCT v.idSede) AS cantidadSedes
              FROM ventas v WHERE v.idClientes = ? AND """ + " " + VENTAS_VALIDAS);
        List<Object> parametros = new ArrayList<>();
        parametros.add(idCliente);
        parametros.add(idCliente);
        agregarFiltrosCompras(sql, parametros, busqueda, canal, estado, idSede, fecha, mes, anio, fechaDesde, fechaHasta);
        return jdbc.queryForObject(sql.toString(), (rs, rowNum) -> mapResumen(rs), parametros.toArray());
    }

    private void agregarFiltrosCompras(
            StringBuilder sql, List<Object> parametros, String busqueda, String canal,
            String estado, Integer idSede, LocalDate fecha, Integer mes, Integer anio,
            LocalDate fechaDesde, LocalDate fechaHasta) {
        if (busqueda != null && !busqueda.isBlank()) {
            sql.append(" AND (v.NroComprobante LIKE ? OR v.CodigoTicket LIKE ? OR CAST(v.idVentas AS CHAR) LIKE ?)");
            String like = "%" + busqueda.trim() + "%";
            parametros.add(like); parametros.add(like); parametros.add(like);
        }
        if (canal != null && !canal.isBlank() && !"TODOS".equalsIgnoreCase(canal)) {
            sql.append(" AND UPPER(v.canal_venta) = ?");
            parametros.add(canal.trim().toUpperCase());
        }
        if (estado != null && !estado.isBlank() && !"TODOS".equalsIgnoreCase(estado)) {
            sql.append(" AND UPPER(v.EstadoVenta) = ?");
            parametros.add(estado.trim().toUpperCase());
        }
        if (idSede != null && idSede > 0) { sql.append(" AND v.idSede = ?"); parametros.add(idSede); }
        if (fecha != null) {
            sql.append(" AND v.FechaVenta >= ? AND v.FechaVenta < ?");
            parametros.add(fecha.atStartOfDay());
            parametros.add(fecha.plusDays(1).atStartOfDay());
        } else {
            if (mes != null && mes >= 1 && mes <= 12) { sql.append(" AND MONTH(v.FechaVenta) = ?"); parametros.add(mes); }
            if (anio != null && anio > 0) { sql.append(" AND YEAR(v.FechaVenta) = ?"); parametros.add(anio); }
            if (fechaDesde != null) { sql.append(" AND v.FechaVenta >= ?"); parametros.add(fechaDesde.atStartOfDay()); }
            if (fechaHasta != null) { sql.append(" AND v.FechaVenta < ?"); parametros.add(fechaHasta.plusDays(1).atStartOfDay()); }
        }
    }

    @Override
    public List<ClienteRankingDto> obtenerRanking(
            Integer mes, Integer anio, String canal, Integer idSede,
            String criterio, int limite) {
        StringBuilder sql = new StringBuilder("""
            SELECT c.idClientes,
                   COALESCE(NULLIF(c.razon_social, ''), NULLIF(CONCAT_WS(' ', c.nombres, c.apellidos), ''), 'Cliente') AS cliente,
                   c.nro_documento,
                   COUNT(v.idVentas) AS cantidadCompras,
                   COALESCE(SUM(v.TotalVenta), 0) AS totalComprado,
                   COALESCE(AVG(v.TotalVenta), 0) AS ticketPromedio,
                   MAX(v.FechaVenta) AS ultimaCompra
              FROM ventas v INNER JOIN clientes c ON c.idClientes = v.idClientes
             WHERE """ + " " + VENTAS_VALIDAS);
        List<Object> parametros = new ArrayList<>();
        if (mes != null && mes >= 1 && mes <= 12) { sql.append(" AND MONTH(v.FechaVenta) = ?"); parametros.add(mes); }
        if (anio != null && anio > 0) { sql.append(" AND YEAR(v.FechaVenta) = ?"); parametros.add(anio); }
        if ("MIXTO".equalsIgnoreCase(canal)) {
            sql.append("""
                 AND EXISTS (SELECT 1 FROM ventas vm1 WHERE vm1.idClientes = c.idClientes
                              AND UPPER(vm1.EstadoVenta) IN ('PAGADO', 'CREDITO') AND UPPER(vm1.canal_venta) = 'ONLINE'
                              AND NOT EXISTS (SELECT 1 FROM pedidos pm1 INNER JOIN estadopedido em1 ON em1.idEstadoPedido = pm1.idEstadoPedido WHERE pm1.idVenta = vm1.idVentas AND UPPER(em1.nombreEstado) = 'ANULADO'))
                 AND EXISTS (SELECT 1 FROM ventas vm2 WHERE vm2.idClientes = c.idClientes
                              AND UPPER(vm2.EstadoVenta) IN ('PAGADO', 'CREDITO') AND UPPER(vm2.canal_venta) = 'PRESENCIAL'
                              AND NOT EXISTS (SELECT 1 FROM pedidos pm2 INNER JOIN estadopedido em2 ON em2.idEstadoPedido = pm2.idEstadoPedido WHERE pm2.idVenta = vm2.idVentas AND UPPER(em2.nombreEstado) = 'ANULADO'))
                """);
        } else if (canal != null && !canal.isBlank() && !"TODOS".equalsIgnoreCase(canal)) {
            sql.append(" AND UPPER(v.canal_venta) = ?"); parametros.add(canal.trim().toUpperCase());
        }
        if (idSede != null && idSede > 0) { sql.append(" AND v.idSede = ?"); parametros.add(idSede); }
        sql.append(" GROUP BY c.idClientes, c.razon_social, c.nombres, c.apellidos, c.nro_documento");
        if ("COMPRAS".equalsIgnoreCase(criterio)) {
            sql.append(" ORDER BY cantidadCompras DESC, totalComprado DESC");
        } else {
            sql.append(" ORDER BY totalComprado DESC, cantidadCompras DESC");
        }
        sql.append(" LIMIT ?"); parametros.add(limite);

        return jdbc.query(sql.toString(), (rs, rowNum) -> {
            Timestamp ultima = rs.getTimestamp("ultimaCompra");
            return new ClienteRankingDto(rowNum + 1, rs.getInt("idClientes"), rs.getString("cliente"),
                    rs.getString("nro_documento"), "SIN COMPRAS", rs.getLong("cantidadCompras"),
                    valorDecimal(rs, "totalComprado"), valorDecimal(rs, "ticketPromedio"),
                    ultima != null ? ultima.toLocalDateTime() : null, null, null);
        }, parametros.toArray());
    }

    @Override
    public List<Integer> listarAniosConCompras() {
        return jdbc.queryForList("""
                SELECT DISTINCT YEAR(v.FechaVenta) FROM ventas v
                 WHERE UPPER(v.EstadoVenta) IN ('PAGADO', 'CREDITO')
                   AND NOT EXISTS (SELECT 1 FROM pedidos p INNER JOIN estadopedido ep ON ep.idEstadoPedido = p.idEstadoPedido
                                    WHERE p.idVenta = v.idVentas AND UPPER(ep.nombreEstado) = 'ANULADO')
                 ORDER BY 1 DESC
                """, Integer.class);
    }

}
