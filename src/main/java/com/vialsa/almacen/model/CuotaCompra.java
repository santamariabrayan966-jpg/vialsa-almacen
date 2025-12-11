package com.vialsa.almacen.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CuotaCompra {

    private Integer idCuotaCompra;
    private Integer idCompra;
    private Integer numeroCuota;
    private LocalDate fechaVencimiento;
    private BigDecimal montoCuota;
    private String estado;       // PENDIENTE / PAGADA
    private LocalDate fechaPago; // opcional
    private String metodoPago;   // EFECTIVO / TRANSFERENCIA / etc
    private String observacion;  // opcional

    public Integer getIdCuotaCompra() {
        return idCuotaCompra;
    }

    public void setIdCuotaCompra(Integer idCuotaCompra) {
        this.idCuotaCompra = idCuotaCompra;
    }

    public Integer getIdCompra() {
        return idCompra;
    }

    public void setIdCompra(Integer idCompra) {
        this.idCompra = idCompra;
    }

    public Integer getNumeroCuota() {
        return numeroCuota;
    }

    public void setNumeroCuota(Integer numeroCuota) {
        this.numeroCuota = numeroCuota;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public BigDecimal getMontoCuota() {
        return montoCuota;
    }

    public void setMontoCuota(BigDecimal montoCuota) {
        this.montoCuota = montoCuota;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
