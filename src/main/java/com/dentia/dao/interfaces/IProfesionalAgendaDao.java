package com.dentia.dao.interfaces;

import com.dentia.model.agenda.BloqueoAgenda;
import com.dentia.model.agenda.HorarioProfesional;
import com.dentia.model.agenda.Profesional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IProfesionalAgendaDao {
    List<Profesional> listarTodos();
    List<Profesional> listarActivos();
    Optional<Profesional> buscarPorId(long idProfesional);
    long crear(Profesional profesional);
    void actualizar(Profesional profesional);
    void cambiarEstado(long idProfesional, boolean activo);
    void reemplazarEspecialidades(long idProfesional, List<Long> idsEspecialidad);
    List<HorarioProfesional> listarHorarios(long idProfesional);
    List<HorarioProfesional> listarHorariosActivos(long idProfesional, int diaSemana);
    long agregarHorario(HorarioProfesional horario);
    void actualizarHorario(HorarioProfesional horario);
    void eliminarHorario(long idHorario, long idProfesional);
    List<BloqueoAgenda> listarBloqueos(long idProfesional, LocalDateTime desde, LocalDateTime hasta);
    long crearBloqueo(BloqueoAgenda bloqueo);
    void eliminarBloqueo(long idBloqueo, long idProfesional);
    long contarActivos();
}
