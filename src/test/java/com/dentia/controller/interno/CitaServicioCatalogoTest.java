package com.dentia.controller.interno;

import com.dentia.dao.interfaces.IPacienteDao;
import com.dentia.dto.agenda.SolicitudReservaTurnoCita;
import com.dentia.service.agenda.CitaAgendaService;
import com.dentia.service.agenda.DisponibilidadService;
import com.dentia.service.agenda.ProfesionalAgendaService;
import com.dentia.service.clinica.ServicioDentalCatalogoService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CitaServicioCatalogoTest {
    @Test
    void reservaConElServicioVisibleYMantieneElTipoTecnicoParaCitas() {
        var citas = mock(CitaAgendaService.class);
        var servicios = mock(ServicioDentalCatalogoService.class);
        when(servicios.tipoAgendaParaServicio(7L)).thenReturn(19L);
        var controller = new CitaAgendaController(citas, mock(DisponibilidadService.class),
                mock(ProfesionalAgendaService.class), mock(IPacienteDao.class), servicios);
        var session = mock(HttpSession.class);
        when(session.getAttribute("usuarioActualId")).thenReturn(3);
        LocalDate fecha = LocalDate.of(2026, 9, 22);

        controller.crear(5L, 6L, 8L, 7L, null, fecha, "Limpieza", null,
                session, new RedirectAttributesModelMap());

        ArgumentCaptor<SolicitudReservaTurnoCita> captor = ArgumentCaptor.forClass(SolicitudReservaTurnoCita.class);
        verify(citas).reservarTurno(captor.capture());
        assertEquals(19L, captor.getValue().idTipoAtencion());
        assertEquals(8L, captor.getValue().idHorario());
    }

    @Test
    void reservaSinServicioUsaTipoAgendaPorDefectoYMotivoGenerico() {
        var citas = mock(CitaAgendaService.class);
        var servicios = mock(ServicioDentalCatalogoService.class);
        when(servicios.tipoAgendaPorDefecto()).thenReturn(1L);
        var controller = new CitaAgendaController(citas, mock(DisponibilidadService.class),
                mock(ProfesionalAgendaService.class), mock(IPacienteDao.class), servicios);
        var session = mock(HttpSession.class);
        when(session.getAttribute("usuarioActualId")).thenReturn(5);
        LocalDate fecha = LocalDate.of(2026, 9, 23);

        controller.crear(10L, 2L, 4L, null, null, fecha, null, "Paciente primera vez",
                session, new RedirectAttributesModelMap());

        ArgumentCaptor<SolicitudReservaTurnoCita> captor = ArgumentCaptor.forClass(SolicitudReservaTurnoCita.class);
        verify(citas).reservarTurno(captor.capture());
        assertEquals(1L, captor.getValue().idTipoAtencion());
        assertEquals(4L, captor.getValue().idHorario());
        assertEquals("Consulta odontológica general [Obs: Paciente primera vez]", captor.getValue().motivoConsulta());
    }
}
