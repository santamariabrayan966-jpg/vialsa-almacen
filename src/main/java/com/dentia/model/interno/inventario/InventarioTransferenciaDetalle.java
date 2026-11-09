package com.dentia.model.interno.inventario;

import java.math.BigDecimal;

public class InventarioTransferenciaDetalle {

    private Integer idDetalleTransferencia;
    private Integer idTransferencia;
    private Integer idProducto;

    // Cantidad original solicitada en la transferencia
    private BigDecimal cantidad;

    // NUEVO: cantidad que sale desde sede origen
    private BigDecimal cantidadEnviada;

    // NUEVO: cantidad validada por sede destino
    private BigDecimal cantidadRecibida;

    // NUEVO: faltante calculado
    private BigDecimal cantidadFaltante;

    // NUEVO: observación si llegó incompleto, dañado o con diferencia
    private String observacionRecepcion;

    // Campos auxiliares para mostrar en vistas
    private String codigoProducto;
    private String nombreProducto;
    private String fotoProducto;
    private String unidadProducto;

    public Integer getIdDetalleTransferencia() {
        return idDetalleTransferencia;
    }

    public void setIdDetalleTransferencia(Integer idDetalleTransferencia) {
        this.idDetalleTransferencia = idDetalleTransferencia;
    }

    public Integer getIdTransferencia() {
        return idTransferencia;
    }

    public void setIdTransferencia(Integer idTransferencia) {
        this.idTransferencia = idTransferencia;
    }

    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getCantidadEnviada() {
        return cantidadEnviada;
    }

    public void setCantidadEnviada(BigDecimal cantidadEnviada) {
        this.cantidadEnviada = cantidadEnviada;
    }

    public BigDecimal getCantidadRecibida() {
        return cantidadRecibida;
    }

    public void setCantidadRecibida(BigDecimal cantidadRecibida) {
        this.cantidadRecibida = cantidadRecibida;
    }

    public BigDecimal getCantidadFaltante() {
        return cantidadFaltante;
    }

    public void setCantidadFaltante(BigDecimal cantidadFaltante) {
        this.cantidadFaltante = cantidadFaltante;
    }

    public String getObservacionRecepcion() {
        return observacionRecepcion;
    }

    public void setObservacionRecepcion(String observacionRecepcion) {
        this.observacionRecepcion = observacionRecepcion;
    }

    public String getCodigoProducto() {
        return codigoProducto;
    }

    public void setCodigoProducto(String codigoProducto) {
        this.codigoProducto = codigoProducto;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public String getFotoProducto() {
        return fotoProducto;
    }

    public void setFotoProducto(String fotoProducto) {
        this.fotoProducto = fotoProducto;
    }

    public String getUnidadProducto() {
        return unidadProducto;
    }

    public void setUnidadProducto(String unidadProducto) {
        this.unidadProducto = unidadProducto;
    }
}