package com.dentia.service.agenda;

import com.dentia.dao.interfaces.ICitaAgendaDao;
import com.dentia.dao.interfaces.IPacienteDao;
import com.dentia.dto.agenda.SolicitudReservaCita;
import com.dentia.model.agenda.*;
import com.dentia.model.paciente.Paciente;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReservaConcurrenteServiceTest {
    @Test
    void dosReservasSimultaneasSoloCreanUnaCita() throws Exception {
        FakeCitaDao dao = new FakeCitaDao();
        IPacienteDao pacientes = mock(IPacienteDao.class);
        DisponibilidadService disponibilidad = mock(DisponibilidadService.class);
        Paciente paciente = new Paciente(); paciente.setIdPaciente(5L); paciente.setActivo(true);
        when(pacientes.buscarPorId(5L)).thenReturn(Optional.of(paciente));
        when(disponibilidad.tipoActivo(2L)).thenReturn(new TipoAtencion(2L, "Control", "", 30, true));
        when(disponibilidad.esDisponible(anyLong(), anyLong(), anyLong(), any(LocalDateTime.class)))
                .thenAnswer(inv -> dao.creadas.get() == 0);
        CitaAgendaService service = new CitaAgendaService(dao, pacientes, disponibilidad);
        SolicitudReservaCita solicitud = new SolicitudReservaCita(5L, 1L, 2L,
                LocalDateTime.of(2026, 9, 1, 10, 0), "Control", OrigenCita.PORTAL_PACIENTE, null, null);
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<Boolean> a = pool.submit(() -> ejecutar(service, solicitud, salida));
        Future<Boolean> b = pool.submit(() -> ejecutar(service, solicitud, salida));
        salida.countDown();

        int exitos = (a.get() ? 1 : 0) + (b.get() ? 1 : 0);
        pool.shutdownNow();

        assertEquals(1, exitos);
        assertEquals(1, dao.creadas.get());
    }

    private boolean ejecutar(CitaAgendaService service, SolicitudReservaCita solicitud, CountDownLatch salida) throws Exception {
        salida.await();
        try { service.reservar(solicitud); return true; }
        catch (AgendaValidationException ex) { return false; }
    }

    private static class FakeCitaDao implements ICitaAgendaDao {
        private final ReentrantLock lock = new ReentrantLock();
        private final AtomicInteger creadas = new AtomicInteger();
        @Override public void bloquearReserva(long p, long pa, LocalDate f) { lock.lock(); }
        @Override public long crear(Cita cita) { int n=creadas.incrementAndGet(); lock.unlock(); return n; }
        @Override public void registrarHistorial(long i, EstadoCita a, EstadoCita n, EventoCita e, Integer u, Long p, String o) {}
        @Override public List<IntervaloAgenda> listarOcupacionProfesional(long i, LocalDateTime d, LocalDateTime h){return List.of();}
        @Override public List<IntervaloAgenda> listarOcupacionPaciente(long i, LocalDateTime d, LocalDateTime h){return List.of();}
        @Override public boolean existeSuperposicionProfesional(long i, LocalDateTime d, LocalDateTime h){return creadas.get()>0;}
        @Override public boolean existeSuperposicionPaciente(long i, LocalDateTime d, LocalDateTime h){return creadas.get()>0;}
        @Override public Optional<Cita> buscarPorId(long i){return Optional.empty();}
        @Override public boolean actualizarEstado(long i, EstadoCita a, EstadoCita n){return false;}
        @Override public boolean registrarCancelacion(long i, EstadoCita a, String m, Integer u, Long p){return false;}
        @Override public List<CitaResumen> listarAgenda(LocalDateTime d,LocalDateTime h,Long p,EstadoCita e,String q){return List.of();}
        @Override public List<CitaResumen> listarPorPaciente(long i){return List.of();}
        @Override public long contarEntre(LocalDateTime d,LocalDateTime h){return 0;}
        @Override public long contarPorEstadoEntre(EstadoCita e,LocalDateTime d,LocalDateTime h){return 0;}
    }
}
