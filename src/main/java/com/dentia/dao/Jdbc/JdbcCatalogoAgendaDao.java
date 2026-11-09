package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.ICatalogoAgendaDao;
import com.dentia.model.agenda.Especialidad;
import com.dentia.model.agenda.TipoAtencion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcCatalogoAgendaDao implements ICatalogoAgendaDao {
    private final JdbcTemplate jdbc;

    public JdbcCatalogoAgendaDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Especialidad> listarEspecialidades(boolean soloActivas) {
        String filtro = soloActivas ? " WHERE activo = 1" : "";
        return jdbc.query("SELECT * FROM especialidades" + filtro + " ORDER BY nombre", (rs, row) ->
                new Especialidad(rs.getLong("id_especialidad"), rs.getString("nombre"),
                        rs.getString("descripcion"), rs.getBoolean("activo")));
    }

    @Override
    public Optional<Especialidad> buscarEspecialidad(long idEspecialidad) {
        return jdbc.query("SELECT * FROM especialidades WHERE id_especialidad = ?", (rs, row) ->
                new Especialidad(rs.getLong("id_especialidad"), rs.getString("nombre"),
                        rs.getString("descripcion"), rs.getBoolean("activo")), idEspecialidad).stream().findFirst();
    }

    @Override
    public long guardarEspecialidad(Especialidad especialidad) {
        if (especialidad.idEspecialidad() != null) {
            jdbc.update("UPDATE especialidades SET nombre=?, descripcion=? WHERE id_especialidad=?",
                    especialidad.nombre().trim(), especialidad.descripcion(), especialidad.idEspecialidad());
            return especialidad.idEspecialidad();
        }
        return insertarConId("INSERT INTO especialidades (nombre, descripcion, activo) VALUES (?, ?, 1)",
                especialidad.nombre().trim(), especialidad.descripcion());
    }

    @Override
    public void cambiarEstadoEspecialidad(long idEspecialidad, boolean activo) {
        jdbc.update("UPDATE especialidades SET activo=? WHERE id_especialidad=?", activo, idEspecialidad);
    }

    @Override
    public List<TipoAtencion> listarTiposAtencion(boolean soloActivos) {
        String filtro = soloActivos ? " WHERE activo = 1" : "";
        return jdbc.query("SELECT * FROM tipos_atencion" + filtro + " ORDER BY nombre", (rs, row) ->
                new TipoAtencion(rs.getLong("id_tipo_atencion"), rs.getString("nombre"),
                        rs.getString("descripcion"), rs.getInt("duracion_minutos"), rs.getBoolean("activo")));
    }

    @Override
    public Optional<TipoAtencion> buscarTipoAtencion(long idTipoAtencion) {
        return jdbc.query("SELECT * FROM tipos_atencion WHERE id_tipo_atencion=?", (rs, row) ->
                new TipoAtencion(rs.getLong("id_tipo_atencion"), rs.getString("nombre"),
                        rs.getString("descripcion"), rs.getInt("duracion_minutos"), rs.getBoolean("activo")),
                idTipoAtencion).stream().findFirst();
    }

    @Override
    public long guardarTipoAtencion(TipoAtencion tipo) {
        if (tipo.idTipoAtencion() != null) {
            jdbc.update("UPDATE tipos_atencion SET nombre=?, descripcion=?, duracion_minutos=? WHERE id_tipo_atencion=?",
                    tipo.nombre().trim(), tipo.descripcion(), tipo.duracionMinutos(), tipo.idTipoAtencion());
            return tipo.idTipoAtencion();
        }
        return insertarConId("INSERT INTO tipos_atencion (nombre, descripcion, duracion_minutos, activo) VALUES (?, ?, ?, 1)",
                tipo.nombre().trim(), tipo.descripcion(), tipo.duracionMinutos());
    }

    @Override
    public void cambiarEstadoTipoAtencion(long idTipoAtencion, boolean activo) {
        jdbc.update("UPDATE tipos_atencion SET activo=? WHERE id_tipo_atencion=?", activo, idTipoAtencion);
    }

    private long insertarConId(String sql, Object... parametros) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < parametros.length; i++) ps.setObject(i + 1, parametros[i]);
            return ps;
        }, keys);
        Number key = keys.getKey();
        if (key == null) throw new IllegalStateException("No se pudo obtener el identificador generado.");
        return key.longValue();
    }
}
