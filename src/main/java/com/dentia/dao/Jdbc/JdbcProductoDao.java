package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.IProductoDao;
import com.dentia.model.interno.Producto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.Map;

@Repository
public class JdbcProductoDao implements IProductoDao {

    private final JdbcTemplate jdbc;

    public JdbcProductoDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ============================================================
    // SELECT BASE CON JOINS
    // ============================================================

    private static final String SELECT_PRODUCTOS_BASE = """
        SELECT
            p.idProducto,
            p.CodigoInterno              AS codigoInterno,
            p.NombreProducto             AS nombreProducto,
            p.foto,
            COALESCE(
                (
                    SELECT pf.nombreArchivo
                    FROM producto_foto pf
                    WHERE pf.idProducto = p.idProducto
                      AND pf.estado = 1
                    ORDER BY pf.portada DESC, pf.orden ASC, pf.idProductoFoto ASC
                    LIMIT 1
                ),
                p.foto
            ) AS fotoPortada,
            p.Dimensiones                AS dimensiones,
            p.PrecioUnitario             AS precioUnitario,
                    p.costoPromedio              AS costoPromedio,
                    p.margenMinimoPorcentaje     AS margenMinimoPorcentaje,
                    p.StockActual                AS stockActual,
                    p.StockMinimo                AS stockMinimo,
            p.idUnidad,
            p.controlInventario,
            p.nombreUnidadInventario,
            p.formaVenta,
            p.nombreUnidadVenta,
            p.abreviaturaUnidadVenta,
            p.idEstadoProducto,
            p.idMarca,
            p.idCategoria,
            p.idSubcategoria,
            p.modeloProducto,
            p.descripcionProducto,
            p.descuentoMaximo,

            p.ventaPorMedida,
            p.tipoMedidaBase,
            p.largoBase,
            p.anchoBase,
            p.medidaTotalBase,
            p.medidaVendibleBase,
            p.descripcionMedidaBase,

            m.nombreMarca,
            c.nombreCategoria,
            sc.nombreSubcategoria,

            u.NombreUnidad               AS nombreUnidad,
            u.abreviatura                AS abreviaturaUnidad,
            u.tipoCalculo                AS tipoCalculoUnidad

        FROM productos p
        LEFT JOIN marca_producto m
            ON m.idMarca = p.idMarca
        LEFT JOIN categoria_producto c
            ON c.idCategoria = p.idCategoria
        LEFT JOIN subcategoria_producto sc
            ON sc.idSubcategoria = p.idSubcategoria
        LEFT JOIN unidades u
            ON u.idUnidad = p.idUnidad
    """;

    // ============================================================
    // MAPPER
    // ============================================================

    private static class ProductoMapper implements RowMapper<Producto> {

