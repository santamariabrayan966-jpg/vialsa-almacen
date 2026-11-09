package com.dentia.model.interno;

import java.math.BigDecimal;

public class InventarioReservaDetalle {

    private Integer idReservaDetalle;
    private Integer idReserva;
    private Integer idOrdenDetalle;
    private Integer idProducto;
    private Integer idPrecioPresentacion;
    private Integer idUnidad;
    private String nombreProducto;
    private String nombrePresentacion;
    private String tipoCalculoVenta;
    private BigDecimal factorConversionStock;
    private BigDecimal cantidadSolicitada;
    private BigDecimal cantidadReservadaStock;

    public Integer getIdReservaDetalle() {
        return idReservaDetalle;
    }

    public void setIdReservaDetalle(Integer idReservaDetalle) {
        this.idReservaDetalle = idReservaDetalle;
    }

    public Integer getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(Integer idReserva) {
        this.idReserva = idReserva;
    }

    public Integer getIdOrdenDetalle() {
        return idOrdenDetalle;
    }

    public void setIdOrdenDetalle(Integer idOrdenDetalle) {
        this.idOrdenDetalle = idOrdenDetalle;
    }

    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public Integer getIdPrecioPresentacion() {
        return idPrecioPresentacion;
    }

    public void setIdPrecioPresentacion(Integer idPrecioPresentacion) {
        this.idPrecioPresentacion = idPrecioPresentacion;
    }

    public Integer getIdUnidad() {
        return idUnidad;
    }

    public void setIdUnidad(Integer idUnidad) {
        this.idUnidad = idUnidad;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public String getNombrePresentacion() {
        return nombrePresentacion;
    }

    public void setNombrePresentacion(String nombrePresentacion) {
        this.nombrePresentacion = nombrePresentacion;
    }

    public String getTipoCalculoVenta() {
        return tipoCalculoVenta;
    }

    public void setTipoCalculoVenta(String tipoCalculoVenta) {
        this.tipoCalculoVenta = tipoCalculoVenta;
    }

    public BigDecimal getFactorConversionStock() {
        return factorConversionStock;
    }

    public void setFactorConversionStock(BigDecimal factorConversionStock) {
        this.factorConversionStock = factorConversionStock;
    }

    public BigDecimal getCantidadSolicitada() {
        return cantidadSolicitada;
    }

    public void setCantidadSolicitada(BigDecimal cantidadSolicitada) {
        this.cantidadSolicitada = cantidadSolicitada;
    }

    public BigDecimal getCantidadReservadaStock() {
        return cantidadReservadaStock;
    }

    public void setCantidadReservadaStock(BigDecimal cantidadReservadaStock) {
        this.cantidadReservadaStock = cantidadReservadaStock;
    }
}
