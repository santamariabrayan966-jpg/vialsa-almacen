package com.dentia.model.interno;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;


public class Producto {

    private Integer idProducto;

    private String foto;
    private String fotoPortada;

    @Size(max = 45)
    private String codigoInterno;

    @NotBlank(message = "El nombre del producto es obligatorio.")
    @Size(max = 45, message = "El nombre no debe superar los 45 caracteres.")
    private String nombreProducto;

    @Size(max = 45)
    private String dimensiones;

    @NotNull(message = "El precio principal es obligatorio.")
    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo.")
    private BigDecimal precioUnitario;

    /*
     * CAMPOS TEMPORALES / HEREDADOS
     * Luego stockActual y stockMinimo deben manejarse desde inventario_stock.
     */
    @Min(value = 0, message = "El stock actual no puede ser negativo.")
    private Integer stockActual;

    @Min(value = 0, message = "El stock mínimo no puede ser negativo.")
    private Integer stockMinimo;
    private Integer idUnidad;
    private String controlInventario;
    private String nombreUnidadInventario;
    private String formaVenta;
    private String nombreUnidadVenta;
    private String abreviaturaUnidadVenta;
    private Integer idEstadoProducto;

    private BigDecimal stockActualDecimal;

    /*
     * CAMPOS PARA VENTA POR MEDIDA
     * Estos campos indican a cuánto equivale realmente
     * una unidad de inventario del producto.
     *
     * Ejemplo vidrio:
     * 1 plancha = 8.20 m² reales
     * medidaVendibleBase = 8 m² vendibles
     *
     * Ejemplo aluminio:
     * 1 varilla = 5.90 m reales
     * medidaVendibleBase = 5 m vendibles
     */
    private Boolean ventaPorMedida;
    private String tipoMedidaBase;
    private BigDecimal largoBase;
    private BigDecimal anchoBase;
    private BigDecimal medidaTotalBase;
    private Integer medidaVendibleBase;
    private String descripcionMedidaBase;

    private BigDecimal descuentoMaximo;

    /*
     * CAMPOS NUEVOS DEL CATÁLOGO MAESTRO
     */
    private Integer idMarca;
    private Integer idCategoria;
    private Integer idSubcategoria;

    @Size(max = 80)
    private String modeloProducto;

    @Size(max = 255)
    private String descripcionProducto;

    /*
     * CAMPOS PARA MOSTRAR EN LA VISTA
     * Estos se llenan con LEFT JOIN en el DAO.
     * No son columnas directas obligatorias de productos.
     */
    private String nombreMarca;
    private String nombreCategoria;
    private String nombreSubcategoria;
    private String nombreUnidad;
    private String abreviaturaUnidad;
    private String tipoCalculoUnidad;
    private BigDecimal costoPromedio;
    private BigDecimal margenMinimoPorcentaje;
    private BigDecimal precioOfertaPortada;
    private Boolean tieneOfertaActiva;
    private String textoOfertaPortada;
    private LocalDate fechaInicioOferta;
    private LocalDate fechaFinOferta;

    // ============================================================
    // GETTERS & SETTERS
    // ============================================================

