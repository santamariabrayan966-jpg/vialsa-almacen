package com.vialsa.almacen.model;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class Producto {

    private Integer idProducto;

    private String foto;

    @Size(max = 45)
    private String codigoInterno;

    @NotBlank
    @Size(max = 45)
    private String nombreProducto;

    @Size(max = 45)
    private String dimensiones;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal precioUnitario;

    @Min(0)
    private Integer stockActual;

    @Min(0)
    private Integer stockMinimo;

    private Integer idUnidad;
    private Integer idTipoProducto;
    private Integer idEstadoProducto;
    private BigDecimal descuentoMaximo;

    public BigDecimal getDescuentoMaximo() { return descuentoMaximo; }
    public void setDescuentoMaximo(BigDecimal descuentoMaximo) { this.descuentoMaximo = descuentoMaximo; }


    // GETTERS & SETTERS

    public Integer getIdProducto() { return idProducto; }
    public void setIdProducto(Integer idProducto) { this.idProducto = idProducto; }

    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; }

    public String getCodigoInterno() { return codigoInterno; }
    public void setCodigoInterno(String codigoInterno) { this.codigoInterno = codigoInterno; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public String getDimensiones() { return dimensiones; }
    public void setDimensiones(String dimensiones) { this.dimensiones = dimensiones; }

    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }

    public Integer getStockActual() { return stockActual; }
    public void setStockActual(Integer stockActual) { this.stockActual = stockActual; }

    public Integer getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(Integer stockMinimo) { this.stockMinimo = stockMinimo; }

    public Integer getIdUnidad() { return idUnidad; }
    public void setIdUnidad(Integer idUnidad) { this.idUnidad = idUnidad; }

    public Integer getIdTipoProducto() { return idTipoProducto; }
    public void setIdTipoProducto(Integer idTipoProducto) { this.idTipoProducto = idTipoProducto; }

    public Integer getIdEstadoProducto() { return idEstadoProducto; }
    public void setIdEstadoProducto(Integer idEstadoProducto) { this.idEstadoProducto = idEstadoProducto; }
}
