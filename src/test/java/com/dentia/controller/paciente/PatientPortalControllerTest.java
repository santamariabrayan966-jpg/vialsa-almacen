package com.dentia.controller.paciente;

import com.dentia.dao.interfaces.ICuentaPacienteDao;
import com.dentia.dto.agenda.SolicitudReservaTurnoCita;
import com.dentia.model.agenda.OrigenCita;
import com.dentia.model.paciente.CuentaPaciente;
import com.dentia.service.agenda.CitaAgendaService;
import com.dentia.service.agenda.DisponibilidadService;
import com.dentia.service.agenda.ProfesionalAgendaService;
import com.dentia.controller.shared.HomeController;
import com.dentia.service.interno.DentiaDashboardService;
import com.dentia.security.GoogleOAuthAvailability;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PatientPortalControllerTest {
    private HomeController controller(ICuentaPacienteDao cuentas, CitaAgendaService citas) {
        return controller(cuentas, citas, "not-configured", "");
    }

    private HomeController controller(ICuentaPacienteDao cuentas, CitaAgendaService citas,
                                      String clientId, String clientSecret) {
        return new HomeController(mock(DentiaDashboardService.class), mock(ProfesionalAgendaService.class),
                mock(DisponibilidadService.class), citas, cuentas,
                new GoogleOAuthAvailability(clientId, clientSecret));
    }

    @Test
    void reservaAutenticadaUsaPacienteDeLaCuentaYNoDatosDelFormulario() {
        ICuentaPacienteDao cuentas = mock(ICuentaPacienteDao.class);
        CitaAgendaService citas = mock(CitaAgendaService.class);
        HomeController controller = controller(cuentas, citas);
        CuentaPaciente cuenta = new CuentaPaciente(); cuenta.setIdPaciente(77L); cuenta.setActivo(true);
        when(cuentas.buscarPorCorreo("paciente@dentia.pe")).thenReturn(Optional.of(cuenta));
        var auth = new UsernamePasswordAuthenticationToken("paciente@dentia.pe", "", java.util.List.of());
        LocalDate fecha = LocalDate.of(2026, 9, 1);

        String vista = controller.confirmarReserva(4L, 9L, 2L, fecha, "Dolor dental", auth,
                new RedirectAttributesModelMap());

        ArgumentCaptor<SolicitudReservaTurnoCita> captor = ArgumentCaptor.forClass(SolicitudReservaTurnoCita.class);
        verify(citas).reservarTurno(captor.capture());
        assertEquals(77L, captor.getValue().idPaciente());
        assertEquals(9L, captor.getValue().idHorario());
        assertEquals(OrigenCita.PORTAL_PACIENTE, captor.getValue().origen());
        assertEquals("redirect:/?citaReservada=true#reservar-cita", vista);
    }

    @Test
    void raizAnonimaAbreElAccesoDelPersonal() {
        HomeController controller = controller(mock(ICuentaPacienteDao.class), mock(CitaAgendaService.class));
        var model = new ExtendedModelMap();

        assertEquals("redirect:/interno/login", controller.root(null, model));
    }

    @Test
    void raizConPacienteAutenticadoAbreSuPortal() {
        HomeController controller = controller(mock(ICuentaPacienteDao.class), mock(CitaAgendaService.class),
                "cliente-real.apps.googleusercontent.com", "secreto-real");
        var model = new ExtendedModelMap();
        var auth = new UsernamePasswordAuthenticationToken("paciente@dentia.pe", "",
                java.util.List.of(() -> "ROLE_PACIENTE"));

        assertEquals("redirect:/paciente", controller.root(auth, model));
    }
}
