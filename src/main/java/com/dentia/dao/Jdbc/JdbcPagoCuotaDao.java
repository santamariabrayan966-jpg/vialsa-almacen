package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.IPagoCuotaDao;
import com.dentia.model.interno.PagoCuota;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcPagoCuotaDao implements IPagoCuotaDao {

    private final JdbcTemplate jdbc;

    public JdbcPagoCuotaDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public int registrar(PagoCuota pago) {
        String sql = """
            INSERT INTO pagos_cuota (
                idCuotaVenta,
                idVenta,
                montoPagado,
                metodoPago,
                fechaPago,
                referencia,
                observacion,
                idUsuario
            )
            VALUES (?, ?, ?, ?, NOW(), ?, ?, ?)
        """;

        return jdbc.update(sql,
                pago.getIdCuotaVenta(),
                pago.getIdVenta(),
                pago.getMontoPagado(),
                pago.getMetodoPago(),
                pago.getReferencia(),
                pago.getObservacion(),
                pago.getIdUsuario()
        );
    }

    @Override
    public List<PagoCuota> listarPorCuota(Integer idCuotaVenta) {
        String sql = """
            SELECT *
            FROM pagos_cuota
            WHERE idCuotaVenta = ?
            ORDER BY fechaPago DESC
        """;

        return jdbc.query(
                sql,
                new BeanPropertyRowMapper<>(PagoCuota.class),
                idCuotaVenta
        );
    }

    @Override
    public List<PagoCuota> listarPorVenta(Integer idVenta) {
        String sql = """
            SELECT *
            FROM pagos_cuota
            WHERE idVenta = ?
            ORDER BY fechaPago DESC
        """;

        return jdbc.query(
                sql,
                new BeanPropertyRowMapper<>(PagoCuota.class),
                idVenta
        );
    }
}