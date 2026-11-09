package com.dentia.service.agenda;

import com.dentia.dao.interfaces.ICatalogoAgendaDao;
import com.dentia.dao.interfaces.IProfesionalAgendaDao;
import com.dentia.model.agenda.BloqueoAgenda;
import com.dentia.model.agenda.HorarioProfesional;
import com.dentia.model.agenda.Profesional;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfesionalAgendaServiceTest {
    private final IProfesionalAgendaDao dao = mock(IProfesionalAgendaDao.class);
    private final ICatalogoAgendaDao catalogo = mock(ICatalogoAgendaDao.class);
    private final ProfesionalAgendaService service = new ProfesionalAgendaService(dao, catalogo);

    @Test
    void creaProfesionalYAsignaEspecialidades() {
        Profesional nuevo = new Profesional(null, "Ana", "Torres", "12345678", "COP-99",
                "999999999", "ana@dentia.pe", null, true, null, null, null, List.of());
        when(dao.crear(nuevo)).thenReturn(8L);

        long id = service.guardarProfesional(nuevo, List.of(1L, 2L));

        assertEquals(8L, id);
        verify(dao).reemplazarEspecialidades(8L, List.of(1L, 2L));
    }

    @Test
    void rechazaHorarioSuperpuesto() {
        when(dao.buscarPorId(1L)).thenReturn(Optional.of(profesional()));
        when(dao.listarHorariosActivos(1L, 1)).thenReturn(List.of(
                new HorarioProfesional(1L, 1L, 1, LocalTime.of(9, 0), LocalTime.of(13, 0), true)));

        assertThrows(AgendaValidationException.class,
                () -> service.agregarHorario(1L, 1, LocalTime.of(12, 0), LocalTime.of(15, 0)));
    }

    @Test
    void permiteDosTurnosSeparadosEnElMismoDia() {
        when(dao.buscarPorId(1L)).thenReturn(Optional.of(profesional()));
        when(dao.listarHorariosActivos(1L, 1)).thenReturn(List.of(
                new HorarioProfesional(1L, 1L, 1, LocalTime.of(9, 0), LocalTime.of(12, 0), true)));
        when(dao.agregarHorario(any(HorarioProfesional.class))).thenReturn(2L);

        long id = service.agregarHorario(1L, 1, LocalTime.of(17, 0), LocalTime.of(20, 0));

        assertEquals(2L, id);
        verify(dao).agregarHorario(any(HorarioProfesional.class));
    }

    @Test
    void creaBloqueoValidoConResponsable() {
        when(dao.buscarPorId(1L)).thenReturn(Optional.of(profesional()));
        when(dao.crearBloqueo(any())).thenReturn(4L);
        LocalDateTime inicio = LocalDateTime.of(2026, 9, 1, 9, 0);

        long id = service.crearBloqueo(1L, inicio, inicio.plusDays(2), "vacaciones", "Viaje", 2);

        assertEquals(4L, id);
        verify(dao).crearBloqueo(any(BloqueoAgenda.class));
    }

    @Test
    void editaHorarioSinDetectarElMismoBloqueComoSolapamiento() {
        when(dao.buscarPorId(1L)).thenReturn(Optional.of(profesional()));
        when(dao.listarHorariosActivos(1L, 1)).thenReturn(List.of(
                new HorarioProfesional(3L, 1L, 1, LocalTime.of(9, 0), LocalTime.of(13, 0), true)));

        service.guardarHorario(3L, 1L, 1, LocalTime.of(8, 0), LocalTime.of(12, 0));

        verify(dao).actualizarHorario(any(HorarioProfesional.class));
    }

    @Test
    void impideVincularUnaCuentaInternaADosProfesionales() {
        Profesional existente = new Profesional(2L, "Mario", "Paz", null, null, null, null,
                null, true, 7, null, null, List.of());
        when(dao.listarTodos()).thenReturn(List.of(existente));
        Profesional nuevo = new Profesional(null, "Ana", "Torres", null, null, null, null,
                null, true, 7, null, null, List.of());

        assertThrows(AgendaValidationException.class, () -> service.guardarProfesional(nuevo, List.of()));
    }

    private Profesional profesional() {
        return new Profesional(1L, "Ana", "Torres", null, null, null, null,
                null, true, null, null, null, List.of());
    }
}
