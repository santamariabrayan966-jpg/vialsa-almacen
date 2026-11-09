package com.dentia.dao.interfaces;

import com.dentia.model.paciente.CuentaPaciente;

import java.util.Optional;

public interface ICuentaPacienteDao {
    void crear(CuentaPaciente cuenta);
    boolean existeCorreo(String correo);
    Optional<CuentaPaciente> buscarPorCorreo(String correo);
}
