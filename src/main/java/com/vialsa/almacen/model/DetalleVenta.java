package com.vialsa.almacen.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DetalleVenta {

    private int idDetalleVenta;
    private int idVenta;
    private int idProducto;
    private int idUnidad;

    private BigDecimal cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal descuento;   // descuento por línea

    // Información complementaria (NO persistente pero útil)
    private String nombreProducto;
    private String nombreUnidad;
    private String dimensiones;
    private BigDecimal costoUnitario;  // último precio de compra validado

    // Estado por si en un futuro gestiona notas de crédito por línea
    private String estado = "ACTIVO";  // ACTIVO / ANULADO

    // ============================
    //   CÁLCULOS PROFESIONALES
    // ============================

    public BigDecimal getSubtotal() {
        if (cantidad == null || precioUnitario == null)
            return BigDecimal.ZERO;

        BigDecimal subtotal = cantidad.multiply(precioUnitario);

        if (descuento != null)
            subtotal = subtotal.subtract(descuento);

        return subtotal.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getSubtotalSinDescuento() {
        if (cantidad == null || precioUnitario == null)
            return BigDecimal.ZERO;

        return cantidad.multiply(precioUnitario).setScale(2, RoundingMode.HALF_UP);
    }

    // ============================
    //   GETTERS / SETTERS
    // ============================

    public int getIdDetalleVenta() { return idDetalleVenta; }
    public void setIdDetalleVenta(int idDetalleVenta) { this.idDetalleVenta = idDetalleVenta; }

    public int getIdVenta() { return idVenta; }
    public void setIdVenta(int idVenta) { this.idVenta = idVenta; }

    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }

    public int getIdUnidad() { return idUnidad; }
    public void setIdUnidad(int idUnidad) { this.idUnidad = idUnidad; }

    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }

    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }

    public BigDecimal getDescuento() { return descuento; }
    public void setDescuento(BigDecimal descuento) { this.descuento = descuento; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public String getNombreUnidad() { return nombreUnidad; }
    public void setNombreUnidad(String nombreUnidad) { this.nombreUnidad = nombreUnidad; }

    public String getDimensiones() { return dimensiones; }
    public void setDimensiones(String dimensiones) { this.dimensiones = dimensiones; }

    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario = costoUnitario; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
