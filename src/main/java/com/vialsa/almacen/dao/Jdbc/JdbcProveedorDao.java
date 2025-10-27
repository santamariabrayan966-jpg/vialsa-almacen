package com.vialsa.almacen.dao.Jdbc;

import com.vialsa.almacen.dao.interfaces.IProveedorDao;
import com.vialsa.almacen.model.Proveedor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcProveedorDao implements IProveedorDao {

    private final JdbcTemplate jdbcTemplate;

    public JdbcProveedorDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private RowMapper<Proveedor> mapRow = (rs, rowNum) -> {
        Proveedor p = new Proveedor();
        p.setIdProveedor(rs.getInt("idProveedor"));
        p.setNombreProveedor(rs.getString("nombreProveedor"));
        p.setNroDocumento(rs.getString("nroDocumento"));
        p.setDireccion(rs.getString("direccion"));
        p.setTelefono(rs.getString("telefono"));
        p.setCorreo(rs.getString("correo"));
        return p;
    };

    @Override
    public List<Proveedor> listar() {
        String sql = "SELECT * FROM proveedores";
        return jdbcTemplate.query(sql, mapRow);
    }

    @Override
    public Optional<Proveedor> buscarPorId(int id) {
        String sql = "SELECT * FROM proveedores WHERE idProveedor = ?";
        List<Proveedor> proveedores = jdbcTemplate.query(sql, mapRow, id);
        return proveedores.isEmpty() ? Optional.empty() : Optional.of(proveedores.get(0));
    }

    @Override
    public int crear(Proveedor proveedor) {
        String sql = "INSERT INTO proveedores (nombreProveedor, nroDocumento, direccion, telefono, correo) VALUES (?, ?, ?, ?, ?)";
        return jdbcTemplate.update(sql,
                proveedor.getNombreProveedor(),
                proveedor.getNroDocumento(),
                proveedor.getDireccion(),
                proveedor.getTelefono(),
                proveedor.getCorreo());
    }

    @Override
    public int actualizar(Proveedor proveedor) {
        String sql = "UPDATE proveedores SET nombreProveedor=?, nroDocumento=?, direccion=?, telefono=?, correo=? WHERE idProveedor=?";
        return jdbcTemplate.update(sql,
                proveedor.getNombreProveedor(),
                proveedor.getNroDocumento(),
                proveedor.getDireccion(),
                proveedor.getTelefono(),
                proveedor.getCorreo(),
                proveedor.getIdProveedor());
    }

    @Override
    public int eliminar(int id) {
        String sql = "DELETE FROM proveedores WHERE idProveedor=?";
        return jdbcTemplate.update(sql, id);
    }
}
