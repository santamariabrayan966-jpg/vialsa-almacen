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

    // =========================
    //   MAPPER
    // =========================
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

            p.setFoto(rs.getString("foto"));

            // ✅ Nuevo campo
            p.setDescuentoMaximo(rs.getBigDecimal("descuentoMaximo"));

            return p;
        }
    }

    // =========================
    //   CRUD BÁSICO
    // =========================

    @Override
    public List<Producto> listar() {
        String sql = """
            SELECT idProducto, codigoInterno, nombreProducto, dimensiones,
                   precioUnitario, stockActual, stockMinimo,
                   idUnidad, idTipoProducto, idEstadoProducto,
                   foto, descuentoMaximo
            FROM productos
            ORDER BY idProducto DESC
        """;

        return jdbc.query(sql, new ProductoMapper());
    }

    @Override
    public List<Producto> listarActivos() {
        String sql = """
            SELECT idProducto, codigoInterno, nombreProducto, dimensiones,
                   precioUnitario, stockActual, stockMinimo,
                   idUnidad, idTipoProducto, idEstadoProducto,
                   foto, descuentoMaximo
            FROM productos
            WHERE idEstadoProducto = 1
              AND stockActual > 0
            ORDER BY idProducto DESC
        """;

        return jdbc.query(sql, new ProductoMapper());
    }

    @Override
    public Optional<Producto> buscarPorId(Integer id) {
        String sql = """
            SELECT idProducto, codigoInterno, nombreProducto, dimensiones,
                   precioUnitario, stockActual, stockMinimo,
                   idUnidad, idTipoProducto, idEstadoProducto,
                   foto, descuentoMaximo
            FROM productos
            WHERE idProducto = ?
        """;

        List<Producto> list = jdbc.query(sql, new ProductoMapper(), id);
        return list.stream().findFirst();
    }

    // =========================
    //   CREAR PRODUCTO
    // =========================
    @Override
    public int crear(Producto p) {
        String sql = """
            INSERT INTO productos (
                codigoInterno, nombreProducto, dimensiones,
                precioUnitario, stockActual, stockMinimo,
                idUnidad, idTipoProducto, idEstadoProducto,
                foto, descuentoMaximo
            )
            VALUES (?,?,?,?,?,?,?,?,?,?,?)
        """;

        return jdbc.update(sql,
                p.getCodigoInterno(),
                p.getNombreProducto(),
                p.getDimensiones(),
                p.getPrecioUnitario(),
                p.getStockActual(),
                p.getStockMinimo(),
                p.getIdUnidad(),
                p.getIdTipoProducto(),
                p.getIdEstadoProducto(),
                p.getFoto(),
                p.getDescuentoMaximo()   // ← 🔥 AHORA SE GUARDA
        );
    }

    // =========================
    //   ACTUALIZAR PRODUCTO
    // =========================
    @Override
    public int actualizar(Producto p) {
        String sql = """
            UPDATE productos
               SET codigoInterno   = ?,
                   nombreProducto  = ?,
                   dimensiones     = ?,
                   precioUnitario  = ?,
                   stockActual     = ?,
                   stockMinimo     = ?,
                   idUnidad        = ?,
                   idTipoProducto  = ?,
                   idEstadoProducto= ?,
                   foto            = ?,
                   descuentoMaximo = ?
             WHERE idProducto      = ?
        """;

        return jdbc.update(sql,
                p.getCodigoInterno(),
                p.getNombreProducto(),
                p.getDimensiones(),
                p.getPrecioUnitario(),
                p.getStockActual(),
                p.getStockMinimo(),
                p.getIdUnidad(),
                p.getIdTipoProducto(),
                p.getIdEstadoProducto(),
                p.getFoto(),
                p.getDescuentoMaximo(), // ← 🔥 AHORA SE ACTUALIZA
                p.getIdProducto()
        );
    }

    @Override
    public int eliminar(Integer id) {
        return jdbc.update("DELETE FROM productos WHERE idProducto = ?", id);
    }

    // =========================
    //   STOCK
    // =========================
    @Override
    public void aumentarStock(int idProducto, BigDecimal cantidad) {
        String sql = """
            UPDATE productos
               SET stockActual = COALESCE(stockActual, 0) + ?
             WHERE idProducto = ?
        """;
        jdbc.update(sql, cantidad, idProducto);
    }

    @Override
    public void descontarStock(int idProducto, BigDecimal cantidad) {
        String sql = """
            UPDATE productos
               SET stockActual = GREATEST(COALESCE(stockActual, 0) - ?, 0)
             WHERE idProducto = ?
        """;
        jdbc.update(sql, cantidad, idProducto);
    }

    // =========================
    //   BÚSQUEDA
    // =========================
    @Override
    public List<Producto> buscar(String filtro) {
        String sql = """
            SELECT idProducto, codigoInterno, nombreProducto, dimensiones,
                   precioUnitario, stockActual, stockMinimo,
                   idUnidad, idTipoProducto, idEstadoProducto,
                   foto, descuentoMaximo
            FROM productos
            WHERE nombreProducto LIKE ?
               OR codigoInterno LIKE ?
               OR dimensiones LIKE ?
            ORDER BY idProducto DESC
        """;

        String like = "%" + filtro + "%";
        return jdbc.query(sql, new ProductoMapper(), like, like, like);
    }

    // =========================
    //   CÓDIGO INTERNO
    // =========================
    private String obtenerPrefijoTipo(int idTipoProducto) {
        return switch (idTipoProducto) {
            case 1 -> "ALU";
            case 2 -> "VID";
            case 3 -> "ACC";
            default -> "PRD";
        };
    }

    @Override
    public String generarCodigoInterno(int idTipoProducto) {
        String prefijo = obtenerPrefijoTipo(idTipoProducto);

        String sql = """
            SELECT codigoInterno
            FROM productos
            WHERE codigoInterno LIKE ?
            ORDER BY codigoInterno DESC
            LIMIT 1
        """;

        String like = prefijo + "-%";

        List<String> result = jdbc.query(sql,
                (rs, rowNum) -> rs.getString("codigoInterno"),
                like
        );

        if (result.isEmpty()) {
            return prefijo + "-001";
        }

        String ultimo = result.get(0);
        int numero = Integer.parseInt(ultimo.split("-")[1]);

        return String.format("%s-%03d", prefijo, numero + 1);
    }

    // =========================
    //   CAMBIAR ESTADO
    // =========================
    @Override
    public int cambiarEstado(int idProducto, int nuevoEstado) {
        String sql =
                "UPDATE productos SET idEstadoProducto = ? WHERE idProducto = ?";
        return jdbc.update(sql, nuevoEstado, idProducto);
    }

    // =========================
    //   ÚLTIMO PRECIO DE COMPRA
    // =========================
    @Override
    public BigDecimal obtenerUltimoPrecioCompra(int idProducto) {
        String sql = """
            SELECT dc.PrecioUnitario, c.Moneda, c.TipoCambio
            FROM detallecompra dc
            INNER JOIN compras c ON c.idCompra = dc.idCompra
            WHERE dc.idProducto = ?
            ORDER BY dc.idDetalleCompra DESC
            LIMIT 1
        """;

        return jdbc.query(sql, rs -> {
            if (!rs.next()) return null;

            BigDecimal precio = rs.getBigDecimal("PrecioUnitario");
            String moneda = rs.getString("Moneda");
            BigDecimal tipoCambio = rs.getBigDecimal("TipoCambio");

            if (precio == null) return null;

            if ("USD".equalsIgnoreCase(moneda) && tipoCambio != null) {
                return precio.multiply(tipoCambio);
            }

            return precio;
        }, idProducto);
    }

    @Override
    public List<Producto> buscarPorTexto(String texto) {
        String sql = """
            SELECT idProducto, CodigoInterno, NombreProducto, foto,
                   Dimensiones, PrecioUnitario, StockActual, StockMinimo,
                   idUnidad, idTipoProducto, idEstadoProducto,
                   descuentoMaximo
            FROM productos
            WHERE NombreProducto LIKE ?
               OR CodigoInterno LIKE ?
            ORDER BY NombreProducto ASC
            LIMIT 15
        """;

        String filtro = "%" + texto + "%";
        return jdbc.query(sql, new ProductoMapper(), filtro, filtro);
    }
}
