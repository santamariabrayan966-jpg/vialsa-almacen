package com.vialsa.almacen.dao.Jdbc;

import com.vialsa.almacen.model.Rol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcRolDao {

    private final JdbcTemplate jdbc;

    @Autowired
    public JdbcRolDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // 🔹 Método que faltaba
    public List<Rol> listarRoles() {
        String sql = "SELECT idRol, NombreRol FROM roles";
        return jdbc.query(sql, (rs, rowNum) ->
                new Rol(
                        rs.getInt("idRol"),
                        rs.getString("NombreRol")
                )
        );
    }
}
