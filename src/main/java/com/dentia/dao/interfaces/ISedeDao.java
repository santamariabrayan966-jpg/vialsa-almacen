package com.dentia.dao.interfaces;

import com.dentia.model.interno.Sede;
import com.dentia.model.interno.SedeHorarioRecojo;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDate;

public interface ISedeDao {

    List<Sede> listar();

    Optional<Sede> buscarPorId(Integer id);

    Optional<Sede> buscarPorCodigo(String codigo);

    int crear(Sede sede);

    int actualizar(Sede sede);

    int cambiarEstado(Integer id, boolean activo);

    int eliminar(Integer id);

    int contarTodas();

    int contarPorTipo(String tipo);

    int contarInactivas();

    Optional<String> obtenerDescripcionPublicaAtencion(Integer idSede);

    List<Map<String, Object>> listarHorariosRecojoPorSede(Integer idSede);

    int guardarDescripcionPublicaAtencion(Integer idSede, String descripcionPublica, boolean activo);

    int eliminarHorariosRecojoPorSede(Integer idSede);

    int crearHorarioRecojo(Integer idSede,
                           String nombreBloque,
                           LocalTime horaInicio,
                           LocalTime horaFin,
                           Integer duracionFranjaMinutos,
                           Integer capacidadPorFranja,
                           boolean activo);

    Map<LocalTime, Integer> contarReservasRecojoPorSedeYFecha(Integer idSede, LocalDate fecha);

    String generarCodigo(String tipo);

    Optional<SedeHorarioRecojo> buscarHorarioRecojoPorId(Integer idHorarioRecojo);

    /**
     * Usa SELECT ... FOR UPDATE. Debe invocarse solo dentro de una transaccion activa.
     */
    Optional<SedeHorarioRecojo> bloquearHorarioRecojoPorId(Integer idHorarioRecojo);

    int contarPedidosValidosPorFranja(Integer idSede,
                                      LocalDate fechaRecojo,
                                      LocalTime horaInicioRecojo,
                                      LocalTime horaFinRecojo);

    int contarReservasActivasPorFranja(Integer idSede,
                                       LocalDate fechaRecojo,
                                       LocalTime horaInicioRecojo,
                                       LocalTime horaFinRecojo);
}