        @Override
        public Producto mapRow(ResultSet rs, int rowNum) throws SQLException {

            Producto p = new Producto();

            p.setIdProducto(rs.getInt("idProducto"));
            p.setCodigoInterno(rs.getString("codigoInterno"));
            p.setNombreProducto(rs.getString("nombreProducto"));
            p.setFoto(rs.getString("foto"));
            p.setFotoPortada(rs.getString("fotoPortada"));
            p.setDimensiones(rs.getString("dimensiones"));
            p.setPrecioUnitario(rs.getBigDecimal("precioUnitario"));
            p.setCostoPromedio(rs.getBigDecimal("costoPromedio"));
            p.setMargenMinimoPorcentaje(rs.getBigDecimal("margenMinimoPorcentaje"));

            p.setStockActual(rs.getObject("stockActual") == null ? 0 : rs.getInt("stockActual"));
            p.setStockActualDecimal(rs.getObject("stockActual") == null ? BigDecimal.ZERO : rs.getBigDecimal("stockActual"));

            p.setStockMinimo(rs.getObject("stockMinimo") == null ? 0 : rs.getInt("stockMinimo"));

            p.setIdUnidad(rs.getObject("idUnidad") == null ? null : rs.getInt("idUnidad"));

            p.setControlInventario(rs.getString("controlInventario"));
            p.setNombreUnidadInventario(rs.getString("nombreUnidadInventario"));
            p.setFormaVenta(rs.getString("formaVenta"));
            p.setNombreUnidadVenta(rs.getString("nombreUnidadVenta"));
            p.setAbreviaturaUnidadVenta(rs.getString("abreviaturaUnidadVenta"));

            p.setIdEstadoProducto(rs.getObject("idEstadoProducto") == null ? null : rs.getInt("idEstadoProducto"));

            p.setIdMarca(rs.getObject("idMarca") == null ? null : rs.getInt("idMarca"));
            p.setIdCategoria(rs.getObject("idCategoria") == null ? null : rs.getInt("idCategoria"));
            p.setIdSubcategoria(rs.getObject("idSubcategoria") == null ? null : rs.getInt("idSubcategoria"));

            p.setModeloProducto(rs.getString("modeloProducto"));
            p.setDescripcionProducto(rs.getString("descripcionProducto"));
            p.setDescuentoMaximo(rs.getBigDecimal("descuentoMaximo"));

            p.setVentaPorMedida(rs.getObject("ventaPorMedida") != null && rs.getBoolean("ventaPorMedida"));
            p.setTipoMedidaBase(rs.getString("tipoMedidaBase"));
            p.setLargoBase(rs.getBigDecimal("largoBase"));
            p.setAnchoBase(rs.getBigDecimal("anchoBase"));
            p.setMedidaTotalBase(rs.getBigDecimal("medidaTotalBase"));
            p.setMedidaVendibleBase(rs.getObject("medidaVendibleBase") == null ? null : rs.getInt("medidaVendibleBase"));
            p.setDescripcionMedidaBase(rs.getString("descripcionMedidaBase"));

            p.setNombreMarca(rs.getString("nombreMarca"));
            p.setNombreCategoria(rs.getString("nombreCategoria"));
            p.setNombreSubcategoria(rs.getString("nombreSubcategoria"));

            /*
             * Campos antiguos provenientes de la tabla unidades.
             * Se mantienen temporalmente por compatibilidad con vistas y módulos antiguos.
             */
            p.setNombreUnidad(rs.getString("nombreUnidad"));
            p.setAbreviaturaUnidad(rs.getString("abreviaturaUnidad"));
            p.setTipoCalculoUnidad(rs.getString("tipoCalculoUnidad"));

            return p;
        }
    }

    // ============================================================
    // LISTAR
    // ============================================================

    @Override
    public List<Producto> listar() {
        String sql = SELECT_PRODUCTOS_BASE + """
            WHERE p.idEstadoProducto IN (1, 2)
            ORDER BY p.idProducto DESC
        """;

        return jdbc.query(sql, new ProductoMapper());
    }

    @Override
    public List<Producto> listarActivos() {
        String sql = SELECT_PRODUCTOS_BASE + """
            WHERE p.idEstadoProducto = 1
            ORDER BY p.NombreProducto ASC
        """;

        return jdbc.query(sql, new ProductoMapper());
    }

    @Override
    public List<Producto> listarEliminados() {
        String sql = SELECT_PRODUCTOS_BASE + """
            WHERE p.idEstadoProducto = 3
            ORDER BY p.idProducto DESC
        """;

        return jdbc.query(sql, new ProductoMapper());
    }

    // ============================================================
    // BUSCAR POR ID
    // ============================================================

    @Override
    public Optional<Producto> buscarPorId(Integer id) {
        String sql = SELECT_PRODUCTOS_BASE + """
            WHERE p.idProducto = ?
        """;

        List<Producto> lista = jdbc.query(sql, new ProductoMapper(), id);
        return lista.stream().findFirst();
    }

    // ============================================================
    // CREAR PRODUCTO
    // ============================================================

