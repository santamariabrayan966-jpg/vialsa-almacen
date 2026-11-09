package com.dentia.dao.interfaces;

import com.dentia.model.agenda.Cita;
import com.dentia.model.agenda.CitaResumen;
import com.dentia.model.agenda.EstadoCita;
import com.dentia.model.agenda.EventoCita;
import com.dentia.model.agenda.IntervaloAgenda;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ICitaAgendaDao {
    void bloquearReserva(long idProfesional, long idPaciente, LocalDate fecha);
    List<IntervaloAgenda> listarOcupacionProfesional(long idProfesional, LocalDateTime desde, LocalDateTime hasta);
    List<IntervaloAgenda> listarOcupacionPaciente(long idPaciente, LocalDateTime desde, LocalDateTime hasta);
    boolean existeSuperposicionProfesional(long idProfesional, LocalDateTime inicio, LocalDateTime fin);
    boolean existeSuperposicionPaciente(long idPaciente, LocalDateTime inicio, LocalDateTime fin);
    long crear(Cita cita);
    Optional<Cita> buscarPorId(long idCita);
    boolean actualizarEstado(long idCita, EstadoCita estadoEsperado, EstadoCita estadoNuevo);
    boolean registrarCancelacion(long idCita, EstadoCita estadoEsperado, String motivo, Integer idUsuario, Long idPaciente);
    void registrarHistorial(long idCita, EstadoCita anterior, EstadoCita nuevo, EventoCita evento,
                            Integer idUsuario, Long idPaciente, String observacion);
    List<CitaResumen> listarAgenda(LocalDateTime desde, LocalDateTime hasta, Long idProfesional,
                                   EstadoCita estado, String paciente);
    List<CitaResumen> listarPorPaciente(long idPaciente);
    long contarEntre(LocalDateTime desde, LocalDateTime hasta);
    long contarPorEstadoEntre(EstadoCita estado, LocalDateTime desde, LocalDateTime hasta);
    default int marcarCitasVencidasComoNoAsistio(LocalDateTime ahora) {
        return 0;
    }
}
