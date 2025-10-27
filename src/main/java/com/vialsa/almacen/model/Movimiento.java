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

    // ✅ Getters y Setters

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

    // 🔹 Nuevo campo para mostrar nombre del producto
    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }
}
