package com.vialsa.almacen.model;

import java.math.BigDecimal;

public class DetalleCompra {

    private int idDetalleCompra;
    private int idCompra;
    private int idProducto;
    private int idUnidad;

    private BigDecimal cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal descuento;

    private String nombreProducto;
    private String nombreUnidad;

    // ============================
    //          GETTERS
    // ============================

    public int getIdDetalleCompra() { return idDetalleCompra; }
    public void setIdDetalleCompra(int idDetalleCompra) { this.idDetalleCompra = idDetalleCompra; }

    public int getIdCompra() { return idCompra; }
    public void setIdCompra(int idCompra) { this.idCompra = idCompra; }

    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }

    public int getIdUnidad() { return idUnidad; }
    public void setIdUnidad(int idUnidad) { this.idUnidad = idUnidad; }

    public BigDecimal getCantidad() {
        return cantidad != null ? cantidad : BigDecimal.ZERO;
    }
    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad != null ? cantidad : BigDecimal.ZERO;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario != null ? precioUnitario : BigDecimal.ZERO;
    }
    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario != null ? precioUnitario : BigDecimal.ZERO;
    }

    public BigDecimal getDescuento() {
        return descuento != null ? descuento : BigDecimal.ZERO;
    }
    public void setDescuento(BigDecimal descuento) {
        this.descuento = descuento != null ? descuento : BigDecimal.ZERO;
    }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public String getNombreUnidad() { return nombreUnidad; }
    public void setNombreUnidad(String nombreUnidad) { this.nombreUnidad = nombreUnidad; }


    // ============================
    //     SUBTOTAL DINÁMICO
    // ============================
    public BigDecimal getSubtotal() {
        BigDecimal subtotal = getPrecioUnitario().multiply(getCantidad()).subtract(getDescuento());

        if (subtotal.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO; // nunca negativo, igual que el backend
        }
        return subtotal;
    }
}
