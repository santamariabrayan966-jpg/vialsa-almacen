package com.dentia.service.agenda;

import com.dentia.dao.interfaces.ICatalogoAgendaDao;
import com.dentia.dao.interfaces.ICitaAgendaDao;
import com.dentia.dao.interfaces.IProfesionalAgendaDao;
import com.dentia.model.agenda.BloqueoAgenda;
import com.dentia.model.agenda.HorarioProfesional;
import com.dentia.model.agenda.IntervaloAgenda;
import com.dentia.model.agenda.Profesional;
import com.dentia.model.agenda.SlotDisponible;
import com.dentia.model.agenda.TipoAtencion;
import com.dentia.model.agenda.TurnoDisponible;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DisponibilidadServiceTest {
    private final IProfesionalAgendaDao profesionales = mock(IProfesionalAgendaDao.class);
    private final ICatalogoAgendaDao catalogo = mock(ICatalogoAgendaDao.class);
    private final ICitaAgendaDao citas = mock(ICitaAgendaDao.class);
    private final LocalDate fecha = LocalDate.of(2026, 8, 24); // lunes
    private DisponibilidadService service;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-08-20T14:00:00Z"), ZoneId.of("America/Lima"));
        service = new DisponibilidadService(profesionales, catalogo, citas, 30, reloj);
        when(catalogo.buscarTipoAtencion(1L)).thenReturn(Optional.of(new TipoAtencion(1L, "Consulta", "", 30, true)));
        when(profesionales.buscarPorId(1L)).thenReturn(Optional.of(profesional()));
        when(profesionales.listarActivos()).thenReturn(List.of(profesional()));
        when(profesionales.listarHorariosActivos(1L, 1)).thenReturn(List.of(
                new HorarioProfesional(1L, 1L, 1, LocalTime.of(9, 0), LocalTime.of(12, 0), true)));
    }

    @Test
    void calculaSlotsExcluyendoBloqueosYCitasDeProfesionalYPaciente() {
        when(profesionales.listarBloqueos(eq(1L), any(), any())).thenReturn(List.of(
                new BloqueoAgenda(1L, 1L, fecha.atTime(10, 0), fecha.atTime(10, 30), "DESCANSO", null, 1, null)));
        when(citas.listarOcupacionProfesional(eq(1L), any(), any())).thenReturn(List.of(
                new IntervaloAgenda(fecha.atTime(9, 30), fecha.atTime(10, 0))));
        when(citas.listarOcupacionPaciente(eq(5L), any(), any())).thenReturn(List.of(
                new IntervaloAgenda(fecha.atTime(11, 0), fecha.atTime(11, 30))));

        List<SlotDisponible> slots = service.buscar(fecha, 1L, 1L, 5L);

        assertEquals(List.of(LocalTime.of(9, 0), LocalTime.of(10, 30), LocalTime.of(11, 30)),
                slots.stream().map(s -> s.inicio().toLocalTime()).toList());
    }

    @Test
    void rechazaSuperposicionDelProfesional() {
        when(profesionales.listarBloqueos(eq(1L), any(), any())).thenReturn(List.of());
        when(citas.existeSuperposicionProfesional(1L, fecha.atTime(9, 0), fecha.atTime(9, 30))).thenReturn(true);
        assertFalse(service.esDisponible(1L, 5L, 1L, fecha.atTime(9, 0)));
    }

    @Test
    void rechazaCitaSimultaneaDelPaciente() {
        when(profesionales.listarBloqueos(eq(1L), any(), any())).thenReturn(List.of());
        when(citas.existeSuperposicionPaciente(5L, fecha.atTime(9, 0), fecha.atTime(9, 30))).thenReturn(true);
        assertFalse(service.esDisponible(1L, 5L, 1L, fecha.atTime(9, 0)));
    }

    @Test
    void devuelveTurnosCompletosSinFraccionarlosYPermiteVariasReservasDelProfesional() {
        when(profesionales.listarHorariosActivos(1L, 1)).thenReturn(List.of(
                new HorarioProfesional(1L, 1L, 1, LocalTime.of(9, 0), LocalTime.of(12, 0), true),
                new HorarioProfesional(2L, 1L, 1, LocalTime.of(17, 0), LocalTime.of(20, 0), true)));
        when(profesionales.listarBloqueos(eq(1L), any(), any())).thenReturn(List.of());
        when(citas.existeSuperposicionProfesional(eq(1L), any(), any())).thenReturn(true);

        List<TurnoDisponible> turnos = service.buscarTurnos(fecha, 1L, 5L);

        assertEquals(List.of(1L, 2L), turnos.stream().map(TurnoDisponible::idHorario).toList());
        assertEquals(List.of(LocalTime.of(9, 0), LocalTime.of(17, 0)),
                turnos.stream().map(t -> t.inicio().toLocalTime()).toList());
    }

    @Test
    void excluyeTurnoYaReservadoPorElMismoPaciente() {
        when(profesionales.listarBloqueos(eq(1L), any(), any())).thenReturn(List.of());
        when(citas.existeSuperposicionPaciente(5L, fecha.atTime(9, 0), fecha.atTime(12, 0))).thenReturn(true);
        assertEquals(List.of(), service.buscarTurnos(fecha, 1L, 5L));
    }

    private Profesional profesional() {
        return new Profesional(1L, "Ana", "Torres", null, null, null, null, null,
                true, null, null, null, List.of());
    }
}
