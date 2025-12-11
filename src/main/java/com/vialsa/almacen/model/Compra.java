package com.vialsa.almacen.model;

import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Compra {

    private Integer idCompra;
    private LocalDateTime fechaCompra;

    private String nroComprobante;
    private Integer idProveedor;
    private Integer idUsuario;

    private BigDecimal totalCompra;
    private BigDecimal subtotal;
    private BigDecimal montoIgv;

    private String nombreProveedor;
    private String nombreUsuario;

    // -------- Datos del comprobante --------
    private String tipoComprobante;
    private String serie;
    private String numero;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;


    // -------- Moneda / IGV --------
    private String moneda;
    private BigDecimal tipoCambio;

    private Boolean incluyeIgv;
    private BigDecimal porcentajeIgv;

    // -------- Pago / Crédito --------
    private String formaPago;
    private Integer plazoDias;
    private Integer numeroCuotas;

    // -------- Deuda --------
    private BigDecimal deuda;
    private BigDecimal totalPagado;

    // Calculado automáticamente
    public BigDecimal getDeudaPendiente() {
        if (deuda == null) return BigDecimal.ZERO;
        if (totalPagado == null) return deuda;
        return deuda.subtract(totalPagado);
    }

    // -------- Otros --------
    private String nroOrdenCompra;
    private String nroGuiaRemision;

    private String estado;
    private String observaciones;


    // =============================
    //          GETTERS
    // =============================

    public Integer getIdCompra() { return idCompra; }
    public LocalDateTime getFechaCompra() { return fechaCompra; }
    public String getNroComprobante() { return nroComprobante; }
    public Integer getIdProveedor() { return idProveedor; }
    public Integer getIdUsuario() { return idUsuario; }
    public BigDecimal getTotalCompra() { return totalCompra; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getMontoIgv() { return montoIgv; }

    public String getNombreProveedor() { return nombreProveedor; }
    public String getNombreUsuario() { return nombreUsuario; }

    public String getTipoComprobante() { return tipoComprobante; }
    public String getSerie() { return serie; }
    public String getNumero() { return numero; }

    public LocalDate getFechaEmision() { return fechaEmision; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }

    public String getMoneda() { return moneda; }
    public BigDecimal getTipoCambio() { return tipoCambio; }

    public Boolean getIncluyeIgv() { return incluyeIgv; }
    public Boolean isIncluyeIgv() { return incluyeIgv; }

    public BigDecimal getPorcentajeIgv() { return porcentajeIgv; }

    public String getFormaPago() { return formaPago; }
    public Integer getPlazoDias() { return plazoDias; }
    public Integer getNumeroCuotas() { return numeroCuotas; }

    public BigDecimal getDeuda() { return deuda; }
    public BigDecimal getTotalPagado() { return totalPagado; }

    public String getNroOrdenCompra() { return nroOrdenCompra; }
    public String getNroGuiaRemision() { return nroGuiaRemision; }

    public String getEstado() { return estado; }
    public String getObservaciones() { return observaciones; }


    // =============================
    //          SETTERS
    // =============================

    public void setIdCompra(Integer idCompra) { this.idCompra = idCompra; }
    public void setFechaCompra(LocalDateTime fechaCompra) { this.fechaCompra = fechaCompra; }
    public void setNroComprobante(String nroComprobante) { this.nroComprobante = nroComprobante; }
    public void setIdProveedor(Integer idProveedor) { this.idProveedor = idProveedor; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public void setTotalCompra(BigDecimal totalCompra) { this.totalCompra = totalCompra; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public void setMontoIgv(BigDecimal montoIgv) { this.montoIgv = montoIgv; }

    public void setNombreProveedor(String nombreProveedor) { this.nombreProveedor = nombreProveedor; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public void setTipoComprobante(String tipoComprobante) { this.tipoComprobante = tipoComprobante; }
    public void setSerie(String serie) { this.serie = serie; }
    public void setNumero(String numero) { this.numero = numero; }


    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public void setMoneda(String moneda) { this.moneda = moneda; }
    public void setTipoCambio(BigDecimal tipoCambio) { this.tipoCambio = tipoCambio; }

    public void setIncluyeIgv(Boolean incluyeIgv) { this.incluyeIgv = incluyeIgv; }
    public void setPorcentajeIgv(BigDecimal porcentajeIgv) { this.porcentajeIgv = porcentajeIgv; }

    public void setFormaPago(String formaPago) { this.formaPago = formaPago; }
    public void setPlazoDias(Integer plazoDias) { this.plazoDias = plazoDias; }
    public void setNumeroCuotas(Integer numeroCuotas) { this.numeroCuotas = numeroCuotas; }

    public void setDeuda(BigDecimal deuda) { this.deuda = deuda; }
    public void setTotalPagado(BigDecimal totalPagado) { this.totalPagado = totalPagado; }

    public void setNroOrdenCompra(String nroOrdenCompra) { this.nroOrdenCompra = nroOrdenCompra; }
    public void setNroGuiaRemision(String nroGuiaRemision) { this.nroGuiaRemision = nroGuiaRemision; }

    public void setEstado(String estado) { this.estado = estado; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