    @Override
    public int crear(Producto p) {
        String sql = """
            INSERT INTO productos (
                CodigoInterno,
                NombreProducto,
                foto,
                Dimensiones,
                PrecioUnitario,
                StockActual,
                StockMinimo,
                idUnidad,
                controlInventario,
                nombreUnidadInventario,
                formaVenta,
                nombreUnidadVenta,
                abreviaturaUnidadVenta,
                idEstadoProducto,
                idMarca,
                idCategoria,
                idSubcategoria,
                modeloProducto,
                descripcionProducto,
                descuentoMaximo,
                ventaPorMedida,
                tipoMedidaBase,
                largoBase,
                anchoBase,
                medidaTotalBase,
                medidaVendibleBase,
                descripcionMedidaBase
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        return jdbc.update(sql,
                p.getCodigoInterno(),
                p.getNombreProducto(),
                p.getFoto(),
                p.getDimensiones(),
                p.getPrecioUnitario(),
                p.getStockActual(),
                p.getStockMinimo(),
                p.getIdUnidad(),
                p.getControlInventario(),
                p.getNombreUnidadInventario(),
                p.getFormaVenta(),
                p.getNombreUnidadVenta(),
                p.getAbreviaturaUnidadVenta(),
                p.getIdEstadoProducto(),
                p.getIdMarca(),
                p.getIdCategoria(),
                p.getIdSubcategoria(),
                p.getModeloProducto(),
                p.getDescripcionProducto(),
                p.getDescuentoMaximo(),
                p.getVentaPorMedida() != null && p.getVentaPorMedida(),
                p.getTipoMedidaBase(),
                p.getLargoBase(),
                p.getAnchoBase(),
                p.getMedidaTotalBase(),
                p.getMedidaVendibleBase(),
                p.getDescripcionMedidaBase()
        );
    }

    @Override
    public int crearYRetornarId(Producto p) {

        String sql = """
            INSERT INTO productos (
                CodigoInterno,
                NombreProducto,
                foto,
                Dimensiones,
                PrecioUnitario,
                StockActual,
                StockMinimo,
                idUnidad,
                controlInventario,
                nombreUnidadInventario,
                formaVenta,
                nombreUnidadVenta,
                abreviaturaUnidadVenta,
                idEstadoProducto,
                idMarca,
                idCategoria,
                idSubcategoria,
                modeloProducto,
                descripcionProducto,
                descuentoMaximo,
                ventaPorMedida,
                tipoMedidaBase,
                largoBase,
                anchoBase,
                medidaTotalBase,
                medidaVendibleBase,
                descripcionMedidaBase
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, p.getCodigoInterno());
            ps.setString(2, p.getNombreProducto());
            ps.setString(3, p.getFoto());
            ps.setString(4, p.getDimensiones());
            ps.setBigDecimal(5, p.getPrecioUnitario());
            ps.setObject(6, p.getStockActual());
            ps.setObject(7, p.getStockMinimo());
            ps.setObject(8, p.getIdUnidad());

            ps.setString(9, p.getControlInventario());
            ps.setString(10, p.getNombreUnidadInventario());
            ps.setString(11, p.getFormaVenta());
            ps.setString(12, p.getNombreUnidadVenta());
            ps.setString(13, p.getAbreviaturaUnidadVenta());

            ps.setObject(14, p.getIdEstadoProducto());
            ps.setObject(15, p.getIdMarca());
            ps.setObject(16, p.getIdCategoria());
            ps.setObject(17, p.getIdSubcategoria());
            ps.setString(18, p.getModeloProducto());
            ps.setString(19, p.getDescripcionProducto());
            ps.setBigDecimal(20, p.getDescuentoMaximo());

            ps.setBoolean(21, p.getVentaPorMedida() != null && p.getVentaPorMedida());
            ps.setString(22, p.getTipoMedidaBase());
            ps.setBigDecimal(23, p.getLargoBase());
            ps.setBigDecimal(24, p.getAnchoBase());
            ps.setBigDecimal(25, p.getMedidaTotalBase());
            ps.setObject(26, p.getMedidaVendibleBase());
            ps.setString(27, p.getDescripcionMedidaBase());

            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();

        if (key == null) {
            throw new IllegalStateException("No se pudo obtener el ID del producto creado.");
        }

        return key.intValue();
    }

    // ============================================================
    // ACTUALIZAR PRODUCTO
    // ============================================================

