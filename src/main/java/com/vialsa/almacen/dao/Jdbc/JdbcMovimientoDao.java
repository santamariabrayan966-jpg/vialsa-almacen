package com.vialsa.almacen.dao.Jdbc;

import com.vialsa.almacen.dao.interfaces.IMovimientoDao;
import com.vialsa.almacen.model.Movimiento;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class JdbcMovimientoDao implements IMovimientoDao {

    private final JdbcTemplate jdbc;

    public JdbcMovimientoDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ============================================================
    //  MAPPER actualizado con los nuevos campos
    // ============================================================
    private static class MovimientoMapper implements RowMapper<Movimiento> {
        @Override
        public Movimiento mapRow(ResultSet rs, int rowNum) throws SQLException {
            Movimiento m = new Movimiento();
            m.setIdMovimientosAlmacen(rs.getInt("idMovimientosAlmacen"));
            m.setTipoMovimiento(rs.getString("TipoMovimiento"));
            m.setCantidad(rs.getBigDecimal("Cantidad"));
            m.setFecha(rs.getTimestamp("Fecha").toLocalDateTime());
            m.setIdProducto(rs.getInt("idProducto"));
            m.setIdUnidad(rs.getInt("idUnidad"));
            m.setIdUsuario(rs.getInt("idUsuario"));
            m.setNombreProducto(rs.getString("NombreProducto"));

            // 🆕 Campos nuevos de la tabla movimientosalmacen
            m.setOrigen(rs.getString("origen"));
            m.setIdDocumento(rs.getObject("idDocumento") != null ? rs.getInt("idDocumento") : null);
            m.setObservacion(rs.getString("observacion"));
            m.setStockAntes(rs.getBigDecimal("stockAntes"));
            m.setStockDespues(rs.getBigDecimal("stockDespues"));

            return m;
        }
    }

    // ============================================================
    //     LISTAR (incluye los nuevos campos)
    // ============================================================
    @Override
    public List<Movimiento> listar() {
        String sql = """
            SELECT m.idMovimientosAlmacen, m.TipoMovimiento, m.Cantidad, m.Fecha,
                   m.idProducto, m.idUnidad, m.idUsuario,
                   m.origen, m.idDocumento, m.observacion,
                   m.stockAntes, m.stockDespues,
                   p.NombreProducto
            FROM movimientosalmacen m
            INNER JOIN productos p ON m.idProducto = p.idProducto
            ORDER BY m.Fecha DESC
        """;

        return jdbc.query(sql, new MovimientoMapper());
    }

    // ============================================================
    //     REGISTRAR (ahora guarda stockAntes y stockDespues)
    // ============================================================
    @Override
    public int registrar(Movimiento m) {

        // 1️⃣ Obtener stock actual ANTES del movimiento
        BigDecimal stockActual = jdbc.queryForObject(
                "SELECT StockActual FROM productos WHERE idProducto = ?",
                BigDecimal.class,
                m.getIdProducto()
        );

        BigDecimal stockAntes = stockActual;
        BigDecimal stockDespues;

        if (m.getTipoMovimiento().equalsIgnoreCase("ENTRADA")) {
            stockDespues = stockActual.add(m.getCantidad());
            m.setOrigen("MANUAL");
        } else {
            stockDespues = stockActual.subtract(m.getCantidad());
            m.setOrigen("MANUAL");
        }

        m.setStockAntes(stockAntes);
        m.setStockDespues(stockDespues);

        // 2️⃣ Registrar movimiento con todos los campos nuevos
        int result = jdbc.update("""
            INSERT INTO movimientosalmacen 
            (TipoMovimiento, Cantidad, Fecha, idProducto, idUnidad, idUsuario,
             origen, idDocumento, observacion, stockAntes, stockDespues)
            VALUES (?, ?, NOW(), ?, ?, ?, ?, ?, ?, ?, ?)
        """,
                m.getTipoMovimiento(),
                m.getCantidad(),
                m.getIdProducto(),
                m.getIdUnidad(),
                m.getIdUsuario(),
                m.getOrigen(),
                m.getIdDocumento(),
                m.getObservacion(),
                m.getStockAntes(),
                m.getStockDespues()
        );

        // 3️⃣ Actualizar stock del producto
        if (result > 0) {
            jdbc.update("UPDATE productos SET StockActual = ? WHERE idProducto = ?",
                    stockDespues, m.getIdProducto());
        }

        return result;
    }


    // ============================================================
    //  Obtener ID usuario por nombre
    // ============================================================
    @Override
    public Integer obtenerIdUsuarioPorNombre(String nombreUsuario) {
        try {
            return jdbc.queryForObject(
                    "SELECT idUsuario FROM usuarios WHERE NombreUsuario = ?",
                    Integer.class,
                    nombreUsuario
            );
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }


    // ============================================================
    //  NUEVO — Buscar movimiento por ID
    // ============================================================
    @Override
    public Movimiento buscarPorId(Integer id) {
        try {
            return jdbc.queryForObject("""
                SELECT m.*, p.NombreProducto
                FROM movimientosalmacen m
                INNER JOIN productos p ON m.idProducto = p.idProducto
                WHERE m.idMovimientosAlmacen = ?
            """, new MovimientoMapper(), id);

        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    // ============================================================
    //  NUEVO — Listar por producto
    // ============================================================
    @Override
    public List<Movimiento> listarPorProducto(Integer idProducto) {
        return jdbc.query("""
                SELECT m.*, p.NombreProducto
                FROM movimientosalmacen m
                INNER JOIN productos p ON m.idProducto = p.idProducto
                WHERE m.idProducto = ?
                ORDER BY m.Fecha DESC
        """, new MovimientoMapper(), idProducto);
    }
    @Override
    public BigDecimal obtenerStockActual(Integer idProducto) {
        try {
            return jdbc.queryForObject(
                    "SELECT StockActual FROM productos WHERE idProducto = ?",
                    BigDecimal.class,
                    idProducto
            );
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

}
