package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.IProductoPresentacionOfertaDao;
import com.dentia.model.interno.ProductoPresentacionOferta;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcProductoPresentacionOfertaDao implements IProductoPresentacionOfertaDao {

    private final JdbcTemplate jdbc;

    public JdbcProductoPresentacionOfertaDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private ProductoPresentacionOferta mapear(java.sql.ResultSet rs) throws java.sql.SQLException {
        ProductoPresentacionOferta oferta = new ProductoPresentacionOferta();

        oferta.setIdOferta(rs.getInt("idOferta"));
        oferta.setIdPrecioPresentacion(rs.getInt("idPrecioPresentacion"));
        oferta.setPrecioOferta(rs.getBigDecimal("precioOferta"));
        oferta.setFechaInicio(rs.getDate("fechaInicio").toLocalDate());
        oferta.setFechaFin(rs.getDate("fechaFin").toLocalDate());
        oferta.setTextoOferta(rs.getString("textoOferta"));
        oferta.setActivo(rs.getObject("activo") != null && rs.getBoolean("activo"));

        Timestamp fechaCreacion = rs.getTimestamp("fechaCreacion");
        oferta.setFechaCreacion(fechaCreacion == null ? null : fechaCreacion.toLocalDateTime());

        Timestamp fechaActualizacion = rs.getTimestamp("fechaActualizacion");
        oferta.setFechaActualizacion(fechaActualizacion == null ? null : fechaActualizacion.toLocalDateTime());

        return oferta;
    }

    @Override
    public Optional<ProductoPresentacionOferta> buscarActivaPorPresentacion(Integer idPrecioPresentacion) {
        String sql = """
            SELECT
                idOferta,
                idPrecioPresentacion,
                precioOferta,
                fechaInicio,
                fechaFin,
                textoOferta,
                activo,
                fechaCreacion,
                fechaActualizacion
            FROM producto_presentacion_oferta
            WHERE idPrecioPresentacion = ?
              AND activo = 1
            ORDER BY fechaCreacion DESC
            LIMIT 1
        """;

        List<ProductoPresentacionOferta> ofertas =
                jdbc.query(sql, (rs, rowNum) -> mapear(rs), idPrecioPresentacion);

        return ofertas.stream().findFirst();
    }

    @Override
    public int crear(ProductoPresentacionOferta oferta) {
        String sql = """
            INSERT INTO producto_presentacion_oferta (
                idPrecioPresentacion,
                precioOferta,
                fechaInicio,
                fechaFin,
                textoOferta,
                activo
            )
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        return jdbc.update(sql,
                oferta.getIdPrecioPresentacion(),
                oferta.getPrecioOferta(),
                Date.valueOf(oferta.getFechaInicio()),
                Date.valueOf(oferta.getFechaFin()),
                oferta.getTextoOferta(),
                Boolean.TRUE.equals(oferta.getActivo())
        );
    }

    @Override
    public int actualizar(ProductoPresentacionOferta oferta) {
        String sql = """
            UPDATE producto_presentacion_oferta
               SET precioOferta = ?,
                   fechaInicio = ?,
                   fechaFin = ?,
                   textoOferta = ?,
                   activo = ?,
                   fechaActualizacion = CURRENT_TIMESTAMP
             WHERE idOferta = ?
        """;

        return jdbc.update(sql,
                oferta.getPrecioOferta(),
                Date.valueOf(oferta.getFechaInicio()),
                Date.valueOf(oferta.getFechaFin()),
                oferta.getTextoOferta(),
                Boolean.TRUE.equals(oferta.getActivo()),
                oferta.getIdOferta()
        );
    }

    @Override
    public int desactivarActivasPorPresentacion(Integer idPrecioPresentacion) {
        String sql = """
            UPDATE producto_presentacion_oferta
               SET activo = 0,
                   fechaActualizacion = CURRENT_TIMESTAMP
             WHERE idPrecioPresentacion = ?
               AND activo = 1
        """;

        return jdbc.update(sql, idPrecioPresentacion);
    }
}
