package com.dentia.dao.Jdbc;

import com.dentia.dao.interfaces.ICitaAgendaDao;
import com.dentia.model.agenda.Cita;
import com.dentia.model.agenda.CitaResumen;
import com.dentia.model.agenda.EstadoCita;
import com.dentia.model.agenda.EventoCita;
import com.dentia.model.agenda.IntervaloAgenda;
import com.dentia.model.agenda.OrigenCita;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class JdbcCitaAgendaDao implements ICitaAgendaDao {
    private static final String RESUMEN_SELECT = """
            SELECT c.id_cita,c.id_paciente,CONCAT(p.nombres,' ',p.apellidos) paciente,p.celular paciente_celular,
                   c.id_profesional,CONCAT(pr.nombres,' ',pr.apellidos) profesional,
                   COALESCE((SELECT GROUP_CONCAT(e.nombre ORDER BY e.nombre SEPARATOR ', ')
                     FROM profesional_especialidades pe JOIN especialidades e ON e.id_especialidad=pe.id_especialidad
                     WHERE pe.id_profesional=pr.id_profesional AND e.activo=1),'Odontologia General') especialidad,
                   ta.nombre tipo_atencion,c.fecha_hora_inicio,c.fecha_hora_fin,c.estado,c.origen,c.motivo_consulta
            FROM citas c
            JOIN pacientes p ON p.id_paciente=c.id_paciente
            JOIN profesionales pr ON pr.id_profesional=c.id_profesional
            JOIN tipos_atencion ta ON ta.id_tipo_atencion=c.id_tipo_atencion
            """;

    private final JdbcTemplate jdbc;

    private final RowMapper<CitaResumen> resumenMapper = (rs, row) -> new CitaResumen(
            rs.getLong("id_cita"), rs.getLong("id_paciente"), rs.getString("paciente"),
            rs.getLong("id_profesional"), rs.getString("profesional"), rs.getString("especialidad"),
            rs.getString("tipo_atencion"), fecha(rs.getTimestamp("fecha_hora_inicio")),
            fecha(rs.getTimestamp("fecha_hora_fin")), EstadoCita.valueOf(rs.getString("estado")),
            OrigenCita.valueOf(rs.getString("origen")), rs.getString("motivo_consulta"),
            rs.getString("paciente_celular"));

    public JdbcCitaAgendaDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void bloquearReserva(long idProfesional, long idPaciente, LocalDate fecha) {
        jdbc.update("INSERT IGNORE INTO agenda_reserva_locks (tipo_entidad,id_entidad,fecha) VALUES ('PACIENTE',?,?)",
                idPaciente, fecha);
        jdbc.update("INSERT IGNORE INTO agenda_reserva_locks (tipo_entidad,id_entidad,fecha) VALUES ('PROFESIONAL',?,?)",
                idProfesional, fecha);
        jdbc.query("""
                SELECT tipo_entidad,id_entidad FROM agenda_reserva_locks
                WHERE fecha=? AND ((tipo_entidad='PACIENTE' AND id_entidad=?)
                    OR (tipo_entidad='PROFESIONAL' AND id_entidad=?))
                ORDER BY tipo_entidad,id_entidad FOR UPDATE
                """, (rs, row) -> rs.getString(1), fecha, idPaciente, idProfesional);
    }

    @Override
    public List<IntervaloAgenda> listarOcupacionProfesional(long idProfesional, LocalDateTime desde, LocalDateTime hasta) {
        return intervalos("""
                SELECT fecha_hora_inicio,fecha_hora_fin FROM citas
                WHERE id_profesional=? AND estado NOT IN ('CANCELADA','NO_ASISTIO')
                  AND fecha_hora_inicio < ? AND fecha_hora_fin > ?
                """, idProfesional, hasta, desde);
    }

    @Override
    public List<IntervaloAgenda> listarOcupacionPaciente(long idPaciente, LocalDateTime desde, LocalDateTime hasta) {
        return intervalos("""
                SELECT fecha_hora_inicio,fecha_hora_fin FROM citas
                WHERE id_paciente=? AND estado NOT IN ('CANCELADA','NO_ASISTIO')
                  AND fecha_hora_inicio < ? AND fecha_hora_fin > ?
                """, idPaciente, hasta, desde);
    }

    private List<IntervaloAgenda> intervalos(String sql, Object... args) {
        return jdbc.query(sql, (rs, row) -> new IntervaloAgenda(fecha(rs.getTimestamp(1)), fecha(rs.getTimestamp(2))), args);
    }

    @Override
    public boolean existeSuperposicionProfesional(long idProfesional, LocalDateTime inicio, LocalDateTime fin) {
        return existeSuperposicion("id_profesional", idProfesional, inicio, fin);
    }

    @Override
    public boolean existeSuperposicionPaciente(long idPaciente, LocalDateTime inicio, LocalDateTime fin) {
        return existeSuperposicion("id_paciente", idPaciente, inicio, fin);
    }

    private boolean existeSuperposicion(String columna, long id, LocalDateTime inicio, LocalDateTime fin) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM citas WHERE " + columna +
                        "=? AND estado NOT IN ('CANCELADA','NO_ASISTIO') AND fecha_hora_inicio < ? AND fecha_hora_fin > ?",
                Integer.class, id, fin, inicio);
        return total != null && total > 0;
    }

    @Override
    public long crear(Cita c) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO citas
                    (id_paciente,id_profesional,id_tipo_atencion,fecha_hora_inicio,fecha_hora_fin,
                     motivo_consulta,estado,origen,id_cita_origen,creado_por)
                    VALUES (?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setObject(1, c.idPaciente()); ps.setObject(2, c.idProfesional()); ps.setObject(3, c.idTipoAtencion());
            ps.setObject(4, c.fechaHoraInicio()); ps.setObject(5, c.fechaHoraFin()); ps.setString(6, c.motivoConsulta());
            ps.setString(7, c.estado().name()); ps.setString(8, c.origen().name()); ps.setObject(9, c.idCitaOrigen());
            ps.setObject(10, c.creadoPor());
            return ps;
        }, keys);
        Number key = keys.getKey();
        if (key == null) throw new IllegalStateException("No se pudo registrar la cita.");
        return key.longValue();
    }

    @Override
    public Optional<Cita> buscarPorId(long idCita) {
        return jdbc.query("SELECT * FROM citas WHERE id_cita=?", (rs, row) -> new Cita(
                rs.getLong("id_cita"), rs.getLong("id_paciente"), rs.getLong("id_profesional"),
                rs.getLong("id_tipo_atencion"), fecha(rs.getTimestamp("fecha_hora_inicio")),
                fecha(rs.getTimestamp("fecha_hora_fin")), rs.getString("motivo_consulta"),
                EstadoCita.valueOf(rs.getString("estado")), OrigenCita.valueOf(rs.getString("origen")),
                (Long) rs.getObject("id_cita_origen"), (Integer) rs.getObject("creado_por"),
                fecha(rs.getTimestamp("fecha_creacion")), fecha(rs.getTimestamp("fecha_actualizacion"))), idCita)
                .stream().findFirst();
    }

    @Override
    public boolean actualizarEstado(long idCita, EstadoCita estadoEsperado, EstadoCita estadoNuevo) {
        return jdbc.update("UPDATE citas SET estado=? WHERE id_cita=? AND estado=?",
                estadoNuevo.name(), idCita, estadoEsperado.name()) == 1;
    }

    @Override
    public boolean registrarCancelacion(long idCita, EstadoCita estadoEsperado, String motivo, Integer idUsuario, Long idPaciente) {
        return jdbc.update("""
                UPDATE citas SET estado='CANCELADA',fecha_cancelacion=NOW(),motivo_cancelacion=?,
                    cancelado_por_usuario=?,cancelado_por_paciente=? WHERE id_cita=? AND estado=?
                """, motivo, idUsuario, idPaciente, idCita, estadoEsperado.name()) == 1;
    }

    @Override
    public void registrarHistorial(long idCita, EstadoCita anterior, EstadoCita nuevo, EventoCita evento,
                                   Integer idUsuario, Long idPaciente, String observacion) {
        jdbc.update("""
                INSERT INTO cita_historial
                (id_cita,estado_anterior,estado_nuevo,id_usuario_responsable,id_paciente_responsable,observacion,tipo_evento)
                VALUES (?,?,?,?,?,?,?)
                """, idCita, anterior == null ? null : anterior.name(), nuevo.name(), idUsuario, idPaciente,
                observacion, evento.name());
    }

    @Override
    public int marcarCitasVencidasComoNoAsistio(LocalDateTime ahora) {
        String sqlSelect = """
                SELECT id_cita, estado
                FROM citas
                WHERE estado IN ('RESERVADA', 'CONFIRMADA', 'EN_ESPERA')
                  AND fecha_hora_fin <= ?
                """;
        List<Map<String, Object>> filas = jdbc.queryForList(sqlSelect, ahora);
        int total = 0;
        for (Map<String, Object> fila : filas) {
            long idCita = ((Number) fila.get("id_cita")).longValue();
            String estadoStr = (String) fila.get("estado");
            EstadoCita estadoAnterior = EstadoCita.valueOf(estadoStr);
            if (actualizarEstado(idCita, estadoAnterior, EstadoCita.NO_ASISTIO)) {
                registrarHistorial(idCita, estadoAnterior, EstadoCita.NO_ASISTIO, EventoCita.CAMBIO_ESTADO,
                        null, null, "Inasistencia automática: finalizó el horario programado de atención");
                total++;
            }
        }
        return total;
    }

    @Override
    public List<CitaResumen> listarAgenda(LocalDateTime desde, LocalDateTime hasta, Long idProfesional,
                                          EstadoCita estado, String paciente) {
        marcarCitasVencidasComoNoAsistio(LocalDateTime.now());
        StringBuilder sql = new StringBuilder(RESUMEN_SELECT + " WHERE c.fecha_hora_inicio>=? AND c.fecha_hora_inicio<?");
        List<Object> args = new ArrayList<>(List.of(desde, hasta));
        if (idProfesional != null) { sql.append(" AND c.id_profesional=?"); args.add(idProfesional); }
        if (estado != null) { sql.append(" AND c.estado=?"); args.add(estado.name()); }
        if (paciente != null && !paciente.isBlank()) {
            sql.append(" AND CONCAT(p.nombres,' ',p.apellidos) LIKE ?"); args.add("%" + paciente.trim() + "%");
        }
        // Cada turno admite varias reservas. Quienes ya llegaron van primero,
        // en el orden real en que recepción registró su llegada.
        sql.append("""
                 ORDER BY c.fecha_hora_inicio,c.id_profesional,
                   CASE WHEN c.estado='EN_ESPERA' THEN 0 ELSE 1 END,
                   (SELECT MIN(h.id_historial) FROM cita_historial h
                    WHERE h.id_cita=c.id_cita AND h.estado_nuevo='EN_ESPERA'),c.id_cita
                """);
        return jdbc.query(sql.toString(), resumenMapper, args.toArray());
    }

    @Override
    public List<CitaResumen> listarPorPaciente(long idPaciente) {
        marcarCitasVencidasComoNoAsistio(LocalDateTime.now());
        return jdbc.query(RESUMEN_SELECT + " WHERE c.id_paciente=? ORDER BY c.fecha_hora_inicio DESC", resumenMapper, idPaciente);
    }

    @Override
    public long contarEntre(LocalDateTime desde, LocalDateTime hasta) {
        Long total = jdbc.queryForObject("""
                SELECT COUNT(*) FROM citas WHERE fecha_hora_inicio>=? AND fecha_hora_inicio<?
                  AND estado NOT IN ('CANCELADA','NO_ASISTIO')
                """, Long.class, desde, hasta);
        return total == null ? 0 : total;
    }

    @Override
    public long contarPorEstadoEntre(EstadoCita estado, LocalDateTime desde, LocalDateTime hasta) {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM citas WHERE estado=? AND fecha_hora_inicio>=? AND fecha_hora_inicio<?",
                Long.class, estado.name(), desde, hasta);
        return total == null ? 0 : total;
    }

    private static LocalDateTime fecha(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
