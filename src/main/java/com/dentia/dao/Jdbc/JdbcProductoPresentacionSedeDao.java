package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.IProductoPresentacionSedeDao;
import com.dentia.model.interno.ProductoPresentacionSede;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

@Repository
public class JdbcProductoPresentacionSedeDao implements IProductoPresentacionSedeDao {

    private final JdbcTemplate jdbc;

    public JdbcProductoPresentacionSedeDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT_BASE = """
        SELECT
            pps.idPresentacionSede,
            pps.idPrecioPresentacion,
            pps.idSede,
            pps.activo,
            pps.fechaCreacion,
            pps.fechaActualizacion,
            s.nombre AS nombreSede,
            s.tipo AS tipoSede,
            s.ciudad AS ciudadSede
        FROM producto_presentacion_sede pps
        INNER JOIN sedes s
            ON s.idSede = pps.idSede
    """;

    @Override
    public List<ProductoPresentacionSede> listarPorPresentacion(Integer idPrecioPresentacion) {
        String sql = SELECT_BASE + """
            WHERE pps.idPrecioPresentacion = ?
            ORDER BY s.nombre ASC
        """;

        return jdbc.query(sql, (rs, rowNum) -> mapear(rs), idPrecioPresentacion);
    }

    @Override
    public List<ProductoPresentacionSede> listarActivasPorPresentacion(Integer idPrecioPresentacion) {
        String sql = SELECT_BASE + """
            WHERE pps.idPrecioPresentacion = ?
              AND pps.activo = 1
              AND s.activo = 1
            ORDER BY s.nombre ASC
        """;

        return jdbc.query(sql, (rs, rowNum) -> mapear(rs), idPrecioPresentacion);
    }

    @Override
    public List<ProductoPresentacionSede> listarActivasPorProducto(Integer idProducto) {
        String sql = SELECT_BASE + """
            INNER JOIN producto_precio_presentacion ppp
                ON ppp.idPrecioPresentacion = pps.idPrecioPresentacion
            WHERE ppp.idProducto = ?
              AND ppp.estado = 1
              AND pps.activo = 1
              AND s.activo = 1
            ORDER BY s.nombre ASC, ppp.nombrePresentacion ASC
        """;

        return jdbc.query(sql, (rs, rowNum) -> mapear(rs), idProducto);
    }

    @Override
    public boolean estaActiva(Integer idPrecioPresentacion, Integer idSede) {
        String sql = """
            SELECT COUNT(*)
            FROM producto_presentacion_sede pps
            INNER JOIN sedes s
                ON s.idSede = pps.idSede
            WHERE pps.idPrecioPresentacion = ?
              AND pps.idSede = ?
              AND pps.activo = 1
              AND s.activo = 1
        """;

        Integer total = jdbc.queryForObject(sql, Integer.class, idPrecioPresentacion, idSede);
        return total != null && total > 0;
    }

    @Override
    public int habilitar(Integer idPrecioPresentacion, Integer idSede) {
        String sql = """
            INSERT INTO producto_presentacion_sede (
                idPrecioPresentacion,
                idSede,
                activo
            )
            VALUES (?, ?, 1)
            ON DUPLICATE KEY UPDATE
                activo = 1,
                fechaActualizacion = NOW()
        """;

        return jdbc.update(sql, idPrecioPresentacion, idSede);
    }

    @Override
    public int deshabilitar(Integer idPrecioPresentacion, Integer idSede) {
        String sql = """
            UPDATE producto_presentacion_sede
            SET activo = 0,
                fechaActualizacion = NOW()
            WHERE idPrecioPresentacion = ?
              AND idSede = ?
        """;

        return jdbc.update(sql, idPrecioPresentacion, idSede);
    }

    @Override
    public void sincronizarSedes(Integer idPrecioPresentacion, Set<Integer> idsSede) {
        if (idPrecioPresentacion == null || idPrecioPresentacion <= 0) {
            throw new IllegalArgumentException("Presentacion invalida.");
        }

        Set<Integer> ids = idsSede == null ? Set.of() : idsSede;

        jdbc.update("""
            UPDATE producto_presentacion_sede
            SET activo = 0,
                fechaActualizacion = NOW()
            WHERE idPrecioPresentacion = ?
        """, idPrecioPresentacion);

        for (Integer idSede : ids) {
            if (idSede != null && idSede > 0) {
                habilitar(idPrecioPresentacion, idSede);
            }
        }
    }

    private ProductoPresentacionSede mapear(java.sql.ResultSet rs) throws java.sql.SQLException {
        ProductoPresentacionSede item = new ProductoPresentacionSede();
        item.setIdPresentacionSede(rs.getInt("idPresentacionSede"));
        item.setIdPrecioPresentacion(rs.getInt("idPrecioPresentacion"));
        item.setIdSede(rs.getInt("idSede"));
        item.setActivo(rs.getObject("activo") != null && rs.getBoolean("activo"));

        Timestamp fechaCreacion = rs.getTimestamp("fechaCreacion");
        Timestamp fechaActualizacion = rs.getTimestamp("fechaActualizacion");
        item.setFechaCreacion(fechaCreacion == null ? null : fechaCreacion.toLocalDateTime());
        item.setFechaActualizacion(fechaActualizacion == null ? null : fechaActualizacion.toLocalDateTime());

        item.setNombreSede(rs.getString("nombreSede"));
        item.setTipoSede(rs.getString("tipoSede"));
        item.setCiudadSede(rs.getString("ciudadSede"));
        return item;
    }
}
