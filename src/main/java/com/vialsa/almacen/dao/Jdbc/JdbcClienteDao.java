package com.vialsa.almacen.dao.Jdbc;

import com.vialsa.almacen.dao.interfaces.IClienteDao;
import com.vialsa.almacen.model.Cliente;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class JdbcClienteDao implements IClienteDao {

    private final JdbcTemplate jdbc;

    public JdbcClienteDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ✅ Mapeo correcto según tu tabla "clientes"
    private static class ClienteMapper implements RowMapper<Cliente> {
        @Override
        public Cliente mapRow(ResultSet rs, int rowNum) throws SQLException {
            Cliente c = new Cliente();
            c.setIdClientes(rs.getInt("idClientes"));
            c.setNombres(rs.getString("nombres"));
            c.setApellidos(rs.getString("apellidos"));
            c.setNro_documento(rs.getString("nro_documento")); // ✅ campo correcto
            c.setDireccion(rs.getString("direccion"));
            c.setTelefono(rs.getString("telefono"));
            c.setCorreo(rs.getString("correo"));
            return c;
        }
    }

    // ✅ Buscar cliente por número de documento
    @Override
    public Cliente buscarPorDocumento(String documento) {
        String sql = "SELECT * FROM clientes WHERE nro_documento = ?";
        try {
            return jdbc.queryForObject(sql, new ClienteMapper(), documento);
        } catch (Exception e) {
            return null;
        }
    }

    // ✅ Registrar nuevo cliente
    @Override
    public int registrar(Cliente cliente) {
        String sql = "INSERT INTO clientes (nombres, apellidos, nro_documento, direccion, telefono, correo, fecha_registro) "
                + "VALUES (?, ?, ?, ?, ?, ?, NOW())";
        return jdbc.update(sql,
                cliente.getNombres(),
                cliente.getApellidos(),
                cliente.getNro_documento(),
                cliente.getDireccion(),
                cliente.getTelefono(),
                cliente.getCorreo());
    }
}
