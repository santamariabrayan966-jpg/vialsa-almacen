package com.vialsa.almacen.dao.Jdbc;

import com.vialsa.almacen.dao.interfaces.ICompraDao;
import com.vialsa.almacen.model.Compra;
import com.vialsa.almacen.model.DetalleCompra;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class JdbcCompraDao implements ICompraDao {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JdbcCompraDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 📋 Listar todas las compras
    @Override
    public List<Compra> listar() {
        String sql = """
            SELECT 
                c.idCompra,
                c.FechaCompra,
                c.NroComprobante,
                p.NombreProveedor AS nombreProveedor,
                u.NombreUsuario AS nombreUsuario
            FROM compras c
            JOIN proveedores p ON c.idProveedor = p.idProveedor
            JOIN usuarios u ON c.idUsuario = u.idUsuario
            ORDER BY c.FechaCompra DESC
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRowToCompra(rs));
    }

    // 💾 Registrar nueva compra
    @Override
    public int registrar(Compra compra) {
        String sql = "INSERT INTO compras (FechaCompra, NroComprobante, idProveedor, idUsuario) VALUES (?, ?, ?, ?)";
        return jdbcTemplate.update(sql,
                Timestamp.valueOf(compra.getFechaCompra()),
                compra.getNroComprobante(),
                compra.getIdProveedor(),
                compra.getIdUsuario());
    }

    // 💾 Registrar compra y devolver ID generado
    @Override
    public int registrarYObtenerId(Compra compra) {
        String sql = """
        INSERT INTO compras (FechaCompra, NroComprobante, idProveedor, idUsuario)
        VALUES (?, ?, ?, ?)
    """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setTimestamp(1, Timestamp.valueOf(compra.getFechaCompra()));
            ps.setString(2, compra.getNroComprobante());
            ps.setInt(3, compra.getIdProveedor());
            ps.setInt(4, compra.getIdUsuario());
            return ps;
        }, keyHolder);

        return keyHolder.getKey().intValue();
    }


    // 🔍 Buscar compra por ID
    @Override
    public Compra buscarPorId(int idCompra) {
        String sql = """
            SELECT 
                c.idCompra,
                c.FechaCompra,
                c.NroComprobante,
                c.idProveedor,
                c.idUsuario
            FROM compras c
            WHERE c.idCompra = ?
        """;

        List<Compra> compras = jdbcTemplate.query(sql, (rs, rowNum) -> mapRowToCompra(rs), idCompra);
        return compras.isEmpty() ? null : compras.get(0);
    }

    // 📑 Listar detalles de una compra
    @Override
    public List<DetalleCompra> listarPorCompra(int idCompra) {
        String sql = """
            SELECT 
                d.idDetalleCompra,
                d.idProducto,
                d.idUnidad,
                d.Cantidad,
                d.PrecioUnitario,
                d.Descuento,
                p.NombreProducto,
                u.NombreUnidad
            FROM detallecompra d
            JOIN productos p ON d.idProducto = p.idProducto
            JOIN unidades u ON d.idUnidad = u.idUnidad
            WHERE d.idCompra = ?
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            DetalleCompra detalle = new DetalleCompra();
            detalle.setIdDetalleCompra(rs.getInt("idDetalleCompra"));
            detalle.setIdProducto(rs.getInt("idProducto"));
            detalle.setIdUnidad(rs.getInt("idUnidad"));
            detalle.setCantidad(rs.getBigDecimal("Cantidad"));
            detalle.setPrecioUnitario(rs.getBigDecimal("PrecioUnitario"));
            detalle.setDescuento(rs.getBigDecimal("Descuento"));
            detalle.setNombreProducto(rs.getString("NombreProducto"));
            detalle.setNombreUnidad(rs.getString("NombreUnidad"));
            return detalle;
        }, idCompra);
    }

    // 👤 Obtener ID del usuario por su nombre
    @Override
    public Integer obtenerIdUsuarioPorNombre(String nombreUsuario) {
        String sql = "SELECT idUsuario FROM usuarios WHERE NombreUsuario = ? LIMIT 1";
        try {
            return jdbcTemplate.queryForObject(sql, Integer.class, nombreUsuario);
        } catch (Exception e) {
            return null; // Usuario no encontrado
        }
    }

    // 🔧 Mapeador común para Compra
    private Compra mapRowToCompra(ResultSet rs) throws SQLException {
        Compra compra = new Compra();
        compra.setIdCompra(rs.getInt("idCompra"));
        compra.setFechaCompra(rs.getTimestamp("FechaCompra").toLocalDateTime());
        compra.setNroComprobante(rs.getString("NroComprobante"));

        try { compra.setIdProveedor(rs.getInt("idProveedor")); } catch (SQLException ignored) {}
        try { compra.setIdUsuario(rs.getInt("idUsuario")); } catch (SQLException ignored) {}
        try { compra.setNombreProveedor(rs.getString("nombreProveedor")); } catch (SQLException ignored) {}
        try { compra.setNombreUsuario(rs.getString("nombreUsuario")); } catch (SQLException ignored) {}

        return compra;
    }

}
