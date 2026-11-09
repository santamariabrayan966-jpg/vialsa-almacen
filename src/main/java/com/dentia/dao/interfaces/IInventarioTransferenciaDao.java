package com.dentia.dao.interfaces;

import com.dentia.model.interno.inventario.InventarioTransferencia;
import com.dentia.model.interno.inventario.InventarioTransferenciaDetalle;
import com.dentia.model.interno.inventario.InventarioTransferenciaEvento;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface IInventarioTransferenciaDao {

    // ==========================
    // TRANSFERENCIA BASE
    // ==========================
    int registrarTransferencia(InventarioTransferencia transferencia);

    void registrarDetalle(InventarioTransferenciaDetalle detalle);

    List<InventarioTransferencia> listarTransferencias();

    List<InventarioTransferencia> listarTransferenciasPorSede(Integer idSede);

    Optional<InventarioTransferencia> buscarPorId(Integer idTransferencia);

    List<InventarioTransferenciaDetalle> listarDetalles(Integer idTransferencia);

    String generarCodigo();

// ==========================
// EDICIÓN DE TRANSFERENCIA PENDIENTE
// ==========================

    void eliminarDetallesPorTransferencia(Integer idTransferencia);

    // ==========================
    // NUEVO: TRASLADOS INTERNOS
    // ==========================

    List<InventarioTransferencia> listarTrasladosPendientes();

    List<InventarioTransferencia> listarTrasladosEnProceso();

    List<InventarioTransferencia> listarTrasladosPorRecepcion();

    List<InventarioTransferencia> listarHistorialTraslados();

    // ==========================
// NUEVO: CAMBIO DE ESTADOS
// ==========================

    void actualizarEstado(Integer idTransferencia, String nuevoEstado);

    void aceptarTraslado(Integer idTransferencia, Integer idTransportista);

    void marcarLlegadaOrigen(Integer idTransferencia, String fotoLlegadaOrigen);

    void marcarPedidoListo(Integer idTransferencia, String fotoCarga);

    void iniciarRuta(Integer idTransferencia);

    void marcarLlegadaDestino(Integer idTransferencia, String fotoLlegada);

    void cancelarTraslado(Integer idTransferencia, String observacion);

    // ==========================
    // NUEVO: RECEPCIÓN
    // ==========================

    void actualizarRecepcionDetalle(
            Integer idDetalleTransferencia,
            BigDecimal cantidadRecibida,
            BigDecimal cantidadFaltante,
            String observacionRecepcion
    );

    void confirmarRecepcion(
            Integer idTransferencia,
            String estadoFinal,
            String observacionRecepcion
    );

    // ==========================
    // NUEVO: HISTORIAL / EVENTOS
    // ==========================

    void registrarEvento(InventarioTransferenciaEvento evento);

    List<InventarioTransferenciaEvento> listarEventos(Integer idTransferencia);
}