    @Override
    public int actualizar(Producto p) {
        String sql = """
            UPDATE productos
               SET CodigoInterno             = ?,
                   NombreProducto            = ?,
                   foto                      = ?,
                   Dimensiones               = ?,
                   PrecioUnitario            = ?,
                   StockActual               = ?,
                   StockMinimo               = ?,
                   idUnidad                  = ?,
                   controlInventario         = ?,
                   nombreUnidadInventario    = ?,
                   formaVenta                = ?,
                   nombreUnidadVenta         = ?,
                   abreviaturaUnidadVenta    = ?,
                   idEstadoProducto          = ?,
                   idMarca                   = ?,
                   idCategoria               = ?,
                   idSubcategoria            = ?,
                   modeloProducto            = ?,
                   descripcionProducto       = ?,
                   descuentoMaximo           = ?,
                   ventaPorMedida            = ?,
                   tipoMedidaBase            = ?,
                   largoBase                 = ?,
                   anchoBase                 = ?,
                   medidaTotalBase           = ?,
                   medidaVendibleBase        = ?,
                   descripcionMedidaBase     = ?
             WHERE idProducto                = ?
        """;

        return jdbc.update(sql,
                p.getCodigoInterno(),
                p.getNombreProducto(),
                p.getFoto(),
                p.getDimensiones(),
                p.getPrecioUnitario(),
                p.getStockActual(),
                p.getStockMinimo(),
                p.getIdUnidad(),
                p.getControlInventario(),
                p.getNombreUnidadInventario(),
                p.getFormaVenta(),
                p.getNombreUnidadVenta(),
                p.getAbreviaturaUnidadVenta(),
                p.getIdEstadoProducto(),
                p.getIdMarca(),
                p.getIdCategoria(),
                p.getIdSubcategoria(),
                p.getModeloProducto(),
                p.getDescripcionProducto(),
                p.getDescuentoMaximo(),
                p.getVentaPorMedida() != null && p.getVentaPorMedida(),
                p.getTipoMedidaBase(),
                p.getLargoBase(),
                p.getAnchoBase(),
                p.getMedidaTotalBase(),
                p.getMedidaVendibleBase(),
                p.getDescripcionMedidaBase(),
                p.getIdProducto()
        );
    }

    // ============================================================
    // ELIMINAR
    // ============================================================

    @Override
    public int eliminar(Integer id) {
        String sql = """
            UPDATE productos
               SET idEstadoProducto = 3
             WHERE idProducto = ?
        """;

        return jdbc.update(sql, id);
    }

    // ============================================================
    // RESTAURAR
    // ============================================================

    @Override
    public int restaurar(Integer idProducto) {
        String sql = """
            UPDATE productos
               SET idEstadoProducto = 1
             WHERE idProducto = ?
        """;

        return jdbc.update(sql, idProducto);
    }

    // ============================================================
    // STOCK TEMPORAL
    // Luego esto debe pasar completamente a inventario_stock.
    // ============================================================

    @Override
    public void aumentarStock(int idProducto, BigDecimal cantidad) {
        String sql = """
            UPDATE productos
               SET StockActual = COALESCE(StockActual, 0) + ?
             WHERE idProducto = ?
        """;

        jdbc.update(sql, cantidad, idProducto);
    }

    @Override
    public void descontarStock(int idProducto, BigDecimal cantidad) {
        String sql = """
            UPDATE productos
               SET StockActual = GREATEST(COALESCE(StockActual, 0) - ?, 0)
             WHERE idProducto = ?
        """;

        jdbc.update(sql, cantidad, idProducto);
    }

    // ============================================================
    // BÚSQUEDA
    // ============================================================

    @Override
    public List<Producto> buscar(String filtro) {
        String sql = SELECT_PRODUCTOS_BASE + """
            WHERE p.idEstadoProducto IN (1, 2)
              AND (
                   p.NombreProducto LIKE ?
                OR p.CodigoInterno LIKE ?
                OR p.Dimensiones LIKE ?
                OR p.modeloProducto LIKE ?
                OR p.descripcionProducto LIKE ?
                OR p.nombreUnidadInventario LIKE ?
                OR p.nombreUnidadVenta LIKE ?
                OR p.formaVenta LIKE ?
                OR m.nombreMarca LIKE ?
                OR c.nombreCategoria LIKE ?
                OR sc.nombreSubcategoria LIKE ?
              )
            ORDER BY p.idProducto DESC
        """;

        String like = "%" + filtro + "%";

        return jdbc.query(sql, new ProductoMapper(),
                like, like, like, like, like, like, like, like, like, like, like
        );
    }

