package com.dentia.model.interno.inventario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InventarioMovimiento {

    private Integer idMovimiento;
    private Integer idProducto;
    private Integer idSede;

    private String codigoProducto;
    private String nombreProducto;
    private String nombreSede;
    private String tipoSede;

    private String tipoMovimiento;
    private String origenMovimiento;
    private String documentoReferencia;

    private BigDecimal entrada;
    private BigDecimal salida;
    private BigDecimal stockResultante;
    private BigDecimal costoUnitario;

    private String observacion;
    private Integer idUsuario;
    private String nombreUsuario;
    private LocalDateTime fechaMovimiento;

    public Integer getIdMovimiento() { return idMovimiento; }
    public void setIdMovimiento(Integer idMovimiento) { this.idMovimiento = idMovimiento; }

    public Integer getIdProducto() { return idProducto; }
    public void setIdProducto(Integer idProducto) { this.idProducto = idProducto; }

    public Integer getIdSede() { return idSede; }
    public void setIdSede(Integer idSede) { this.idSede = idSede; }

    public String getCodigoProducto() { return codigoProducto; }
    public void setCodigoProducto(String codigoProducto) { this.codigoProducto = codigoProducto; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public String getNombreSede() { return nombreSede; }
    public void setNombreSede(String nombreSede) { this.nombreSede = nombreSede; }

    public String getTipoSede() { return tipoSede; }
    public void setTipoSede(String tipoSede) { this.tipoSede = tipoSede; }

    public String getTipoMovimiento() { return tipoMovimiento; }
    public void setTipoMovimiento(String tipoMovimiento) { this.tipoMovimiento = tipoMovimiento; }

    public String getOrigenMovimiento() { return origenMovimiento; }
    public void setOrigenMovimiento(String origenMovimiento) { this.origenMovimiento = origenMovimiento; }

    public String getDocumentoReferencia() { return documentoReferencia; }
    public void setDocumentoReferencia(String documentoReferencia) { this.documentoReferencia = documentoReferencia; }

    public BigDecimal getEntrada() { return entrada; }
    public void setEntrada(BigDecimal entrada) { this.entrada = entrada; }

    public BigDecimal getSalida() { return salida; }
    public void setSalida(BigDecimal salida) { this.salida = salida; }

    public BigDecimal getStockResultante() { return stockResultante; }
    public void setStockResultante(BigDecimal stockResultante) { this.stockResultante = stockResultante; }

    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario = costoUnitario; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public LocalDateTime getFechaMovimiento() { return fechaMovimiento; }
    public void setFechaMovimiento(LocalDateTime fechaMovimiento) { this.fechaMovimiento = fechaMovimiento; }

    public String getEntradaTexto() {
        if (entrada == null) {
            return "0";
        }

        return entrada
                .stripTrailingZeros()
                .toPlainString();
    }

    public String getSalidaTexto() {
        if (salida == null) {
            return "0";
        }

        return salida
                .stripTrailingZeros()
                .toPlainString();
    }

    public String getStockResultanteTexto() {
        if (stockResultante == null) {
            return "0";
        }

        return stockResultante
                .stripTrailingZeros()
                .toPlainString();
    }

    public String getCostoUnitarioTexto() {
        if (costoUnitario == null) {
            return "0";
        }

        return costoUnitario
                .stripTrailingZeros()
                .toPlainString();
    }
}
