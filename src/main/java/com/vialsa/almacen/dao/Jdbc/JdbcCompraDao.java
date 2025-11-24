package com.vialsa.almacen.dao.Jdbc;

import com.vialsa.almacen.dao.interfaces.ICompraDao;
import com.vialsa.almacen.model.Compra;
import com.vialsa.almacen.model.DetalleCompra;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;


import java.sql.*;
import java.util.List;

@Repository
public class JdbcCompraDao implements ICompraDao {

    private final JdbcTemplate jdbcTemplate;

    public JdbcCompraDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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
                c.Moneda,
                c.TipoCambio,
                c.IncluyeIGV,
                c.PorcentajeIGV,
                c.Subtotal,
                c.MontoIGV,
                c.TotalCompra,
                c.FormaPago,
                c.PlazoDias,
                c.NumeroCuotas,
                c.NroOrdenCompra,
                c.NroGuiaRemision,
                c.Estado,
                c.Observaciones,
                p.NombreProveedor AS nombreProveedor,
                u.NombreUsuario  AS nombreUsuario
            FROM compras c
            JOIN proveedores p ON c.idProveedor = p.idProveedor
            JOIN usuarios   u ON c.idUsuario   = u.idUsuario
            ORDER BY c.FechaCompra DESC
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRowToCompra(rs));
    }

    @Override
    public int registrarYObtenerId(Compra c) {
        String sql = """
            INSERT INTO compras (
                FechaCompra,
                NroComprobante,
                TipoComprobante,
                Serie,
                Numero,
                FechaEmision,
                FechaVencimiento,
                idProveedor,
                idUsuario,
                Moneda,
                TipoCambio,
                IncluyeIGV,
                PorcentajeIGV,
                Subtotal,
                MontoIGV,
                TotalCompra,
                FormaPago,
                PlazoDias,
                NumeroCuotas,
                NroOrdenCompra,
                NroGuiaRemision,
                Estado,
                Observaciones
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setTimestamp(1, Timestamp.valueOf(c.getFechaCompra()));
            ps.setString(2, c.getNroComprobante());
            ps.setString(3, c.getTipoComprobante());
            ps.setString(4, c.getSerie());
            ps.setString(5, c.getNumero());
            ps.setTimestamp(6, Timestamp.valueOf(c.getFechaEmision()));
            if (c.getFechaVencimiento() != null) {
                ps.setTimestamp(7, Timestamp.valueOf(c.getFechaVencimiento()));
            } else {
                ps.setNull(7, Types.TIMESTAMP);
            }

            ps.setInt(8, c.getIdProveedor());
            ps.setInt(9, c.getIdUsuario());

            ps.setString(10, c.getMoneda());
            if (c.getTipoCambio() != null) {
                ps.setBigDecimal(11, c.getTipoCambio());
            } else {
                ps.setNull(11, Types.DECIMAL);
            }

            ps.setBoolean(12, c.isIncluyeIgv());
            // Porcentaje IGV nunca debe ir null a la BD
            if (c.getPorcentajeIgv() != null) {
                ps.setBigDecimal(13, c.getPorcentajeIgv());
            } else {
                // aquí decides el default: 0 o 18, tú mandas
                ps.setBigDecimal(13, java.math.BigDecimal.ZERO);
                // Si quieres que sea 18% por defecto:
                // ps.setBigDecimal(13, new java.math.BigDecimal("18.00"));
            }

            ps.setBigDecimal(14, c.getSubtotal());
            ps.setBigDecimal(15, c.getMontoIgv());
            ps.setBigDecimal(16, c.getTotalCompra());

            ps.setString(17, c.getFormaPago());
            if (c.getPlazoDias() != null) {
                ps.setInt(18, c.getPlazoDias());
            } else {
                ps.setNull(18, Types.INTEGER);
            }
            if (c.getNumeroCuotas() != null) {
                ps.setInt(19, c.getNumeroCuotas());
            } else {
                ps.setNull(19, Types.INTEGER);
            }

            ps.setString(20, c.getNroOrdenCompra());
            ps.setString(21, c.getNroGuiaRemision());
            ps.setString(22, c.getEstado());
            ps.setString(23, c.getObservaciones());

            return ps;
        }, keyHolder);

        return keyHolder.getKey().intValue();
    }

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
                c.idProveedor,
                c.idUsuario,
                c.Moneda,
                c.TipoCambio,
                c.IncluyeIGV,
                c.PorcentajeIGV,
                c.Subtotal,
                c.MontoIGV,
                c.TotalCompra,
                c.FormaPago,
                c.PlazoDias,
                c.NumeroCuotas,
                c.NroOrdenCompra,
                c.NroGuiaRemision,
                c.Estado,
                c.Observaciones,
                p.NombreProveedor AS nombreProveedor,
                u.NombreUsuario AS nombreUsuario
            FROM compras c
            LEFT JOIN proveedores p ON c.idProveedor = p.idProveedor
            LEFT JOIN usuarios u   ON c.idUsuario  = u.idUsuario
            WHERE c.idCompra = ?
        """;

        List<Compra> compras = jdbcTemplate.query(sql, (rs, rowNum) -> mapRowToCompra(rs), idCompra);
        return compras.isEmpty() ? null : compras.get(0);
    }

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

    @Override
    public Integer obtenerIdUsuarioPorNombre(String nombreUsuario) {
        String sql = "SELECT idUsuario FROM usuarios WHERE NombreUsuario = ? LIMIT 1";
        try {
            return jdbcTemplate.queryForObject(sql, Integer.class, nombreUsuario);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public int actualizarEstado(int idCompra, String estado) {
        String sql = "UPDATE compras SET Estado = ? WHERE idCompra = ?";
        return jdbcTemplate.update(sql, estado, idCompra);
    }

    // 🔹 Actualizar SOLO cabecera
    @Override
    public int actualizarCabecera(Compra c) {
        String sql = """
            UPDATE compras
               SET FormaPago      = ?,
                   PlazoDias      = ?,
                   NumeroCuotas   = ?,
                   NroOrdenCompra = ?,
                   NroGuiaRemision= ?,
                   Observaciones  = ?
             WHERE idCompra       = ?
        """;

        return jdbcTemplate.update(sql,
                c.getFormaPago(),
                c.getPlazoDias(),
                c.getNumeroCuotas(),
                c.getNroOrdenCompra(),
                c.getNroGuiaRemision(),
                c.getObservaciones(),
                c.getIdCompra()
        );
    }

    // 🗑️ Eliminar solo si está en BORRADOR
    @Override
    public int eliminarSiBorrador(int idCompra) {
        try {
            String sqlEstado = "SELECT Estado FROM compras WHERE idCompra = ?";
            String estado = jdbcTemplate.queryForObject(sqlEstado, String.class, idCompra);

            if (estado == null || !"BORRADOR".equalsIgnoreCase(estado)) {
                return 0; // no es borrador, no se elimina
            }

            // Primero elimina los detalles
            jdbcTemplate.update("DELETE FROM detallecompra WHERE idCompra = ?", idCompra);
            // Luego la cabecera
            return jdbcTemplate.update("DELETE FROM compras WHERE idCompra = ?", idCompra);

        } catch (Exception e) {
            // si no existe o cualquier error
            return 0;
        }
    }


    // ---------- Mapeador común ----------
    private Compra mapRowToCompra(ResultSet rs) throws SQLException {
        Compra c = new Compra();

        c.setIdCompra(rs.getInt("idCompra"));

        Timestamp fCompra = rs.getTimestamp("FechaCompra");
        if (fCompra != null) {
            c.setFechaCompra(fCompra.toLocalDateTime());
        }

        try { c.setNroComprobante(rs.getString("NroComprobante")); } catch (SQLException ignored) {}
        try { c.setTipoComprobante(rs.getString("TipoComprobante")); } catch (SQLException ignored) {}
        try { c.setSerie(rs.getString("Serie")); } catch (SQLException ignored) {}
        try { c.setNumero(rs.getString("Numero")); } catch (SQLException ignored) {}

        try {
            Timestamp fEmi = rs.getTimestamp("FechaEmision");
            if (fEmi != null) c.setFechaEmision(fEmi.toLocalDateTime());
        } catch (SQLException ignored) {}

        try {
            Timestamp fVen = rs.getTimestamp("FechaVencimiento");
            if (fVen != null) c.setFechaVencimiento(fVen.toLocalDateTime());
        } catch (SQLException ignored) {}

        try { c.setIdProveedor(rs.getInt("idProveedor")); } catch (SQLException ignored) {}
        try { c.setIdUsuario(rs.getInt("idUsuario")); } catch (SQLException ignored) {}

        try { c.setMoneda(rs.getString("Moneda")); } catch (SQLException ignored) {}
        try { c.setTipoCambio(rs.getBigDecimal("TipoCambio")); } catch (SQLException ignored) {}
        try { c.setIncluyeIgv(rs.getBoolean("IncluyeIGV")); } catch (SQLException ignored) {}
        try { c.setPorcentajeIgv(rs.getBigDecimal("PorcentajeIGV")); } catch (SQLException ignored) {}
        try { c.setSubtotal(rs.getBigDecimal("Subtotal")); } catch (SQLException ignored) {}
        try { c.setMontoIgv(rs.getBigDecimal("MontoIGV")); } catch (SQLException ignored) {}
        try { c.setTotalCompra(rs.getBigDecimal("TotalCompra")); } catch (SQLException ignored) {}

        try { c.setFormaPago(rs.getString("FormaPago")); } catch (SQLException ignored) {}
        try { c.setPlazoDias((Integer) rs.getObject("PlazoDias")); } catch (SQLException ignored) {}
        try { c.setNumeroCuotas((Integer) rs.getObject("NumeroCuotas")); } catch (SQLException ignored) {}

        try { c.setNroOrdenCompra(rs.getString("NroOrdenCompra")); } catch (SQLException ignored) {}
        try { c.setNroGuiaRemision(rs.getString("NroGuiaRemision")); } catch (SQLException ignored) {}

        try { c.setEstado(rs.getString("Estado")); } catch (SQLException ignored) {}
        try { c.setObservaciones(rs.getString("Observaciones")); } catch (SQLException ignored) {}

        try { c.setNombreProveedor(rs.getString("nombreProveedor")); } catch (SQLException ignored) {}
        try { c.setNombreUsuario(rs.getString("nombreUsuario")); } catch (SQLException ignored) {}

        return c;
    }

}
