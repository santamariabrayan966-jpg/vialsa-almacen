package com.vialsa.almacen.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Movimiento {

    private Integer idMovimientosAlmacen;
    private String tipoMovimiento;
    private BigDecimal cantidad;
    private LocalDateTime fecha;
    private Integer idProducto;
    private Integer idUnidad;
    private Integer idUsuario;

    // 🔹 Campo adicional para mostrar el nombre del producto en la vista
    private String nombreProducto;

    // ============================================
    // 🆕 NUEVOS CAMPOS (coinciden con tu tabla SQL)
    // ============================================

    private String origen;            // COMPRA / VENTA / MANUAL / AJUSTE / DEVOLUCION
    private Integer idDocumento;      // ID de compra/venta (si aplica)
    private String observacion;       // Nota opcional del movimiento
    private BigDecimal stockAntes;    // Stock antes del movimiento
    private BigDecimal stockDespues;  // Stock después del movimiento

    // ============================================
    // GETTERS & SETTERS
    // ============================================

    public Integer getIdMovimientosAlmacen() {
        return idMovimientosAlmacen;
    }

    public void setIdMovimientosAlmacen(Integer idMovimientosAlmacen) {
        this.idMovimientosAlmacen = idMovimientosAlmacen;
    }

    public String getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(String tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public Integer getIdUnidad() {
        return idUnidad;
    }

    public void setIdUnidad(Integer idUnidad) {
        this.idUnidad = idUnidad;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    // =============================
    // GETTERS & SETTERS NUEVOS
    // =============================

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public Integer getIdDocumento() {
        return idDocumento;
    }

    public void setIdDocumento(Integer idDocumento) {
        this.idDocumento = idDocumento;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public BigDecimal getStockAntes() {
        return stockAntes;
    }

    public void setStockAntes(BigDecimal stockAntes) {
        this.stockAntes = stockAntes;
    }

    public BigDecimal getStockDespues() {
        return stockDespues;
    }

    public void setStockDespues(BigDecimal stockDespues) {
        this.stockDespues = stockDespues;
    }
}
