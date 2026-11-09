package com.dentia.model.interno;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ProductoPrecioPresentacion {

    private Integer idPrecioPresentacion;
    private Integer idProducto;
    private Integer idSede;
    private String nombrePresentacion;
    private Integer idUnidadVenta;
    private String tipoCalculoVenta;
    private BigDecimal cantidadIncluida;
    private BigDecimal largoBase;
    private BigDecimal anchoBase;
    private BigDecimal medidaTotalBase;
    private BigDecimal factorConversionStock;
    private BigDecimal precioVenta;
    private BigDecimal descuentoMaximo;
    private Boolean precioPortada;
    private Boolean precioBajoCosto;
    private Boolean margenBajo;
    private BigDecimal costoBaseCalculado;
    private BigDecimal precioMinimoRecomendado;
    private String motivoPrecioBajoCosto;
    private String usuarioAutorizaPrecio;
    private java.time.LocalDateTime fechaAutorizacionPrecio;
    private String descripcionUso;
    private Integer estado;
    private BigDecimal precioOfertaPortada;
    private Boolean tieneOfertaActiva;
    private String textoOfertaPortada;
    private LocalDate fechaInicioOferta;
    private LocalDate fechaFinOferta;

    // Campos para mostrar en la vista
    private String nombreUnidadVenta;
    private String abreviaturaUnidadVenta;

    public Integer getIdPrecioPresentacion() {
        return idPrecioPresentacion;
    }

    public void setIdPrecioPresentacion(Integer idPrecioPresentacion) {
        this.idPrecioPresentacion = idPrecioPresentacion;
    }

    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public Integer getIdSede() {
        return idSede;
    }

    public void setIdSede(Integer idSede) {
        this.idSede = idSede;
    }

    public String getNombrePresentacion() {
        return nombrePresentacion;
    }

    public void setNombrePresentacion(String nombrePresentacion) {
        this.nombrePresentacion = nombrePresentacion;
    }

    public Integer getIdUnidadVenta() {
        return idUnidadVenta;
    }

    public void setIdUnidadVenta(Integer idUnidadVenta) {
        this.idUnidadVenta = idUnidadVenta;
    }

    public BigDecimal getCantidadIncluida() {
        return cantidadIncluida;
    }

    public void setCantidadIncluida(BigDecimal cantidadIncluida) {
        this.cantidadIncluida = cantidadIncluida;
    }

    public BigDecimal getFactorConversionStock() {
        return factorConversionStock;
    }

    public void setFactorConversionStock(BigDecimal factorConversionStock) {
        this.factorConversionStock = factorConversionStock;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }

    public BigDecimal getDescuentoMaximo() {
        return descuentoMaximo;
    }

    public void setDescuentoMaximo(BigDecimal descuentoMaximo) {
        this.descuentoMaximo = descuentoMaximo;
    }

    public Boolean getPrecioPortada() {
        return precioPortada;
    }

    public void setPrecioPortada(Boolean precioPortada) {
        this.precioPortada = precioPortada;
    }

    public Integer getEstado() {
        return estado;
    }

    public void setEstado(Integer estado) {
        this.estado = estado;
    }

    public String getNombreUnidadVenta() {
        return nombreUnidadVenta;
    }

    public void setNombreUnidadVenta(String nombreUnidadVenta) {
        this.nombreUnidadVenta = nombreUnidadVenta;
    }

    public String getAbreviaturaUnidadVenta() {
        return abreviaturaUnidadVenta;
    }

    public void setAbreviaturaUnidadVenta(String abreviaturaUnidadVenta) {
        this.abreviaturaUnidadVenta = abreviaturaUnidadVenta;
    }

    public String getTipoCalculoVenta() {
        return tipoCalculoVenta;
    }

    public void setTipoCalculoVenta(String tipoCalculoVenta) {
        this.tipoCalculoVenta = tipoCalculoVenta;
    }

    public BigDecimal getLargoBase() {
        return largoBase;
    }

    public void setLargoBase(BigDecimal largoBase) {
        this.largoBase = largoBase;
    }

    public BigDecimal getAnchoBase() {
        return anchoBase;
    }

    public void setAnchoBase(BigDecimal anchoBase) {
        this.anchoBase = anchoBase;
    }

    public BigDecimal getMedidaTotalBase() {
        return medidaTotalBase;
    }

    public void setMedidaTotalBase(BigDecimal medidaTotalBase) {
        this.medidaTotalBase = medidaTotalBase;
    }

    public String getDescripcionUso() {
        return descripcionUso;
    }

    public void setDescripcionUso(String descripcionUso) {
        this.descripcionUso = descripcionUso;
    }

    public Boolean getPrecioBajoCosto() {
        return precioBajoCosto;
    }

    public void setPrecioBajoCosto(Boolean precioBajoCosto) {
        this.precioBajoCosto = precioBajoCosto;
    }

    public Boolean getMargenBajo() {
        return margenBajo;
    }

    public void setMargenBajo(Boolean margenBajo) {
        this.margenBajo = margenBajo;
    }

    public BigDecimal getCostoBaseCalculado() {
        return costoBaseCalculado;
    }

    public void setCostoBaseCalculado(BigDecimal costoBaseCalculado) {
        this.costoBaseCalculado = costoBaseCalculado;
    }

    public BigDecimal getPrecioMinimoRecomendado() {
        return precioMinimoRecomendado;
    }

    public void setPrecioMinimoRecomendado(BigDecimal precioMinimoRecomendado) {
        this.precioMinimoRecomendado = precioMinimoRecomendado;
    }

    public String getMotivoPrecioBajoCosto() {
        return motivoPrecioBajoCosto;
    }

    public void setMotivoPrecioBajoCosto(String motivoPrecioBajoCosto) {
        this.motivoPrecioBajoCosto = motivoPrecioBajoCosto;
    }

    public String getUsuarioAutorizaPrecio() {
        return usuarioAutorizaPrecio;
    }

    public void setUsuarioAutorizaPrecio(String usuarioAutorizaPrecio) {
        this.usuarioAutorizaPrecio = usuarioAutorizaPrecio;
    }

    public java.time.LocalDateTime getFechaAutorizacionPrecio() {
        return fechaAutorizacionPrecio;
    }

    public void setFechaAutorizacionPrecio(java.time.LocalDateTime fechaAutorizacionPrecio) {
        this.fechaAutorizacionPrecio = fechaAutorizacionPrecio;
    }

    public BigDecimal getPrecioOfertaPortada() {
        return precioOfertaPortada;
    }

    public void setPrecioOfertaPortada(BigDecimal precioOfertaPortada) {
        this.precioOfertaPortada = precioOfertaPortada;
    }

    public Boolean getTieneOfertaActiva() {
        return tieneOfertaActiva;
    }

    public void setTieneOfertaActiva(Boolean tieneOfertaActiva) {
        this.tieneOfertaActiva = tieneOfertaActiva;
    }

    public String getTextoOfertaPortada() {
        return textoOfertaPortada;
    }

    public void setTextoOfertaPortada(String textoOfertaPortada) {
        this.textoOfertaPortada = textoOfertaPortada;
    }

    public LocalDate getFechaInicioOferta() {
        return fechaInicioOferta;
    }

    public void setFechaInicioOferta(LocalDate fechaInicioOferta) {
        this.fechaInicioOferta = fechaInicioOferta;
    }

    public LocalDate getFechaFinOferta() {
        return fechaFinOferta;
    }

    public void setFechaFinOferta(LocalDate fechaFinOferta) {
        this.fechaFinOferta = fechaFinOferta;
    }

}
