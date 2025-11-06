package com.vialsa.almacen.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Modelo que representa una Compra en el sistema de almacén VIALSA.
 * Incluye información de cabecera de la compra y referencias a proveedor, usuario y totales.
 */
public class Compra {

    private Integer idCompra;
    private LocalDateTime fechaCompra;
    private String nroComprobante;
    private Integer idProveedor;
    private Integer idUsuario;
    private BigDecimal totalCompra;
    private String nombreProveedor;
    private String nombreUsuario;

    // 🔹 Constructor vacío
    public Compra() {
    }

    // 🔹 Constructor con parámetros (opcional, útil si lo usas en pruebas o seeds)
    public Compra(Integer idCompra, LocalDateTime fechaCompra, String nroComprobante,
                  Integer idProveedor, Integer idUsuario, BigDecimal totalCompra,
                  String nombreProveedor, String nombreUsuario) {
        this.idCompra = idCompra;
        this.fechaCompra = fechaCompra;
        this.nroComprobante = nroComprobante;
        this.idProveedor = idProveedor;
        this.idUsuario = idUsuario;
        this.totalCompra = totalCompra;
        this.nombreProveedor = nombreProveedor;
        this.nombreUsuario = nombreUsuario;
    }

    // ✅ Getters y Setters
    public Integer getIdCompra() {
        return idCompra;
    }

    public void setIdCompra(Integer idCompra) {
        this.idCompra = idCompra;
    }

    public LocalDateTime getFechaCompra() {
        return fechaCompra;
    }

    public void setFechaCompra(LocalDateTime fechaCompra) {
        this.fechaCompra = fechaCompra;
    }

    public String getNroComprobante() {
        return nroComprobante;
    }

    public void setNroComprobante(String nroComprobante) {
        this.nroComprobante = nroComprobante;
    }

    public Integer getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(Integer idProveedor) {
        this.idProveedor = idProveedor;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public BigDecimal getTotalCompra() {
        return totalCompra;
    }

    public void setTotalCompra(BigDecimal totalCompra) {
        this.totalCompra = totalCompra;
    }

    public String getNombreProveedor() {
        return nombreProveedor;
    }

    public void setNombreProveedor(String nombreProveedor) {
        this.nombreProveedor = nombreProveedor;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }
}
