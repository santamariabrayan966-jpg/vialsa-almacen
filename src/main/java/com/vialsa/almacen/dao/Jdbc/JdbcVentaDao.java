package com.vialsa.almacen.dao.Jdbc;

import com.vialsa.almacen.dao.interfaces.IVentaDao;
import com.vialsa.almacen.model.Venta;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class JdbcVentaDao implements IVentaDao {

    private final JdbcTemplate jdbc;

    public JdbcVentaDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ======================================
    //   MAPPER PROFESIONAL
    // ======================================
    private Venta mapVenta(ResultSet rs) throws SQLException {
        Venta v = new Venta();

        v.setIdVentas(rs.getInt("idVentas"));

        // Evitar NullPointer si no existe FechaVenta
        if (rs.getTimestamp("FechaVenta") != null) {
            v.setFechaVenta(rs.getTimestamp("FechaVenta").toLocalDateTime());
        }

        v.setTipoComprobante(rs.getString("TipoComprobante"));
        v.setNroComprobante(rs.getString("NroComprobante"));

        // idClientes puede ser NULL → usar getObject()
        Integer idCli = (Integer) rs.getObject("idClientes");
        v.setIdCliente(idCli);

        v.setIdUsuario(rs.getInt("idUsuario"));

        v.setFormaPago(rs.getString("FormaPago"));
        v.setEstadoVenta(rs.getString("EstadoVenta"));
        v.setTotalVenta(rs.getBigDecimal("TotalVenta"));
        v.setDeuda(rs.getBigDecimal("Deuda"));

        // Datos auxiliares (pueden ser NULL si no hay cliente)
        v.setNombreCliente(rs.getString("nombreCliente"));
        v.setNombreUsuario(rs.getString("nombreUsuario"));

        return v;
    }

    // ======================================
    //      LISTAR TODAS LAS VENTAS
    // ======================================
    @Override
    public List<Venta> listar() {
        String sql = """
            SELECT v.*,
                   c.Nombres AS nombreCliente,
                   u.NombreUsuario AS nombreUsuario
            FROM ventas v
            LEFT JOIN clientes c ON v.idClientes = c.idClientes   -- ✔ permite NULL
            INNER JOIN usuarios u ON v.idUsuario = u.idUsuario
            ORDER BY v.FechaVenta DESC
        """;

        return jdbc.query(sql, (rs, rowNum) -> mapVenta(rs));
    }

    // ======================================
    //      BUSCAR POR ID
    // ======================================
    @Override
    public Venta buscarPorId(int idVenta) {
        try {
            String sql = """
                SELECT v.*,
                       c.Nombres AS nombreCliente,
                       u.NombreUsuario AS nombreUsuario
                FROM ventas v
                LEFT JOIN clientes c ON v.idClientes = c.idClientes   -- ✔ permite cliente NULL
                INNER JOIN usuarios u ON v.idUsuario = u.idUsuario
                WHERE v.idVentas = ?
            """;

            return jdbc.queryForObject(sql, (rs, rowNum) -> mapVenta(rs), idVenta);

        } catch (Exception e) {
            System.err.println("⚠️ Venta no encontrada en DB (ID = " + idVenta + ")");
            return null;
        }
    }

    // ======================================
    //   REGISTRAR BORRADOR DE VENTA
    // ======================================
    @Override
    public int registrarBorrador(Venta v) {
        String sql = """
            INSERT INTO ventas (FechaVenta, TipoComprobante, NroComprobante,
                                idClientes, idUsuario, FormaPago,
                                EstadoVenta, TotalVenta, Deuda)
            VALUES (?, ?, ?, ?, ?, ?, 'BORRADOR', ?, ?)
        """;

        return jdbc.update(sql,
                LocalDateTime.now(),
                v.getTipoComprobante(),
                v.getNroComprobante(),
                v.getIdCliente(),     // ✔ puede ser NULL
                v.getIdUsuario(),
                v.getFormaPago(),
                v.getTotalVenta(),
                v.getDeuda()
        );
    }

    @Override
    public int registrarYObtenerId(Venta v) {
        registrarBorrador(v);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    // ======================================
    //     ACTUALIZAR VENTA
    // ======================================
    @Override
    public int actualizarVenta(Venta v) {
        String sql = """
            UPDATE ventas
            SET TipoComprobante=?, NroComprobante=?, FormaPago=?,
                TotalVenta=?, Deuda=?, idClientes=?, idUsuario=?
            WHERE idVentas=?
        """;

        return jdbc.update(sql,
                v.getTipoComprobante(),
                v.getNroComprobante(),
                v.getFormaPago(),
                v.getTotalVenta(),
                v.getDeuda(),
                v.getIdCliente(),     // ✔ soporta NULL
                v.getIdUsuario(),
                v.getIdVentas()
        );
    }

    // ======================================
    //       ACTUALIZAR ESTADO
    // ======================================
    @Override
    public int actualizarEstado(int idVenta, String estado) {
        String sql = "UPDATE ventas SET EstadoVenta=? WHERE idVentas=?";
        return jdbc.update(sql, estado, idVenta);
    }

    // ======================================
    //       ACTUALIZAR DEUDA
    // ======================================
    @Override
    public int actualizarDeuda(int idVenta, BigDecimal deuda) {
        String sql = "UPDATE ventas SET Deuda=? WHERE idVentas=?";
        return jdbc.update(sql, deuda, idVenta);
    }

    // ======================================
    //   REGISTRAR PAGO INICIAL (CRÉDITO)
    // ======================================
    @Override
    public int registrarPagoInicial(int idVenta, BigDecimal pagoInicial) {
        String sql = "UPDATE ventas SET pagoInicial=? WHERE idVentas=?";
        return jdbc.update(sql, pagoInicial, idVenta);
    }

    // ======================================
    //   GENERAR SIGUIENTE COMPROBANTE
    // ======================================
    @Override
    public String obtenerSiguienteComprobante(String tipo) {
        String sql = """
            SELECT NroComprobante
            FROM ventas
            WHERE TipoComprobante=?
            ORDER BY idVentas DESC
            LIMIT 1
        """;

        try {
            String ultimo = jdbc.queryForObject(sql, String.class, tipo);

            if (ultimo == null) return "001-000001";

            String[] partes = ultimo.split("-");
            int correlativo = Integer.parseInt(partes[1]) + 1;

            return partes[0] + "-" + String.format("%06d", correlativo);

        } catch (Exception e) {
            return "001-000001";
        }
    }

    // ======================================
    //   ELIMINAR BORRADOR
    // ======================================
    @Override
    public int eliminarVenta(int idVenta) {
        String sql = "DELETE FROM ventas WHERE idVentas=? AND EstadoVenta='BORRADOR'";
        return jdbc.update(sql, idVenta);
    }

    // ======================================
    //   ANULAR VENTA
    // ======================================
    @Override
    public int anularVenta(int idVenta) {
        return jdbc.update("UPDATE ventas SET EstadoVenta='ANULADA' WHERE idVentas=?", idVenta);
    }

    // ======================================
    //   OBTENER ID USUARIO
    // ======================================
    @Override
    public Integer obtenerIdUsuarioPorNombre(String nombreUsuario) {
        try {
            return jdbc.queryForObject(
                    "SELECT idUsuario FROM usuarios WHERE NombreUsuario=?",
                    Integer.class,
                    nombreUsuario
            );
        } catch (Exception e) {
            return null;
        }
    }
}
