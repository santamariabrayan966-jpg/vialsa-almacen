package com.dentia.model.interno.inventario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InventarioParte {

    private Integer idParte;
    private Integer idProducto;
    private Integer idSede;

    private String tipoParte;
    private Integer cantidadPartes;
    private String descripcion;

    private String tipoMedida;
    private String unidadMedida;

    private Integer medidaDisponible;
    private Integer medidaPorParte;
    private BigDecimal medidaFisicaDisponible;

    /*
     * Dimensiones físicas reales de la pieza.
     * Se usan principalmente para RETAZO, MEDIA y COMPLETA de tipo AREA.
     */
    private BigDecimal largoDisponibleMm;
    private BigDecimal anchoDisponibleMm;

    /*
     * Trazabilidad de la pieza o retazo.
     */
    private Integer idParteOrigen;
    private Integer idDetalleVentaOrigen;
    private Integer numeroPlanoOrigen;

    private BigDecimal equivalenteUnidadFisica;

    private BigDecimal costoTotalOriginal;
    private BigDecimal costoTotalDisponible;
    private BigDecimal costoUnitarioParte;
    private BigDecimal costoPorMedida;

    private String estado;

    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaActualizacion;

    public Integer getIdParte() {
        return idParte;
    }

    public void setIdParte(Integer idParte) {
        this.idParte = idParte;
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

    public String getTipoParte() {
        return tipoParte;
    }

    public void setTipoParte(String tipoParte) {
        this.tipoParte = tipoParte;
    }

    public Integer getCantidadPartes() {
        return cantidadPartes;
    }

    public void setCantidadPartes(Integer cantidadPartes) {
        this.cantidadPartes = cantidadPartes;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getTipoMedida() {
        return tipoMedida;
    }

    public void setTipoMedida(String tipoMedida) {
        this.tipoMedida = tipoMedida;
    }

    public String getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(String unidadMedida) {
        this.unidadMedida = unidadMedida;
    }

    public Integer getMedidaDisponible() {
        return medidaDisponible;
    }

    public void setMedidaDisponible(Integer medidaDisponible) {
        this.medidaDisponible = medidaDisponible;
    }

    public Integer getMedidaPorParte() {
        return medidaPorParte;
    }

    public void setMedidaPorParte(Integer medidaPorParte) {
        this.medidaPorParte = medidaPorParte;
    }

    public BigDecimal getMedidaFisicaDisponible() {
        return medidaFisicaDisponible;
    }

    public void setMedidaFisicaDisponible(BigDecimal medidaFisicaDisponible) {
        this.medidaFisicaDisponible = medidaFisicaDisponible;
    }

    public BigDecimal getLargoDisponibleMm() {
        return largoDisponibleMm;
    }

    public void setLargoDisponibleMm(BigDecimal largoDisponibleMm) {
        this.largoDisponibleMm = largoDisponibleMm;
    }

    public BigDecimal getAnchoDisponibleMm() {
        return anchoDisponibleMm;
    }

    public void setAnchoDisponibleMm(BigDecimal anchoDisponibleMm) {
        this.anchoDisponibleMm = anchoDisponibleMm;
    }

    public Integer getIdParteOrigen() {
        return idParteOrigen;
    }

    public void setIdParteOrigen(Integer idParteOrigen) {
        this.idParteOrigen = idParteOrigen;
    }

    public Integer getIdDetalleVentaOrigen() {
        return idDetalleVentaOrigen;
    }

    public void setIdDetalleVentaOrigen(Integer idDetalleVentaOrigen) {
        this.idDetalleVentaOrigen = idDetalleVentaOrigen;
    }

    public Integer getNumeroPlanoOrigen() {
        return numeroPlanoOrigen;
    }

    public void setNumeroPlanoOrigen(Integer numeroPlanoOrigen) {
        this.numeroPlanoOrigen = numeroPlanoOrigen;
    }

    public BigDecimal getEquivalenteUnidadFisica() {
        return equivalenteUnidadFisica;
    }

    public void setEquivalenteUnidadFisica(BigDecimal equivalenteUnidadFisica) {
        this.equivalenteUnidadFisica = equivalenteUnidadFisica;
    }

    public BigDecimal getCostoTotalOriginal() {
        return costoTotalOriginal;
    }

    public void setCostoTotalOriginal(BigDecimal costoTotalOriginal) {
        this.costoTotalOriginal = costoTotalOriginal;
    }

    public BigDecimal getCostoTotalDisponible() {
        return costoTotalDisponible;
    }

    public void setCostoTotalDisponible(BigDecimal costoTotalDisponible) {
        this.costoTotalDisponible = costoTotalDisponible;
    }

    public BigDecimal getCostoUnitarioParte() {
        return costoUnitarioParte;
    }

    public void setCostoUnitarioParte(BigDecimal costoUnitarioParte) {
        this.costoUnitarioParte = costoUnitarioParte;
    }

    public BigDecimal getCostoPorMedida() {
        return costoPorMedida;
    }

    public void setCostoPorMedida(BigDecimal costoPorMedida) {
        this.costoPorMedida = costoPorMedida;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public boolean puedeContenerCorte(
            BigDecimal anchoCorteMm,
            BigDecimal altoCorteMm
    ) {
        if (anchoCorteMm == null
                || altoCorteMm == null
                || anchoCorteMm.signum() <= 0
                || altoCorteMm.signum() <= 0) {
            return false;
        }

        if (largoDisponibleMm == null
                || anchoDisponibleMm == null
                || largoDisponibleMm.signum() <= 0
                || anchoDisponibleMm.signum() <= 0) {
            return false;
        }

        boolean posicionNormal =
                anchoCorteMm.compareTo(largoDisponibleMm) <= 0
                        && altoCorteMm.compareTo(anchoDisponibleMm) <= 0;

        boolean posicionRotada =
                altoCorteMm.compareTo(largoDisponibleMm) <= 0
                        && anchoCorteMm.compareTo(anchoDisponibleMm) <= 0;

        return posicionNormal || posicionRotada;
    }
}