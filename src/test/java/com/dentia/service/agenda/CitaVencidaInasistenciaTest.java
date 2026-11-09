package com.dentia.service.agenda;

import com.dentia.dao.Jdbc.JdbcCitaAgendaDao;
import com.dentia.model.agenda.EstadoCita;
import com.dentia.scheduler.CitaAgendaScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CitaVencidaInasistenciaTest {

    @Test
    void schedulerEjecutaMarcadoDeInasistencias() {
        CitaAgendaService service = mock(CitaAgendaService.class);
        when(service.marcarCitasVencidasComoNoAsistio()).thenReturn(2);

        CitaAgendaScheduler scheduler = new CitaAgendaScheduler(service);
        scheduler.procesarInasistenciasAutomaticas();

        verify(service).marcarCitasVencidasComoNoAsistio();
    }

    @Test
    void jdbcDaoMarcaCitasVencidasYRegistraHistorial() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        JdbcCitaAgendaDao dao = new JdbcCitaAgendaDao(jdbc);

        LocalDateTime ahora = LocalDateTime.of(2026, 9, 30, 22, 30, 0);

        // Simula que la consulta encuentra 2 citas vencidas en estados RESERVADA y CONFIRMADA
        List<Map<String, Object>> filasVencidas = List.of(
                Map.of("id_cita", 101L, "estado", "RESERVADA"),
                Map.of("id_cita", 102L, "estado", "CONFIRMADA")
        );

        when(jdbc.queryForList(anyString(), eq(ahora))).thenReturn(filasVencidas);
        // Simula actualización exitosa de cada cita
        when(jdbc.update(eq("UPDATE citas SET estado=? WHERE id_cita=? AND estado=?"),
                eq(EstadoCita.NO_ASISTIO.name()), eq(101L), eq(EstadoCita.RESERVADA.name()))).thenReturn(1);
        when(jdbc.update(eq("UPDATE citas SET estado=? WHERE id_cita=? AND estado=?"),
                eq(EstadoCita.NO_ASISTIO.name()), eq(102L), eq(EstadoCita.CONFIRMADA.name()))).thenReturn(1);

        int total = dao.marcarCitasVencidasComoNoAsistio(ahora);

        assertEquals(2, total);
        // Verifica que se actualizaron ambas citas
        verify(jdbc).update(eq("UPDATE citas SET estado=? WHERE id_cita=? AND estado=?"),
                eq(EstadoCita.NO_ASISTIO.name()), eq(101L), eq(EstadoCita.RESERVADA.name()));
        verify(jdbc).update(eq("UPDATE citas SET estado=? WHERE id_cita=? AND estado=?"),
                eq(EstadoCita.NO_ASISTIO.name()), eq(102L), eq(EstadoCita.CONFIRMADA.name()));
    }
}