    @Override
    public List<Producto> listarActivosConStockPorSede(Integer idSede) {

        String sql = """
        SELECT
            p.idProducto,
            p.CodigoInterno              AS codigoInterno,
            p.NombreProducto             AS nombreProducto,
            p.foto,
            COALESCE(
                (
                    SELECT pf.nombreArchivo
                    FROM producto_foto pf
                    WHERE pf.idProducto = p.idProducto
                      AND pf.estado = 1
                    ORDER BY pf.portada DESC, pf.orden ASC, pf.idProductoFoto ASC
                    LIMIT 1
                ),
                p.foto
            ) AS fotoPortada,
            p.Dimensiones                AS dimensiones,
            p.PrecioUnitario             AS precioUnitario,
            p.costoPromedio              AS costoPromedio,
            p.margenMinimoPorcentaje     AS margenMinimoPorcentaje,

            COALESCE(st.stockActual, 0)  AS stockActual,
            COALESCE(st.stockMinimo, p.StockMinimo, 0) AS stockMinimo,

            p.idUnidad,
            p.controlInventario,
            p.nombreUnidadInventario,
            p.formaVenta,
            p.nombreUnidadVenta,
            p.abreviaturaUnidadVenta,
            p.idEstadoProducto,
            p.idMarca,
            p.idCategoria,
            p.idSubcategoria,
            p.modeloProducto,
            p.descripcionProducto,
            p.descuentoMaximo,

            p.ventaPorMedida,
            p.tipoMedidaBase,
            p.largoBase,
            p.anchoBase,
            p.medidaTotalBase,
            p.medidaVendibleBase,
            p.descripcionMedidaBase,

            m.nombreMarca,
            c.nombreCategoria,
            sc.nombreSubcategoria,

            u.NombreUnidad               AS nombreUnidad,
            u.abreviatura                AS abreviaturaUnidad,
            u.tipoCalculo                AS tipoCalculoUnidad

        FROM productos p
        LEFT JOIN inventario_stock st
            ON st.idProducto = p.idProducto
           AND st.idSede = ?
           AND st.activo = 1

        LEFT JOIN marca_producto m
            ON m.idMarca = p.idMarca
        LEFT JOIN categoria_producto c
            ON c.idCategoria = p.idCategoria
        LEFT JOIN subcategoria_producto sc
            ON sc.idSubcategoria = p.idSubcategoria
        LEFT JOIN unidades u
            ON u.idUnidad = p.idUnidad

        WHERE p.idEstadoProducto = 1
        ORDER BY p.NombreProducto ASC
    """;

        return jdbc.query(sql, new ProductoMapper(), idSede);
    }

    @Override
    public List<Producto> buscarVendiblesPorSede(String filtro, Integer idSede) {

        String sql = """
        SELECT
            p.idProducto,
            p.CodigoInterno              AS codigoInterno,
            p.NombreProducto             AS nombreProducto,
            p.foto,
            COALESCE(
                (
                    SELECT pf.nombreArchivo
                    FROM producto_foto pf
                    WHERE pf.idProducto = p.idProducto
                      AND pf.estado = 1
                    ORDER BY pf.portada DESC, pf.orden ASC, pf.idProductoFoto ASC
                    LIMIT 1
                ),
                p.foto
            ) AS fotoPortada,
            p.Dimensiones                AS dimensiones,
            p.PrecioUnitario             AS precioUnitario,
            p.costoPromedio              AS costoPromedio,
            p.margenMinimoPorcentaje     AS margenMinimoPorcentaje,

            st.stockActual               AS stockActual,
            COALESCE(st.stockMinimo, p.StockMinimo, 0) AS stockMinimo,

            p.idUnidad,
            p.controlInventario,
            p.nombreUnidadInventario,
            p.formaVenta,
            p.nombreUnidadVenta,
            p.abreviaturaUnidadVenta,
            p.idEstadoProducto,
            p.idMarca,
            p.idCategoria,
            p.idSubcategoria,
            p.modeloProducto,
            p.descripcionProducto,
            p.descuentoMaximo,

            p.ventaPorMedida,
            p.tipoMedidaBase,
            p.largoBase,
            p.anchoBase,
            p.medidaTotalBase,
            p.medidaVendibleBase,
            p.descripcionMedidaBase,

            m.nombreMarca,
            c.nombreCategoria,
            sc.nombreSubcategoria,

            u.NombreUnidad               AS nombreUnidad,
            u.abreviatura                AS abreviaturaUnidad,
            u.tipoCalculo                AS tipoCalculoUnidad

        FROM productos p
        INNER JOIN inventario_stock st
            ON st.idProducto = p.idProducto
        LEFT JOIN marca_producto m
            ON m.idMarca = p.idMarca
        LEFT JOIN categoria_producto c
            ON c.idCategoria = p.idCategoria
        LEFT JOIN subcategoria_producto sc
            ON sc.idSubcategoria = p.idSubcategoria
        LEFT JOIN unidades u
            ON u.idUnidad = p.idUnidad
       WHERE p.idEstadoProducto = 1
                     AND st.activo = 1
                     AND st.idSede = ?
                     AND st.stockActual > 0
                    AND EXISTS (
                                    SELECT 1
                                    FROM producto_precio_presentacion pp
                                    WHERE pp.idProducto = p.idProducto
                                      AND pp.estado = 1
                                      AND COALESCE(pp.precioVenta, 0) > 0
                                      AND COALESCE(pp.factorConversionStock, 0) > 0
                                      AND pp.factorConversionStock <= st.stockActual
                                )
         AND (
                          CAST(p.idProducto AS CHAR) LIKE ?
                       OR p.NombreProducto LIKE ?
                       OR p.CodigoInterno LIKE ?
                       OR p.Dimensiones LIKE ?
                       OR p.modeloProducto LIKE ?
                       OR p.descripcionProducto LIKE ?
                       OR p.nombreUnidadInventario LIKE ?
                       OR p.nombreUnidadVenta LIKE ?
                       OR p.formaVenta LIKE ?
                       OR m.nombreMarca LIKE ?
                       OR c.nombreCategoria LIKE ?
                       OR sc.nombreSubcategoria LIKE ?
                     )
        ORDER BY p.NombreProducto ASC
        LIMIT 20
    """;

        String like = "%" + (filtro == null ? "" : filtro.trim()) + "%";

        return jdbc.query(sql, new ProductoMapper(),
                idSede,
                like, like, like, like, like, like, like, like, like, like, like, like
        );
    }

