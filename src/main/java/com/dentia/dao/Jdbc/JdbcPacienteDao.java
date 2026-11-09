package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.IPacienteDao;
import com.dentia.model.paciente.Paciente;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;
import java.util.List;

@Repository
public class JdbcPacienteDao implements IPacienteDao {

    private final JdbcTemplate jdbc;

    private final RowMapper<Paciente> mapper = (rs, rowNum) -> {
        Paciente paciente = new Paciente();
        paciente.setIdPaciente(rs.getLong("id_paciente"));
        paciente.setDni(rs.getString("dni"));
        paciente.setNombres(rs.getString("nombres"));
        paciente.setApellidos(rs.getString("apellidos"));
        paciente.setFechaNacimiento(rs.getObject("fecha_nacimiento", java.time.LocalDate.class));
        paciente.setDireccion(rs.getString("direccion"));
        paciente.setCelular(rs.getString("celular"));
        paciente.setCorreo(rs.getString("correo"));
        paciente.setActivo(rs.getBoolean("activo"));
        return paciente;
    };

    public JdbcPacienteDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long crear(Paciente paciente) {
        String sql = """
                INSERT INTO pacientes
                    (dni, nombres, apellidos, fecha_nacimiento, direccion, celular, correo, activo)
                VALUES (?, ?, ?, ?, ?, ?, ?, 1)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, paciente.getDni());
            ps.setString(2, paciente.getNombres());
            ps.setString(3, paciente.getApellidos());
            ps.setObject(4, paciente.getFechaNacimiento());
            ps.setString(5, paciente.getDireccion());
            ps.setString(6, paciente.getCelular());
            ps.setString(7, paciente.getCorreo());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("No se pudo obtener el identificador del paciente.");
        return key.longValue();
    }

    @Override
    public boolean existeDni(String dni) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM pacientes WHERE dni = ?", Integer.class, dni);
        return total != null && total > 0;
    }

    @Override
    public Optional<Paciente> buscarPorId(long idPaciente) {
        return jdbc.query("SELECT * FROM pacientes WHERE id_paciente = ?", mapper, idPaciente)
                .stream().findFirst();
    }

    @Override
    public List<Paciente> listarActivos() {
        return jdbc.query("SELECT * FROM pacientes WHERE activo=1 ORDER BY apellidos,nombres", mapper);
    }

    @Override
    public long contarActivos() {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM pacientes WHERE activo = 1", Long.class);
        return total == null ? 0 : total;
    }

    @Override
    public List<Paciente> buscar(String texto, Boolean activo) {
        StringBuilder sql=new StringBuilder("SELECT * FROM pacientes WHERE 1=1");
        java.util.ArrayList<Object> args=new java.util.ArrayList<>();
        if(activo!=null){sql.append(" AND activo=?");args.add(activo);}
        if(texto!=null&&!texto.isBlank()){sql.append(" AND (CONCAT(nombres,' ',apellidos) LIKE ? OR dni LIKE ? OR celular LIKE ?)");String q="%"+texto.trim()+"%";args.add(q);args.add(q);args.add(q);}
        sql.append(" ORDER BY activo DESC,apellidos,nombres");
        return jdbc.query(sql.toString(),mapper,args.toArray());
    }

    @Override
    public void actualizar(Paciente p) {
        jdbc.update("UPDATE pacientes SET dni=?,nombres=?,apellidos=?,fecha_nacimiento=?,direccion=?,celular=?,correo=? WHERE id_paciente=?",
                p.getDni(),p.getNombres(),p.getApellidos(),p.getFechaNacimiento(),p.getDireccion(),p.getCelular(),p.getCorreo(),p.getIdPaciente());
    }

    @Override
    public void cambiarEstado(long idPaciente, boolean activo) { jdbc.update("UPDATE pacientes SET activo=? WHERE id_paciente=?",activo,idPaciente); }
}
