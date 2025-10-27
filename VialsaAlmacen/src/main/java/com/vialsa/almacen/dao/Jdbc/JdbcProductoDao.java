package com.vialsa.almacen.dao.Jdbc;

import com.vialsa.almacen.dao.interfaces.IProductoDao;
import com.vialsa.almacen.model.Producto;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcProductoDao implements IProductoDao {

    private final JdbcTemplate jdbc;

    public JdbcProductoDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static class ProductoMapper implements RowMapper<Producto> {
        @Override
        public Producto mapRow(ResultSet rs, int rowNum) throws SQLException {
            Producto p = new Producto();
            p.setIdProducto(rs.getInt("idProducto"));
            p.setCodigoInterno(rs.getString("CodigoInterno"));
            p.setNombreProducto(rs.getString("NombreProducto"));
            p.setDimensiones(rs.getString("Dimensiones"));
            p.setPrecioUnitario(rs.getBigDecimal("PrecioUnitario"));
            p.setStockActual(rs.getObject("StockActual") == null ? 0 : rs.getInt("StockActual"));
            p.setStockMinimo((Integer) rs.getObject("StockMinimo"));
            p.setIdUnidad((Integer) rs.getObject("idUnidad"));
            p.setIdTipoProducto((Integer) rs.getObject("idTipoProducto"));
            p.setIdEstadoProducto((Integer) rs.getObject("idEstadoProducto"));
            return p;
        }
    }

    @Override
    public List<Producto> listar() {
        String sql = """
            SELECT idProducto, CodigoInterno, NombreProducto, Dimensiones,
                   PrecioUnitario, StockActual, StockMinimo, idUnidad, idTipoProducto, idEstadoProducto
            FROM productos
            ORDER BY idProducto DESC
        """;
        return jdbc.query(sql, new ProductoMapper());
    }

    @Override
    public Optional<Producto> buscarPorId(Integer id) {
        String sql = """
            SELECT idProducto, CodigoInterno, NombreProducto, Dimensiones,
                   PrecioUnitario, StockActual, StockMinimo, idUnidad, idTipoProducto, idEstadoProducto
            FROM productos WHERE idProducto = ?
        """;
        List<Producto> list = jdbc.query(sql, new ProductoMapper(), id);
        return list.stream().findFirst();
    }

    @Override
    public int crear(Producto p) {
        String sql = """
            INSERT INTO productos (CodigoInterno, NombreProducto, Dimensiones, PrecioUnitario, StockActual, StockMinimo,
                                   idUnidad, idTipoProducto, idEstadoProducto)
            VALUES (?,?,?,?,?,?,?,?,?)
        """;
        return jdbc.update(sql,
                p.getCodigoInterno(), p.getNombreProducto(), p.getDimensiones(),
                p.getPrecioUnitario(), p.getStockActual(), p.getStockMinimo(),
                p.getIdUnidad(), p.getIdTipoProducto(), p.getIdEstadoProducto());
    }

    @Override
    public int actualizar(Producto p) {
        String sql = """
            UPDATE productos
               SET CodigoInterno=?, NombreProducto=?, Dimensiones=?, PrecioUnitario=?, StockActual=?, StockMinimo=?,
                   idUnidad=?, idTipoProducto=?, idEstadoProducto=?
             WHERE idProducto=?
        """;
        return jdbc.update(sql,
                p.getCodigoInterno(), p.getNombreProducto(), p.getDimensiones(),
                p.getPrecioUnitario(), p.getStockActual(), p.getStockMinimo(),
                p.getIdUnidad(), p.getIdTipoProducto(), p.getIdEstadoProducto(),
                p.getIdProducto());
    }

    @Override
    public int eliminar(Integer id) {
        return jdbc.update("DELETE FROM productos WHERE idProducto=?", id);
    }
}
