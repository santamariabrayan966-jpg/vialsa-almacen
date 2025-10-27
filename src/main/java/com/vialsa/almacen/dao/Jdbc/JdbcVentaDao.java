package com.vialsa.almacen.dao.Jdbc;

import com.vialsa.almacen.dao.interfaces.IVentaDao;
import com.vialsa.almacen.model.Venta;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class JdbcVentaDao implements IVentaDao {

    private final JdbcTemplate jdbc;

    public JdbcVentaDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ✅ Mapeador de ventas
    private static class VentaMapper implements RowMapper<Venta> {
        @Override
        public Venta mapRow(ResultSet rs, int rowNum) throws SQLException {
            Venta v = new Venta();
            v.setIdVentas(rs.getInt("idVentas"));
            v.setFechaVenta(rs.getTimestamp("FechaVenta").toLocalDateTime());
            v.setTipoComprobante(rs.getString("TipoComprobante"));
            v.setNroComprobante(rs.getString("NroComprobante"));
            v.setTotalVenta(rs.getBigDecimal("TotalVenta"));
            v.setNombreCliente(rs.getString("nombreCliente"));
            v.setNombreUsuario(rs.getString("nombreUsuario"));
            return v;
        }
    }

    // 📋 Listar todas las ventas
    @Override
    public List<Venta> listar() {
        String sql = """
            SELECT v.idVentas, v.FechaVenta, v.TipoComprobante, v.NroComprobante,
                   v.TotalVenta,
                   c.Nombres AS nombreCliente,
                   u.NombreUsuario AS nombreUsuario
            FROM ventas v
            INNER JOIN clientes c ON v.idClientes = c.idClientes
            INNER JOIN usuarios u ON v.idUsuario = u.idUsuario
            ORDER BY v.FechaVenta DESC
        """;
        return jdbc.query(sql, new VentaMapper());
    }

    // 💾 Registrar venta simple
    @Override
    public int registrar(Venta v) {
        String sql = """
            INSERT INTO ventas (FechaVenta, TipoComprobante, NroComprobante,
                                TotalVenta, idClientes, idUsuario)
            VALUES (NOW(), ?, ?, ?, ?, ?)
        """;
        return jdbc.update(sql,
                v.getTipoComprobante(),
                v.getNroComprobante(),
                v.getTotalVenta(),
                v.getIdCliente(),
                v.getIdUsuario());
    }

    // 💾 Registrar venta y devolver ID generado
    @Override
    public int registrarYObtenerId(Venta v) {
        String sql = """
            INSERT INTO ventas (FechaVenta, TipoComprobante, NroComprobante,
                                TotalVenta, idClientes, idUsuario)
            VALUES (NOW(), ?, ?, ?, ?, ?)
        """;
        jdbc.update(sql,
                v.getTipoComprobante(),
                v.getNroComprobante(),
                v.getTotalVenta(),
                v.getIdCliente(),
                v.getIdUsuario());

        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    // 🔍 Obtener ID de usuario por nombre (para VentaController)
    @Override
    public Integer obtenerIdUsuarioPorNombre(String nombreUsuario) {
        try {
            String sql = "SELECT idUsuario FROM usuarios WHERE NombreUsuario = ?";
            return jdbc.queryForObject(sql, Integer.class, nombreUsuario);
        } catch (Exception e) {
            System.err.println("⚠️ No se encontró usuario con nombre: " + nombreUsuario);
            return null;
        }
    }

    // 🔍 Buscar una venta por ID
    @Override
    public Venta buscarPorId(int idVenta) {
        String sql = """
            SELECT v.idVentas, v.FechaVenta, v.TipoComprobante, v.NroComprobante,
                   v.TotalVenta,
                   c.Nombres AS nombreCliente,
                   u.NombreUsuario AS nombreUsuario
            FROM ventas v
            INNER JOIN clientes c ON v.idClientes = c.idClientes
            INNER JOIN usuarios u ON v.idUsuario = u.idUsuario
            WHERE v.idVentas = ?
        """;
        try {
            return jdbc.queryForObject(sql, (rs, rowNum) -> {
                Venta v = new Venta();
                v.setIdVentas(rs.getInt("idVentas"));
                v.setFechaVenta(rs.getTimestamp("FechaVenta").toLocalDateTime());
                v.setTipoComprobante(rs.getString("TipoComprobante"));
                v.setNroComprobante(rs.getString("NroComprobante"));
                v.setTotalVenta(rs.getBigDecimal("TotalVenta"));
                v.setNombreCliente(rs.getString("nombreCliente"));
                v.setNombreUsuario(rs.getString("nombreUsuario"));
                return v;
            }, idVenta);
        } catch (Exception e) {
            System.err.println("⚠️ Error al buscar venta por ID (" + idVenta + "): " + e.getMessage());
            return null;
        }
    }
}
