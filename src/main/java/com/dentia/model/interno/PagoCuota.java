package com.dentia.model.interno;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PagoCuota {

    private Integer idPagoCuota;
    private Integer idCuotaVenta;
    private Integer idVenta;

    private BigDecimal montoPagado;

    private String metodoPago;
    private LocalDateTime fechaPago;

    private String referencia;
    private String observacion;

    private Integer idUsuario;

    public Integer getIdPagoCuota() {
        return idPagoCuota;
    }

    public void setIdPagoCuota(Integer idPagoCuota) {
        this.idPagoCuota = idPagoCuota;
    }

    public Integer getIdCuotaVenta() {
        return idCuotaVenta;
    }

    public void setIdCuotaVenta(Integer idCuotaVenta) {
        this.idCuotaVenta = idCuotaVenta;
    }

    public Integer getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(Integer idVenta) {
        this.idVenta = idVenta;
    }

    public BigDecimal getMontoPagado() {
        return montoPagado;
    }

    public void setMontoPagado(BigDecimal montoPagado) {
        this.montoPagado = montoPagado;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }
}