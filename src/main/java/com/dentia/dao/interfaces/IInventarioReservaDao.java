package com.dentia.dao.interfaces;

import com.dentia.model.interno.InventarioReserva;
import com.dentia.model.interno.InventarioReservaDetalle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface IInventarioReservaDao {

    int registrarReserva(InventarioReserva reserva);

    int registrarDetalle(InventarioReservaDetalle detalle);

    InventarioReserva buscarPorId(Integer idReserva);

    InventarioReserva buscarPorCodigo(String codigoReserva);

    InventarioReserva buscarActivaPorOrden(Integer idOrdenOnline);

    List<InventarioReservaDetalle> listarDetalles(Integer idReserva);

    int confirmarReservaSiActiva(Integer idReserva);

    int liberarReservaSiActiva(Integer idReserva, String nuevoEstado, String motivoLiberacion);

    List<InventarioReserva> listarExpiradasActivas(LocalDateTime ahora, int limite);

    List<InventarioReserva> listarActivasVencidas(LocalDateTime ahora, int limite);

    int expirarReservaSiVencida(Integer idReserva, LocalDateTime ahora);

    /**
     * Usa SELECT ... FOR UPDATE. Debe invocarse solo dentro de una transaccion activa.
     */
    InventarioReserva bloquearPorId(Integer idReserva);

    BigDecimal sumarReservadoActivoPorProductoYSede(Integer idProducto, Integer idSede);

    BigDecimal sumarReservadoActivoExcluyendoReserva(Integer idProducto, Integer idSede, Integer idReservaExcluir);
}
