package com.dentia.service.agenda;

import com.dentia.dao.interfaces.ICitaAgendaDao;
import com.dentia.dao.interfaces.IPacienteDao;
import com.dentia.dto.agenda.SolicitudReservaCita;
import com.dentia.dto.agenda.SolicitudReservaTurnoCita;
import com.dentia.model.agenda.Cita;
import com.dentia.model.agenda.EstadoCita;
import com.dentia.model.agenda.OrigenCita;
import com.dentia.model.agenda.TipoAtencion;
import com.dentia.model.agenda.TurnoDisponible;
import com.dentia.model.paciente.Paciente;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CitaAgendaServiceTest {
    private final ICitaAgendaDao dao = mock(ICitaAgendaDao.class);
    private final IPacienteDao pacientes = mock(IPacienteDao.class);
    private final DisponibilidadService disponibilidad = mock(DisponibilidadService.class);
    private final CitaAgendaService service = new CitaAgendaService(dao, pacientes, disponibilidad);

    @Test
    void reservaBloqueaYRevalidaAntesDelInsert() {
        SolicitudReservaCita solicitud = solicitud();
        when(pacientes.buscarPorId(5L)).thenReturn(Optional.of(paciente()));
        when(disponibilidad.tipoActivo(2L)).thenReturn(new TipoAtencion(2L, "Control", "", 20, true));
        when(disponibilidad.esDisponible(1L, 5L, 2L, solicitud.fechaHoraInicio())).thenReturn(true);
        when(dao.crear(any())).thenReturn(20L);

        service.reservar(solicitud);

        InOrder orden = inOrder(dao, disponibilidad);
        orden.verify(dao).bloquearReserva(1L, 5L, solicitud.fechaHoraInicio().toLocalDate());
        orden.verify(disponibilidad).esDisponible(1L, 5L, 2L, solicitud.fechaHoraInicio());
        orden.verify(dao).crear(any(Cita.class));
        verify(dao).registrarHistorial(any(Long.class), any(), any(), any(), any(), any(), any());
    }

    @Test
    void permiteCambioDeEstadoValido() {
        when(dao.buscarPorId(10L)).thenReturn(Optional.of(cita(EstadoCita.CONFIRMADA)));
        when(dao.actualizarEstado(10L, EstadoCita.CONFIRMADA, EstadoCita.EN_ESPERA)).thenReturn(true);

        service.cambiarEstado(10L, EstadoCita.EN_ESPERA, 1, "Paciente llego");

        verify(dao).actualizarEstado(10L, EstadoCita.CONFIRMADA, EstadoCita.EN_ESPERA);
    }

    @Test
    void reservaTurnoCompletoYRevalidaBajoBloqueo() {
        var fecha = java.time.LocalDate.of(2026, 9, 21);
        var solicitud = new SolicitudReservaTurnoCita(5L, 1L, 7L, 2L, fecha,
                "Control", OrigenCita.PANEL_INTERNO, 3, null);
        when(pacientes.buscarPorId(5L)).thenReturn(Optional.of(paciente()));
        when(disponibilidad.tipoActivo(2L)).thenReturn(new TipoAtencion(2L, "Control", "", 20, true));
        when(disponibilidad.obtenerTurno(fecha, 7L, 1L, 5L)).thenReturn(new TurnoDisponible(7L, 1L,
                "Ana Torres", fecha.atTime(9, 0), fecha.atTime(12, 0)));
        when(dao.crear(any())).thenReturn(21L);

        service.reservarTurno(solicitud);

        InOrder orden = inOrder(dao, disponibilidad);
        orden.verify(dao).bloquearReserva(1L, 5L, fecha);
        orden.verify(disponibilidad).obtenerTurno(fecha, 7L, 1L, 5L);
        orden.verify(dao).crear(org.mockito.ArgumentMatchers.argThat(c ->
                c.fechaHoraInicio().equals(fecha.atTime(9, 0)) && c.fechaHoraFin().equals(fecha.atTime(12, 0))));
    }

    @Test
    void rechazaCambioDeEstadoInvalido() {
        when(dao.buscarPorId(10L)).thenReturn(Optional.of(cita(EstadoCita.RESERVADA)));
        assertThrows(AgendaValidationException.class,
                () -> service.cambiarEstado(10L, EstadoCita.ATENDIDA, 1, null));
    }

    @Test
    void reservaTurnoConMotivoVacioAsignaMotivoPorDefecto() {
        var fecha = java.time.LocalDate.of(2026, 9, 21);
        var solicitud = new SolicitudReservaTurnoCita(5L, 1L, 7L, 2L, fecha,
                "   ", OrigenCita.PANEL_INTERNO, 3, null);
        when(pacientes.buscarPorId(5L)).thenReturn(Optional.of(paciente()));
        when(disponibilidad.tipoActivo(2L)).thenReturn(new TipoAtencion(2L, "Consulta general", "", 30, true));
        when(disponibilidad.obtenerTurno(fecha, 7L, 1L, 5L)).thenReturn(new TurnoDisponible(7L, 1L,
                "Ana Torres", fecha.atTime(9, 0), fecha.atTime(9, 30)));
        when(dao.crear(any())).thenReturn(22L);

        service.reservarTurno(solicitud);

        verify(dao).crear(org.mockito.ArgumentMatchers.argThat(c ->
                "Consulta odontológica general".equals(c.motivoConsulta())));
    }

    @Test
    void marcarCitasVencidasComoNoAsistioDelegaEnDao() {
        when(dao.marcarCitasVencidasComoNoAsistio(any(LocalDateTime.class))).thenReturn(3);
        int total = service.marcarCitasVencidasComoNoAsistio();
        org.junit.jupiter.api.Assertions.assertEquals(3, total);
        verify(dao).marcarCitasVencidasComoNoAsistio(any(LocalDateTime.class));
    }

    @Test
    void listarAgendaMarcaCitasVencidasAntesDeConsultar() {
        var hoy = java.time.LocalDate.now();
        service.listarAgenda(hoy, hoy, null, null, null);
        verify(dao).marcarCitasVencidasComoNoAsistio(any(LocalDateTime.class));
        verify(dao).listarAgenda(any(), any(), any(), any(), any());
    }

    @Test
    void transicionNoAsistioPermiteReactivacionOAtencion() {
        org.junit.jupiter.api.Assertions.assertTrue(service.esTransicionValida(EstadoCita.NO_ASISTIO, EstadoCita.RESERVADA));
        org.junit.jupiter.api.Assertions.assertTrue(service.esTransicionValida(EstadoCita.NO_ASISTIO, EstadoCita.EN_ATENCION));
        org.junit.jupiter.api.Assertions.assertTrue(service.esTransicionValida(EstadoCita.RESERVADA, EstadoCita.NO_ASISTIO));
        org.junit.jupiter.api.Assertions.assertTrue(service.esTransicionValida(EstadoCita.CONFIRMADA, EstadoCita.NO_ASISTIO));
        org.junit.jupiter.api.Assertions.assertTrue(service.esTransicionValida(EstadoCita.EN_ESPERA, EstadoCita.NO_ASISTIO));
    }

    private SolicitudReservaCita solicitud() {
        return new SolicitudReservaCita(5L, 1L, 2L, LocalDateTime.of(2026, 9, 1, 10, 0),
                "Control", OrigenCita.PORTAL_PACIENTE, null, null);
    }

    private Paciente paciente() {
        Paciente p = new Paciente(); p.setIdPaciente(5L); p.setActivo(true); return p;
    }

    private Cita cita(EstadoCita estado) {
        return new Cita(10L, 5L, 1L, 2L, LocalDateTime.of(2026, 9, 1, 10, 0),
                LocalDateTime.of(2026, 9, 1, 10, 20), "Control", estado,
                OrigenCita.PANEL_INTERNO, null, 1, null, null);
    }
}
