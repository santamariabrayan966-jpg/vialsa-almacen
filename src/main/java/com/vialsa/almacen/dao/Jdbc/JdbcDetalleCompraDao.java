package com.vialsa.almacen.dao.Jdbc;

import com.vialsa.almacen.dao.interfaces.IDetalleCompraDao;
import com.vialsa.almacen.model.DetalleCompra;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository // 👈 Esto es lo que hace que Spring la reconozca
public class JdbcDetalleCompraDao implements IDetalleCompraDao {

    private final JdbcTemplate jdbc;

    public JdbcDetalleCompraDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public int registrar(DetalleCompra detalle) {
        String sql = """
            INSERT INTO detallecompra (idCompra, idProducto, idUnidad, Cantidad, PrecioUnitario, Descuento)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        return jdbc.update(sql,
                detalle.getIdCompra(),
                detalle.getIdProducto(),
                detalle.getIdUnidad(),
                detalle.getCantidad(),
                detalle.getPrecioUnitario(),
                detalle.getDescuento());
    }

    @Override
    public List<DetalleCompra> listarPorCompra(int idCompra) {
        String sql = """
            SELECT dc.idDetalleCompra, dc.idCompra, dc.idProducto, dc.idUnidad,
                   dc.Cantidad, dc.PrecioUnitario, dc.Descuento,
                   p.NombreProducto, u.NombreUnidad
            FROM detallecompra dc
            INNER JOIN productos p ON dc.idProducto = p.idProducto
            INNER JOIN unidad u ON dc.idUnidad = u.idUnidad
            WHERE dc.idCompra = ?
        """;
        return jdbc.query(sql, (ResultSet rs, int rowNum) -> {
            DetalleCompra d = new DetalleCompra();
            d.setIdDetalleCompra(rs.getInt("idDetalleCompra"));
            d.setIdCompra(rs.getInt("idCompra"));
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
    public boolean eliminarPorCompra(int idCompra) {
        String sql = "DELETE FROM detallecompra WHERE idCompra = ?";
        return jdbc.update(sql, idCompra) > 0;
    }
}
