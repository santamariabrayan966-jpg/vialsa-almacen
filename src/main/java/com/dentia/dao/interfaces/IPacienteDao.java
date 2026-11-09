package com.dentia.dao.interfaces;

import com.dentia.model.paciente.Paciente;

import java.util.Optional;
import java.util.List;

public interface IPacienteDao {
    long crear(Paciente paciente);
    boolean existeDni(String dni);
    Optional<Paciente> buscarPorId(long idPaciente);
    List<Paciente> listarActivos();
    long contarActivos();
    List<Paciente> buscar(String texto, Boolean activo);
    void actualizar(Paciente paciente);
    void cambiarEstado(long idPaciente, boolean activo);
}
