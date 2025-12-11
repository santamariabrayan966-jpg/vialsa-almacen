package com.vialsa.almacen.dao.Jdbc;

import com.vialsa.almacen.dao.interfaces.ICuotaCompraDao;
import com.vialsa.almacen.model.CuotaCompra;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Repository
public class JdbcCuotaCompraDao implements ICuotaCompraDao {

    private final JdbcTemplate jdbcTemplate;

    public JdbcCuotaCompraDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // =============================================================
    // MAPPER
    // =============================================================
    private static class CuotaMapper implements RowMapper<CuotaCompra> {
        @Override
        public CuotaCompra mapRow(ResultSet rs, int rowNum) throws SQLException {

            CuotaCompra c = new CuotaCompra();

            c.setIdCuotaCompra(rs.getInt("idCuotaCompra"));
            c.setIdCompra(rs.getInt("idCompra"));
            c.setNumeroCuota(rs.getInt("nroCuota"));
            c.setFechaVencimiento(rs.getDate("fechaVencimiento").toLocalDate());
            c.setMontoCuota(rs.getBigDecimal("montoCuota"));
            c.setEstado(rs.getString("estado"));

            if (rs.getDate("fechaPago") != null) {
                c.setFechaPago(rs.getDate("fechaPago").toLocalDate());
            }

            c.setMetodoPago(rs.getString("metodoPago"));
            c.setObservacion(rs.getString("observacion"));

            return c;
        }
    }

    // =============================================================
    // REGISTRAR CUOTA
    // =============================================================
    @Override
    public int registrar(CuotaCompra cuota) {

        String sql = """
            INSERT INTO cuotas_compra
            (idCompra, nroCuota, fechaVencimiento, montoCuota, estado, fechaPago, metodoPago, observacion)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        return jdbcTemplate.update(sql,
                cuota.getIdCompra(),
                cuota.getNumeroCuota(),
                java.sql.Date.valueOf(cuota.getFechaVencimiento()),
                cuota.getMontoCuota(),
                cuota.getEstado(),
                cuota.getFechaPago() != null ? java.sql.Date.valueOf(cuota.getFechaPago()) : null,
                cuota.getMetodoPago(),
                cuota.getObservacion()
        );
    }

    // =============================================================
    // LISTAR POR COMPRA
    // =============================================================
    @Override
    public List<CuotaCompra> listarPorCompra(int idCompra) {
        return jdbcTemplate.query(
                "SELECT * FROM cuotas_compra WHERE idCompra = ? ORDER BY nroCuota",
                new CuotaMapper(),
                idCompra
        );
    }

    // =============================================================
    // BUSCAR POR ID (NECESARIO PARA PAGAR)
    // =============================================================
    @Override
    public CuotaCompra buscarPorId(int idCuotaCompra) {

        String sql = "SELECT * FROM cuotas_compra WHERE idCuotaCompra = ?";

        return jdbcTemplate.queryForObject(sql, new CuotaMapper(), idCuotaCompra);
    }

    // =============================================================
    // MARCAR CUOTA COMO PAGADA
    // =============================================================
    @Override
    public int marcarComoPagada(int idCuotaCompra) {

        String sql = """
            UPDATE cuotas_compra
            SET estado = 'PAGADA',
                fechaPago = ?
            WHERE idCuotaCompra = ?
        """;

        return jdbcTemplate.update(sql,
                java.sql.Date.valueOf(LocalDate.now()),
                idCuotaCompra
        );
    }

    // =============================================================
    // DESCONTAR DEUDA DE LA COMPRA
    // =============================================================
    public int descontarDeuda(int idCompra, java.math.BigDecimal monto) {

        String sql = """
            UPDATE compras
            SET deuda = deuda - ?
            WHERE idCompra = ?
        """;

        return jdbcTemplate.update(sql, monto, idCompra);
    }

    // =============================================================
    // ELIMINAR CUOTAS (correcto)
    // =============================================================
    @Override
    public void eliminarPorCompra(int idCompra) {
        jdbcTemplate.update("DELETE FROM cuotas_compra WHERE idCompra = ?", idCompra);
    }
}
