package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.ICompraDao;
import com.dentia.model.interno.Compra;
import com.dentia.model.interno.DetalleCompra;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;


@Repository
public class JdbcCompraDao implements ICompraDao {

    private final JdbcTemplate jdbcTemplate;

    public JdbcCompraDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ============================================================
    // LISTAR COMPRAS (CORREGIDO)
    // ============================================================
    @Override
    public List<Compra> listar() {

        String sql = """
        SELECT 
            c.idCompra,
            c.FechaCompra,
            c.NroComprobante,
            c.TipoComprobante,
            c.Serie,
            c.Numero,
            c.FechaEmision,
            c.FechaVencimiento,
            c.PeriodoMes,
                    c.PeriodoAnio,
            c.Moneda,
            c.TipoCambio,
            c.IncluyeIGV,
            c.PorcentajeIGV,
            c.Subtotal,
            c.MontoIGV,
           c.TotalCompra,
                   c.FormaPago,
                   c.TipoRecepcion,
                   c.PlazoDias,
            c.NumeroCuotas,
            c.NroOrdenCompra,
            c.NroGuiaRemision,
            c.Estado,
            c.Observaciones,
           p.NombreProveedor AS nombreProveedor,
                   u.NombreUsuario  AS nombreUsuario,
                   s.nombre AS nombreSede,

            -- DEUDA REAL = solo cuotas pendientes
            COALESCE(SUM(CASE WHEN cc.estado = 'PENDIENTE' THEN cc.montoCuota ELSE 0 END), 0) AS deuda,

            -- TOTAL PAGADO = cuotas con estado PAGADA
            COALESCE(SUM(CASE WHEN cc.estado = 'PAGADA' THEN cc.montoCuota ELSE 0 END), 0) AS totalPagado,

          -- TOTAL DE CUOTAS
                  COALESCE(COUNT(cc.idCuotaCompra), 0) AS cantidadCuotas,
                
                  -- ÚLTIMO CIERRE CON DIFERENCIA
                  MAX(COALESCE(cf.valorFaltante, 0)) AS valorFaltanteCierre,
                  MAX(COALESCE(cf.montoAjustado, 0)) AS montoAjustadoFaltante,
                  MAX(COALESCE(cf.saldoFavor, 0)) AS saldoFavor,
                  MAX(cf.estadoDevolucion) AS estadoDevolucion,
                  MAX(COALESCE(cf.montoDevolucionPendiente, 0)) AS montoDevolucionPendiente,
                  MAX(cf.fechaDevolucion) AS fechaDevolucion

        FROM compras c
                JOIN proveedores p ON c.idProveedor = p.idProveedor
                JOIN usuarios   u ON c.idUsuario   = u.idUsuario
                LEFT JOIN sedes s ON c.idSede = s.idSede
                LEFT JOIN cuotas_compra cc ON cc.idCompra = c.idCompra
                LEFT JOIN (
                            SELECT cf1.*
                            FROM compra_cierre_faltante cf1
                            INNER JOIN (
                                SELECT idCompra, MAX(idCierreFaltante) AS ultimoCierre
                                FROM compra_cierre_faltante
                                GROUP BY idCompra
                            ) ult
                                ON ult.ultimoCierre = cf1.idCierreFaltante
                        ) cf ON cf.idCompra = c.idCompra

        GROUP BY c.idCompra
        ORDER BY c.FechaCompra DESC
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Compra c = mapRowToCompra(rs);

            c.setDeuda(rs.getBigDecimal("deuda"));
            c.setTotalPagado(rs.getBigDecimal("totalPagado"));
            c.setNumeroCuotas(rs.getInt("cantidadCuotas"));
            c.setValorFaltante(rs.getBigDecimal("valorFaltanteCierre"));
            c.setMontoAjustadoFaltante(rs.getBigDecimal("montoAjustadoFaltante"));
            c.setSaldoFavor(rs.getBigDecimal("saldoFavor"));
            c.setEstadoDevolucion(rs.getString("estadoDevolucion"));
            c.setMontoDevolucionPendiente(rs.getBigDecimal("montoDevolucionPendiente"));

            Date fechaDev = rs.getDate("fechaDevolucion");
            if (fechaDev != null) {
                c.setFechaDevolucion(fechaDev.toLocalDate());
            }

            return c;
        });
    }

    // ============================================================
// LISTAR COMPRAS POR SEDE ACTIVA
// ============================================================
    @Override
    public List<Compra> listarPorSede(Integer idSede) {

        String sql = """
    SELECT 
        c.idCompra,
        c.FechaCompra,
        c.NroComprobante,
        c.TipoComprobante,
        c.Serie,
        c.Numero,
        c.FechaEmision,
        c.FechaVencimiento,
        c.PeriodoMes,
                    c.PeriodoAnio,
        c.idProveedor,
        c.idUsuario,
        c.idSede,
        c.Moneda,
        c.TipoCambio,
        c.IncluyeIGV,
        c.PorcentajeIGV,
        c.Subtotal,
        c.MontoIGV,
       c.TotalCompra,
                   c.FormaPago,
                   c.TipoRecepcion,
                   c.PlazoDias,
        c.NumeroCuotas,
        c.NroOrdenCompra,
        c.NroGuiaRemision,
        c.Estado,
        c.Observaciones,
       p.NombreProveedor AS nombreProveedor,
                   u.NombreUsuario  AS nombreUsuario,
                   s.nombre AS nombreSede,

       COALESCE(SUM(CASE WHEN cc.estado = 'PENDIENTE' THEN cc.montoCuota ELSE 0 END), 0) AS deuda,
                   COALESCE(SUM(CASE WHEN cc.estado = 'PAGADA' THEN cc.montoCuota ELSE 0 END), 0) AS totalPagado,
                   COALESCE(COUNT(cc.idCuotaCompra), 0) AS cantidadCuotas,
                
                   MAX(COALESCE(cf.valorFaltante, 0)) AS valorFaltanteCierre,
                   MAX(COALESCE(cf.montoAjustado, 0)) AS montoAjustadoFaltante,
                   MAX(COALESCE(cf.saldoFavor, 0)) AS saldoFavor,
                   MAX(cf.estadoDevolucion) AS estadoDevolucion,
                   MAX(COALESCE(cf.montoDevolucionPendiente, 0)) AS montoDevolucionPendiente,
                   MAX(cf.fechaDevolucion) AS fechaDevolucion

    FROM compras c
                JOIN proveedores p ON c.idProveedor = p.idProveedor
                JOIN usuarios   u ON c.idUsuario   = u.idUsuario
                LEFT JOIN sedes s ON c.idSede = s.idSede
                LEFT JOIN cuotas_compra cc ON cc.idCompra = c.idCompra
                LEFT JOIN (
                                SELECT cf1.*
                                FROM compra_cierre_faltante cf1
                                INNER JOIN (
                                    SELECT idCompra, MAX(idCierreFaltante) AS ultimoCierre
                                    FROM compra_cierre_faltante
                                    GROUP BY idCompra
                                ) ult
                                    ON ult.ultimoCierre = cf1.idCierreFaltante
                            ) cf ON cf.idCompra = c.idCompra

    WHERE c.idSede = ?

    GROUP BY c.idCompra
    ORDER BY c.FechaCompra DESC
    """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Compra c = mapRowToCompra(rs);

            c.setDeuda(rs.getBigDecimal("deuda"));
            c.setTotalPagado(rs.getBigDecimal("totalPagado"));
            c.setNumeroCuotas(rs.getInt("cantidadCuotas"));
            c.setValorFaltante(rs.getBigDecimal("valorFaltanteCierre"));
            c.setMontoAjustadoFaltante(rs.getBigDecimal("montoAjustadoFaltante"));
            c.setSaldoFavor(rs.getBigDecimal("saldoFavor"));
            c.setEstadoDevolucion(rs.getString("estadoDevolucion"));
            c.setMontoDevolucionPendiente(rs.getBigDecimal("montoDevolucionPendiente"));

            Date fechaDev = rs.getDate("fechaDevolucion");
            if (fechaDev != null) {
                c.setFechaDevolucion(fechaDev.toLocalDate());
            }

            return c;
        }, idSede);
    }

    // ============================================================
    // REGISTRAR
    // ============================================================
    @Override
    public int registrarYObtenerId(Compra c) {

        String nuevoNroOrden = generarNuevoNumeroOrden();
        c.setNroOrdenCompra(nuevoNroOrden);

        String sql = """
INSERT INTO compras (
    FechaCompra,
    NroComprobante,
    TipoComprobante,
    Serie,
    Numero,
    FechaEmision,
    FechaVencimiento,
    PeriodoMes,
    PeriodoAnio,
    idProveedor,
    idUsuario,
    idSede,
    Moneda,
    TipoCambio,
    IncluyeIGV,
    PorcentajeIGV,
    Subtotal,
    MontoIGV,
    TotalCompra,
    FormaPago,
    TipoRecepcion,
    PlazoDias,
    NumeroCuotas,
    NroOrdenCompra,
    NroGuiaRemision,
    Estado,
    Observaciones
)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
""";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setTimestamp(1, Timestamp.valueOf(c.getFechaCompra()));
            ps.setString(2, c.getNroComprobante());
            ps.setString(3, c.getTipoComprobante());
            ps.setString(4, c.getSerie());
            ps.setString(5, c.getNumero());
            ps.setTimestamp(6, c.getFechaEmision() != null ?
                    Timestamp.valueOf(c.getFechaEmision().atStartOfDay()) : null);

            if (c.getFechaVencimiento() != null) {
                ps.setTimestamp(7, Timestamp.valueOf(c.getFechaVencimiento().atStartOfDay()));
            } else {
                ps.setNull(7, Types.TIMESTAMP);
            }

            ps.setObject(8, c.getPeriodoMes(), Types.INTEGER);
            ps.setObject(9, c.getPeriodoAnio(), Types.INTEGER);

            ps.setInt(10, c.getIdProveedor());
            ps.setInt(11, c.getIdUsuario());

            if (c.getIdSede() != null) {
                ps.setInt(12, c.getIdSede());
            } else {
                ps.setNull(12, Types.INTEGER);
            }

            ps.setString(13, c.getMoneda());
            ps.setBigDecimal(14, c.getTipoCambio());
            ps.setBoolean(15, Boolean.TRUE.equals(c.isIncluyeIgv()));
            ps.setBigDecimal(16, c.getPorcentajeIgv());
            ps.setBigDecimal(17, c.getSubtotal());
            ps.setBigDecimal(18, c.getMontoIgv());
            ps.setBigDecimal(19, c.getTotalCompra());
            ps.setString(20, c.getFormaPago());
            ps.setString(21, c.getTipoRecepcion() != null ? c.getTipoRecepcion() : "PENDIENTE_GUIA");
            ps.setObject(22, c.getPlazoDias(), Types.INTEGER);
            ps.setObject(23, c.getNumeroCuotas(), Types.INTEGER);
            ps.setString(24, nuevoNroOrden);
            ps.setString(25, c.getNroGuiaRemision());
            ps.setString(26, c.getEstado());
            ps.setString(27, c.getObservaciones());

            return ps;
        }, keyHolder);

        return keyHolder.getKey().intValue();
    }

    // ============================================================
    // BUSCAR POR ID
    // ============================================================
    @Override
    public Compra buscarPorId(int idCompra) {
        String sql = """
        SELECT 
            c.idCompra,
            c.FechaCompra,
            c.NroComprobante,
            c.TipoComprobante,
            c.Serie,
            c.Numero,
            c.FechaEmision,
            c.FechaVencimiento,
            c.PeriodoMes,
            c.PeriodoAnio,
            c.idProveedor,
            c.idUsuario,
            c.idSede,
            c.Moneda,
            c.TipoCambio,
            c.IncluyeIGV,
            c.PorcentajeIGV,
            c.Subtotal,
            c.MontoIGV,
            c.TotalCompra,
            c.FormaPago,
            c.TipoRecepcion,
            c.PlazoDias,
            c.NumeroCuotas,
            c.NroOrdenCompra,
            c.NroGuiaRemision,
            c.Estado,
            c.Observaciones,

            p.NombreProveedor AS nombreProveedor,
            u.NombreUsuario AS nombreUsuario,
            s.nombre AS nombreSede,

            -- ÚLTIMO CIERRE CON DIFERENCIA
            COALESCE(cf.valorFaltante, 0) AS valorFaltanteCierre,
            COALESCE(cf.montoAjustado, 0) AS montoAjustadoFaltante,
            COALESCE(cf.saldoFavor, 0) AS saldoFavor,
            cf.estadoDevolucion AS estadoDevolucion,
            COALESCE(cf.montoDevolucionPendiente, 0) AS montoDevolucionPendiente,
            cf.fechaDevolucion AS fechaDevolucion

        FROM compras c
        LEFT JOIN proveedores p ON c.idProveedor = p.idProveedor
        LEFT JOIN usuarios u ON c.idUsuario = u.idUsuario
        LEFT JOIN sedes s ON c.idSede = s.idSede

        LEFT JOIN (
            SELECT cf1.*
            FROM compra_cierre_faltante cf1
            INNER JOIN (
                SELECT idCompra, MAX(idCierreFaltante) AS ultimoCierre
                FROM compra_cierre_faltante
                GROUP BY idCompra
            ) ult
                ON ult.ultimoCierre = cf1.idCierreFaltante
        ) cf ON cf.idCompra = c.idCompra

        WHERE c.idCompra = ?
    """;

        List<Compra> compras = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> mapRowToCompra(rs),
                idCompra
        );

        return compras.isEmpty() ? null : compras.get(0);
    }

    // ============================================================
    // DETALLE COMPRA
    // ============================================================
    @Override
    public List<DetalleCompra> listarPorCompra(int idCompra) {
        String sql = """
            SELECT 
                d.idDetalleCompra,
                d.idProducto,
                d.idUnidad,
                d.Cantidad,
                d.PrecioUnitario,
                d.Descuento,
                p.NombreProducto,
                u.NombreUnidad
            FROM detallecompra d
            JOIN productos p ON d.idProducto = p.idProducto
            JOIN unidades u ON d.idUnidad   = u.idUnidad
            WHERE d.idCompra = ?
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            DetalleCompra d = new DetalleCompra();
            d.setIdDetalleCompra(rs.getInt("idDetalleCompra"));
            d.setIdProducto(rs.getInt("idProducto"));
            d.setIdUnidad(rs.getInt("idUnidad"));
            d.setCantidad(rs.getBigDecimal("Cantidad"));
            d.setPrecioUnitario(rs.getBigDecimal("PrecioUnitario"));
            d.setDescuento(rs.getBigDecimal("Descuento"));
            d.setNombreProducto(rs.getString("NombreProducto"));
            d.setNombreUnidad(rs.getString("NombreUnidad"));
            return d;
        }, idCompra);
    }

    // ============================================================
    // ACTUALIZAR ESTADO
    // ============================================================
    @Override
    public int actualizarEstado(int idCompra, String estado) {
        String sql = "UPDATE compras SET Estado = ? WHERE idCompra = ?";
        return jdbcTemplate.update(sql, estado, idCompra);
    }

    // ============================================================
    // ACTUALIZAR CABECERA
    // ============================================================
    @Override
    public int actualizarCabecera(Compra c) {
        String sql = """
        UPDATE compras
           SET NroComprobante  = ?,
               TipoComprobante = ?,
               Serie           = ?,
               Numero          = ?,
               FechaEmision    = ?,
               FechaVencimiento= ?,
               PeriodoMes      = ?,
               PeriodoAnio     = ?,
               idProveedor     = ?,
               idSede          = ?,
               Moneda          = ?,
               TipoCambio      = ?,
               IncluyeIGV      = ?,
               PorcentajeIGV   = ?,
               Subtotal        = ?,
               MontoIGV        = ?,
            TotalCompra     = ?,
                        FormaPago       = ?,
                        TipoRecepcion   = ?,
                        PlazoDias       = ?,
               NumeroCuotas    = ?,
               NroOrdenCompra  = ?,
               NroGuiaRemision = ?,
               Estado          = ?,
               Observaciones   = ?
         WHERE idCompra        = ?
    """;

        return jdbcTemplate.update(sql,
                c.getNroComprobante(),
                c.getTipoComprobante(),
                c.getSerie(),
                c.getNumero(),
                c.getFechaEmision() != null ? java.sql.Date.valueOf(c.getFechaEmision()) : null,
                c.getFechaVencimiento() != null ? java.sql.Date.valueOf(c.getFechaVencimiento()) : null,
                c.getPeriodoMes(),
                c.getPeriodoAnio(),
                c.getIdProveedor(),
                c.getIdSede(),
                c.getMoneda(),
                c.getTipoCambio(),
                Boolean.TRUE.equals(c.isIncluyeIgv()),
                c.getPorcentajeIgv(),
                c.getSubtotal(),
                c.getMontoIgv(),
                c.getTotalCompra(),
                c.getFormaPago(),
                c.getTipoRecepcion() != null ? c.getTipoRecepcion() : "PENDIENTE_GUIA",
                c.getPlazoDias(),
                c.getNumeroCuotas(),
                c.getNroOrdenCompra(),
                c.getNroGuiaRemision(),
                c.getEstado(),
                c.getObservaciones(),
                c.getIdCompra()
        );
    }
    // ============================================================
    // ELIMINAR SI ES BORRADOR
    // ============================================================
    @Override
    public int eliminarSiBorrador(int idCompra) {
        try {
            String sqlEstado = "SELECT Estado FROM compras WHERE idCompra = ?";
            String estado = jdbcTemplate.queryForObject(sqlEstado, String.class, idCompra);

            if (estado == null || !"BORRADOR".equalsIgnoreCase(estado)) {
                return 0;
            }

            jdbcTemplate.update("DELETE FROM detallecompra WHERE idCompra = ?", idCompra);
            return jdbcTemplate.update("DELETE FROM compras WHERE idCompra = ?", idCompra);

        } catch (Exception e) {
            return 0;
        }
    }

    // ============================================================
    // MAPEO DE COMPRA
    // ============================================================
    private Compra mapRowToCompra(ResultSet rs) throws SQLException {
        Compra c = new Compra();

        c.setIdCompra(rs.getInt("idCompra"));

        Timestamp fCompra = rs.getTimestamp("FechaCompra");
        if (fCompra != null) c.setFechaCompra(fCompra.toLocalDateTime());

        try { c.setNroComprobante(rs.getString("NroComprobante")); } catch (SQLException ignored) {}
        try { c.setTipoComprobante(rs.getString("TipoComprobante")); } catch (SQLException ignored) {}
        try { c.setSerie(rs.getString("Serie")); } catch (SQLException ignored) {}
        try { c.setNumero(rs.getString("Numero")); } catch (SQLException ignored) {}

        Timestamp fEmi = rs.getTimestamp("FechaEmision");
        if (fEmi != null) c.setFechaEmision(fEmi.toLocalDateTime().toLocalDate());


        Timestamp fVen = rs.getTimestamp("FechaVencimiento");
        if (fVen != null) c.setFechaVencimiento(fVen.toLocalDateTime().toLocalDate());
        try { c.setPeriodoMes((Integer) rs.getObject("PeriodoMes")); } catch (SQLException ignored) {}
        try { c.setPeriodoAnio((Integer) rs.getObject("PeriodoAnio")); } catch (SQLException ignored) {}


        try { c.setIdProveedor(rs.getInt("idProveedor")); } catch (SQLException ignored) {}
        try { c.setIdUsuario(rs.getInt("idUsuario")); } catch (SQLException ignored) {}
        try { c.setIdSede((Integer) rs.getObject("idSede")); } catch (SQLException ignored) {}
        try { c.setMoneda(rs.getString("Moneda")); } catch (SQLException ignored) {}
        try { c.setTipoCambio(rs.getBigDecimal("TipoCambio")); } catch (SQLException ignored) {}
        try { c.setIncluyeIgv(rs.getBoolean("IncluyeIGV")); } catch (SQLException ignored) {}
        try { c.setPorcentajeIgv(rs.getBigDecimal("PorcentajeIGV")); } catch (SQLException ignored) {}
        try { c.setSubtotal(rs.getBigDecimal("Subtotal")); } catch (SQLException ignored) {}
        try { c.setMontoIgv(rs.getBigDecimal("MontoIGV")); } catch (SQLException ignored) {}
        try { c.setTotalCompra(rs.getBigDecimal("TotalCompra")); } catch (SQLException ignored) {}

        try { c.setFormaPago(rs.getString("FormaPago")); } catch (SQLException ignored) {}

        try {
            String tipoRecepcion = rs.getString("TipoRecepcion");
            c.setTipoRecepcion(tipoRecepcion != null ? tipoRecepcion : "PENDIENTE_GUIA");
        } catch (SQLException ignored) {
            c.setTipoRecepcion("PENDIENTE_GUIA");
        }

        try { c.setPlazoDias((Integer) rs.getObject("PlazoDias")); } catch (SQLException ignored) {}
        try { c.setNumeroCuotas((Integer) rs.getObject("NumeroCuotas")); } catch (SQLException ignored) {}
        try { c.setNroOrdenCompra(rs.getString("NroOrdenCompra")); } catch (SQLException ignored) {}
        try { c.setNroGuiaRemision(rs.getString("NroGuiaRemision")); } catch (SQLException ignored) {}
        try { c.setEstado(rs.getString("Estado")); } catch (SQLException ignored) {}
        try { c.setObservaciones(rs.getString("Observaciones")); } catch (SQLException ignored) {}

        try { c.setNombreProveedor(rs.getString("nombreProveedor")); } catch (SQLException ignored) {}
        try { c.setNombreUsuario(rs.getString("nombreUsuario")); } catch (SQLException ignored) {}
        try { c.setNombreSede(rs.getString("nombreSede")); } catch (SQLException ignored) {}


        try { c.setValorFaltante(rs.getBigDecimal("valorFaltanteCierre")); } catch (SQLException ignored) {}
        try { c.setMontoAjustadoFaltante(rs.getBigDecimal("montoAjustadoFaltante")); } catch (SQLException ignored) {}
        try { c.setSaldoFavor(rs.getBigDecimal("saldoFavor")); } catch (SQLException ignored) {}
        try { c.setEstadoDevolucion(rs.getString("estadoDevolucion")); } catch (SQLException ignored) {}
        try { c.setMontoDevolucionPendiente(rs.getBigDecimal("montoDevolucionPendiente")); } catch (SQLException ignored) {}

        try {
            Date fechaDev = rs.getDate("fechaDevolucion");
            if (fechaDev != null) {
                c.setFechaDevolucion(fechaDev.toLocalDate());
            }
        } catch (SQLException ignored) {}

        return c;
    }

    // ============================================================
    // ÚLTIMO NÚMERO ORDEN
    // ============================================================
    @Override
    public String obtenerUltimoNumeroOrden() {
        String sql = """
        SELECT nroOrdenCompra 
        FROM compras
        WHERE nroOrdenCompra IS NOT NULL
        ORDER BY CAST(SUBSTRING(nroOrdenCompra, 4) AS UNSIGNED) DESC
        LIMIT 1
        """;

        try {
            return jdbcTemplate.queryForObject(sql, String.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String generarNuevoNumeroOrden() {

        String ultimo = obtenerUltimoNumeroOrden();

        if (ultimo == null || ultimo.isBlank()) {
            return "OC-0001";
        }

        try {
            int num = Integer.parseInt(ultimo.replace("OC-", ""));
            num++;
            return "OC-" + String.format("%04d", num);

        } catch (Exception e) {
            return "OC-0001";
        }
    }

    // ============================================================
    // ACTUALIZAR DEUDA
    // ============================================================
    @Override
    public int actualizarDeuda(int idCompra, BigDecimal nuevaDeuda) {
        String sql = "UPDATE compras SET deuda = ? WHERE idCompra = ?";
        return jdbcTemplate.update(sql, nuevaDeuda, idCompra);
    }

    @Override
    public Integer obtenerIdUsuarioPorNombre(String nombreUsuario) {
        String sql = "SELECT idUsuario FROM usuarios WHERE NombreUsuario = ? LIMIT 1";
        try {
            return jdbcTemplate.queryForObject(sql, Integer.class, nombreUsuario);
        } catch (Exception e) {
            return null;
        }
    }
    // ============================================================
// OBTENER ÚLTIMA SERIE POR TIPO
// ============================================================
    @Override
    public String obtenerUltimaSeriePorTipo(String tipo) {

        String sql = """
        SELECT Serie 
        FROM compras
        WHERE TipoComprobante = ?
        AND Serie IS NOT NULL
        ORDER BY Serie DESC
        LIMIT 1
    """;

        try {
            return jdbcTemplate.queryForObject(sql, String.class, tipo);
        } catch (Exception e) {
            return null;
        }
    }

    // ============================================================
// OBTENER ÚLTIMO NÚMERO POR TIPO
// ============================================================
    @Override
    public String obtenerUltimoNumeroPorTipo(String tipo) {

        String sql = """
        SELECT Numero
        FROM compras
        WHERE TipoComprobante = ?
        AND Numero IS NOT NULL
        ORDER BY CAST(Numero AS UNSIGNED) DESC
        LIMIT 1
    """;

        try {
            return jdbcTemplate.queryForObject(sql, String.class, tipo);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void actualizarCostoPromedioProducto(
            int idProducto,
            BigDecimal cantidadComprada,
            BigDecimal costoUnitarioCompraPen
    ) {
        String sql = """
        UPDATE productos
        SET costoPromedio = CASE
                WHEN COALESCE(StockActual, 0) <= 0 THEN ?
                ELSE (
                    (COALESCE(StockActual, 0) * COALESCE(costoPromedio, 0))
                    + (? * ?)
                ) / (COALESCE(StockActual, 0) + ?)
            END,
            StockActual = COALESCE(StockActual, 0) + ?
        WHERE idProducto = ?
    """;

        jdbcTemplate.update(
                sql,
                costoUnitarioCompraPen,
                cantidadComprada,
                costoUnitarioCompraPen,
                cantidadComprada,
                cantidadComprada,
                idProducto
        );
    }

    @Override
    public boolean existeComprobanteProveedor(
            Integer idProveedor,
            String tipoComprobante,
            String serie,
            String numero,
            Integer idCompraExcluir
    ) {
        String sql = """
        SELECT COUNT(*)
        FROM compras
        WHERE idProveedor = ?
          AND UPPER(TipoComprobante) = UPPER(?)
          AND UPPER(Serie) = UPPER(?)
          AND Numero = ?
          AND (? IS NULL OR idCompra <> ?)
    """;

        Integer total = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                idProveedor,
                tipoComprobante,
                serie,
                numero,
                idCompraExcluir,
                idCompraExcluir
        );

        return total != null && total > 0;
    }


}
