package com.dentia.controller.interno;

import com.dentia.security.CustomAuthorizationService;
import com.dentia.service.agenda.ProfesionalAgendaService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReportesClinicosDatosTest {
    @Test void baseVaciaProduceCerosSinEstadisticasInventadas() {
        var jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(Object[].class))).thenReturn(0);
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());
        var permisos = mock(CustomAuthorizationService.class);
        var auth = new UsernamePasswordAuthenticationToken("profesional", "", List.of());
        when(permisos.tieneAcceso(auth, "reportes")).thenReturn(true);
        var controller = new ReportesClinicosController(permisos, mock(ProfesionalAgendaService.class), jdbc);
        var datos = controller.datosAjax(LocalDate.of(2026,10,1), LocalDate.of(2026,10,24), null, auth);
        for (String key : List.of("pacientesAtendidos", "tratamientosRealizados", "diagnosticosRegistrados", "ocupacionAgenda")) assertEquals(0, datos.get(key));
        assertTrue(((List<?>)datos.get("tratamientosPorTipo")).isEmpty());
        for (var semana : (List<Map<String,Object>>)datos.get("pacientesPorSemana")) assertEquals(0, semana.get("valor"));
        assertFalse(controller.exportarCsv(LocalDate.of(2026,10,1), LocalDate.of(2026,10,24), null, auth).getBody().contains("+12%"));
    }
    @Test void rechazaPeriodoInvertido() {
        var permisos = mock(CustomAuthorizationService.class);
        var auth = new UsernamePasswordAuthenticationToken("profesional", "", List.of());
        when(permisos.tieneAcceso(auth,"reportes")).thenReturn(true);
        var jdbc = mock(JdbcTemplate.class);
        var controller = new ReportesClinicosController(permisos,mock(ProfesionalAgendaService.class),jdbc);
        assertThrows(ResponseStatusException.class, () -> controller.datosAjax(LocalDate.of(2026,10,24),LocalDate.of(2026,10,1),null,auth));
        verifyNoInteractions(jdbc);
    }
}
