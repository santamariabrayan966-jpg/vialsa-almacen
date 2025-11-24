package com.vialsa.almacen.dao.Jdbc;

import com.vialsa.almacen.dao.interfaces.IProductoDao;
import com.vialsa.almacen.model.Producto;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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

    // ✅ Mapper actualizado a los nombres correctos de tu base de datos
    private static class ProductoMapper implements RowMapper<Producto> {
        @Override
        public Producto mapRow(ResultSet rs, int rowNum) throws SQLException {
            Producto p = new Producto();
            p.setIdProducto(rs.getInt("idProducto"));
            p.setCodigoInterno(rs.getString("codigoInterno"));
            p.setNombreProducto(rs.getString("nombreProducto"));
            p.setDimensiones(rs.getString("dimensiones"));
            p.setPrecioUnitario(rs.getBigDecimal("precioUnitario"));
            p.setStockActual(rs.getObject("stockActual") == null ? 0 : rs.getInt("stockActual"));
            p.setStockMinimo(rs.getObject("stockMinimo") == null ? 0 : rs.getInt("stockMinimo"));
            p.setIdUnidad(rs.getInt("idUnidad"));
            p.setIdTipoProducto(rs.getInt("idTipoProducto"));
            p.setIdEstadoProducto(rs.getInt("idEstadoProducto"));
            return p;
        }
    }

    @Override
    public List<Producto> listar() {
        String sql = """
            SELECT idProducto, codigoInterno, nombreProducto, dimensiones,
                   precioUnitario, stockActual, stockMinimo, idUnidad, idTipoProducto, idEstadoProducto
            FROM productos
            ORDER BY idProducto DESC
        """;
        return jdbc.query(sql, new ProductoMapper());
    }
    @Override
    public List<Producto> listarActivos() {
        String sql = """
        SELECT idProducto, codigoInterno, nombreProducto, dimensiones,
               precioUnitario, stockActual, stockMinimo, idUnidad, idTipoProducto, idEstadoProducto
        FROM productos
        WHERE idEstadoProducto = 1       -- 👈 asumimos 1 = ACTIVO
          AND stockActual > 0            -- 👈 solo con stock
        ORDER BY idProducto DESC
    """;
        return jdbc.query(sql, new ProductoMapper());
    }


    @Override
    public Optional<Producto> buscarPorId(Integer id) {
        String sql = """
            SELECT idProducto, codigoInterno, nombreProducto, dimensiones,
                   precioUnitario, stockActual, stockMinimo, idUnidad, idTipoProducto, idEstadoProducto
            FROM productos WHERE idProducto = ?
        """;
        List<Producto> list = jdbc.query(sql, new ProductoMapper(), id);
        return list.stream().findFirst();
    }

    @Override
    public int crear(Producto p) {
        String sql = """
            INSERT INTO productos (codigoInterno, nombreProducto, dimensiones, precioUnitario, stockActual, stockMinimo,
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
               SET codigoInterno=?, nombreProducto=?, dimensiones=?, precioUnitario=?, stockActual=?, stockMinimo=?,
                   idUnidad=?, idTipoProducto=?, idEstadoProducto=?
             WHERE idProducto=?
        """;
        return jdbc.update(sql,
                p.getCodigoInterno(), p.getNombreProducto(), p.getDimensiones(),
                p.getPrecioUnitario(), p.getStockActual(), p.getStockMinimo(),
                p.getIdUnidad(), p.getIdTipoProducto(), p.getIdEstadoProducto(),
                p.getIdProducto());
    }

    // ✅ Método corregido para usar el nombre real de la columna (stockActual)
    @Override
    public void descontarStock(int idProducto, BigDecimal cantidad) {
        String sql = "UPDATE productos SET stockActual = stockActual - ? WHERE idProducto = ?";
        jdbc.update(sql, cantidad, idProducto);
    }

    @Override
    public int eliminar(Integer id) {
        return jdbc.update("DELETE FROM productos WHERE idProducto=?", id);
    }

    @Override
    public void aumentarStock(int idProducto, BigDecimal cantidad) {
        String sql = "UPDATE productos SET StockActual = StockActual + ? WHERE idProducto = ?";
        jdbc.update(sql, cantidad, idProducto);
    }


}
