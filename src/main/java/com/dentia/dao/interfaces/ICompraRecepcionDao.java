package com.dentia.dao.interfaces;

import com.dentia.model.interno.CompraRecepcion;
import com.dentia.model.interno.CompraRecepcionDetalle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ICompraRecepcionDao {

    int registrarRecepcion(CompraRecepcion recepcion);

    void registrarDetalle(CompraRecepcionDetalle detalle);

    List<CompraRecepcion> listarPorCompra(Integer idCompra);

    List<CompraRecepcionDetalle> listarDetallesPorRecepcion(Integer idRecepcion);

    List<CompraRecepcionDetalle> listarPendientesPorCompra(Integer idCompra);

    boolean existeGuiaEnCompra(Integer idCompra, String nroGuiaRemision);

    boolean compraEstaCompleta(Integer idCompra);

    void actualizarEstadoCompra(Integer idCompra, String estado);

    void registrarCierreFaltante(
            Integer idCompra,
            Integer idUsuario,
            String motivo,
            String tipoSustento,
            String serieDocumento,
            String numeroDocumento,
            LocalDate fechaDocumento,
            BigDecimal montoAjustado,
            BigDecimal valorFaltante,
            BigDecimal deudaAntes,
            BigDecimal deudaDespues,
            BigDecimal saldoFavor,
            String estadoDevolucion,
            BigDecimal montoDevolucionPendiente,
            LocalDate fechaDevolucion,
            String observacion
    );

    boolean confirmarDevolucionProveedor(
            Integer idCompra,
            LocalDate fechaDevolucion,
            String observacionDevolucion
    );
}
