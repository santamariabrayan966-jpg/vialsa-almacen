package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.IProveedorDao;
import com.dentia.dto.interno.ProveedorCompraDto;
import com.dentia.dto.interno.ProveedorComprasResumenDto;
import com.dentia.dto.interno.ProveedorSedeResumenDto;
import com.dentia.model.interno.Proveedor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcProveedorDao implements IProveedorDao {

    private final JdbcTemplate jdbcTemplate;

    public JdbcProveedorDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ==== MAPEADOR SIN FechaRegistro (CORREGIDO) ====
    private final RowMapper<Proveedor> mapRow = (rs, rowNum) -> {
        Proveedor p = new Proveedor();
        p.setIdProveedor(rs.getInt("idProveedor"));
        p.setNombreProveedor(rs.getString("NombreProveedor"));
        p.setNroDocumento(rs.getString("NroDocumento"));
        p.setDireccion(rs.getString("Direccion"));
        p.setTelefono(rs.getString("Telefono"));
        p.setCorreo(rs.getString("Correo"));
        p.setEstado(rs.getInt("activo"));
        return p;
    };

    @Override
    public List<Proveedor> listar() {
        String sql = """
        SELECT idProveedor, NombreProveedor, NroDocumento,
               Direccion, Telefono, Correo, activo
        FROM proveedores
        WHERE activo <> 2     -- ocultar eliminados
        ORDER BY activo DESC, NombreProveedor ASC
    """;
        return jdbcTemplate.query(sql, mapRow);
    }


    @Override
    public Optional<Proveedor> buscarPorId(Integer id) {
        String sql = """
            SELECT idProveedor, NombreProveedor, NroDocumento,
                   Direccion, Telefono, Correo, activo
            FROM proveedores
            WHERE idProveedor = ?
        """;
        List<Proveedor> proveedores = jdbcTemplate.query(sql, mapRow, id);
        return proveedores.isEmpty()
                ? Optional.empty()
                : Optional.of(proveedores.get(0));
    }

    @Override
    public Optional<Proveedor> buscarPorNroDocumento(String nroDocumento) {
        String sql = """
            SELECT idProveedor, NombreProveedor, NroDocumento,
                   Direccion, Telefono, Correo, activo
            FROM proveedores
            WHERE NroDocumento = ?
        """;
        List<Proveedor> list = jdbcTemplate.query(sql, mapRow, nroDocumento);
        return list.isEmpty()
                ? Optional.empty()
                : Optional.of(list.get(0));
    }

    @Override
    public int crear(Proveedor proveedor) {
        String sql = """
            INSERT INTO proveedores
                (NombreProveedor, NroDocumento, Direccion, Telefono, Correo, activo)
            VALUES (?, ?, ?, ?, ?, 1)
        """;
        return jdbcTemplate.update(sql,
                proveedor.getNombreProveedor(),
                proveedor.getNroDocumento(),
                proveedor.getDireccion(),
                proveedor.getTelefono(),
                proveedor.getCorreo()
        );
    }

    @Override
    public int actualizar(Proveedor proveedor) {
        String sql = """
            UPDATE proveedores SET
                NombreProveedor = ?,
                NroDocumento   = ?,
                Direccion      = ?,
                Telefono       = ?,
                Correo         = ?,
                activo         = ?
            WHERE idProveedor = ?
        """;
        return jdbcTemplate.update(sql,
                proveedor.getNombreProveedor(),
                proveedor.getNroDocumento(),
                proveedor.getDireccion(),
                proveedor.getTelefono(),
                proveedor.getCorreo(),
                proveedor.getEstado(),
                proveedor.getIdProveedor()
        );
    }

    @Override
    public int eliminar(Integer id) {
        // Estado 2 = eliminado (no visible)
        String sql = "UPDATE proveedores SET activo = 2 WHERE idProveedor = ?";
        return jdbcTemplate.update(sql, id);
    }


    @Override
    public int cambiarEstado(Integer id, boolean activo) {
        String sql = "UPDATE proveedores SET activo = ? WHERE idProveedor = ?";
        return jdbcTemplate.update(sql, activo, id);
    }

    @Override
    public int actualizarEstado(Integer id, boolean activo) {
        String sql = "UPDATE proveedores SET activo = ? WHERE idProveedor = ?";
        return jdbcTemplate.update(sql, activo, id);
    }
    @Override
    public List<Proveedor> listarActivos() {
        String sql = """
        SELECT idProveedor, NombreProveedor, NroDocumento,
               Direccion, Telefono, Correo, activo
        FROM proveedores
        WHERE activo = 1
        ORDER BY NombreProveedor ASC
    """;
        return jdbcTemplate.query(sql, mapRow);
    }

    @Override
    public List<Proveedor> listarPaginado(String busqueda, int limite, int desplazamiento) {
        StringBuilder sql = new StringBuilder("""
            SELECT idProveedor, NombreProveedor, NroDocumento,
                   Direccion, Telefono, Correo, activo
            FROM proveedores
            WHERE activo <> 2
        """);
        List<Object> parametros = new ArrayList<>();
        agregarFiltroProveedor(sql, parametros, busqueda);
        sql.append(" ORDER BY activo DESC, NombreProveedor ASC, idProveedor ASC LIMIT ? OFFSET ?");
        parametros.add(limite);
        parametros.add(desplazamiento);
        return jdbcTemplate.query(sql.toString(), mapRow, parametros.toArray());
    }

    @Override
    public long contar(String busqueda) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM proveedores WHERE activo <> 2");
        List<Object> parametros = new ArrayList<>();
        agregarFiltroProveedor(sql, parametros, busqueda);
        Long total = jdbcTemplate.queryForObject(sql.toString(), Long.class, parametros.toArray());
        return total != null ? total : 0;
    }

    @Override
    public ProveedorComprasResumenDto obtenerResumenCompras(Integer idProveedor) {
        String sql = """
            SELECT COUNT(*) AS totalCompras,
                   COALESCE(SUM(CASE
                       WHEN UPPER(COALESCE(Estado, '')) NOT IN ('ANULADA', 'BORRADOR')
                       THEN CASE WHEN UPPER(COALESCE(Moneda, 'PEN')) = 'USD'
                            THEN COALESCE(TotalCompra, 0) * COALESCE(TipoCambio, 0)
                            ELSE COALESCE(TotalCompra, 0) END
                       ELSE 0 END), 0) AS totalHistorico,
                   MAX(CASE
                       WHEN UPPER(COALESCE(Estado, '')) NOT IN ('ANULADA', 'BORRADOR')
                       THEN FechaCompra ELSE NULL END) AS ultimaCompra
            FROM compras
            WHERE idProveedor = ?
        """;

        ProveedorComprasResumenDto resumen = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            Timestamp ultima = rs.getTimestamp("ultimaCompra");
            BigDecimal total = rs.getBigDecimal("totalHistorico");
            return new ProveedorComprasResumenDto(
                    rs.getLong("totalCompras"),
                    total != null ? total : BigDecimal.ZERO,
                    ultima != null ? ultima.toLocalDateTime() : null,
                    List.of()
            );
        }, idProveedor);

        List<String> sedes = jdbcTemplate.queryForList("""
            SELECT DISTINCT s.nombre
            FROM compras c
            JOIN sedes s ON s.idSede = c.idSede
            WHERE c.idProveedor = ?
              AND UPPER(COALESCE(c.Estado, '')) NOT IN ('ANULADA', 'BORRADOR')
            ORDER BY s.nombre
        """, String.class, idProveedor);

        if (resumen == null) {
            return ProveedorComprasResumenDto.vacio();
        }
        return new ProveedorComprasResumenDto(
                resumen.totalCompras(), resumen.totalHistorico(), resumen.ultimaCompra(), sedes);
    }

    @Override
    public List<ProveedorSedeResumenDto> obtenerComprasPorSede(Integer idProveedor) {
        String sql = """
            SELECT c.idSede,
                   COALESCE(s.nombre, 'Sin sede asignada') AS nombreSede,
                   COUNT(*) AS cantidadCompras,
                   COALESCE(SUM(CASE WHEN UPPER(COALESCE(c.Moneda, 'PEN')) = 'USD'
                       THEN COALESCE(c.TotalCompra, 0) * COALESCE(c.TipoCambio, 0)
                       ELSE COALESCE(c.TotalCompra, 0) END), 0) AS totalComprado,
                   MAX(c.FechaCompra) AS ultimaCompra
            FROM compras c
            LEFT JOIN sedes s ON s.idSede = c.idSede
            WHERE c.idProveedor = ?
              AND UPPER(COALESCE(c.Estado, '')) NOT IN ('ANULADA', 'BORRADOR')
            GROUP BY c.idSede, s.nombre
            ORDER BY totalComprado DESC, nombreSede ASC
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp ultima = rs.getTimestamp("ultimaCompra");
            return new ProveedorSedeResumenDto(
                    (Integer) rs.getObject("idSede"),
                    rs.getString("nombreSede"),
                    rs.getLong("cantidadCompras"),
                    rs.getBigDecimal("totalComprado"),
                    ultima != null ? ultima.toLocalDateTime() : null
            );
        }, idProveedor);
    }

    @Override
    public List<ProveedorCompraDto> listarCompras(
            Integer idProveedor, String busqueda, Integer idSede, String estado,
            LocalDate fechaDesde, LocalDate fechaHasta, int limite, int desplazamiento) {

        StringBuilder sql = new StringBuilder("""
            SELECT c.idCompra, c.FechaCompra, c.NroOrdenCompra,
                   c.TipoComprobante, c.Serie, c.Numero,
                   COALESCE(NULLIF(c.NroComprobante, ''),
                            CONCAT_WS('-', NULLIF(c.Serie, ''), NULLIF(c.Numero, ''))) AS nroComprobante,
                   p.NombreProveedor AS nombreProveedor,
                   c.idSede, s.nombre AS nombreSede,
                   u.NombreUsuario AS nombreUsuario,
                   c.Subtotal, c.MontoIGV, c.TotalCompra, c.Moneda,
                   c.FormaPago, c.TipoRecepcion, c.Estado,
                   c.NroGuiaRemision, c.FechaEmision, c.FechaVencimiento
            FROM compras c
            JOIN proveedores p ON p.idProveedor = c.idProveedor
            JOIN usuarios u ON u.idUsuario = c.idUsuario
            LEFT JOIN sedes s ON s.idSede = c.idSede
            WHERE c.idProveedor = ?
        """);
        List<Object> parametros = new ArrayList<>();
        parametros.add(idProveedor);
        agregarFiltrosCompras(sql, parametros, busqueda, idSede, estado, fechaDesde, fechaHasta);
        sql.append(" ORDER BY c.FechaCompra DESC, c.idCompra DESC LIMIT ? OFFSET ?");
        parametros.add(limite);
        parametros.add(desplazamiento);

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            Timestamp fecha = rs.getTimestamp("FechaCompra");
            Date emision = rs.getDate("FechaEmision");
            Date vencimiento = rs.getDate("FechaVencimiento");
            return new ProveedorCompraDto(
                    rs.getInt("idCompra"),
                    fecha != null ? fecha.toLocalDateTime() : null,
                    rs.getString("NroOrdenCompra"),
                    rs.getString("TipoComprobante"),
                    rs.getString("Serie"),
                    rs.getString("Numero"),
                    rs.getString("nroComprobante"),
                    rs.getString("nombreProveedor"),
                    (Integer) rs.getObject("idSede"),
                    rs.getString("nombreSede"),
                    rs.getString("nombreUsuario"),
                    rs.getBigDecimal("Subtotal"),
                    rs.getBigDecimal("MontoIGV"),
                    rs.getBigDecimal("TotalCompra"),
                    rs.getString("Moneda"),
                    rs.getString("FormaPago"),
                    rs.getString("TipoRecepcion"),
                    rs.getString("Estado"),
                    rs.getString("NroGuiaRemision"),
                    emision != null ? emision.toLocalDate() : null,
                    vencimiento != null ? vencimiento.toLocalDate() : null
            );
        }, parametros.toArray());
    }

    @Override
    public long contarCompras(
            Integer idProveedor, String busqueda, Integer idSede, String estado,
            LocalDate fechaDesde, LocalDate fechaHasta) {

        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(*)
            FROM compras c
            LEFT JOIN sedes s ON s.idSede = c.idSede
            LEFT JOIN usuarios u ON u.idUsuario = c.idUsuario
            WHERE c.idProveedor = ?
        """);
        List<Object> parametros = new ArrayList<>();
        parametros.add(idProveedor);
        agregarFiltrosCompras(sql, parametros, busqueda, idSede, estado, fechaDesde, fechaHasta);
        Long total = jdbcTemplate.queryForObject(sql.toString(), Long.class, parametros.toArray());
        return total != null ? total : 0;
    }

    @Override
    public List<String> listarEstadosCompras(Integer idProveedor) {
        return jdbcTemplate.queryForList("""
            SELECT DISTINCT Estado
            FROM compras
            WHERE idProveedor = ? AND Estado IS NOT NULL AND TRIM(Estado) <> ''
            ORDER BY Estado
        """, String.class, idProveedor);
    }

    private void agregarFiltrosCompras(
            StringBuilder sql, List<Object> parametros, String busqueda, Integer idSede,
            String estado, LocalDate fechaDesde, LocalDate fechaHasta) {

        if (busqueda != null && !busqueda.isBlank()) {
            String patron = "%" + busqueda.trim().toLowerCase() + "%";
            sql.append("""
                 AND (LOWER(COALESCE(c.NroOrdenCompra, '')) LIKE ?
                   OR LOWER(COALESCE(c.NroComprobante, '')) LIKE ?
                   OR LOWER(COALESCE(c.TipoComprobante, '')) LIKE ?
                   OR LOWER(COALESCE(c.Serie, '')) LIKE ?
                   OR LOWER(COALESCE(c.Numero, '')) LIKE ?
                   OR LOWER(COALESCE(s.nombre, '')) LIKE ?
                   OR LOWER(COALESCE(u.NombreUsuario, '')) LIKE ?)
            """);
            for (int i = 0; i < 7; i++) parametros.add(patron);
        }
        if (idSede != null && idSede > 0) {
            sql.append(" AND c.idSede = ?");
            parametros.add(idSede);
        }
        if (estado != null && !estado.isBlank()) {
            sql.append(" AND UPPER(c.Estado) = UPPER(?)");
            parametros.add(estado.trim());
        }
        if (fechaDesde != null) {
            sql.append(" AND c.FechaCompra >= ?");
            parametros.add(Timestamp.valueOf(fechaDesde.atStartOfDay()));
        }
        if (fechaHasta != null) {
            sql.append(" AND c.FechaCompra < ?");
            parametros.add(Timestamp.valueOf(fechaHasta.plusDays(1).atStartOfDay()));
        }
    }

    private void agregarFiltroProveedor(StringBuilder sql, List<Object> parametros, String busqueda) {
        if (busqueda == null || busqueda.isBlank()) return;

        String patron = "%" + busqueda.trim().toLowerCase() + "%";
        sql.append("""
             AND (LOWER(COALESCE(NombreProveedor, '')) LIKE ?
               OR LOWER(COALESCE(NroDocumento, '')) LIKE ?
               OR LOWER(COALESCE(Telefono, '')) LIKE ?
               OR LOWER(COALESCE(Correo, '')) LIKE ?
               OR LOWER(COALESCE(Direccion, '')) LIKE ?)
        """);
        for (int i = 0; i < 5; i++) parametros.add(patron);
    }

}
