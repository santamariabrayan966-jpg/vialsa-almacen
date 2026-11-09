package com.dentia.dao.Jdbc;

import com.dentia.model.interno.CuotaVenta;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class JdbcCuotaVentaDao {

    private final JdbcTemplate jdbc;

    public JdbcCuotaVentaDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<CuotaVenta> listarPorVenta(Integer idVenta) {
        String sql = """
            SELECT *
            FROM cuotas_venta
            WHERE idVenta = ?
            ORDER BY nroCuota
        """;

        return jdbc.query(sql, new BeanPropertyRowMapper<>(CuotaVenta.class), idVenta);
    }

    public CuotaVenta buscarPorId(Integer idCuotaVenta) {
        String sql = """
            SELECT *
            FROM cuotas_venta
            WHERE idCuotaVenta = ?
        """;

        List<CuotaVenta> lista = jdbc.query(
                sql,
                new BeanPropertyRowMapper<>(CuotaVenta.class),
                idCuotaVenta
        );

        return lista.isEmpty() ? null : lista.get(0);
    }

    public void registrarCuotas(Integer idVenta,
                                List<Integer> nroCuota,
                                List<String> fechaVencimiento,
                                List<BigDecimal> montoCuota) {

        if (nroCuota == null || nroCuota.isEmpty()) return;

        jdbc.update("DELETE FROM cuotas_venta WHERE idVenta = ?", idVenta);

        String sql = """
            INSERT INTO cuotas_venta (
                idVenta,
                nroCuota,
                fechaVencimiento,
                montoCuota,
                montoPagado,
                saldoPendiente,
                estado
            )
            VALUES (?, ?, ?, ?, 0.00, ?, 'PENDIENTE')
        """;

        for (int i = 0; i < nroCuota.size(); i++) {
            jdbc.update(
                    sql,
                    idVenta,
                    nroCuota.get(i),
                    fechaVencimiento.get(i),
                    montoCuota.get(i),
                    montoCuota.get(i)
            );
        }
    }

    public void actualizarPagoCuota(CuotaVenta cuota) {
        String sql = """
            UPDATE cuotas_venta
            SET montoPagado = ?,
                saldoPendiente = ?,
                estado = ?,
                fechaPago = ?,
                metodoPago = ?
            WHERE idCuotaVenta = ?
        """;

        jdbc.update(
                sql,
                cuota.getMontoPagado(),
                cuota.getSaldoPendiente(),
                cuota.getEstado(),
                cuota.getFechaPago(),
                cuota.getMetodoPago(),
                cuota.getIdCuotaVenta()
        );
    }
}