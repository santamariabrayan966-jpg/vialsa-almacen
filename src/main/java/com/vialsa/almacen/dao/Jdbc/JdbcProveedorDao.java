package com.vialsa.almacen.dao.Jdbc;

import com.vialsa.almacen.dao.interfaces.IProveedorDao;
import com.vialsa.almacen.model.Proveedor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JdbcProveedorDao implements IProveedorDao {

    private final JdbcTemplate jdbcTemplate;

    public JdbcProveedorDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Mapea una fila de la tabla a un objeto Proveedor
    private final RowMapper<Proveedor> mapRow = (rs, rowNum) -> {
        Proveedor p = new Proveedor();
        p.setIdProveedor(rs.getInt("idProveedor"));
        p.setNombreProveedor(rs.getString("NombreProveedor"));
        p.setNroDocumento(rs.getString("NroDocumento"));
        p.setDireccion(rs.getString("Direccion"));
        p.setTelefono(rs.getString("Telefono"));
        p.setCorreo(rs.getString("Correo"));
        p.setActivo(rs.getBoolean("activo"));
        return p;
    };

    @Override
    public List<Proveedor> listar() {
        String sql = """
            SELECT idProveedor, NombreProveedor, NroDocumento,
                   Direccion, Telefono, Correo, activo
            FROM proveedores
            WHERE activo = 1
        """;
        return jdbcTemplate.query(sql, mapRow);
    }

    @Override
    public Optional<Proveedor> buscarPorId(Integer id) {
        String sql = """
            SELECT idProveedor, NombreProveedor, NroDocumento,
                   Direccion, Telefono, Correo, activo
            FROM proveedores
            WHERE idProveedor = ?
        """;
        List<Proveedor> proveedores = jdbcTemplate.query(sql, mapRow, id);
        return proveedores.isEmpty()
                ? Optional.empty()
                : Optional.of(proveedores.get(0));
    }

    @Override
    public Optional<Proveedor> buscarPorNroDocumento(String nroDocumento) {
        String sql = """
            SELECT idProveedor, NombreProveedor, NroDocumento,
                   Direccion, Telefono, Correo, activo
            FROM proveedores
            WHERE NroDocumento = ?
        """;
        List<Proveedor> list = jdbcTemplate.query(sql, mapRow, nroDocumento);
        return list.isEmpty()
                ? Optional.empty()
                : Optional.of(list.get(0));
    }

    @Override
    public int crear(Proveedor proveedor) {
        String sql = """
            INSERT INTO proveedores
                (NombreProveedor, NroDocumento, Direccion, Telefono, Correo, activo)
            VALUES (?, ?, ?, ?, ?, 1)
        """;
        return jdbcTemplate.update(sql,
                proveedor.getNombreProveedor(),
                proveedor.getNroDocumento(),
                proveedor.getDireccion(),
                proveedor.getTelefono(),
                proveedor.getCorreo()
        );
    }

    @Override
    public int actualizar(Proveedor proveedor) {
        String sql = """
            UPDATE proveedores SET
                NombreProveedor = ?,
                NroDocumento   = ?,
                Direccion      = ?,
                Telefono       = ?,
                Correo         = ?,
                activo         = ?
            WHERE idProveedor = ?
        """;
        return jdbcTemplate.update(sql,
                proveedor.getNombreProveedor(),
                proveedor.getNroDocumento(),
                proveedor.getDireccion(),
                proveedor.getTelefono(),
                proveedor.getCorreo(),
                proveedor.isActivo(),
                proveedor.getIdProveedor()
        );
    }

    @Override
    public int eliminar(Integer id) {
        // Soft delete: solo marcamos activo = 0
        String sql = "UPDATE proveedores SET activo = 0 WHERE idProveedor = ?";
        return jdbcTemplate.update(sql, id);
    }
}
