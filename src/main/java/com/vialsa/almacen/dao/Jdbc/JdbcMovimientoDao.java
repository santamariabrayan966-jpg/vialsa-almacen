package com.vialsa.almacen.dao.Jdbc;

import com.vialsa.almacen.dao.interfaces.IMovimientoDao;
import com.vialsa.almacen.model.Movimiento;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class JdbcMovimientoDao implements IMovimientoDao {

    private final JdbcTemplate jdbc;

    public JdbcMovimientoDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ✅ Mapper con JOIN para incluir el nombre del producto
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
            m.setNombreProducto(rs.getString("NombreProducto")); // 🔹 Campo adicional del JOIN
            return m;
        }
    }

    // 📋 Listar todos los movimientos de inventario
    @Override
    public List<Movimiento> listar() {
        String sql = """
            SELECT m.idMovimientosAlmacen, m.TipoMovimiento, m.Cantidad, m.Fecha,
                   m.idProducto, m.idUnidad, m.idUsuario,
                   p.NombreProducto
            FROM movimientosalmacen m
            INNER JOIN productos p ON m.idProducto = p.idProducto
            ORDER BY m.Fecha DESC
        """;
        return jdbc.query(sql, new MovimientoMapper());
    }

    // 💾 Registrar un movimiento y actualizar stock
    @Override
    public int registrar(Movimiento m) {
        int result = jdbc.update("""
            INSERT INTO movimientosalmacen (TipoMovimiento, Cantidad, Fecha, idProducto, idUnidad, idUsuario)
            VALUES (?, ?, NOW(), ?, ?, ?)
        """, m.getTipoMovimiento(), m.getCantidad(), m.getIdProducto(), m.getIdUnidad(), m.getIdUsuario());

        // ✅ Actualizar stock si el registro fue exitoso
        if (result > 0) {
            String sqlStock;
            if ("ENTRADA".equalsIgnoreCase(m.getTipoMovimiento())) {
                sqlStock = "UPDATE productos SET StockActual = StockActual + ? WHERE idProducto = ?";
            } else if ("SALIDA".equalsIgnoreCase(m.getTipoMovimiento())) {
                sqlStock = "UPDATE productos SET StockActual = StockActual - ? WHERE idProducto = ?";
            } else {
                throw new IllegalArgumentException("Tipo de movimiento inválido: " + m.getTipoMovimiento());
            }

            jdbc.update(sqlStock, m.getCantidad(), m.getIdProducto());
        }

        return result;
    }

    // 🔍 Buscar ID de usuario por su nombre (para el controlador)
    @Override
    public Integer obtenerIdUsuarioPorNombre(String nombreUsuario) {
        String sql = "SELECT idUsuario FROM usuarios WHERE NombreUsuario = ?";
        try {
            return jdbc.queryForObject(sql, Integer.class, nombreUsuario);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}