    @Override
    public Integer obtenerEstadoProductoDuplicado(Integer idProductoExcluir,
                                                  String nombreProducto,
                                                  Integer idMarca,
                                                  Integer idCategoria,
                                                  Integer idSubcategoria) {

        String sql = """
            SELECT p.idEstadoProducto
            FROM productos p
            WHERE LOWER(TRIM(p.NombreProducto)) = LOWER(TRIM(?))
              AND (p.idMarca <=> ?)
              AND (p.idCategoria <=> ?)
              AND (p.idSubcategoria <=> ?)
              AND (? IS NULL OR p.idProducto <> ?)
            LIMIT 1
        """;

        List<Integer> estados = jdbc.queryForList(
                sql,
                Integer.class,
                nombreProducto,
                idMarca,
                idCategoria,
                idSubcategoria,
                idProductoExcluir,
                idProductoExcluir
        );

        return estados.isEmpty() ? null : estados.get(0);
    }

    @Override
    public List<Producto> buscarPorTexto(String texto) {
        String sql = SELECT_PRODUCTOS_BASE + """
            WHERE p.idEstadoProducto IN (1, 2)
              AND (
                   p.NombreProducto LIKE ?
                OR p.CodigoInterno LIKE ?
                OR p.nombreUnidadInventario LIKE ?
                OR p.nombreUnidadVenta LIKE ?
                OR m.nombreMarca LIKE ?
              )
            ORDER BY p.NombreProducto ASC
            LIMIT 15
        """;

        String filtro = "%" + texto + "%";
        return jdbc.query(sql, new ProductoMapper(), filtro, filtro, filtro, filtro, filtro);
    }


    private String obtenerPrefijoCategoria(Integer idCategoria) {
        if (idCategoria == null) {
            return "PRD";
        }

        String sql = """
        SELECT nombreCategoria
        FROM categoria_producto
        WHERE idCategoria = ?
        LIMIT 1
    """;

        List<String> nombres = jdbc.query(
                sql,
                (rs, rowNum) -> rs.getString("nombreCategoria"),
                idCategoria
        );

        if (nombres.isEmpty() || nombres.get(0) == null) {
            return "PRD";
        }

        String categoria = nombres.get(0)
                .trim()
                .toUpperCase()
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U");

        if (categoria.contains("ALUMIN")) {
            return "ALU";
        }

        if (categoria.contains("VIDRIO")) {
            return "VID";
        }

        if (categoria.contains("ACCESOR")) {
            return "ACC";
        }

        if (categoria.contains("SILICON")) {
            return "SIL";
        }

        String limpio = categoria.replaceAll("[^A-Z]", "");

        if (limpio.length() >= 3) {
            return limpio.substring(0, 3);
        }

        return "PRD";
    }

