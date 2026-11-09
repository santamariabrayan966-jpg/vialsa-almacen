package com.dentia.model.interno.inventario;

import java.math.BigDecimal;

public class InventarioAjusteDetalle {

    private Integer idDetalleAjuste;
    private Integer idAjuste;
    private Integer idProducto;

    // NUEVO: presentación con la que se ajusta el inventario
    // Valores esperados: UNIDAD, MEDIA, PIE2, METRO_LINEAL
    private String tipoPresentacion;

    // NUEVO: motivo específico del producto ajustado
    // Ej: CONTEO_FISICO, PERDIDA, DANO, VENCIMIENTO, ROTURA, OTRO
    private String motivoDetalle;

    // NUEVO: factor usado para saber cuánto afecta al stock
    // Ej: UNIDAD=1, MEDIA=0.5, METRO_LINEAL=1, PIE2=según cálculo
    private BigDecimal factorConversionStock;

    // Cantidad ingresada por el usuario
    private BigDecimal cantidad;

    // NUEVO: cantidad real que se descontó o aumentó del stock
    // Ej: 1 MEDIA puede descontar 0.5 del stock equivalente
    private BigDecimal cantidadDescontadaStock;

    private BigDecimal stockAnterior;
    private BigDecimal stockNuevo;

    // NUEVO: costo perdido si el motivo es pérdida, daño, vencimiento o rotura
    private BigDecimal valorPerdida;

    // NUEVO: observación específica del producto ajustado
    private String observacionDetalle;

    private String codigoProducto;
    private String nombreProducto;

    public Integer getIdDetalleAjuste() {
        return idDetalleAjuste;
    }

    public void setIdDetalleAjuste(Integer idDetalleAjuste) {
        this.idDetalleAjuste = idDetalleAjuste;
    }

    public Integer getIdAjuste() {
        return idAjuste;
    }

    public void setIdAjuste(Integer idAjuste) {
        this.idAjuste = idAjuste;
    }

    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public String getTipoPresentacion() {
        return tipoPresentacion;
    }

    public void setTipoPresentacion(String tipoPresentacion) {
        this.tipoPresentacion = tipoPresentacion;
    }

    public String getMotivoDetalle() {
        return motivoDetalle;
    }

    public void setMotivoDetalle(String motivoDetalle) {
        this.motivoDetalle = motivoDetalle;
    }

    public BigDecimal getFactorConversionStock() {
        return factorConversionStock;
    }

    public void setFactorConversionStock(BigDecimal factorConversionStock) {
        this.factorConversionStock = factorConversionStock;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getCantidadDescontadaStock() {
        return cantidadDescontadaStock;
    }

    public void setCantidadDescontadaStock(BigDecimal cantidadDescontadaStock) {
        this.cantidadDescontadaStock = cantidadDescontadaStock;
    }

    public BigDecimal getStockAnterior() {
        return stockAnterior;
    }

    public void setStockAnterior(BigDecimal stockAnterior) {
        this.stockAnterior = stockAnterior;
    }

    public BigDecimal getStockNuevo() {
        return stockNuevo;
    }

    public void setStockNuevo(BigDecimal stockNuevo) {
        this.stockNuevo = stockNuevo;
    }

    public BigDecimal getValorPerdida() {
        return valorPerdida;
    }

    public void setValorPerdida(BigDecimal valorPerdida) {
        this.valorPerdida = valorPerdida;
    }

    public String getObservacionDetalle() {
        return observacionDetalle;
    }

    public void setObservacionDetalle(String observacionDetalle) {
        this.observacionDetalle = observacionDetalle;
    }

    public String getCodigoProducto() {
        return codigoProducto;
    }

    public void setCodigoProducto(String codigoProducto) {
        this.codigoProducto = codigoProducto;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }
}