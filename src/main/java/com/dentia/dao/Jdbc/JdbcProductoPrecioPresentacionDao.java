package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.IProductoPrecioPresentacionDao;
import com.dentia.model.interno.ProductoPrecioPresentacion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcProductoPrecioPresentacionDao implements IProductoPrecioPresentacionDao {

    private final JdbcTemplate jdbc;

    public JdbcProductoPrecioPresentacionDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT_BASE = """
        SELECT
            pp.idPrecioPresentacion,
            pp.idProducto,
            pp.idSede,
            pp.nombrePresentacion,
            pp.idUnidadVenta,
            pp.tipoCalculoVenta,
            pp.cantidadIncluida,
            pp.largoBase,
            pp.anchoBase,
            pp.medidaTotalBase,
            pp.factorConversionStock,
            pp.precioVenta,
            pp.descuentoMaximo,
            pp.precioPortada,
            ofe.precioOferta AS precioOfertaPortada,
            CASE WHEN ofe.idOferta IS NULL THEN 0 ELSE 1 END AS tieneOfertaActiva,
            ofe.textoOferta AS textoOfertaPortada,
            ofe.fechaInicio AS fechaInicioOferta,
            ofe.fechaFin AS fechaFinOferta,
            pp.precioBajoCosto,
            pp.margenBajo,
            pp.costoBaseCalculado,
            pp.precioMinimoRecomendado,
            pp.motivoPrecioBajoCosto,
            pp.usuarioAutorizaPrecio,
            pp.fechaAutorizacionPrecio,
            pp.descripcionUso,
            pp.estado,
            u.NombreUnidad AS nombreUnidadVenta,
            u.abreviatura AS abreviaturaUnidadVenta
        FROM producto_precio_presentacion pp
        LEFT JOIN unidades u
            ON u.idUnidad = pp.idUnidadVenta
        LEFT JOIN producto_presentacion_oferta ofe
            ON ofe.idOferta = (
                SELECT o.idOferta
                FROM producto_presentacion_oferta o
                WHERE o.idPrecioPresentacion = pp.idPrecioPresentacion
                  AND o.activo = 1
                  AND o.fechaInicio <= CURDATE()
                  AND o.fechaFin >= CURDATE()
                ORDER BY o.idOferta DESC
                LIMIT 1
            )
    """;

    private ProductoPrecioPresentacion mapear(java.sql.ResultSet rs) throws java.sql.SQLException {
        ProductoPrecioPresentacion p = new ProductoPrecioPresentacion();

        p.setIdPrecioPresentacion(rs.getInt("idPrecioPresentacion"));
        p.setIdProducto(rs.getInt("idProducto"));
        p.setIdSede(rs.getObject("idSede") == null ? null : rs.getInt("idSede"));

        p.setNombrePresentacion(rs.getString("nombrePresentacion"));
        p.setIdUnidadVenta(rs.getObject("idUnidadVenta") == null ? null : rs.getInt("idUnidadVenta"));
        p.setTipoCalculoVenta(rs.getString("tipoCalculoVenta"));
        p.setCantidadIncluida(rs.getBigDecimal("cantidadIncluida"));
        p.setLargoBase(rs.getBigDecimal("largoBase"));
        p.setAnchoBase(rs.getBigDecimal("anchoBase"));
        p.setMedidaTotalBase(rs.getBigDecimal("medidaTotalBase"));
        p.setFactorConversionStock(rs.getBigDecimal("factorConversionStock"));
        p.setPrecioVenta(rs.getBigDecimal("precioVenta"));
        p.setDescuentoMaximo(rs.getBigDecimal("descuentoMaximo"));
        p.setPrecioPortada(rs.getObject("precioPortada") != null && rs.getBoolean("precioPortada"));
        p.setPrecioOfertaPortada(rs.getBigDecimal("precioOfertaPortada"));
        p.setTieneOfertaActiva(rs.getObject("tieneOfertaActiva") != null && rs.getBoolean("tieneOfertaActiva"));
        p.setTextoOfertaPortada(rs.getString("textoOfertaPortada"));
        p.setFechaInicioOferta(rs.getDate("fechaInicioOferta") == null ? null : rs.getDate("fechaInicioOferta").toLocalDate());
        p.setFechaFinOferta(rs.getDate("fechaFinOferta") == null ? null : rs.getDate("fechaFinOferta").toLocalDate());

        p.setPrecioBajoCosto(rs.getObject("precioBajoCosto") != null && rs.getBoolean("precioBajoCosto"));
        p.setMargenBajo(rs.getObject("margenBajo") != null && rs.getBoolean("margenBajo"));
        p.setCostoBaseCalculado(rs.getBigDecimal("costoBaseCalculado"));
        p.setPrecioMinimoRecomendado(rs.getBigDecimal("precioMinimoRecomendado"));
        p.setMotivoPrecioBajoCosto(rs.getString("motivoPrecioBajoCosto"));
        p.setUsuarioAutorizaPrecio(rs.getString("usuarioAutorizaPrecio"));

        Timestamp fechaAutorizacion = rs.getTimestamp("fechaAutorizacionPrecio");
        p.setFechaAutorizacionPrecio(
                fechaAutorizacion == null ? null : fechaAutorizacion.toLocalDateTime()
        );

        p.setDescripcionUso(rs.getString("descripcionUso"));
        p.setEstado(rs.getInt("estado"));

        p.setNombreUnidadVenta(rs.getString("nombreUnidadVenta"));
        p.setAbreviaturaUnidadVenta(rs.getString("abreviaturaUnidadVenta"));

        return p;
    }

    @Override
    public List<ProductoPrecioPresentacion> listarPorProducto(Integer idProducto) {
        String sql = SELECT_BASE + """
            WHERE pp.idProducto = ?
            ORDER BY pp.nombrePresentacion ASC
        """;

        return jdbc.query(sql, (rs, rowNum) -> mapear(rs), idProducto);
    }

    @Override
    public List<ProductoPrecioPresentacion> listarPorProductoYSede(Integer idProducto, Integer idSede) {
        String sql = SELECT_BASE + """
            INNER JOIN producto_presentacion_sede pps
                ON pps.idPrecioPresentacion = pp.idPrecioPresentacion
            INNER JOIN sedes s
                ON s.idSede = pps.idSede
            WHERE pp.idProducto = ?
              AND pps.idSede = ?
              AND pps.activo = 1
              AND s.activo = 1
            ORDER BY pp.nombrePresentacion ASC
        """;

        return jdbc.query(sql, (rs, rowNum) -> mapear(rs), idProducto, idSede);
    }

    @Override
    public List<ProductoPrecioPresentacion> listarActivasPorProducto(Integer idProducto) {
        String sql = SELECT_BASE + """
            WHERE pp.idProducto = ?
              AND pp.estado = 1
            ORDER BY pp.nombrePresentacion ASC
        """;

        return jdbc.query(sql, (rs, rowNum) -> mapear(rs), idProducto);
    }

    @Override
    public List<ProductoPrecioPresentacion> listarActivasPorProductoYSede(Integer idProducto, Integer idSede) {
        return listarPorProductoYSede(idProducto, idSede).stream()
                .filter(p -> p.getEstado() != null && p.getEstado() == 1)
                .toList();
    }

    @Override
    public Optional<ProductoPrecioPresentacion> buscarPorId(Integer idPrecioPresentacion) {
        String sql = SELECT_BASE + """
            WHERE pp.idPrecioPresentacion = ?
            LIMIT 1
        """;

        List<ProductoPrecioPresentacion> lista =
                jdbc.query(sql, (rs, rowNum) -> mapear(rs), idPrecioPresentacion);

        return lista.stream().findFirst();
    }

    @Override
    public int crear(ProductoPrecioPresentacion precio) {
        String sql = """
            INSERT INTO producto_precio_presentacion (
                idProducto,
                idSede,
                nombrePresentacion,
                idUnidadVenta,
                tipoCalculoVenta,
                cantidadIncluida,
                largoBase,
                anchoBase,
                medidaTotalBase,
                factorConversionStock,
                precioVenta,
                descuentoMaximo,
                precioPortada,
                precioBajoCosto,
                margenBajo,
                costoBaseCalculado,
                precioMinimoRecomendado,
                motivoPrecioBajoCosto,
                usuarioAutorizaPrecio,
                fechaAutorizacionPrecio,
                descripcionUso,
                estado
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        return jdbc.update(sql,
                precio.getIdProducto(),
                null,
                precio.getNombrePresentacion(),
                precio.getIdUnidadVenta(),
                precio.getTipoCalculoVenta(),
                precio.getCantidadIncluida(),
                precio.getLargoBase(),
                precio.getAnchoBase(),
                precio.getMedidaTotalBase(),
                precio.getFactorConversionStock(),
                precio.getPrecioVenta(),
                precio.getDescuentoMaximo(),
                Boolean.TRUE.equals(precio.getPrecioPortada()),
                precio.getPrecioBajoCosto() != null && precio.getPrecioBajoCosto(),
                precio.getMargenBajo() != null && precio.getMargenBajo(),
                precio.getCostoBaseCalculado(),
                precio.getPrecioMinimoRecomendado(),
                precio.getMotivoPrecioBajoCosto(),
                precio.getUsuarioAutorizaPrecio(),
                precio.getFechaAutorizacionPrecio(),
                precio.getDescripcionUso(),
                precio.getEstado() == null ? 1 : precio.getEstado()
        );
    }

    @Override
    public int actualizar(ProductoPrecioPresentacion precio) {
        String sql = """
            UPDATE producto_precio_presentacion
               SET nombrePresentacion = ?,
                   idUnidadVenta = ?,
                   tipoCalculoVenta = ?,
                   cantidadIncluida = ?,
                   largoBase = ?,
                   anchoBase = ?,
                   medidaTotalBase = ?,
                   factorConversionStock = ?,
                   precioVenta = ?,
                   descuentoMaximo = ?,
                   precioPortada = ?,
                   precioBajoCosto = ?,
                   margenBajo = ?,
                   costoBaseCalculado = ?,
                   precioMinimoRecomendado = ?,
                   motivoPrecioBajoCosto = ?,
                   usuarioAutorizaPrecio = ?,
                   fechaAutorizacionPrecio = ?,
                   descripcionUso = ?,
                   estado = ?
             WHERE idPrecioPresentacion = ?
               AND idProducto = ?
        """;

        return jdbc.update(sql,
                precio.getNombrePresentacion(),
                precio.getIdUnidadVenta(),
                precio.getTipoCalculoVenta(),
                precio.getCantidadIncluida(),
                precio.getLargoBase(),
                precio.getAnchoBase(),
                precio.getMedidaTotalBase(),
                precio.getFactorConversionStock(),
                precio.getPrecioVenta(),
                precio.getDescuentoMaximo(),
                precio.getPrecioPortada() != null && precio.getPrecioPortada(),
                precio.getPrecioBajoCosto() != null && precio.getPrecioBajoCosto(),
                precio.getMargenBajo() != null && precio.getMargenBajo(),
                precio.getCostoBaseCalculado(),
                precio.getPrecioMinimoRecomendado(),
                precio.getMotivoPrecioBajoCosto(),
                precio.getUsuarioAutorizaPrecio(),
                precio.getFechaAutorizacionPrecio(),
                precio.getDescripcionUso(),
                precio.getEstado() == null ? 1 : precio.getEstado(),
                precio.getIdPrecioPresentacion(),
                precio.getIdProducto()
        );
    }

    @Override
    public int cambiarEstado(Integer idPrecioPresentacion, Integer estado) {
        String sql = """
            UPDATE producto_precio_presentacion
               SET estado = ?,
                   precioPortada = CASE WHEN ? = 1 THEN precioPortada ELSE 0 END
             WHERE idPrecioPresentacion = ?
        """;

        return jdbc.update(sql, estado, estado, idPrecioPresentacion);
    }

    @Override
    public int marcarPrecioPortada(Integer idPrecioPresentacion) {
        ProductoPrecioPresentacion seleccionada = buscarPorId(idPrecioPresentacion)
                .orElseThrow(() -> new IllegalArgumentException("La presentacion seleccionada no existe."));

        if (seleccionada.getEstado() == null || seleccionada.getEstado() != 1) {
            throw new IllegalArgumentException("Solo una presentacion activa puede marcarse como precio portada.");
        }

        if (seleccionada.getIdProducto() == null) {
            throw new IllegalArgumentException("La presentacion seleccionada no tiene producto valido.");
        }

        jdbc.update("""
            UPDATE producto_precio_presentacion
               SET precioPortada = 0
             WHERE idProducto = ?
        """, seleccionada.getIdProducto());

        return jdbc.update("""
            UPDATE producto_precio_presentacion
               SET precioPortada = 1
             WHERE idPrecioPresentacion = ?
               AND estado = 1
        """, idPrecioPresentacion);
    }

    @Override
    public boolean existePresentacionEnSede(
            Integer idProducto,
            Integer idSede,
            String nombrePresentacion,
            Integer idPrecioPresentacionExcluir
    ) {
        String sql = """
            SELECT COUNT(*)
            FROM producto_precio_presentacion
            WHERE idProducto = ?
              AND LOWER(TRIM(nombrePresentacion)) = LOWER(TRIM(?))
              AND (? IS NULL OR idPrecioPresentacion <> ?)
        """;

        Integer total = jdbc.queryForObject(
                sql,
                Integer.class,
                idProducto,
                nombrePresentacion,
                idPrecioPresentacionExcluir,
                idPrecioPresentacionExcluir
        );

        return total != null && total > 0;
    }
}
