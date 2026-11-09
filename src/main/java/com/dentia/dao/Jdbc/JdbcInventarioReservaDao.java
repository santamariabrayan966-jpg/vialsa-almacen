package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.IInventarioReservaDao;
import com.dentia.model.interno.InventarioReserva;
import com.dentia.model.interno.InventarioReservaDetalle;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Repository
public class JdbcInventarioReservaDao implements IInventarioReservaDao {

    private static final String RESERVA_COLUMNS = """
            idReserva, codigoReserva, idOrdenOnline, idCliente, idSede, estado,
            fechaCreacion, fechaExpiracion, fechaConfirmacion, fechaLiberacion,
            motivoLiberacion, tokenIdempotencia
            """;

    private static final String DETALLE_COLUMNS = """
            idReservaDetalle, idReserva, idOrdenDetalle, idProducto, idPrecioPresentacion,
            idUnidad, nombreProducto, nombrePresentacion, tipoCalculoVenta,
            factorConversionStock, cantidadSolicitada, cantidadReservadaStock
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcInventarioReservaDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<InventarioReserva> reservaMapper = (rs, rowNum) -> {
        InventarioReserva reserva = new InventarioReserva();
        reserva.setIdReserva(rs.getInt("idReserva"));
        reserva.setCodigoReserva(rs.getString("codigoReserva"));
        reserva.setIdOrdenOnline(rs.getInt("idOrdenOnline"));
        reserva.setIdCliente(rs.getInt("idCliente"));
        reserva.setIdSede(rs.getInt("idSede"));
        reserva.setEstado(rs.getString("estado"));
        reserva.setFechaCreacion(toLocalDateTime(rs.getTimestamp("fechaCreacion")));
        reserva.setFechaExpiracion(toLocalDateTime(rs.getTimestamp("fechaExpiracion")));
        reserva.setFechaConfirmacion(toLocalDateTime(rs.getTimestamp("fechaConfirmacion")));
        reserva.setFechaLiberacion(toLocalDateTime(rs.getTimestamp("fechaLiberacion")));
        reserva.setMotivoLiberacion(rs.getString("motivoLiberacion"));
        reserva.setTokenIdempotencia(rs.getString("tokenIdempotencia"));
        return reserva;
    };

    private final RowMapper<InventarioReservaDetalle> detalleMapper = (rs, rowNum) -> {
        InventarioReservaDetalle detalle = new InventarioReservaDetalle();
        detalle.setIdReservaDetalle(rs.getInt("idReservaDetalle"));
        detalle.setIdReserva(rs.getInt("idReserva"));
        detalle.setIdOrdenDetalle(rs.getInt("idOrdenDetalle"));
        detalle.setIdProducto(rs.getInt("idProducto"));
        detalle.setIdPrecioPresentacion(rs.getInt("idPrecioPresentacion"));
        detalle.setIdUnidad((Integer) rs.getObject("idUnidad"));
        detalle.setNombreProducto(rs.getString("nombreProducto"));
        detalle.setNombrePresentacion(rs.getString("nombrePresentacion"));
        detalle.setTipoCalculoVenta(rs.getString("tipoCalculoVenta"));
        detalle.setFactorConversionStock(rs.getBigDecimal("factorConversionStock"));
        detalle.setCantidadSolicitada(rs.getBigDecimal("cantidadSolicitada"));
        detalle.setCantidadReservadaStock(rs.getBigDecimal("cantidadReservadaStock"));
        return detalle;
    };

    @Override
    public int registrarReserva(InventarioReserva reserva) {
        String sql = """
                INSERT INTO inventario_reserva (
                    codigoReserva, idOrdenOnline, idCliente, idSede, estado,
                    fechaExpiracion, tokenIdempotencia
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, reserva.getCodigoReserva());
            ps.setObject(2, reserva.getIdOrdenOnline());
            ps.setObject(3, reserva.getIdCliente());
            ps.setObject(4, reserva.getIdSede());
            ps.setString(5, reserva.getEstado());
            ps.setTimestamp(6, toTimestamp(reserva.getFechaExpiracion()));
            ps.setString(7, reserva.getTokenIdempotencia());
            return ps;
        }, keyHolder);

        return Objects.requireNonNull(keyHolder.getKey()).intValue();
    }

    @Override
    public int registrarDetalle(InventarioReservaDetalle detalle) {
        String sql = """
                INSERT INTO inventario_reserva_detalle (
                    idReserva, idOrdenDetalle, idProducto, idPrecioPresentacion, idUnidad,
                    nombreProducto, nombrePresentacion, tipoCalculoVenta, factorConversionStock,
                    cantidadSolicitada, cantidadReservadaStock
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setObject(1, detalle.getIdReserva());
            ps.setObject(2, detalle.getIdOrdenDetalle());
            ps.setObject(3, detalle.getIdProducto());
            ps.setObject(4, detalle.getIdPrecioPresentacion());
            ps.setObject(5, detalle.getIdUnidad());
            ps.setString(6, detalle.getNombreProducto());
            ps.setString(7, detalle.getNombrePresentacion());
            ps.setString(8, detalle.getTipoCalculoVenta());
            ps.setBigDecimal(9, detalle.getFactorConversionStock());
            ps.setBigDecimal(10, detalle.getCantidadSolicitada());
            ps.setBigDecimal(11, detalle.getCantidadReservadaStock());
            return ps;
        }, keyHolder);

        return Objects.requireNonNull(keyHolder.getKey()).intValue();
    }

    @Override
    public InventarioReserva buscarPorId(Integer idReserva) {
        return queryReserva("WHERE idReserva = ?", idReserva);
    }

    @Override
    public InventarioReserva buscarPorCodigo(String codigoReserva) {
        return queryReserva("WHERE codigoReserva = ?", codigoReserva);
    }

    @Override
    public InventarioReserva buscarActivaPorOrden(Integer idOrdenOnline) {
        return queryReserva("""
                WHERE idOrdenOnline = ?
                  AND estado = 'ACTIVA'
                  AND fechaExpiracion > NOW()
                ORDER BY idReserva DESC
                LIMIT 1
                """, idOrdenOnline);
    }

    @Override
    public List<InventarioReservaDetalle> listarDetalles(Integer idReserva) {
        String sql = "SELECT " + DETALLE_COLUMNS + " FROM inventario_reserva_detalle WHERE idReserva = ? ORDER BY idReservaDetalle";
        return jdbcTemplate.query(sql, detalleMapper, idReserva);
    }

    @Override
    public int confirmarReservaSiActiva(Integer idReserva) {
        String sql = """
                UPDATE inventario_reserva
                SET estado = 'CONFIRMADA',
                    fechaConfirmacion = NOW()
                WHERE idReserva = ?
                  AND estado = 'ACTIVA'
                  AND fechaExpiracion > NOW()
                """;
        return jdbcTemplate.update(sql, idReserva);
    }

    @Override
    public int liberarReservaSiActiva(Integer idReserva, String nuevoEstado, String motivoLiberacion) {
        String sql = """
                UPDATE inventario_reserva
                SET estado = ?,
                    fechaLiberacion = NOW(),
                    motivoLiberacion = ?
                WHERE idReserva = ?
                  AND estado = 'ACTIVA'
                """;
        return jdbcTemplate.update(sql, nuevoEstado, motivoLiberacion, idReserva);
    }

    @Override
    public List<InventarioReserva> listarExpiradasActivas(LocalDateTime ahora, int limite) {
        String sql = """
                SELECT %s
                FROM inventario_reserva
                WHERE estado = 'ACTIVA'
                  AND fechaExpiracion <= ?
                ORDER BY fechaExpiracion ASC
                LIMIT ?
                """.formatted(RESERVA_COLUMNS);
        return jdbcTemplate.query(sql, reservaMapper, toTimestamp(ahora), limite);
    }

    @Override
    public List<InventarioReserva> listarActivasVencidas(LocalDateTime ahora, int limite) {
        return listarExpiradasActivas(ahora, limite);
    }

    @Override
    public int expirarReservaSiVencida(Integer idReserva, LocalDateTime ahora) {
        String sql = """
                UPDATE inventario_reserva
                SET estado = 'EXPIRADA',
                    fechaLiberacion = NOW(),
                    motivoLiberacion = 'Reserva expirada'
                WHERE idReserva = ?
                  AND estado = 'ACTIVA'
                  AND fechaExpiracion <= ?
                """;
        return jdbcTemplate.update(sql, idReserva, toTimestamp(ahora));
    }

    @Override
    public InventarioReserva bloquearPorId(Integer idReserva) {
        return queryReserva("WHERE idReserva = ? FOR UPDATE", idReserva);
    }

    @Override
    public BigDecimal sumarReservadoActivoPorProductoYSede(Integer idProducto, Integer idSede) {
        String sql = """
                SELECT COALESCE(SUM(rd.cantidadReservadaStock), 0)
                FROM inventario_reserva_detalle rd
                INNER JOIN inventario_reserva r ON r.idReserva = rd.idReserva
                WHERE rd.idProducto = ?
                  AND r.idSede = ?
                  AND r.estado = 'ACTIVA'
                  AND r.fechaExpiracion > NOW()
                """;
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, idProducto, idSede);
    }

    @Override
    public BigDecimal sumarReservadoActivoExcluyendoReserva(Integer idProducto, Integer idSede, Integer idReservaExcluir) {
        String sql = """
                SELECT COALESCE(SUM(rd.cantidadReservadaStock), 0)
                FROM inventario_reserva_detalle rd
                INNER JOIN inventario_reserva r ON r.idReserva = rd.idReserva
                WHERE rd.idProducto = ?
                  AND r.idSede = ?
                  AND r.estado = 'ACTIVA'
                  AND r.fechaExpiracion > NOW()
                  AND (? IS NULL OR r.idReserva <> ?)
                """;
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, idProducto, idSede, idReservaExcluir, idReservaExcluir);
    }

    private InventarioReserva queryReserva(String whereClause, Object... params) {
        try {
            String sql = "SELECT " + RESERVA_COLUMNS + " FROM inventario_reserva " + whereClause;
            return jdbcTemplate.queryForObject(sql, reservaMapper, params);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value != null ? Timestamp.valueOf(value) : null;
    }
}
