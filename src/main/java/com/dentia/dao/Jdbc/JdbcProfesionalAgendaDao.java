package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.IProfesionalAgendaDao;
import com.dentia.model.agenda.BloqueoAgenda;
import com.dentia.model.agenda.Especialidad;
import com.dentia.model.agenda.HorarioProfesional;
import com.dentia.model.agenda.Profesional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcProfesionalAgendaDao implements IProfesionalAgendaDao {
    private final JdbcTemplate jdbc;

    public JdbcProfesionalAgendaDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Profesional> listarTodos() {
        return listar("SELECT * FROM profesionales ORDER BY activo DESC, apellidos, nombres");
    }

    @Override
    public List<Profesional> listarActivos() {
        return listar("SELECT * FROM profesionales WHERE activo=1 ORDER BY apellidos, nombres");
    }

    private List<Profesional> listar(String sql) {
        return jdbc.query(sql, (rs, row) -> mapearProfesional(rs.getLong("id_profesional"),
                rs.getString("nombres"), rs.getString("apellidos"), rs.getString("documento"),
                rs.getString("numero_colegiatura"), rs.getString("telefono"), rs.getString("correo"),
                rs.getString("foto"), rs.getBoolean("activo"), (Integer) rs.getObject("id_usuario"),
                fecha(rs.getTimestamp("fecha_creacion")), fecha(rs.getTimestamp("fecha_actualizacion"))));
    }

    @Override
    public Optional<Profesional> buscarPorId(long idProfesional) {
        return jdbc.query("SELECT * FROM profesionales WHERE id_profesional=?", (rs, row) ->
                mapearProfesional(rs.getLong("id_profesional"), rs.getString("nombres"),
                        rs.getString("apellidos"), rs.getString("documento"), rs.getString("numero_colegiatura"),
                        rs.getString("telefono"), rs.getString("correo"), rs.getString("foto"),
                        rs.getBoolean("activo"), (Integer) rs.getObject("id_usuario"),
                        fecha(rs.getTimestamp("fecha_creacion")), fecha(rs.getTimestamp("fecha_actualizacion"))),
                idProfesional).stream().findFirst();
    }

    private Profesional mapearProfesional(long id, String nombres, String apellidos, String documento,
                                           String colegiatura, String telefono, String correo, String foto,
                                           boolean activo, Integer idUsuario, LocalDateTime creado, LocalDateTime actualizado) {
        return new Profesional(id, nombres, apellidos, documento, colegiatura, telefono, correo, foto,
                activo, idUsuario, creado, actualizado, listarEspecialidades(id));
    }

    private List<Especialidad> listarEspecialidades(long idProfesional) {
        return jdbc.query("""
                SELECT e.* FROM especialidades e
                JOIN profesional_especialidades pe ON pe.id_especialidad=e.id_especialidad
                WHERE pe.id_profesional=? ORDER BY e.nombre
                """, (rs, row) -> new Especialidad(rs.getLong("id_especialidad"), rs.getString("nombre"),
                rs.getString("descripcion"), rs.getBoolean("activo")), idProfesional);
    }

    @Override
    public long crear(Profesional p) {
        return insertarConId("""
                INSERT INTO profesionales
                    (nombres, apellidos, documento, numero_colegiatura, telefono, correo, foto, activo, id_usuario)
                VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?)
                """, p.nombres(), p.apellidos(), nuloSiVacio(p.documento()), nuloSiVacio(p.numeroColegiatura()),
                nuloSiVacio(p.telefono()), nuloSiVacio(p.correo()), nuloSiVacio(p.foto()), p.idUsuario());
    }

    @Override
    public void actualizar(Profesional p) {
        jdbc.update("""
                UPDATE profesionales SET nombres=?, apellidos=?, documento=?, numero_colegiatura=?,
                    telefono=?, correo=?, foto=?, id_usuario=? WHERE id_profesional=?
                """, p.nombres(), p.apellidos(), nuloSiVacio(p.documento()), nuloSiVacio(p.numeroColegiatura()),
                nuloSiVacio(p.telefono()), nuloSiVacio(p.correo()), nuloSiVacio(p.foto()), p.idUsuario(), p.idProfesional());
    }

    @Override
    public void cambiarEstado(long idProfesional, boolean activo) {
        jdbc.update("UPDATE profesionales SET activo=? WHERE id_profesional=?", activo, idProfesional);
    }

    @Override
    public void reemplazarEspecialidades(long idProfesional, List<Long> ids) {
        jdbc.update("DELETE FROM profesional_especialidades WHERE id_profesional=?", idProfesional);
        if (ids == null) return;
        ids.stream().distinct().forEach(id -> jdbc.update(
                "INSERT INTO profesional_especialidades (id_profesional,id_especialidad) VALUES (?,?)",
                idProfesional, id));
    }

    @Override
    public List<HorarioProfesional> listarHorarios(long idProfesional) {
        return horarios("SELECT * FROM profesional_horarios WHERE id_profesional=? ORDER BY dia_semana,hora_inicio", idProfesional);
    }

    @Override
    public List<HorarioProfesional> listarHorariosActivos(long idProfesional, int diaSemana) {
        return horarios("SELECT * FROM profesional_horarios WHERE id_profesional=? AND dia_semana=? AND activo=1 ORDER BY hora_inicio",
                idProfesional, diaSemana);
    }

    private List<HorarioProfesional> horarios(String sql, Object... args) {
        return jdbc.query(sql, (rs, row) -> new HorarioProfesional(rs.getLong("id_horario"),
                rs.getLong("id_profesional"), rs.getInt("dia_semana"), rs.getTime("hora_inicio").toLocalTime(),
                rs.getTime("hora_fin").toLocalTime(), rs.getBoolean("activo")), args);
    }

    @Override
    public long agregarHorario(HorarioProfesional h) {
        return insertarConId("""
                INSERT INTO profesional_horarios (id_profesional,dia_semana,hora_inicio,hora_fin,activo)
                VALUES (?,?,?,?,1)
                """, h.idProfesional(), h.diaSemana(), h.horaInicio(), h.horaFin());
    }

    @Override
    public void actualizarHorario(HorarioProfesional h) {
        jdbc.update("UPDATE profesional_horarios SET dia_semana=?,hora_inicio=?,hora_fin=? WHERE id_horario=? AND id_profesional=?",
                h.diaSemana(),h.horaInicio(),h.horaFin(),h.idHorario(),h.idProfesional());
    }

    @Override
    public void eliminarHorario(long idHorario, long idProfesional) {
        jdbc.update("DELETE FROM profesional_horarios WHERE id_horario=? AND id_profesional=?", idHorario, idProfesional);
    }

    @Override
    public List<BloqueoAgenda> listarBloqueos(long idProfesional, LocalDateTime desde, LocalDateTime hasta) {
        return jdbc.query("""
                SELECT * FROM bloqueos_agenda
                WHERE id_profesional=? AND fecha_hora_inicio < ? AND fecha_hora_fin > ?
                ORDER BY fecha_hora_inicio
                """, (rs, row) -> new BloqueoAgenda(rs.getLong("id_bloqueo"), rs.getLong("id_profesional"),
                fecha(rs.getTimestamp("fecha_hora_inicio")), fecha(rs.getTimestamp("fecha_hora_fin")),
                rs.getString("motivo"), rs.getString("observacion"), rs.getInt("creado_por"),
                fecha(rs.getTimestamp("fecha_creacion"))), idProfesional, hasta, desde);
    }

    @Override
    public long crearBloqueo(BloqueoAgenda b) {
        return insertarConId("""
                INSERT INTO bloqueos_agenda
                    (id_profesional,fecha_hora_inicio,fecha_hora_fin,motivo,observacion,creado_por)
                VALUES (?,?,?,?,?,?)
                """, b.idProfesional(), b.fechaHoraInicio(), b.fechaHoraFin(), b.motivo(),
                nuloSiVacio(b.observacion()), b.creadoPor());
    }

    @Override
    public void eliminarBloqueo(long idBloqueo, long idProfesional) {
        jdbc.update("DELETE FROM bloqueos_agenda WHERE id_bloqueo=? AND id_profesional=?", idBloqueo, idProfesional);
    }

    @Override
    public long contarActivos() {
        Long total=jdbc.queryForObject("SELECT COUNT(*) FROM profesionales WHERE activo=1",Long.class);
        return total==null?0:total;
    }

    private long insertarConId(String sql, Object... args) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            return ps;
        }, keys);
        Number key = keys.getKey();
        if (key == null) throw new IllegalStateException("No se pudo obtener el identificador generado.");
        return key.longValue();
    }

    private String nuloSiVacio(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private LocalDateTime fecha(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
