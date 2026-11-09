package com.dentia.model.interno.inventario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InventarioStock {

    private Integer idStock;
    private Integer idProducto;
    private Integer idSede;

    private String codigoProducto;
    private String nombreProducto;
    private String fotoProducto;
    private String categoria;

    private String nombreSede;
    private String tipoSede;
    private String ciudadSede;
    private String direccionSede;

    private BigDecimal stockActual;
    private BigDecimal stockMinimo;
    private BigDecimal stockMaximo;
    private BigDecimal costoPromedio;
    private BigDecimal valorAprox;

    private Boolean activo;
    private LocalDateTime fechaActualizacion;

    public Integer getIdStock() { return idStock; }
    public void setIdStock(Integer idStock) { this.idStock = idStock; }

    public Integer getIdProducto() { return idProducto; }
    public void setIdProducto(Integer idProducto) { this.idProducto = idProducto; }

    public Integer getIdSede() { return idSede; }
    public void setIdSede(Integer idSede) { this.idSede = idSede; }

    public String getCodigoProducto() { return codigoProducto; }
    public void setCodigoProducto(String codigoProducto) { this.codigoProducto = codigoProducto; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public String getFotoProducto() { return fotoProducto; }
    public void setFotoProducto(String fotoProducto) { this.fotoProducto = fotoProducto; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getNombreSede() { return nombreSede; }
    public void setNombreSede(String nombreSede) { this.nombreSede = nombreSede; }

    public String getTipoSede() { return tipoSede; }
    public void setTipoSede(String tipoSede) { this.tipoSede = tipoSede; }

    public String getCiudadSede() { return ciudadSede; }
    public void setCiudadSede(String ciudadSede) { this.ciudadSede = ciudadSede; }

    public String getDireccionSede() { return direccionSede; }
    public void setDireccionSede(String direccionSede) { this.direccionSede = direccionSede; }

    public BigDecimal getStockActual() { return stockActual; }
    public void setStockActual(BigDecimal stockActual) { this.stockActual = stockActual; }

    public BigDecimal getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(BigDecimal stockMinimo) { this.stockMinimo = stockMinimo; }

    public BigDecimal getStockMaximo() { return stockMaximo; }
    public void setStockMaximo(BigDecimal stockMaximo) { this.stockMaximo = stockMaximo; }

    public BigDecimal getCostoPromedio() { return costoPromedio; }
    public void setCostoPromedio(BigDecimal costoPromedio) { this.costoPromedio = costoPromedio; }

    public BigDecimal getValorAprox() { return valorAprox; }
    public void setValorAprox(BigDecimal valorAprox) { this.valorAprox = valorAprox; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public String getStockActualTexto() {
        if (stockActual == null) {
            return "0.00";
        }

        return stockActual
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .toPlainString();
    }

    public String getStockMinimoTexto() {
        if (stockMinimo == null) {
            return "0";
        }

        return stockMinimo
                .stripTrailingZeros()
                .toPlainString();
    }

    public String getStockMaximoTexto() {
        if (stockMaximo == null) {
            return "0";
        }

        return stockMaximo
                .stripTrailingZeros()
                .toPlainString();
    }
}
