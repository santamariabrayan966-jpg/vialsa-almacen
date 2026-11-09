package com.dentia.model.interno;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DetalleVenta {

    private int idDetalleVenta;
    private int idVenta;
    private int idProducto;
    private int idUnidad;

    private Integer idPrecioPresentacion;
    private String nombrePresentacion;
    private BigDecimal factorConversionStock;
    private BigDecimal cantidadDescontadaStock;

    private BigDecimal cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal precioNormal;
    private BigDecimal precioOferta;
    private BigDecimal precioFinalOnline;
    private BigDecimal descuentoOferta;
    private Integer idOferta;
    private BigDecimal descuento;

    // Información complementaria (NO persistente pero útil)
    private String nombreProducto;
    private String nombreUnidad;
    private String dimensiones;
    private BigDecimal costoUnitario;

    // Estado por si en un futuro gestiona notas de crédito por línea
    private String estado = "ACTIVO";

    private String foto;

    // ============================
    //   PREPARACIÓN / CORTE
    // ============================
    private boolean requierePreparacion;
    private String tipoPreparacion;
    private boolean entregarSobrante;
    private String observacionPreparacion;
    private String cortesJson;
    private String planoCorteJson;
    private String croquisSvg;
    private String instruccionMaterial;
    private String grupoMedidaUuid;
    private boolean lineaPrincipalMedida;

    // ============================
    //   CÁLCULOS PROFESIONALES
    // ============================

    public BigDecimal getSubtotal() {
        if (cantidad == null || precioUnitario == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal subtotal = cantidad.multiply(precioUnitario);

        if (descuento != null) {
            subtotal = subtotal.subtract(descuento);
        }

        return subtotal.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getSubtotalSinDescuento() {
        if (cantidad == null || precioUnitario == null) {
            return BigDecimal.ZERO;
        }

        return cantidad.multiply(precioUnitario)
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ============================
    //   GETTERS / SETTERS
    // ============================

    public int getIdDetalleVenta() {
        return idDetalleVenta;
    }

    public void setIdDetalleVenta(int idDetalleVenta) {
        this.idDetalleVenta = idDetalleVenta;
    }

    public int getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(int idVenta) {
        this.idVenta = idVenta;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public int getIdUnidad() {
        return idUnidad;
    }

    public void setIdUnidad(int idUnidad) {
        this.idUnidad = idUnidad;
    }

    public Integer getIdPrecioPresentacion() {
        return idPrecioPresentacion;
    }

    public void setIdPrecioPresentacion(Integer idPrecioPresentacion) {
        this.idPrecioPresentacion = idPrecioPresentacion;
    }

    public String getNombrePresentacion() {
        return nombrePresentacion;
    }

    public void setNombrePresentacion(String nombrePresentacion) {
        this.nombrePresentacion = nombrePresentacion;
    }

    public BigDecimal getFactorConversionStock() {
        return factorConversionStock;
    }

    public void setFactorConversionStock(BigDecimal factorConversionStock) {
        this.factorConversionStock = factorConversionStock;
    }

    public BigDecimal getCantidadDescontadaStock() {
        return cantidadDescontadaStock;
    }

    public void setCantidadDescontadaStock(BigDecimal cantidadDescontadaStock) {
        this.cantidadDescontadaStock = cantidadDescontadaStock;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public void setDescuento(BigDecimal descuento) {
        this.descuento = descuento;
    }

    public BigDecimal getPrecioNormal() {
        return precioNormal;
    }

    public void setPrecioNormal(BigDecimal precioNormal) {
        this.precioNormal = precioNormal;
    }

    public BigDecimal getPrecioOferta() {
        return precioOferta;
    }

    public void setPrecioOferta(BigDecimal precioOferta) {
        this.precioOferta = precioOferta;
    }

    public BigDecimal getPrecioFinalOnline() {
        return precioFinalOnline;
    }

    public void setPrecioFinalOnline(BigDecimal precioFinalOnline) {
        this.precioFinalOnline = precioFinalOnline;
    }

    public BigDecimal getDescuentoOferta() {
        return descuentoOferta;
    }

    public void setDescuentoOferta(BigDecimal descuentoOferta) {
        this.descuentoOferta = descuentoOferta;
    }

    public Integer getIdOferta() {
        return idOferta;
    }

    public void setIdOferta(Integer idOferta) {
        this.idOferta = idOferta;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public String getNombreUnidad() {
        return nombreUnidad;
    }

    public void setNombreUnidad(String nombreUnidad) {
        this.nombreUnidad = nombreUnidad;
    }

    public String getDimensiones() {
        return dimensiones;
    }

    public void setDimensiones(String dimensiones) {
        this.dimensiones = dimensiones;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(BigDecimal costoUnitario) {
        this.costoUnitario = costoUnitario;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public boolean isRequierePreparacion() {
        return requierePreparacion;
    }

    public void setRequierePreparacion(boolean requierePreparacion) {
        this.requierePreparacion = requierePreparacion;
    }

    public String getTipoPreparacion() {
        return tipoPreparacion;
    }

    public void setTipoPreparacion(String tipoPreparacion) {
        this.tipoPreparacion = tipoPreparacion;
    }

    public boolean isEntregarSobrante() {
        return entregarSobrante;
    }

    public void setEntregarSobrante(boolean entregarSobrante) {
        this.entregarSobrante = entregarSobrante;
    }

    public String getObservacionPreparacion() {
        return observacionPreparacion;
    }

    public void setObservacionPreparacion(String observacionPreparacion) {
        this.observacionPreparacion = observacionPreparacion;
    }

    public String getCortesJson() {
        return cortesJson;
    }

    public void setCortesJson(String cortesJson) {
        this.cortesJson = cortesJson;
    }

    public String getPlanoCorteJson() {
        return planoCorteJson;
    }

    public void setPlanoCorteJson(String planoCorteJson) {
        this.planoCorteJson = planoCorteJson;
    }

    public String getCroquisSvg() {
        return croquisSvg;
    }

    public void setCroquisSvg(String croquisSvg) {
        this.croquisSvg = croquisSvg;
    }

    public String getInstruccionMaterial() {
        return instruccionMaterial;
    }

    public void setInstruccionMaterial(String instruccionMaterial) {
        this.instruccionMaterial = instruccionMaterial;
    }

    public String getGrupoMedidaUuid() {
        return grupoMedidaUuid;
    }

    public void setGrupoMedidaUuid(String grupoMedidaUuid) {
        this.grupoMedidaUuid = grupoMedidaUuid;
    }

    public boolean isLineaPrincipalMedida() {
        return lineaPrincipalMedida;
    }

    public void setLineaPrincipalMedida(boolean lineaPrincipalMedida) {
        this.lineaPrincipalMedida = lineaPrincipalMedida;
    }
}