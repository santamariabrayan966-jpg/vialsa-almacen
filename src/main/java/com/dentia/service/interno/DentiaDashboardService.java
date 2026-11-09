package com.dentia.service.interno;

import com.dentia.dao.interfaces.IPacienteDao;
import com.dentia.dao.interfaces.ICitaAgendaDao;
import com.dentia.dao.interfaces.IProfesionalAgendaDao;
import com.dentia.dto.interno.DentiaDashboardResumen;
import com.dentia.model.agenda.CitaResumen;
import com.dentia.model.agenda.EstadoCita;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class DentiaDashboardService {

    private final IPacienteDao pacienteDao;
    private final ICitaAgendaDao citaDao;
    private final IProfesionalAgendaDao profesionalDao;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    public DentiaDashboardService(IPacienteDao pacienteDao, ICitaAgendaDao citaDao, IProfesionalAgendaDao profesionalDao, org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.pacienteDao = pacienteDao;
        this.citaDao = citaDao;
        this.profesionalDao = profesionalDao;
    }

    public DentiaDashboardResumen obtenerResumen() {
        long pacientes = 0;
        try {
            pacientes = pacienteDao.contarActivos();
        } catch (DataAccessException ignored) {
            // La migracion inicial puede estar aun pendiente. El dashboard no inventa datos.
        }

        long citasHoy = 0, enEspera = 0, atendidosHoy = 0;
        try {
            var desde = LocalDate.now(java.time.ZoneId.of("America/Lima")).atStartOfDay();
            var hasta = LocalDate.now(java.time.ZoneId.of("America/Lima")).plusDays(1).atStartOfDay();
            citasHoy = citaDao.contarEntre(desde, hasta);
            enEspera = citaDao.contarPorEstadoEntre(EstadoCita.EN_ESPERA, desde, hasta);
            atendidosHoy = jdbc.queryForObject("SELECT COUNT(DISTINCT paciente_id) FROM clinica_atenciones WHERE fecha_atencion >= ? AND fecha_atencion < ?", Long.class, desde, hasta);
        } catch (DataAccessException ignored) {
            // Permite iniciar mientras la migracion aditiva de Fase 2 aun no se aplico.
        }
        long proximas=0,profesionales=0;
        try { proximas=citaDao.listarAgenda(java.time.LocalDateTime.now(),java.time.LocalDateTime.now().plusDays(7),null,null,null).size(); } catch(DataAccessException ignored) {}
        try { profesionales=profesionalDao.contarActivos(); } catch(DataAccessException ignored) {}
        return new DentiaDashboardResumen(pacientes, citasHoy, enEspera, atendidosHoy, proximas, profesionales);
    }

    public List<CitaResumen> obtenerAgendaHoy() {
        try {
            var desde = LocalDate.now(java.time.ZoneId.of("America/Lima")).atStartOfDay();
            return citaDao.listarAgenda(desde, desde.plusDays(1), null, null, null);
        } catch (DataAccessException ignored) {
            return List.of();
        }
    }
}
