package com.dentia.service.notificacion;

import com.dentia.dto.notificacion.NotificacionAlertaDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class AlertaNotificacionService {

    private static final Logger log = LoggerFactory.getLogger(AlertaNotificacionService.class);
    private final JdbcTemplate jdbc;
    private final com.dentia.dao.interfaces.ICitaAgendaDao citaAgendaDao;
    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public AlertaNotificacionService(JdbcTemplate jdbc, com.dentia.dao.interfaces.ICitaAgendaDao citaAgendaDao) {
        this.jdbc = jdbc;
        this.citaAgendaDao = citaAgendaDao;
    }

    public List<NotificacionAlertaDto> obtenerAlertasPendientes() {
        List<NotificacionAlertaDto> alertas = new ArrayList<>();

        try {
            // Actualizar citas pasadas no atendidas a NO_ASISTIO
            citaAgendaDao.marcarCitasVencidasComoNoAsistio(LocalDateTime.now());

            // 1. Inasistencias (Estado NO_ASISTIO en los últimos 7 días o citas pasadas pendientes de cierre)
            String sqlNoAsistio = """
                SELECT c.id_cita, c.id_paciente, CONCAT(p.nombres, ' ', p.apellidos) AS paciente_nombre,
                       p.nombres AS paciente_solo_nombre, p.celular AS paciente_celular,
                       CONCAT(pr.nombres, ' ', pr.apellidos) AS profesional_nombre,
                       ta.nombre AS tipo_atencion, c.fecha_hora_inicio, c.estado
                FROM citas c
                JOIN pacientes p ON p.id_paciente = c.id_paciente
                JOIN profesionales pr ON pr.id_profesional = c.id_profesional
                JOIN tipos_atencion ta ON ta.id_tipo_atencion = c.id_tipo_atencion
                WHERE (c.estado = 'NO_ASISTIO' AND c.fecha_hora_inicio >= CURDATE() - INTERVAL 7 DAY)
                   OR (c.estado IN ('RESERVADA', 'CONFIRMADA') AND c.fecha_hora_fin < NOW() AND c.fecha_hora_inicio >= CURDATE() - INTERVAL 2 DAY)
                ORDER BY c.fecha_hora_inicio DESC
                LIMIT 15
            """;

            jdbc.query(sqlNoAsistio, (rs) -> {
                long idCita = rs.getLong("id_cita");
                long idPaciente = rs.getLong("id_paciente");
                String paciente = rs.getString("paciente_nombre");
                String soloNombre = rs.getString("paciente_solo_nombre");
                String celular = rs.getString("paciente_celular");
                String profesional = rs.getString("profesional_nombre");
                String atencion = rs.getString("tipo_atencion");
                Timestamp ts = rs.getTimestamp("fecha_hora_inicio");
                LocalDateTime inicio = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

                String id = "no_asistio_" + idCita + "_" + inicio.toLocalDate();
                String titulo = "Paciente No Asistió / Cita Vencida";
                String mensaje = paciente + " no asistió a su cita de " + atencion + " (" + inicio.format(FECHA_FMT) + " " + inicio.format(HORA_FMT) + ")";

                String wa = "Hola " + (soloNombre != null ? soloNombre : paciente) +
                        ", te saludamos de la clínica odontológica DentIA. 🦷\n" +
                        "Notamos que no pudiste asistir a tu cita de *" + atencion + "* programada para el " +
                        inicio.format(FECHA_FMT) + " a las " + inicio.format(HORA_FMT) + ".\n\n" +
                        "¿Deseas que te ayudemos a reprogramarla para otra fecha que te acomode mejor? Estamos a tu disposición.";

                alertas.add(new NotificacionAlertaDto(
                        id, idCita, idPaciente, paciente, celular, profesional,
                        atencion, inicio, "NO_ASISTIO", titulo, mensaje, wa, "danger",
                        "/interno/citas?fecha=" + inicio.toLocalDate()
                ));
            });

            // 2. Citas de Hoy (RESERVADA, CONFIRMADA, EN_ESPERA)
            String sqlHoy = """
                SELECT c.id_cita, c.id_paciente, CONCAT(p.nombres, ' ', p.apellidos) AS paciente_nombre,
                       p.nombres AS paciente_solo_nombre, p.celular AS paciente_celular,
                       CONCAT(pr.nombres, ' ', pr.apellidos) AS profesional_nombre,
                       ta.nombre AS tipo_atencion, c.fecha_hora_inicio, c.estado
                FROM citas c
                JOIN pacientes p ON p.id_paciente = c.id_paciente
                JOIN profesionales pr ON pr.id_profesional = c.id_profesional
                JOIN tipos_atencion ta ON ta.id_tipo_atencion = c.id_tipo_atencion
                WHERE DATE(c.fecha_hora_inicio) = CURDATE()
                  AND c.estado IN ('RESERVADA', 'CONFIRMADA', 'EN_ESPERA')
                ORDER BY c.fecha_hora_inicio ASC
                LIMIT 20
            """;

            jdbc.query(sqlHoy, (rs) -> {
                long idCita = rs.getLong("id_cita");
                long idPaciente = rs.getLong("id_paciente");
                String paciente = rs.getString("paciente_nombre");
                String soloNombre = rs.getString("paciente_solo_nombre");
                String celular = rs.getString("paciente_celular");
                String profesional = rs.getString("profesional_nombre");
                String atencion = rs.getString("tipo_atencion");
                Timestamp ts = rs.getTimestamp("fecha_hora_inicio");
                LocalDateTime inicio = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

                String id = "hoy_" + idCita + "_" + LocalDate.now();
                String titulo = "Cita Programada para Hoy";
                String mensaje = paciente + " tiene cita hoy a las " + inicio.format(HORA_FMT) + " con " + profesional + " (" + atencion + ")";

                String wa = "Hola " + (soloNombre != null ? soloNombre : paciente) +
                        ", te recordamos tu cita odontológica de *HOY* en DentIA 🦷\n" +
                        "⏰ Hora: " + inicio.format(HORA_FMT) + "\n" +
                        "👨‍⚕️ Profesional: " + profesional + "\n" +
                        "📋 Atención: " + atencion + "\n\n" +
                        "¡Te esperamos! Por favor avísanos si ya estás en camino o si presentas algún retraso.";

                alertas.add(new NotificacionAlertaDto(
                        id, idCita, idPaciente, paciente, celular, profesional,
                        atencion, inicio, "HOY", titulo, mensaje, wa, "warning",
                        "/interno/citas?fecha=" + LocalDate.now()
                ));
            });

            // 3. Citas de Mañana (Preventivo: RESERVADA, CONFIRMADA)
            String sqlManana = """
                SELECT c.id_cita, c.id_paciente, CONCAT(p.nombres, ' ', p.apellidos) AS paciente_nombre,
                       p.nombres AS paciente_solo_nombre, p.celular AS paciente_celular,
                       CONCAT(pr.nombres, ' ', pr.apellidos) AS profesional_nombre,
                       ta.nombre AS tipo_atencion, c.fecha_hora_inicio, c.estado
                FROM citas c
                JOIN pacientes p ON p.id_paciente = c.id_paciente
                JOIN profesionales pr ON pr.id_profesional = c.id_profesional
                JOIN tipos_atencion ta ON ta.id_tipo_atencion = c.id_tipo_atencion
                WHERE DATE(c.fecha_hora_inicio) = CURDATE() + INTERVAL 1 DAY
                  AND c.estado IN ('RESERVADA', 'CONFIRMADA')
                ORDER BY c.fecha_hora_inicio ASC
                LIMIT 20
            """;

            jdbc.query(sqlManana, (rs) -> {
                long idCita = rs.getLong("id_cita");
                long idPaciente = rs.getLong("id_paciente");
                String paciente = rs.getString("paciente_nombre");
                String soloNombre = rs.getString("paciente_solo_nombre");
                String celular = rs.getString("paciente_celular");
                String profesional = rs.getString("profesional_nombre");
                String atencion = rs.getString("tipo_atencion");
                Timestamp ts = rs.getTimestamp("fecha_hora_inicio");
                LocalDateTime inicio = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

                String id = "manana_" + idCita + "_" + inicio.toLocalDate();
                String titulo = "Cita Próxima (Mañana)";
                String mensaje = paciente + " tiene cita mañana a las " + inicio.format(HORA_FMT) + " con " + profesional;

                String wa = "Hola " + (soloNombre != null ? soloNombre : paciente) +
                        ", te recordamos que *MAÑANA " + inicio.format(FECHA_FMT) + "* tienes cita odontológica en DentIA 🦷\n" +
                        "⏰ Hora: " + inicio.format(HORA_FMT) + "\n" +
                        "👨‍⚕️ Especialista: " + profesional + "\n" +
                        "📋 Consulta: " + atencion + "\n\n" +
                        "Por favor confirma tu asistencia respondiendo a este mensaje. ¡Que tengas un excelente día!";

                alertas.add(new NotificacionAlertaDto(
                        id, idCita, idPaciente, paciente, celular, profesional,
                        atencion, inicio, "MANANA", titulo, mensaje, wa, "info",
                        "/interno/citas?fecha=" + inicio.toLocalDate()
                ));
            });

        } catch (Exception e) {
            log.error("[DentIA] Error consultando alertas de notificaciones: {}", e.getMessage(), e);
        }

        return alertas;
    }
}
