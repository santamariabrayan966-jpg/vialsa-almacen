package com.dentia.model.interno;

import java.math.BigDecimal;

public class CompraRecepcionDetalle {

    private Integer idRecepcionDetalle;
    private Integer idRecepcion;
    private Integer idDetalleCompra;
    private Integer idProducto;
    private Integer idUnidad;
    private BigDecimal cantidadRecibida;

    private String nombreProducto;
    private String nombreUnidad;
    private BigDecimal cantidadComprada;
    private BigDecimal cantidadYaRecibida;
    private BigDecimal cantidadPendiente;

    public Integer getIdRecepcionDetalle() { return idRecepcionDetalle; }
    public void setIdRecepcionDetalle(Integer idRecepcionDetalle) { this.idRecepcionDetalle = idRecepcionDetalle; }

    public Integer getIdRecepcion() { return idRecepcion; }
    public void setIdRecepcion(Integer idRecepcion) { this.idRecepcion = idRecepcion; }

    public Integer getIdDetalleCompra() { return idDetalleCompra; }
    public void setIdDetalleCompra(Integer idDetalleCompra) { this.idDetalleCompra = idDetalleCompra; }

    public Integer getIdProducto() { return idProducto; }
    public void setIdProducto(Integer idProducto) { this.idProducto = idProducto; }

    public Integer getIdUnidad() { return idUnidad; }
    public void setIdUnidad(Integer idUnidad) { this.idUnidad = idUnidad; }

    public BigDecimal getCantidadRecibida() { return cantidadRecibida; }
    public void setCantidadRecibida(BigDecimal cantidadRecibida) { this.cantidadRecibida = cantidadRecibida; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public String getNombreUnidad() { return nombreUnidad; }
    public void setNombreUnidad(String nombreUnidad) { this.nombreUnidad = nombreUnidad; }

    public BigDecimal getCantidadComprada() { return cantidadComprada; }
    public void setCantidadComprada(BigDecimal cantidadComprada) { this.cantidadComprada = cantidadComprada; }

    public BigDecimal getCantidadYaRecibida() { return cantidadYaRecibida; }
    public void setCantidadYaRecibida(BigDecimal cantidadYaRecibida) { this.cantidadYaRecibida = cantidadYaRecibida; }

    public BigDecimal getCantidadPendiente() { return cantidadPendiente; }
    public void setCantidadPendiente(BigDecimal cantidadPendiente) { this.cantidadPendiente = cantidadPendiente; }
}