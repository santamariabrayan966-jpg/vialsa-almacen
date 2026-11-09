package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.ICompraRecepcionDao;
import com.dentia.model.interno.CompraRecepcion;
import com.dentia.model.interno.CompraRecepcionDetalle;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;

@Repository
public class JdbcCompraRecepcionDao implements ICompraRecepcionDao {

    private final JdbcTemplate jdbcTemplate;

    public JdbcCompraRecepcionDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int registrarRecepcion(CompraRecepcion r) {
        String sql = """
            INSERT INTO compra_recepcion (
                idCompra,
                nroGuiaRemision,
                fechaRecepcion,
                idSede,
                idUsuario,
                estado,
                observacion
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setInt(1, r.getIdCompra());
            ps.setString(2, r.getNroGuiaRemision());
            ps.setDate(3, Date.valueOf(r.getFechaRecepcion()));
            ps.setInt(4, r.getIdSede());
            ps.setInt(5, r.getIdUsuario());
            ps.setString(6, r.getEstado());
            ps.setString(7, r.getObservacion());

            return ps;
        }, keyHolder);

        return keyHolder.getKey().intValue();
    }

    @Override
    public void registrarDetalle(CompraRecepcionDetalle d) {
        String sql = """
            INSERT INTO compra_recepcion_detalle (
                idRecepcion,
                idDetalleCompra,
                idProducto,
                idUnidad,
                cantidadRecibida
            )
            VALUES (?, ?, ?, ?, ?)
        """;

        jdbcTemplate.update(
                sql,
                d.getIdRecepcion(),
                d.getIdDetalleCompra(),
                d.getIdProducto(),
                d.getIdUnidad(),
                d.getCantidadRecibida()
        );
    }

    @Override
    public List<CompraRecepcion> listarPorCompra(Integer idCompra) {
        String sql = """
            SELECT 
                r.idRecepcion,
                r.idCompra,
                r.nroGuiaRemision,
                r.fechaRecepcion,
                r.idSede,
                r.idUsuario,
                r.estado,
                r.observacion,
                r.fechaRegistro,
                p.NombreProveedor AS proveedor,
                CONCAT(c.TipoComprobante, ' ', c.Serie, '-', c.Numero) AS comprobante,
                s.nombre AS sedeNombre
            FROM compra_recepcion r
            INNER JOIN compras c ON r.idCompra = c.idCompra
            INNER JOIN proveedores p ON c.idProveedor = p.idProveedor
            INNER JOIN sedes s ON r.idSede = s.idSede
            WHERE r.idCompra = ?
            ORDER BY r.fechaRecepcion DESC, r.idRecepcion DESC
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRecepcion(rs), idCompra);
    }

    @Override
    public List<CompraRecepcionDetalle> listarDetallesPorRecepcion(Integer idRecepcion) {
        String sql = """
            SELECT 
                rd.idRecepcionDetalle,
                rd.idRecepcion,
                rd.idDetalleCompra,
                rd.idProducto,
                rd.idUnidad,
                rd.cantidadRecibida,
                p.NombreProducto AS nombreProducto,
                u.NombreUnidad AS nombreUnidad
            FROM compra_recepcion_detalle rd
            INNER JOIN productos p ON rd.idProducto = p.idProducto
            INNER JOIN unidades u ON rd.idUnidad = u.idUnidad
            WHERE rd.idRecepcion = ?
            ORDER BY rd.idRecepcionDetalle ASC
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> mapDetalleBasico(rs), idRecepcion);
    }

    @Override
    public List<CompraRecepcionDetalle> listarPendientesPorCompra(Integer idCompra) {
        String sql = """
            SELECT 
                dc.idDetalleCompra,
                dc.idProducto,
                dc.idUnidad,
                dc.Cantidad AS cantidadComprada,
                p.NombreProducto AS nombreProducto,
                u.NombreUnidad AS nombreUnidad,

                COALESCE(SUM(rd.cantidadRecibida), 0) AS cantidadYaRecibida,

                GREATEST(
                    dc.Cantidad - COALESCE(SUM(rd.cantidadRecibida), 0),
                    0
                ) AS cantidadPendiente

            FROM detallecompra dc
            INNER JOIN productos p ON dc.idProducto = p.idProducto
            INNER JOIN unidades u ON dc.idUnidad = u.idUnidad
            LEFT JOIN compra_recepcion_detalle rd 
                   ON rd.idDetalleCompra = dc.idDetalleCompra

            WHERE dc.idCompra = ?

            GROUP BY 
                dc.idDetalleCompra,
                dc.idProducto,
                dc.idUnidad,
                dc.Cantidad,
                p.NombreProducto,
                u.NombreUnidad

            HAVING cantidadPendiente > 0

            ORDER BY dc.idDetalleCompra ASC
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            CompraRecepcionDetalle d = new CompraRecepcionDetalle();

            d.setIdDetalleCompra(rs.getInt("idDetalleCompra"));
            d.setIdProducto(rs.getInt("idProducto"));
            d.setIdUnidad(rs.getInt("idUnidad"));
            d.setCantidadComprada(rs.getBigDecimal("cantidadComprada"));
            d.setCantidadYaRecibida(rs.getBigDecimal("cantidadYaRecibida"));
            d.setCantidadPendiente(rs.getBigDecimal("cantidadPendiente"));
            d.setNombreProducto(rs.getString("nombreProducto"));
            d.setNombreUnidad(rs.getString("nombreUnidad"));

            return d;
        }, idCompra);
    }

    @Override
    public boolean existeGuiaEnCompra(Integer idCompra, String nroGuiaRemision) {
        String sql = """
            SELECT COUNT(*)
            FROM compra_recepcion
            WHERE idCompra = ?
              AND UPPER(nroGuiaRemision) = UPPER(?)
        """;

        Integer total = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                idCompra,
                nroGuiaRemision
        );

        return total != null && total > 0;
    }

    @Override
    public boolean compraEstaCompleta(Integer idCompra) {
        String sql = """
            SELECT COUNT(*)
            FROM (
                SELECT 
                    dc.idDetalleCompra,
                    dc.Cantidad AS cantidadComprada,
                    COALESCE(SUM(rd.cantidadRecibida), 0) AS cantidadRecibida
                FROM detallecompra dc
                LEFT JOIN compra_recepcion_detalle rd
                       ON rd.idDetalleCompra = dc.idDetalleCompra
                WHERE dc.idCompra = ?
                GROUP BY dc.idDetalleCompra, dc.Cantidad
                HAVING cantidadRecibida < cantidadComprada
            ) pendientes
        """;

        Integer pendientes = jdbcTemplate.queryForObject(sql, Integer.class, idCompra);

        return pendientes != null && pendientes == 0;
    }

    @Override
    public void actualizarEstadoCompra(Integer idCompra, String estado) {
        String sql = "UPDATE compras SET Estado = ? WHERE idCompra = ?";
        jdbcTemplate.update(sql, estado, idCompra);
    }

    @Override
    public void registrarCierreFaltante(
            Integer idCompra,
            Integer idUsuario,
            String motivo,
            String tipoSustento,
            String serieDocumento,
            String numeroDocumento,
            LocalDate fechaDocumento,
            BigDecimal montoAjustado,
            BigDecimal valorFaltante,
            BigDecimal deudaAntes,
            BigDecimal deudaDespues,
            BigDecimal saldoFavor,
            String estadoDevolucion,
            BigDecimal montoDevolucionPendiente,
            LocalDate fechaDevolucion,
            String observacion
    ) {
        String sql = """
        INSERT INTO compra_cierre_faltante (
            idCompra,
            idUsuario,
            motivo,
            tipoSustento,
            serieDocumento,
            numeroDocumento,
            fechaDocumento,
            montoAjustado,
            valorFaltante,
            deudaAntes,
            deudaDespues,
            saldoFavor,
            estadoDevolucion,
            montoDevolucionPendiente,
            fechaDevolucion,
            observacion
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """;

        jdbcTemplate.update(
                sql,
                idCompra,
                idUsuario,
                motivo,
                tipoSustento,
                serieDocumento,
                numeroDocumento,
                fechaDocumento != null ? Date.valueOf(fechaDocumento) : null,
                montoAjustado != null ? montoAjustado : BigDecimal.ZERO,
                valorFaltante != null ? valorFaltante : BigDecimal.ZERO,
                deudaAntes != null ? deudaAntes : BigDecimal.ZERO,
                deudaDespues != null ? deudaDespues : BigDecimal.ZERO,
                saldoFavor != null ? saldoFavor : BigDecimal.ZERO,
                estadoDevolucion != null ? estadoDevolucion : "NO_APLICA",
                montoDevolucionPendiente != null ? montoDevolucionPendiente : BigDecimal.ZERO,
                fechaDevolucion != null ? Date.valueOf(fechaDevolucion) : null,
                observacion
        );
    }

    @Override
    public boolean confirmarDevolucionProveedor(
            Integer idCompra,
            LocalDate fechaDevolucion,
            String observacionDevolucion
    ) {
        String sql = """
        UPDATE compra_cierre_faltante
        SET estadoDevolucion = 'DEVUELTO_AJUSTADO',
            montoDevolucionPendiente = 0.00,
            fechaDevolucion = ?,
            observacion = CONCAT(
                COALESCE(observacion, ''),
                CASE 
                    WHEN observacion IS NULL OR observacion = '' THEN ''
                    ELSE ' | '
                END,
                ?
            )
        WHERE idCierreFaltante = (
            SELECT idCierreFaltante
            FROM (
                SELECT idCierreFaltante
                FROM compra_cierre_faltante
                WHERE idCompra = ?
                  AND estadoDevolucion = 'PENDIENTE_DEVOLUCION'
                  AND montoDevolucionPendiente > 0
                ORDER BY idCierreFaltante DESC
                LIMIT 1
            ) tmp
        )
    """;

        String observacionFinal = observacionDevolucion != null
                ? observacionDevolucion.trim()
                : "";

        int filas = jdbcTemplate.update(
                sql,
                fechaDevolucion != null ? Date.valueOf(fechaDevolucion) : null,
                !observacionFinal.isBlank()
                        ? "DEVOLUCIÓN CONFIRMADA: " + observacionFinal
                        : "DEVOLUCIÓN CONFIRMADA.",
                idCompra
        );
        return filas > 0;
    }

    private CompraRecepcion mapRecepcion(ResultSet rs) throws SQLException {
        CompraRecepcion r = new CompraRecepcion();

        r.setIdRecepcion(rs.getInt("idRecepcion"));
        r.setIdCompra(rs.getInt("idCompra"));
        r.setNroGuiaRemision(rs.getString("nroGuiaRemision"));

        Date fecha = rs.getDate("fechaRecepcion");
        if (fecha != null) {
            r.setFechaRecepcion(fecha.toLocalDate());
        }

        r.setIdSede(rs.getInt("idSede"));
        r.setIdUsuario(rs.getInt("idUsuario"));
        r.setEstado(rs.getString("estado"));
        r.setObservacion(rs.getString("observacion"));

        Timestamp registro = rs.getTimestamp("fechaRegistro");
        if (registro != null) {
            r.setFechaRegistro(registro.toLocalDateTime());
        }

        try { r.setProveedor(rs.getString("proveedor")); } catch (SQLException ignored) {}
        try { r.setComprobante(rs.getString("comprobante")); } catch (SQLException ignored) {}
        try { r.setSedeNombre(rs.getString("sedeNombre")); } catch (SQLException ignored) {}

        return r;
    }

    private CompraRecepcionDetalle mapDetalleBasico(ResultSet rs) throws SQLException {
        CompraRecepcionDetalle d = new CompraRecepcionDetalle();

        d.setIdRecepcionDetalle(rs.getInt("idRecepcionDetalle"));
        d.setIdRecepcion(rs.getInt("idRecepcion"));
        d.setIdDetalleCompra(rs.getInt("idDetalleCompra"));
        d.setIdProducto(rs.getInt("idProducto"));
        d.setIdUnidad(rs.getInt("idUnidad"));
        d.setCantidadRecibida(rs.getBigDecimal("cantidadRecibida"));

        try { d.setNombreProducto(rs.getString("nombreProducto")); } catch (SQLException ignored) {}
        try { d.setNombreUnidad(rs.getString("nombreUnidad")); } catch (SQLException ignored) {}

        return d;
    }
}
