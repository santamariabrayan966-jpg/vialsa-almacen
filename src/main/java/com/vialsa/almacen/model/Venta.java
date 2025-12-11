package com.vialsa.almacen.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Venta {

    private Integer idVentas;  // ✔ ahora acepta null

    private LocalDateTime fechaVenta;

    // Datos del comprobante
    private String tipoComprobante;
    private String nroComprobante;

    // Relaciones por ID
    private Integer idCliente;
    private int idUsuario;

    // Estados
    private String estadoVenta;

    // Forma de pago
    private String formaPago;

    // Crédito
    private BigDecimal deuda;
    private BigDecimal pagoInicial;
    private int numeroCuotas;

    // Totales
    private BigDecimal totalVenta;

    // Información del cliente (cuando se registra manual)
    private String documentoCliente;
    private String nombreCliente;
    private String telefonoCliente;

    // Información adicional
    private String nombreUsuario;

    // Lista de detalles
    private List<DetalleVenta> detalles = new ArrayList<>();


    // ===========================
    // GETTERS & SETTERS
    // ===========================

    public Integer getIdVentas() {
        return idVentas;
    }

    public void setIdVentas(Integer idVentas) {
        this.idVentas = idVentas;
    }

    public LocalDateTime getFechaVenta() { return fechaVenta; }
    public void setFechaVenta(LocalDateTime fechaVenta) { this.fechaVenta = fechaVenta; }

    public String getTipoComprobante() { return tipoComprobante; }
    public void setTipoComprobante(String tipoComprobante) { this.tipoComprobante = tipoComprobante; }

    public String getNroComprobante() { return nroComprobante; }
    public void setNroComprobante(String nroComprobante) { this.nroComprobante = nroComprobante; }

    public Integer getIdCliente() { return idCliente; }
    public void setIdCliente(Integer idCliente) { this.idCliente = idCliente; }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public String getEstadoVenta() { return estadoVenta; }
    public void setEstadoVenta(String estadoVenta) { this.estadoVenta = estadoVenta; }

    public String getFormaPago() { return formaPago; }
    public void setFormaPago(String formaPago) { this.formaPago = formaPago; }

    public BigDecimal getDeuda() { return deuda; }
    public void setDeuda(BigDecimal deuda) { this.deuda = deuda; }

    public BigDecimal getPagoInicial() { return pagoInicial; }
    public void setPagoInicial(BigDecimal pagoInicial) { this.pagoInicial = pagoInicial; }

    public int getNumeroCuotas() { return numeroCuotas; }
    public void setNumeroCuotas(int numeroCuotas) { this.numeroCuotas = numeroCuotas; }

    public BigDecimal getTotalVenta() { return totalVenta; }
    public void setTotalVenta(BigDecimal totalVenta) { this.totalVenta = totalVenta; }

    public String getDocumentoCliente() { return documentoCliente; }
    public void setDocumentoCliente(String documentoCliente) { this.documentoCliente = documentoCliente; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    public String getTelefonoCliente() { return telefonoCliente; }
    public void setTelefonoCliente(String telefonoCliente) { this.telefonoCliente = telefonoCliente; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public List<DetalleVenta> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleVenta> detalles) { this.detalles = detalles; }
}
