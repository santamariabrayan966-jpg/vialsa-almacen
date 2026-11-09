package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.IInventarioStockDao;
import com.dentia.model.interno.inventario.InventarioStock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class JdbcInventarioStockDao implements IInventarioStockDao {

    private final JdbcTemplate jdbc;

    public JdbcInventarioStockDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<InventarioStock> mapper = (rs, rowNum) -> {
        InventarioStock s = new InventarioStock();

        s.setIdStock(rs.getInt("idStock"));
        s.setIdProducto(rs.getInt("idProducto"));
        s.setIdSede(rs.getInt("idSede"));

        s.setCodigoProducto(rs.getString("CodigoInterno"));
        s.setNombreProducto(rs.getString("NombreProducto"));
        s.setFotoProducto(rs.getString("foto"));
        s.setCategoria(rs.getString("nombreCategoria"));

        s.setNombreSede(rs.getString("nombreSede"));
        s.setTipoSede(rs.getString("tipoSede"));
        s.setCiudadSede(rs.getString("ciudad"));
        s.setDireccionSede(rs.getString("direccion"));

        s.setStockActual(rs.getBigDecimal("stockActual"));
        s.setStockMinimo(rs.getBigDecimal("stockMinimo"));
        s.setStockMaximo(rs.getBigDecimal("stockMaximo"));
        s.setCostoPromedio(rs.getBigDecimal("costoPromedio"));
        s.setValorAprox(rs.getBigDecimal("valorAprox"));

        s.setActivo(rs.getBoolean("activo"));

        if (rs.getTimestamp("fechaActualizacion") != null) {
            s.setFechaActualizacion(rs.getTimestamp("fechaActualizacion").toLocalDateTime());
        }

        return s;
    };

    @Override
    public List<InventarioStock> listarExistencias() {
        String sql = """
            SELECT
                st.idStock,
                st.idProducto,
                st.idSede,
                p.CodigoInterno,
                p.NombreProducto,
                p.foto,
               COALESCE(cp.nombreCategoria, 'Sin categoría') AS nombreCategoria,
                se.nombre AS nombreSede,
                se.tipo AS tipoSede,
                se.ciudad,
                se.direccion,
                st.stockActual,
                st.stockMinimo,
                st.stockMaximo,
               COALESCE(
                           NULLIF(st.costoPromedio, 0),
                           NULLIF(p.costoPromedio, 0),
                           NULLIF(p.PrecioUnitario, 0),
                           0
                       ) AS costoPromedio,
                       (
                           st.stockActual * COALESCE(
                               NULLIF(st.costoPromedio, 0),
                               NULLIF(p.costoPromedio, 0),
                               NULLIF(p.PrecioUnitario, 0),
                               0
                           )
                       ) AS valorAprox,
                st.activo,
                st.fechaActualizacion
            FROM inventario_stock st
            INNER JOIN productos p ON p.idProducto = st.idProducto
           LEFT JOIN categoria_producto cp ON cp.idCategoria = p.idCategoria
            INNER JOIN sedes se ON se.idSede = st.idSede
            ORDER BY se.nombre ASC, p.NombreProducto ASC
        """;

        return jdbc.query(sql, mapper);
    }

    @Override
    public List<InventarioStock> listarExistenciasPorSede(Integer idSede) {
        String sql = """
        SELECT
            st.idStock,
            st.idProducto,
            st.idSede,
            p.CodigoInterno,
            p.NombreProducto,
            p.foto,
           COALESCE(cp.nombreCategoria, 'Sin categoría') AS nombreCategoria,
            se.nombre AS nombreSede,
            se.tipo AS tipoSede,
            se.ciudad,
            se.direccion,
            st.stockActual,
            st.stockMinimo,
            st.stockMaximo,
          COALESCE(
                          NULLIF(st.costoPromedio, 0),
                          NULLIF(p.costoPromedio, 0),
                          NULLIF(p.PrecioUnitario, 0),
                          0
                      ) AS costoPromedio,
                      (
                          st.stockActual * COALESCE(
                              NULLIF(st.costoPromedio, 0),
                              NULLIF(p.costoPromedio, 0),
                              NULLIF(p.PrecioUnitario, 0),
                              0
                          )
                      ) AS valorAprox,
            st.activo,
            st.fechaActualizacion
        FROM inventario_stock st
        INNER JOIN productos p ON p.idProducto = st.idProducto
     LEFT JOIN categoria_producto cp ON cp.idCategoria = p.idCategoria
        INNER JOIN sedes se ON se.idSede = st.idSede
        WHERE st.idSede = ?
          AND st.activo = 1
        ORDER BY p.NombreProducto ASC
    """;

        return jdbc.query(sql, mapper, idSede);
    }

    @Override
    public Optional<InventarioStock> buscarPorProductoYSede(Integer idProducto, Integer idSede) {
        String sql = """
            SELECT
                st.idStock,
                st.idProducto,
                st.idSede,
                p.CodigoInterno,
                p.NombreProducto,
                p.foto,
               COALESCE(cp.nombreCategoria, 'Sin categoría') AS nombreCategoria,
                se.nombre AS nombreSede,
                se.tipo AS tipoSede,
                se.ciudad,
                se.direccion,
                st.stockActual,
                st.stockMinimo,
                st.stockMaximo,
               COALESCE(
                           NULLIF(st.costoPromedio, 0),
                           NULLIF(p.costoPromedio, 0),
                           NULLIF(p.PrecioUnitario, 0),
                           0
                       ) AS costoPromedio,
                       (
                           st.stockActual * COALESCE(
                               NULLIF(st.costoPromedio, 0),
                               NULLIF(p.costoPromedio, 0),
                               NULLIF(p.PrecioUnitario, 0),
                               0
                           )
                       ) AS valorAprox,
                st.activo,
                st.fechaActualizacion
            FROM inventario_stock st
            INNER JOIN productos p ON p.idProducto = st.idProducto
           LEFT JOIN categoria_producto cp ON cp.idCategoria = p.idCategoria
            INNER JOIN sedes se ON se.idSede = st.idSede
            WHERE st.idProducto = ?
              AND st.idSede = ?
        """;

        List<InventarioStock> lista = jdbc.query(sql, mapper, idProducto, idSede);
        return lista.stream().findFirst();
    }

    @Override
    public Optional<InventarioStock> bloquearPorProductoYSede(Integer idProducto, Integer idSede) {
        String sql = """
            SELECT
                st.idStock,
                st.idProducto,
                st.idSede,
                p.CodigoInterno,
                p.NombreProducto,
                p.foto,
                COALESCE(cp.nombreCategoria, 'Sin categoria') AS nombreCategoria,
                se.nombre AS nombreSede,
                se.tipo AS tipoSede,
                se.ciudad,
                se.direccion,
                st.stockActual,
                st.stockMinimo,
                st.stockMaximo,
                COALESCE(
                    NULLIF(st.costoPromedio, 0),
                    NULLIF(p.costoPromedio, 0),
                    NULLIF(p.PrecioUnitario, 0),
                    0
                ) AS costoPromedio,
                (
                    st.stockActual * COALESCE(
                        NULLIF(st.costoPromedio, 0),
                        NULLIF(p.costoPromedio, 0),
                        NULLIF(p.PrecioUnitario, 0),
                        0
                    )
                ) AS valorAprox,
                st.activo,
                st.fechaActualizacion
            FROM inventario_stock st
            INNER JOIN productos p ON p.idProducto = st.idProducto
            LEFT JOIN categoria_producto cp ON cp.idCategoria = p.idCategoria
            INNER JOIN sedes se ON se.idSede = st.idSede
            WHERE st.idProducto = ?
              AND st.idSede = ?
              AND st.activo = 1
            FOR UPDATE
        """;

        return jdbc.query(sql, mapper, idProducto, idSede)
                .stream()
                .findFirst();
    }

    @Override
    public List<InventarioStock> bloquearPorSedeYProductos(Integer idSede, List<Integer> idsProducto) {
        if (idSede == null || idsProducto == null || idsProducto.isEmpty()) {
            return List.of();
        }

        List<Integer> idsOrdenados = idsProducto.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();

        if (idsOrdenados.isEmpty()) {
            return List.of();
        }

        String placeholders = idsOrdenados.stream()
                .map(id -> "?")
                .collect(Collectors.joining(", "));

        String sql = """
            SELECT
                st.idStock,
                st.idProducto,
                st.idSede,
                p.CodigoInterno,
                p.NombreProducto,
                p.foto,
                COALESCE(cp.nombreCategoria, 'Sin categoria') AS nombreCategoria,
                se.nombre AS nombreSede,
                se.tipo AS tipoSede,
                se.ciudad,
                se.direccion,
                st.stockActual,
                st.stockMinimo,
                st.stockMaximo,
                COALESCE(
                    NULLIF(st.costoPromedio, 0),
                    NULLIF(p.costoPromedio, 0),
                    NULLIF(p.PrecioUnitario, 0),
                    0
                ) AS costoPromedio,
                (
                    st.stockActual * COALESCE(
                        NULLIF(st.costoPromedio, 0),
                        NULLIF(p.costoPromedio, 0),
                        NULLIF(p.PrecioUnitario, 0),
                        0
                    )
                ) AS valorAprox,
                st.activo,
                st.fechaActualizacion
            FROM inventario_stock st
            INNER JOIN productos p ON p.idProducto = st.idProducto
            LEFT JOIN categoria_producto cp ON cp.idCategoria = p.idCategoria
            INNER JOIN sedes se ON se.idSede = st.idSede
            WHERE st.idSede = ?
              AND st.idProducto IN (%s)
              AND st.activo = 1
            ORDER BY st.idSede ASC, st.idProducto ASC
            FOR UPDATE
        """.formatted(placeholders);

        List<Object> params = new ArrayList<>();
        params.add(idSede);
        params.addAll(idsOrdenados);

        return jdbc.query(sql, mapper, params.toArray());
    }

    @Override
    public void crearStockInicial(Integer idProducto, Integer idSede, BigDecimal stockInicial, BigDecimal costoPromedio) {
        String sql = """
        INSERT INTO inventario_stock
        (idProducto, idSede, stockActual, stockMinimo, costoPromedio, activo, fechaActualizacion)
        VALUES (?, ?, ?, 0, ?, 1, NOW())
    """;

        jdbc.update(sql, idProducto, idSede, stockInicial, costoPromedio);
    }

    @Override
    public void actualizarStock(Integer idProducto, Integer idSede, BigDecimal nuevoStock) {
        String sql = """
        UPDATE inventario_stock
           SET stockActual = ?,
               fechaActualizacion = NOW()
         WHERE idProducto = ?
           AND idSede = ?
    """;

        jdbc.update(sql, nuevoStock, idProducto, idSede);
    }

    @Override
    public int descontarStockCondicional(Integer idProducto, Integer idSede, BigDecimal cantidad) {
        String sql = """
        UPDATE inventario_stock
           SET stockActual = stockActual - ?,
               fechaActualizacion = NOW()
         WHERE idProducto = ?
           AND idSede = ?
           AND stockActual >= ?
    """;

        return jdbc.update(sql, cantidad, idProducto, idSede, cantidad);
    }

    @Override
    public void actualizarStockYCostoPromedio(
            Integer idProducto,
            Integer idSede,
            BigDecimal nuevoStock,
            BigDecimal costoPromedio
    ) {
        String sql = """
        UPDATE inventario_stock
           SET stockActual = ?,
               costoPromedio = ?,
               fechaActualizacion = NOW()
         WHERE idProducto = ?
           AND idSede = ?
    """;

        jdbc.update(sql, nuevoStock, costoPromedio, idProducto, idSede);
    }

    @Override
    public int contarProductosConStock() {
        String sql = """
            SELECT COUNT(DISTINCT idProducto)
            FROM inventario_stock
            WHERE activo = 1
              AND stockActual > 0
        """;

        Integer total = jdbc.queryForObject(sql, Integer.class);
        return total != null ? total : 0;
    }

    @Override
    public int contarProductosConStockPorSede(Integer idSede) {
        String sql = """
        SELECT COUNT(DISTINCT idProducto)
        FROM inventario_stock
        WHERE activo = 1
          AND stockActual > 0
          AND idSede = ?
    """;

        Integer total = jdbc.queryForObject(sql, Integer.class, idSede);
        return total != null ? total : 0;
    }

    @Override
    public BigDecimal obtenerStockTotal() {
        String sql = """
            SELECT COALESCE(SUM(stockActual), 0)
            FROM inventario_stock
            WHERE activo = 1
        """;

        return jdbc.queryForObject(sql, BigDecimal.class);
    }

    @Override
    public BigDecimal obtenerStockTotalPorSede(Integer idSede) {
        String sql = """
        SELECT COALESCE(SUM(stockActual), 0)
        FROM inventario_stock
        WHERE activo = 1
          AND idSede = ?
    """;

        BigDecimal total = jdbc.queryForObject(sql, BigDecimal.class, idSede);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal obtenerValorTotalInventario() {
        String sql = """
           SELECT COALESCE(SUM(
                       st.stockActual * COALESCE(
                           NULLIF(st.costoPromedio, 0),
                           NULLIF(p.costoPromedio, 0),
                           NULLIF(p.PrecioUnitario, 0),
                           0
                       )
                   ), 0)
            FROM inventario_stock st
            INNER JOIN productos p ON p.idProducto = st.idProducto
            WHERE st.activo = 1
        """;

        return jdbc.queryForObject(sql, BigDecimal.class);
    }

    @Override
    public BigDecimal obtenerValorTotalInventarioPorSede(Integer idSede) {
        String sql = """
        SELECT COALESCE(SUM(
                        st.stockActual * COALESCE(
                            NULLIF(st.costoPromedio, 0),
                            NULLIF(p.costoPromedio, 0),
                            NULLIF(p.PrecioUnitario, 0),
                            0
                        )
                    ), 0)
        FROM inventario_stock st
        INNER JOIN productos p ON p.idProducto = st.idProducto
        WHERE st.activo = 1
          AND st.idSede = ?
    """;

        BigDecimal total = jdbc.queryForObject(sql, BigDecimal.class, idSede);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal obtenerStockEquivalenteDesdePartes(Integer idProducto, Integer idSede) {
        String sql = """
        SELECT COALESCE(SUM(equivalenteUnidadFisica), 0)
        FROM inventario_partes
        WHERE idProducto = ?
          AND idSede = ?
          AND estado IN ('DISPONIBLE', 'PARCIAL')
    """;

        BigDecimal total = jdbc.queryForObject(sql, BigDecimal.class, idProducto, idSede);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal obtenerValorDisponibleDesdePartes(Integer idProducto, Integer idSede) {
        String sql = """
        SELECT COALESCE(SUM(costoTotalDisponible), 0)
        FROM inventario_partes
        WHERE idProducto = ?
          AND idSede = ?
          AND estado IN ('DISPONIBLE', 'PARCIAL')
    """;

        BigDecimal total = jdbc.queryForObject(sql, BigDecimal.class, idProducto, idSede);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public void recalcularStockDesdePartes(Integer idProducto, Integer idSede) {
        BigDecimal stockEquivalente = obtenerStockEquivalenteDesdePartes(idProducto, idSede);

        String sqlExiste = """
        SELECT COUNT(*)
        FROM inventario_stock
        WHERE idProducto = ?
          AND idSede = ?
    """;

        Integer existe = jdbc.queryForObject(sqlExiste, Integer.class, idProducto, idSede);

        if (existe != null && existe > 0) {
            String sqlUpdate = """
            UPDATE inventario_stock
               SET stockActual = ?,
                   fechaActualizacion = NOW()
             WHERE idProducto = ?
               AND idSede = ?
        """;

            jdbc.update(sqlUpdate, stockEquivalente, idProducto, idSede);
        } else {
            crearStockInicial(idProducto, idSede, stockEquivalente, BigDecimal.ZERO);
        }
    }

    @Override
    public void recalcularValorDesdePartes(Integer idProducto, Integer idSede) {
        BigDecimal stockEquivalente = obtenerStockEquivalenteDesdePartes(idProducto, idSede);
        BigDecimal valorDisponible = obtenerValorDisponibleDesdePartes(idProducto, idSede);

        BigDecimal costoPromedioEquivalente = BigDecimal.ZERO;

        if (stockEquivalente.compareTo(BigDecimal.ZERO) > 0) {
            costoPromedioEquivalente = valorDisponible.divide(
                    stockEquivalente,
                    6,
                    java.math.RoundingMode.HALF_UP
            );
        }

        String sqlExiste = """
        SELECT COUNT(*)
        FROM inventario_stock
        WHERE idProducto = ?
          AND idSede = ?
    """;

        Integer existe = jdbc.queryForObject(sqlExiste, Integer.class, idProducto, idSede);

        if (existe != null && existe > 0) {
            String sqlUpdate = """
            UPDATE inventario_stock
               SET stockActual = ?,
                   costoPromedio = ?,
                   fechaActualizacion = NOW()
             WHERE idProducto = ?
               AND idSede = ?
        """;

            jdbc.update(sqlUpdate, stockEquivalente, costoPromedioEquivalente, idProducto, idSede);
        } else {
            crearStockInicial(idProducto, idSede, stockEquivalente, costoPromedioEquivalente);
        }
    }
}