    public Integer getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(Integer idProducto) {
        this.idProducto = idProducto;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public String getFotoPortada() {
        return fotoPortada;
    }

    public void setFotoPortada(String fotoPortada) {
        this.fotoPortada = fotoPortada;
    }

    public String getCodigoInterno() {
        return codigoInterno;
    }

    public void setCodigoInterno(String codigoInterno) {
        this.codigoInterno = codigoInterno;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public String getDimensiones() {
        return dimensiones;
    }

    public void setDimensiones(String dimensiones) {
        this.dimensiones = dimensiones;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public Integer getStockActual() {
        return stockActual;
    }

    public void setStockActual(Integer stockActual) {
        this.stockActual = stockActual;
    }

    public Integer getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(Integer stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public Integer getIdUnidad() {
        return idUnidad;
    }

    public void setIdUnidad(Integer idUnidad) {
        this.idUnidad = idUnidad;
    }

    public Integer getIdEstadoProducto() {
        return idEstadoProducto;
    }

    public void setIdEstadoProducto(Integer idEstadoProducto) {
        this.idEstadoProducto = idEstadoProducto;
    }

    public Boolean getVentaPorMedida() {
        return ventaPorMedida;
    }

    public void setVentaPorMedida(Boolean ventaPorMedida) {
        this.ventaPorMedida = ventaPorMedida;
    }

    public String getTipoMedidaBase() {
        return tipoMedidaBase;
    }

    public void setTipoMedidaBase(String tipoMedidaBase) {
        this.tipoMedidaBase = tipoMedidaBase;
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

    public Integer getMedidaVendibleBase() {
        return medidaVendibleBase;
    }

    public void setMedidaVendibleBase(Integer medidaVendibleBase) {
        this.medidaVendibleBase = medidaVendibleBase;
    }

    public String getDescripcionMedidaBase() {
        return descripcionMedidaBase;
    }

    public void setDescripcionMedidaBase(String descripcionMedidaBase) {
        this.descripcionMedidaBase = descripcionMedidaBase;
    }

    public BigDecimal getDescuentoMaximo() {
        return descuentoMaximo;
    }

    public void setDescuentoMaximo(BigDecimal descuentoMaximo) {
        this.descuentoMaximo = descuentoMaximo;
    }

    public Integer getIdMarca() {
        return idMarca;
    }

    public void setIdMarca(Integer idMarca) {
        this.idMarca = idMarca;
    }

    public Integer getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(Integer idCategoria) {
        this.idCategoria = idCategoria;
    }

    public Integer getIdSubcategoria() {
        return idSubcategoria;
    }

    public void setIdSubcategoria(Integer idSubcategoria) {
        this.idSubcategoria = idSubcategoria;
    }

    public String getModeloProducto() {
        return modeloProducto;
    }

    public void setModeloProducto(String modeloProducto) {
        this.modeloProducto = modeloProducto;
    }

    public String getDescripcionProducto() {
        return descripcionProducto;
    }

    public void setDescripcionProducto(String descripcionProducto) {
        this.descripcionProducto = descripcionProducto;
    }

    public String getNombreMarca() {
        return nombreMarca;
    }

    public void setNombreMarca(String nombreMarca) {
        this.nombreMarca = nombreMarca;
    }

    public String getNombreCategoria() {
        return nombreCategoria;
    }

    public void setNombreCategoria(String nombreCategoria) {
        this.nombreCategoria = nombreCategoria;
    }

    public String getNombreSubcategoria() {
        return nombreSubcategoria;
    }

    public void setNombreSubcategoria(String nombreSubcategoria) {
        this.nombreSubcategoria = nombreSubcategoria;
    }

    public String getNombreUnidad() {
        return nombreUnidad;
    }

    public void setNombreUnidad(String nombreUnidad) {
        this.nombreUnidad = nombreUnidad;
    }

    public String getAbreviaturaUnidad() {
        return abreviaturaUnidad;
    }

    public void setAbreviaturaUnidad(String abreviaturaUnidad) {
        this.abreviaturaUnidad = abreviaturaUnidad;
    }

    public String getTipoCalculoUnidad() {
        return tipoCalculoUnidad;
    }

    public void setTipoCalculoUnidad(String tipoCalculoUnidad) {
        this.tipoCalculoUnidad = tipoCalculoUnidad;
    }
    public String getControlInventario() {
        return controlInventario;
    }

    public void setControlInventario(String controlInventario) {
        this.controlInventario = controlInventario;
    }

    public String getNombreUnidadInventario() {
        return nombreUnidadInventario;
    }

    public void setNombreUnidadInventario(String nombreUnidadInventario) {
        this.nombreUnidadInventario = nombreUnidadInventario;
    }

    public String getFormaVenta() {
        return formaVenta;
    }

    public void setFormaVenta(String formaVenta) {
        this.formaVenta = formaVenta;
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
    public BigDecimal getCostoPromedio() {
        return costoPromedio;
    }

    public void setCostoPromedio(BigDecimal costoPromedio) {
        this.costoPromedio = costoPromedio;
    }

    public BigDecimal getMargenMinimoPorcentaje() {
        return margenMinimoPorcentaje;
    }

    public void setMargenMinimoPorcentaje(BigDecimal margenMinimoPorcentaje) {
        this.margenMinimoPorcentaje = margenMinimoPorcentaje;
    }

    public BigDecimal getStockActualDecimal() {
        return stockActualDecimal;
    }

    public void setStockActualDecimal(BigDecimal stockActualDecimal) {
        this.stockActualDecimal = stockActualDecimal;
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
