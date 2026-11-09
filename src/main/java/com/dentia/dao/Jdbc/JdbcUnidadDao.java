package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.IUnidadDao;
import com.dentia.model.interno.Unidad;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class JdbcUnidadDao implements IUnidadDao {

    private final JdbcTemplate jdbc;

    public JdbcUnidadDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static class UnidadMapper implements RowMapper<Unidad> {
        @Override
        public Unidad mapRow(ResultSet rs, int rowNum) throws SQLException {
            Unidad u = new Unidad();
            u.setIdUnidad(rs.getInt("idUnidad"));
            u.setNombreUnidad(rs.getString("NombreUnidad"));
            return u;
        }
    }

    @Override
    public List<Unidad> listar() {
        String sql = "SELECT idUnidad, NombreUnidad FROM unidades ORDER BY NombreUnidad ASC";
        return jdbc.query(sql, new UnidadMapper());
    }
}