    @Override
    public String generarCodigoInterno(Integer idCategoria) {
        String prefijo = obtenerPrefijoCategoria(idCategoria);

        String sql = """
            SELECT CodigoInterno
            FROM productos
            WHERE CodigoInterno LIKE ?
            ORDER BY CodigoInterno DESC
            LIMIT 1
        """;

        String like = prefijo + "-%";

        List<String> resultado = jdbc.query(sql,
                (rs, rowNum) -> rs.getString("CodigoInterno"),
                like
        );

        if (resultado.isEmpty()) {
            return prefijo + "-001";
        }

        String ultimo = resultado.get(0);

        try {
            int numero = Integer.parseInt(ultimo.split("-")[1]);
            return String.format("%s-%03d", prefijo, numero + 1);
        } catch (Exception e) {
            return prefijo + "-001";
        }
    }

    // ============================================================
    // CAMBIAR ESTADO
    // ============================================================

    @Override
    public int cambiarEstado(int idProducto, int nuevoEstado) {
        String sql = """
            UPDATE productos
               SET idEstadoProducto = ?
             WHERE idProducto = ?
        """;

        return jdbc.update(sql, nuevoEstado, idProducto);
    }

    // ============================================================
    // ÚLTIMO PRECIO DE COMPRA
    // ============================================================

    @Override
    public BigDecimal obtenerUltimoPrecioCompra(int idProducto) {
        String sql = """
            SELECT dc.PrecioUnitario, c.Moneda, c.TipoCambio
            FROM detallecompra dc
            INNER JOIN compras c
                ON c.idCompra = dc.idCompra
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
    public BigDecimal obtenerCostoPromedioPorSede(Integer idProducto, Integer idSede) {
        String sql = """
        SELECT
            COALESCE(
                NULLIF(st.costoPromedio, 0),
                NULLIF(p.costoPromedio, 0),
                0
            ) AS costoPromedio
        FROM productos p
        LEFT JOIN inventario_stock st
            ON st.idProducto = p.idProducto
           AND st.idSede = ?
           AND st.activo = 1
        WHERE p.idProducto = ?
        LIMIT 1
    """;

        return jdbc.query(sql, rs -> {
            if (!rs.next()) {
                return BigDecimal.ZERO;
            }

            BigDecimal costo = rs.getBigDecimal("costoPromedio");
            return costo != null ? costo : BigDecimal.ZERO;
        }, idSede, idProducto);
    }
    @Override
    public List<Producto> listarActivosConStockEnSedes() {

        String sql = SELECT_PRODUCTOS_BASE + """
        WHERE p.idEstadoProducto = 1
          AND EXISTS (
              SELECT 1
              FROM inventario_stock st
              INNER JOIN sedes s
                  ON s.idSede = st.idSede
              WHERE st.idProducto = p.idProducto
                AND st.activo = 1
                AND st.stockActual > 0
                AND s.activo = 1
          )
          AND EXISTS (
              SELECT 1
              FROM producto_precio_presentacion ppp
              WHERE ppp.idProducto = p.idProducto
                AND ppp.estado = 1
                AND COALESCE(ppp.precioVenta, 0) > 0
          )
        ORDER BY p.NombreProducto ASC
    """;

        return jdbc.query(sql, new ProductoMapper());
    }

    @Override
    public List<Map<String, Object>> obtenerDisponibilidadPorProducto(Integer idProducto) {

        String sql = """
        SELECT
            s.idSede,
            s.nombre AS nombreSede,
            s.tipo AS tipoSede,
            s.direccion AS direccionSede,
            s.ciudad AS ciudadSede,
            st.stockActual
        FROM inventario_stock st
        INNER JOIN sedes s
            ON s.idSede = st.idSede
        WHERE st.idProducto = ?
          AND st.activo = 1
          AND st.stockActual > 0
          AND s.activo = 1
        ORDER BY s.tipo ASC, s.nombre ASC
    """;

        return jdbc.queryForList(sql, idProducto);
    }
}
