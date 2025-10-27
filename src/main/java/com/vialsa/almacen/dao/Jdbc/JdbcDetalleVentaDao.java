package com.vialsa.almacen.dao.Jdbc;

import com.vialsa.almacen.dao.interfaces.IDetalleVentaDao;
import com.vialsa.almacen.model.DetalleVenta;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcDetalleVentaDao implements IDetalleVentaDao {

    private final JdbcTemplate jdbc;

    public JdbcDetalleVentaDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // 💾 Registrar detalle de venta
    @Override
    public int registrar(DetalleVenta d) {
        String sql = """
            INSERT INTO detalleventa (idVentas, idProducto, idUnidad, Cantidad, PrecioUnitario, Descuento)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        return jdbc.update(sql,
                d.getIdVenta(),
                d.getIdProducto(),
                d.getIdUnidad(),
                d.getCantidad(),
                d.getPrecioUnitario(),
                d.getDescuento());
    }

    // 📋 Listar los detalles de una venta
    @Override
    public List<DetalleVenta> listarPorVenta(int idVenta) {
        String sql = """
            SELECT dv.idDetalleVenta, dv.idVentas, dv.idProducto, dv.idUnidad,
                   dv.Cantidad, dv.PrecioUnitario, dv.Descuento,
                   p.NombreProducto, u.NombreUnidad
            FROM detalleventa dv
            INNER JOIN productos p ON dv.idProducto = p.idProducto
            INNER JOIN unidades u ON dv.idUnidad = u.idUnidad
            WHERE dv.idVentas = ?
        """;

        return jdbc.query(sql, (rs, rowNum) -> {
            DetalleVenta d = new DetalleVenta();
            d.setIdDetalleVenta(rs.getInt("idDetalleVenta"));
            d.setIdVenta(rs.getInt("idVentas"));
            d.setIdProducto(rs.getInt("idProducto"));
            d.setIdUnidad(rs.getInt("idUnidad"));
            d.setCantidad(rs.getBigDecimal("Cantidad"));
            d.setPrecioUnitario(rs.getBigDecimal("PrecioUnitario"));
            d.setDescuento(rs.getBigDecimal("Descuento"));
            d.setNombreProducto(rs.getString("NombreProducto"));
            d.setNombreUnidad(rs.getString("NombreUnidad"));
            return d;
        }, idVenta);
    }

    // ❌ Eliminar todos los detalles de una venta
    @Override
    public int eliminarPorVenta(int idVenta) {
        String sql = "DELETE FROM detalleventa WHERE idVentas = ?";
        return jdbc.update(sql, idVenta);
    }
}
