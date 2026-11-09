package com.dentia.dao.interfaces;

import com.dentia.model.agenda.Especialidad;
import com.dentia.model.agenda.TipoAtencion;

import java.util.List;
import java.util.Optional;

public interface ICatalogoAgendaDao {
    List<Especialidad> listarEspecialidades(boolean soloActivas);
    Optional<Especialidad> buscarEspecialidad(long idEspecialidad);
    long guardarEspecialidad(Especialidad especialidad);
    void cambiarEstadoEspecialidad(long idEspecialidad, boolean activo);
    List<TipoAtencion> listarTiposAtencion(boolean soloActivos);
    Optional<TipoAtencion> buscarTipoAtencion(long idTipoAtencion);
    long guardarTipoAtencion(TipoAtencion tipo);
    void cambiarEstadoTipoAtencion(long idTipoAtencion, boolean activo);
}
